package com.zrlog.test.polyglot.markdown;

import com.zrlog.blog.polyglot.markdown.MarkdownJsRenderer;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.*;

public class MarkdownJsRendererTest {

    @Test
    public void shouldRenderGfmMarkdownWithFrontendCompatibleLineBreaks() {
        MarkdownJsRenderer renderer = new MarkdownJsRenderer();

        String html = renderer.render("# Title\n\nfirst line\nsecond line\n\n| A | B |\n| - | - |\n| 1 | 2 |");

        assertTrue(html.contains("<h1>Title</h1>"));
        assertTrue(html.contains("first line<br>second line"));
        assertTrue(html.contains("<table>"));
        assertTrue(html.contains("<td>2</td>"));
    }

    @Test
    public void shouldRenderNullMarkdownAsEmptyHtml() {
        assertEquals("", new MarkdownJsRenderer().render(null));
    }

    @Test
    public void shouldRenderEditorCodeHighlightingAndCjkStrongBoundaries() {
        String html = new MarkdownJsRenderer().render(
                "\u4e2d**\u201c\u6587\u201d**\u5b57\n\n```javascript\nconst x = 1;\n```");

        assertTrue(html.contains("<strong>\u201c\u6587\u201d</strong>"));
        assertTrue(html.contains("code-block-wrapper"));
        assertTrue(html.contains("hljs-keyword"));
        assertFalse(html.contains("zrlog-cjk-strong"));
    }

    @Test
    public void shouldRenderInlineDisplayAndFencedMathWithoutDom() {
        MarkdownJsRenderer renderer = new MarkdownJsRenderer();

        assertTrue(renderer.render("$x^2$").contains("katex-html"));
        assertTrue(renderer.render("$$x^2$$").contains("katex-display"));
        for (String language : new String[]{"math", "latex", "katex"}) {
            String html = renderer.render("```" + language + "\nx^2\n```");
            assertTrue(language, html.contains("katex-html"));
            assertFalse(language, html.contains("data-code="));
        }
        assertTrue(renderer.render("```math\nx^{\n```").contains("katex-error"));
    }

    @Test
    public void shouldKeepCodeLiteralAndRenderDiagramsAsEscapedCode() {
        MarkdownJsRenderer renderer = new MarkdownJsRenderer();
        String html = renderer.render("`$x^2$`\n\n```plaintext\n$x^2$\n```");

        assertTrue(html.contains("<code>$x^2$</code>"));
        assertFalse(html.contains("katex"));
        for (String language : new String[]{"flow", "seq"}) {
            String diagram = renderer.render("```" + language + "\nA->B: <script>alert(1)</script>\n```");
            assertTrue(diagram.contains("language-" + language));
            assertTrue(diagram.contains("&lt;script&gt;"));
            assertFalse(diagram.contains("<script>"));
            assertFalse(diagram.contains("class=\"" + language + "\""));
        }
    }

    @Test
    public void shouldRenderConcurrentlyWithIsolatedContexts() throws Exception {
        MarkdownJsRenderer renderer = new MarkdownJsRenderer();
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            List<Future<String>> futures = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                int index = i;
                futures.add(executor.submit(() -> renderer.render("**item-" + index + "**")));
            }
            for (int i = 0; i < futures.size(); i++) {
                assertEquals("<p><strong>item-" + i + "</strong></p>\n", futures.get(i).get());
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
