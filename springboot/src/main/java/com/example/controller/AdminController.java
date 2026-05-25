package com.example.controller;

import com.example.common.Result;
import com.example.entity.Admin;
import com.example.service.AdminService;
import com.example.service.TencentCosService;
import com.example.common.utils.ImageUtils;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 前端请求接口
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Resource
    private AdminService adminService;
    
    @Resource
    private TencentCosService tencentCosService;

    /**
     * 新增
     */
    @PostMapping("/add")
    public Result add(@RequestBody Admin admin) {
        adminService.add(admin);
        return Result.success(admin);
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    public Result update(@RequestBody Admin admin) {
        adminService.updateById(admin);
        return Result.success();
    }

    /**
     * 上传管理员头像到腾讯云COS
     * 存储路径：admin-avatar/{id}.png
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
            String fileName = "admin-avatar/" + id + ".png";
            
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
     * 获取管理员头像URL（从COS查询）
     * 返回头像URL或null
     */
    @GetMapping("/getAvatarUrl/{id}")
    public Result getAvatarUrl(@PathVariable Integer id) {
        try {
            String fileName = "admin-avatar/" + id + ".png";
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
        adminService.deleteById(id);
        return Result.success();
    }

    /**
     * 批量删除
     */
    @DeleteMapping("/delete/batch")
    public Result delete(@RequestBody List<Integer> ids) {
        adminService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 单个查询
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        Admin admin = adminService.selectById(id);
        return Result.success(admin);
    }

    /**
     * 查询所有
     */
    @GetMapping("/selectAll")
    public Result selectAll(Admin admin) {
        List<Admin> list = adminService.selectAll(admin);
        return Result.success(list);
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result selectPage(Admin admin,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Admin> pageInfo = adminService.selectPage(admin, pageNum, pageSize);
        return Result.success(pageInfo);
    }

}
