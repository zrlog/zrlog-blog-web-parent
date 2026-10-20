package com.zrlog.blog.web.controller.api;

import com.hibegin.http.HttpMethod;
import com.hibegin.http.annotation.RequestMethod;
import com.hibegin.http.annotation.ResponseBody;
import com.hibegin.http.server.web.Controller;
import com.zrlog.blog.business.rest.response.ApiStandardResponse;
import com.zrlog.business.rest.response.PublicInfoVO;
import com.zrlog.business.service.CommonService;
import com.zrlog.util.BlogBuildInfoUtil;
import com.zrlog.util.I18nUtil;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class BlogApiPublicController extends Controller {

    private final CommonService commonService;

    @Inject
    public BlogApiPublicController(CommonService commonService) {
        this.commonService = commonService;
    }

    @ResponseBody
    @RequestMethod(method = HttpMethod.GET)
    public ApiStandardResponse<Map<String, Object>> blogResource() {
        return new ApiStandardResponse<>(_blogResourceInfo());
    }

    private Map<String, Object> _blogResourceInfo() {
        if (Objects.isNull(I18nUtil.threadLocal.get())) {
            return new HashMap<>();
        }
        Map<String, Object> stringObjectMap = new HashMap<>(I18nUtil.getBlog());
        PublicInfoVO publicInfoVO = commonService.getPublicInfo(request);
        stringObjectMap.put("websiteTitle", publicInfoVO.getWebsiteTitle());
        stringObjectMap.put("homeUrl", publicInfoVO.getHomeUrl());
        stringObjectMap.put("articleRoute", "");
        stringObjectMap.put("admin_darkMode", publicInfoVO.getAdmin_darkMode());
        stringObjectMap.put("buildId", BlogBuildInfoUtil.getBuildId());
        return stringObjectMap;
    }


}
