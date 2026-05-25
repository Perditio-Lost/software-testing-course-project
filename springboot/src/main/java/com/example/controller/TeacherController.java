package com.example.controller;

import com.example.common.Result;
import com.example.entity.Teacher;
import com.example.service.TeacherService;
import com.example.service.TencentCosService;
import com.example.common.utils.ImageUtils;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 教师信息前端请求接口
 */
@RestController
@RequestMapping("/teacher")
public class TeacherController {

    @Resource
    private TeacherService teacherService;
    
    @Resource
    private TencentCosService tencentCosService;

    /**
     * 新增
     */
    @PostMapping("/add")
    public Result add(@RequestBody Teacher teacher) {
        teacherService.add(teacher);
        return Result.success(teacher);
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    public Result update(@RequestBody Teacher teacher) {
        teacherService.updateById(teacher);
        return Result.success();
    }

    /**
     * 上传教师头像到腾讯云COS
     * 存储路径：teacher-avatar/{id}.png
     * 统一转换为PNG格式
     * 如果已存在则自动替换
     */
    @PostMapping("/uploadAvatar")
    public Result uploadAvatar(@RequestParam("file") MultipartFile file, 
                              @RequestParam("id") Integer id) {
        try {
            if (file == null || file.isEmpty()) {
                return Result.error("文件为空");
            }
            
            // 统一使用PNG格式
            String fileName = "teacher-avatar/" + id + ".png";
            
            // 检查是否已存在旧头像，如果存在则先删除
            if (tencentCosService.doesObjectExist(fileName)) {
                tencentCosService.deleteFile(fileName);
            }
            
            // 转换为PNG格式并上传
            byte[] pngBytes = ImageUtils.convertToPng(file);
            String fileUrl = tencentCosService.uploadPngImage(pngBytes, fileName);
            
            return Result.success(fileUrl);
        } catch (Exception e) {            return Result.error("头像上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取教师头像URL（从COS查询）
     * 返回头像URL或null
     */
    @GetMapping("/getAvatarUrl/{id}")
    public Result getAvatarUrl(@PathVariable Integer id) {
        try {
            String fileName = "teacher-avatar/" + id + ".png";
            if (tencentCosService.doesObjectExist(fileName)) {
                String avatarUrl = tencentCosService.getFileUrl(fileName);
                return Result.success(avatarUrl);
            }
            
            // 未找到头像
            return Result.success(null);
        } catch (Exception e) {            return Result.error("查询头像失败: " + e.getMessage());
        }
    }

    /**
     * 单个删除
     */
    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        teacherService.deleteById(id);
        return Result.success();
    }

    /**
     * 批量删除
     */
    @DeleteMapping("/delete/batch")
    public Result delete(@RequestBody List<Integer> ids) {
        teacherService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 单个查询
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        Teacher teacher = teacherService.selectById(id);
        return Result.success(teacher);
    }

    /**
     * 查询所有
     */
    @GetMapping("/selectAll")
    public Result selectAll(Teacher teacher) {
        List<Teacher> list = teacherService.selectAll(teacher);
        return Result.success(list);
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result selectPage(Teacher teacher,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Teacher> pageInfo = teacherService.selectPage(teacher, pageNum, pageSize);
        return Result.success(pageInfo);
    }

}
