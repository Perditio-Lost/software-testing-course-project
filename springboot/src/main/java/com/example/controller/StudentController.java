package com.example.controller;

import com.example.common.Result;
import com.example.entity.Student;
import com.example.service.MonitorService;
import com.example.service.StudentService;
import com.example.service.TencentCosService;
import com.example.common.utils.ImageUtils;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.List;

/**
 * 学生信息前端请求接口
 */
@RestController
@RequestMapping("/student")
public class StudentController {

    @Resource
    private StudentService studentService;

    @Resource
    private TencentCosService tencentCosService;

    @Resource
    private MonitorService monitorService;

    /**
     * 新增
     */
    @PostMapping("/add")
    public Result add(@RequestBody Student student) {
        studentService.add(student);
        return Result.success(student);
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    public Result update(@RequestBody Student student) {
        studentService.updateById(student);
        return Result.success();
    }

    /**
     * 上传学生照片到腾讯云COS
     * 存储路径：face-photos/{id}.png
     * 统一转换为PNG格式
     * 如果已存在则自动替换
     */
    @PostMapping("/uploadPhoto")
    public Result uploadPhoto(@RequestParam("file") MultipartFile file,
            @RequestParam("id") Integer id) {
        try {
            if (file == null || file.isEmpty()) {
                return Result.error("文件为空");
            }

            // 统一使用PNG格式
            String fileName = "face-photos/" + id + ".png";

            // 检查是否已存在旧照片，如果存在则先删除
            if (tencentCosService.doesObjectExist(fileName)) {
                tencentCosService.deleteFile(fileName);
            }

            // 验证学生是否存在
            Student student = studentService.selectById(id);
            if (student == null) {
                return Result.error("学生不存在");
            }

            // 转换为PNG格式并上传
            byte[] pngBytes = ImageUtils.convertToPng(file);
            String fileUrl = tencentCosService.uploadPngImage(pngBytes, fileName);

            // 同步注册到百度云人脸库
            try {
                String base64Image = Base64.getEncoder().encodeToString(pngBytes);
                monitorService.registerFace(String.valueOf(id), base64Image);
            } catch (Exception e) {
                // 人脸注册失败仅记录日志，不影响主流程照片上传
                System.err.println("人脸注册失败: " + e.getMessage());
                e.printStackTrace();
            }

            return Result.success(fileUrl);
        } catch (Exception e) {            return Result.error("照片上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传学生头像到腾讯云COS
     * 存储路径：student-avatar/{id}.png
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
            String fileName = "student-avatar/" + id + ".png";

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
     * 获取学生头像URL（从COS查询）
     * 返回头像URL或null
     */
    @GetMapping("/getAvatarUrl/{id}")
    public Result getAvatarUrl(@PathVariable Integer id) {
        try {
            String fileName = "student-avatar/" + id + ".png";
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
        studentService.deleteById(id);
        return Result.success();
    }

    /**
     * 批量删除
     */
    @DeleteMapping("/delete/batch")
    public Result delete(@RequestBody List<Integer> ids) {
        studentService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 单个查询
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        Student student = studentService.selectById(id);
        return Result.success(student);
    }

    /**
     * 根据用户名查询学生
     */
    @GetMapping("/selectByUsername/{username}")
    public Result selectByUsername(@PathVariable String username) {
        Student student = studentService.selectByUsername(username);
        return Result.success(student);
    }

    /**
     * 获取学生照片URL（从COS查询）
     * 返回照片URL或null
     */
    @GetMapping("/getPhotoUrl/{id}")
    public Result getPhotoUrl(@PathVariable Integer id) {
        try {
            String fileName = "face-photos/" + id + ".png";
            if (tencentCosService.doesObjectExist(fileName)) {
                String photoUrl = tencentCosService.getFileUrl(fileName);
                return Result.success(photoUrl);
            }

            // 未找到照片
            return Result.success(null);
        } catch (Exception e) {            return Result.error("查询照片失败: " + e.getMessage());
        }
    }

    /**
     * 删除学生照片（打回）
     * 从COS中删除学生照片
     */
    @DeleteMapping("/deletePhoto/{id}")
    public Result deletePhoto(@PathVariable Integer id) {
        try {
            String fileName = "face-photos/" + id + ".png";
            if (tencentCosService.doesObjectExist(fileName)) {
                tencentCosService.deleteFile(fileName);
                return Result.success();
            }
            return Result.error("照片不存在");
        } catch (Exception e) {            return Result.error("删除照片失败: " + e.getMessage());
        }
    }

    /**
     * 查询所有
     */
    @GetMapping("/selectAll")
    public Result selectAll(Student student) {
        List<Student> list = studentService.selectAll(student);
        return Result.success(list);
    }

    /**
     * 查询所有审核通过的学生（用于教师选择学生）
     */
    @GetMapping("/selectApproved")
    public Result selectApproved(Student student) {
        List<Student> list = studentService.selectApproved(student);
        return Result.success(list);
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result selectPage(Student student,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Student> pageInfo = studentService.selectPage(student, pageNum, pageSize);
        return Result.success(pageInfo);
    }

}
