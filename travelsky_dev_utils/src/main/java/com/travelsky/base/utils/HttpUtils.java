package com.travelsky.base.utils;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class HttpUtils {

    public static final void response(HttpServletResponse response, int code, String msg) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(code);
        PrintWriter writer = response.getWriter();
        writer.write(msg);
        writer.flush();
        writer.close();
    }

    public static String getClientIpAddress(HttpServletRequest request) {
        // 优先从代理头中获取
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // X-Forwarded-For 可能包含多个IP（用逗号分隔），第一个为原始客户端IP
            String[] ips = xForwardedFor.split(",");
            for (String ip : ips) {
                ip = ip.trim();
                if (!ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                    // 过滤本地回环地址或内网地址（可选）
                    if (!ip.startsWith("127.") && !ip.startsWith("192.168.") && !ip.equals("10.0.0.0")) {
                        return ip;
                    }
                }
            }
        }

        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty() && !"unknown".equalsIgnoreCase(xRealIP)) {
            return xRealIP;
        }

        // 如果没有代理头，则使用远程地址
        return request.getRemoteAddr();
    }

}
