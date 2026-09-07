  
![FaceSearch](https://i.postimg.cc/Gpcjk3s5/face-search.png)
  
<div align="center">
  <h1>FaceAI Search</h1>
  <p>Android 端侧离线 1:N 人脸搜索识别插件</p>
  <p><strong>uni-app · uni-app x · Android 5–16 · 完全离线</strong></p>
</div>

---

## 项目简介

FaceAI Search 面向门禁、签到、设备登录等本地身份识别场景。搜索、特征管理和抓拍均在 Android 设备端完成，无需部署后端服务，不上传或收集人脸数据。

> **隐私优先：** 人脸图片、特征值和识别过程均保留在本地设备。

## 核心能力

| 功能分组 | 已支持能力 |
| --- | --- |
| **搜索与录入** | 1:N 相机搜索、图片搜索（Beta）、相机录入、图片录入 |
| **特征管理** | 删除、查询、单条同步、批量同步人脸特征值 |
| **人脸抓拍** | 全屏持续抓拍、标准模式组件、兼容模式组件 |
| **活体检测** | 相机搜索和人脸抓拍支持静默活体检测 |
| **相机能力** | 前后摄像头切换、预览自动铺满、异常预览自动兼容 |
| **屏幕适配** | 手机、平板、横竖屏及可调整大小窗口 |

### 展示形态

| 能力 | 全屏体验 | 标准模式组件 | 兼容模式组件 |
| --- | :---: | :---: | :---: |
| 相机人脸搜索 | ✓ | ✓ | ✓ |
| 持续人脸抓拍 | ✓ | ✓ | ✓ |

- 搜索组件默认显示原生人脸圆形引导框和状态提示。
- 抓拍组件支持连续抓拍以及手动开始下一轮抓拍。
- 相机权限被拒绝时，Demo 会提供重新授权或进入系统设置的引导。

## 运行环境

| 项目 | 支持情况 |
| --- | --- |
| 系统 | Android 5–16 |
| 框架 | uni-app、uni-app x |
| 运行方式 | Android 自定义调试基座 |
| 网络依赖 | 识别过程无需网络 |
| 设备权限 | 相机 |

## 运行 Demo

> 本仓库已包含全部演示代码。首页按 **搜索与录入**、**特征管理**、**抓拍能力** 三组展示，具体实现直接查看对应 Demo 页面。

本插件包含第三方 Android SDK 和原生资源，标准基座不包含这些内容，因此运行前必须先制作自定义调试基座。完整流程也可参考 [FaceAI 1:1 插件说明](https://ext.dcloud.net.cn/plugin?id=23881)和 [DCloud 自定义基座文档](https://uniapp.dcloud.net.cn/tutorial/run/run-app#customplayground)。

### 1. 准备运行环境

1. 安装并登录 HBuilderX，打开本工程。
2. 参考 [DCloud UTS 插件环境说明](https://doc.dcloud.net.cn/uni-app-x/plugin/uts-plugin.html)完成 Android 开发环境配置。
3. Android 真机开启开发者选项和 USB 调试，并确认 HBuilderX 能识别设备；使用模拟器时请选择 ARM 架构镜像。

### 2. 制作自定义调试基座（必须要自定义基座）

1. 在 HBuilderX 顶部选择 **运行 → 运行到手机或模拟器 → 制作自定义调试基座**。
2. 选择 Android 平台，按打包窗口提示确认应用信息并提交云端打包。
3. 等待打包完成。生成的 Android 调试基座通常位于项目的 `unpackage/debug/android_debug.apk`。

<div align="center">
  <img src="https://i.postimg.cc/QVZFgycd/1.png" width="720" alt="制作自定义调试基座" />
</div>

### 3. 使用自定义基座运行

1. 连接 Android 手机或启动 ARM 架构模拟器。
2. 选择 **运行 → 运行到手机或模拟器 → 运行到 Android App 基座**。
3. 在运行窗口中选择 **使用自定义基座运行**，再选择刚生成的本地基座和目标设备。
4. 点击运行，HBuilderX 会安装或启动基座，并将当前 Demo 编译到设备中。

<div align="center">
  <img src="https://i.postimg.cc/QdwtZM60/2.png" width="720" alt="使用自定义基座运行" />
</div>

> **请特别确认：** 制作完基座后，运行时仍需手动选择“使用自定义基座运行”。如果误选标准基座，插件中的第三方依赖和原生资源不会生效。

### 4. 什么时候需要重新制作

- 只修改页面、样式和普通业务逻辑时，通常可以继续使用现有基座运行。
- 更新插件版本、第三方 SDK、原生资源、AndroidManifest 或原生配置后，需要重新制作自定义调试基座。
- 如果提示基座不包含插件，可重新制作基座；仍未恢复时，先卸载设备上的旧基座再运行。
- 自定义调试基座只用于开发调试，不能作为正式安装包发布。

相机搜索和人脸抓拍均提供全屏、标准模式、兼容模式独立演示页面。

### Demo App

<div align="center">
  <img src="https://www.pgyer.com/app/qrcode/hiface" width="150" alt="扫一扫下载 Demo App" />
  <p>扫码体验完整人脸识别功能</p>
</div>

## 使用说明

| 推荐场景 | 不建议场景 |
| --- | --- |
| 门禁、签到、设备登录、本地身份识别 | 金融支付、14 周岁以下群体 |

为保证识别效果，摄像头应具备清晰成像能力，宽动态建议不低于 105dB，室外场景建议不低于 120dB。

## 相关项目与反馈

- 1:1 人脸验证：[FaceAI 1:1 插件](https://ext.dcloud.net.cn/plugin?id=23881)
- 原生 Android 项目：[FaceAISDK Android](https://github.com/FaceAISDK/FaceAISDK_Android)
- 问题反馈：[GitHub Issues](https://github.com/FaceAISDK/FaceSearch_uniapp_plugin/issues)
- 联系邮箱：FaceAISDK.Service@gmail.com

> 提交问题时，请注明页面类型、功能、设备型号和复现场景。

<div align="center">
  <sub>Powered by FaceAISDK · Copyright © 2026</sub>
</div>
