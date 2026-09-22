package com.zrlog.blog.web.util;

import com.zrlog.theme.spi.BundledThemes;

import com.hibegin.common.util.LoggerUtil;
import com.hibegin.common.util.StringUtils;
import com.hibegin.http.HttpMethod;
import com.hibegin.http.server.ApplicationContext;
import com.hibegin.http.server.util.FreeMarkerUtil;
import com.hibegin.http.server.util.HttpRequestBuilder;
import com.hibegin.http.server.util.NativeImageUtils;
import com.hibegin.http.server.util.PathUtil;
import com.zrlog.blog.freemarker.template.FreemarkerZrLogTemplate;
import com.zrlog.blog.polyglot.util.PolyglotNativeImageUtils;
import com.zrlog.blog.web.BlogWebSetup;
import com.zrlog.blog.web.template.PagerVO;
import com.zrlog.blog.web.template.vo.ArticleDetailPageVO;
import com.zrlog.blog.web.template.vo.ArticleListPageVO;
import com.zrlog.blog.web.template.vo.BasePageInfo;
import com.zrlog.blog.web.template.vo.NotFindPageVO;
import com.zrlog.business.template.util.BlogResourceUtils;
import com.zrlog.common.Constants;
import com.zrlog.common.ZrLogConfig;
import com.zrlog.data.dto.ArticleBasicDTO;
import com.zrlog.data.dto.ArticleDetailDTO;
import com.zrlog.data.dto.VisitorCommentDTO;

import java.util.List;
import java.util.stream.Collectors;

public class BlogNativeImageUtils {

    public static void reg(ZrLogConfig zrLogConfig) {
        PolyglotNativeImageUtils.reg();
        nativeJson();
        List<String> resources = BlogResourceUtils.getInstance().getResources();
        NativeImageUtils.doResourceLoadByResourceNames(resources.stream().filter(StringUtils::isNotEmpty).map(e -> "/" + e).collect(Collectors.toList()));

        for (com.zrlog.theme.spi.BundledThemeProvider provider : BundledThemes.getInstance().providers()) {
            if (!"freemarker".equals(provider.engine())) continue;
            try {
                new FreemarkerZrLogTemplate().initClassTemplate(provider.path());
            } catch (Exception e) {
                throw new IllegalStateException("Unable to initialize bundled theme " + provider.id(), e);
            }
        }
        try {
            ApplicationContext applicationContext = new ApplicationContext(zrLogConfig.getServerConfig());
            applicationContext.init();
            FreeMarkerUtil.renderToFM("empty", HttpRequestBuilder.buildRequest(HttpMethod.GET, "/", "",
                    "", zrLogConfig.getRequestConfig(),
                    applicationContext));
        } catch (Exception e) {
            LoggerUtil.getLogger(BlogWebSetup.class).info("Freemarker render error " + e.getMessage());
        }

    }


    public static void nativeJson() {
        //freemarker need
        regWithGetMethod(ArticleDetailPageVO.class, ArticleListPageVO.class, NotFindPageVO.class,
                BasePageInfo.class, ArticleBasicDTO.class, ArticleDetailDTO.class,
                ArticleDetailDTO.LastLogDTO.class, ArticleDetailDTO.NextLogDTO.class,
                ArticleDetailDTO.TagsDTO.class, PagerVO.PageEntry.class, PagerVO.class, PagerVO.PageEntry.class, VisitorCommentDTO.class);
    }

    private static void regWithGetMethod(Class<?>... objects) {
        NativeImageUtils.gsonNativeAgentByClazz(List.of(objects));
        for (Class<?> o : objects) {
            NativeImageUtils.regGetMethodByClassName(o);
        }
    }

}
