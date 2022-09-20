package com.pkpk.genshin.mode


data class InquireUser(
    var msg: String,
    var isExist: Boolean,
    var isPws: Boolean,
    var ranking: Long = -1
)

// 排行  ranking
data class Ranking(
    var id: Long,
    var rank: Long
)


// 签到Post Class
data class PostSignData(
    var region: String, // 区服
    var uid: String, // uid
    var act_id: String? = null
)

// 提交信息
data class AskInfoData(
    var cookie: String,     // cookie
    var uuid: String,       // 设备uuid  UUID.randomUUID().toString()
    var sinData: PostSignData? = null
)




// 米游社 用户信息
data class JsonUserInfo(
    val `data`: JsonUserData?,
    var message: String,
    val retcode: Int
)
data class JsonUserData(
    val list: List<Game>
) {
    data class Game(
        val game_biz: String,       // 游戏代号    "hk4e_cn" = 原神
        val game_uid: String,       // uid
        val is_chosen: Boolean,     // 是否绑定签到
        val is_official: Boolean,   // 是否官服
        val level: Int,             // 等级
        val nickname: String,       // 昵称
        val region: String,         // 游戏区服
        val region_name: String,    // 区服昵称   "天空岛"
        var ranking: Long = -1      // 排行
    )
}


// 米游社 签到信息
data class JsonSignInfo(
    val `data`: JsonSignData?,
    var message: String,
    val retcode: Int
)
data class JsonSignData(
    val first_bind: Boolean,    // 是否第一次
    val is_sign: Boolean,       // 是否签到
    val is_sub: Boolean,        //
    val month_first: Boolean,   //
    val sign_cnt_missed: Int,   // 本月未签到多少天
    val today: String,          // 日期
    val total_sign_day: Int     // 已签多少天
)


// 米游社  签到奖励列表
data class JsonSignReward(
    val `data`: JsonSignRewardData?,
    var message: String,
    val retcode: Int
)
data class JsonSignRewardData(
    val awards: List<Award>,   // 奖励列表
    val month: Int,            // 第几个月
    val resign: Boolean        //
) {
    data class Award(
        val cnt: Int,           // 奖励数量
        val icon: String,       // 奖励物品图标
        val name: String        // 奖励物品名称
    )
}



// 签到完成
data class JsonSignOk(
    val `data`: JsonSignOkData,
    var message: String,
    val retcode: Int
)
data class JsonSignOkData(
    val challenge: String,
    val code: String,
    val gt: String,
    val risk_code: Int,
    val success: Int  // 0 = 成功
)