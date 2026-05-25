package com.example.controller;

import com.example.common.Result;
import com.example.entity.Article;
import com.example.service.ArticleService;
import com.example.service.TencentCosService;
import com.example.common.utils.ImageUtils;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 帖子信息前端请求接口
 */
@RestController
@RequestMapping("/article")
public class ArticleController {

    @Resource
    private ArticleService articleService;
    
    @Resource
    private TencentCosService tencentCosService;

    /**
     * 上传帖子封面到COS
     */
    @PostMapping("/uploadImg")
    public Result uploadImg(@RequestParam("file") MultipartFile file, @RequestParam("id") Integer id) {
        try {
            // 转换为PNG格式
            byte[] pngBytes = ImageUtils.convertToPng(file);
            String fileName = "article/" + id + ".png";
            tencentCosService.uploadPngImage(pngBytes, fileName);
            String url = tencentCosService.getFileUrl(fileName);
            return Result.success(url);
        } catch (Exception e) {
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取帖子封面URL
     */
    @GetMapping("/getImgUrl/{id}")
    public Result getImgUrl(@PathVariable Integer id) {
        String fileName = "article/" + id + ".png";
        if (tencentCosService.doesObjectExist(fileName)) {
            return Result.success(tencentCosService.getFileUrl(fileName));
        }
        return Result.success(null);
    }

    /**
     * 新增
     */
    @PostMapping("/add")
    public Result add(@RequestBody Article article) {
        articleService.add(article);
        return Result.success(article);
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    public Result update(@RequestBody Article article) {
        articleService.updateById(article);
        return Result.success();
    }

    /**
     * 单个删除
     */
    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        articleService.deleteById(id);
        return Result.success();
    }

    /**
     * 批量删除
     */
    @DeleteMapping("/delete/batch")
    public Result delete(@RequestBody List<Integer> ids) {
        articleService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 单个查询
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        Article article = articleService.selectById(id);
        return Result.success(article);
    }

    /**
     * 查询所有
     */
    @GetMapping("/selectAll")
    public Result selectAll(Article article) {
        List<Article> list = articleService.selectAll(article);
        return Result.success(list);
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result selectPage(Article article,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Article> pageInfo = articleService.selectPage(article, pageNum, pageSize);
        return Result.success(pageInfo);
    }

    @GetMapping("/selectRandom")
    public Result selectRandom() {
        List<Article> list = articleService.selectRandom();
        return Result.success(list);
    }

}
