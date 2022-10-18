package com.pkpk.genshin.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.pkpk.genshin.GenshinApplication
import com.pkpk.genshin.controller.GenshinController
import com.pkpk.genshin.getRunApplication
import com.pkpk.genshin.utils.AskUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Component
import java.io.File


@Component
class StartContextTask : ApplicationContextAware {

    @Autowired
    private var json: ObjectMapper? = null

    private val property = "${System.getProperty("user.dir")}${File.separator}reward-list.json"

    private val log: Logger = LoggerFactory.getLogger(GenshinController::class.java)


    override fun setApplicationContext(applicationContext: ApplicationContext) {
        println(
            "\n" +
                    "            _  _   _      __   __      _____  _\n" +
                    "           (_)| | | |     \\ \\ / /     /  ___|(_)              \n" +
                    " _ __ ___   _ | |_| |  ___ \\ V / ___  \\ `--.  _   __ _  _ __  \n" +
                    "| '_ ` _ \\ | ||  _  | / _ \\ \\ / / _ \\  `--. \\| | / _` || '_ \\ \n" +
                    "| | | | | || || | | || (_) || || (_) |/\\__/ /| || (_| || | | |\n" +
                    "|_| |_| |_||_|\\_| |_/ \\___/ \\_/ \\___/ \\____/ |_| \\__, ||_| |_|\n" +
                    "                                                  __/ |       \n" +
                    "                                                 |___/"
        )



        val confFile = File("${System.getProperty("user.dir")}${File.separator}conf.json")
        if (!confFile.exists()) {
            log.error("=================== 配置文件不存在！ ===================")
            getRunApplication().close()
            return
        }
        try {
            AskUtils.confData = json!!.readValue(confFile)
        }catch (e: Exception){
            log.error("=================== 配置文件解析出错 ===================")
            getRunApplication().close()
        }
        GenshinSignTask.writeSignAwardsJson(log, json, property)
    }


}