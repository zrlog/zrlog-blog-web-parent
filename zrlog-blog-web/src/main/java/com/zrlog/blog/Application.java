package com.zrlog.blog;

import com.hibegin.http.server.WebServerBuilder;
import com.hibegin.http.server.util.PathUtil;
import com.zrlog.blog.web.BlogWebSetup;
import com.zrlog.business.service.UpgradeNoticeService;
import com.zrlog.business.updater.UpdateVersionInfoPlugin;
import com.zrlog.common.CacheService;
import com.zrlog.common.Constants;
import com.zrlog.common.Updater;
import com.zrlog.common.ZrLogConfig;
import com.zrlog.plugin.IPlugin;
import com.zrlog.plugin.Plugins;
import com.zrlog.web.WebSetupContext;
import com.zrlog.web.WebSetupLoader;
import com.zrlog.web.inteceptor.DefaultInterceptor;

import java.util.List;
import java.util.Objects;

import static com.zrlog.common.Constants.getZrLogHome;

public class Application {

    static {
        System.setProperty("sws.run.mode", "dev");
        String home = getZrLogHome();
        if (Objects.nonNull(home)) {
            PathUtil.setRootPath(home);
        }
    }

    public static void main(String[] args) {
        Constants.zrLogConfig = new DevZrLogConfig(7080, null, "");
        WebServerBuilder build = new WebServerBuilder.Builder().config(Constants.zrLogConfig).build();
        build.start();
    }
}

class DevZrLogConfig extends ZrLogConfig {

    protected DevZrLogConfig(Integer port, Updater updater, String contextPath) {
        super(port, updater, contextPath);
        webSetups.addAll(WebSetupLoader.load(
                new WebSetupContext(this, dbPropertiesFile, installLockFile, contextPath, updater),
                provider -> provider.name().equals("data") || provider.name().equals("services")));
        webSetups.add(new BlogWebSetup(this, contextPath, false));
        initializeWebSetups();
        this.serverConfig.addInterceptor(DefaultInterceptor.class);
    }

    @Override
    public List<IPlugin> getBasePluginList() {
        Plugins basePlugin = new Plugins();
        var services = serverConfig.getBindings().build();
        basePlugin.add(new UpdateVersionInfoPlugin(services.get(UpgradeNoticeService.class),
                () -> services.get(CacheService.class).getPublicWebSiteInfo().getLanguage(), updater));
        return basePlugin;
    }
}
