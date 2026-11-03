package com.zrlog.blog.hexo.template.impl;

import com.hibegin.common.util.LoggerUtil;
import com.zrlog.common.cache.dto.TagDTO;
import java.util.List;
import java.util.Map;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

public class HexoTagCloud implements ProxyExecutable {

    private final List<TagDTO> tagList;

    public HexoTagCloud(List<TagDTO> tagList) {
        this.tagList = tagList;
    }

    @Override
    public Object execute(Value... args) {
        try {
            // 1. 获取参数 (JS 传入的 tags 集合和 options)
            if (args.length == 0) return "";
            if (tagList.isEmpty()) return "";
            // 假设第一个参数是 site.tags，第二个是配置对象
            Map<String, Object> options = args[0].as(Map.class);

            // 配置默认值
            double minFont = Double.parseDouble(options.getOrDefault("min_font", 12) + "");
            double maxFont = Double.parseDouble(options.getOrDefault("max_font", 30) + "");
            String unit = (String) options.getOrDefault("unit", "px");
            String orderby = (String) options.getOrDefault("orderby", "name");
            double order =
                    Double.parseDouble(options.getOrDefault("order", 1) + ""); // 1 asc, -1 desc

            // 颜色配置: 默认灰色到深灰色，或者由用户传入如 #ccc 到 #111
            String startColor = (String) options.getOrDefault("start_color", "");
            String endColor = (String) options.getOrDefault("end_color", "");
            boolean colorEnabled = !startColor.isEmpty() && !endColor.isEmpty();

            // 3. 计算 count 极值
            int minCount = tagList.stream().mapToInt(t -> t.getCount().intValue()).min().orElse(0);
            int maxCount = tagList.stream().mapToInt(t -> t.getCount().intValue()).max().orElse(0);
            int range = maxCount - minCount;

            // 4. 排序
            tagList.sort(
                    (a, b) -> {
                        int res =
                                (orderby.equals("count"))
                                        ? Long.compare(a.getCount(), b.getCount())
                                        : a.getText().compareTo(b.getText());
                        return res * (int) order;
                    });

            // 5. 生成 HTML
            StringBuilder sb = new StringBuilder();
            for (TagDTO tag : tagList) {
                double size =
                        (range == 0)
                                ? minFont
                                : minFont
                                        + ((double) (tag.getCount() - minCount) / range)
                                                * (maxFont - minFont);
                double ratio = (range == 0) ? 0.5 : (double) (tag.getCount() - minCount) / range;
                // 计算颜色
                String colorAttr = "";
                if (colorEnabled) {
                    colorAttr =
                            String.format(
                                    "color: %s;", interpolateColor(startColor, endColor, ratio));
                } else if (options.containsKey("color")) {
                    // 如果开启了默认彩色模式
                    colorAttr = String.format("color: %s;", getRandomColor());
                }
                sb.append(
                        String.format(
                                "<a href=\"%s\" style=\"font-size: %.2f%s; %s\">%s</a>\n",
                                tag.getUrl(), size, unit, colorAttr, tag.getText()));
            }
            return sb.toString();
        } catch (Exception e) {
            return LoggerUtil.recordStackTraceMsg(e);
        }
    }

    // RGB 插值核心算法
    private String interpolateColor(String startHex, String endHex, double ratio) {
        int[] rgb1 = hexToRgb(startHex);
        int[] rgb2 = hexToRgb(endHex);

        int r = (int) (rgb1[0] + ratio * (rgb2[0] - rgb1[0]));
        int g = (int) (rgb1[1] + ratio * (rgb2[1] - rgb1[1]));
        int b = (int) (rgb1[2] + ratio * (rgb2[2] - rgb1[2]));

        return String.format("rgb(%d, %d, %d)", r, g, b);
    }

    private int[] hexToRgb(String hex) {
        hex = hex.replace("#", "");
        if (hex.length() == 3) {
            hex =
                    String.format(
                            "%c%c%c%c%c%c",
                            hex.charAt(0),
                            hex.charAt(0),
                            hex.charAt(1),
                            hex.charAt(1),
                            hex.charAt(2),
                            hex.charAt(2));
        }
        int val = Integer.parseInt(hex, 16);
        return new int[] {(val >> 16) & 0xFF, (val >> 8) & 0xFF, val & 0xFF};
    }

    private String getRandomColor() {
        // 生成鲜艳但不刺眼的随机色
        return String.format(
                "rgb(%d, %d, %d)",
                (int) (Math.random() * 200),
                (int) (Math.random() * 200),
                (int) (Math.random() * 200));
    }
}
