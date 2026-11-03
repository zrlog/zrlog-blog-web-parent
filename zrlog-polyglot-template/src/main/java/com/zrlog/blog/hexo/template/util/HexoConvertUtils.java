package com.zrlog.blog.hexo.template.util;

import com.zrlog.blog.web.template.vo.ArticleDetailPageVO;
import java.util.Map;
import java.util.Objects;

public class HexoConvertUtils {

    public static Map<String, Object> getPrevLog(ArticleDetailPageVO articleDetailPageVO) {
        if (Objects.isNull(articleDetailPageVO.getLog().getLastLog())) {
            return Map.of("enable", false);
        }
        return Map.of(
                "title",
                articleDetailPageVO.getLog().getLastLog().getTitle(),
                "path",
                articleDetailPageVO.getLog().getLastLog().getUrl());
    }

    public static Map<String, Object> getNextLog(ArticleDetailPageVO articleDetailPageVO) {
        if (Objects.isNull(articleDetailPageVO.getLog().getNextLog())) {
            return Map.of("enable", false);
        }
        return Map.of(
                "title",
                articleDetailPageVO.getLog().getNextLog().getTitle(),
                "path",
                articleDetailPageVO.getLog().getNextLog().getUrl());
    }
}
