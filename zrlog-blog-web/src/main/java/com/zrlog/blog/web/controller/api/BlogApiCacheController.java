package com.zrlog.blog.web.controller.api;

import com.hibegin.http.HttpMethod;
import com.hibegin.http.annotation.RequestMethod;
import com.hibegin.http.annotation.ResponseBody;
import com.hibegin.http.server.web.Controller;
import com.zrlog.blog.business.rest.response.ApiStandardResponse;
import com.zrlog.common.CacheService;
import com.zrlog.common.cache.vo.BaseDataInitVO;
import jakarta.inject.Inject;

public class BlogApiCacheController extends Controller {

    private final CacheService cacheService;

    @Inject
    public BlogApiCacheController(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @ResponseBody
    @RequestMethod(method = HttpMethod.GET)
    public ApiStandardResponse<BaseDataInitVO> index() {
        return new ApiStandardResponse<>(cacheService.getInitData());
    }
}
