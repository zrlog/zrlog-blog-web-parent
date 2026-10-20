package com.zrlog.test.hexo.template.impl;

import com.zrlog.blog.hexo.template.impl.HexoPaginator;
import com.zrlog.blog.web.template.PagerVO;
import org.graalvm.polyglot.Value;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HexoPaginatorTest {

    private static PagerVO.PageEntry entry(String url, String desc, boolean current, boolean prev, boolean next) {
        PagerVO.PageEntry entry = new PagerVO.PageEntry();
        entry.setUrl(url);
        entry.setDesc(desc);
        entry.setCurrent(current);
        entry.setPrev(prev);
        entry.setNext(next);
        return entry;
    }

    @Test
    public void shouldReturnEmptyHtmlWhenSinglePage() {
        PagerVO pagerVO = new PagerVO();
        pagerVO.setPageList(List.of(entry("/1", "1", true, false, false)));
        assertEquals("", new HexoPaginator(pagerVO).execute());
    }

    @Test
    public void shouldRenderPageLinks() {
        PagerVO pagerVO = new PagerVO();
        pagerVO.setPageList(List.of(
                entry("/1", "1", true, false, false),
                entry("/2", "2", false, false, false),
                entry("/3", "next", false, false, true)
        ));
        String html = (String) new HexoPaginator(pagerVO).execute(Value.asValue(Map.of("format", "page#anchor", "prev_text", "Prev", "next_text", "Next")));
        assertTrue(html.contains("current\">1</span>"));
        assertTrue(html.contains("href=\"/2#anchor\">2</a>"));
        assertTrue(html.contains("href=\"/3#anchor\">Next</a>"));
    }
}
