把 OneKeyMiner 的 **NeoForge 专用 jar** 放在这个目录里（不是 Modrinth 上那个三平台合并包！）。

正确文件（GitHub Releases 的资产，约 200 KB）：
    onekeyminer-neoforge-1.6.6-1.21.7.jar

下载：
    https://github.com/Mai-xiyu/OneKeyMiner/releases/tag/1.21.7-1.6.6
    https://github.com/Mai-xiyu/OneKeyMiner/releases/download/1.21.7-1.6.6/onekeyminer-neoforge-1.6.6-1.21.7.jar

⚠️ 不要用下面这个：
    onekeyminer-1.6.6-1.21.7.jar          ← Modrinth 上的 Forgix 合并包
它把三平台的类合并到一起并改名（例如 ChainShape_neoforge），编译时会报
"ChainShape_neoforge cannot be converted to ChainShape"。

⚠️ 也不要用 1.5.x 的旧版（可能没有 getPreviewPositions / playerLookingVertical）。

说明：
- build.gradle 以 compileOnly + runtimeOnly 方式引用 libs/*.jar，不会打进本模组产物。
- 玩家实际安装时，也应该用这个 neoforge 专用 jar（它的 minecraft 依赖范围是 [1.21.7,)，
  所以 1.21.8 也能加载）。
