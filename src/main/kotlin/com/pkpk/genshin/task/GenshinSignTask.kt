package com.pkpk.genshin.task

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.pkpk.genshin.controller.GenshinController
import com.pkpk.genshin.mapper.RecordMapper
import com.pkpk.genshin.mapper.UserMapper
import com.pkpk.genshin.mode.*
import com.pkpk.genshin.utils.AskUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.io.File
import javax.annotation.Resource
import kotlin.random.Random


// 在线Cron表达式生成器 https://www.matools.com/cron/
@Configuration
@EnableScheduling
@Component
class GenshinSignTask {


    private val property = "${System.getProperty("user.dir")}${File.separator}reward-list.json"

    private val log: Logger = LoggerFactory.getLogger(GenshinController::class.java)

    @Autowired
    private var json: ObjectMapper? = null


    // 用户数据库
    @Resource
    private val userMapper: UserMapper? = null

    @Resource
    private val recordMapper: RecordMapper? = null


    companion object {
        // 签到奖励列表
        var signAwards: JsonMiHoYoBack<JsonSignRewardData>? = null

        // 帖子列表
        var postList: JsonMiHoYoBack<BbsPostListData>? = null

        fun writeSignAwardsJson(log: Logger, json: ObjectMapper?, path: String) {
            val signAwards = AskUtils.getSignAwards()
            if (signAwards == null || signAwards.retcode != 0) {
                log.warn("原神奖励列表获取失败！！")
                return
            }
            log.info("原神奖励列表获取成功！！")
            GenshinSignTask.signAwards = signAwards
            json?.writeValue(File(path), signAwards)
        }


        fun getRandomPostList(): BbsPostListData.PostListData? =
            postList?.let {
                it.data ?: return null
                // println("${it.data}")
                if (it.data.list.isEmpty()) return null
                return it.data.list[Random.nextInt(0, it.data.list.size)]
            }

    }


    /**
     * 原神签到任务
     *
     * 签到计划执行时间(每天00:30:00)
     */
    @Scheduled(cron = "0 30 0 * * ?")
    fun startGenshinSignTask() {
        log.info("=========== 开始任务 ===========")

        userMapper ?: return log.error("userMapper null")

        recordMapper ?: return log.error("recordMapper null")

        val userList = userMapper.selectList(null)

        if (userList == null || userList.isEmpty()) {
            log.error("没有待执行用户")
            return
        }

        // 奖励列表
        if (signAwards == null) {
            json?.let {
                signAwards =
                    it.readValue(File(property), object : TypeReference<JsonMiHoYoBack<JsonSignRewardData>>() {})
            }
        }

        userList.forEach {
            val recordModer = RecordModer(it.uid)
            recordMapper.insert(recordModer)

            if (it.isLock) {
                putSqlErrMsg(recordModer, "已拉黑！")
                return
            }

            // game sign
            startGameRewardTask(it, recordModer)
            // bbs  sign
            startBbsRewardTask(it, recordModer)
        }

        log.info("=========== 结束任务 ===========")
    }


