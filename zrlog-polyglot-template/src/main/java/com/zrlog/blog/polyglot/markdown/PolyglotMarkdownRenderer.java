package com.zrlog.blog.polyglot.markdown;

import com.zrlog.business.service.MarkdownRenderer;

/**
 * Adapts the optional Polyglot engine to the shared Markdown capability.
 */
public final class PolyglotMarkdownRenderer implements MarkdownRenderer {

    @Override
    public String render(String markdown) {
        return RendererHolder.RENDERER.render(markdown);
    }

    private static final class RendererHolder {
        private static final MarkdownJsRenderer RENDERER = new MarkdownJsRenderer();
    }
}
