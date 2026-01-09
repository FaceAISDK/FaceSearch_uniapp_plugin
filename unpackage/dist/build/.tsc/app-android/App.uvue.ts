

	let firstBackTime = 0

	const __sfc__ = defineApp({
		onLaunch() {
			console.log('App Launch')
		},
		onShow() {
			console.log('App Show')
		},
		onHide() {
			console.log('App Hide')
		},

		onLastPageBackPress() {
			console.log('App LastPageBackPress')
			if (firstBackTime == 0) {
				uni.showToast({
					title: '再按一次退出应用',
					position: 'bottom',
				})
				firstBackTime = Date.now()
				setTimeout(() => {
					firstBackTime = 0
				}, 2000)
			} else if (Date.now() - firstBackTime < 2000) {
				firstBackTime = Date.now()
				uni.exit()
			}
		},

		onExit() {
			console.log('App Exit')
		},
	})

export default __sfc__
const GenAppStyles = [_uM([["uni-row", _pS(_uM([["flexDirection", "row"]]))], ["uni-column", _pS(_uM([["flexDirection", "column"]]))]])]
