## 插件简介

人脸搜索识别Android端，支持uniappX和uniapp（上线前新建一个测试工程）。
后面我们会支持主题色定制等功能，更多可根据原生工程项目修改升级插件

**插件Demo工程：**  
[先跑通这个插件接入Demo工程](https://github.com/FaceAISDK/FaceSearch_uniapp_plugin)

![demo](https://i.postimg.cc/LXdddddddn.png)

插件应用市场地址：https://ext.dcloud.net.cn/plugin?id=99999999999999999999999

## 使用方法
  如果你是第一次运行UTS插件工程/引入UTS API插件，你应先安装官方说明配置好基础环境 [基础环境](https://doc.dcloud.net.cn/uni-app-x/plugin/uts-plugin.html) 

  - 1. 下载Demo工程[](https://github.com/FaceAISDK/FaceAISDK_uniapp_UTS)先跑通。熟悉后参考文档集成到主项目

  - 2. 按照文档 -》把插件引入项目（即 import {faceVerify,livenessVerify,getFaceFeature等方法} from "@/uni_modules/FaceAISDK-Core";）

  - 3. 运行-》运行到手机或模拟器 -》制作自定义调试基座 -》打包 等基座制作完成   
    ![制作自定义调试基座](https://i.postimg.cc/QVZFgycd/1.png)

  - 4. 运行 -》 运行到手机或模拟器-》运行到iOS/Android基座-》选择使用自定义基座运行-》选择手机-》运行
    ![运行到手机](https://i.postimg.cc/QdwtZM60/2.png)
	**请手动勾选对正确的运行方式，很多朋友辛苦打好了自定义基座包，结果运行的时候没选对导致运行不了**  
	
	
  若之前手机安装过基座需要先卸载之前的基座，iOS 可能会提示你安装好后杀死应进程后重新启动(可以点击几个其他应用加快彻底杀死重启)
  注：只支持真机调试，需要用到硬件摄像头



