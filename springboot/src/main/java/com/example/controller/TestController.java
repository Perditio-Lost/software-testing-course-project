package com.example.controller;

import com.example.common.Result;
import com.example.entity.Test;
import com.example.service.TestService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 考试信息前端请求接口（替代原ExamPlan）
 */
@RestController
@RequestMapping("/test")
public class TestController {

    @Resource
    private TestService testService;

    /**
     * 新增
     */
    @PostMapping("/add")
    public Result add(@RequestBody Test test) {
        testService.add(test);
        return Result.success();
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    public Result update(@RequestBody Test test) {
        testService.updateById(test);
        return Result.success();
    }

    /**
     * 单个删除
     */
    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        testService.deleteById(id);
        return Result.success();
    }

    /**
     * 批量删除
     */
    @DeleteMapping("/delete/batch")
    public Result delete(@RequestBody List<Integer> ids) {
        testService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 单个查询
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        Test test = testService.selectById(id);
        return Result.success(test);
    }

    /**
     * 查询所有
     */
    @GetMapping("/selectAll")
    public Result selectAll(Test test) {
        List<Test> list = testService.selectAll(test);
        return Result.success(list);
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result selectPage(Test test,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Test> pageInfo = testService.selectPage(test, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    /**
     * 根据学生ID分页查询该学生班级的考试
     */
    @GetMapping("/selectPageByStudent")
    public Result selectPageByStudent(@RequestParam Integer studentId,
                                     @RequestParam(required = false) String courseName,
                                     @RequestParam(required = false) Integer clazzId,
                                     @RequestParam(defaultValue = "1") Integer pageNum,
                                     @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Test> pageInfo = testService.selectPageByStudent(studentId, courseName, clazzId, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    /**
     * 根据班级ID查询该班级的考试列表
     */
    @GetMapping("/selectByClazz/{clazzId}")
    public Result selectByClazz(@PathVariable Integer clazzId) {
        List<Test> list = testService.selectByClazz(clazzId);
        return Result.success(list);
    }

    /**
     * 根据班级ID查询该班级的考试列表（使用参数方式）
     */
    @GetMapping("/selectByClazz")
    public Result selectByClazzParam(@RequestParam Integer clazzId, 
                                     @RequestParam(required = false, defaultValue = "false") Boolean onlyFinished) {
        List<Test> list = testService.selectByClazz(clazzId, onlyFinished);
        return Result.success(list);
    }
}
