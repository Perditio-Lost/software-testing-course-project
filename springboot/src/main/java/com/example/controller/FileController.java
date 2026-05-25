package com.example.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import com.example.common.Result;
import com.example.service.TencentCosService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/files")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    private static final String filePath = System.getProperty("user.dir") + "/files/";

    @Value("${fileBaseUrl:}")
    private String fileBaseUrl;
    
    @Resource
    private TencentCosService tencentCosService;

    /**
     * 文件上传（本地存储）
     */
    @PostMapping("/upload")
    public Result upload(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        try {
            if (!FileUtil.isDirectory(filePath)) {
                FileUtil.mkdir(filePath);
            }
            fileName = System.currentTimeMillis() + "-" + fileName;
            String realFilePath = filePath + fileName;
            // 文件存储形式：时间戳-文件名
            FileUtil.writeBytes(file.getBytes(), realFilePath);
        } catch (Exception e) {
            log.error(fileName + "--文件上传失败", e);
        }
        String url = fileBaseUrl + "/files/download/" + fileName;
        return Result.success(url);
    }

    /**
     * 获取文件
     */
    @GetMapping("/download/{fileName}")
    public void download(@PathVariable String fileName, HttpServletResponse response) {
        OutputStream os;
        try {
            if (StrUtil.isNotEmpty(fileName)) {
                response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
                response.setContentType("application/octet-stream");
                byte[] bytes = FileUtil.readBytes(filePath + fileName);
                os = response.getOutputStream();
                os.write(bytes);
                os.flush();
                os.close();
            }
        } catch (Exception e) {
            log.warn("文件下载失败：" + fileName);
        }
    }

    /**
     * wang-editor编辑器文件上传接口
     */
    @PostMapping("/wang/upload")
    public Map<String, Object> wangEditorUpload(MultipartFile file) {
        String flag = System.currentTimeMillis() + "";
        String fileName = file.getOriginalFilename();
        try {
            // 文件存储形式：时间戳-文件名
            FileUtil.writeBytes(file.getBytes(), filePath + flag + "-" + fileName);
            Thread.sleep(1L);
        } catch (Exception e) {
            // 文件上传失败
        }
        String http = fileBaseUrl + "/files/download/";
        Map<String, Object> resMap = new HashMap<>();
        // wangEditor上传图片成功后， 需要返回的参数
        resMap.put("errno", 0);
        resMap.put("data", CollUtil.newArrayList(Dict.create().set("url", http + flag + "-" + fileName)));
        return resMap;
    }
    
    /**
     * 文件上传到腾讯云COS
     */
    @PostMapping("/cos/upload")
    public Result cosUpload(@RequestParam("file") MultipartFile file) {
        try {
            String fileUrl = tencentCosService.uploadFile(file);
            return Result.success(fileUrl);
        } catch (Exception e) {
            log.error("文件上传到COS失败", e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 从腾讯云COS删除文件
     */
    @DeleteMapping("/cos/delete")
    public Result cosDelete(@RequestParam("fileUrl") String fileUrl) {
        try {
            tencentCosService.deleteFile(fileUrl);
            return Result.success("文件删除成功");
        } catch (Exception e) {
            log.error("文件删除失败", e);
            return Result.error("文件删除失败: " + e.getMessage());
        }
    }
    
    /**
     * wang-editor编辑器文件上传到COS
     */
    @PostMapping("/cos/wang/upload")
    public Map<String, Object> wangEditorCosUpload(MultipartFile file) {
        Map<String, Object> resMap = new HashMap<>();
        try {
            String fileUrl = tencentCosService.uploadFile(file);
            resMap.put("errno", 0);
            resMap.put("data", CollUtil.newArrayList(Dict.create().set("url", fileUrl)));
        } catch (Exception e) {
            log.error("wang-editor上传到COS失败", e);
            resMap.put("errno", 1);
            resMap.put("message", "上传失败: " + e.getMessage());
        }
        return resMap;
    }
}
