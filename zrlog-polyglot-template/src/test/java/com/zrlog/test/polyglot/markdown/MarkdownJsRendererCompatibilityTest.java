package com.zrlog.test.polyglot.markdown;

import org.junit.Test;

import java.io.DataInputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class MarkdownJsRendererCompatibilityTest {

    @Test
    public void shouldSkipRenderingAndWarnWithoutLoadingGraalOnBaseJvm() throws Exception {
        String className = "com.zrlog.blog.polyglot.markdown.MarkdownJsRenderer";
        Logger logger = Logger.getLogger(className);
        List<LogRecord> records = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        logger.addHandler(handler);
        URL baseClasses = Path.of(System.getProperty("markdown.base.classes")).toUri().toURL();
        // No application dependencies: the base implementation must not require GraalJS.
        try (URLClassLoader loader = new URLClassLoader(new URL[]{baseClasses},
                ClassLoader.getPlatformClassLoader())) {
            try (DataInputStream bytecode = new DataInputStream(loader.getResourceAsStream(
                    className.replace('.', '/') + ".class"))) {
                assertEquals(0xCAFEBABE, bytecode.readInt());
                assertEquals(0, bytecode.readUnsignedShort());
                assertEquals(55, bytecode.readUnsignedShort());
            }
            Class<?> renderer = loader.loadClass(className);
            assertNull(renderer.getMethod("render", String.class)
                    .invoke(renderer.getConstructor().newInstance(), "# private article body"));
            assertEquals(1, records.size());
            assertEquals(Level.WARNING, records.get(0).getLevel());
            assertTrue(records.get(0).getMessage().contains("Skipping server-side Markdown rendering"));
            assertFalse(records.get(0).getMessage().contains("private article body"));
        } finally {
            logger.removeHandler(handler);
        }
    }
}
