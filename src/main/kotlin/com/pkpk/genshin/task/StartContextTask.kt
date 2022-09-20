package com.pkpk.genshin.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.pkpk.genshin.controller.GenshinController
import com.pkpk.genshin.mode.AskInfoData
import com.pkpk.genshin.mode.JsonSignReward
import com.pkpk.genshin.utils.AskUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component
import java.io.File

@Component
class StartContextTask : ApplicationContextAware {

    private val property = "${System.getProperty("user.dir")}${File.separator}reward-list.json"

    private val log: Logger = LoggerFactory.getLogger(GenshinController::class.java)

    @Autowired
    private var json: ObjectMapper? = null

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
        GenshinSignTask.writeSignAwardsJson(log, json, property)
    }


}