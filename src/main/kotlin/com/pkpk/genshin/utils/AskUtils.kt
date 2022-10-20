package com.pkpk.genshin.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.pkpk.genshin.mapper.UserMapper
import com.pkpk.genshin.mode.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.util.DigestUtils
import org.springframework.web.client.RestTemplate
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl
import java.sql.Timestamp
import java.util.*
import kotlin.math.floor
import kotlin.random.Random


@Component
object AskUtils {

    // log
    private val log: Logger = LoggerFactory.getLogger(AskUtils::class.java)

    // json
    private var mapper: ObjectMapper = ObjectMapper()

    // 配置数据
    lateinit var confData: ConfData

    /////////////////////////////////////////////////////////////////////////////////////////////

    // 获取用户信息 原神
    /**
     * uuid not null
     * cookie not null
     * 无需 ds   需 cookie
     */
    fun getUserInfo(askInfoData: AskInfoData<out Any>): JsonMiHoYoBack<JsonUserData>? {
        return getUrl(
            "${confData.globals.url_game_user_info}?game_biz=hk4e_cn",
            getHeaders(askInfoData.apply {
                type = AskInfoType.GAME_SIGN
            }),
            object : ParameterizedTypeReference<JsonMiHoYoBack<JsonUserData>>() {}
        )?.body
    }


    // 获取 原神 签到奖励List url_game_sign_awards
    // 无需 ds  无需 cookie
    fun getSignAwards(actId: String = confData.globals.act_id) =
        getUrl(
            "${confData.globals.url_game_sign_awards}?act_id=$actId",
            getHeaders(null),
            JsonSignRewardData::class.java
        )?.body


    // 获取用户签到信息
    // 无需 ds  需 cookie
    fun getUserSignInfo(askInfoData: AskInfoData<PostSignData>): JsonMiHoYoBack<JsonSignData>? {
        if (askInfoData.sinData == null) return null
        return getUrl(
            "${confData.globals.url_game_sign_info}?region=${askInfoData.sinData!!.region}&act_id=${confData.globals.act_id}&uid=${askInfoData.sinData!!.uid}",
            getHeaders(askInfoData.apply {
                type = AskInfoType.GAME_SIGN
            }),
            JsonSignData::class.java
        )?.body
    }

