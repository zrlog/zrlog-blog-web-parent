package com.zrlog.test.web;

import com.hibegin.http.HttpMethod;
import com.hibegin.http.annotation.RequestMethod;
import org.junit.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BlogApiDocumentationContractTest {

    @Test
    @SuppressWarnings("unchecked")
    public void shouldKeepOpenApiOperationsBoundToExplicitControllerMethods() throws Exception {
        Path openApiPath = findOpenApiPath();
        Path canonical = openApiPath.getParent().getParent().getParent().getParent().resolve("zrlog-api/blog-web.yaml");
        if (Files.isRegularFile(canonical)) {
            assertEquals("Stale OpenAPI snapshot; synchronize from zrlog-api", Files.readString(canonical), Files.readString(openApiPath));
        }
        Map<String, Object> spec;
        try (Reader reader = Files.newBufferedReader(openApiPath)) {
            spec = new Yaml(new SafeConstructor(new LoaderOptions())).load(reader);
        }

        assertEquals("3.1.0", spec.get("openapi"));
        assertEquals("blog-web", spec.get("x-zrlog-id"));
        Map<String, Object> paths = (Map<String, Object>) spec.get("paths");
        assertNotNull(paths);
        assertFalse(paths.isEmpty());

        Set<String> operationIds = new HashSet<>();
        int operationCount = 0;
        for (Map.Entry<String, Object> pathEntry : paths.entrySet()) {
            Map<String, Object> pathItem = (Map<String, Object>) pathEntry.getValue();
            for (Map.Entry<String, Object> methodEntry : pathItem.entrySet()) {
                HttpMethod httpMethod = toHttpMethod(methodEntry.getKey());
                if (httpMethod == null) {
                    continue;
                }
                operationCount++;
                Map<String, Object> operation = (Map<String, Object>) methodEntry.getValue();
                String operationId = String.valueOf(operation.get("operationId"));
                assertTrue("Duplicate operationId " + operationId, operationIds.add(operationId));
                assertControllerMethod(operation, httpMethod);
            }
        }
        assertEquals(5, operationCount);
    }

    private static void assertControllerMethod(Map<String, Object> operation, HttpMethod httpMethod) throws Exception {
        String controllerRef = String.valueOf(operation.get("x-zrlog-controller"));
        String[] parts = controllerRef.split("#", 2);
        assertEquals("Invalid controller reference " + controllerRef, 2, parts.length);
        Method method = Class.forName(parts[0]).getDeclaredMethod(parts[1]);
        RequestMethod annotation = method.getAnnotation(RequestMethod.class);
        assertNotNull(controllerRef + " must declare @RequestMethod", annotation);
        assertEquals(controllerRef, httpMethod, annotation.method());
    }

    private static HttpMethod toHttpMethod(String value) {
        try {
            return HttpMethod.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Path findOpenApiPath() throws IOException {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve("docs/api/openapi.yaml");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IOException("docs/api/openapi.yaml not found");
    }
}
