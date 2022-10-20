package com.pkpk.genshin.utils

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.pkpk.genshin.mapper.UserMapper
import com.pkpk.genshin.mode.UserModer

object SqlUtils {

    /**
     * 用户是否存在
     * @param uid uid
     */
    fun isdUserExist(userMapper: UserMapper, uid: String): Boolean {
        val selectList = queryByField(userMapper, "uid", uid)
        return selectList != null && selectList.isNotEmpty()
    }

    /**
     * 查询数据库米游社 cookie_token 是否存在
     * @param cookieToken cookie_token
     */
    fun isCookieToken(userMapper: UserMapper, cookieToken: String): Boolean {
        val selectList = queryByField(userMapper, "cookie_token", cookieToken)
        return selectList != null && selectList.isNotEmpty()
    }




    /**
     * 用户cookie 是否过期
     * @param uid uid
     */
    fun isUserCookieExpired(uid: String): Boolean {
        return false
    }




    /**
     * 根据game_uid 查询数据
     * @param uid uid
     * @return UserModer
     */
    fun selectGameUid(userMapper: UserMapper, uid: String): UserModer? = try {
        userMapper.selectOne(QueryWrapper<UserModer>().apply {
            eq("uid", uid)
        })
    } catch (e: Exception) {
        null
    }

    /**
     * 根据字段查询 内容
     */
    private fun queryByField(userMapper: UserMapper, field: String, fieldValue: String): List<UserModer>? {
        return userMapper.selectList(QueryWrapper<UserModer>().apply {
            eq(field, fieldValue)
        })
    }



    /**
     * 更新用户数据
     *
     */
    // fun upUserData(userModer: UserModer)


}