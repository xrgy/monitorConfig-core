package com.gy.monitorConfig.service;

import com.gy.monitorConfig.entity.AlertAvlRuleMonitorEntity;
import com.gy.monitorConfig.entity.AlertPerfRuleMonitorEntity;
import com.gy.monitorConfig.entity.TestEntity;
import com.gy.monitorConfig.entity.metric.NewTemplateView;
import com.gy.monitorConfig.entity.metric.ResMetricInfo;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Created by gy on 2018/3/31.
 */
public interface MonitorConfigService {
    public TestEntity getJPAInfo();

    public CompletionStage<String> initAlertRule(String configTemplateName, List<Map<String,Object>> param);

    /**
     * 通过可用性告警规则名获取规则监控实体
     * @param name
     * @return
     */
    public AlertAvlRuleMonitorEntity getAvlRuleMonitor(String name);

    /**
     * 通过性能性告警规则名获取规则监控实体
     * @param name
     * @return
     */
    public AlertPerfRuleMonitorEntity getPerfRuleMonitor(String name);

    /**
     * 根据三级规格和监控方式获取指标数据
     * @param lightType
     * @param monitorMode
     * @return
     */
    public ResMetricInfo getMetricInfo(String lightType, String monitorMode);

    /**
     * 判断模板名字是否被使用
     * @param name
     * @return
     */
    public boolean isTemplateNameDup(String name);

    /**
     * 新建模板
     * @param view
     * @return
     */
    public boolean addTemplate(NewTemplateView view);

}
