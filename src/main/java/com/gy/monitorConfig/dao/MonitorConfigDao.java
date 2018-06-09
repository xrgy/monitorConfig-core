package com.gy.monitorConfig.dao;

import com.gy.monitorConfig.entity.AlertAvlRuleMonitorEntity;
import com.gy.monitorConfig.entity.AlertPerfRuleMonitorEntity;
import com.gy.monitorConfig.entity.TestEntity;
import com.gy.monitorConfig.entity.metric.*;

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
}
