package com.zrlog.test.model;

import com.zrlog.common.Constants;
import com.zrlog.util.I18nUtil;
import com.zrlog.util.ZrLogUtil;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import org.junit.Test;

public class TestCommon {

    @Test
    public void testVersionCompile() {
        boolean result = ZrLogUtil.greatThenCurrentVersion("9999999", new Date(), "1.6.4");
        System.out.println("result = " + result);
        assert result;
    }

    @Test
    public void testI18n() {
        I18nUtil.addToRequestWithTemplatePath(Constants.TEMPLATE_BASE_PATH + "test", null, "zh_CN");
        Map<String, Object> backend = I18nUtil.getBackend();
        System.out.println("backend = " + backend);
        assert Objects.nonNull(backend);
    }
}
