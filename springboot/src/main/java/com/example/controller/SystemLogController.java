package com.example.controller;

import com.example.common.Result;
import com.example.entity.SystemLog;
import com.example.service.SystemLogService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统日志接口
 */
@RestController
@RequestMapping("/systemLog")
public class SystemLogController {

    @Resource
    private SystemLogService systemLogService;

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result selectPage(SystemLog systemLog,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize,
                             @RequestParam(required = false) String userName) {
        // 如果传入了userName参数，设置到systemLog对象中
        if (userName != null && !userName.trim().isEmpty()) {
            systemLog.setUserName(userName);
        }
        PageInfo<SystemLog> pageInfo = systemLogService.selectPage(systemLog, pageNum, pageSize);
        return Result.success(pageInfo);
    }
}
