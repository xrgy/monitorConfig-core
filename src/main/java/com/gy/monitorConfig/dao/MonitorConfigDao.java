package com.gy.monitorConfig.dao;

import com.gy.monitorConfig.entity.*;
import com.gy.monitorConfig.entity.metric.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Created by gy on 2018/3/31.
 */
public interface MonitorConfigDao {
    public TestEntity getJPAInfo();

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
     * 获取指标全部收集方式
     * @return
     */
    public List<MetricsCollection> getMetricsCollection();

    /**
     * 获取指标全部分组
     * @return
     */
    public List<MetricsGroup> getMetricsGroup();

    /**
     * 获取指标全部类型
     * @return
     */
    public List<MetricsType> getMetricsType();

    /**
     * 通过三级规格id和监控方式获取指标
     * @param lightType
     * @param monitorMode
     * @return
     */
    List<Metrics> getMetricByTypeAndMode(String lightType, String monitorMode);

    /**
     * 判断模板名字是否被使用
     * @param name
     * @return
     */
    public boolean isTemplateNameDup(String name);

    /**
     * 持久化模板实体到数据库
     * @param templateEntity
     * @return
     */
    public boolean addTemplate(AlertRuleTemplateEntity templateEntity);


    /**
     * 持久化可用性规则到数据库
     * @param entity
     * @return
     */
    public boolean addAvlRule(AlertAvlRuleEntity entity);

    /**
     * 持久化性能规则到数据库
     * @param entity
     * @return
     */
    public boolean addPerfRule(AlertPerfRuleEntity entity);

    /**
     * 根据三级规格和监控模式获取监控模板
     * @param lightType
     * @param monitorMode
     * @return
     */
    public List<AlertRuleTemplateEntity> getTemplateByLightType(String lightType, String monitorMode);


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
     * 持久化可用性监控实体到数据库
     * @param x
     */
    public boolean addAvlRuleMonitor(AlertAvlRuleMonitorEntity x);

    /**
     * 持久化性能监控实体到数据库
     * @param x
     */
    public boolean addPerfRuleMonitor(AlertPerfRuleMonitorEntity x);

    /**
     * 持久化模板监控实体到数据库
     * @param templateMonitorEntity
     * @return
     */
    boolean addTemplateMonitor(AlertRuleTemplateMonitorEntity templateMonitorEntity);
}
