package com.zrlog.blog.hexo.template.impl;

import com.zrlog.blog.web.template.PagerVO;
import java.util.Map;
import java.util.Objects;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

public class HexoPaginator implements ProxyExecutable {

    private final PagerVO pagerVO;

    public HexoPaginator(PagerVO pagerVO) {
        this.pagerVO = pagerVO;
    }

    @Override
    public Object execute(Value... args) {
        // 1. 获取分页参数 (通常从全局变量 page 获取)
        // 假设你已经把当前的 page 对象传进来了，或者从参数中解析 options
        int total = pagerVO.getPageList().size();

        Map<String, Object> options = args.length > 0 ? args[0].as(Map.class) : Map.of();
        if (total <= 1) return "";
        StringBuilder html = getStringBuilder(options);

        return html.toString();
    }

    private String appendFormat(String url, String format) {
        if (format == null || !format.contains("#")) {
            return url;
        }
        return url + "#" + (format.split("#")[1]);
    }

    private String getDesc(PagerVO.PageEntry pager, Map<String, Object> options) {
        if (Objects.equals(pager.getPrev(), true)) {
            return options.get("prev_text").toString();
        } else if (Objects.equals(pager.getNext(), true)) {
            return options.get("next_text").toString();
        }
        return pager.getDesc();
    }

    private StringBuilder getStringBuilder(Map<String, Object> options) {
        StringBuilder html = new StringBuilder();
        for (PagerVO.PageEntry pager : pagerVO.getPageList()) {
            if (pager.getCurrent()) {
                html.append(
                        String.format(
                                "<span class=\"page-number current\">%s</span>", pager.getDesc()));
            } else {
                html.append(
                        String.format(
                                "<a class=\"page-number\" href=\"%s\">%s</a>",
                                appendFormat(pager.getUrl(), (String) options.get("format")),
                                getDesc(pager, options)));
            }
        }
        return html;
    }
}
