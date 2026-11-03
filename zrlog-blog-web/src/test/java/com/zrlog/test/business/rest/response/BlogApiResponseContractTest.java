package com.zrlog.test.business.rest.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.zrlog.blog.business.rest.response.ApiStandardResponse;
import com.zrlog.blog.business.rest.response.CreateCommentResponse;
import org.junit.Test;

public class BlogApiResponseContractTest {

    @Test
    public void shouldExposeApiStandardResponseDataAndMessage() {
        ApiStandardResponse<String> empty = new ApiStandardResponse<>();
        assertNull(empty.getData());
        assertEquals("", empty.getMessage());

        ApiStandardResponse<String> withData = new ApiStandardResponse<>("payload");
        assertEquals("payload", withData.getData());
        assertEquals("", withData.getMessage());

        ApiStandardResponse<String> withMessage = new ApiStandardResponse<>("payload", "ok");
        assertEquals("payload", withMessage.getData());
        assertEquals("ok", withMessage.getMessage());

        withMessage.setData("changed");
        withMessage.setError(7);
        withMessage.setMessage("failed");
        assertEquals("changed", withMessage.getData());
        assertEquals(7, withMessage.getError());
        assertEquals("failed", withMessage.getMessage());
    }

    @Test
    public void shouldExposeCreateCommentAlias() {
        CreateCommentResponse empty = new CreateCommentResponse();
        assertNull(empty.getAlias());

        CreateCommentResponse response = new CreateCommentResponse("hello");
        assertEquals("hello", response.getAlias());

        response.setAlias("changed");
        assertEquals("changed", response.getAlias());
    }
}
