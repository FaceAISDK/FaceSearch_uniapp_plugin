<template>
	<view>
		<button class="gray-button" @tap="startFaceSearchDemo">打开持续人脸搜索</button>
		<button class="gray-button" @tap="faceSearchDemo">1:N人脸搜索识别</button> 
		<button class="gray-button" @tap="addFaceSearchFeatureDemo">1:N人脸搜索录入人脸</button>
		<button class="gray-button" @tap="deleteFaceSearchFeatureDemo">删除人脸搜索特征值</button>
		<button class="gray-button" @tap="insertFaceSearchFeatureDemo">同步人脸搜索特征值</button>
		<button class="gray-button" @tap="insertFaceSearchFeatureSDemo">批量同步人脸搜索特征值</button>
		
		<view class="result-box">
		      <view> Email: FaceAISDK.Service@gmail.com</view>
		       <scroll-view scroll-y="true" class="scroll-view-box">
		       <text class="text-content">{{faceAIResult}}</text>
		       </scroll-view>
		</view>
	</view>
</template>

<script> 
	import {
		startFaceSearch,
		insertFaceSearchFeature,
		insertManyFeatures,
		addFaceSearchFeature,
		deleteFaceSearchFeature,
		faceSearch
	} from "@/uni_modules/FaceAI-Search";
	
	// 引入模拟数据，注意后缀改为了 .js
	import { JSON_FACE_FEATURES_DATA } from "./testData.js"; 
	
	export default {
		data() {
			return {
				faceID: 'Test',
				faceFeature: 'faceFeature is a string with lenth 1024',
				faceJSONFeatures: 'faceFeature is a string with lenth 1024',
				faceAIResult: 'faceAIResult'
			}
		},
		onLoad() {

		},
		methods: {
			/**
			 * 人脸搜索
			 */
			startFaceSearchDemo: function () {
				// 移除 (jsonStr: string) 类型注解，改为普通 JS 参数
			    startFaceSearch((jsonStr) => {
			        // 这里会持续不断地收到回调
			        console.log("收到搜索结果:", jsonStr);
			        
			        // 1. 简单展示，已经按照搜索匹配度降序排列
			        this.faceAIResult = "【人脸搜索回调】\n" + jsonStr;
			        
			        [cite_start]// 2. 如果需要处理数据，建议包裹 try-catch 解析 JSON [cite: 21]
			        /*
			        try {
			           const results = JSON.parse(jsonStr);
			           if(results.length > 0) {
			              // 可以在这里做业务逻辑，比如签到、开门等
			           }
			        } catch(e) { console.error(e) }
			        */
			    });
			},

			/**
			* 人脸搜索，插件beta版本
			*/
			faceSearchDemo: function () {
				faceSearch((result) => {
					this.faceAIResult = JSON.stringify(result)
				})
			},
			
			/**
			* 人脸搜索人脸特征录入，通过相机
			*/
			addFaceSearchFeatureDemo: function () {
				addFaceSearchFeature(
				     this.faceID,
					 1,           // 1.快速模式 2.精确模式
					 (result) => {
						// 录入的人脸
						this.faceAIResult = JSON.stringify(result)
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
					})
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
						// 录入的人脸
						this.faceAIResult = JSON.stringify(result)
					})
			},
			
			/**
			* 批量操作人脸搜索人脸特征更新同步
			*/
			insertFaceSearchFeatureSDemo: function () {
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
		color: #fc0280;
		/* word-break: break-all; 关键：解决长JSON字符串不换行的问题 */
		white-space: pre-wrap;
		/* 保留格式并自动换行 */
	}
</style>

<style>
	.gray-button {
		background-color: #ffffff;
		/* 灰色背景 */
		color: #800080;
		/* 深灰色文字 */
		/* 如果需要，可以覆盖默认的边框 */
		border: none;
	}
</style>