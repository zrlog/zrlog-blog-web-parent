package com.zrlog.blog.business.service;

import com.hibegin.common.dao.ResultValueConvertUtils;
import com.hibegin.common.dao.dto.PageData;
import com.hibegin.common.dao.dto.PageRequest;
import com.hibegin.common.util.ObjectHelpers;
import com.hibegin.common.util.StringUtils;
import com.hibegin.common.util.UrlEncodeUtils;
import com.hibegin.http.server.api.HttpRequest;
import com.zrlog.blog.web.util.OutlineUtil;
import com.zrlog.blog.web.util.WebTools;
import com.zrlog.business.plugin.StaticSitePlugin;
import com.zrlog.business.util.ArticleHelpers;
import com.zrlog.common.CacheService;
import com.zrlog.common.Constants;
import com.zrlog.common.vo.Outline;
import com.zrlog.common.vo.PublicWebSiteInfo;
import com.zrlog.data.dto.ArticleBasicDTO;
import com.zrlog.data.dto.ArticleDetailDTO;
import com.zrlog.model.Log;
import com.zrlog.util.I18nUtil;
import com.zrlog.util.ParseUtil;
import com.zrlog.util.ZrLogUtil;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ArticleService {

    private final CacheService cacheService;

    public ArticleService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    public enum ArticleListOrder {
        STICKY_FIRST,
        NEWEST_FIRST
    }

    private static List<ArticleDetailDTO.TagsDTO> getTags(ArticleBasicDTO log, HttpRequest request, String suffix) {
        String keywords = ObjectHelpers.requireNonNullElse(log.getKeywords(), "");
        List<ArticleDetailDTO.TagsDTO> tags = new ArrayList<>();
        for (String tag : keywords.split(",")) {
            if (StringUtils.isEmpty(tag.trim())) {
                continue;
            }
            ArticleDetailDTO.TagsDTO map = new ArticleDetailDTO.TagsDTO();
            map.setName(tag);
            map.setUrl(WebTools.buildEncodedUrl(request, "tag/" + tag + suffix));
            tags.add(map);
        }
        return tags;
    }

    private ArticleDetailDTO fillArticleInfo(ArticleDetailDTO log, HttpRequest request) {
        handlerArticle(log, request, cacheService);
        String suffix = StaticSitePlugin.getSuffix(request, Boolean.TRUE.equals(cacheService.getPublicWebSiteInfo().getGenerator_html_status()));
        log.getNextLog().setUrl(WebTools.buildEncodedUrl(request, Constants.getArticleUri() + log.getNextLog().getAlias() + suffix));
        log.getLastLog().setUrl(WebTools.buildEncodedUrl(request, Constants.getArticleUri() + log.getLastLog().getAlias() + suffix));

        if (StringUtils.isNotEmpty(log.getMarkdown())) {
            List<Outline> outlineVO = OutlineUtil.extractOutline(log.getContent());
            if (!outlineVO.isEmpty()) {
                log.setTocHtml(OutlineUtil.buildTocHtml(outlineVO, ""));
            }
            log.setToc(outlineVO);
        }
        return log;
    }

    public ArticleDetailDTO detail(Object idOrAlias, HttpRequest request) throws SQLException {
        ArticleDetailDTO log = new Log().findByIdOrAlias(idOrAlias, Boolean.TRUE.equals(cacheService.getPublicWebSiteInfo().getDisable_comment_status()));
        if (Objects.isNull(log)) {
            return null;
        }
        if (log.getNextLog() == null) {
            ArticleDetailDTO.NextLogDTO nextLogDTO = new ArticleDetailDTO.NextLogDTO();
            nextLogDTO.setAlias(idOrAlias + "");
            nextLogDTO.setTitle(I18nUtil.getBlogStringFromRes("noNextLog"));
            log.setNextLog(nextLogDTO);
        }
        //
        if (log.getLastLog() == null) {
            ArticleDetailDTO.LastLogDTO lastLogDTO = new ArticleDetailDTO.LastLogDTO();
            lastLogDTO.setAlias(idOrAlias + "");
            lastLogDTO.setTitle(I18nUtil.getBlogStringFromRes("noLastLog"));
            log.setLastLog(lastLogDTO);
        }
        return fillArticleInfo(log, request);
    }

    public static <T extends ArticleBasicDTO> T handlerArticle(T log, HttpRequest request, CacheService cacheService) {
        String suffix = StaticSitePlugin.getSuffix(request, Boolean.TRUE.equals(cacheService.getPublicWebSiteInfo().getGenerator_html_status()));
        String originalAlias = log.getAlias();
        String aliasUrl = UrlEncodeUtils.encodeUrl(originalAlias) + suffix;
        String articleUrl = WebTools.buildEncodedUrl(request, Constants.getArticleUri() + originalAlias + suffix);
        log.setAlias(aliasUrl);
        log.setUrl(articleUrl);
        log.setRubbish(ResultValueConvertUtils.toBoolean(log.getRubbish()));
        log.setPrivacy(ResultValueConvertUtils.toBoolean(log.getPrivacy()));
        log.setHot(ResultValueConvertUtils.toBoolean(log.getHot()));
        PublicWebSiteInfo publicWebSiteInfo = cacheService.getPublicWebSiteInfo();
        log.setCanComment(ResultValueConvertUtils.toBoolean(log.getCanComment()) && Objects.equals(publicWebSiteInfo.getDisable_comment_status(), false));
        log.setTypeUrl(WebTools.buildEncodedUrl(request, Constants.getArticleUri() + "sort/" + log.getTypeAlias() + suffix));
        log.setNoSchemeUrl(ZrLogUtil.getHomeUrlWithHost(request, publicWebSiteInfo.getHost()) + Constants.getArticleUri() + aliasUrl);
        log.setCommentUrl(ZrLogUtil.getHomeUrlWithHost(request, publicWebSiteInfo.getHost()) + Constants.getArticleUri() + "addComment");
        //
        log.setRecommended(ResultValueConvertUtils.toBoolean(log.getRecommended()));
        Object lastUpdateDate = Objects.nonNull(log.getLastUpdateDate()) ? log.getLastUpdateDate()
                : log.getLast_update_date();
        log.setFullReleaseTime(formatDateValue(log.getReleaseTime(), "yyyy-MM-dd HH:mm:ss"));
        log.setFullLastUpdateDate(formatDateValue(lastUpdateDate, "yyyy-MM-dd HH:mm:ss"));
        log.setReleaseTime(formatDateValue(log.getReleaseTime(), "yyyy-MM-dd"));
        if (Objects.nonNull(log.getLogId())) {
            log.setId(log.getLogId());
        }
        log.setLastUpdateDate(formatDateValue(lastUpdateDate, "yyyy-MM-dd"));
        log.setLast_update_date(formatDateValue(lastUpdateDate, "yyyy-MM-dd"));
        if (Objects.isNull(log.getDigest())) {
            log.setDigest("");
        }
        if (Objects.isNull(log.getContent())) {
            log.setContent("");
        }
        if (publicWebSiteInfo.getArticle_thumbnail_status() && StringUtils.isNotEmpty(log.getThumbnail())) {
            log.setThumbnailAlt(ParseUtil.removeHtmlElement(log.getTitle()));
        } else {
            log.setThumbnail(null);
            log.setThumbnailAlt(null);
        }
        log.setTags(getTags(log, request, suffix));
        return log;
    }

    private static String formatDateValue(Object date, String format) {
        Long time = parseDateValue(date);
        if (Objects.isNull(time)) {
            if (Objects.isNull(date)) {
                return "";
            }
            return date.toString();
        }
        return new SimpleDateFormat(format).format(new Date(time));
    }

    private static Long parseDateValue(Object date) {
        try {
            return ResultValueConvertUtils.parseDate(date);
        } catch (RuntimeException e) {
            Long parsed = parseDbUtilsDateString(date);
            if (parsed != null) {
                return parsed;
            }
            throw e;
        }
    }

    private static Long parseDbUtilsDateString(Object date) {
        if (!(date instanceof String)) {
            return null;
        }
        String value = ((String) date).replace('\u202f', ' ').replace('\u00a0', ' ');
        List<DateFormat> formats = Arrays.asList(
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.getDefault()),
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.ENGLISH),
                new SimpleDateFormat("MMM d, yyyy, h:mm:ss a", Locale.ENGLISH)
        );
        for (DateFormat dateFormat : formats) {
            try {
                return dateFormat.parse(value).getTime();
            } catch (java.text.ParseException ignored) {
            }
        }
        return null;
    }

    public PageData<ArticleBasicDTO> pageByKeywords(PageRequest pageRequest, String keywords, HttpRequest request) {
        return pageByKeywords(pageRequest, keywords, request, ArticleListOrder.NEWEST_FIRST);
    }

    public PageData<ArticleBasicDTO> pageByKeywords(PageRequest pageRequest, String keywords, HttpRequest request,
                                                     ArticleListOrder listOrder) {
        Log log = new Log();
        PageData<ArticleBasicDTO> data = StringUtils.isEmpty(keywords)
                && listOrder == ArticleListOrder.STICKY_FIRST
                ? log.visitorFindHome(pageRequest)
                : log.visitorFind(pageRequest, keywords);
        ArticleHelpers.wrapperSearchKeyword(data, keywords, cacheService.getPublicWebSiteInfo().getArticle_auto_digest_length());
        data.setKey(keywords);
        data.getRows().forEach(e -> handlerArticle(e, request, cacheService));
        return data;
    }

}
