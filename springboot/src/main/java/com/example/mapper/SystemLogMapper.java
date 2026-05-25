package com.example.mapper;

import com.example.entity.SystemLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SystemLogMapper {
    
    /**
     * 新增日志
     */
    void insert(SystemLog systemLog);
    
    /**
     * 查询所有日志
     */
    List<SystemLog> selectAll(SystemLog systemLog);
}
