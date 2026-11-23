# 怎么把源码变成 jar（不用装 JDK / Gradle）

这个工程里**没有 jar**，需要编译一次。编译要下载 NeoForge / Minecraft 依赖，所以交给 GitHub 的服务器做最省事：
把工程推到一个 GitHub 仓库，仓库里的 [`.github/workflows/build.yml`](build.yml) 会自动编译，然后把 jar 作为可下载的 Artifact 给你。

---

## 准备

- 一个 GitHub 账号（免费即可）
- 浏览器
- 本工程（`okm-shapeswitch-neo.zip` 解压后的文件夹）

> 私有仓库同样能用，GitHub 免费账号每月有 2000 分钟 Actions 额度，这个工程一次构建大约 5–15 分钟（首次要反编译 Minecraft，会比较久；之后再构建会走缓存）。

---

## 步骤

### 1. 解压 zip

解压后应该看到这些文件/文件夹（`.github` 可能是隐藏风格的名字，但 Windows 资源管理器默认能看到）：

```
okm-shapeswitch-neo/
├── .github/workflows/build.yml     <- 自动构建脚本（必须有！）
├── build.gradle
├── gradle.properties
├── settings.gradle
├── README.md
├── LICENSE
├── libs/README.txt
└── src/main/java/...               <- 模组源码
```

### 2. 在 GitHub 上新建仓库

1. 打开 <https://github.com/new>
2. `Repository name` 填 `okm-shapeswitch-neo`（随便取）
3. 选 `Public` 或 `Private` 都行
4. **不要**勾选 `Add a README file` / `.gitignore` / `license`（保持空仓库）
5. 点 `Create repository`

### 3. 上传文件

在新建出来的空仓库页面，点中间那段提示里的 **`uploading an existing file`** 链接（或直接访问
`https://github.com/<你的用户名>/<仓库名>/upload/main`）。

1. 打开解压出来的 `okm-shapeswitch-neo` 文件夹
2. 全选里面的**内容**（`Ctrl+A`），拖进浏览器的上传框
   - 要拖**文件夹里的内容**，不是把外层文件夹或 zip 本身拖进去；
   - 请确认拖进去的内容里包含 **`.github` 文件夹**，否则 GitHub 不会自动构建。
3. 等文件列表出现后，页面下方 `Commit changes` 点一下

> 如果浏览器上传 `.github` 文件夹失败：在仓库页面点 `Add file → Create new file`，
> 文件名输入 `.github/workflows/build.yml`，把本目录 [build.yml](build.yml) 的内容粘进去，提交即可。

### 4. 等自动构建

1. 打开仓库顶部的 **`Actions`** 标签
2. 左侧选 `build`，右侧点最新一次运行（提交后几秒就会出现，标着黄色圆点 = 运行中）
3. 首次大约 5–15 分钟；绿色对勾 = 成功

如果没有自动运行：进入 `Actions` → 左侧 `build` → 右侧 `Run workflow` → `Run workflow`（这就是 `workflow_dispatch` 手动触发）。

### 5. 下载 jar

1. 打开那次**成功的**运行
2. 页面往下滚到底部 **`Artifacts`** 区域
3. 点 **`okmshapeswitch-jar`** 下载（得到一个 zip）
4. 解压这个 zip，里面就是成品：

```
okmshapeswitch-1.21.8-1.0.0.jar
```

### 6. 装进游戏

1. `%APPDATA%\.minecraft\mods\`（或你的启动器实例的 `mods` 文件夹）里放入：
   - `okmshapeswitch-1.21.8-1.0.0.jar`
   - `onekeyminer-neoforge-1.6.6-1.21.7.jar`（NeoForge 专用版，来自
     [GitHub Releases](https://github.com/Mai-xiyu/OneKeyMiner/releases/tag/1.21.7-1.6.6)；
     **不要**用 Modrinth 的三平台合并包 `onekeyminer-1.6.6-1.21.7.jar`）
2. 启动 Minecraft 1.21.8 + NeoForge 21.8.54
3. 进世界后：准星指向方块看预览；**按住 Shift 滚动滚轮**切换形状

---

## 构建失败怎么办

先点开失败的那一步看红色日志，常见几类：

| 日志关键字 | 原因 | 处理 |
| --- | --- | --- |
| `Could not find net.neoforged:neoforge:21.8.54` | 该补丁号在 Maven 上不存在 | 工作流已自动用 `21.8.57` 重试；也可把 `gradle.properties` 的 `neo_version` 直接改成 `21.8.57` |
| `ChainShape_neoforge cannot be converted to ChainShape` | `libs/` 放的是 Forgix 三平台合并包 | 换成 `onekeyminer-neoforge-1.6.6-1.21.7.jar`（工作流已自动下载并校验） |
| `Could not resolve org.gradle.toolchains.foojay-resolver-convention` | 网络/仓库波动 | 重新运行（`Re-run all jobs`） |
| `compileJava` 里出现 `org.xiyu.onekeyminer...` 找不到 | `libs/` 里放的是 1.5.x 旧版 OKM | 换 1.6.x |
| 一切正常但 `Artifacts` 是空的 | 上传步骤失败 | 看 `Upload jar` 步骤；确认 `build/libs/*.jar` 有产物 |

## 本机编译（不想用 GitHub 的话）

需要 JDK 21、Gradle 8.14+、能访问 Maven Central 与 `maven.neoforged.net`：

```bash
# 1. 把 1.6.x 的 OKM NeoForge jar 放进 libs/
#    https://modrinth.com/mod/onekeyminer_nf/versions?g=1.21.8

# 2. 生成 wrapper（仓库里没有内置 gradle-wrapper.jar）
gradle wrapper --gradle-version 8.14

# 3. 构建
./gradlew build            # Windows: gradlew.bat build

# 产物：build/libs/okmshapeswitch-1.21.8-1.0.0.jar
```

IntelliJ IDEA 也可以：`File → Open` 选 `build.gradle`，同步完成后右侧 Gradle 面板
`Tasks → build → build`，同样在 `build/libs/` 出 jar。
