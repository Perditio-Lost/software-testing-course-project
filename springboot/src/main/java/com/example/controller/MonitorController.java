package com.example.controller;

import com.example.common.Result;
import com.example.service.MonitorService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 考试监控接口 - 人脸识别验证
 */
@RestController
@RequestMapping("/monitor")
public class MonitorController {

    @Resource
    private MonitorService monitorService;

    /**
     * 人脸验证
     * 
     * @param params 包含 examId, studentId, imageData
     */
    @PostMapping("/faceVerify")
    public Result faceVerify(@RequestBody Map<String, Object> params) {
        try {
            String examId = String.valueOf(params.get("examId"));
            String studentId = String.valueOf(params.get("studentId"));
            String imageData = (String) params.get("imageData");

            Map<String, Object> result = monitorService.verifyFace(examId, studentId, imageData);
            return Result.success(result);
        } catch (Exception e) {            return Result.error("人脸验证失败: " + e.getMessage());
        }
    }

    /**
     * 报告考试异常
     */
    @PostMapping("/reportAbnormal")
    public Result reportAbnormal(@RequestBody Map<String, Object> params) {
        try {
            String examId = String.valueOf(params.get("examId"));
            String studentId = String.valueOf(params.get("studentId"));
            Integer warningCount = (Integer) params.get("warningCount");
            String reason = (String) params.get("reason");

            monitorService.reportAbnormal(examId, studentId, warningCount, reason);
            return Result.success();
        } catch (Exception e) {            return Result.error("报告异常失败: " + e.getMessage());
        }
    }
}
