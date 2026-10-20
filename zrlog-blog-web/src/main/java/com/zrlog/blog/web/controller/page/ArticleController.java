package com.zrlog.blog.web.controller.page;

import com.hibegin.common.dao.dto.PageData;
import com.hibegin.common.dao.dto.PageRequest;
import com.hibegin.common.dao.dto.PageRequestImpl;
import com.hibegin.common.util.LoggerUtil;
import com.hibegin.common.util.StringUtils;
import com.hibegin.http.HttpMethod;
import com.hibegin.http.annotation.RequestMethod;
import com.hibegin.http.server.web.Controller;
import com.zrlog.blog.business.service.ArticleService;
import com.zrlog.blog.web.template.PagerUtil;
import com.zrlog.blog.web.template.PagerVO;
import com.zrlog.blog.web.util.WebTools;
import com.zrlog.business.plugin.PluginCorePlugin;
import com.zrlog.business.plugin.StaticSitePlugin;
import com.zrlog.common.CacheService;
import com.zrlog.common.Constants;
import com.zrlog.common.TokenService;
import com.zrlog.common.cache.dto.TypeDTO;
import com.zrlog.common.vo.AdminTokenVO;
import com.zrlog.data.dto.ArticleBasicDTO;
import com.zrlog.data.dto.ArticleDetailDTO;
import com.zrlog.model.Log;
import com.zrlog.util.I18nUtil;
import com.zrlog.util.ParseUtil;
import jakarta.inject.Inject;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class ArticleController extends Controller {

    private static final Logger LOGGER = LoggerUtil.getLogger(ArticleController.class);
    private final ArticleService articleService;
    private final CacheService cacheService;
    private final Supplier<TokenService> tokenService;
    private final Supplier<PluginCorePlugin> pluginCore;

    private final jakarta.inject.Provider<Log> articles;

    @Inject
    public ArticleController(
            CacheService cacheService,
            ArticleService articleService,
            Supplier<TokenService> tokenService,
            Supplier<PluginCorePlugin> pluginCore,
            jakarta.inject.Provider<Log> articles) {
        this.articles = articles;
        this.tokenService = tokenService;
        this.pluginCore = pluginCore;
        this.cacheService = cacheService;
        this.articleService = articleService;
    }

    static ArticleUriInfoVO parseUriInfo(String uri) {
        String rawUrl = uri;
        if (rawUrl.endsWith(".html")) {
            rawUrl = rawUrl.substring(0, rawUrl.length() - ".html".length());
        }
        String rawArgs = rawUrl.substring(rawUrl.lastIndexOf("/") + 1);
        List<String> list = Arrays.asList(rawArgs.split("-"));
        boolean numeric = ParseUtil.isNumeric(list.get(list.size() - 1));
        StringJoiner sj = new StringJoiner("-");
        int page = 1;
        if (numeric) {
            page = ParseUtil.strToInt(list.get(list.size() - 1), 1);
            for (int i = 0; i < list.size() - 1; i++) {
                sj.add(list.get(i));
            }
        } else {
            for (String arg : list) {
                sj.add(arg);
            }
        }
        String key = sj.toString();
        return new ArticleUriInfoVO(key, page);
    }

    /**
     * add page info for template more easy
     */
    private void setPageDataInfo(String currentUri, PageData<ArticleBasicDTO> data, PageRequest pageRequest) {
        data.getRows().forEach(e -> ArticleService.handlerArticle(e, request, cacheService));
        getRequest().getAttr().put("yurl", Constants.getArticleUri() + currentUri);
        getRequest().getAttr().put("data", data);
        long totalPage = BigDecimal.valueOf(Math.ceil(data.getTotalElements() * 1.0 / pageRequest.getSize())).longValue();
        if (totalPage > 0) {
            //大于1页
            if (totalPage > 1) {
                PagerVO pager = PagerUtil.generatorPager(currentUri, pageRequest.getPage(), totalPage);
                String suffix = StaticSitePlugin.getSuffix(request, Boolean.TRUE.equals(cacheService.getPublicWebSiteInfo().getGenerator_html_status()));
                for (PagerVO.PageEntry pageMap : pager.getPageList()) {
                    pageMap.setUrl(WebTools.buildEncodedUrl(request, Constants.getArticleUri() + pageMap.getUrl()) + suffix);
                }
                pager.setPageStartUrl(WebTools.buildEncodedUrl(request, Constants.getArticleUri() + pager.getPageStartUrl() + suffix));
                pager.setPageEndUrl(WebTools.buildEncodedUrl(request, Constants.getArticleUri() + pager.getPageEndUrl() + suffix));
                //pager
                getRequest().getAttr().put("pager", pager);
            }
        }

    }

    @RequestMethod
    public String index() {
        PageRequest pageRequest = new PageRequestImpl(parseUriInfo(request.getUri()).getPage(), cacheService.getPublicWebSiteInfo().getRows());
        PageData<ArticleBasicDTO> data = articles.get().visitorFindHome(pageRequest);
        setPageDataInfo("all-", data, pageRequest);
        return "index";
    }

    public String search() {
        String key = request.getParaToStr("key", "");
        if (StringUtils.isEmpty(key) && request.getMethod() == HttpMethod.POST) {
            return index();
        }
        ArticleUriInfoVO uriInfoVO = parseUriInfo(request.getUri());
        PageData<ArticleBasicDTO> data;
        if (StringUtils.isEmpty(key) && request.getMethod() == HttpMethod.GET) {
            try {
                key = uriInfoVO.getKey();
            } catch (Exception e) {
                LOGGER.warning("Parse " + request.getUri() + " error " + e.getMessage());
            }
        }
        if (StringUtils.isEmpty(key)) {
            return index();
        }
        Long rows = cacheService.getPublicWebSiteInfo().getRows();
        PageRequest pageRequest = new PageRequestImpl(uriInfoVO.getPage(), rows);
        data = articles.get().visitorFind(pageRequest, key);
        // 记录回话的Key
        request.getAttr().put("key", WebTools.htmlEncode(key));
        request.getAttr().put("tipsType", I18nUtil.getBlogStringFromRes("search"));
        request.getAttr().put("tipsName", WebTools.htmlEncode(key));

        setPageDataInfo("search/" + key + "-", data, pageRequest);
        return "page";
    }

    @RequestMethod
    public String record() {
        ArticleUriInfoVO uriInfoVO = parseUriInfo(request.getUri());
        request.getAttr().put("tipsType", I18nUtil.getBlogStringFromRes("archive"));
        request.getAttr().put("tipsName", uriInfoVO.getKey());

        Long rows = cacheService.getPublicWebSiteInfo().getRows();
        setPageDataInfo(
                "record/" + uriInfoVO.getKey() + "-",
                articles.get().findByDate(uriInfoVO.getPage(), rows, uriInfoVO.getKey()),
                new PageRequestImpl(uriInfoVO.getPage(), rows));
        return "page";
    }

    @RequestMethod(method = HttpMethod.POST)
    public void addComment() throws IOException, URISyntaxException, InterruptedException {
        saveComment();
    }

    @RequestMethod(method = HttpMethod.POST)
    public void saveComment() throws IOException, URISyntaxException, InterruptedException {
        PluginCorePlugin pluginCorePlugin = pluginCore.get();
        TokenService tokens = tokenService.get();
        AdminTokenVO adminTokenVO = tokens == null ? null : tokens.getAdminTokenVO(request);
        pluginCorePlugin.accessPlugin("/p/comment/addComment", request, response, adminTokenVO);
    }

    @RequestMethod
    public String detail() throws SQLException {
        String uri = getRequest().getUri();
        ArticleDetailDTO detail = articleService.detail(uri.replace("/", "").replace(".html", ""), request);
        if (Objects.nonNull(detail)) {
            request.getAttr().put("log", detail);
        }
        return "detail";
    }

    @RequestMethod
    public String sort() throws SQLException {
        ArticleUriInfoVO uriInfoVO = parseUriInfo(request.getUri());
        Long rows = cacheService.getPublicWebSiteInfo().getRows();
        setPageDataInfo(
                "sort/" + uriInfoVO.getKey() + "-",
                articles.get().findByTypeAlias(uriInfoVO.getPage(), rows, uriInfoVO.getKey()),
                new PageRequestImpl(uriInfoVO.getPage(), rows));
        request.getAttr().put("tipsType", I18nUtil.getBlogStringFromRes("category"));
        Optional<TypeDTO> first = cacheService.getArticleTypes().stream().filter(e -> Objects.equals(e.getAlias(), uriInfoVO.getKey())).findFirst();
        if (first.isPresent()) {
            request.getAttr().put("tipsName", first.get().getTypeName());
        } else {
            request.getAttr().put("tipsName", "");
        }
        return "page";
    }

    @RequestMethod
    public String tag() throws SQLException {
        ArticleUriInfoVO uriInfoVO = parseUriInfo(request.getUri());
        String tag = uriInfoVO.getKey();
        Long rows = cacheService.getPublicWebSiteInfo().getRows();
        setPageDataInfo(
                "tag/" + tag + "-",
                articles.get().findByTag(uriInfoVO.getPage(), rows, tag),
                new PageRequestImpl(uriInfoVO.getPage(), rows));
        getRequest().getAttr().put("tipsType", I18nUtil.getBlogStringFromRes("tag"));
        getRequest().getAttr().put("tipsName", tag);
        return "page";
    }

    @RequestMethod
    public String tags() {
        return aggregatePage("tags", "tag");
    }

    @RequestMethod
    public String link() {
        return links();
    }

    @RequestMethod
    public String links() {
        return aggregatePage("links", "link");
    }

    @RequestMethod
    public String archives() {
        return aggregatePage("archives", "archive");
    }

    @RequestMethod
    public String categories() {
        return aggregatePage("categories", "category");
    }

    private String aggregatePage(String templateName, String i18nKey) {
        request.getAttr().put("tipsType", I18nUtil.getBlogStringFromRes(i18nKey));
        request.getAttr().put("tipsName", "");
        Long rows = cacheService.getPublicWebSiteInfo().getRows();
        setPageDataInfo(templateName, new PageData<>(), new PageRequestImpl(1L, rows));
        return templateName;
    }

}
