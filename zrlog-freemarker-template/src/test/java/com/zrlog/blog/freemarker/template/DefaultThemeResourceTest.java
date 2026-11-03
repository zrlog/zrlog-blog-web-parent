package com.zrlog.blog.freemarker.template;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

public class DefaultThemeResourceTest {

    @Test
    public void shouldHideCommentCardWhenPluginRendersNoElements() throws Exception {
        String resourcePath = "include/templates/default/css/style_v3.css";
        try (InputStream inputStream =
                getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertNotNull(inputStream);
            String css = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(css.contains(".comment-shell:not(:has(*))"));
            assertTrue(css.contains(".comment-shell:not(:has(*)) {\n    display: none;\n}"));
        }
    }
}
