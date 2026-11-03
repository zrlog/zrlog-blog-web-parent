package com.zrlog.blog.hexo.template.article;

import com.zrlog.blog.hexo.template.util.HexoDataUtils;
import com.zrlog.common.cache.vo.HotLogBasicInfoEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArticleInfoUtils {

    public static Map<String, Object> getHotArticle(
            List<HotLogBasicInfoEntry> hotLogBasicInfoEntries, Long total) {
        List<Map<String, Object>> hotArticles = new ArrayList<>();
        for (HotLogBasicInfoEntry entry : hotLogBasicInfoEntries) {
            Map<String, Object> hotArticle = new HashMap<>();
            hotArticle.put("title", entry.getTitle());
            hotArticle.put("path", "/" + entry.getLogId());
            hotArticles.add(hotArticle);
        }
        return HexoDataUtils.wrap(hotArticles, Math.toIntExact(total));
    }
}
