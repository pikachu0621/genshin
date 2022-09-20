package com.pkpk.genshin.mapper
import com.baomidou.mybatisplus.core.mapper.BaseMapper
import com.pkpk.genshin.mode.Ranking
import com.pkpk.genshin.mode.UserModer
import org.apache.ibatis.annotations.Select

interface UserMapper : BaseMapper<UserModer>{

    @Select("SELECT * FROM (SELECT a.id, (@rank:=@rank+1) as rank FROM user a,(select (@rank:=0)) b ORDER BY a.id ) c WHERE c.id = \${id}")
    fun queryByIdRanking(id: Long): Ranking

}