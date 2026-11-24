# 一键连锁形状切换+ (Neo)

[OneKeyMiner](https://www.mcmod.cn/class/15461.html) 的附属模组。给它加了两个东西：滚轮换形状，以及挖之前先看见会挖到哪。

## 环境

- Minecraft 1.21.8
- NeoForge 21.8.54
- OneKeyMiner(Neo)Forge 1.6.x

## 用法

| 操作 | 效果 |
| --- | --- |
| Shift + 滚轮 | 换挖掘形状，屏幕中间出现轮盘 |
| 准星对准方块 | 线框预览：金色是准星方块，青色是连锁目标 |
| V | 开关预览（可在「选项 → 控制」里改键） |

轮盘中间是当前形状名、尺寸（宽×高×深 · 方块数）和序号，尺寸取自实际预览坐标。

预览和形状算法都来自 OKM，所以预览的几何跟真正挖出来的形状是一致的；不模拟工具限制和方块黑白名单。

## 配置

`config/okm-shapeswitch-client.toml`，也可以从模组列表点「配置」打开。

| 键 | 默认 | 说明 |
| --- | --- | --- |
| `enabled` | true | 总开关 |
| `requireShift` | true | 改 false 则单滚轮就切换（会顶掉快捷栏滚动） |
| `invertScroll` | false | 反转滚轮方向 |
| `showWheel` | true | 显示轮盘 |
| `wheelHoldMillis` | 1200 | 松开 Shift 后轮盘保留时间 |
| `wheelOffsetY` | 0 | 轮盘垂直偏移（像素） |
| `showActionBar` | true | 切换时物品栏上方提示 |
| `previewMode` | always | `always` / `shift` / `off` |
| `previewMaxBlocks` | 512 | 预览最多画多少方块 |
| `previewAlpha` | 150 | 线框不透明度 0-255 |
| `previewShowOrigin` | true | 高亮准星方块 |
| `previewUseOkmLimits` | true | 沿用 OKM 的 maxBlocks / maxDistance |
| `excludedShapes` | 空 | 不参与轮换的形状 ID，逗号分隔 |

## 构建

JDK 21 + Gradle 8.14+，不需要 OKM 的 jar。

```bash
gradle build
```

产物在 `build/libs/`。

对 OKM 的调用走运行期反射（`OkmBridge`），因为它的 NeoForge 发布包被 Forgix 重命名过，形状类带 `_neoforge` 后缀，其中 Builder 的名字在源码里写不出来。这样 universal / neoforge / dev 三种包都能用。

## 已知问题

- 触控板滚动量不足一格时不会触发切换，这是原版滚轮累积阈值的表现。
- 预览只算准星指向的那一块，不会预测连锁之后下一块的位置。

## 许可

MIT
