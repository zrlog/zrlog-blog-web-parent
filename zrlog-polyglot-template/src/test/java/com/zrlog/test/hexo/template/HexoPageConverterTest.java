package com.zrlog.test.hexo.template;

import com.hibegin.common.dao.dto.PageData;
import com.zrlog.blog.hexo.template.HexoPageConverter;
import com.zrlog.blog.web.template.vo.ArticleListPageVO;
import com.zrlog.common.Constants;
import com.zrlog.common.cache.vo.BaseDataInitVO;
import com.zrlog.common.vo.PublicWebSiteInfo;
import com.zrlog.data.dto.ArticleBasicDTO;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class HexoPageConverterTest {

    private static ArticleBasicDTO article(long logId, String alias) {
        ArticleBasicDTO article = new ArticleBasicDTO();
        article.setLogId(logId);
        article.setAlias(alias);
        article.setTitle(alias);
        article.setTypeId(1L);
        article.setTypeName("Default");
        article.setTags(Collections.emptyList());
        article.setUrl("/" + alias);
        return article;
    }

    private static BaseDataInitVO initData() {
        PublicWebSiteInfo website = new PublicWebSiteInfo();
        website.setTitle("ZrLog");
        website.setSecond_title("Blog");
        website.setAuthor("admin");
        BaseDataInitVO init = new BaseDataInitVO();
        init.setWebSite(website);
        init.getTemplateConfigCacheMap().put(
                "test-template", Map.of(Constants.TEMPLATE_CONFIG_STR_KEY, "{}"));
        return init;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldExposeNumericStickyValuesToHexoPosts() {
        ArticleBasicDTO pinned = article(1L, "pinned");
        pinned.setSticky(3);
        ArticleBasicDTO unpinned = article(2L, "unpinned");
        unpinned.setSticky(null);
        BaseDataInitVO init = initData();
        ArticleListPageVO pageInfo = new ArticleListPageVO(
                new PageData<>(2L, List.of(pinned, unpinned), 1L, 10L), init);
        pageInfo.setTemplate("test-template");
        pageInfo.setReqUriPath("/");
        pageInfo.setBaseWithHostPath("//blog.example.com/");
        pageInfo.setBaseUrl("/");
        pageInfo.setLang("en");

        Map<String, Object> root = HexoPageConverter.toRootMap(pageInfo, "index", "/missing-template");
        Map<String, Object> page = (Map<String, Object>) root.get("page");
        Map<String, Object> posts = (Map<String, Object>) page.get("posts");
        List<Map<String, Object>> rows = (List<Map<String, Object>>) posts.get("data");

        assertEquals(3, rows.get(0).get("sticky"));
        assertEquals(0, rows.get(1).get("sticky"));
    }
}
