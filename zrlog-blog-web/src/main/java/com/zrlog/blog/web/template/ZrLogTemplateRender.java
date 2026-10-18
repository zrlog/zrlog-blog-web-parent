package com.zrlog.blog.web.template;

import com.hibegin.common.util.EnvKit;
import com.hibegin.common.util.LoggerUtil;
import com.hibegin.common.util.StringUtils;
import com.hibegin.http.server.api.HttpRequest;
import com.hibegin.http.server.util.PathUtil;
import com.hibegin.template.TemplateRender;
import com.zrlog.blog.freemarker.template.FreemarkerZrLogTemplate;
import com.zrlog.blog.hexo.template.HexoTemplate;
import com.zrlog.blog.web.template.vo.BasePageInfo;
import com.zrlog.blog.web.template.vo.NotFindPageVO;
import com.zrlog.business.plugin.BodySaveResponse;
import com.zrlog.business.service.TemplateInfoHelper;
import com.zrlog.business.template.HtmlTemplateProcessor;
import com.zrlog.business.type.TemplateType;
import com.zrlog.common.CacheService;
import com.zrlog.common.Constants;
import com.zrlog.common.TokenService;
import com.zrlog.common.exception.NotImplementException;
import com.zrlog.common.vo.AdminTokenVO;
import com.zrlog.common.vo.TemplateVO;
import com.zrlog.plugin.BaseStaticSitePlugin;
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;

public class ZrLogTemplateRender implements TemplateRender {

    private static final Logger LOGGER = LoggerUtil.getLogger(ZrLogTemplateRender.class);

    private final HttpRequest request;

    private final BasePageInfo pageInfo;

    private final ZrLogTemplate zrLogTemplate;

    public ZrLogTemplateRender(HttpRequest request, CacheService cacheService) throws Exception {
        this.request = request;
        this.pageInfo = TemplateRenderUtils.fullTemplateInfo(request, cacheService);
        loadClasspathTemplateI18n(pageInfo);
        this.zrLogTemplate = setupTemplate();
    }

    private static void loadClasspathTemplateI18n(BasePageInfo pageInfo) throws Exception {
        TemplateVO templateVO = TemplateInfoHelper.loadTemplateVO(pageInfo.getTemplate());
        if (Objects.isNull(templateVO) || !templateVO.isClasspathTemplate()) {
            return;
        }
        String resourcePath = pageInfo.getTemplate() + "/language/i18n_" + pageInfo.getLocal() + ".properties";
        try (InputStream inputStream = ZrLogTemplateRender.class.getResourceAsStream(resourcePath)) {
            if (Objects.isNull(inputStream)) {
                return;
            }
            Properties properties = new Properties();
            properties.load(inputStream);
            Map<String, Object> resources = pageInfo.get_res();
            if (Objects.isNull(resources)) {
                return;
            }
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                resources.put(entry.getKey().toString(), entry.getValue());
            }
        }
    }

    private ZrLogTemplate setupTemplate() throws Exception {

        TemplateVO templateVO = TemplateInfoHelper.loadTemplateVO(pageInfo.getTemplate());
        if (Objects.isNull(templateVO)) {
            throw new RuntimeException("missing template -> " + pageInfo.getTemplate());
        }
        ZrLogTemplate template;
        if (templateVO.getTemplateType() == TemplateType.NODE_JS) {
            template = new HexoTemplate(templateVO);
        } else if (templateVO.getTemplateType() == TemplateType.STANDARD) {
            template = new FreemarkerZrLogTemplate();
        } else {
            throw new NotImplementException();
        }
        if (templateVO.isClasspathTemplate()) {
            template.initClassTemplate(pageInfo.getTemplate());
        } else {
            File path = PathUtil.getStaticFile(pageInfo.getTemplate());
            if (!path.exists()) {
                throw new RuntimeException("missing template files -> " + path.getName());
            }
            template.init(path);
        }
        return template;
    }

    public boolean isNotFound() {
        return pageInfo instanceof NotFindPageVO;
    }

    public boolean existsByTemplateName(String templateName) {
        File path = PathUtil.getStaticFile(pageInfo.getTemplate());
        if (path.exists() && !TemplateInfoHelper.isDefaultTemplate(pageInfo.getTemplate())) {
            return Arrays.stream(Objects.requireNonNull(path.listFiles())).anyMatch(e -> e.getName().startsWith(templateName + "."));
        } else {
            return Objects.nonNull(ZrLogTemplateRender.class.getResourceAsStream(pageInfo.getTemplate() + "/" + templateName + ".ftl"));
        }
    }

    @Override
    public String render(InputStream inputStream) {
        throw new NotImplementException();
    }

    @Override
    public String render(String s) {
        throw new NotImplementException();
    }

    /**
     * 处理静态化文件
     */
    static boolean catGeneratorHtml(HttpRequest request) {
        if (BaseStaticSitePlugin.isStaticPluginRequest(request)) {
            return false;
        }
        String targetUri = request.getUri();
        if (!Constants.isStaticHtmlStatus()) {
            return false;
        }
        return targetUri.endsWith(".html");
    }

    @Override
    public String renderByTemplateName(String page) throws Exception {
        if (StringUtils.isNotEmpty(pageInfo.getArrangePlugin()) && existsByTemplateName("arrange")) {
            page = "arrange";
        }
        long start = System.currentTimeMillis();
        String htmlStr = zrLogTemplate.render(page, pageInfo);
        TokenService tokenService = Constants.zrLogConfig.getTokenService();
        AdminTokenVO adminTokenVO = null;
        if (Objects.nonNull(tokenService)) {
            adminTokenVO = tokenService.getAdminTokenVO(request);
        }
        HtmlTemplateProcessor pluginTagHande = new HtmlTemplateProcessor(
                request, adminTokenVO, Objects.requireNonNullElse(pageInfo.getStaticResourceBaseUrl(), "/"));
        String realHtmlStr = pluginTagHande.transform(htmlStr);
        if (EnvKit.isDevMode()) {
            LOGGER.info("Render " + request.getUri() + " page, used time " + (System.currentTimeMillis() - start) + "ms");
        }
        if (isNotFound() || !catGeneratorHtml(request)) {
            return realHtmlStr;
        }
        //save to cache
        try (BodySaveResponse bodySaveResponse = new BodySaveResponse(request, Constants.zrLogConfig.getResponseConfig(), false)) {
            bodySaveResponse.renderHtmlStr(realHtmlStr);
        }
        return realHtmlStr;
    }
}
