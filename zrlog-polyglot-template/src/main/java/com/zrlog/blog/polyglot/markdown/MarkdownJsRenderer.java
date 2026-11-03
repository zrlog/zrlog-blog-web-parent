package com.zrlog.blog.polyglot.markdown;

import java.util.logging.Logger;

public class MarkdownJsRenderer {

    public static final String MARKED_VERSION = "16.0.0";
    public static final String EDITOR_VERSION = "2.1.32";

    private static final Logger LOGGER = Logger.getLogger(MarkdownJsRenderer.class.getName());

    /**
     * @param markdown Markdown source
     * @return null when rendering is unavailable; callers must retain their existing content
     */
    public String render(String markdown) {
        LOGGER.warning(
                "Skipping server-side Markdown rendering: Java 17 or newer is required; "
                        + "provide rendered content when running on Java 11.");
        return null;
    }
}
