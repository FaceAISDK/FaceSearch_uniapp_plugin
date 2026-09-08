<template>
	<view
		class="page"
		:class="useWideLayout ? 'page-landscape' : 'page-portrait'"
	>
		<scroll-view scroll-y="true" class="menu-panel">
			<view class="menu-group">
				<text class="group-title">搜索与录入</text>
				<view class="button-grid">
					<button class="gray-button" @tap="faceSearchByCameraDemo">相机人脸搜索识别</button>
					<button class="gray-button" @tap="faceSearchByImageDemo">图片人脸搜索识别(Beta)</button>
					<button class="gray-button" @tap="addFaceSearchFeatureByCameraDemo">SDK相机录入人脸信息</button>
					<button class="gray-button" @tap="addFaceSearchFeatureByImageDemo">通过图片录入人脸信息</button>
				</view>
			</view>
			<view class="menu-group">
				<text class="group-title">特征管理</text>
				<view class="button-grid">
					<button class="gray-button" @tap="deleteFaceSearchFeatureDemo">删除人脸搜索特征值</button>
					<button class="gray-button" @tap="queryFaceSearchFeatureDemo">查询人脸搜索特征值</button>
					<button class="gray-button" @tap="insertFaceSearchFeatureDemo">同步人脸搜索特征值</button>
					<button class="gray-button" @tap="insertManyFaceFeatureSDemo">批量同步人脸搜索特征值</button>
				</view>
			</view>
			<view class="menu-group">
				<text class="group-title">抓拍能力</text>
				<view class="button-grid">
					<button class="gray-button" @tap="captureFaceByCameraDemo">UTS API 全屏持续抓拍</button>
				</view>
			</view>
		</scroll-view>

		<view
			class="result-box"
			:class="useWideLayout ? 'result-box-landscape' : 'result-box-portrait'"
		>
			<scroll-view scroll-y="true" class="scroll-view-box">
				<text class="text-content">{{faceAIResult}}</text>
			</scroll-view>
			<text class="email-text">Email: FaceAISDK.Service@gmail.com</text>
		</view>
	</view>
</template>

