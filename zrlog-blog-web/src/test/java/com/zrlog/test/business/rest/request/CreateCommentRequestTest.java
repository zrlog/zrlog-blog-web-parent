package com.zrlog.test.business.rest.request;

import static org.junit.Assert.assertEquals;

import com.google.gson.Gson;
import com.zrlog.blog.business.rest.request.CreateCommentRequest;
import org.junit.Test;

public class CreateCommentRequestTest {

    @Test
    public void shouldDeserializeCurrentAndLegacyCommentFieldNames() {
        String json =
                "{"
                        + "\"userHome\":\"https://example.com\","
                        + "\"userMail\":\"user@example.com\","
                        + "\"userComment\":\"hello\","
                        + "\"userName\":\"xiaochun\","
                        + "\"logId\":\"1\","
                        + "\"replyId\":2"
                        + "}";

        CreateCommentRequest request = new Gson().fromJson(json, CreateCommentRequest.class);

        assertEquals("https://example.com", request.getUserHome());
        assertEquals("user@example.com", request.getMail());
        assertEquals("hello", request.getComment());
        assertEquals("xiaochun", request.getUserName());
        assertEquals("1", request.getLogId());
        assertEquals(2, request.getReplyId());
    }

    @Test
    public void shouldDeserializePrimaryCommentFieldNames() {
        String json =
                "{"
                        + "\"webHome\":\"https://zrlog.com\","
                        + "\"mail\":\"admin@zrlog.com\","
                        + "\"comment\":\"primary\","
                        + "\"userIp\":\"203.0.113.1\","
                        + "\"ip\":\"198.51.100.2\","
                        + "\"userAgent\":\"Mozilla/5.0\""
                        + "}";

        CreateCommentRequest request = new Gson().fromJson(json, CreateCommentRequest.class);

        assertEquals("https://zrlog.com", request.getUserHome());
        assertEquals("admin@zrlog.com", request.getMail());
        assertEquals("primary", request.getComment());
        assertEquals("203.0.113.1", request.getUserIp());
        assertEquals("198.51.100.2", request.getIp());
        assertEquals("Mozilla/5.0", request.getUserAgent());
    }

    @Test
    public void shouldDeserializeBlogAliasForUserHome() {
        CreateCommentRequest request =
                new Gson()
                        .fromJson(
                                "{\"blog\":\"https://legacy.example.com\"}",
                                CreateCommentRequest.class);

        assertEquals("https://legacy.example.com", request.getUserHome());
    }

    @Test
    public void shouldExposeMutableCommentFields() {
        CreateCommentRequest request = new CreateCommentRequest();

        request.setUserHome("https://example.com");
        request.setMail("mail@example.com");
        request.setUserIp("203.0.113.10");
        request.setUserName("author");
        request.setLogId("9");
        request.setComment("body");
        request.setIp("198.51.100.20");
        request.setUserAgent("agent");
        request.setReplyId(12);

        assertEquals("https://example.com", request.getUserHome());
        assertEquals("mail@example.com", request.getMail());
        assertEquals("203.0.113.10", request.getUserIp());
        assertEquals("author", request.getUserName());
        assertEquals("9", request.getLogId());
        assertEquals("body", request.getComment());
        assertEquals("198.51.100.20", request.getIp());
        assertEquals("agent", request.getUserAgent());
        assertEquals(12, request.getReplyId());
    }
}
