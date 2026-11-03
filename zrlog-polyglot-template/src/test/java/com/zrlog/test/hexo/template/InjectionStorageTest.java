package com.zrlog.test.hexo.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.zrlog.blog.hexo.template.InjectionStorage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class InjectionStorageTest {

    @Test
    public void shouldStoreRelativeInjectionPath() {
        Map<String, List<String>> storage = new HashMap<>();
        InjectionStorage injectionStorage = new InjectionStorage(storage, "/tmp/theme");
        injectionStorage.add("head", "/tmp/theme/layout/header.njk");
        assertEquals(List.of("layout/header.njk"), injectionStorage.get("head"));
    }

    @Test
    public void shouldIgnoreAddWhenTemplateDirMissing() {
        InjectionStorage injectionStorage = new InjectionStorage(new HashMap<>(), null);
        injectionStorage.add("head", "/tmp/theme/layout/header.njk");
        assertNull(injectionStorage.get("head"));
    }
}
