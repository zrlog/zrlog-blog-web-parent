package com.zrlog.test.polyglot.resource;

import com.zrlog.blog.polyglot.resource.ScriptProvider;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class ScriptProviderTest {

    @Test
    public void shouldLoadBaseLibraryScript() throws Exception {
        ScriptProvider scriptProvider = ScriptProvider.getInstance();
        scriptProvider.addBaseScriptByPath("path", "base/scripts/path.js");
        scriptProvider.addBaseScriptByPath("path", "missing.js");
        assertTrue(scriptProvider.exists("path"));
        assertFalse(scriptProvider.load("path").isEmpty());
        assertFalse(scriptProvider.load("../path").isEmpty());
    }

    @Test
    public void shouldCacheTemplateScriptAndReturnEmptyForMissingScript() {
        ScriptProvider scriptProvider = new ScriptProvider();
        String scriptPath = new File("src/main/resources/conf/hexo/scripts/hexo-util.js").getPath();

        scriptProvider.addScript("hexo-util", scriptPath);
        String loaded = scriptProvider.load("hexo-util");
        scriptProvider.addScript("hexo-util", "missing.js");

        assertTrue(scriptProvider.exists("hexo-util"));
        assertFalse(loaded.isEmpty());
        assertEquals(loaded, scriptProvider.load("../hexo-util"));
        assertFalse(scriptProvider.exists("missing"));
        assertEquals("", scriptProvider.load("missing"));
    }
}
