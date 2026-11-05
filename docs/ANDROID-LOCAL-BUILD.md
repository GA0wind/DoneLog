# DoneLog 本地 Android 打包

本项目使用 uni-app x VDOM。流程为 HBuilderX 导出 Kotlin / 资源，再由本机 Gradle 编译 APK。无需云端打包。

## 一键构建

先打开 HBuilderX，在项目根目录运行：

```powershell
.\scripts\Build-Android.ps1
```

输出为：

```text
unpackage/release/apk/DoneLog-1.0.0-local-debug.apk
```

这是可安装的 Debug APK，使用 Android Debug 调试签名。已有应用升级需要相同签名；正式发布应使用原有 keystore 签名。

构建脚本会自动：

1. 调用 `cli publish app-android --type appResource` 导出当前源码。
2. 首次下载匹配的官方 uni-app x SDK 5.34 和 Gradle 8.14.3。
3. 将 Kotlin、静态资源、桌面组件源码、Manifest 和布局复制到生成的原生工程。
4. 使用 HBuilderX 自带 JDK 17、本机 Android SDK 编译。
5. 复制 APK 到输出目录并打印 SHA-256。

首次构建会下载 Maven 依赖；编译和签名均在本机完成。后续复用依赖缓存。

## 本机环境

| 项目 | 配置 |
| --- | --- |
| HBuilderX | `D:\HBuilderX`，5.34.2026102909 |
| uni-app x SDK | 5.34 / 15179，VDOM |
| JDK | HBuilderX 自带 Amazon Corretto 17.0.12 |
| Android SDK | `D:\androidSDK`，Platform 35 / Build Tools 35.0.0 |
| Gradle / AGP / Kotlin | 8.14.3 / 8.12.0 / 2.2.0 |
| Python | Python 3.9+，仅使用标准库准备原生工程 |
| PowerShell | 本机已验证 PowerShell 7.6.9 |
| 包名 / 版本 | `com.donelog.app`，1.0.0 / 100（版本取自导出资源） |

自定义安装位置：

```powershell
.\scripts\Build-Android.ps1 -HBuilderPath 'D:\HBuilderX' -AndroidSdk 'D:\androidSDK'
```

默认使用 `GRADLE_USER_HOME` 缓存；未配置时使用项目的 `unpackage/local-build/gradle-cache`。

原生工程生成到 `unpackage/local-build/android`，可在 Android Studio 打开。该目录为生成目录，手工修改可能被下次构建覆盖；需要保留的构建配置请修改 `scripts/android/`。

## Release 与调试安装

```powershell
# 生成未签名 Release APK，之后使用自己的 keystore 签名
.\scripts\Build-Android.ps1 -Configuration Release

# 已经导出当前源码，仅重建原生部分
.\scripts\Build-Android.ps1 -SkipExport

# 安装本地 Debug APK
adb -s emulator-5554 install -r unpackage\release\apk\DoneLog-1.0.0-local-debug.apk
```

`-SkipExport` 仅适用于导出资源已经与当前源码一致的情况。默认不跳过导出。

## 版本与桌面组件

- 编译器与离线 SDK 必须匹配；脚本会校验资源的 `compilerVersion`。
- 桌面组件从 `@/uni_modules/donelogWidget` 导入，由 HBuilderX 导出为独立原生模块。直接导入内部 `utssdk` 目录会绕过模块资源与类的导出。
- 生成的三个 Gradle 模块为 `app`、`uniappx` 和 `donelogWidget`。
- SDK、依赖和构建产物均放在 Git 忽略的 `unpackage/` 中。

官方资料：[Android 原生工程配置](https://doc.dcloud.net.cn/uni-app-x/native/use/android.html)、[匹配版本 SDK 下载](https://doc.dcloud.net.cn/uni-app-x/native/download/android.html)、[UTS 插件配置](https://doc.dcloud.net.cn/uni-app-x/native/use/androiduts.html)。
