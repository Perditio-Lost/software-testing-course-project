package com.example.service;

import com.example.entity.SystemLog;
import com.example.mapper.SystemLogMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SystemLogService {

    @Resource
    private SystemLogMapper systemLogMapper;

    /**
     * 新增日志
     */
    public void add(SystemLog systemLog) {
        systemLogMapper.insert(systemLog);
    }

    /**
     * 分页查询
     */
    public PageInfo<SystemLog> selectPage(SystemLog systemLog, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<SystemLog> list = systemLogMapper.selectAll(systemLog);
        return PageInfo.of(list);
    }
}