    /**
     * 游戏奖励任务
     *
     */
    private fun startGameRewardTask(user: UserModer, record: RecordModer) {
        recordMapper!!
        userMapper!!

        if (!user.isCookieAvailable) {
            putSqlErrMsg(record, "cookie 已失效！")
            return
        }

        val askInfoData = AskInfoData(
            user.uuid,
            user.accountId,
            user.sToken,
            user.cookie,
            PostSignData(user.region, user.uid)
        )
        val userSignInfo = AskUtils.getUserSignInfo(askInfoData) ?: let {
            putGameSqlErrMsg(record, "网络错误！")
            return
        }
        // 防止风控  暂停
        Thread.sleep(Random.nextLong(2000, 3000))


        if (userSignInfo.retcode != 0) {
            user.isCookieAvailable = false
            userMapper.updateById(user)
            putGameSqlErrMsg(record, "cookie 已失效！")
            return
        }
        if (userSignInfo.data!!.is_sign) {
            putGameSqlErrMsg(record, "你已手动签到！")
            return
        }
        // userSignInfo.data!!
        // 签到
        val postUserSignIn = AskUtils.postUserSignIn(askInfoData) ?: let {
            putGameSqlErrMsg(record, "网络错误！")
            return
        }
         log.info("$postUserSignIn")

        // 防止风控  暂停
        Thread.sleep(Random.nextLong(2000, 3000))

        if (postUserSignIn.data == null || postUserSignIn.retcode != 0) {
            putGameSqlErrMsg(record, "签到失败！")
            return
        }

        if (postUserSignIn.data.success != 0) {
            putGameSqlErrMsg(record, "触发验证码！")
            return
        }

        var nameRecord = "签到奖励获取失败"
        var quantityRecord = 0
        if ((signAwards != null
                    && signAwards!!.data != null
                    && signAwards!!.data!!.awards.isNotEmpty())
            && userSignInfo.data.total_sign_day < signAwards!!.data!!.awards.size
        ) {
            nameRecord = signAwards!!.data!!.awards[userSignInfo.data.total_sign_day].name
            quantityRecord = signAwards!!.data!!.awards[userSignInfo.data.total_sign_day].cnt
        }
        putGameSqlOkMsg(record, nameRecord, quantityRecord)
    }


