package com.pkpk.genshin.mode

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// 配置文件类 json
data class ConfData(
    val game_list: List<Game>,
    val globals: Globals,
    val headers: Headers
) {
    data class Game(
        val forum_id: Int,
        val id: Int,
        val name: String,
        val url: String
    )

    data class Headers(
        val reward: Map<String, String>,
        val bbs: Map<String, String>
    )


    data class Globals(
        val act_id: String,
        val app_version: String,
        val ds_game_password: String,
        val ds_bbs_password: String,
        val ds_bbs_password2: String,
        val url_bbs_token: String,
        val url_bbs_forum_list: String,
        val url_bbs_task_list: String,
        val url_bbs_detail: String,
        val url_bbs_share: String,
        val url_bbs_sign: String,
        val url_bbs_vote: String,
        val url_game_sign: String,
        val url_game_sign_awards: String,
        val url_game_sign_info: String,
        val url_game_user_info: String
    )

}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
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
    var region: String,     // 区服
    var uid: String,        // uid
    var act_id: String? = null
)

// bbs 签到
data class PostBbsSignData(
    val gids: String  // 游戏id
)

// bbs 点赞
data class PostBbsLikeData(
    val post_id: String,  // 文章id
    val is_cancel: Boolean = false, // true 为取消点赞
)


// 提交信息
data class AskInfoData<T>(
    var uuid: String,           // 设备uuid  UUID.randomUUID().toString()
    var accountId: String,      // 米游社account_id
    var sToken: String? = null, // 米游社stoken
    var cookie: String,         // 米游社cookie   签到用这个
    var sinData: T? = null,
    var type: AskInfoType = AskInfoType.GAME_SIGN    // 类型
) {
    constructor(
        uuid: String? = null,
        accountId: String? = null,
        sToken: String? = null,
        cookie: String? = null,
        sinData: T? = null,
        type: AskInfoType = AskInfoType.GAME_SIGN_NULL,
        tag: Any? = null,
    ) : this(uuid?:"", accountId?:"", sToken, cookie?:"", sinData, type)

}

enum class AskInfoType {
    GAME_SIGN,  // 签到任务
    BBS_SIGN,    // 米游币任务
    GAME_SIGN_NULL,    // 签到签到 不加 DS 以及cookie
    BBS_SIGN_NULL    // 米游币任务 不加 DS 以及cookie
}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// 米游社 返回信息包裹类
data class JsonMiHoYoBack<T>(
    val `data`: T?,
    var message: String,
    val retcode: Int
)


// 米游社 用户信息
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
        var ranking: Long = -1,      // 排行
        var addResults: HashMap<String, AddResults> = hashMapOf<String, AddResults>().apply {
            put(NameKey.GAME_SIGN.KEY, AddResults(true, "每日签到添加成功"))
        }      // 添加结果
    )

    data class AddResults(
        var isOk: Boolean,
        var msg: String
    )

    enum class NameKey(val KEY: String) {
        GAME_SIGN("game_sign"),
        BBS_SING("bbs_sign")
    }
}


// 米游社 签到信息
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
data class JsonSignOkData(
    val challenge: String,
    val code: String,
    val gt: String,
    val risk_code: Int,
    val success: Int  // 0 = 成功
)

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// bbs

// 帖子列表
data class BbsPostListData(
    val list: List<PostListData>
) {
    data class PostListData(
        val post: Post
    )

    data class Post(
        val f_forum_id: Int,
        val game_id: Int,
        val post_id: String,
        val uid: String,
    )
}


// 任务列表
data class BbsUserTaskListData(
    val already_received_points: Int,   // 今天已获取的
    val can_get_points: Int,            // 还可以获取到的米游币
    val is_unclaimed: Boolean,
    val states: List<State>,            // 任务列表
    val today_total_points: Int,        // 今天最多可获取数量
    val total_points: Int               // 用户总米游币数量
) {
    data class State(
        val mission_id: Int,            // 任务 id    58(讨论区签到),  59(浏览帖子), 60(点赞), 61(分享)
        val process: Int,               // 1 已完成    0 未完成
        val happened_times: Int,        // 已完成了多少次
        val is_get_award: Boolean,      // 奖励是否已获取 （任务是否已完成）
        val mission_key: String         // 任务关键词
    )
}

enum class BbsTaskType(
    val TASK_ID: Int, // 任务id
    val TASK_COUNT: Int, // 次数
    val TASK_REWARD: Int, // 最多奖励的米游币
    var TASK_OK: Boolean = false
) {
    // 讨论区签到
    TASK_SIGN(58, 1, 50),

    // 浏览帖子
    TASK_BROWSE(59, 3, 20),

    // 点赞
    TASK_LIKE(60, 5, 30),

    // 分享
    TASK_SHARE(61, 1, 10)
}


// 签到成功
data class PointsData(
    val points: Int
)

// 获取 stoken
data class TokenListData(
    val list: List<TokenData>
) {
    data class TokenData(
        val name: String,
        val token: String
    )
}


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////