package com.sfi.matrix.iam.audit.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User-Agent 解析：从 UA 串提取浏览器与操作系统，供审计日志记录设备信息。
 *
 * <p>UA 完全由客户端提供、可被伪造，仅作审计参考不作为安全依据；
 * 解析失败返回空串，任何情况都不抛异常影响审计写入。</p>
 */
public final class UserAgentUtils {

    private static final Pattern EDGE = Pattern.compile("Edg(?:e|A|iOS)?/([\\d.]+)");
    private static final Pattern OPERA = Pattern.compile("(?:OPR|Opera)/([\\d.]+)");
    private static final Pattern WECHAT = Pattern.compile("MicroMessenger/([\\d.]+)");
    private static final Pattern ALIPAY = Pattern.compile("AlipayClient/([\\d.]+)");
    private static final Pattern FIREFOX = Pattern.compile("Firefox/([\\d.]+)");
    private static final Pattern CHROME = Pattern.compile("(?:Chrome|CriOS)/([\\d.]+)");
    private static final Pattern SAFARI = Pattern.compile("Version/([\\d.]+).*Safari");
    private static final Pattern IE = Pattern.compile("(?:MSIE |rv:)([\\d.]+)");

    private static final Pattern ANDROID = Pattern.compile("Android[ /]([\\d.]+)");
    private static final Pattern IPHONE = Pattern.compile("iPhone OS ([\\d_]+)");
    private static final Pattern IPAD = Pattern.compile("CPU OS ([\\d_]+)");
    private static final Pattern WINDOWS = Pattern.compile("Windows NT ([\\d.]+)");
    private static final Pattern MACOS = Pattern.compile("Mac OS X ([\\d_]+)");

    private UserAgentUtils() {
    }

    /** 解析浏览器名称与版本，如 Chrome 120、WeChat 8.0；无法识别返回空串。 */
    public static String browser(String ua) {
        if (ua == null || ua.isEmpty()) {
            return "";
        }
        String v;
        if ((v = match(WECHAT, ua)) != null) return "WeChat " + v;
        if ((v = match(ALIPAY, ua)) != null) return "Alipay " + v;
        if ((v = match(EDGE, ua)) != null) return "Edge " + v;
        if ((v = match(OPERA, ua)) != null) return "Opera " + v;
        if ((v = match(FIREFOX, ua)) != null) return "Firefox " + v;
        if ((v = match(CHROME, ua)) != null) return "Chrome " + v;
        if ((v = match(SAFARI, ua)) != null) return "Safari " + v;
        if ((v = match(IE, ua)) != null) return "IE " + v;
        return "";
    }

    /** 解析操作系统，如 Windows 10/11、Android 13、iOS 17；无法识别返回空串。 */
    public static String os(String ua) {
        if (ua == null || ua.isEmpty()) {
            return "";
        }
        String v;
        if ((v = match(ANDROID, ua)) != null) return "Android " + v;
        if ((v = match(IPHONE, ua)) != null) return "iOS " + v.replace('_', '.');
        if ((v = match(IPAD, ua)) != null) return "iPadOS " + v.replace('_', '.');
        if ((v = match(WINDOWS, ua)) != null) return "Windows " + windowsName(v);
        if ((v = match(MACOS, ua)) != null) return "macOS " + v.replace('_', '.');
        if (ua.contains("Linux")) return "Linux";
        return "";
    }

    private static String match(Pattern pattern, String ua) {
        Matcher m = pattern.matcher(ua);
        return m.find() ? m.group(1) : null;
    }

    private static String windowsName(String nt) {
        if ("10.0".equals(nt)) return "10/11";
        if ("6.3".equals(nt)) return "8.1";
        if ("6.2".equals(nt)) return "8";
        if ("6.1".equals(nt)) return "7";
        return nt;
    }
}