    /**
     * 米游社任务
     */
    private fun startBbsRewardTask(user: UserModer, record: RecordModer) {
        recordMapper!!
        userMapper!!


        if (!user.isCookieAvailable) {
            putSqlErrMsg(record, "cookie 已失效！")
            return
        }
        user.sToken ?: let {
            putBbsSqlErrMsg(record, "无 sToken 米游社不进行签到")
            return
        }


        // 今日帮你获取的米游币
        var bbsAward = 0

        BbsTaskType.TASK_SIGN.TASK_OK = false
        BbsTaskType.TASK_BROWSE.TASK_OK = false
        BbsTaskType.TASK_LIKE.TASK_OK = false
        BbsTaskType.TASK_SHARE.TASK_OK = false


        val askInfoData = AskInfoData(
            user.uuid,
            user.accountId,
            user.sToken,
            user.cookieToken,
            String(),
        )



        // 获取帖子列表
        postList ?: let {
            postList = AskUtils.getBbsForumPostList() ?: let {
                putBbsSqlErrMsg(record, "帖子-网络错误！")
                return
            }
            if (postList!!.retcode != 0) {
                postList = null
                putBbsSqlErrMsg(record, "帖子获取失败！")
                return
            }
        }

        // 防止风控  暂停
        Thread.sleep(Random.nextLong(2000, 3000))

        val bbsTaskList = AskUtils.getBbsTaskList(askInfoData) ?: let {
            putBbsSqlErrMsg(record, "任务列表-网络错误！")
            return
        }

        if (bbsTaskList.retcode != 0) {
            user.isCookieAvailable = false
            userMapper.updateById(user)
            putBbsSqlErrMsg(record, "cookie 已失效！")
            return
        }

        bbsTaskList.data!!

        if (bbsTaskList.data.can_get_points <= 0) {
            putBbsSqlErrMsg(record, "今日任务你已手动全部完成！")
            return
        }

        // 签到任务
        run {
            val taskSignState = getBbsUserTaskStateByMissionId(bbsTaskList.data, BbsTaskType.TASK_SIGN)
            if (taskSignState == null || taskSignState.process != 1) {
                val postBbsSign = AskUtils.postBbsSign(AskInfoData(
                    user.uuid,
                    user.accountId,
                    user.sToken,
                    user.cookieToken,
                    null,
                ))
                if (postBbsSign?.data == null || postBbsSign.retcode != 0) {
                    putBbsSqlErrMsg(record, "签到任务失败！")
                } else {
                    // 根据已获取 和 未获取 计算出今日签到获取的米游币
                    bbsAward += BbsTaskType.TASK_SIGN.TASK_REWARD - (bbsTaskList.data.today_total_points - (bbsTaskList.data.already_received_points + bbsTaskList.data.can_get_points))
                    // putBbsSqlErrMsg(record, "签到任务成功！")
                    BbsTaskType.TASK_SIGN.TASK_OK = true
                }
            } else {
                putBbsSqlErrMsg(record, "签到任务你已手动完成！")
            }

            // 防止风控  暂停 5 ~ 8 s
            Thread.sleep(Random.nextLong(5000, 8000))
        }


        // 浏览帖子
        run {
            val taskBrowseState = getBbsUserTaskStateByMissionId(bbsTaskList.data, BbsTaskType.TASK_BROWSE)

            val taskBrowseCount = if (taskBrowseState == null)
                BbsTaskType.TASK_BROWSE.TASK_COUNT
            else
                BbsTaskType.TASK_BROWSE.TASK_COUNT - taskBrowseState.happened_times

            if (taskBrowseCount == 0) {
                putBbsSqlErrMsg(record, "浏览帖子任务你已手动完成！")
                BbsTaskType.TASK_BROWSE.TASK_OK = false
                return@run
            }

            var bbsLookOk = 0

            for (i in 1..taskBrowseCount) {
                val randomPostList = getRandomPostList() ?: continue
                val bbsLook = AskUtils.getBbsLook(askInfoData.apply { sinData = randomPostList.post.post_id })
                if (bbsLook == null) {
                    putBbsSqlErrMsg(record, "第${i}次浏览帖子--网络出错！")
                    continue
                }
                if (bbsLook.retcode != 0) {
                    putBbsSqlErrMsg(record, "第${i}次浏览帖子--失败！")
                    continue
                }
                // putBbsSqlErrMsg(record, "第${i}次浏览帖子--成功！")
                bbsLookOk++
                // 防止风控  随机暂停
                Thread.sleep(Random.nextLong(3000, 4000))
            }

            BbsTaskType.TASK_BROWSE.TASK_OK = taskBrowseCount == bbsLookOk

            if (BbsTaskType.TASK_BROWSE.TASK_OK) {
                bbsAward += BbsTaskType.TASK_BROWSE.TASK_REWARD
            }
        }

        // 点赞
        run {
            val taskLikeState = getBbsUserTaskStateByMissionId(bbsTaskList.data, BbsTaskType.TASK_LIKE)

            val taskLikeCount = if (taskLikeState == null)
                BbsTaskType.TASK_LIKE.TASK_COUNT
            else
                BbsTaskType.TASK_LIKE.TASK_COUNT - taskLikeState.happened_times

            if (taskLikeCount == 0) {
                putBbsSqlErrMsg(record, "点赞帖子任务你已手动完成！")
                BbsTaskType.TASK_LIKE.TASK_OK = false
                return@run
            }
            var bbsVoteOk = 0

            for (i in 1..taskLikeCount) {
                val randomPostList = getRandomPostList() ?: continue
                val bbsVote = AskUtils.postBbsVote(
                    AskInfoData(
                        user.uuid,
                        user.accountId,
                        user.sToken,
                        user.cookieToken,
                        PostBbsLikeData(randomPostList.post.post_id),
                    )
                )
                if (bbsVote == null) {
                    putBbsSqlErrMsg(record, "第${i}次点赞--网络出错！")
                    continue
                }
                if (bbsVote.retcode != 0) {
                    putBbsSqlErrMsg(record, "第${i}次点赞--失败！")
                    continue
                }
                // putBbsSqlErrMsg(record, "第${i}次点赞--成功！")
                bbsVoteOk++
                // 防止风控  随机暂停
                Thread.sleep(Random.nextLong(3000, 4000))
            }
            BbsTaskType.TASK_LIKE.TASK_OK = taskLikeCount == bbsVoteOk
            if (BbsTaskType.TASK_LIKE.TASK_OK) {
                bbsAward += BbsTaskType.TASK_LIKE.TASK_REWARD
            }

        }


        // 分享
        run {
            val taskShareState = getBbsUserTaskStateByMissionId(bbsTaskList.data, BbsTaskType.TASK_SHARE)
            if (taskShareState == null || taskShareState.process != 1) {
                val randomPostList = getRandomPostList() ?: let {
                    putBbsSqlErrMsg(record, "分享任务失败-！")
                    return@run
                }
                val getBbsShare = AskUtils.getBbsShare(askInfoData.apply {
                    sinData = randomPostList.post.post_id
                })
                if (getBbsShare?.data == null || getBbsShare.retcode != 0) {
                    putBbsSqlErrMsg(record, "分享任务失败！")
                } else {
                    bbsAward += BbsTaskType.TASK_SHARE.TASK_REWARD
                    // putBbsSqlErrMsg(record, "签到任务成功！")
                    BbsTaskType.TASK_SHARE.TASK_OK = true
                }
            } else {
                putBbsSqlErrMsg(record, "分享任务你已手动完成！")
            }
        }


        if (bbsAward != 0) {
            if (BbsTaskType.TASK_SIGN.TASK_OK &&
                BbsTaskType.TASK_BROWSE.TASK_OK &&
                BbsTaskType.TASK_LIKE.TASK_OK &&
                BbsTaskType.TASK_SHARE.TASK_OK
            ) {
                putBbsSqlOkMsg(record, bbsAward, "论坛任务全部完成！")
                return
            }
            putBbsSqlOkMsg(record, bbsAward, "论坛任务部分完成！", false)
        }
    }



