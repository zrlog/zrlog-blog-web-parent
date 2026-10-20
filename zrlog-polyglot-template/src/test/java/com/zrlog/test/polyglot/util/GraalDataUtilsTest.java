package com.zrlog.test.polyglot.util;

import com.zrlog.blog.polyglot.util.GraalDataUtils;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class GraalDataUtilsTest {

    @Test
    public void shouldConvertNullAndSimpleValue() {
        assertNull(GraalDataUtils.makeJsFriendly(null));
        assertEquals("value", GraalDataUtils.makeJsFriendly("value"));
    }

    @Test
    public void shouldConvertNestedMapAndList() {
        Object converted = GraalDataUtils.makeJsFriendly(Map.of("list", List.of("a", Map.of("b", 1))));
        assertTrue(converted instanceof ProxyObject);

        ProxyObject proxyObject = (ProxyObject) converted;
        Object list = proxyObject.getMember("list");
        assertTrue(list instanceof ProxyArray);
        ProxyArray proxyArray = (ProxyArray) list;
        assertEquals("a", proxyArray.get(0));
        assertTrue(proxyArray.get(1) instanceof ProxyObject);
    }
}
