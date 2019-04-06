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
//    public AlertAvlRuleMonitorEntity getAvlRuleMonitor(String name);

    /**
     * 通过性能性告警规则名获取规则监控实体
     * @param name
     * @return
     */
//    public AlertPerfRuleMonitorEntity getPerfRuleMonitor(String name);

//    /**
//     * 获取指标全部收集方式
//     * @return
//     */
//    public List<MetricsCollection> getMetricsCollection();
//
//    /**
//     * 获取指标全部分组
//     * @return
//     */
//    public List<MetricsGroup> getMetricsGroup();
//
//    /**
//     * 获取指标全部类型
//     * @return
//     */
//    public List<MetricsType> getMetricsType();

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
    public  List<AlertRuleTemplateEntity> isTemplateNameDup(String name);

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
     * @return
     */
    public List<AlertRuleTemplateEntity> getTemplateByLightType(String lightType);

    /**
     * 通过模板id获取模板实体
     * @param uuid
     * @return
     */
    public AlertRuleTemplateEntity getTemplateByUuid(String uuid);
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
//    boolean addTemplateMonitor(AlertRuleTemplateMonitorEntity templateMonitorEntity);

    /**
     * 通过三级规格获取该资源的指标列表
     * @param lightTypeId
     * @return
     */
    List<Metrics> getMetricsByLightType(String lightTypeId);

    /**
     * 删除模板监控实体
     * @param uuid
     * @return
     */
    boolean delTemplateMonitorByMonitorUuid(String uuid);

    /**
     * 删除可用性监控实体
     * @param uuid
     * @return
     */
    boolean delAvlMonitorByMonitorUuid(String uuid);

    /**
     * 删除性能监控实体
     * @param uuid
     * @return
     */
    boolean delPerfMonitorByMonitorUuid(String uuid);


    /**
     * 根据monitoruuid获取模板监控实体
     * @param uuid
     * @return
     */
//    AlertRuleTemplateMonitorEntity getTemplateMonitorByMonitorUuid(String uuid);

    /**
     * 删除监控模板实体
     * @param uuid
     * @return
     */
    boolean delTemplateByTemplateUuid(String uuid);


    /**
     * 删除可用性模板
     * @param uuid
     * @return
     */
    boolean delAvlByTemplateUuid(String uuid);

    /**
     * 删除性能模板
     * @param uuid
     * @return
     */
    boolean delPerfByTemplateUuid(String uuid);


    /**
     * 根据指标id获取指标信息
     * @param uuid
     * @return
     */
    Metrics getMetricsByUuid(String uuid);

    /**
     * 获取所有监控模板
     * @return
     */
    List<AlertRuleTemplateEntity> getAllTemplate(List<String> type);

    List<AlertRuleTemplateEntity> getTemplateByPage(int startIndex,int pageSize,List<String> type);

    /**
     * 通过templateid获取监控模板实体
     * @param uuid
     * @return
     */
//    List<AlertRuleTemplateMonitorEntity> getTemplateMonitorByTemplateUuid(String uuid);

    /**
     * 通过monitorid获取可用性监控实体列表
     * @param monitorId
     * @return
     */
//    List<AlertAvlRuleMonitorEntity> getAvlRuleMonitorByMonitorId(String monitorId);

    /**
     * 通过monitorid获取性能监控实体列表
     * @param monitorId
     * @return
     */
//    List<AlertPerfRuleMonitorEntity> getPerfRuleMonitorByMonitorId(String monitorId);


    /**
     * 查看该规则是否存在
     * @param uuid
     * @return
     */
    List<AlertAvlRuleEntity> getAvlRuleByRuleUuid(String uuid);

    /**
     * 查看该规则是否存在
     * @param uuid
     * @return
     */
    List<AlertPerfRuleEntity> getPerfRuleByRuleUuid(String uuid);


    boolean delPerfByUuid(String uuid);

    boolean delPerfByTemAndMetric(String uuid, String metricUuid);
}
