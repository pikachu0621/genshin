package com.pkpk.genshin

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.pkpk.genshin.mode.ConfData
import com.pkpk.genshin.task.GenshinSignTask
import com.pkpk.genshin.utils.AskUtils
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.File
import kotlin.math.log

@SpringBootTest
class GenshinApplicationTests {


    // json
    // private var mapper: ObjectMapper = ObjectMapper()

    // 配置文件
    // private val confJson = "${System.getProperty("user.dir")}${File.separator}conf.json"

    // private val confData = mapper.readValue<ConfData>(confJson)

    // @Autowired
    // var genshinSignTask: GenshinSignTask? = null

    @Test
    fun contextLoads() {
        // val confJson = "${System.getProperty("user.dir")}${File.separator}conf.json"
        // genshinSignTask?.startGenshinSignTask()
        // println(confJson)
       /* val listGameKeyData = AskUtils.getListGameKeyData(2)
        println(listGameKeyData)*/

        // genshinSignTask?.startGenshinSignTask()
    }

}
