package com.zrlog.blog.business.service;

import com.zrlog.data.dto.ArticleBasicDTO;

import java.util.Objects;

public class CommentService {

    static boolean isValidEmailAddress(String email) {
        String ePattern = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(ePattern);
        java.util.regex.Matcher m = p.matcher(email);
        return m.matches();
    }

    boolean isAllowComment(ArticleBasicDTO articleBasicDTO, boolean commentsDisabled) {
        if (commentsDisabled) {
            return false;
        }
        return Objects.equals(articleBasicDTO.getCanComment(), true);
    }
}
