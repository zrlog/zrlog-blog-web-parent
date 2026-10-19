package com.zrlog.blog.web.plugin;

import com.hibegin.common.BaseLockObject;
import com.hibegin.common.dao.DAO;
import com.hibegin.common.util.EnvKit;
import com.hibegin.common.util.LoggerUtil;
import com.hibegin.common.util.StringUtils;
import com.zrlog.business.plugin.RequestInfo;
import com.zrlog.model.Log;
import com.zrlog.plugin.BaseStaticSitePlugin;
import com.zrlog.util.UserAgentUtils;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ArticleStatisticsRunnable extends BaseLockObject implements Runnable {
    private final Supplier<String> webCm;

    public ArticleStatisticsRunnable(Supplier<String> webCm) {
        this.webCm = Objects.requireNonNull(webCm);
    }

    private static final Logger LOGGER = LoggerUtil.getLogger(ArticleStatisticsRunnable.class);

    /**
     * 保留2分钟的，2分钟内不重复记数量
     */
    private static final long REMOVE_TIME = 2 * 1000 * 60L;
    private final List<RequestInfo> requestInfoList = Collections.synchronizedList(new ArrayList<>());

    /**
     *
     */
    private void clickAdd(Long logId) throws SQLException {
        new DAO().execute("update " + Log.TABLE_NAME + " set click = click + 1  where logId=?", logId);
    }

    @Override
    public void run() {
        lock.lock();
        try {
            List<RequestInfo> removeList = new ArrayList<>();
            for (RequestInfo requestInfo : requestInfoList) {
                if (!requestInfo.isDeal()) {
                    if (BaseStaticSitePlugin.STATIC_USER_AGENT.equals(requestInfo.getUserAgent())) {
                        continue;
                    }
                    if (UserAgentUtils.parse(requestInfo.getUserAgent()).isCrawler()) {
                        continue;
                    }
                    clickAdd(requestInfo.getArticleId());
                    requestInfo.setDeal(true);
                }
                if (System.currentTimeMillis() - requestInfo.getRequestTime() > REMOVE_TIME) {
                    removeList.add(requestInfo);
                }
            }
            requestInfoList.removeAll(removeList);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Save error", e);
        } finally {
            lock.unlock();
        }
    }

    private boolean isDisabled() {
        String webCmStr = webCm.get();
        if (StringUtils.isEmpty(webCmStr)) {
            return false;
        }
        Elements select = Jsoup.parse(webCmStr).body().select("plugin[name=statistics]");
        return select.isEmpty();
    }

    public void addTask(RequestInfo requestInfo) {
        if (isDisabled()) {
            return;
        }
        requestInfoList.add(requestInfo);
        if (EnvKit.isFaaSMode()) {
            this.run();
        }
    }
}
