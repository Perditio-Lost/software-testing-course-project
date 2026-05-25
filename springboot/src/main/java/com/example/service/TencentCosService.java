package com.example.service;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.*;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * 腾讯云COS文件上传服务
 */
@Service
public class TencentCosService {

    private static final Logger log = LoggerFactory.getLogger(TencentCosService.class);

    @Resource
    private COSClient cosClient;

    @Value("${tencent.cos.bucketName}")
    private String bucketName;

    @Value("${tencent.cos.region}")
    private String region;

    @Value("${tencent.cos.baseUrl}")
    private String baseUrl;

    @Value("${tencent.cos.pathPrefix:}")
    private String pathPrefix;

    /**
     * 上传文件到腾讯云COS
     *
     * @param file 上传的文件
     * @return 文件访问URL
     */
    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("文件为空，上传失败");
        }

        try {
            // 获取原始文件名
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                throw new RuntimeException("文件名为空");
            }

            // 生成唯一文件名：日期/UUID_原始文件名
            String dateDir = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
            String uuid = UUID.randomUUID().toString().replace("-", "");
            String fileName = pathPrefix + dateDir + "/" + uuid + "_" + originalFilename;

            // 获取文件输入流
            InputStream inputStream = file.getInputStream();

            // 创建上传对象元信息
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(file.getSize());
            objectMetadata.setContentType(file.getContentType());

            // 上传文件
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileName, inputStream, objectMetadata);
            PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);

            log.info("文件上传成功: {}, ETag: {}", fileName, putObjectResult.getETag());

            // 返回文件访问URL
            return baseUrl + "/" + fileName;

        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传文件（指定文件名）
     *
     * @param file     上传的文件
     * @param fileName 指定的文件名（包含路径，如果已包含完整路径则不添加前缀）
     * @return 文件访问URL
     */
    public String uploadFile(MultipartFile file, String fileName) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("文件为空，上传失败");
        }

        try {
            // 如果fileName已经包含了目录路径（如face-photos/），则不添加pathPrefix
            String fullFileName = fileName.contains("/") ? fileName : pathPrefix + fileName;

            // 获取文件输入流
            InputStream inputStream = file.getInputStream();

            // 创建上传对象元信息
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(file.getSize());
            objectMetadata.setContentType(file.getContentType());

            // 上传文件
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fullFileName, inputStream, objectMetadata);
            PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);

            log.info("文件上传成功: {}, ETag: {}", fullFileName, putObjectResult.getETag());

            // 返回文件访问URL
            return baseUrl + "/" + fullFileName;

        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传PNG格式图片（从字节数组）
     *
     * @param pngBytes PNG格式的字节数组
     * @param fileName 指定的文件名（包含路径）
     * @return 文件访问URL
     */
    public String uploadPngImage(byte[] pngBytes, String fileName) {
        if (pngBytes == null || pngBytes.length == 0) {
            throw new RuntimeException("图片数据为空，上传失败");
        }

        try {
            // 如果fileName已经包含了目录路径，则不添加pathPrefix
            String fullFileName = fileName.contains("/") ? fileName : pathPrefix + fileName;

            // 创建输入流
            InputStream inputStream = new java.io.ByteArrayInputStream(pngBytes);

            // 创建上传对象元信息
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(pngBytes.length);
            objectMetadata.setContentType("image/png");

            // 上传文件
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fullFileName, inputStream, objectMetadata);
            PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);

            log.info("PNG图片上传成功: {}, ETag: {}", fullFileName, putObjectResult.getETag());

            // 返回文件访问URL
            return baseUrl + "/" + fullFileName;

        } catch (Exception e) {
            log.error("PNG图片上传失败", e);
            throw new RuntimeException("PNG图片上传失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件
     *
     * @param fileUrl 文件URL（可以是完整URL或文件路径）
     */
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        try {
            // 从URL中提取文件名
            String fileName;
            if (fileUrl.startsWith("http")) {
                // 完整URL，提取文件路径
                fileName = fileUrl.substring(baseUrl.length() + 1);
            } else {
                // 相对路径
                fileName = fileUrl;
            }

            // 删除文件
            cosClient.deleteObject(bucketName, fileName);
            log.info("文件删除成功: {}", fileName);

        } catch (Exception e) {
            log.error("文件删除失败", e);
            throw new RuntimeException("文件删除失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件访问URL（公有读取的文件）
     *
     * @param fileName 文件名（包含路径）
     * @return 文件访问URL
     */
    public String getFileUrl(String fileName) {
        // 如果fileName已经包含了目录路径，则不添加pathPrefix
        String fullFileName = fileName.contains("/") ? fileName : pathPrefix + fileName;
        return baseUrl + "/" + fullFileName;
    }

    /**
     * 获取文件访问URL（用于私有读取的文件）
     *
     * @param fileName   文件名
     * @param expireTime 过期时间（秒）
     * @return 带签名的临时访问URL
     */
    public String getPresignedUrl(String fileName, int expireTime) {
        try {
            Date expiration = new Date(System.currentTimeMillis() + expireTime * 1000L);
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, fileName);
            request.setExpiration(expiration);
            return cosClient.generatePresignedUrl(request).toString();
        } catch (Exception e) {
            log.error("生成签名URL失败", e);
            throw new RuntimeException("生成签名URL失败: " + e.getMessage());
        }
    }

    /**
     * 判断文件是否存在
     *
     * @param fileName 文件名
     * @return 是否存在
     */
    public boolean doesObjectExist(String fileName) {
        try {
            return cosClient.doesObjectExist(bucketName, fileName);
        } catch (Exception e) {
            log.error("检查文件是否存在失败", e);
            return false;
        }
    }

    /**
     * 下载文件
     *
     * @param fileName 文件名（COS中的完整路径）
     * @return COSObject
     */
    public COSObject downloadFile(String fileName) {
        try {
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, fileName);
            COSObject cosObject = cosClient.getObject(getObjectRequest);
            log.info("文件下载成功: {}", fileName);
            return cosObject;
        } catch (Exception e) {
            log.error("文件下载失败: {}", fileName, e);
            throw new RuntimeException("文件下载失败: " + e.getMessage());
        }
    }
}
