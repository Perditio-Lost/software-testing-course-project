package com.example.controller;

import com.alibaba.fastjson2.JSONObject;
import com.example.common.Result;
import com.example.entity.Question;
import com.example.service.AIAnalysisService;
import com.example.service.QuestionService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.List;

/**
 * 题目信息前端请求接口
 */
@RestController
@RequestMapping("/question")
public class QuestionController {

    @Resource
    private QuestionService questionService;

    @Resource
    private AIAnalysisService aiAnalysisService;

    /**
     * 新增
     */
    @PostMapping("/add")
    public Result add(@RequestBody Question question) {
        questionService.add(question);
        return Result.success();
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    public Result update(@RequestBody Question question) {
        questionService.updateById(question);
        return Result.success();
    }

    /**
     * 单个删除
     */
    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        questionService.deleteById(id);
        return Result.success();
    }

    /**
     * 批量删除
     */
    @DeleteMapping("/delete/batch")
    public Result delete(@RequestBody List<Integer> ids) {
        questionService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 单个查询
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        Question question = questionService.selectById(id);
        return Result.success(question);
    }

    /**
     * 查询所有
     */
    @GetMapping("/selectAll")
    public Result selectAll(Question question) {
        List<Question> list = questionService.selectAll(question);
        return Result.success(list);
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result selectPage(Question question,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Question> pageInfo = questionService.selectPage(question, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    /**
     * Excel批量导入题目
     */
    @PostMapping("/importQuestions")
    public Result importQuestions(@RequestParam("file") MultipartFile file) {
        try {
            String result = questionService.importQuestionsFromExcel(file);
            return Result.success(result);
        } catch (Exception e) {            return Result.error("导入失败：" + e.getMessage());
        }
    }

    /**
     * AI识别题目照片
     */
    @PostMapping("/recognizeQuestion")
    public Result recognizeQuestion(@RequestParam("file") MultipartFile file) {
        try {
            // 将图片转换为base64
            byte[] bytes = file.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(bytes);
            
            // 调用AI识别服务
            JSONObject result = aiAnalysisService.recognizeQuestion(base64Image);
            
            // 检查是否有错误
            if (result.containsKey("error")) {
                return Result.error(result.getString("error"));
            }
            
            return Result.success(result);
        } catch (Exception e) {            return Result.error("识别失败：" + e.getMessage());
        }
    }

}
