package com.zrlog.test.hexo.template.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.zrlog.blog.hexo.template.HexoDateWrapper;
import com.zrlog.blog.hexo.template.util.HexoDateObjUtils;
import java.util.Calendar;
import java.util.Date;
import org.junit.Test;

public class HexoDateObjUtilsTest {

    @Test
    public void shouldFormatDateVariants() {
        HexoDateObjUtils utils = new HexoDateObjUtils();
        Calendar calendar = Calendar.getInstance();
        calendar.set(2024, Calendar.MAY, 1, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long localMidnight = calendar.getTimeInMillis();
        assertEquals(
                "2024-05-01",
                utils.toDateString(new Date(localMidnight), "LL", "YYYY-MM-DD", "zh_CN"));
        assertEquals(
                "2024-05-01 00:00:00",
                utils.toDateString(localMidnight, "t", "YYYY-MM-DD", "en_US"));
        assertEquals(
                "2024-05-01",
                utils.toDateString(new HexoDateWrapper("2024-05-01"), null, "YYYY-MM-DD", "zh_CN"));
    }

    @Test
    public void shouldFallbackWhenTypeUnsupported() {
        String result =
                HexoDateObjUtils.getInstance().toDateString("raw", "LL", "YYYY-MM-DD", "en_US");
        assertEquals("raw", result);
        assertNotNull(
                HexoDateObjUtils.getInstance().toDateString(null, null, "YYYY-MM-DD", "en_US"));
    }
}
