
new Vue({
    el: '#root',
    data: {
        serve_url: "http://pkpk.run:8083", // pkpk.run   127.0.0.1
        element: document.documentElement,
        dialogWidth: 35,
        inputUid: '',

        // 添加话框
        dialogAddUserVisible: false,
        formAddUser: {
            cookie: '',
            password: ''
        },
        dialogAddLoading: false,
        dialogAddSuccessVisible: false,


        // 密码对话框
        dialogPwsVisible: false,
        dialogPwsInputPws: '',
        dialogPwsLoading: false,
        onPwsClick: 0,

        // 更新对话框
        dialogReplaceVisible: false,
        formReplaceUser: {
            cookie: '',
            password: ''
        },
        dialogReplaceLoading: false,

        // 解绑
        dialogUnbindVisible: false,
        dialogUnbindLoading: false,

        // 查询
        dialogInquireVisible: false,
        checkAll: false,
        checkedCities: ['失败的', '成功的'],
        radio: false,
        successNum: 0,
        tableData: [],



        addStr: "添加", replaceStr: "更新", inquireStr: "查询", unbindStr: "解除",
        addLoading: false, replaceLoading: false, inquireLoading: false, unbindLoading: false,


        gameData: {
            game_uid: '',
            level: 0,
            nickname: '',
            region_name: '',
            ranking: 0
        },
        timer: null,
    },
    mounted() {
        let _this = this
        this.windowDialogAdjustment()
        window.addEventListener('mousewheel', this.handleScroll, false)
        window.addEventListener('DOMMouseScroll', this.handleScroll, false)
        window.addEventListener('scroll', this.handleScroll, true)
        window.addEventListener('resize', this.windowDialogAdjustment)
    },

    methods: {
        /*提示*/
        warning: function (msg, title = "失败") {

            if (this.element.clientWidth < 992) {
                this.$message.warning({
                    message: msg, center: true
                });
                return
            }
            this.$notify.warning({
                title: title, message: msg, position: 'bottom-right'
            });
        },
        success: function (msg, title = "成功") {
            if (this.element.clientWidth < 992) {
                this.$message.success({
                    message: msg, center: true
                });
                return
            }
            this.$notify.success({
                title: title, message: msg, position: 'bottom-right'
            });
        },
        error: function (msg, title = "错误") {
            if (this.element.clientWidth < 992) {
                this.$message.error({
                    message: msg, center: true
                });
                return
            }
            this.$notify.error({
                title: title, message: msg, position: 'bottom-right'
            });
        },

        // 添加
        addUser() {
            let _this = this
            if (this.isInputUid()) {
                _this.warning("uid 有误")
                return
            }
            _this.addLoading = true
            this.getUrl(`/genshin-api/inquire-user/${this.inputUid}`, function (data) {
                console.log(data)
                _this.addLoading = false
                if (data.result.isExist) {
                    _this.warning(data.result.msg)
                } else {
                    _this.dialogAddUserVisible = true
                }
            }, function (e) {
                _this.addLoading = false
                console.log(e)
                _this.error(e.reason == null ? "服务器出错" : e.reason)
            })
        },

        // 更新
        replaceUser() {
            let _this = this
            _this.replaceLoading = true
            this.queryUserPublicData(function (data) {
                _this.onPwsClick = 0
                _this.replaceLoading = false
            }, function (data) {
                _this.inquireBaseUserInfo()
            }, function (data) {
                _this.replaceLoading = false
            }, function (data) {
                _this.replaceLoading = false
            })
        },

        inquireUser() {
            let _this = this
            _this.inquireLoading = true
            this.queryUserPublicData(function (_) {
                // 用户需要输入密码
                _this.onPwsClick = 1
                _this.inquireLoading = false
            }, function (_) {
                // 直接查询数据
                _this.inquireUserRecordInfo()
            }, function (_) {
                _this.inquireLoading = false
            }, function (_) {
                _this.inquireLoading = false
            })
        },

        unbindUser() {
            // 解绑
            let _this = this
            if (this.isInputUid()) {
                _this.warning("uid 有误")
                return
            }
            _this.unbindLoading = true
            this.getUrl(`/genshin-api/inquire-user/${this.inputUid}`, function (data) {
                _this.unbindLoading = false
                if (data.result.isExist) {
                    if (data.result.isPws) {
                        // 有密码
                        _this.onPwsClick = 2
                        _this.dialogPwsVisible = true
                    } else {
                        // 无密码
                        _this.dialogUnbindVisible = true
                    }
                } else {
                    _this.warning(data.result.msg)
                }
            }, function (e) {
                _this.unbindLoading = false
                console.log(e)
                _this.error(e.reason == null ? "服务器出错" : e.reason)
            })
        },




        // 添加cookie
        addCookieEfficient() {
            let _this = this
            if (this.formAddUser.cookie == null || this.formAddUser.cookie.length <= 0) {
                _this.warning("cookie 不能为空")
                return
            }
            this.dialogAddLoading = true

            let param = new FormData()
            param.append('uid', this.inputUid)
            param.append('cookie', this.formAddUser.cookie)
            param.append('password', this.formAddUser.password)


            this.postUrl('/genshin-api/add-user', param, function (data) {
                // 添加成功
                // console.log(data)
                _this.dialogAddLoading = false
                _this.dialogAddSuccessVisible = true
                _this.dialogAddUserVisible = false
                _this.formAddUser.password = ''
                _this.formAddUser.cookie = ''


                _this.gameData.region_name = data.result.region_name
                _this.gameData.level = data.result.level
                _this.gameData.game_uid = data.result.game_uid
                _this.gameData.nickname = data.result.nickname
                _this.gameData.ranking = data.result.ranking

                _this.inputUid = data.result.game_uid
                // _this.success(data.reason)
            }, function (e) {
                _this.dialogAddLoading = false
                // 添加失败
                // console.log(e)
                _this.warning(e.reason == null ? "服务器出错" : e.reason)
            })

        },

        // 查询用户基础数据
        inquireBaseUserInfo(password = "") {
            let _this = this
            this.getUrl(`/genshin-api/user-info?uid=${_this.inputUid}&password=${password}`, function (data) {
                _this.replaceLoading = false
                _this.dialogReplaceVisible = true
                _this.dialogPwsVisible = false
                _this.dialogPwsLoading = false

                _this.formReplaceUser.cookie = data.result.cookie
                _this.formReplaceUser.password = data.result.password
            }, function (e) {
                //_this.dialogPwsVisible = false
                _this.replaceLoading = false
                _this.dialogPwsLoading = false
                _this.warning(e.reason == null ? "服务器出错" : e.reason)
            })
        },


        // 查询用户签到数据
        inquireUserRecordInfo(order = false, password = this.dialogPwsInputPws) {
            let _this = this
            this.successNum = 0
            this.getUrl(`/genshin-api/user-record?uid=${_this.inputUid}&password=${password}&order=${order}`, function (data) {

                _this.inquireLoading = false
                _this.dialogInquireVisible = true
                _this.dialogPwsVisible = false
                _this.dialogPwsLoading = false

                data.result.forEach(function (v, i) {
                    if (v.name == null || v.name.length <= 0) {
                        v.name = '<无数据>'
                    }
                    let arr = v.time.split(/[ \/:]/g)[0].split("-");
                    v.time = `${arr[1]}月${arr[2]}日`
                    if (v.isError) {
                        v.isError = '成功'
                        _this.successNum++
                    } else {
                        v.isError = '失败'
                    }
                })

                _this.tableData = data.result

            }, function (e) {
                _this.inquireLoading = false
                _this.dialogPwsLoading = false
                console.log(e)
                _this.warning(e.reason == null ? "服务器出错" : e.reason)
            })
        },





        // 更新用户数据
        replaceUserData() {
            let _this = this
            /*if (this.formReplaceUser.cookie == null || this.formReplaceUser.cookie.length <= 0){
                this.warning("cookie 不能为空")
                return
            }*/
            this.dialogReplaceLoading = false

            let param = new FormData()
            param.append('uid', this.inputUid)
            param.append('cookie', this.formReplaceUser.cookie)
            param.append('new-password', this.formReplaceUser.password)
            param.append('old-password', this.dialogPwsInputPws)

            this.postUrl('/genshin-api/replace-user', param, function (data) {
                // 添加成功
                // console.log(data)
                _this.dialogReplaceLoading = false
                _this.formReplaceUser.password = ''
                _this.formReplaceUser.cookie = ''
                _this.dialogPwsInputPws = _this.formReplaceUser.password
                _this.dialogReplaceVisible = false
                _this.success(data.reason)
            }, function (e) {
                _this.dialogReplaceLoading = false
                // 添加失败
                // console.log(e)
                _this.warning(e.reason == null ? "服务器出错" : e.reason)
            })
        },

        // 查询用户公开数据
        queryUserPublicData(user_pws_call, user_not_pws_call, user_not_exist_call, error_call) {
            let _this = this
            if (this.isInputUid()) {
                _this.warning("uid 有误")
                error_call(null)
                return
            }
            this.getUrl(`/genshin-api/inquire-user/${this.inputUid}`, function (data) {
                console.log(data)
                if (data.result.isExist) {
                    if (data.result.isPws) {
                        user_pws_call(data)
                        _this.dialogPwsVisible = true
                    } else {
                        // 直接查询
                        user_not_pws_call(data)
                    }
                } else {
                    user_not_exist_call(data)
                    _this.warning(data.result.msg)
                }
            }, function (e) {
                error_call(e)
                console.log(e)
                _this.error(e.reason == null ? "服务器出错" : e.reason)
            })
        },

        // 解绑用户
        sendUnbindUser(){
            let _this = this
            if (this.isInputUid()) {
                this.warning("uid 有误")
                return
            }
            this.dialogUnbindLoading = true
            this.getUrl(`/genshin-api/unbind-user?uid=${this.inputUid}&password=${this.dialogPwsInputPws}`, function (data) {
                _this.dialogUnbindLoading = false
                _this.dialogUnbindVisible = false
                console.log(data)
                _this.success(data.reason)
            }, function (e) {
                _this.dialogUnbindLoading = false
                console.log(e)
                _this.error(e.reason == null ? "服务器出错" : e.reason)
            })
        },



        onPwsClickChoose(){
            let _this = this
            this.dialogPwsLoading = true
            if (this.onPwsClick === 0) {
                // 更新
                this.inquireBaseUserInfo(this.dialogPwsInputPws)
            } else  if (this.onPwsClick === 1){
                // 查询
                this.inquireUserRecordInfo()
            } else if (this.onPwsClick === 2){
                // 解绑
                this.getUrl(`/genshin-api/user-info?uid=${this.inputUid}&password=${this.dialogPwsInputPws}`, function (data) {
                    _this.dialogPwsVisible = false
                    _this.dialogUnbindVisible = true
                    _this.dialogPwsLoading = false
                }, function (e) {
                    _this.dialogPwsLoading = false
                    _this.warning(e.reason == null ? "服务器出错" : e.reason)
                })
            }
        },

        windowDialogAdjustment() {
            if (this.element.clientWidth < 1200) {
                this.dialogWidth = 60
                if (this.element.clientWidth < 992) {
                    this.dialogWidth = 80
                }
                this.unbindStr = null
                this.inquireStr = null
                this.addStr = null
                this.replaceStr = null
            } else {
                this.dialogWidth = 40
                this.addStr = "添加"
                this.inquireStr = "查询"
                this.unbindStr = "解除"
                this.replaceStr = "更新"
            }
            this.mobileScroll()
        },
        handleScroll() {
        },
        mobileScroll() {
            if (this.isMobile()) {
                this.element.scrollTop = this.element.clientHeight
            }
        },
        isMobile() {
            return navigator.userAgent.match(/(phone|pad|pod|iPhone|iPod|ios|iPad|Android|Mobile|BlackBerry|IEMobile|MQQBrowser|JUC|Fennec|wOSBrowser|BrowserNG|WebOS|Symbian|Windows Phone)/i);
        },
        autoScroll() {
            let scroll = this.element.scrollTop;
            if (this.element.scrollTop < this.element.clientHeight * 0.5) {
                let timer = setInterval(() => {
                    if (scroll < this.element.clientHeight) {
                        if (scroll < this.element.clientHeight * 0.9) { //在大于20px时滚动速度较快
                            scroll += 10
                        } else {
                            scroll += 2
                        }
                    } else {
                        clearInterval(timer)
                    }
                    this.element.scrollTop = scroll
                }, 1)
            } else {
                let timer = setInterval(() => {
                    if (scroll > 0) {
                        if (scroll > this.element.clientHeight - this.element.clientHeight * 0.9) { //在大于20px时滚动速度较快
                            scroll -= 10
                        } else {
                            scroll -= 2
                        }
                    } else {
                        clearInterval(timer)
                    }
                    this.element.scrollTop = scroll
                }, 1)

            }

        },

        isInputUid() {
            return this.inputUid == null ||
                this.inputUid === '' ||
                this.inputUid.length !== 9 ||
                isNaN(this.inputUid)
        },

        postUrl(url, param, success_callback, error_callback) {
            let config = {
                headers: {'Content-Type': 'multipart/form-data'}
            }
            this.$http.post(`${this.serve_url}${url}`, param, config).then((res) => {
                if (res.data.error_code === 200) {
                    success_callback(res.data)
                } else {
                    error_callback(res.data)
                }
            }).catch((err) => {
                error_callback(err)
            })
        },

        getUrl(url, success_callback, error_callback) {
            this.$http.get(`${this.serve_url}${url}`).then((res) => {
                if (res.data.error_code === 200) {
                    success_callback(res.data)
                } else {
                    error_callback(res.data)
                }
            }).catch((err) => {
                error_callback(err)
            })
        }
    }

});

