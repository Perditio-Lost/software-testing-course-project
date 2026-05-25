package com.example.controller;

import com.example.common.Result;
import com.example.common.dto.TestSubmitDTO;
import com.example.entity.Answer;
import com.example.entity.Score;
import com.example.service.AIAnalysisService;
import com.example.service.ScoreService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成绩信息前端请求接口
 */
@RestController
@RequestMapping("/score")
public class ScoreController {

    @Resource
    private ScoreService scoreService;
    
    @Resource
    private AIAnalysisService aiAnalysisService;

    /**
     * 新增
     */
    @PostMapping("/add")
    public Result add(@RequestBody TestSubmitDTO submitDTO) {
        scoreService.add(submitDTO);
        return Result.success();
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    public Result update(@RequestBody Score score) {
        scoreService.updateById(score);
        return Result.success();
    }

    /**
     * 单个删除
     */
    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        scoreService.deleteById(id);
        return Result.success();
    }

    /**
     * 批量删除
     */
    @DeleteMapping("/delete/batch")
    public Result delete(@RequestBody List<Integer> ids) {
        scoreService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 单个查询
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        Score score = scoreService.selectById(id);
        return Result.success(score);
    }
    @GetMapping("/selectAnswer/{id}")
    public Result selectAnswer(@PathVariable Integer id) {
        List<Answer> list = scoreService.selectAnswer(id);
        return Result.success(list);
    }

    /**
     * 查询所有
     */
    @GetMapping("/selectAll")
    public Result selectAll(Score score) {
        List<Score> list = scoreService.selectAll(score);
        return Result.success(list);
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result selectPage(Score score,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Score> pageInfo = scoreService.selectPage(score, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    /**
     * AI分析错题
     */
    @GetMapping("/aiAnalysis/{id}")
    public Result aiAnalysis(@PathVariable Integer id) {
        try {
            // 先获取成绩详情
            Score score = scoreService.selectById(id);
            if (score == null) {
                return Result.error("成绩记录不存在");
            }
            
            // 调用AI分析服务
            String analysisResult = aiAnalysisService.analyzeWrongQuestions(id, score);
            return Result.success(analysisResult);
        } catch (Exception e) {            return Result.error("AI分析失败：" + e.getMessage());
        }
    }

    /**
     * 根据考试ID查询学生成绩列表（用于按学生批阅）
     */
    @GetMapping("/selectByTest")
    public Result selectByTest(@RequestParam Integer testId,
                               @RequestParam(required = false) Integer clazzId,
                               @RequestParam(defaultValue = "1") Integer pageNum,
                               @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Score> pageInfo = scoreService.selectByTest(testId, clazzId, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    /**
     * 根据考试ID查询需要批阅的题目列表（用于按题目批阅）
     */
    @GetMapping("/selectQuestionsByTest")
    public Result selectQuestionsByTest(@RequestParam Integer testId,
                                        @RequestParam(required = false) Integer clazzId) {
        List<?> questionList = scoreService.selectQuestionsByTest(testId, clazzId);
        return Result.success(questionList);
    }

    /**
     * 根据题目ID查询所有学生的答案（用于按题目批阅）
     */
    @GetMapping("/selectAnswersByQuestion")
    public Result selectAnswersByQuestion(@RequestParam Integer testId,
                                         @RequestParam Integer questionId) {
        Object result = scoreService.selectAnswersByQuestion(testId, questionId);
        return Result.success(result);
    }

    /**
     * 保存单个题目的评分（用于按题目批阅）
     */
    @PostMapping("/saveQuestionScore")
    public Result saveQuestionScore(@RequestBody java.util.Map<String, Object> params) {
        scoreService.saveQuestionScore(params);
        return Result.success();
    }

    /**
     * 查询异常试卷（作弊记录）
     */
    @GetMapping("/selectAbnormal")
    public Result selectAbnormal(@RequestParam(required = false) String testName,
                                 @RequestParam(required = false) String studentName,
                                 @RequestParam(required = false) String testPaperName,
                                 @RequestParam(required = false) Integer teacherId,
                                 @RequestParam(defaultValue = "1") Integer pageNum,
                                 @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Score> pageInfo = scoreService.selectAbnormal(testName, studentName, testPaperName, teacherId, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    /**
     * 导出已批改的成绩Excel
     */
    @GetMapping("/exportGradedScores")
    public void exportGradedScores(@RequestParam Integer clazzId,
                                   @RequestParam Integer testId,
                                   HttpServletResponse response) {
        try {
            scoreService.exportGradedScores(clazzId, testId, response);
        } catch (Exception e) {            throw new RuntimeException("导出Excel失败：" + e.getMessage());
        }
    }

}
