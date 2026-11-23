# OneKeyMiner ShapeSwitch Plus (Neo)

[OKMNF] 一键连锁(Neo)Forge 的**客户端附属模组**：用 **Shift + 鼠标滚轮** 循环切换挖掘形状，并在准星所指方块上**实时预览**当前形状会连锁到哪些方块。

- Minecraft **1.21.8**
- NeoForge **21.8.54**（`gradle.properties` 里的 `neo_version` 可改，官方 MDK 用的是 21.8.57）
- 依赖：[OneKeyMiner(Neo)Forge](https://www.mcmod.cn/class/15461.html)（mod id `onekeyminer`，**1.6.x**）

---

## 功能

| 功能 | 说明 |
| --- | --- |
| Shift + 滚轮切换形状 | 默认按住 Shift 才切换，避免抢走快捷栏滚动；可关掉 Shift 要求、可反转方向 |
| 形状预览 | 准星指向方块时，用线框画出当前形状会连锁到的方块（金色 = 准星原方块，青色 = 连锁目标） |
| 形状轮盘 HUD | 按住 Shift 或刚切换完的一小段时间内，屏幕中央显示一圈形状点位 + 当前形状名 + 序号 |
| 物品栏提示 | 切换时在物品栏上方提示当前形状名 |
| 跟随 OKM | 形状列表直接读 OKM 注册表，其它附属模组注册的自定义形状也会自动出现 |
| 不改存档 / 不改服务端 | 只调用 OKM 公开 API（`OneKeyMinerAPI.setSelectedShape`），存档与同步由 OKM 自己处理 |

预览用的是 OKM 自己的形状算法（`ChainShape#getPreviewPositions` + `ShapeContext.Builder`），所以**形状几何与实际连锁一致**；
但预览不会模拟工具限制、方块黑白名单等运行期规则，这部分以实际挖掘为准。

## 安装

1. 安装 Minecraft 1.21.8 + NeoForge 21.8.54。
2. 把 **OneKeyMiner(Neo)Forge 1.6.x** 放进 `mods/`。
3. 把本模组的 jar 放进 `mods/`。
4. 进游戏后按住 **Shift 滚动滚轮** 切换；准星对着方块就能看到预览。

> 本模组是**客户端模组**（`@Mod(dist = Dist.CLIENT)`），服务器不需要安装。
> 服务器上的 OKM 会校验客户端偏好，双方最好使用同一代 OKM（1.6.x）。

## 配置

文件：`config/okm-shapeswitch-client.toml`（首次进入游戏后生成；也可在模组列表里点“配置”打开图形界面）

| 键 | 默认 | 说明 |
| --- | --- | --- |
| `enabled` | `true` | 总开关 |
| `requireShift` | `true` | 切换时是否必须按住 Shift |
| `invertScroll` | `false` | 反转滚轮方向 |
| `showWheel` | `true` | 是否显示形状轮盘 |
| `wheelHoldMillis` | `1200` | 松开 Shift 后轮盘保留时长 |
| `wheelOffsetY` | `0` | 轮盘中心的垂直偏移（像素） |
| `showActionBar` | `true` | 切换时物品栏上方提示 |
| `previewMode` | `always` | `always` / `shift` / `off` |
| `previewMaxBlocks` | `512` | 预览最多计算并渲染多少个方块 |
| `previewAlpha` | `150` | 线框不透明度 0–255 |
| `previewShowOrigin` | `true` | 是否把准星原方块一起高亮 |
| `previewUseOkmLimits` | `true` | 预览使用 OKM 的 `maxBlocks` / `maxDistance` / `allowDiagonal` |
| `excludedShapes` | 空 | 不参与轮换的形状 ID，逗号分隔，如 `onekeyminer:amorphous` |

## 怎么拿到 jar

工程里**只有源码**，jar 需要编译一次才会产生（编译要下载 NeoForge / Minecraft 依赖，约 1–2 GB）。
最省事的做法是用工程自带的 GitHub Actions 工作流：见 **[GUIDE.md](GUIDE.md)**（逐步图文说明）。
三种方式任选：

1. **GitHub Actions（最省事，不用装任何东西）**
   把本工程推到 GitHub → 仓库会自动跑 `.github/workflows/build.yml`（自动下载 OKM、编译）→
   打开 `Actions` → 最新的 `build` → 页面底部 `Artifacts` → 下载 `okmshapeswitch-jar.zip`，解压即得 jar。
2. **本机命令行**：见下面的“自己编译”。
3. **IntelliJ IDEA**：`File → Open` 选中 `build.gradle` 导入，等依赖同步完后右侧 Gradle 面板 → `Tasks → build → build`，产物在 `build/libs/`。

## 自己编译

### 前置

- JDK 21（`java -version` 应为 21）
- Gradle 8.14+（或直接用 IntelliJ IDEA 打开 `build.gradle` 导入）
- 网络能访问 Maven Central / `maven.neoforged.net`

### 步骤

```bash
# 1. 把 OneKeyMiner 的 NeoForge jar 放进 libs/（必须；OKM 没有发布 Maven 坐标）
#    https://modrinth.com/mod/onekeyminer_nf/versions?g=1.21.8
#    https://github.com/Mai-xiyu/OneKeyMiner/releases   （1.6.0 及以上）

# 2. 生成 Gradle Wrapper（仓库里没有内置 gradle-wrapper.jar）
gradle wrapper --gradle-version 8.14

# 3. 构建
./gradlew build          # Windows: gradlew.bat build

# 产物：build/libs/okmshapeswitch-1.21.8-1.0.0.jar
```

`libs/` 为空时 `compileJava` 会直接报错并提示放哪个 jar（不会吐一堆找不到 `org.xiyu.onekeyminer` 的编译错误）。

### 版本说明

- `neo_version=21.8.54`：按需求填写；若解析不到该补丁号，改成官方 MDK 使用的 `21.8.57`（同属 21.8.x）。
- 编译依赖是 `compileOnly`（另加 `runtimeOnly` 方便 `runClient` 调试）：运行时由玩家自己安装 OKM，不会打进本模组 jar。
- 用到的是 OKM 1.6.x 稳定的公开 API：
  `OneKeyMinerAPI.getRegisteredShapes/getShape/getSelectedShape/setSelectedShape`、
  `ShapeContext.Builder`、`ChainShape#getPreviewPositions`、`ConfigManager#getConfig`。

## 代码结构

```
src/main/java/dev/okmplus/shapeswitch/
├── OkmShapeSwitch.java              # @Mod 入口（客户端模组）
└── client/
    ├── ClientBootstrap.java         # 注册 HUD 图层 / 输入 / 渲染事件 / 配置界面
    ├── ShapeScrollHandler.java      # InputEvent.MouseScrollingEvent -> 切换形状
    ├── ShapeSwitcher.java           # 轮换逻辑 + 物品栏提示
    ├── ShapeCatalog.java            # 读 OKM 形状注册表（带缓存）、本地化名称
    ├── ShapePreviewRenderer.java    # RenderHighlightEvent.Block -> 世界线框预览
    ├── PreviewGeometry.java         # 用 OKM 的形状算法算预览坐标（带缓存）
    ├── ShapeWheelOverlay.java       # 屏幕中央形状轮盘 HUD
    ├── ShapeSwitchConfig.java       # ModConfigSpec 客户端配置
    └── ShiftTracker.java            # Shift 按键检测
```

没有 Mixin、没有反射、没有自定义网络包。

## 已知限制

- 滚轮事件在“滚动量不足一格”时（部分触控板 / 平滑滚动）不会触发，这是原版 `ScrollWheelHandler` 的累积阈值行为。
- 预览只画**当前准星选中方块**对应的形状，不会预测“挖掉这一块之后下一块会连到哪”。
- 只响应垂直滚动（`getScrollDeltaY`）；横向滚动不切换，避免误触。
- 若 OKM 未来改动 `setSelectedShape` 语义，需要重新编译适配。

## 许可

MIT。本模组与 OneKeyMiner 作者无隶属关系，仅为公开 API 的第三方附属。
