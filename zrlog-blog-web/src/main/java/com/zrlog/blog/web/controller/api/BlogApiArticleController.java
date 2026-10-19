package com.zrlog.blog.web.controller.api;

import com.hibegin.common.dao.dto.PageData;
import com.hibegin.http.HttpMethod;
import com.hibegin.http.annotation.RequestMethod;
import com.hibegin.http.annotation.ResponseBody;
import com.hibegin.http.server.web.Controller;
import com.zrlog.blog.business.rest.response.ApiStandardResponse;
import com.zrlog.blog.business.service.ArticleService;
import com.zrlog.blog.business.service.ArticleService.ArticleListOrder;
import com.zrlog.business.util.ControllerUtil;
import com.zrlog.data.dto.ArticleBasicDTO;
import com.zrlog.data.dto.ArticleDetailDTO;
import com.zrlog.data.dto.VisitorCommentDTO;
import com.zrlog.model.Comment;

import jakarta.inject.Inject;

import java.sql.SQLException;
import java.util.List;

public class BlogApiArticleController extends Controller {

    private final ArticleService articleService;

    private final jakarta.inject.Provider<Comment> comments;

    @Inject
    public BlogApiArticleController(
            ArticleService articleService, jakarta.inject.Provider<Comment> comments) {
        this.comments = comments;
        this.articleService = articleService;
    }

    @ResponseBody
    @RequestMethod(method = HttpMethod.GET)
    public ApiStandardResponse<ArticleDetailDTO> detail() throws SQLException {
        return new ApiStandardResponse<>(articleService.detail(request.getParaToStr("id", ""), request));
    }

    @ResponseBody
    @RequestMethod(method = HttpMethod.GET)
    public ApiStandardResponse<PageData<ArticleBasicDTO>> index() {
        String key = getRequest().getParaToStr("key", "");
        ArticleListOrder listOrder = getRequest().getParaToBool("feed", false)
                ? ArticleListOrder.NEWEST_FIRST
                : ArticleListOrder.STICKY_FIRST;
        return new ApiStandardResponse<>(articleService.pageByKeywords(
                ControllerUtil.getPageRequest(this), key, getRequest(), listOrder));
    }

    @ResponseBody
    @RequestMethod(method = HttpMethod.GET)
    public ApiStandardResponse<List<VisitorCommentDTO>> comment() throws SQLException {
        return new ApiStandardResponse<>(
                comments.get()
                        .visitorFindAllByLogId(Integer.parseInt(request.getParaToStr("id", ""))));
    }
}