<script> 
	// 引入模块，注意保持路径一致
	import {
		faceSearchByCamera,
		faceSearchByImage,
		switchCamera,
		insertFaceSearchFeature,
		insertManyFeatures,
	    addFaceSearchFeatureByCamera,
		addFaceSearchFeatureByImage,
		deleteFaceSearchFeature,
		queryFaceSearchFeature,
		captureFaceByCamera,
		TTSPlayer,
		toastMessage
	} from "@/uni_modules/FaceAI-Search";
	
	// 注意：vue应该使用 testData.uts 而不是 testData.js	
	import { JSON_FACE_FEATURES_DATA } from "./faceFeatureList.js";
	import { base64FaceSearch, base64FaceImage } from './imageData.js';
	
	export default {
		data() {
			return {
				faceID: 'Test',
				faceFeature: 'faceFeature is a string with lenth 1024',
			faceAIResult: 'faceAIResult',
			captureFaceCount: 0,
				useWideLayout: false,
				base64FaceSearch: base64FaceSearch,
				base64FaceImage: base64FaceImage  //建议640*480 人脸图需要遵守规范：https://i.postimg.cc/RCwNy0kV/add-Face.jpg
			}
		},
		onLoad() {
			const windowInfo = uni.getWindowInfo()
			this.useWideLayout = windowInfo.windowWidth > windowInfo.windowHeight && windowInfo.windowWidth >= 600
		},
		onResize(options) {
			this.useWideLayout = options.size.windowWidth > options.size.windowHeight && options.size.windowWidth >= 600
		},
		
		methods: {
			captureFaceByCameraDemo: function () {
				captureFaceByCamera(
					1,
					true,
					0,
					0.12,
					-1,
					(result) => {
						this.captureFaceCount++
						this.faceAIResult = `持续抓拍 #${this.captureFaceCount}\n` +
							`silentScore: ${result.silentScore}\n` +
							`croppedBase64 length: ${result.croppedBase64.length}\n` +
							`originBase64 length: ${result.originBase64.length}`
					},
					(error) => {
						this.faceAIResult = `持续抓拍错误 ${error.code}: ${error.message}`
					}
				)
			},

			/**
			 * 1:N相机人脸搜索识别，建议使用SDK相机录入人脸，图片没有校验
			 * threshold默认0.85以上，否则可能误识别 以及强烈建议使用SDK 相机录入人脸
			 * 摄像头需成像清晰，宽动态值大于105DB，室外120DB
			 * 
			 * */
			faceSearchByCameraDemo: function () {				
				const threshold = 0.85;    // 阈值[0.8.0.9],只有人脸库中匹配到的人脸相似度大于此才有结果返回
				const oneTime = false;     // 搜索页持续存准备人脸搜索， 还是仅仅搜索一次返回结果后关闭搜索页面
				const searchTimeOut = 4000; // 搜索超时时间，超时后会提示无结果,默认3000，范围[3000,6000]毫秒
				
			    faceSearchByCamera(
			        threshold,
			        oneTime,
			        searchTimeOut,
			        (jsonStr) => { 
			            try {
			                const root = JSON.parse(jsonStr);
			                const results = root.data;
			                const base64 = root.base64; //注意base64可能为空
							console.log("收到搜索结果:", results);

							// 如果需要活体检测，加上相应的判断
							const liveness = root.liveness
			            
			                this.faceAIResult = "【人脸搜索回调】\nList: " + JSON.stringify(results);
			                if (results && results.length > 0) {
								//结果已经排好序，第一个就是相似度最高的
			                    const firstFace = results[0];
			                    const name = firstFace.faceName;
			                    const score = firstFace.faceScore;
			                    
			                    if (name != null ) {
			                            toastMessage(base64,"最匹配:" + name + "," + score);
										TTSPlayer(name)
			                    }
			                } else {
			                    toastMessage("","暂无搜索匹配人脸");
			                }
			            } catch (e) {
			                console.error("解析数据失败:", e);
			            }
			        }
			    );
			},
			
			
			/**
			 * 「图片人脸搜索识别」检测图片中出现的人脸的坐标以及检索是否有大于threshold的最佳匹配人员
			 * 人脸像素宽高需要大于130，无遮挡的清晰正脸
			 */
			faceSearchByImageDemo: function () {
			
			    const threshold = 0.82;    // 阈值[0.8.0.9],只有人脸库中匹配到的人脸相似度大于此才有结果返回			
			    faceSearchByImage(
			        this.base64FaceSearch,
			        threshold,
			        (jsonStr) => {  

			            const root = JSON.parse(jsonStr);  
			            
			            const code = root.code ?? 0;
			            const msg = root.msg ?? "";
			            
			            console.log("图片检索收到响应 -> code:", code, "msg:", msg);
			            console.log("图片检索 -> jsonStr:", jsonStr);
			            this.faceAIResult = jsonStr;  
			
			            if (code == 1) {
			                const results = root.result;  
			                
			                if (results != null && results.length > 0) {
			                    // 1. 统计检测到的人脸总数
			                    const totalFaces = results.length;
			                    
			                    // 2. 统计匹配成功（faceName 不为空）的人数
			                    let matchedCount = 0;
			                    for (let i = 0; i < totalFaces; i++) {
			                        const faceData = results[i];
			                        const faceName = faceData.faceName ?? "";  
			                        
			                        // 如果 faceName 存在且不为空字符串，则认为匹配成功
			                        if (faceName.trim() != "") {
			                            matchedCount++;
			                        }
			                    }
			                    
			                    // 3. 更新 UI 显示结果
			                    this.faceAIResult = `图片检索完成\n` +
			                                        `检测到人脸数: ${totalFaces}\n` +
			                                        `匹配成功人数: ${matchedCount}\n` +
			                                        `详细数据: ${jsonStr}`;
			                                            
			                    toastMessage("", `检测到${totalFaces}人脸，匹配成功${matchedCount}人`);
			
			                } else {
			                    this.faceAIResult = `图片检索完成\n未检测到人脸`;
			                    toastMessage("", "未检测到人脸");
			                }
			            } else {
			                this.faceAIResult = `图片检索失败\n` +
			                                    `错误码(code): ${code}\n` +
			                                    `错误原因(msg): ${msg}`;
			                                        
			                toastMessage("", `检索失败: ${msg}`);
			            }           
			    
			        }   
			    );  
			},
			
			/**
			* 人脸搜索人脸特征录入
			*/
			addFaceSearchFeatureByCameraDemo: function () {  
				addFaceSearchFeatureByCamera(
					this.faceID,
					1,    // 1.快速模式 2.精确模式
					true, // 是否显示确认框
					(result) => { 
						// 打印结果 json
						this.faceAIResult = JSON.stringify(result, ['code', 'msg', 'faceBase64'], 4)
					}
				)
			},
			
			/**
			* 人脸搜索人脸特征录入，通过Base64图片（警告：图片方式录入人脸没有质量校验，会带来精度降低）
			* 规范人脸原始图片收集 https://mp.weixin.qq.com/s/aGPwYUYxnr6ZDRxwAQd8vg
			* 建议640*480 人脸图需要遵守规范：https://i.postimg.cc/RCwNy0kV/add-Face.jpg
			*/
			addFaceSearchFeatureByImageDemo: function () {
				addFaceSearchFeatureByImage(
				     this.faceID,
					 this.base64FaceImage,
					 (result)  => {
						//打印结果 json
						console.log("result:", result);
						this.faceAIResult = JSON.stringify(result, ['code', 'msg', 'faceBase64'], 4)
					})
			},
			
			
			/**
			* 删除人脸搜索人脸特征
			*/
			deleteFaceSearchFeatureDemo: function () {
				deleteFaceSearchFeature(
					this.faceID,
					(result) => {
						this.faceAIResult = JSON.stringify(result)
					}
				)
			},
			
		   /**
			* 查询人脸搜索人脸特征
			*/
			queryFaceSearchFeatureDemo: function () {
				queryFaceSearchFeature(
					this.faceID,  // 不传则查询本地所有的数据
					(result) => { // 移除 :string 类型
						this.faceAIResult = "【人脸查询回调】\n" + result;
					}
				)
			},
			
			
		   /**
			* 人脸搜索人脸特征更新同步
			*/
			insertFaceSearchFeatureDemo: function () {
				insertFaceSearchFeature(
				   this.faceID,
				   this.faceFeature,
				   "tag",
				   "group",
					 (result) => {
						this.faceAIResult = JSON.stringify(result)
					})
			},
			
		   /**
			* 批量操作人脸搜索人脸特征更新同步
			*/
			insertManyFaceFeatureSDemo: function () {
				insertManyFeatures(
					JSON_FACE_FEATURES_DATA,
					(result) => {
						this.faceAIResult = JSON.stringify(result)
					}
				)
			},
			
		   /**
			* 切换前后摄像头，一般0是前置， 1是后置 （但是部分定制Android设备不太标准）
			* 插件目前仅仅支持系统RGB摄像头，UVC协议相机只有原生Android 代码支持
			* */
			switchCameraDemo: function () {
				switchCamera(0)
			},					
			
	   }			
	}
