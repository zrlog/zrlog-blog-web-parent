package com.zrlog.test.polyglot.markdown;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.zrlog.business.service.MarkdownRenderer;
import java.util.ServiceLoader;
import org.junit.Test;

public class MarkdownRendererProviderTest {
    @Test
    public void shouldDiscoverAndRenderThroughTheSharedCapability() {
        var providers = ServiceLoader.load(MarkdownRenderer.class).iterator();
        assertTrue(providers.hasNext());
        MarkdownRenderer renderer = providers.next();
        assertEquals("<h1>Title</h1>\n", renderer.render("# Title"));
        assertEquals("<p>first<br>second</p>\n", renderer.render("first\nsecond"));
    }
}
