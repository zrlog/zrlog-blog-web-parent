package com.zrlog.blog.business.rest.response;

public class CreateCommentResponse {

    private String alias;

    public CreateCommentResponse() {
    }

    public CreateCommentResponse(String alias) {
        this.alias = alias;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }
}
