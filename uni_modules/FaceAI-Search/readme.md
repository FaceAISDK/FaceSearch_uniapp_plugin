  
![FaceSearch](https://i.postimg.cc/Gpcjk3s5/face-search.png)
  
<div align="center">
  <h2>FaceAI Search</h2>
  <p>Android 端侧离线 1:N 人脸搜索识别</p>
  <p><strong>本地运行 · 无需后端 · 不上传人脸数据</strong></p>
</div>

---

## 功能概览

| 功能分组 | 功能说明 |
| --- | --- |
| **人脸搜索** | 1:N 相机搜索、图片搜索（Beta） |
| **人脸录入** | 相机录入、图片录入 |
| **特征管理** | 删除、查询、单条同步、批量同步 |
| **人脸抓拍** | 全屏持续抓拍、标准模式组件、兼容模式组件 |
| **活体检测** | 相机搜索和抓拍支持静默活体检测 |

### 相机搜索

- 提供全屏、标准模式组件和兼容模式组件三种演示形态。
- 搜索组件默认显示原生人脸圆形引导框和状态提示。
- 支持前后摄像头切换及无匹配提示。

### 人脸抓拍

- 提供全屏持续抓拍、标准模式组件和兼容模式组件。
- 支持连续抓拍和手动开始下一轮抓拍。
- 支持静默活体检测。

### 设备适配

- 支持 Android 5–16。
- 支持 uni-app 和 uni-app x。
- 支持手机、平板及横竖屏切换。
- 相机预览自动铺满组件区域。
- 部分设备预览异常时自动尝试兼容模式。
- Demo 提供相机权限申请、拒绝提示和系统设置引导。

## 能力矩阵

| 能力 | 全屏体验 | 标准模式组件 | 兼容模式组件 |
| --- | :---: | :---: | :---: |
| 相机人脸搜索 | ✓ | ✓ | ✓ |
| 持续人脸抓拍 | ✓ | ✓ | ✓ |

## Demo 工程

> 页面示例均已放入 Demo。README 仅介绍插件能力，实际效果请直接运行对应页面。

Demo 首页包含三个清晰分组：

1. **搜索与录入**：相机搜索、图片搜索、人脸录入及两种搜索组件。
2. **特征管理**：删除、查询、同步和批量同步。
3. **抓拍能力**：全屏抓拍及两种抓拍组件。

本插件包含第三方 Android SDK 和原生资源，不能使用标准基座直接体验。运行 Demo 前必须先制作自定义调试基座。流程可参考 [FaceAI 1:1 插件说明](https://ext.dcloud.net.cn/plugin?id=23881)和 [DCloud 自定义基座文档](https://uniapp.dcloud.net.cn/tutorial/run/run-app#customplayground)。

### 制作自定义调试基座

1. 使用 HBuilderX 打开 Demo 工程，并完成 [Android UTS 运行环境](https://doc.dcloud.net.cn/uni-app-x/plugin/uts-plugin.html)配置。
2. 在 HBuilderX 顶部选择 **运行 → 运行到手机或模拟器 → 制作自定义调试基座**。
3. 选择 Android 平台，按窗口提示提交云端打包并等待完成。
4. 打包后的 Android 调试基座通常保存在项目的 `unpackage/debug/android_debug.apk`。

<div align="center">
  <img src="https://i.postimg.cc/QVZFgycd/1.png" width="720" alt="制作自定义调试基座" />
</div>

### 使用自定义基座运行(必须要自定义基座)

1. 连接已开启 USB 调试的 Android 手机，或启动 ARM 架构 Android 模拟器。
2. 选择 **运行 → 运行到手机或模拟器 → 运行到 Android App 基座**。
3. 在运行窗口中选择 **使用自定义基座运行**，并选择刚生成的本地基座和目标设备。
4. 点击运行，等待 HBuilderX 完成安装、编译并启动 Demo。

<div align="center">
  <img src="https://i.postimg.cc/QdwtZM60/2.png" width="720" alt="使用自定义基座运行" />
</div>

> 制作完成后仍需在运行窗口中手动选择“使用自定义基座运行”。误选标准基座时，插件中的第三方依赖和原生资源不会生效。

### 重新制作基座的情况

- 更新插件版本、第三方 SDK、原生资源或原生配置。
- 修改 AndroidManifest 或其他需要打包生效的配置。
- 控制台提示当前基座不包含插件。
- HBuilderX 大版本升级后出现基座版本不一致。

只修改页面、样式和普通业务逻辑时，通常可以继续使用现有基座。自定义调试基座仅用于开发调试，正式发布时需要重新制作正式安装包。

### Demo App

<div align="center">
  <img src="https://www.pgyer.com/app/qrcode/hiface" width="150" alt="扫一扫下载 Demo App" />
  <p>扫码体验完整功能</p>
</div>

## 适用范围

| 推荐场景 | 不建议场景 |
| --- | --- |
| 门禁、签到、设备登录、本地身份识别 | 金融支付、14 周岁以下群体 |

摄像头建议具备清晰成像能力，宽动态不低于 105dB；室外场景建议不低于 120dB。

## 更多信息

- 1:1 人脸验证：[FaceAI 1:1 插件](https://ext.dcloud.net.cn/plugin?id=23881)
- 原生 Android 功能：[FaceAISDK Android](https://github.com/FaceAISDK/FaceAISDK_Android)
- 问题反馈：[GitHub Issues](https://github.com/FaceAISDK/FaceSearch_uniapp_plugin/issues)
- 高精度版本及其他问题：FaceAISDK.Service@gmail.com

> 反馈问题时，请注明页面类型、功能、设备型号和复现场景。

<div align="center">
  <sub>Powered by FaceAISDK · Copyright © 2026</sub>
</div>
