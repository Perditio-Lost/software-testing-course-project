package com.example.common.utils;

import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 图片处理工具类
 */
public class ImageUtils {

    /**
     * 将图片转换为PNG格式的字节数组
     * @param file 原始图片文件
     * @return PNG格式的字节数组
     * @throws IOException 如果转换失败
     */
    public static byte[] convertToPng(MultipartFile file) throws IOException {
        // 读取原始图片
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        if (originalImage == null) {
            throw new IOException("无法读取图片文件，请确保上传的是有效的图片格式");
        }
        
        // 创建一个新的BufferedImage，确保支持PNG格式（处理透明度问题）
        BufferedImage newImage = new BufferedImage(
            originalImage.getWidth(),
            originalImage.getHeight(),
            BufferedImage.TYPE_INT_ARGB
        );
        
        // 绘制原始图片到新图片上
        newImage.getGraphics().drawImage(originalImage, 0, 0, null);
        
        // 将新图片写入字节数组
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(newImage, "png", outputStream);
        
        return outputStream.toByteArray();
    }
    
    /**
     * 将图片转换为PNG格式的InputStream
     * @param file 原始图片文件
     * @return PNG格式的InputStream
     * @throws IOException 如果转换失败
     */
    public static InputStream convertToPngInputStream(MultipartFile file) throws IOException {
        byte[] pngBytes = convertToPng(file);
        return new ByteArrayInputStream(pngBytes);
    }
    
    /**
     * 获取转换后的PNG文件大小
     * @param file 原始图片文件
     * @return PNG格式的文件大小（字节）
     * @throws IOException 如果转换失败
     */
    public static long getPngSize(MultipartFile file) throws IOException {
        return convertToPng(file).length;
    }
}
