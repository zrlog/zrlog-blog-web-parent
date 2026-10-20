package com.zrlog.test.hexo.template;

import com.zrlog.blog.hexo.template.JsMapAdapter;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class JsMapAdapterTest {

    @Test
    public void shouldExposeMapLikeApi() {
        JsMapAdapter adapter = new JsMapAdapter(Map.of("home", "/"));
        assertTrue(adapter.has("home"));
        assertFalse(adapter.has(null));
        assertEquals("/", adapter.get("home"));
        assertEquals(1, adapter.size());
    }
}
