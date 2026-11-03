package com.zrlog.test.polyglot.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.zrlog.blog.polyglot.util.YamlLoader;
import java.util.Map;
import org.junit.Test;

public class YamlLoaderTest {

    @Test
    public void shouldLoadYamlAndReadNestedValue() {
        Map<String, Object> config = YamlLoader.loadConfig("menu:\n  home:\n    title: Home\n");
        assertEquals("Home", YamlLoader.getNestedValue(config, "menu.home.title"));
        assertNull(YamlLoader.getNestedValue(config, "menu.archive.title"));
    }
}
