package com.example.mapper;

import com.example.entity.Clazz;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ClazzMapper {

    int insert(Clazz clazz);

    int updateById(Clazz clazz);

    int deleteById(Integer id);

    Clazz selectById(Integer id);

    List<Clazz> selectAll(Clazz clazz);

    @Select("select * from clazz")
    List<Clazz> selectAllClazz();
}
