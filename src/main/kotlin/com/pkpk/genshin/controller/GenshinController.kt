package com.pkpk.genshin.controller

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.fasterxml.jackson.databind.ObjectMapper
import com.pkpk.genshin.mapper.RecordMapper
import com.pkpk.genshin.mapper.UserMapper
import com.pkpk.genshin.mode.*
import com.pkpk.genshin.utils.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.annotation.Resource


@RestController
@RequestMapping("/genshin-api")
@CrossOrigin // 跨域
class GenshinController {

    private val log: Logger = LoggerFactory.getLogger(GenshinController::class.java)

    @Resource
    private var userMapper: UserMapper? = null

    @Resource
    private var recordMapper: RecordMapper? = null

    // 查询用户公开数据
    @GetMapping("/inquire-user/{uid}", "/inquire-user")
    fun inquireUser(@PathVariable("uid", required = false) uid: String?): Result {
        if (uid == null || uid.isEmpty()) return Result.err("uid为空", ERROR_PARAMETER)
        log.info("api - [inquire-user] $uid")
        userMapper?.let {

            val isdUserExist = SqlUtils.isdUserExist(it, uid)
            if (isdUserExist) {
                val selectGameUid = SqlUtils.selectGameUid(it, uid)
                val queryByIdRanking = it.queryByIdRanking(selectGameUid!!.id!!)
                return Result.ok(
                    InquireUser(
                        "用户已存在",
                        true,
                        !(selectGameUid.password == null || selectGameUid.password!!.isEmpty()),
                        queryByIdRanking.rank
                    )
                )
            } else {
                return Result.ok(
                    InquireUser(
                        "用户不存在",
                        isExist = false,
                        isPws = false
                    )
                )
            }
        }
        return Result.err("数据库连接失败", ERROR_SQL_CONNECT)
    }


    // 查询用户信息
    @GetMapping("/user-info")
    fun inquireUserInfo(
        @RequestParam("uid", required = false) uid: String?,
        @RequestParam("password", required = false) password: String?
    ): Result {
        log.info("api - [user-info] $uid")
        if (AskUtils.isFieldEmpty(uid))
            return Result.err("参数不能为空", ERROR_PARAMETER)
        if (!AskUtils.isStrUidEfficient(uid!!))
            return Result.err("uid 有误", ERROR_PARAMETER)
        userMapper?.let {
            if (SqlUtils.isdUserExist(it, uid)) {
                val selectGameUid = SqlUtils.selectGameUid(it, uid)
                if (selectGameUid!!.password == null || selectGameUid.password!!.isEmpty()) {
                    return Result.ok(selectGameUid)
                }
                if (!selectGameUid.password.equals(password)) {
                    return Result.err("密码错误", ERROR_USER_PWS_ERR)
                }
                selectGameUid.ranking = it.queryByIdRanking(selectGameUid.id!!).rank
                return Result.ok(selectGameUid)
            } else {
                return Result.err("用户不存在", ERROR_USER_EXIST)
            }
        }
        return Result.err("数据库连接失败", ERROR_SQL_CONNECT)
    }