</script>

<style>
	.page {
		display: flex;
		min-width: 0;
		min-height: 100vh;
		padding: 12px;
		box-sizing: border-box;
		background-color: #ffffff;
	}

	.page-portrait {
		flex-direction: column;
	}

	.page-landscape {
		flex-direction: row;
		height: 100vh;
		min-height: 0;
		align-items: stretch;
	}

	.menu-panel {
		flex: 1;
		min-width: 0;
		min-height: 0;
		padding-right: 6px;
		box-sizing: border-box;
	}

	.menu-group {
		margin-bottom: 8px;
	}

	.group-title {
		display: block;
		margin-left: 4px;
		color: #6f6472;
		font-size: 14px;
	}

	.button-grid {
		display: flex;
		flex-direction: row;
		flex-wrap: wrap;
	}

	.gray-button {
		flex: 1;
		min-width: 280px;
		margin: 4px;
		background-color: #faf7fa;
		color: #800080;
		font-size: 18px;
		border: 1px solid #eadfea;
		border-radius: 8px;
	}

	.result-box {
		display: flex;
		flex-direction: column;
		min-width: 0;
		min-height: 0;
		padding: 10px;
		border: 1px solid #d8d8d8;
		border-radius: 10px;
		box-sizing: border-box;
		background-color: #f8f8f8;
	}

	.result-box-portrait {
		height: 176px;
		margin-top: 12px;
		flex-shrink: 0;
	}

	.result-box-landscape {
		width: 42%;
		margin-left: 12px;
		flex-shrink: 0;
		align-self: stretch;
	}

	.scroll-view-box {
		flex: 1;
		min-height: 0;
		width: 100%;
	}

	.text-content {
		color: #333333;
		font-size: 14px;
		line-height: 20px;
		white-space: pre-wrap;
	}

	.email-text {
		margin-top: 4px;
		color: #777777;
		font-size: 11px;
	}
</style>
