package com.pkpk.genshin

import com.fasterxml.jackson.databind.ObjectMapper
import com.pkpk.genshin.mode.AskInfoData
import org.mybatis.spring.annotation.MapperScan
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.io.File

@SpringBootApplication
@MapperScan("com.pkpk.genshin.mapper")
//@PropertySource(value = ["classpath:business.properties"], encoding = "UTF-8")
class GenshinApplication
fun main(args: Array<String>) {
    runApplication<GenshinApplication>(*args)
}
