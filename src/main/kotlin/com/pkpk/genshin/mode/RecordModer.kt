package com.pkpk.genshin.mode

import com.baomidou.mybatisplus.annotation.*
import com.fasterxml.jackson.annotation.JsonFormat

@TableName(value = "record")
data class RecordModer(
    @TableId(type = IdType.AUTO, value = "c_id")
    var id: Long? = null,
    @TableField(value = "c_belong")
    var belong: String,                         // 属于那个 uid
    @TableField(value = "c_game_name")
    var gameName: String? = null,               // 游戏签到奖励名称
    @TableField(value = "c_game_quantity")
    var gameQuantity: Int? = 0,                 // 游戏签到奖励数量
    @TableField(value = "c_bbs_quantity")
    var bbsQuantity: Int? = 0,                  // bbs奖励米游币数量
    @TableField(value = "c_is_game_ok")
    var isGameOk: Boolean = false,              // 游戏签到是否成功  true成功
    @TableField(value = "c_is_bbs_ok")
    var isBbsOk: Boolean = false,               // 社区任务是否成功  true成功
    @TableField(value = "c_logo_msg")
    var logoMsg: String? = null,                // 日志
    @JsonFormat(timezone = "GMT+8", pattern = "MM月dd日")
    @TableField(value = "c_time", updateStrategy = FieldStrategy.NEVER) // updateStrategy = FieldStrategy.IGNORED,
    var time: String? = null                    // 签到时间
) {

    /**
     * @param uid           属于谁uid
     * @param gameName      奖励名称
     * @param gameQuantity  奖励数量
     * @param isGameError   是否错误
     * @param logoMsg       日志
     */
    constructor(uid: String,
                gameName: String?,
                gameQuantity: Int?,
                isGameError: Boolean,
                logoMsg: String? = "")
            : this(null, uid, gameName, gameQuantity, 0, isGameError, false, logoMsg)

    // 错误
    constructor(uid: String, logoMsg: String? = "")
            : this(null, uid, null, 0, 0, false, false, logoMsg)

}