    /**
     * 基础错误
     */
    fun putSqlErrMsg(record: RecordModer, msg: String) {
        recordMapper?.apply {
            updateById(record.apply {
                isGameOk = false
                isBbsOk = false
                logoMsg = msg
            })
        }
    }

    /**
     * 游戏奖励签到错误
     */
    fun putGameSqlErrMsg(record: RecordModer, msg: String) {
        recordMapper?.apply {
            updateById(record.apply {
                isGameOk = false
                logoMsg = if (logoMsg == null || logoMsg!!.isEmpty()) {
                    "[game]($msg)"
                } else {
                    "$logoMsg, [game]($msg)"
                }
            })
        }
    }


    /**
     * 论坛签到错误
     */
    fun putBbsSqlErrMsg(record: RecordModer, msg: String) {
        recordMapper?.apply {
            updateById(record.apply {
                isBbsOk = false
                logoMsg = if (logoMsg == null || logoMsg!!.isEmpty()) {
                    "[bbs]($msg)"
                } else {
                    "$logoMsg, [bbs]($msg)"
                }
            })
        }
    }


    /**
     * 游戏奖励签到成功
     */
    fun putGameSqlOkMsg(record: RecordModer, nameRecord: String, quantityRecord: Int, msg: String = "签到完成") {
        recordMapper?.apply {
            recordMapper.updateById(record.apply {
                gameName = nameRecord
                gameQuantity = quantityRecord
                isGameOk = true
                logoMsg = if (logoMsg == null || logoMsg!!.isEmpty()) {
                    "[game]($msg)"
                } else {
                    "$logoMsg, [game]($msg)"
                }
            })
        }
    }


    /**
     * 论坛奖励签到成功
     */
    fun putBbsSqlOkMsg(record: RecordModer,
                       quantityRecord: Int,
                       msg: String, isBbsOk: Boolean = true) {
        recordMapper?.apply {
            recordMapper.updateById(record.apply {
                bbsQuantity = quantityRecord
                this.isBbsOk = isBbsOk
                logoMsg = if (logoMsg == null || logoMsg!!.isEmpty()) {
                    "[bbs]($msg)"
                } else {
                    "$logoMsg, [bbs]($msg)"
                }
            })
        }
    }


    /**
     * 根据任务id 获取任务详情
     * @return 为 null 时 这个任务没做
     */
    fun getBbsUserTaskStateByMissionId(
        bbsUserTaskListData: BbsUserTaskListData,
        bbsTaskType: BbsTaskType
    ): BbsUserTaskListData.State? {
        bbsUserTaskListData.states.forEach {
            if (it.mission_id == bbsTaskType.TASK_ID) return it
        }
        return null
    }


    /**
     * 奖励列表更新
     * 奖励列表更新频率(每月1日00:00:00, 自动执行一次)
     */
    @Scheduled(cron = "0 0 0 1 * ?")
    fun startSignRewardList() {
        log.error("=========== 获取签到奖励数据 ===========")
        writeSignAwardsJson(log, json, property)
    }


}