package com.example.controller;

import com.example.common.Result;
import com.example.common.enums.RoleEnum;
import com.example.common.dto.Account;
import com.example.entity.SystemLog;
import com.example.service.AdminService;
import com.example.service.StudentService;
import com.example.service.SystemLogService;
import com.example.service.TeacherService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
public class WebController {

    @Resource
    private AdminService adminService;
    @Resource
    private TeacherService teacherService;
    @Resource
    private StudentService studentService;
    @Resource
    private SystemLogService systemLogService;

    /**
     * 默认请求接口
     */
    @GetMapping("/")
    public Result hello () {
        return Result.success();
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    public Result login(@RequestBody Account account, HttpServletRequest request) {
        long startTime = System.currentTimeMillis();
        Account loginAccount = null;
        if (RoleEnum.ADMIN.name().equals(account.getRole())) {
            loginAccount = adminService.login(account);
        }
        if (RoleEnum.TEACHER.name().equals(account.getRole())) {
            loginAccount = teacherService.login(account);
        }
        if (RoleEnum.STUDENT.name().equals(account.getRole())) {
            loginAccount = studentService.login(account);
        }
        
        // 记录登录日志
        if (loginAccount != null) {
            try {
                SystemLog log = new SystemLog();
                log.setUserId(loginAccount.getId());
                log.setRole(loginAccount.getRole());
                log.setOperation("用户登录");
                log.setMethod("POST /login");
                log.setParams("username: " + account.getUsername());
                String ip = getIpAddress(request);
                log.setIp(ip);
                log.setLocation(getLocationByIp(ip));
                log.setTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                log.setDuration(System.currentTimeMillis() - startTime);
                systemLogService.add(log);
            } catch (Exception e) {            }
        }
        
        return Result.success(loginAccount);
    }
    
    /**
     * 获取IP地址
     */
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_CLUSTER_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_FORWARDED");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_VIA");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理情况下，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        // 本地IP处理
        if ("0:0:0:0:0:0:0:1".equals(ip) || "127.0.0.1".equals(ip)) {
            ip = "本地";
        }
        return ip;
    }
    
    /**
     * 根据IP获取位置信息
     */
    private String getLocationByIp(String ip) {
        if ("本地".equals(ip) || ip == null || ip.isEmpty()) {
            return "本地";
        }
        
        // 内网IP直接返回
        if (isInternalIp(ip)) {
            return "内网IP";
        }
        
        try {
            // 使用免费的IP定位API
            String apiUrl = "http://ip-api.com/json/" + ip + "?lang=zh-CN&fields=country,regionName,city";
            java.net.URL url = new java.net.URL(apiUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            
            if (conn.getResponseCode() == 200) {
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                // 解析JSON
                com.alibaba.fastjson2.JSONObject json = com.alibaba.fastjson2.JSON.parseObject(response.toString());
                String country = json.getString("country");
                String region = json.getString("regionName");
                String city = json.getString("city");
                
                StringBuilder location = new StringBuilder();
                if (country != null && !country.isEmpty()) {
                    location.append(country);
                }
                if (region != null && !region.isEmpty() && !region.equals(country)) {
                    if (location.length() > 0) location.append("-");
                    location.append(region);
                }
                if (city != null && !city.isEmpty() && !city.equals(region)) {
                    if (location.length() > 0) location.append("-");
                    location.append(city);
                }
                
                return location.length() > 0 ? location.toString() : "未知";
            }
        } catch (Exception e) {
            // IP定位失败不影响主流程
        }
        
        return "未知";
    }
    
    /**
     * 判断是否为内网IP
     */
    private boolean isInternalIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return true;
        }
        
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return true;
        }
        
        try {
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);
            
            // 10.0.0.0 - 10.255.255.255
            if (first == 10) {
                return true;
            }
            
            // 172.16.0.0 - 172.31.255.255
            if (first == 172 && second >= 16 && second <= 31) {
                return true;
            }
            
            // 192.168.0.0 - 192.168.255.255
            if (first == 192 && second == 168) {
                return true;
            }
        } catch (Exception e) {
            return true;
        }
        
        return false;
    }

    /**
     * 注册
     */
    @PostMapping("/register")
    public Result register(@RequestBody Account account) {
        if (RoleEnum.TEACHER.name().equals(account.getRole())) {
            teacherService.register(account);
        }
        if (RoleEnum.STUDENT.name().equals(account.getRole())) {
            studentService.register(account);
        }
        return Result.success();
    }

    /**
     * 修改密码
     */
    @PutMapping("/updatePassword")
    public Result updatePassword(@RequestBody Account account) {
        if (RoleEnum.ADMIN.name().equals(account.getRole())) {
            adminService.updatePassword(account);
        }
        if (RoleEnum.TEACHER.name().equals(account.getRole())) {
            teacherService.updatePassword(account);
        }
        if (RoleEnum.STUDENT.name().equals(account.getRole())) {
            studentService.updatePassword(account);
        }
        return Result.success();
    }

}
