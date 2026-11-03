package com.zrlog.test.polyglot.util;

import static org.junit.Assert.assertEquals;

import com.zrlog.blog.polyglot.util.PolyglotContextUtils;
import org.graalvm.polyglot.Context;
import org.junit.Test;

public class PolyglotContextUtilsTest {

    @Test
    public void shouldBuildWorkingJsContext() {
        try (Context context = PolyglotContextUtils.buildJsContext()) {
            assertEquals(3, context.eval("js", "1 + 2").asInt());
        }
    }
}
