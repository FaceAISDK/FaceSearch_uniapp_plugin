! function(t) {
	var e = {};

	function n(i) {
		if (e[i]) return e[i].exports;
		var o = e[i] = {
			i: i,
			l: !1,
			exports: {}
		};
		return t[i].call(o.exports, o, o.exports, n), o.l = !0, o.exports
	}
	n.m = t, n.c = e, n.d = function(t, e, i) {
		n.o(t, e) || Object.defineProperty(t, e, {
			enumerable: !0,
			get: i
		})
	}, n.r = function(t) {
		"undefined" != typeof Symbol && Symbol.toStringTag && Object.defineProperty(t, Symbol.toStringTag, {
			value: "Module"
		}), Object.defineProperty(t, "__esModule", {
			value: !0
		})
	}, n.t = function(t, e) {
		if (1 & e && (t = n(t)), 8 & e) return t;
		if (4 & e && "object" == typeof t && t && t.__esModule) return t;
		var i = Object.create(null);
		if (n.r(i), Object.defineProperty(i, "default", {
			enumerable: !0,
			value: t
		}), 2 & e && "string" != typeof t)
			for (var o in t) n.d(i, o, function(e) {
				return t[e]
			}.bind(null, o));
		return i
	}, n.n = function(t) {
		var e = t && t.__esModule ? function() {
			return t.default
		} : function() {
			return t
		};
		return n.d(e, "a", e), e
	}, n.o = function(t, e) {
		return Object.prototype.hasOwnProperty.call(t, e)
	}, n.p = "", n(n.s = 19)
}([function(t, e) {
	t.exports = {
		"uni-icon": {
			fontFamily: "uniicons",
			fontWeight: "normal"
		},
		"uni-bg-red": {
			backgroundColor: "#F76260",
			color: "#FFFFFF"
		},
		"uni-bg-green": {
			backgroundColor: "#09BB07",
			color: "#FFFFFF"
		},
		"uni-bg-blue": {
			backgroundColor: "#007AFF",
			color: "#FFFFFF"
		},
		"uni-container": {
			flex: 1,
			paddingTop: "15",
			paddingRight: "15",
			paddingBottom: "15",
			paddingLeft: "15",
			backgroundColor: "#f8f8f8"
		},
		"uni-padding-lr": {
			paddingLeft: "15",
			paddingRight: "15"
		},
		"uni-padding-tb": {
			paddingTop: "15",
			paddingBottom: "15"
		},
		"uni-header-logo": {
			paddingTop: "15",
			paddingRight: "15",
			paddingBottom: "15",
			paddingLeft: "15",
			flexDirection: "column",
			justifyContent: "center",
			alignItems: "center",
			marginTop: "10upx"
		},
		"uni-header-image": {
			width: "80",
			height: "80"
		},
		"uni-hello-text": {
			marginBottom: "20"
		},
		"hello-text": {
			color: "#7A7E83",
			fontSize: "14",
			lineHeight: "20"
		},
		"hello-link": {
			color: "#7A7E83",
			fontSize: "14",
			lineHeight: "20"
		},
		"uni-panel": {
			marginBottom: "12"
		},
		"uni-panel-h": {
			backgroundColor: "#ffffff",
			flexDirection: "row",
			alignItems: "center",
			paddingTop: "12",
			paddingRight: "12",
			paddingBottom: "12",
			paddingLeft: "12"
		},
		"uni-panel-h-on": {
			backgroundColor: "#f0f0f0"
		},
		"uni-panel-text": {
			flex: 1,
			color: "#000000",
			fontSize: "14",
			fontWeight: "normal"
		},
		"uni-panel-icon": {
			marginLeft: "15",
			color: "#999999",
			fontSize: "14",
			fontWeight: "normal",
			transform: "rotate(0deg)",
			transitionDuration: 0,
			transitionProperty: "transform"
		},
		"@TRANSITION": {
			"uni-panel-icon": {
				duration: 0,
				property: "transform"
			}
		},
		"uni-panel-icon-on": {
			transform: "rotate(180deg)"
		},
		"uni-navigate-item": {
			flexDirection: "row",
			alignItems: "center",
			backgroundColor: "#FFFFFF",
			borderTopStyle: "solid",
			borderTopColor: "#f0f0f0",
			borderTopWidth: "1",
			paddingTop: "12",
			paddingRight: "12",
			paddingBottom: "12",
			paddingLeft: "12",
			"backgroundColor:active": "#f8f8f8"
		},
		"uni-navigate-text": {
			flex: 1,
			color: "#000000",
			fontSize: "14",
			fontWeight: "normal"
		},
		"uni-navigate-icon": {
			marginLeft: "15",
			color: "#999999",
			fontSize: "14",
			fontWeight: "normal"
		},
		"uni-list-cell": {
			position: "relative",
			flexDirection: "row",
			justifyContent: "flex-start",
			alignItems: "center"
		},
		"uni-list-cell-pd": {
			paddingTop: "22upx",
			paddingRight: "30upx",
			paddingBottom: "22upx",
			paddingLeft: "30upx"
		},
		"flex-r": {
			flexDirection: "row"
		},
		"flex-c": {
			flexDirection: "column"
		},
		"a-i-c": {
			alignItems: "center"
		},
		"j-c-c": {
			justifyContent: "center"
		},
		"list-item": {
			flexDirection: "row",
			paddingTop: "10",
			paddingRight: "10",
			paddingBottom: "10",
			paddingLeft: "10"
		}
	}
}, function(t, e, n) {
	"use strict";
	n.r(e), e.default = {
		appid: "__UNI__BCEC007"
	}
}, function(t, e, n) {
	"use strict";

	function i(t, e, n, i, o, a, r, s, u, c) {
		var l, p = "function" == typeof t ? t.options : t;
		if (u) {
			p.components || (p.components = {});
			var f = Object.prototype.hasOwnProperty;
			for (var d in u) f.call(u, d) && !f.call(p.components, d) && (p.components[d] = u[d])
		}
		if (c && ((c.beforeCreate || (c.beforeCreate = []))
			.unshift((function() {
				this[c.__module] = this
			})), (p.mixins || (p.mixins = []))
			.push(c)), e && (p.render = e, p.staticRenderFns = n, p._compiled = !0), i && (p.functional = !0), a && (p._scopeId = "data-v-" + a), r ? (l = function(t) {
			(t = t || this.$vnode && this.$vnode.ssrContext || this.parent && this.parent.$vnode && this.parent.$vnode.ssrContext) || "undefined" == typeof __VUE_SSR_CONTEXT__ || (t = __VUE_SSR_CONTEXT__), o && o.call(this, t), t && t._registeredComponents && t._registeredComponents.add(r)
		}, p._ssrRegister = l) : o && (l = s ? function() {
			o.call(this, this.$root.$options.shadowRoot)
		} : o), l)
			if (p.functional) {
				p._injectStyles = l;
				var g = p.render;
				p.render = function(t, e) {
					return l.call(e), g(t, e)
				}
			} else {
				var h = p.beforeCreate;
				p.beforeCreate = h ? [].concat(h, l) : [l]
			} return {
			exports: t,
			options: p
		}
	}
	n.d(e, "a", (function() {
		return i
	}))
}, , function(t, e, n) {
	"use strict";
	var i = n(10),
		o = n(7),
		a = n(2),
		r = Object(a.a)(o.default, i.b, i.c, !1, null, null, "361e6432", !1, i.a, void 0);
	e.default = r.exports
}, , , function(t, e, n) {
	"use strict";
	var i = n(8),
		o = n.n(i);
	e.default = o.a
}, function(t, e, n) {
	"use strict";
	(function(t) {
		Object.defineProperty(e, "__esModule", {
			value: !0
		}), e.default = void 0;
		var n = t("FaceAISDKModule"),
			i = t("modal"),
			o = {
				onLoad: function() {
					plus.globalEvent.addEventListener("TestEvent", (function(t) {
						i.toast({
							message: "TestEvent收到：" + t.msg,
							duration: 1.5
						})
					}))
				},
				methods: {
					addFaceImage: function() {
						n.addFaceImage({
                                         "faceID": "yourFaceID_uniApp",
                                          "addFacePerformanceMode": 2
                                       }, (function(t) {
							i.toast({
								message: t,
								duration: 1.5
							})
						}))
					},


					faceVerify: function() {
						n.faceVerify({
                                         "faceID": "yourFaceID_uniApp",
                                          "threshold": 0.85,
                                          "silentThreshold": 0.85,
                                          "faceLivenessType": 3,
                                          "motionStepSize": 1,
                                          "verifyTimeOut": 16,
                                          "motionLivenessTypes": "1,2,3,4,5"
                                       }, (function(t) {
							i.toast({
								message: t,
								duration: 1.5
							})
						}))
					},

					livenessVerify: function() {
						n.livenessVerify({
                                          "silentThreshold": 0.85,
                                          "faceLivenessType": 3,
                                          "motionStepSize": 1,
                                          "verifyTimeOut": 11,
                                          "motionLivenessTypes": "1,2,3,4,5"
                                       }, (function(t) {
							i.toast({
								message: t,
								duration: 1.5
							})
						}))
					},

					isFaceExist: function() {
						n.isFaceExist({
                                         "faceID": "yourFaceID_uniApp"
                                       }, (function(t) {
							i.toast({
								message: t,
								duration: 1.5
							})
						}))
					},

					insertFace2SDK: function() {
						n.insertFace2SDK({
                                         "faceID": "yourFaceID_uniApp",
                                         "faceBase64":"data:image/jpeg;base64,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"
                                       }, (function(t) {
							i.toast({
								message: t,
								duration: 1.5
							})
						}))
					},

					gotoAboutFaceAIPage: function() {
						n.gotoAboutFaceAIPage()
					}
				}
			};
		e.default = o
	})
	.call(this, n(20)
		.default)
}, , function(t, e, n) {
	"use strict";
	var i = function() {
			var t = this.$createElement,
				e = this._self._c || t;
			return e("scroll-view", {
				staticStyle: {
					flexDirection: "column"
				},
				attrs: {
					scrollY: !0,
					enableBackToTop: !0,
					bubble: "true"
				}
			}, [e("div", [

			e("button", {
				attrs: {
					type: "primary"
				},
				on: {
					click: this.addFaceImage
				}
			}, [this._v("添加人脸图")]),

			 e("button", {
				attrs: {
					type: "primary"
				},
				on: {
					click: this.faceVerify
				}
			}, [this._v("人脸识别")]),

			 e("button", {
				attrs: {
					type: "primary"
				},
				on: {
					click: this.livenessVerify
				}
			}, [this._v("仅活体检测")]),

			 e("button", {
				attrs: {
					type: "primary"
				},
				on: {
					click: this.isFaceExist
				}
			}, [this._v("检测人脸图是否存在")]),


			 e("button", {
				attrs: {
					type: "primary"
				},
				on: {
					click: this.insertFace2SDK
				}
			}, [this._v("同步远程人脸图Base64")]),

			 e("button", {
				attrs: {
					type: "primary"
				},
				on: {
					click: this.gotoAboutFaceAIPage
				}
			}, [this._v("体验原生FaceAI SDK所有功能")])], 1)])
		},
		o = [];
	n.d(e, "b", (function() {
		return i
	})), n.d(e, "c", (function() {
		return o
	})), n.d(e, "a", (function() {}))
}, function(t, e, n) {
	"use strict";

	function i(t, e) {
		return !e || "object" != typeof e && "function" != typeof e ? function(t) {
			if (void 0 === t) throw new ReferenceError("this hasn't been initialised - super() hasn't been called");
			return t
		}(t) : e
	}

	function o() {
		if ("undefined" == typeof Reflect || !Reflect.construct) return !1;
		if (Reflect.construct.sham) return !1;
		if ("function" == typeof Proxy) return !0;
		try {
			return Date.prototype.toString.call(Reflect.construct(Date, [], (function() {}))), !0
		} catch (t) {
			return !1
		}
	}

	function a(t) {
		return (a = Object.setPrototypeOf ? Object.getPrototypeOf : function(t) {
			return t.__proto__ || Object.getPrototypeOf(t)
		})(t)
	}

	function r(t, e) {
		return (r = Object.setPrototypeOf || function(t, e) {
			return t.__proto__ = e, t
		})(t, e)
	}

	function s(t, e) {
		if (!(t instanceof e)) throw new TypeError("Cannot call a class as a function")
	}

	function u(t, e) {
		for (var n = 0; n < e.length; n++) {
			var i = e[n];
			i.enumerable = i.enumerable || !1, i.configurable = !0, "value" in i && (i.writable = !0), Object.defineProperty(t, i.key, i)
		}
	}

	function c(t, e, n) {
		return e && u(t.prototype, e), n && u(t, n), t
	}
	var l = n(12)
		.version;

	function p() {
		var t = "";
		if ("n" === d()) {
			try {
				t = plus.runtime.getDCloudId()
			} catch (e) {
				t = ""
			}
			return t
		}
		try {
			t = uni.getStorageSync("__DC_STAT_UUID")
		} catch (e) {
			t = "__DC_UUID_VALUE"
		}
		if (!t) {
			t = Date.now() + "" + Math.floor(1e7 * Math.random());
			try {
				uni.setStorageSync("__DC_STAT_UUID", t)
			} catch (t) {
				uni.setStorageSync("__DC_STAT_UUID", "__DC_UUID_VALUE")
			}
		}
		return t
	}
	var f = function() {
			return parseInt((new Date)
				.getTime() / 1e3)
		},
		d = function() {
			return "n"
		},
		g = 0,
		h = 0,
		_ = function() {
			return g = f(), "n" === d() && uni.setStorageSync("__page__residence__time", f()), g
		},
		v = 0,
		y = 0,
		m = function() {
			var t = (new Date)
				.getTime();
			return v = t, y = 0, t
		},
		S = function() {
			var t = (new Date)
				.getTime();
			return y = t, t
		},
		b = function(t) {
			var e = 0;
			return 0 !== v && (e = y - v), e = (e = parseInt(e / 1e3)) < 1 ? 1 : e, "app" === t ? {
				residenceTime: e,
				overtime: e > 300
			} : "page" === t ? {
				residenceTime: e,
				overtime: e > 1800
			} : {
				residenceTime: e
			}
		},
		D = function(t) {
			var e = getCurrentPages(),
				n = e[e.length - 1].$vm,
				i = t._query,
				o = i && "{}" !== JSON.stringify(i) ? "?" + JSON.stringify(i) : "";
			return t._query = "", "bd" === d() ? n.$mp && n.$mp.page.is + o : n.$scope && n.$scope.route + o || n.$mp && n.$mp.page.route + o
		},
		T = function(t) {
			return !!("page" === t.mpType || t.$mp && "page" === t.$mp.mpType || "page" === t.$options.mpType)
		},
		k = n(13)
		.default,
		w = n(1)
		.default || n(1),
		R = uni.getSystemInfoSync(),
		q = function(t) {
			! function(t, e) {
				if ("function" != typeof e && null !== e) throw new TypeError("Super expression must either be null or a function");
				t.prototype = Object.create(e && e.prototype, {
					constructor: {
						value: t,
						writable: !0,
						configurable: !0
					}
				}), e && r(t, e)
			}(u, t);
			var e, n = (e = u, function() {
				var t, n = a(e);
				if (o()) {
					var r = a(this)
						.constructor;
					t = Reflect.construct(n, arguments, r)
				} else t = n.apply(this, arguments);
				return i(this, t)
			});

			function u() {
				var t;
				return s(this, u), (t = n.call(this))
					.instance = null, "function" == typeof uni.addInterceptor && (t.addInterceptorInit(), t.interceptLogin(), t.interceptShare(!0), t.interceptRequestPayment()), t
			}
			return c(u, null, [{
				key: "getInstance",
				value: function() {
					return this.instance || (this.instance = new u), this.instance
				}
			}]), c(u, [{
				key: "addInterceptorInit",
				value: function() {
					var t = this;
					uni.addInterceptor("setNavigationBarTitle", {
						invoke: function(e) {
							t._navigationBarTitle.page = e.title
						}
					})
				}
			}, {
				key: "interceptLogin",
				value: function() {
					var t = this;
					uni.addInterceptor("login", {
						complete: function() {
							t._login()
						}
					})
				}
			}, {
				key: "interceptShare",
				value: function(t) {
					var e = this;
					t ? uni.addInterceptor("share", {
						success: function() {
							e._share()
						},
						fail: function() {
							e._share()
						}
					}) : e._share()
				}
			}, {
				key: "interceptRequestPayment",
				value: function() {
					var t = this;
					uni.addInterceptor("requestPayment", {
						success: function() {
							t._payment("pay_success")
						},
						fail: function() {
							t._payment("pay_fail")
						}
					})
				}
			}, {
				key: "report",
				value: function(t, e) {
					this.self = e, _(), this.__licationShow = !0, this._sendReportRequest(t, !0)
				}
			}, {
				key: "load",
				value: function(t, e) {
					if (!e.$scope && !e.$mp) {
						var n = getCurrentPages();
						e.$scope = n[n.length - 1]
					}
					this.self = e, this._query = t
				}
			}, {
				key: "show",
				value: function(t) {
					this.self = t, T(t) ? this._pageShow(t) : this._applicationShow(t)
				}
			}, {
				key: "ready",
				value: function(t) {}
			}, {
				key: "hide",
				value: function(t) {
					this.self = t, T(t) ? this._pageHide(t) : this._applicationHide(t, !0)
				}
			}, {
				key: "error",
				value: function(t) {
					this._platform;
					var e = "";
					e = t.message ? t.stack : JSON.stringify(t);
					var n = {
						ak: this.statData.ak,
						uuid: this.statData.uuid,
						lt: "31",
						ut: this.statData.ut,
						ch: this.statData.ch,
						mpsdk: this.statData.mpsdk,
						mpv: this.statData.mpv,
						v: this.statData.v,
						em: e,
						usv: this.statData.usv,
						t: f(),
						p: this.statData.p
					};
					this.request(n)
				}
			}]), u
		}(function() {
			function t() {
				var e, n;
				s(this, t), this.self = "", this._retry = 0, this._platform = "", this._query = {}, this._navigationBarTitle = {
					config: "",
					page: "",
					report: "",
					lt: ""
				}, this._operatingTime = 0, this._reportingRequestData = {
					1: [],
					11: []
				}, this.__prevent_triggering = !1, this.__licationHide = !1, this.__licationShow = !1, this._lastPageRoute = "", this.statData = {
					uuid: p(),
					ut: d(),
					mpn: (n = "", "wx" !== d() && "qq" !== d() || uni.canIUse("getAccountInfoSync") && (n = uni.getAccountInfoSync()
						.miniProgram.appId || ""), n),
					ak: w.appid,
					usv: l,
					v: "n" === d() ? plus.runtime.version : "",
					ch: (e = "", "n" === d() && (e = plus.runtime.channel), e),
					cn: "",
					pn: "",
					ct: "",
					t: f(),
					tt: "",
					p: "android" === R.platform ? "a" : "i",
					brand: R.brand || "",
					md: R.model,
					sv: R.system.replace(/(Android|iOS)\s/, ""),
					mpsdk: R.SDKVersion || "",
					mpv: R.version || "",
					lang: R.language,
					pr: R.pixelRatio,
					ww: R.windowWidth,
					wh: R.windowHeight,
					sw: R.screenWidth,
					sh: R.screenHeight
				}
			}
			return c(t, [{
				key: "_applicationShow",
				value: function() {
					if (this.__licationHide) {
						if (S(), b("app")
							.overtime) {
							var t = {
								path: this._lastPageRoute,
								scene: this.statData.sc
							};
							this._sendReportRequest(t)
						}
						this.__licationHide = !1
					}
				}
			}, {
				key: "_applicationHide",
				value: function(t, e) {
					this.__licationHide = !0, S();
					var n = b();
					m();
					var i = D(this);
					this._sendHideRequest({
						urlref: i,
						urlref_ts: n.residenceTime
					}, e)
				}
			}, {
				key: "_pageShow",
				value: function() {
					var t, e, n = D(this),
						i = (t = getCurrentPages(), e = t[t.length - 1].$vm, "bd" === d() ? e.$mp && e.$mp.page.is : e.$scope && e.$scope.route || e.$mp && e.$mp.page.route);
					if (this._navigationBarTitle.config = k && k.pages[i] && k.pages[i].titleNView && k.pages[i].titleNView.titleText || k && k.pages[i] && k.pages[i].navigationBarTitleText || "", this.__licationShow) return m(), this.__licationShow = !1, void(this._lastPageRoute = n);
					if (S(), this._lastPageRoute = n, b("page")
						.overtime) {
						var o = {
							path: this._lastPageRoute,
							scene: this.statData.sc
						};
						this._sendReportRequest(o)
					}
					m()
				}
			}, {
				key: "_pageHide",
				value: function() {
					if (!this.__licationHide) {
						S();
						var t = b("page");
						return this._sendPageRequest({
							url: this._lastPageRoute,
							urlref: this._lastPageRoute,
							urlref_ts: t.residenceTime
						}), void(this._navigationBarTitle = {
							config: "",
							page: "",
							report: "",
							lt: ""
						})
					}
				}
			}, {
				key: "_login",
				value: function() {
					this._sendEventRequest({
						key: "login"
					}, 0)
				}
			}, {
				key: "_share",
				value: function() {
					this._sendEventRequest({
						key: "share"
					}, 0)
				}
			}, {
				key: "_payment",
				value: function(t) {
					this._sendEventRequest({
						key: t
					}, 0)
				}
			}, {
				key: "_sendReportRequest",
				value: function(t) {
					this._navigationBarTitle.lt = "1";
					var e, n, i = t.query && "{}" !== JSON.stringify(t.query) ? "?" + JSON.stringify(t.query) : "";
					this.statData.lt = "1", this.statData.url = t.path + i || "", this.statData.t = f(), this.statData.sc = function(t) {
						var e = d(),
							n = "";
						return t || ("wx" === e && (n = uni.getLaunchOptionsSync()
							.scene), n)
					}(t.scene), this.statData.fvts = (e = uni.getStorageSync("First__Visit__Time"), n = 0, e ? n = e : (n = f(), uni.setStorageSync("First__Visit__Time", n), uni.removeStorageSync("Last__Visit__Time")), n), this.statData.lvts = function() {
						var t = uni.getStorageSync("Last__Visit__Time"),
							e = 0;
						return e = t || "", uni.setStorageSync("Last__Visit__Time", f()), e
					}(), this.statData.tvc = function() {
						var t = uni.getStorageSync("Total__Visit__Count"),
							e = 1;
						return t && (e = t, e++), uni.setStorageSync("Total__Visit__Count", e), e
					}(), "n" === d() ? this.getProperty() : this.getNetworkInfo()
				}
			}, {
				key: "_sendPageRequest",
				value: function(t) {
					var e = t.url,
						n = t.urlref,
						i = t.urlref_ts;
					this._navigationBarTitle.lt = "11";
					var o = {
						ak: this.statData.ak,
						uuid: this.statData.uuid,
						lt: "11",
						ut: this.statData.ut,
						url: e,
						tt: this.statData.tt,
						urlref: n,
						urlref_ts: i,
						ch: this.statData.ch,
						usv: this.statData.usv,
						t: f(),
						p: this.statData.p
					};
					this.request(o)
				}
			}, {
				key: "_sendHideRequest",
				value: function(t, e) {
					var n = t.urlref,
						i = t.urlref_ts,
						o = {
							ak: this.statData.ak,
							uuid: this.statData.uuid,
							lt: "3",
							ut: this.statData.ut,
							urlref: n,
							urlref_ts: i,
							ch: this.statData.ch,
							usv: this.statData.usv,
							t: f(),
							p: this.statData.p
						};
					this.request(o, e)
				}
			}, {
				key: "_sendEventRequest",
				value: function() {
					var t = arguments.length > 0 && void 0 !== arguments[0] ? arguments[0] : {},
						e = t.key,
						n = void 0 === e ? "" : e,
						i = t.value,
						o = void 0 === i ? "" : i,
						a = this._lastPageRoute,
						r = {
							ak: this.statData.ak,
							uuid: this.statData.uuid,
							lt: "21",
							ut: this.statData.ut,
							url: a,
							ch: this.statData.ch,
							e_n: n,
							e_v: "object" == typeof o ? JSON.stringify(o) : o.toString(),
							usv: this.statData.usv,
							t: f(),
							p: this.statData.p
						};
					this.request(r)
				}
			}, {
				key: "getNetworkInfo",
				value: function() {
					var t = this;
					uni.getNetworkType({
						success: function(e) {
							t.statData.net = e.networkType, t.getLocation()
						}
					})
				}
			}, {
				key: "getProperty",
				value: function() {
					var t = this;
					plus.runtime.getProperty(plus.runtime.appid, (function(e) {
						t.statData.v = e.version || "", t.getNetworkInfo()
					}))
				}
			}, {
				key: "getLocation",
				value: function() {
					var t = this;
					w.getLocation ? uni.getLocation({
						type: "wgs84",
						geocode: !0,
						success: function(e) {
							e.address && (t.statData.cn = e.address.country, t.statData.pn = e.address.province, t.statData.ct = e.address.city), t.statData.lat = e.latitude, t.statData.lng = e.longitude, t.request(t.statData)
						}
					}) : (this.statData.lat = 0, this.statData.lng = 0, this.request(this.statData))
				}
			}, {
				key: "request",
				value: function(t, e) {
					var n = this,
						i = f(),
						o = this._navigationBarTitle;
					t.ttn = o.page, t.ttpj = o.config, t.ttc = o.report;
					var a = this._reportingRequestData;
					if ("n" === d() && (a = uni.getStorageSync("__UNI__STAT__DATA") || {}), a[t.lt] || (a[t.lt] = []), a[t.lt].push(t), "n" === d() && uni.setStorageSync("__UNI__STAT__DATA", a), h = f(), "n" === d() && (g = uni.getStorageSync("__page__residence__time")), !(h - g < 10) || e) {
						var r = this._reportingRequestData;
						"n" === d() && (r = uni.getStorageSync("__UNI__STAT__DATA")), _();
						var s = [],
							u = [],
							c = [],
							p = function(t) {
								r[t].forEach((function(e) {
									var n = function(t) {
										var e = "";
										for (var n in t) e += n + "=" + t[n] + "&";
										return e.substr(0, e.length - 1)
									}(e);
									0 === t ? s.push(n) : 3 === t ? c.push(n) : u.push(n)
								}))
							};
						for (var v in r) p(v);
						s.push.apply(s, u.concat(c));
						var y = {
							usv: l,
							t: i,
							requests: JSON.stringify(s)
						};
						this._reportingRequestData = {}, "n" === d() && uni.removeStorageSync("__UNI__STAT__DATA"), "h5" !== t.ut ? "n" !== d() || "a" !== this.statData.p ? this._sendRequest(y) : setTimeout((function() {
							n._sendRequest(y)
						}), 200) : this.imageRequest(y)
					}
				}
			}, {
				key: "_sendRequest",
				value: function(t) {
					var e = this;
					uni.request({
						url: "https://tongji.dcloud.io/uni/stat",
						method: "POST",
						data: t,
						success: function() {},
						fail: function(n) {
							++e._retry < 3 && setTimeout((function() {
								e._sendRequest(t)
							}), 1e3)
						}
					})
				}
			}, {
				key: "imageRequest",
				value: function(t) {
					var e = new Image,
						n = function(t) {
							var e = Object.keys(t)
								.sort(),
								n = {},
								i = "";
							for (var o in e) n[e[o]] = t[e[o]], i += e[o] + "=" + t[e[o]] + "&";
							return {
								sign: "",
								options: i.substr(0, i.length - 1)
							}
						}(function(t) {
							var e = {};
							for (var n in t) e[n] = encodeURIComponent(t[n]);
							return e
						}(t))
						.options;
					e.src = "https://tongji.dcloud.io/uni/stat.gif?" + n
				}
			}, {
				key: "sendEvent",
				value: function(t, e) {
					var n, i;
					(i = e, (n = t) ? "string" != typeof n ? (console.error("uni.report [eventName] 参数类型错误,只能为 String 类型"), 1) : n.length > 255 ? (console.error("uni.report [eventName] 参数长度不能大于 255"), 1) : "string" != typeof i && "object" != typeof i ? (console.error("uni.report [options] 参数类型错误,只能为 String 或 Object 类型"), 1) : "string" == typeof i && i.length > 255 ? (console.error("uni.report [options] 参数长度不能大于 255"), 1) : "title" === n && "string" != typeof i ? (console.error("uni.report [eventName] 参数为 title 时，[options] 参数只能为 String 类型"), 1) : void 0 : (console.error("uni.report 缺少 [eventName] 参数"), 1)) || ("title" !== t ? this._sendEventRequest({
						key: t,
						value: "object" == typeof e ? JSON.stringify(e) : e
					}, 1) : this._navigationBarTitle.report = e)
				}
			}]), t
		}())
		.getInstance(),
		x = !1,
		I = {
			onLaunch: function(t) {
				q.report(t, this)
			},
			onReady: function() {
				q.ready(this)
			},
			onLoad: function(t) {
				if (q.load(t, this), this.$scope && this.$scope.onShareAppMessage) {
					var e = this.$scope.onShareAppMessage;
					this.$scope.onShareAppMessage = function(t) {
						return q.interceptShare(!1), e.call(this, t)
					}
				}
			},
			onShow: function() {
				x = !1, q.show(this)
			},
			onHide: function() {
				x = !0, q.hide(this)
			},
			onUnload: function() {
				x ? x = !1 : q.hide(this)
			},
			onError: function(t) {
				q.error(t)
			}
		};
	! function() {
		var t = n(14);
		(t.default || t)
		.mixin(I), uni.report = function(t, e) {
			q.sendEvent(t, e)
		}
	}()
}, function(t) {
	t.exports = {
		_from: "@dcloudio/uni-stat@alpha",
		_id: "@dcloudio/uni-stat@2.0.0-alpha-26920200411001",
		_inBundle: !1,
		_integrity: "sha512-cUEIJ3It6EFJT0kIyVgcRMgNa2UBm+4autJWCuzggwnlOr6JdM3/ZVntz1YdwXFTEoKDWi8GLF7wWsZ2vwE0UA==",
		_location: "/@dcloudio/uni-stat",
		_phantomChildren: {},
		_requested: {
			type: "tag",
			registry: !0,
			raw: "@dcloudio/uni-stat@alpha",
			name: "@dcloudio/uni-stat",
			escapedName: "@dcloudio%2funi-stat",
			scope: "@dcloudio",
			rawSpec: "alpha",
			saveSpec: null,
			fetchSpec: "alpha"
		},
		_requiredBy: ["#USER", "/", "/@dcloudio/vue-cli-plugin-uni"],
		_resolved: "https://registry.npmjs.org/@dcloudio/uni-stat/-/uni-stat-2.0.0-alpha-26920200411001.tgz",
		_shasum: "dfab45e371df540c477880ae512b82546ac6e6d0",
		_spec: "@dcloudio/uni-stat@alpha",
		_where: "/Users/guoshengqiang/Documents/dcloud-plugins/alpha/uniapp-cli",
		author: "",
		bugs: {
			url: "https://github.com/dcloudio/uni-app/issues"
		},
		bundleDependencies: !1,
		deprecated: !1,
		description: "",
		devDependencies: {
			"@babel/core": "^7.5.5",
			"@babel/preset-env": "^7.5.5",
			eslint: "^6.1.0",
			rollup: "^1.19.3",
			"rollup-plugin-babel": "^4.3.3",
			"rollup-plugin-clear": "^2.0.7",
			"rollup-plugin-commonjs": "^10.0.2",
			"rollup-plugin-copy": "^3.1.0",
			"rollup-plugin-eslint": "^7.0.0",
			"rollup-plugin-json": "^4.0.0",
			"rollup-plugin-node-resolve": "^5.2.0",
			"rollup-plugin-replace": "^2.2.0",
			"rollup-plugin-uglify": "^6.0.2"
		},
		files: ["dist", "package.json", "LICENSE"],
		gitHead: "6090d656df76642953b405984451ec4a76bae2f5",
		homepage: "https://github.com/dcloudio/uni-app#readme",
		license: "Apache-2.0",
		main: "dist/index.js",
		name: "@dcloudio/uni-stat",
		repository: {
			type: "git",
			url: "git+https://github.com/dcloudio/uni-app.git",
			directory: "packages/uni-stat"
		},
		scripts: {
			build: "NODE_ENV=production rollup -c rollup.config.js",
			dev: "NODE_ENV=development rollup -w -c rollup.config.js"
		},
		version: "2.0.0-alpha-26920200411001"
	}
}, function(t, e, n) {
	"use strict";
	n.r(e), e.default = {
		pages: {},
		globalStyle: {}
	}
}, function(t, e) {
	t.exports = Vue
}, function(t, e, n) {
	Vue.prototype.__$appStyle__ = {}, Vue.prototype.__merge_style && Vue.prototype.__merge_style(n(16)
		.default, Vue.prototype.__$appStyle__)
}, function(t, e, n) {
	"use strict";
	n.r(e);
	var i = n(0),
		o = n.n(i);
	for (var a in i) "default" !== a && function(t) {
		n.d(e, t, (function() {
			return i[t]
		}))
	}(a);
	e.default = o.a
}, , , function(t, e, n) {
	"use strict";
	n.r(e);
	n(11), n(15);
	var i = n(4);
	i.default.mpType = "page", i.default.route = "pages/sample/ext-module", i.default.el = "#root", new Vue(i.default)
}, function(t, e, n) {
	"use strict";
	Object.defineProperty(e, "__esModule", {
		value: !0
	}), e.default = function(t) {
		return weex.requireModule(t)
	}
}]);