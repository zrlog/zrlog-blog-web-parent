package com.zrlog.blog.web;

import com.zrlog.web.WebSetup;
import com.zrlog.web.WebSetupContext;
import com.zrlog.web.WebSetupProvider;

public class BlogWebSetupProvider implements WebSetupProvider {

    @Override
    public String name() {
        return "blog";
    }

    @Override
    public int order() {
        return 300;
    }

    @Override
    public java.util.Set<String> requiredModules() { return java.util.Set.of("data", "services"); }

    @Override
    public WebSetup create(WebSetupContext context) {
        return new BlogWebSetup(context.getZrLogConfig(), context.getContextPath(), true);
    }
}