    // 更新用户
    @PostMapping("/replace-user")
    @ResponseBody
    fun replaceUser(
        @RequestParam("uid", required = false) uid: String?,
        @RequestParam("cookie", required = false) cookie: String?,
        @RequestParam("new-password", required = false) newPassword: String?,     // 空为取消密码
        @RequestParam("old-password", required = false) oldPassword: String?      // 此密码为鉴权密码
    ): Result {
        log.info("api - [replace-user] $uid")
        if (AskUtils.isFieldEmpty(uid)) {
            return Result.err("参数错误", ERROR_PARAMETER)
        }
        if (!AskUtils.isStrUidEfficient(uid!!)) {
            return Result.err("uid 有误", ERROR_PARAMETER)
        }
        if (AskUtils.isPasswordCorrect(newPassword)) {
            return Result.err("密码过长", ERROR_PARAMETER)
        }
        userMapper?.let {
            if (!SqlUtils.isdUserExist(it, uid)) {
                return Result.err("用户不存在", ERROR_USER_EXIST)
            }
            val gameData = SqlUtils.selectGameUid(it, uid)!!
            if (gameData.password != null && gameData.password!!.isNotEmpty()) {
                if (!gameData.password.equals(oldPassword)) {
                    return Result.err("密码错误", ERROR_USER_PWS_ERR)
                }
            }

            val sqlCookieToken: String? = AskUtils.isStrCookieTokenEfficient(it, cookie)
            if (cookie != null && cookie.isNotEmpty()) {
                sqlCookieToken ?: return Result.err("cookie 无效", ERROR_COOKIE)
                if (sqlCookieToken == "1") return Result.err("cookie 未过期不用修改", ERROR_COOKIE)
            } else {
                gameData.password = newPassword
                it.updateById(gameData)
                return Result.ok(null, "密码修改成功")
            }


            // val uuid: String = AskUtils.getGameCookieUUid(cookie) ?: UUID.randomUUID().toString()
            val userInfo: JsonMiHoYoBack<JsonUserData>? = AskUtils.getUserInfo(
                AskInfoData(
                    gameData.uuid,
                    gameData.accountId,
                    cookie = cookie
                )
            )
            log.info("$userInfo")

            userInfo?.let { ud ->
                if (ud.retcode != 0) {
                    return Result.err("cookie 无效", ERROR_COOKIE)
                }
                // 有数据
                val game = ud.data!!.list[0]
                if (!SqlUtils.isdUserExist(it, game.game_uid)) {
                    return Result.err("cookie 对应的uid用户已存在", ERROR_USER_EXIST)
                }

                val sToken: String? = AskUtils.getCookieLoginTicket(cookie)?.let { login_ticket ->
                    AskUtils.getBbsSToken(login_ticket, gameData.accountId)
                }
                val uuid: String = AskUtils.getCookieUUid(cookie) ?: gameData.uuid
                gameData.apply {
                    this.uuid = uuid
                    region = game.region
                    cookieToken = sqlCookieToken
                    this.cookie = cookie
                    this.sToken = sToken ?: this.sToken
                    password = newPassword
                    isCookieAvailable = true
                }
                it.updateById(gameData)
                // 排行
                game.ranking = it.queryByIdRanking(gameData.id!!).rank
                log.info("排行 ${game.ranking}")


                val msg = sToken?.let {
                    "${game.nickname} 【每日签到添加成功[√] | 米游币签到添加成功[√]】"
                } ?: let {
                    "${game.nickname} 【每日签到添加成功[√] | 米游币签到添加失败[×]】"
                }
                return Result.ok(game, msg)
            }
            return Result.err("cookie 无效 :)", ERROR_COOKIE)
        }
        return Result.err("数据库连接失败", ERROR_SQL_CONNECT)
    }


    // 添加用户
    @PostMapping("/add-user")
    @ResponseBody
    fun addUser(
        @RequestParam("uid", required = false) uid: String?,
        @RequestParam("cookie", required = false) cookie: String?,
        @RequestParam("password", required = false) password: String?
    ): Result {
        log.info("api - [add-user] $uid")
        if (AskUtils.isFieldEmpty(uid, cookie)) {
            return Result.err("参数错误", ERROR_PARAMETER)
        }
        if (!AskUtils.isStrUidEfficient(uid!!)) {
            return Result.err("uid 有误", ERROR_PARAMETER)
        }
        if (AskUtils.isPasswordCorrect(password)) {
            return Result.err("密码过长", ERROR_PARAMETER)
        }
        cookie!!
        userMapper?.let {
            if (SqlUtils.isdUserExist(it, uid)) {
                return Result.err("用户已存在", ERROR_USER_EXIST)
            }
            val sqlCookieToken =
                AskUtils.isStrCookieTokenEfficient(it, cookie) ?: return Result.err("cookie 无效", ERROR_COOKIE)
            if (sqlCookieToken == "1") {
                return Result.err("cookie 已存在", ERROR_COOKIE)
            }
            val uuid: String = AskUtils.getCookieUUid(cookie) ?: UUID.randomUUID().toString()
            val userInfo: JsonMiHoYoBack<JsonUserData>? =
                AskUtils.getUserInfo(AskInfoData(uuid, cookie = cookie))
            // log.info(" -----------       ${userInfo?.data}")

            userInfo?.let { ud ->
                if (ud.retcode != 0) {
                    return Result.err("cookie 无效", ERROR_COOKIE)
                }
                // 有数据
                val game = ud.data!!.list[0]
                if (SqlUtils.isdUserExist(it, game.game_uid)) {
                    return Result.err("用户已存在", ERROR_USER_EXIST)
                }

                val cookieAccountId = AskUtils.getCookieAccountId(cookie)!!
                val sToken: String? = AskUtils.getCookieLoginTicket(cookie)?.let { login_ticket ->
                    AskUtils.getBbsSToken(login_ticket, cookieAccountId)
                }

                it.insert(
                    UserModer(
                        uid = game.game_uid,
                        region = game.region,
                        uuid,
                        cookieAccountId,
                        sToken,
                        cookieToken = sqlCookieToken,
                        cookie,
                        password
                    )
                )
                // 排行
                val selectGameUid = SqlUtils.selectGameUid(it, game.game_uid)
                game.ranking = it.queryByIdRanking(selectGameUid!!.id!!).rank
                log.info("排行 ${game.ranking}")

                val msg = sToken?.let {
                    game.addResults[JsonUserData.NameKey.BBS_SING.KEY] = JsonUserData.AddResults(true, "米游币任务添加成功")
                    "${game.nickname} 【每日签到添加成功[√] | 米游币签到添加成功[√]】"
                } ?: let {
                    game.addResults[JsonUserData.NameKey.BBS_SING.KEY] = JsonUserData.AddResults(false, "米游币任务添加失败")
                    "${game.nickname} 【每日签到添加成功[√] | 米游币签到添加失败[×]】"
                }
                return Result.ok(game, msg)
            }
            return Result.err("cookie 无效 :)", ERROR_COOKIE)
        }
        return Result.err("数据库连接失败", ERROR_SQL_CONNECT)
    }


