package com.zrlog.test.polyglot.markdown;

import com.zrlog.business.service.MarkdownRenderer;
import org.junit.Test;

import java.util.ServiceLoader;

import static org.junit.Assert.assertTrue;

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
