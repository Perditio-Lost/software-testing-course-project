package com.example.controller;

import com.example.common.Result;
import com.example.service.ExamDraftService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 考试答题草稿Controller
 */
@RestController
@RequestMapping("/draft")
public class ExamDraftController {

    @Resource
    private ExamDraftService examDraftService;

    /**
     * 保存或更新草稿
     */
    @PostMapping("/save")
    public Result save(@RequestBody Map<String, Object> params) {
        Object testIdObj = params.get("testId");
        Integer testId = testIdObj instanceof Integer ? (Integer) testIdObj : Integer.valueOf(testIdObj.toString());
        String draftData = (String) params.get("draftData");
        examDraftService.saveOrUpdate(testId, draftData);
        return Result.success();
    }

    /**
     * 获取草稿
     */
    @GetMapping("/get/{testId}")
    public Result get(@PathVariable Integer testId) {
        String draftData = examDraftService.getDraft(testId);
        return Result.success(draftData);
    }

    /**
     * 删除草稿
     */
    @DeleteMapping("/clear/{testId}")
    public Result clear(@PathVariable Integer testId) {
        examDraftService.deleteDraft(testId);
        return Result.success();
    }
}
