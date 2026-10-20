package com.zrlog.blog.hexo.template.util;

import com.hibegin.common.util.LoggerUtil;
import com.hibegin.common.util.ObjectHelpers;
import com.zrlog.blog.hexo.template.HexoDateWrapper;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;

public class HexoDateObjUtils {

    private static final HexoDateObjUtils instance = new HexoDateObjUtils();
    private static final Logger LOGGER = LoggerUtil.getLogger(HexoDateObjUtils.class);
    private final Map<String, Locale> locales = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public HexoDateObjUtils() {
        locales.put("zh", Locale.CHINA);
        locales.put("zhCN", Locale.CHINA);
        locales.put("zhTw", Locale.TAIWAN);
        locales.put("en", Locale.ENGLISH);
        locales.put("enUS", Locale.ENGLISH);
    }

    public static HexoDateObjUtils getInstance() {
        return instance;
    }

    public String toDateString(Object dateObj, Object format, String defaultFormat, String local) {
        if (Objects.isNull(dateObj)) {
            dateObj = new Date();
        }

        // 获取日期对象（可能是 Long 时间戳或 Java Date）
        java.time.ZonedDateTime zonedDateTime;

        try {
            if (dateObj instanceof Date) {
                zonedDateTime = ((Date) dateObj).toInstant().atZone(ZoneId.systemDefault());
            } else if (dateObj instanceof Long) {
                zonedDateTime = java.time.Instant.ofEpochMilli((Long) dateObj).atZone(ZoneId.systemDefault());
            } else if (dateObj instanceof HexoDateWrapper) {
                zonedDateTime = java.time.Instant.ofEpochMilli(((HexoDateWrapper) dateObj).getDate().getTime()).atZone(ZoneId.systemDefault());
            } else {
                return dateObj.toString();
            }

            // 第二个参数是可选的格式化字符串，例如 date(post.date, 'MMM D, YYYY')
            String formatStr = Objects.nonNull(format) ? format.toString() : defaultFormat;

            if (formatStr.equalsIgnoreCase("LL a")) {
                formatStr = "yyyy-MM-dd B";
            } else if (formatStr.equalsIgnoreCase("LL")) {
                formatStr = "yyyy-MM-dd";
            } else if (formatStr.equalsIgnoreCase("t")) {
                formatStr = "yyyy-MM-dd HH:mm:ss";
            }

            // 简单处理：将 Hexo 的 YYYY 转换为 Java 的 yyyy
            formatStr = formatStr.replace("YYYY", "yyyy").replace("YY", "yy").replace("DD", "dd");

            return zonedDateTime.format(DateTimeFormatter.ofPattern(formatStr, ObjectHelpers.requireNonNullElse(locales.get(local.replace("_", "")), Locale.getDefault())));
        } catch (Exception e) {
            LOGGER.warning("Invalid Date " + e.getMessage());
            return dateObj.toString();
        }

    }
}
