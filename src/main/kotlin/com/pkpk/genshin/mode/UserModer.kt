package com.pkpk.genshin.mode

import com.baomidou.mybatisplus.annotation.*
import java.io.Serializable

// 用户
@TableName(value = "user")
data class UserModer(
    @TableId(type = IdType.AUTO)
    var id: Long? = null,
    var uid: String,
    var cookie: String,
    var region: String,
    var uuid: String,
    @TableField(value = "cookie_token")
    var cookieToken: String? = null,
    var password: String? = null,
    @TableField(value = "add_time", updateStrategy = FieldStrategy.IGNORED)
    var addTime: String? = null,
    var isLock: Boolean = false,   //       (ture = 1 = 锁定 / false = 0 = 未锁定 )
    @TableField(value = "up_time", updateStrategy = FieldStrategy.IGNORED)
    var upTime: String? = null,
    @TableField(value = "is_cookie_available")
    var isCookieAvailable: Boolean = true,  // (ture = 1 = 可用 / false = 0 = 不可用 )
    @TableField(exist = false)
    var ranking: Long = -1 // 排行
) : Serializable {
    constructor(
        uid: String,
        cookie: String,
        region: String,
        uuid: String,
        cookieToken: String,
        password: String? = null
    ) : this(null, uid, cookie, region, uuid, cookieToken, password)

    constructor(
        id: Long? = null,
        uid: String,
        cookie: String,
        region: String,
        uuid: String,
        cookieToken: String? = null,
        password: String? = null,
        addTime: String? = null,
        isLock: Boolean = false,
        upTime: String? = null,
        isCookieAvailable: Boolean = true
    ) : this(id, uid, cookie, region, uuid, cookieToken, password, addTime, isLock, upTime, isCookieAvailable, -1)
}