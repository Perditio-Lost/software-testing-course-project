package com.example.controller;

import com.example.common.Result;
import com.example.entity.TestPaperTemplate;
import com.example.service.TestPaperTemplateService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/testPaperTemplate")
public class TestPaperTemplateController {

    @Resource
    private TestPaperTemplateService testPaperTemplateService;

    @PostMapping("/add")
    public Result add(@RequestBody TestPaperTemplate template) {
        testPaperTemplateService.add(template);
        return Result.success();
    }

    @PostMapping("/update")
    public Result update(@RequestBody TestPaperTemplate template) {
        testPaperTemplateService.updateById(template);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        testPaperTemplateService.deleteById(id);
        return Result.success();
    }

    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        TestPaperTemplate template = testPaperTemplateService.selectById(id);
        return Result.success(template);
    }

    @GetMapping("/selectAll")
    public Result selectAll(TestPaperTemplate template) {
        List<TestPaperTemplate> list = testPaperTemplateService.selectAll(template);
        return Result.success(list);
    }

    @GetMapping("/selectPage")
    public Result selectPage(TestPaperTemplate template,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<TestPaperTemplate> pageInfo = testPaperTemplateService.selectPage(template, pageNum, pageSize);
        return Result.success(pageInfo);
    }
}

