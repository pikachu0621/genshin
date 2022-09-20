package com.pkpk.genshin.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.pkpk.genshin.mapper.UserMapper
import com.pkpk.genshin.mode.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.util.DigestUtils
import org.springframework.web.client.RestTemplate
import java.sql.Timestamp
import kotlin.math.floor


@Component
object AskUtils {

    // log
    private val log: Logger = LoggerFactory.getLogger(AskUtils::class.java)

    // ds 密码
    private var dsPassword = "9nQiU3AV0rJSIBWgdynfoGMGKaklfbM7"

    // api 动作Id
    private var actId = "e202009291139501"

    // 用户信息  ?game_biz=hk4e_cn  原神 = hk4e_cn
    private var urlUserInfo = "https://api-takumi.mihoyo.com/binding/api/getUserGameRolesByCookie"

    // 用户签到奖励list  ?act_id=
    private var urlSignAwards = "https://api-takumi.mihoyo.com/event/bbs_sign_reward/home"

    // 请求签到   PostSignData {"act_id":"e202009291139501","region":"cn_gf01","uid":"198904404"}  中间无空格
    private var urlSign = "https://api-takumi.mihoyo.com/event/bbs_sign_reward/sign"

    // 获取签到信息   ?region=${region}&act_id=${act_id}&uid=
    private var urlSignInfo = "https://api-takumi.mihoyo.com/event/bbs_sign_reward/info"

    // json
    private var mapper: ObjectMapper = ObjectMapper()


    private val fixedHeaders = mutableMapOf<String, String>().apply {
        put(
            "User_Agent",
            "Mozilla/5.0 (Linux; Android 10; MIX 2 Build/QKQ1.190825.002; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/83.0.4103.101 Mobile Safari/537.36 miHoYoBBS/2.34.1"
        )
        put("Accept", "application/json, text/plain, */*")
        put("Content_Type", "application/json;charset=UTF-8")
        put("Connection", "keep-alive")
        put("Origin", "https://webstatic.mihoyo.com")
        put("X_Requested_With", "com.mihoyo.hyperion")
        put("Sec_Fetch_Site", "same-site")
        put("Sec_Fetch_Mode", "cors")
        put("Sec_Fetch_Dest", "empty")
        put("Accept_Encoding", "gzip,deflate")
        put("Accept_Language", "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7")
        put("Content-Length", "66")
        put("x-rpc-client_type", "5")
        put(
            "Referer",
            "https://webstatic.mihoyo.com/bbs/event/signin-ys/index.html?bbs_auth_required=true&act_id=${actId}1&utm_source=bbs&utm_medium=mys&utm_campaign=icon"
        )
        put("x-rpc-app_version", "2.34.1")
    }


    /////////////////////////////////////////////////////////////////////////////////////////////

    // 获取用户信息 原神
    fun getUserInfo(askInfoData: AskInfoData) =
        getUrl("$urlUserInfo?game_biz=hk4e_cn", askInfoData, JsonUserInfo::class.java)?.body


    // 获取 原神 签到奖励List
    fun getSignAwards(actId: String = this.actId!!) =
        getUrl("$urlSignAwards?act_id=$actId", null, JsonSignReward::class.java)?.body


    // 获取用户签到信息
    fun getUserSignInfo(askInfoData: AskInfoData): JsonSignInfo? {
        if (askInfoData.sinData == null) return null
        return getUrl(
            "$urlSignInfo?region=${askInfoData.sinData!!.region}&act_id=${actId}&uid=${askInfoData.sinData!!.uid}",
            askInfoData,
            JsonSignInfo::class.java
        )?.body
    }

    // 用户签到
    fun postUserSignIn(askInfoData: AskInfoData): JsonSignOk? {
        if (askInfoData.sinData == null) return null
        askInfoData.sinData!!.act_id = actId
        return postUrl(urlSign, askInfoData.sinData!!, askInfoData, JsonSignOk::class.java)?.body
    }


    // 用户cookie是否有效
    fun inquireCookieEfficient(askInfoData: AskInfoData): Boolean {
        val userInfo = getUserInfo(askInfoData)
        if (userInfo != null && userInfo.retcode == 0) {
            return true
        }
        return false
    }


    /////////////////////////////////////////////////////////////////////////////////////////////

    private fun <T> getUrl(
        url: String,
        askInfoData: AskInfoData?,
        responseType: Class<T>
    ): ResponseEntity<T>? {
        val headers = getHeaders(askInfoData)
        // log.info(headers.toString())
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


    /**
     * @param url url
     * @param postAny 要 post 对象
     * @param askInfoData 签到用户必要信息
     * @param responseType 要返回对象类型
     */
    private fun <T> postUrl(
        url: String,
        postAny: Any,
        askInfoData: AskInfoData?,
        responseType: Class<T>
    ): ResponseEntity<T>? {
        val writeValueAsString = mapper.writeValueAsString(postAny)
        log.info("签到post json $writeValueAsString")
        return try {
            RestTemplate().exchange(
                url,
                HttpMethod.POST,
                HttpEntity(writeValueAsString, getHeaders(askInfoData)),
                responseType
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun getHeaders(askInfoData: AskInfoData?) = HttpHeaders().apply {
        askInfoData?.let {
            setAll(fixedHeaders)
            add("DS", getDS())
            add("Cookie", it.cookie)
            add("x-rpc-device_id", it.uuid) // 518777d2-1259-3bf6-881d-ff301ede1392 // UUID.randomUUID().toString()
        }
    }


    private fun getDS(): String {
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


    fun getGameCookieToken(valueStr: String): String? = getCookieValue(valueStr, "cookie_token") // 这个属于米游社uid
    fun getGameCookieUUid(valueStr: String): String? = getCookieValue(valueStr, "_MHYUUID") // 这个属于米游社uid
    fun getCookieValue(valueStr: String, key: String): String? {
        cookieToMap(valueStr)?.let {
            return it[key]
        }
        return null
    }


    private fun cookieToMap(valueStr: String): Map<String, String>? {

        return try {
            var value = valueStr
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
    fun isStrCookieEfficient(userMapper: UserMapper, cookie: String?): String? {
        if (cookie == null || cookie.isEmpty() || cookie.length > 5000 || cookie.length < 100) {
            return null
        }
        val cookieToken = getGameCookieToken(cookie)
        if (cookieToken == null || cookieToken.isEmpty()) {
            return null
        }
        if (SqlUtils.isGameLoginTicket(userMapper, cookieToken)) {
            return "1"
        }
        return cookieToken
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


}