    // 用户签到    cookie  account_id=172095415;cookie_token=lrKIPlzfgBp1hQTnUgP8pPlf4auCYIh5pdWZwABa;
    // 需 ds  需 cookie
    fun postUserSignIn(askInfoData: AskInfoData<PostSignData>): JsonMiHoYoBack<JsonSignOkData>? {
        if (askInfoData.sinData == null) return null
        askInfoData.sinData!!.act_id = confData.globals.act_id
        // log.warn("----------     $headers    ${askInfoData.sinData}")
        /// {"region":"cn_gf01","uid":"198904404","act_id":"e202009291139501"}
        log.info("${getHeaders(askInfoData).apply {
            add("DS", getDS())
        }}")
        return postUrl(
            confData.globals.url_game_sign,
            askInfoData.sinData!!,
            getHeaders(askInfoData.apply {
                type = AskInfoType.GAME_SIGN
            }).apply {
                add("DS", getDS())
            },
            JsonSignOkData::class.java
        )?.body
    }


    // 米游币任务

    // 获取帖子列表
    //  无需 ds  无需 cookie
    fun getBbsForumPostList(): JsonMiHoYoBack<BbsPostListData>? {
        val askInfoData = AskInfoData<Any>(type = AskInfoType.BBS_SIGN_NULL)
        val gameKeyData = getListGameKeyData(2) ?: return null
        // log.info("${confData.globals.url_bbs_forum_list}    ${getHeaders(askInfoData)} ")
        return getUrl(
            "${confData.globals.url_bbs_forum_list}${gameKeyData.forum_id}",
            getHeaders(askInfoData),
            BbsPostListData::class.java
        )?.body
    }

    // 获取 任务列表
    // 无需 ds  需 cookie
    fun getBbsTaskList(askInfoData: AskInfoData<out Any>): JsonMiHoYoBack<BbsUserTaskListData>? {
        // log.info("------- $httpHeaders   ${confData.globals.url_bbs_task_list}")
        return getUrl(
            confData.globals.url_bbs_task_list,
            getHeaders(askInfoData.apply {
                type = AskInfoType.GAME_SIGN
            }),
            BbsUserTaskListData::class.java
        )?.body
    }


    // 签到
    //  需 ds  需 cookie
    fun postBbsSign(askInfoData: AskInfoData<PostBbsSignData>): JsonMiHoYoBack<PointsData>? {
        val gameKeyData = getListGameKeyData(2) ?: return null
        // log.info("------------ ${getHeaders(askInfoData)}    ${confData.globals.url_bbs_sign}   ${PostBbsSignData("${gameKeyData.id}")}")
        return postUrl(
            confData.globals.url_bbs_sign,
            askInfoData.sinData!!,
            getHeaders(askInfoData.apply {
                sinData = PostBbsSignData("${gameKeyData.id}")
                type = AskInfoType.BBS_SIGN
            }).apply {
                log.info(mapper.writeValueAsString(values))
                add("DS", getDS2(mapper.writeValueAsString(askInfoData.sinData)))
            },
            PointsData::class.java
        )?.body
    }


    // 看帖  sinData 文章id
    //  无需 ds  需 cookie
    fun getBbsLook(askInfoData: AskInfoData<String>): JsonMiHoYoBack<Any>? {
        // log.info("----------- ${confData.globals.url_bbs_detail}${askInfoData.sinData}     ${getHeaders(askInfoData)}")
        return getUrl(
            "${confData.globals.url_bbs_detail}${askInfoData.sinData}",
            getHeaders(askInfoData.apply {
                type = AskInfoType.BBS_SIGN
            }),
            Any::class.java
        )?.body
    }

    // 点赞
    //  需 ds  需 cookie
    fun postBbsVote(askInfoData: AskInfoData<PostBbsLikeData>): JsonMiHoYoBack<Any>? {
        askInfoData.sinData ?: return null
        return postUrl(
            confData.globals.url_bbs_vote,
            askInfoData.sinData!!,
            getHeaders(askInfoData.apply {
                type = AskInfoType.BBS_SIGN
            }).apply {
                add("DS", getDS(false))
            },
            Any::class.java
        )?.body
    }

    // 分享   sinData 文章id
    //  无需 ds  需 cookie
    fun getBbsShare(askInfoData: AskInfoData<String>): JsonMiHoYoBack<Any>? {
        return getUrl(
            "${confData.globals.url_bbs_share}${askInfoData.sinData}",
            getHeaders(askInfoData.apply {
                type = AskInfoType.BBS_SIGN
            }).apply {
                add("DS", getDS(false))
            },
            Any::class.java
        )?.body
    }


    // 根据 login_ticket 获取 stoken
    //  需 ds  需 cookie
    fun getBbsSToken(loginTicket: String, accountId: String): String? {
        val askInfoData = AskInfoData<Any>(type = AskInfoType.GAME_SIGN_NULL)
        val formatBbsToken = String.format(confData.globals.url_bbs_token, loginTicket, accountId)
        val body = getUrl(
            formatBbsToken,
            getHeaders(askInfoData),
            getReference(TokenListData::class.java)
        )?.body
        // log.info(" -----------       ${body}   ${formatBbsToken}")
        body ?: return null
        if (body.retcode != 0 || body.data == null || body.data.list.isEmpty()) {
            return null
        }
        body.data.list.forEach {
            if (it.name == "stoken") {
                return it.token
            }
        }
        return null
    }


    // 用户cookie是否有效
    fun inquireCookieEfficient(askInfoData: AskInfoData<out Any>): Boolean {
        val userInfo = getUserInfo(askInfoData)
        if (userInfo != null && userInfo.retcode == 0) {
            return true
        }
        return false
    }

    /////////////////////////////////////////////////////////////////////////////////////////////

    inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

    fun <T> getReference(clazz: Class<T>): ParameterizedTypeReference<JsonMiHoYoBack<T>> {
        val type = ParameterizedTypeImpl.make(
            JsonMiHoYoBack::class.java, arrayOf(clazz),
            JsonMiHoYoBack::class.java.declaringClass
        )
        return ParameterizedTypeReference.forType(type)
    }


    private fun <T> getUrl(
        url: String,
        headers: HttpHeaders,
        clazz: Class<T>
    ): ResponseEntity<JsonMiHoYoBack<T>>? {
        return getUrl(url, headers, getReference(clazz))
    }

    /**
     * @param url url
     * @param headers 头部
     * @param responseType 要返回对象类型
     */
    private fun <T> getUrl(
        url: String,
        headers: HttpHeaders,
        responseType: ParameterizedTypeReference<T>
    ): ResponseEntity<T>? {
        return try {
            RestTemplate().exchange(
                url,
                HttpMethod.GET,
                HttpEntity<String>(headers),
                responseType
            )
        } catch (e: Exception) {
            null
        }
    }


    private fun <T> postUrl(
        url: String,
        postAny: Any,
        headers: HttpHeaders,
        clazz: Class<T>
    ): ResponseEntity<JsonMiHoYoBack<T>>? {
        return postUrl(url, postAny, headers, getReference(clazz))
    }

    /**
     * @param url url
     * @param postAny 要 post 对象
     * @param headers 头部
     * @param responseType 要返回对象类型
     */
    private fun <T> postUrl(
        url: String,
        postAny: Any,
        headers: HttpHeaders,
        responseType: ParameterizedTypeReference<T>
    ): ResponseEntity<T>? {
        val writeValueAsString = mapper.writeValueAsString(postAny)
        return try {
            RestTemplate().exchange(
                url,
                HttpMethod.POST,
                HttpEntity(writeValueAsString, headers),
                responseType
            )
        } catch (e: Exception) {
            null
        }
    }


    /**
     * ds 按需添加
     * add("DS", getDS2(mapper.writeValueAsString(values)))
     * add("DS", getDS())
     */
    private fun getHeaders(askInfoData: AskInfoData<*>?) = HttpHeaders().apply {
        askInfoData?.let {
            when (askInfoData.type) {

                AskInfoType.GAME_SIGN_NULL -> {
                    setAll(confData.headers.reward)
                    add(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 11; MI 9 Build/RKQ1.200826.002; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/83.0.4103.106 Mobile Safari/537.36 miHoYoBBS/${confData.globals.app_version}"
                    )
                }

                AskInfoType.BBS_SIGN_NULL -> setAll(confData.headers.bbs)

                AskInfoType.GAME_SIGN -> {
                    setAll(confData.headers.reward)
                    add(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 11; MI 9 Build/RKQ1.200826.002; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/83.0.4103.106 Mobile Safari/537.36 miHoYoBBS/${confData.globals.app_version}"
                    )
                    add("Cookie", askInfoData.cookie)
                }

                AskInfoType.BBS_SIGN -> {
                    setAll(confData.headers.bbs)
                    add("Cookie", "stuid=${askInfoData.accountId};stoken=${askInfoData.sToken}")
                }
            }
            add("x-rpc-app_version", confData.globals.app_version)
            add("x-rpc-device_id", it.uuid)
        }
    }


    fun getDS2(b: String, q: String = ""): String {
        val ts = Timestamp(System.currentTimeMillis())
        val i = (ts.time / 1000).toString()
        // val r = Random().nextInt(100001, 200000).toString()  open-jdk-17
        val r = Random.nextInt(100001, 200000).toString()
        val add = "&b=$b&q=$q"
        val c: String =
            DigestUtils.md5DigestAsHex(("salt=${confData.globals.ds_bbs_password}&t=$i&r=$r$add").toByteArray())
        return "$i,$r,$c"
    }

    private fun getDS(isWebDs: Boolean = true) =
        getDS(if (isWebDs) confData.globals.ds_game_password else confData.globals.ds_bbs_password2)

    private fun getDS(dsPassword: String): String {
        val i = (Timestamp(System.currentTimeMillis()).time / 1000).toInt().toString()
        val r = getRandomStr(6)
        val c = DigestUtils.md5DigestAsHex("salt=$dsPassword&t=$i&r=$r".toByteArray())
        return "${i},${r},${c}"
    }

    private fun getRandomStr(e: Int): String {
        val d = "ABCDEFGHJKMNPQRSTWXYZabcdefhijkmnprstwxyz2345678"
        var n = ""
        for (i in 0 until e) {
            n += d[floor(Math.random() * d.length).toInt()]
        }
        return n
    }


    fun getCookieAccountId(cookieStr: String): String? = getCookieValue(cookieStr, "account_id")        // 这个属于米游社 account_id
    fun getCookieCookieToken(cookieStr: String): String? = getCookieValue(cookieStr, "cookie_token")    // 这个属于米游社 cookie_token
    fun getCookieLoginTicket(cookieStr: String): String? = getCookieValue(cookieStr, "login_ticket")    // 这个属于米游社 login_ticket
    fun getCookieUUid(cookieStr: String): String? = getCookieValue(cookieStr, "_MHYUUID")               // 这个属于米游社 uuid
    fun getCookieValue(cookieStr: String, key: String): String? {
        cookieToMap(cookieStr)?.let {
            return it[key]
        }
        return null
    }


    private fun cookieToMap(cookieStr: String): Map<String, String>? {
        return try {
            var value = cookieStr
            val map: MutableMap<String, String> = HashMap()
            value = value.replace(" ", "")
            if (value.contains(";")) {
                val values = value.split(";").toTypedArray()
                for (`val` in values) {
                    val vals = `val`.split("=").toTypedArray()
                    map[vals[0]] = vals[1]
                }
            } else {
                val values = value.split("=").toTypedArray()
                map[values[0]] = values[1]
            }
            map
        } catch (e: Exception) {
            null
        }
    }


    // 字段是否为空 string   空 true  非空 false
    fun isFieldEmpty(vararg field: Any?): Boolean {
        field.forEach {
            if (it == null /*|| (it == String && (it as String).isEmpty())*/) {
                return true
            }
        }
        return false
    }

    // 判断字符串cookie 是否有效   有效 cookieToKen   无效 null  已存在 "1"
    fun isStrCookieTokenEfficient(
        userMapper: UserMapper,
        cookie: String?
    ): String? {
        if (cookie == null
            || cookie.isEmpty()
            || cookie.length >= 5000
            || cookie.length < 50
        ) {
            return null
        }
        val cookieCookieToken = getCookieCookieToken(cookie) ?: return null
        val cookieAccountId = getCookieAccountId(cookie) ?: return null
        if (cookieCookieToken.isEmpty() || cookieAccountId.isEmpty()) {
            return null
        }

        if (SqlUtils.isCookieToken(userMapper, cookieCookieToken)) {
            return "1"
        }
        return cookieCookieToken
    }


    // 判断字符串uid 是否有效   有效 true  无效 false
    fun isStrUidEfficient(uid: String?): Boolean {
        if (uid == null) return false
        return uid.length == 9 && isDigit(uid)
    }


    // 判断密码是否有无效    无效  true     有效 false
    fun isPasswordCorrect(password: String?): Boolean {
        return password != null && password.isNotEmpty() && password.length > 12
    }


    fun isDigit(s: String): Boolean {
        if (s.isEmpty()) {
            return false
        }
        s.toCharArray().forEach {
            if (!Character.isDigit(it)) {
                return false
            }
        }
        return true
    }


    /**
     * 获取conf.json 游戏数据
     */
    fun getListGameKeyData(id: Int): ConfData.Game? {
        confData.game_list.forEach {
            if (it.id == id) return it
        }
        return null
    }

}