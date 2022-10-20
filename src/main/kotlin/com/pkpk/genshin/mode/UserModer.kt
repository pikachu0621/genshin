package com.pkpk.genshin.mode

import com.baomidou.mybatisplus.annotation.*
import java.io.Serializable

// 用户
@TableName(value = "user")
data class UserModer(
    @TableId(type = IdType.AUTO)
    var id: Long? = null,
    var uid: String,                        // 游戏uid
    var region: String,                     // 游戏服区
    var uuid: String,                       // 设备UUID
    @TableField(value = "account_id")
    var accountId: String,                  // 米游社 id
    @TableField(value = "stoken")
    var sToken: String? = null,             // 米游社 s token
    @TableField(value = "cookie_token")
    var cookieToken: String,                // 米游社 cookie_token
    @TableField(value = "cookie")
    var cookie: String,                     // 总的cookie 游戏签到用 不然容易风控
    var password: String? = null,
    @TableField(value = "add_time", updateStrategy = FieldStrategy.IGNORED)
    var addTime: String? = null,
    var isLock: Boolean = false,             //    (ture = 1 = 锁定 / false = 0 = 未锁定 )
    @TableField(value = "up_time", updateStrategy = FieldStrategy.IGNORED)
    var upTime: String? = null,
    @TableField(value = "is_cookie_available")
    var isCookieAvailable: Boolean = true,   // (ture = 1 = 可用 / false = 0 = 不可用 )
    @TableField(exist = false)
    var ranking: Long = -1                   // 排行
) : Serializable {
    constructor(
        uid: String,
        region: String,
        uuid: String,
        accountId: String,
        sToken: String? = null,
        cookieToken: String,
        cookie: String,
        password: String? = null
    ) : this(null, uid, region, uuid, accountId, sToken, cookieToken, cookie, password)

    constructor(
        id: Long? = null,
        uid: String,
        region: String,
        uuid: String,
        accountId: String,
        sToken: String? = null,
        cookieToken: String,
        cookie: String,
        password: String? = null,
        addTime: String? = null,
        isLock: Boolean = false,
        upTime: String? = null,
        isCookieAvailable: Boolean = true,
    ) : this(id, uid, region, uuid, accountId, sToken, cookieToken, cookie, password, addTime, isLock, upTime, isCookieAvailable, -1)
}