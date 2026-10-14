# 空岛五选一 (Sky Island Five Choices)

`由AI辅助完成`

Minecraft 1.20.1 / Forge 47.x 模组。新增一个「空岛」世界生成选项：出生在虚空中的一块草方块平台上，
之后每个游戏日由一位随机玩家从 5 种方块中选 1 种，铺到平台下方；到达世界底部那天照常选择，
同时在平台中心顶上发放一个装有 12 个末地传送门框架和 12 个黑曜石的箱子。


## 玩法

1. 新建世界时，左下角打开 **「空岛世界」** 开关，创建世界。
2. 出生在虚空中 9×9 的草方块平台上，世界出生点在平台正中心上方 1 格 `(0, 1, 0)`。
3. 每个游戏日（`dayTime / 24000` 递增，睡觉跳过夜晚也算）触发一次方块选择：
   - 随机挑一位在线玩家（旁观者除外），只有他能操作界面；
   - 界面标题「方块选择」，5 个格子初始只有第 1 格显示方块，其余用「?」遮盖；
   - 点「刷新」展示下一种方块，5 种全部展示后刷新按钮失效；
   - 第一次点「确认」把**当前展示的那一格**锁定为选择结果，
     同时把其余未展示的方块**全部揭晓**；第二次点「确认」写入世界；
   - 聊天栏播报「第 X 天：选择了 XX，跳过了 XX」。
4. 填充层从 `y=-1` 起逐层向下、**不留空档**（-1, -2, -3 ……），每层与平台同宽（9×9）。
5. 当下一层已经低于世界底部时（目标 y < `getMinBuildHeight()`），这一天**照常随机选择并走完
   刷新/确认流程**，但方块不再写入世界，改为在平台中心顶上放置一个箱子，
   内含 **12 末地传送门框架 + 12 黑曜石**。

### 随机方块池

从所有原版方块中筛选可独立放置的方块，排除：

- 需要依附的方块（火把、梯子、告示牌、门、植物、铁轨等，含朝向/附着类方块属性）；
- 重力方块（沙子、沙砾、混凝土粉末等 `FallingBlock`）；
- 带方块实体的方块（箱子、熔炉等）；
- 所有地毯（含苔藓地毯）；
- 非方块实体（空气、火、传送门）。

**水和岩浆被显式放行**（它们是流体，`PushReaction` 为 `DESTROY`，会被通用规则误杀）。
放置后是源方块，会正常流动。

## 代码结构

```
src/main/java/com/skyisland/
├── SkyIslandMod.java                    模组入口
├── choice/
│   ├── BlockSelectionPool.java          随机方块池与过滤规则
│   ├── SelectionSession.java           一次选择的会话状态（展示/确认/跳过）
│   ├── SelectionHandler.java           服务端主逻辑：选玩家、填充、发放箱子
│   ├── SkyIslandDayCycle.java          每日时钟与触发
│   ├── IslandEvents.java               Forge 事件（tick / 登录 / 登出）
│   └── SelectionScreenOpener.java      跨端安全的客户端跳板
├── client/
│   ├── BlockSelectionScreen.java       方块选择界面
│   ├── SelectionScreenBridge.java      界面开关
│   └── WorldCreationOption.java        创建世界界面的「空岛世界」开关
├── network/
│   ├── SkyIslandNetwork.java           网络通道
│   └── SelectionMessage.java           界面协议（OPEN/REVEAL/COMMIT/STATE/CLOSE）
└── world/
    ├── WorldTransformer.java           空岛世界识别、铺平台、清结构、设出生点
    └── SkyIslandData.java              存档状态（天数、已填层数、箱子已发等）

src/main/resources/
├── META-INF/mods.toml
├── pack.mcmeta
├── data/skyisland/worldgen/world_preset/sky_island.json   空岛世界预设（无层超平坦虚空）
└── assets/skyisland/lang/{zh_cn,en_us}.json
```
