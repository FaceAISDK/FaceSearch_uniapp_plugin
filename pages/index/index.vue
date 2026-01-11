<template>
	<view>
		<button class="gray-button" @click="startFaceSearchDemo">1:N人脸搜索</button>
		<button class="gray-button" @click="addFaceSearchFeatureDemo">1:N人脸搜索录入人脸</button>
		<button class="gray-button" @click="deleteFaceSearchFeatureDemo">删除人脸搜索特征值</button>
		<button class="gray-button" @click="insertFaceSearchFeatureDemo">同步人脸搜索特征值</button>
		<button class="gray-button" @click="insertManyFaceFeatureSDemo">批量同步人脸搜索特征值</button>
		
		<view class="result-box">
		      <view> Email: FaceAISDK.Service@gmail.com</view>
		       <scroll-view scroll-y="true" class="scroll-view-box">
		       <text class="text-content">{{faceAIResult}}</text>
		       </scroll-view>
		</view>
	</view>
</template>

<script>
	// 引入插件接口
	import {
		startFaceSearch,
		insertFaceSearchFeature,
		insertManyFeatures,
		addFaceSearchFeature,
		deleteFaceSearchFeature
	} from "@/uni_modules/FaceAI-Search";
	
	// 注意：标准 Vue 无法直接导入 .uts 文件作为数据源，请将 testData.uts 重命名或转换为 testData.js
	import { JSON_FACE_FEATURES_DATA } from "./testData.js"; 

	export default {
		data() {
			return {
				faceID: 'Test',
				faceFeature: 'faceFeature is a string with lenth 1024',
				faceAIResult: 'faceAIResult'
			}
		},
		onLoad() {

		},
		methods: {
			startFaceSearchDemo: function() {
				// 示例参数值：
				const threshold = 0.88; // 阈值,只有人脸库中匹配到的人脸相似度大于此才有结果返回
				const oneTime = false; // 搜索页持续搜索接收结果 还是仅仅搜索一次返回一次结果
				const highRes = false; // true，高分辨率模式，远距离识别更佳，但会牺牲性能和速度
				const camId = 0; // 0，前置摄像头 1，后置摄像头。否则进入兼容模式（部分摄像头需适配）

				startFaceSearch(
					threshold,
					oneTime,
					highRes,
					camId,
					(jsonStr) => {
						// 这里会持续不断地收到json list Str回调，按照降序排列好了 
						console.log("收到搜索结果:", jsonStr);
						this.faceAIResult = "【人脸搜索回调】\n" + jsonStr;

						// 2. 如果需要处理数据，建议包裹 try-catch 解析 JSON
						// try {
						//    const results = JSON.parse(jsonStr);
						//    if(results.length > 0) {
						//       // 可以在这里做业务逻辑，比如签到、开门等
						//    }
						// } catch(e) { console.error(e) }
					}
				);
			},

			/**
			 * 人脸搜索人脸特征录入，通过相机
			 */
			addFaceSearchFeatureDemo: function() {
				addFaceSearchFeature(
					this.faceID,
					1, // 1.快速模式 2.精确模式
					(result) => {
						// 录入的人脸
						this.faceAIResult = JSON.stringify(result)
					})
			},

			/**
			 * 删除人脸搜索人脸特征
			 */
			deleteFaceSearchFeatureDemo: function() {
				deleteFaceSearchFeature(
					this.faceID,
					(result) => {
						this.faceAIResult = JSON.stringify(result)
					})
			},

			/**
			 * 人脸搜索人脸特征更新同步
			 */
			insertFaceSearchFeatureDemo: function() {
				insertFaceSearchFeature(
					this.faceID,
					this.faceFeature,
					"tag",
					"group",
					(result) => {
						// 录入的人脸
						this.faceAIResult = JSON.stringify(result)
					})
			},

			/**
			 * 批量操作人脸搜索人脸特征更新同步
			 */
			insertManyFaceFeatureSDemo: function() {
				insertManyFeatures(
					JSON_FACE_FEATURES_DATA,
					(result) => {
						this.faceAIResult = JSON.stringify(result)
					})
			}
		}
	}
</script>

<style>
	/* 给滚动区域一个固定高度和边框 */
	.result-box {
		margin: 20rpx;
	}

	.scroll-view-box {
		height: 400rpx;
		/* 必须指定高度，否则无法滚动 */
		border: 1px solid #ccc;
		border-radius: 10rpx;
		background-color: #f8f8f8;
		padding: 15rpx;
		box-sizing: border-box;
		/* 确保padding不撑大宽高 */
	}

	.text-content {
		font-size: 28rpx;
		color: #333;
		/* word-break: break-all; 关键：解决长JSON字符串不换行的问题 */
		white-space: pre-wrap;
		/* 保留格式并自动换行 */
	}

	.gray-button {
		background-color: #ffffff;
		/* 灰色背景 */
		color: #800080;
		/* 深灰色文字 */
		border: none;
	}
</style>