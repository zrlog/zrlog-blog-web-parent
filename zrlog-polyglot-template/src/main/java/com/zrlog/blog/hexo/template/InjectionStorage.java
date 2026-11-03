package com.zrlog.blog.hexo.template;

import com.hibegin.common.util.EnvKit;
import com.hibegin.common.util.LoggerUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import org.graalvm.polyglot.HostAccess;

public class InjectionStorage {
    private static final Logger LOGGER = LoggerUtil.getLogger(InjectionStorage.class);
    private final Map<String, List<String>> injectionPoints;
    private final String templateDir;

    public InjectionStorage(Map<String, List<String>> injectionPoints, String templateDir) {
        this.injectionPoints = injectionPoints;
        this.templateDir = templateDir;
    }

    // 必须是 public，且建议显式允许访问
    @HostAccess.Export
    public void add(String slot, String path) {
        if (Objects.isNull(templateDir)) {
            return;
        }
        String rPath = path.substring(this.templateDir.length() + 1);
        if (EnvKit.isDevMode()) {
            LOGGER.info("Inject success: [" + slot + "] -> " + rPath);
        }
        injectionPoints.computeIfAbsent(slot, k -> new ArrayList<>()).add(rPath);
    }

    public List<String> get(String pointName) {
        return injectionPoints.get(pointName);
    }
}