    // 解绑/删除用户
    @GetMapping("/unbind-user")
    fun unbindUser(
        @RequestParam("uid", required = false) uid: String?,
        @RequestParam("password", required = false) password: String?
    ): Result {
        if (AskUtils.isFieldEmpty(uid))
            return Result.err("参数不能为空", ERROR_PARAMETER)
        if (!AskUtils.isStrUidEfficient(uid!!))
            return Result.err("uid 有误", ERROR_PARAMETER)
        log.info("api - [unbind-user] $uid")
        userMapper?.let {
            if (SqlUtils.isdUserExist(it, uid)) {
                val selectGameUid = SqlUtils.selectGameUid(it, uid)
                if (selectGameUid!!.password == null || selectGameUid.password!!.isEmpty()) {
                    it.deleteById(selectGameUid)
                    return Result.ok(null, "解绑完成!")
                }
                if (!selectGameUid.password.equals(password)) {
                    return Result.err("密码错误", ERROR_USER_PWS_ERR)
                }
                it.deleteById(selectGameUid)
                return Result.ok(null, "解绑完成!")
            } else {
                return Result.err("用户不存在", ERROR_USER_EXIST)
            }
        }
        return Result.err("数据库连接失败", ERROR_SQL_CONNECT)
    }


    // 查询签到奖励记录
    @GetMapping("/user-record")
    fun inquireUserRecord(
        @RequestParam("uid", required = false) uid: String?,
        @RequestParam("password", required = false) password: String?,
        @RequestParam("type", required = false) type: Array<Int>? = arrayOf(0),
        @RequestParam("order", required = false) order: Boolean = false // true 正序   false 倒序
    ): Result {
        if (AskUtils.isFieldEmpty(uid))
            return Result.err("参数不能为空", ERROR_PARAMETER)
        if (!AskUtils.isStrUidEfficient(uid!!))
            return Result.err("uid 有误", ERROR_PARAMETER)
        log.info("api - [inquire-user-record] $uid")

        recordMapper?.let {
            try {
                val selectGameUid = SqlUtils.selectGameUid(userMapper!!, uid)
                if (selectGameUid!!.password == null || selectGameUid.password!!.isEmpty()) {

                    val recordModerList = it.selectList(QueryWrapper<RecordModer>().apply {
                        eq("c_belong", uid)
                        if (order) {
                            orderByAsc("c_time")
                        } else {
                            orderByDesc("c_time")
                        }
                    })

                    return Result.ok(recordModerList)
                }
                if (!selectGameUid.password.equals(password)) {
                    return Result.err("密码错误", ERROR_USER_PWS_ERR)
                }

                val recordModerList = it.selectList(QueryWrapper<RecordModer>().apply {
                    eq("c_belong", uid)
                    if (order) {
                        orderByAsc("c_time")
                    } else {
                        orderByDesc("c_time")
                    }
                })

                //////////////////////////////
                return Result.ok(recordModerList)
            } catch (e: Exception) {
                println(e.message)
                return Result.err("没有记录！", ERROR)
            }
        }
        return Result.err("数据库连接失败", ERROR_SQL_CONNECT)
    }


}
