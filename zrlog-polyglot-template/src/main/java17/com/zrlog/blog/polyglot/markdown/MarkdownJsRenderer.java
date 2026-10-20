package com.zrlog.blog.polyglot.markdown;

import org.graalvm.polyglot.*;
import org.graalvm.polyglot.io.IOAccess;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MarkdownJsRenderer {

    public static final String MARKED_VERSION = "16.0.0";
    public static final String EDITOR_VERSION = "2.1.32";

    private static final String RENDERER_RESOURCE = "/conf/base/scripts/zrlog-markdown-v" + EDITOR_VERSION + ".min.js";
    private static final Source RENDERER_SOURCE = loadRendererSource();
    private static final Engine ENGINE = Engine.newBuilder()
            .option("engine.WarnInterpreterOnly", "false")
            .build();

    private static Context buildContext() {
        return Context.newBuilder("js")
                .engine(ENGINE)
                .allowHostAccess(HostAccess.NONE)
                .allowHostClassLookup(className -> false)
                .allowPolyglotAccess(PolyglotAccess.NONE)
                .allowIO(IOAccess.NONE)
                .allowCreateThread(false)
                .allowNativeAccess(false)
                .build();
    }

    private static Source loadRendererSource() {
        try (InputStream inputStream = MarkdownJsRenderer.class.getResourceAsStream(RENDERER_RESOURCE)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing bundled editor Markdown script: " + RENDERER_RESOURCE);
            }
            String script = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return Source.newBuilder("js", script, "zrlog-markdown-v" + EDITOR_VERSION + ".min.js").buildLiteral();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load bundled editor Markdown script", e);
        }
    }

    /**
     * @param markdown Markdown source
     * @return rendered HTML, or null when rendering is unavailable on the current JVM
     */
    public String render(String markdown) {
        try (Context context = buildContext()) {
            context.eval(RENDERER_SOURCE);
            Value render = context.getBindings("js").getMember("ZrLogMarkdown").getMember("markdownToHtml");
            return render.execute(markdown == null ? "" : markdown).asString();
        }
    }
}
