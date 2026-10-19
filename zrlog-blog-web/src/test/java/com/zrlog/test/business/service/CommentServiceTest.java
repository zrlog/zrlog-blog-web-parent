package com.zrlog.blog.business.service;

import com.zrlog.data.dto.ArticleBasicDTO;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CommentServiceTest {

    @Test
    public void shouldValidateEmailAddress() throws Exception {
        assertTrue(CommentService.isValidEmailAddress("user@example.com"));
        assertTrue(CommentService.isValidEmailAddress("user.name+tag@example.co.uk"));
        assertTrue(CommentService.isValidEmailAddress("user@[192.168.1.1]"));
        assertFalse(CommentService.isValidEmailAddress("bad-email"));
        assertFalse(CommentService.isValidEmailAddress("user@example"));
        assertFalse(CommentService.isValidEmailAddress("@example.com"));
    }

    @Test
    public void shouldRespectSiteAndArticleCommentSwitches() {
        CommentService commentService = new CommentService();
        assertTrue(commentService.isAllowComment(article(true), false));
        assertFalse(commentService.isAllowComment(article(false), false));
        assertFalse(commentService.isAllowComment(article(null), false));
        assertFalse(commentService.isAllowComment(article(true), true));
    }

    private static ArticleBasicDTO article(Boolean canComment) {
        ArticleBasicDTO article = new ArticleBasicDTO();
        article.setCanComment(canComment);
        return article;
    }

}
