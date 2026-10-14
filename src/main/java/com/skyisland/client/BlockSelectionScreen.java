package com.skyisland.client;

import com.skyisland.network.SelectionMessage;
import com.skyisland.network.SkyIslandNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * The "Block Selection" screen.
 *
 * <p>Five blocks are offered. Only the first one is visible; the other four are covered by a
 * question mark. Every press of "Refresh" uncovers the next one and stops working once all five
 * have been shown. "Confirm" locks in the block that is currently shown and uncovers the rest; a
 * second "Confirm" sends the final answer to the server.</p>
 */
public class BlockSelectionScreen extends Screen {

    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 150;
    private static final int SLOT_SIZE = 34;
    private static final int SLOT_SPACING = 4;
    private static final int SLOT_TOP = 42;
    private static final int BUTTON_WIDTH = 84;
    private static final int BUTTON_HEIGHT = 20;

    private static final int BACKGROUND = 0xF01A1A1A;
    private static final int BORDER_OUTER = 0xFF9E9E9E;
    private static final int BORDER_INNER = 0xFF4A4A4A;
    private static final int SLOT_BACKGROUND = 0xFF2B2B2B;
    private static final int SLOT_BORDER = 0xFF555555;
    private static final int SLOT_BORDER_HIDDEN = 0xFF303030;
    private static final int SLOT_BORDER_SHOWN = 0xFFFFFFFF;
    private static final int SLOT_BORDER_CHOSEN = 0xFFFFD54F;

    private final List<Block> options = new ArrayList<>();

    /** Index of the block that is on screen; once confirmed it is the last one, revealing them all. */
    private int shownIndex;
    /** Index of the block the player confirmed; the highlight stays here after the first confirm. */
    private int chosenIndex;
    /** True after the first confirm, i.e. when the remaining blocks are only shown for information. */
    private boolean committed;

    private Button refreshButton;
    private Button confirmButton;
    private int panelLeft;
    private int panelTop;

    public BlockSelectionScreen(SelectionMessage message) {
        super(Component.translatable("skyisland.gui.title"));
        for (int id : message.getBlockIds()) {
            Block block = BuiltInRegistries.BLOCK.byId(id);
            this.options.add(block);
        }
        this.shownIndex = message.getShownIndex();
        this.committed = message.isCommitted();
    }

    @Override
    protected void init() {
        this.panelLeft = (this.width - PANEL_WIDTH) / 2;
        this.panelTop = Math.max(4, (this.height - PANEL_HEIGHT) / 2);

        int buttonY = this.panelTop + PANEL_HEIGHT - BUTTON_HEIGHT - 12;
        this.refreshButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("skyisland.gui.refresh"),
                        button -> SkyIslandNetwork.CHANNEL.sendToServer(SelectionMessage.request(SelectionMessage.Mode.REVEAL)))
                .bounds(this.panelLeft + 16, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

        this.confirmButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("skyisland.gui.confirm"),
                        button -> SkyIslandNetwork.CHANNEL.sendToServer(SelectionMessage.request(SelectionMessage.Mode.COMMIT)))
                .bounds(this.panelLeft + PANEL_WIDTH - 16 - BUTTON_WIDTH, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

        this.updateButtons();
    }

    /** Applies a state update that was pushed by the server. */
    public void applyServerState(SelectionMessage message) {
        if (message.getMode() == SelectionMessage.Mode.STATE) {
            this.committed = message.isCommitted();
            // Revealing and highlighting are independent: after confirming, the screen stays
            // revealed up to the last block while the highlight stays on the confirmed one.
            this.shownIndex = message.getShownIndex();
            this.chosenIndex = message.getChosenIndex();
        }
        this.updateButtons();
    }

    private void updateButtons() {
        if (this.refreshButton != null) {
            // Once every block has been shown the refresh button stops working.
            this.refreshButton.active = !this.committed && this.shownIndex < this.options.size() - 1;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        graphics.fill(this.panelLeft - 1, this.panelTop - 1,
                this.panelLeft + PANEL_WIDTH + 1, this.panelTop + PANEL_HEIGHT + 1, BORDER_OUTER);
        graphics.fill(this.panelLeft, this.panelTop,
                this.panelLeft + PANEL_WIDTH, this.panelTop + PANEL_HEIGHT, BACKGROUND);
        graphics.fill(this.panelLeft + 2, this.panelTop + 2,
                this.panelLeft + PANEL_WIDTH - 2, this.panelTop + 3, BORDER_INNER);

        graphics.drawCenteredString(this.font, this.title, this.panelLeft + PANEL_WIDTH / 2,
                this.panelTop + 12, 0xFFFFFF);

        int slotsWidth = this.options.size() * SLOT_SIZE + (this.options.size() - 1) * SLOT_SPACING;
        int slotLeft = this.panelLeft + (PANEL_WIDTH - slotsWidth) / 2;
        int slotTop = this.panelTop + SLOT_TOP;

        for (int index = 0; index < this.options.size(); index++) {
            int x = slotLeft + index * (SLOT_SIZE + SLOT_SPACING);
            this.renderSlot(graphics, index, x, slotTop, mouseX, mouseY);
        }

        Component hint = this.committed
                ? Component.translatable("skyisland.gui.hint.confirm")
                : Component.translatable("skyisland.gui.hint");
        graphics.drawCenteredString(this.font, hint, this.panelLeft + PANEL_WIDTH / 2,
                slotTop + SLOT_SIZE + 12, 0xA0A0A0);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderSlot(GuiGraphics graphics, int index, int x, int y, int mouseX, int mouseY) {
        boolean revealed = index <= this.shownIndex;
        // While choosing, the block on screen is highlighted; after confirming, the highlight stays
        // on the block that was confirmed instead of following the revealed ones.
        boolean chosen = this.committed ? index == this.chosenIndex : index == this.shownIndex;

        graphics.fill(x - 1, y - 1, x + SLOT_SIZE + 1, y + SLOT_SIZE + 1,
                chosen ? SLOT_BORDER_CHOSEN : revealed ? SLOT_BORDER_SHOWN : SLOT_BORDER_HIDDEN);
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_BACKGROUND);

        if (revealed) {
            ItemStack stack = new ItemStack(this.options.get(index));
            graphics.renderItem(stack, x + (SLOT_SIZE - 16) / 2, y + 3);
            Component name = stack.getHoverName();
            int textWidth = Math.min(this.font.width(name), SLOT_SIZE + 8);
            float scale = textWidth > SLOT_SIZE ? (float) SLOT_SIZE / textWidth : 1.0F;
            graphics.pose().pushPose();
            graphics.pose().translate(x + SLOT_SIZE / 2.0F, y + 21.0F, 0.0F);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.drawCenteredString(this.font, name, 0, 0, chosen ? 0xFFD54F : 0xE0E0E0);
            graphics.pose().popPose();

            if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                graphics.renderTooltip(this.font, name, mouseX, mouseY);
            }
        } else {
            graphics.drawCenteredString(this.font, "?", x + SLOT_SIZE / 2, y + SLOT_SIZE / 2 - 4, 0x909090);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        // The selection has to be completed, closing the screen would only desynchronise it.
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Opens (or refreshes) the screen from a server push. */
    public static void open(SelectionMessage message) {
        Minecraft.getInstance().setScreen(new BlockSelectionScreen(message));
    }

    /** Closes the screen from a server push. */
    public static void close() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof BlockSelectionScreen) {
            minecraft.setScreen(null);
        }
    }
}
