package com.zrlog.test.hexo.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.zrlog.blog.hexo.template.HexoDateWrapper;
import org.junit.Test;

public class HexoDateWrapperTest {

    @Test
    public void shouldWrapDateString() {
        HexoDateWrapper wrapper = new HexoDateWrapper("2024-05-01 10:20:30");
        assertEquals("2024-05-01 10:20:30", wrapper.toString());
        assertEquals("5", wrapper.month());
        assertEquals(wrapper, wrapper.clone());
        assertEquals(wrapper, wrapper.locale("zh_CN"));
        assertNotNull(wrapper.getDate());
    }

    @Test
    public void shouldHandleNullInput() {
        HexoDateWrapper wrapper = new HexoDateWrapper(null);
        assertNotNull(wrapper.toString());
        assertEquals("1970-01-01", wrapper.format("yyyy-MM-dd"));
    }
}
