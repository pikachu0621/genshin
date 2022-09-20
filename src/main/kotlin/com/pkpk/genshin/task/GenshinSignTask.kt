package com.pkpk.genshin.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.pkpk.genshin.controller.GenshinController
import com.pkpk.genshin.mapper.RecordMapper
import com.pkpk.genshin.mapper.UserMapper
import com.pkpk.genshin.mode.AskInfoData
import com.pkpk.genshin.mode.JsonSignReward
import com.pkpk.genshin.mode.PostSignData
import com.pkpk.genshin.mode.RecordModer
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


    companion object{
        // 签到奖励列表
        var signAwards: JsonSignReward? = null

        fun writeSignAwardsJson(log: Logger, json: ObjectMapper?, path: String){
            val signAwards = AskUtils.getSignAwards()
            if (signAwards == null) {
                log.warn("原神奖励列表获取失败！！")
                return
            }
            log.info("原神奖励列表获取成功！！")
            GenshinSignTask.signAwards = signAwards
            json?.writeValue(File(path), signAwards)
        }
    }




    /**
     * 原神签到任务
     *
     * 签到计划执行时间(每天00:30:00)
     */
    @Scheduled(cron = "0 30 0 * * ?")
    fun startGenshinSignTask(){
        log.error("=========== 开始签到 ===========")
        // 奖励列表
        if (signAwards == null){
             json?.let {
                 signAwards = it.readValue(File(property), JsonSignReward::class.java)
            }
        }
        userMapper ?: return log.error("userMapper null")

        recordMapper?: return log.error("recordMapper null")

        val userList = userMapper.selectList(null)

        if (userList == null || userList.isEmpty()) {
            log.error("没有签到用户")
            return
        }
        userList.forEach {
            if (it.isLock) {
                recordMapper.insert(RecordModer(it.uid, "该账户已拉黑！"))
                return@forEach
            }
            if (!it.isCookieAvailable) {
                recordMapper.insert(RecordModer(it.uid, "cookie 已失效， 请更新！"))
                return@forEach
            }
            val askInfoData = AskInfoData(it.cookie, it.uuid, PostSignData(it.region, it.uid))
            val userSignInfo = AskUtils.getUserSignInfo(askInfoData)

            if (userSignInfo == null) {
                recordMapper.insert(RecordModer(it.uid, "网络错误！"))
                return@forEach
            }
            if (userSignInfo.retcode != 0) {
                it.isCookieAvailable = false
                userMapper.updateById(it)
                userSignInfo.message = userSignInfo.message.replace("尚未登录", "cookie 已失效！")
                recordMapper.insert(RecordModer(it.uid, userSignInfo.message))
                return@forEach
            }
            if(userSignInfo.data!!.is_sign){
                recordMapper.insert(RecordModer(it.uid, "你已手动签到！"))
                return@forEach
            }


            // 签到
            val postUserSignIn = AskUtils.postUserSignIn(askInfoData)
            if (postUserSignIn == null) {
                recordMapper.insert(RecordModer(it.uid, "网络错误！"))
                return@forEach
            }
            if (postUserSignIn.retcode != 0) {
                recordMapper.insert(RecordModer(it.uid, userSignInfo.message))
                return@forEach
            }

            var nameRecord = "签到奖励获取失败"
            var quantityRecord = 0
            if ((signAwards != null
                        && signAwards!!.data != null
                        && signAwards!!.data!!.awards.isNotEmpty())
                && userSignInfo.data.total_sign_day < signAwards!!.data!!.awards.size){
                nameRecord = signAwards!!.data!!.awards[userSignInfo.data.total_sign_day].name
                quantityRecord = signAwards!!.data!!.awards[userSignInfo.data.total_sign_day].cnt
            }
            recordMapper.insert(RecordModer(it.uid, nameRecord, quantityRecord, true, "签到成功！"))
        }

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

   /* @Scheduled(cron = "59 * * * * ?")
    fun startTestTask(){
        log.info("=========== test task 执行 ===========")
        startGenshinSignTask()
    }*/


}