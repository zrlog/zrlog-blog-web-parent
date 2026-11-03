package com.zrlog.test.polyglot.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.zrlog.blog.polyglot.resource.TemplateResolver;
import java.nio.file.Paths;
import org.junit.Test;

public class TemplateResolverTest {

    @Test
    public void shouldResolveFileSystemPath() {
        TemplateResolver resolver = new TemplateResolver("src/main/resources");
        String resolved = resolver.resolve("../test.txt");
        assertTrue(resolved.endsWith("src/main/test.txt"));
    }

    @Test
    public void shouldResolveClasspathPathAndPushPop() {
        TemplateResolver resolver = new TemplateResolver("classpath:templates/layout");
        assertEquals(
                "classpath:templates/partial/header.njk",
                resolver.resolve("../partial/header.njk"));
        resolver.pushPath("classpath:templates/post/index.njk");
        assertEquals(
                "classpath:templates/post/components/card.njk",
                resolver.resolve("./components/card.njk"));
        resolver.popPath();
        assertEquals(
                "classpath:templates/partial/footer.njk",
                resolver.resolve("../partial/footer.njk"));
    }

    @Test
    public void shouldPushFileParentForFileSystemPath() {
        TemplateResolver resolver = new TemplateResolver(Paths.get("src").toString());
        resolver.pushPath(Paths.get("src/templates/index.njk").toString());
        String resolved = resolver.resolve("components/card.njk");
        assertTrue(resolved.endsWith("src/templates/components/card.njk"));
    }
}
