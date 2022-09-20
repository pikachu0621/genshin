package com.pkpk.genshin.mode

import com.baomidou.mybatisplus.annotation.*
import com.fasterxml.jackson.annotation.JsonFormat
import org.springframework.format.annotation.DateTimeFormat
import javax.xml.crypto.Data

@TableName(value = "record")
data class RecordModer(
    @TableId(type = IdType.AUTO, value = "c_id")
    var id: Long? = null,
    @TableField(value = "c_name")
    var name: String? = null, // 奖励名称
    @TableField(value = "c_quantity")
    var quantity: Int? = 0, // 奖励数量
    @TableField(value = "c_belong")
    var belong: String, // 属于那个 uid
    @JsonFormat(timezone = "GMT+8", pattern = "MM月dd日")
    @TableField(value = "c_time", updateStrategy = FieldStrategy.IGNORED)
    var time: String? = null, // 签到时间
    @TableField(value = "c_error_msg")
    var errorMsg: String? = null, // 错误原因
    @TableField(value = "c_is_error")
    var isError: Boolean = false // 是否成功  true成功
) {

    /**
     * @param uid 属于谁
     * @param name 奖励名称
     * @param quantity 奖励数量
     * @param isError 是否错误
     * @param errorMsg 错误信息  正确可空
     */
    constructor(uid: String, name: String?, quantity: Int?, isError: Boolean, errorMsg: String? = "")
            : this(null, name, quantity, uid, null, errorMsg, isError)

    // 错误
    constructor(uid: String, errorMsg: String? = "")
            : this(null, "", 0, uid, null, errorMsg, false)

}