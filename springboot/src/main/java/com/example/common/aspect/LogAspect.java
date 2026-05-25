package com.example.common.aspect;

import com.alibaba.fastjson2.JSON;
import com.example.entity.SystemLog;
import com.example.service.SystemLogService;
import com.example.common.utils.TokenUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 操作日志切面
 */
@Aspect
@Component
public class LogAspect {

    @Resource
    private SystemLogService systemLogService;

    /**
     * 定义切点：拦截所有Controller的增删改方法
     */
    @Pointcut("execution(* com.example.controller.*.*(..)) && " +
              "(execution(* com.example.controller.*.*add*(..)) || " +
              "execution(* com.example.controller.*.*Add*(..)) || " +
              "execution(* com.example.controller.*.*insert*(..)) || " +
              "execution(* com.example.controller.*.*Insert*(..)) || " +
              "execution(* com.example.controller.*.*update*(..)) || " +
              "execution(* com.example.controller.*.*Update*(..)) || " +
              "execution(* com.example.controller.*.*edit*(..)) || " +
              "execution(* com.example.controller.*.*Edit*(..)) || " +
              "execution(* com.example.controller.*.*delete*(..)) || " +
              "execution(* com.example.controller.*.*Delete*(..)) || " +
              "execution(* com.example.controller.*.*remove*(..)) || " +
              "execution(* com.example.controller.*.*Remove*(..)))")
    public void logPointcut() {
    }

    /**
     * 环绕通知
     */
    @Around("logPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // 执行目标方法
        Object result = joinPoint.proceed();
        
        // 计算执行时长
        long duration = System.currentTimeMillis() - startTime;
        
        // 异步保存日志
        try {
            saveLog(joinPoint, duration);
        } catch (Exception e) {
            // 日志保存失败不影响业务        
        }
        
        return result;
    }

    /**
     * 保存日志
     */
    private void saveLog(ProceedingJoinPoint joinPoint, long duration) {
        // 获取request
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        
        // 获取当前登录用户
        Object currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            return; // 未登录不记录日志
        }
        
        // 创建日志对象
        SystemLog systemLog = new SystemLog();
        
        // 设置用户信息
        try {
            Integer userId = (Integer) currentUser.getClass().getMethod("getId").invoke(currentUser);
            String role = (String) currentUser.getClass().getMethod("getRole").invoke(currentUser);
            systemLog.setUserId(userId);
            systemLog.setRole(role);
        } catch (Exception e) {        }
        
        // 获取方法信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        // 获取操作描述
        String operation = getOperation(method, joinPoint);
        systemLog.setOperation(operation);
        
        // 设置请求方法
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = method.getName();
        systemLog.setMethod(className + "." + methodName);
        
        // 设置请求参数
        Object[] args = joinPoint.getArgs();
        String params = "";
        if (args != null && args.length > 0) {
            try {
                params = JSON.toJSONString(args);
                // 参数太长则截取
                if (params.length() > 500) {
                    params = params.substring(0, 500) + "...";
                }
            } catch (Exception e) {
                params = "参数转换异常";
            }
        }
        systemLog.setParams(params);
        
        // 设置IP地址
        String ip = getIpAddress(request);
        systemLog.setIp(ip);
        
        // 设置IP所在地（可以调用第三方API获取，这里简单处理）
        systemLog.setLocation(getLocationByIp(ip));
        
        // 设置操作时间
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        systemLog.setTime(time);
        
        // 设置执行时长
        systemLog.setDuration(duration);
        
        // 保存日志
        systemLogService.add(systemLog);
    }

    /**
     * 获取操作描述
     */
    private String getOperation(Method method, ProceedingJoinPoint joinPoint) {
        // 根据方法名自动生成
        String methodName = method.getName().toLowerCase();
        String className = joinPoint.getTarget().getClass().getSimpleName().replace("Controller", "");
        
        if (methodName.contains("add") || methodName.contains("insert")) {
            return "新增" + className;
        } else if (methodName.contains("update") || methodName.contains("edit")) {
            return "修改" + className;
        } else if (methodName.contains("delete") || methodName.contains("remove")) {
            if (methodName.contains("batch")) {
                return "批量删除" + className;
            }
            return "删除" + className;
        }
        
        return className + "操作";
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
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多IP情况，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0];
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
            // 使用免费的IP定位API（ip-api.com）
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
}
