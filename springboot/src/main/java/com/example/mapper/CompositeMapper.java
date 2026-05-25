package com.example.mapper;

import com.example.entity.Composite;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface CompositeMapper {

    @Select("select * from composite where question_father_id = #{questionFatherId} order by sequence")
    List<Composite> selectByFatherId(Integer questionFatherId);

    @Select("select * from composite where question_son_id = #{questionSonId}")
    List<Composite> selectBySonId(Integer questionSonId);

    int insert(Composite composite);

    void updateById(Composite composite);

    @Delete("delete from composite where question_father_id = #{questionFatherId}")
    void deleteByFatherId(Integer questionFatherId);

    @Delete("delete from composite where id = #{id}")
    void deleteById(Integer id);
}
