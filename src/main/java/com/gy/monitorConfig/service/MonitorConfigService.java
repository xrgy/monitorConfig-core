package com.gy.monitorConfig.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.gy.monitorConfig.entity.*;
import com.gy.monitorConfig.entity.metric.Metrics;
import com.gy.monitorConfig.entity.metric.NewTemplateView;
import com.gy.monitorConfig.entity.metric.ResMetricInfo;
import com.gy.monitorConfig.entity.metric.UpTemplateView;

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


    /**
     * 根据三级规格获取监控模板列表
     * @param lightType
     * @return
     */
    public String getTemplateByLightType(String lightType) throws JsonProcessingException;

    /**
     * 根据模板id获取可用性规则
     * @param templateId
     * @return
     */
    public List<AlertAvlRuleEntity> getAvlRuleByTemplateId(String templateId);

    /**
     * 根据模板id获取性能规则
     * @param templateId
     * @return
     */
    public List<AlertPerfRuleEntity> getPerfRuleByTemplateId(String templateId);

    /**
     * 持久化可用性监控实体列表到数据库
     * @param avlRuleMonitorList
     * @return
     */
    boolean addAvlRuleMonitorList(List<AlertAvlRuleMonitorEntity> avlRuleMonitorList);

    /**
     * 持久化性能监控实体列表到数据库
     * @param perfRuleMonitorList
     * @return
     */
    boolean addPerfRuleMonitorList(List<AlertPerfRuleMonitorEntity> perfRuleMonitorList);

    /**
     *
     * 持久化模板监控实体到数据库
     * @param templateMonitorEntity
     * @return
     */
    boolean addTemplateMonitor(AlertRuleTemplateMonitorEntity templateMonitorEntity);

    /**
     * 通过三级规格获取该资源指标列表
     * @param lightTypeId
     * @return
     */
    List<Metrics> getMetricsByLightType(String lightTypeId);

    /**
     * 组装模板到etcd
     * @param lightTypeId
     * @param templateId
     * @param ruleMonitorEntity
     */
    void addAlertTemplateToEtcd(String lightTypeId, String templateId, RuleMonitorEntity ruleMonitorEntity);

    /**
     * 删除告警规则监控模板
     * @param uuid 这个就是monitoruuid+templateuuid 去除'-'
     */
    void delAlertMonitorRule(String uuid);

    /**
     * 删除监控模板
     * @param id
     */
    void detTemplate(String id);

    /**
     * 修改监控模板
     * @param view
     * @return
     */
    boolean updateTemplate(UpTemplateView view);

    /**
     *修改打开某个监控模板(模板id)，返回
     * @param uuid
     * @return
     */
    UpTemplateView getOpenTemplateData(String uuid);
}
