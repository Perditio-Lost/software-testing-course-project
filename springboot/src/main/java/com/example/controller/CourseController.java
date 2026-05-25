package com.example.controller;

import com.example.common.Result;
import com.example.entity.Course;
import com.example.service.CourseService;
import com.example.service.TencentCosService;
import com.example.common.utils.ImageUtils;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 课程信息前端请求接口
 */
@RestController
@RequestMapping("/course")
public class CourseController {

    @Resource
    private CourseService courseService;
    
    @Resource
    private TencentCosService tencentCosService;

    /**
     * 新增
     */
    @PostMapping("/add")
    public Result add(@RequestBody Course course) {
        courseService.add(course);
        return Result.success(course);
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    public Result update(@RequestBody Course course) {
        courseService.updateById(course);
        return Result.success();
    }

    /**
     * 上传课程封面到腾讯云COS
     * 存储路径：course/{id}.png
     * 统一转换为PNG格式
     * 如果已存在则自动替换
     */
    @PostMapping("/uploadImg")
    public Result uploadImg(@RequestParam("file") MultipartFile file, 
                           @RequestParam("id") Integer id) {
        try {
            if (file == null || file.isEmpty()) {
                return Result.error("文件为空");
            }
            
            // 统一使用PNG格式
            String fileName = "course/" + id + ".png";
            
            // 检查是否已存在旧封面，如果存在则先删除
            if (tencentCosService.doesObjectExist(fileName)) {
                tencentCosService.deleteFile(fileName);
            }
            
            // 转换为PNG格式并上传
            byte[] pngBytes = ImageUtils.convertToPng(file);
            String fileUrl = tencentCosService.uploadPngImage(pngBytes, fileName);
            
            return Result.success(fileUrl);
        } catch (Exception e) {            return Result.error("封面上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取课程封面URL（从COS查询）
     * 返回封面URL或null
     */
    @GetMapping("/getImgUrl/{id}")
    public Result getImgUrl(@PathVariable Integer id) {
        try {
            String fileName = "course/" + id + ".png";
            if (tencentCosService.doesObjectExist(fileName)) {
                String imgUrl = tencentCosService.getFileUrl(fileName);
                return Result.success(imgUrl);
            }
            
            // 未找到封面
            return Result.success(null);
        } catch (Exception e) {            return Result.error("查询封面失败: " + e.getMessage());
        }
    }

    /**
     * 单个删除
     */
    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        courseService.deleteById(id);
        return Result.success();
    }

    /**
     * 批量删除
     */
    @DeleteMapping("/delete/batch")
    public Result delete(@RequestBody List<Integer> ids) {
        courseService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 单个查询
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        Course course = courseService.selectById(id);
        return Result.success(course);
    }

    /**
     * 查询所有
     */
    @GetMapping("/selectAll")
    public Result selectAll(Course course) {
        List<Course> list = courseService.selectAll(course);
        return Result.success(list);
    }

    /**
     * 查询教师所教的课程（通过班级关联）
     */
    @GetMapping("/selectByTeacher")
    public Result selectByTeacher(@RequestParam Integer teacherId) {
        List<Course> list = courseService.selectByTeacherId(teacherId);
        return Result.success(list);
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result selectPage(Course course,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Course> pageInfo = courseService.selectPage(course, pageNum, pageSize);
        return Result.success(pageInfo);
    }

}
