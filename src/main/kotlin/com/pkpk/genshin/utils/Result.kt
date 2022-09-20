package com.pkpk.genshin.utils

import com.fasterxml.jackson.annotation.JsonInclude
import java.io.Serializable


const val ERROR_PARAMETER = -1  // 参数错误
const val ERROR_SQL_CONNECT = -2 // 数据库连接错误

const val ERROR_USER_PWS = -3 // 用户uid 受保护
const val ERROR_USER_PWS_ERR = -4 // 用户输入密码错误

const val ERROR_USER_EXIST = -5 // 用户存在?
const val ERROR_COOKIE = -6 // 用户cookie错误



const val ERROR = -200 // 其他错误
const val OK = 200

data class Result(
    val reason : String? ,
    var error_code: Int?,
    var result: Any?): Serializable {

    companion object {
        fun ok(data: Any?, reason: String = "ok", error_code: Int = OK): Result {
            return Result(reason, error_code, data)
        }
        fun err(reason: String?, error_code: Int): Result {
            return Result(reason, error_code, null)
        }
    }
}