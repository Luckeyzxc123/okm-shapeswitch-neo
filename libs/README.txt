把 OneKeyMiner(Neo)Forge 的 jar 放在这个目录里。

本模组使用 OKM 1.6.x 的公开形状 API（OneKeyMinerAPI / ShapeRegistry / ShapeContext.Builder /
ChainShape#getPreviewPositions），因此请使用 **1.6.0 及以上**的版本，例如：

    onekeyminer-1.6.6-1.21.7.jar     (Forgix 通用版，同时支持 1.21.7 / 1.21.8)
    onekeyminer-1.6.9-1.21.7.jar     (1.21.7 分支的较新构建)

不要用 1.5.x 之类的旧版：它们可能没有 getPreviewPositions / playerLookingVertical，
编译会失败。

下载地址：
    https://modrinth.com/mod/onekeyminer_nf/versions?g=1.21.8
    https://github.com/Mai-xiyu/OneKeyMiner/releases

build.gradle 会把 libs/*.jar 以 compileOnly + runtimeOnly 方式加入 classpath；
它不会被打进本模组的产物里 —— 玩家需要自己安装 OKM。
