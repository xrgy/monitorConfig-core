package com.gy.monitorConfig.service.impl;

import com.gy.monitorConfig.common.MonitorConfigEnum;
import com.gy.monitorConfig.dao.MonitorConfigDao;
import com.gy.monitorConfig.entity.*;
import com.gy.monitorConfig.entity.metric.*;
import com.gy.monitorConfig.entity.monitor.LightTypeEntity;
import com.gy.monitorConfig.service.MonitorConfigService;
import com.gy.monitorConfig.service.MonitorService;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;


/**
 * Created by gy on 2018/3/31.
 */
@Service
public class MonitorConfigServiceImpl implements MonitorConfigService {

    @Autowired
    MonitorConfigDao dao;

    @Autowired
    MonitorService monitorService;

    private static final String ALERT_RULE_TEMPLATE_PATH = "/template/alert_rule";
    private static final String LEVEL_ONE = "one";
    private static final String LEVEL_TWO = "two";

    @Override
    public TestEntity getJPAInfo() {
        return dao.getJPAInfo();
    }

    @Override
    public CompletionStage<String> initAlertRule(String configTemplateName, List<Map<String, Object>> param) {
        return CompletableFuture.supplyAsync(() -> {
            final StringWriter sw = new StringWriter();
            try {
                final String loadPath = this.getClass().getResource("/").getPath();
                final String vmFilePath = loadPath + ALERT_RULE_TEMPLATE_PATH;
                final VelocityEngine ve = new VelocityEngine();
                ve.setProperty(VelocityEngine.FILE_RESOURCE_LOADER_PATH, vmFilePath);
                ve.setProperty("directive.set.null.allowed", true);
                ve.init();
                final Template velocityTemplate = ve.getTemplate(configTemplateName + ".vm", "UTF-8");
                if (null != velocityTemplate) {
                    return sw.toString();
                }
                final VelocityContext velocityContext = new VelocityContext();
                velocityContext.put("param", param);
                velocityTemplate.merge(velocityContext, sw);
                sw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "";
        });
    }

    @Override
    public AlertAvlRuleMonitorEntity getAvlRuleMonitor(String name) {
        return dao.getAvlRuleMonitor(name);
    }

    @Override
    public AlertPerfRuleMonitorEntity getPerfRuleMonitor(String name) {
        return dao.getPerfRuleMonitor(name);
    }

    @Override
    public ResMetricInfo getMetricInfo(String lightType, String monitorMode) {
        List<MetricsCollection> collectionList = dao.getMetricsCollection();
        List<MetricsGroup> groupList = dao.getMetricsGroup();
        List<MetricsType> typeList = dao.getMetricsType();
        Optional<MetricsCollection> collection = collectionList.stream()
                .filter(x -> monitorMode.equals(x.getName())).findFirst();
        List<LightTypeEntity> lightTypeList = monitorService.getLightTypeEntity();
        Optional<LightTypeEntity> lightEntity = lightTypeList.stream()
                .filter(x -> lightType.equals(x.getName())).findFirst();
        if (collection.isPresent() && lightEntity.isPresent()) {
            List<Metrics> infos = dao.getMetricByTypeAndMode(lightEntity.get().getUuid(), collection.get().getUuid());
            ResMetricInfo resMetricInfo = new ResMetricInfo();
            resMetricInfo.setMonitorMode(monitorMode);
            List<MetricInfo> available = new ArrayList<>();
            List<MetricInfo> performance = new ArrayList<>();
            infos.forEach(info -> {
                MetricInfo metricInfo = new MetricInfo();
                BeanUtils.copyProperties(info, metricInfo);
                metricInfo.setCollectionName(collection.get().getName());
                Optional<MetricsType> type = typeList.stream().filter(x -> info.getMetricTypeId().equals(x.getUuid())).findFirst();
                type.ifPresent(metricsType -> metricInfo.setTypeName(metricsType.getName()));
                Optional<MetricsGroup> group = groupList.stream().filter(x -> info.getMetricGroupId().equals(x.getUuid())).findFirst();
                if (group.isPresent()) {
                    String groupName = group.get().getName();
                    metricInfo.setGroupName(groupName);
                    if (groupName.equals(MonitorConfigEnum.MetricGroupEnum.GROUP_AVAILABLE.value())) {
                        available.add(metricInfo);
                    } else if (groupName.equals(MonitorConfigEnum.MetricGroupEnum.GROUP_PERFORMANCE.value())) {
                        performance.add(metricInfo);
                    }
                }

            });
            resMetricInfo.setAvailable(available);
            resMetricInfo.setPerformance(performance);
            return resMetricInfo;
        }
        return null;
    }

    @Override
    public boolean isTemplateNameDup(String name) {
        return dao.isTemplateNameDup(name);
    }

    @Override
    public boolean addTemplate(NewTemplateView view) {
        AlertRuleTemplateEntity templateEntity = new AlertRuleTemplateEntity();
        templateEntity.setUuid(UUID.randomUUID().toString());
        BeanUtils.copyProperties(view, templateEntity);
        templateEntity.setCreateTime(new Date());
        boolean isInsertTemp = dao.addTemplate(templateEntity);
        if (isInsertTemp) {
            view.getAvailable().forEach(resav -> {
                resav.getData().forEach(ava -> {
                    AlertAvlRuleEntity avlRuleEntity = new AlertAvlRuleEntity();
                    avlRuleEntity.setUuid(UUID.randomUUID().toString());
                    avlRuleEntity.setTemplateUuid(templateEntity.getUuid());
                    avlRuleEntity.setMetricUuid(ava.getUuid());
                    avlRuleEntity.setSeverity(Integer.parseInt(ava.getSeverity()));
                    avlRuleEntity.setDescription(ava.getDescription());
                    dao.addAvlRule(avlRuleEntity);
                });
            });
            view.getPerformance().forEach(resper -> {
                resper.getData().forEach(perf -> {
                    if (null != perf.getLevelOneFirstThreshold()) {
                        AlertPerfRuleEntity perfRuleEntity = new AlertPerfRuleEntity();
                        perfRuleEntity.setUuid(UUID.randomUUID().toString());
                        perfRuleEntity.setTemplateUuid(templateEntity.getUuid());
                        perfRuleEntity.setMetricUuid(perf.getUuid());
                        BeanUtils.copyProperties(convert2CommonRule(perf,LEVEL_ONE),perfRuleEntity);
                        perfRuleEntity.setAlertLevel(LEVEL_ONE);
                        perfRuleEntity.setDescription(perf.getDescription());
                        dao.addPerfRule(perfRuleEntity);
                    }
                    if (null != perf.getLevelTwoFirstThreshold()){
                        AlertPerfRuleEntity perfRuleEntity1 = new AlertPerfRuleEntity();
                        perfRuleEntity1.setUuid(UUID.randomUUID().toString());
                        perfRuleEntity1.setTemplateUuid(templateEntity.getUuid());
                        perfRuleEntity1.setMetricUuid(perf.getUuid());
                        BeanUtils.copyProperties(convert2CommonRule(perf,LEVEL_TWO),perfRuleEntity1);
                        perfRuleEntity1.setAlertLevel(LEVEL_TWO);
                        perfRuleEntity1.setDescription(perf.getDescription());
                        dao.addPerfRule(perfRuleEntity1);
                    }
                });
            });

            return true;
        }
        return false;
    }

    private AlertRuleEntity convert2CommonRule(MetricInfo info,String level){
        AlertRuleEntity ruleEntity = new AlertRuleEntity();
        if (level.equals(LEVEL_ONE)){
            ruleEntity.setSeverity(Integer.parseInt(info.getLevelOneSeverity()));
            ruleEntity.setAlertFirstCondition(Integer.parseInt(info.getLevelOneAlertFirstCondition()));
            ruleEntity.setFirstThreshold(info.getLevelOneFirstThreshold());
            ruleEntity.setExpressionMore(info.getLevelOneExpressionMore());
            ruleEntity.setAlertSecondCondition(Integer.parseInt(info.getLevelOneAlertSecondCondition()));
            if (null != info.getLevelOneSecondThreshold()){
                ruleEntity.setSecondThreshold(info.getLevelOneSecondThreshold());
            }
        }else {
            ruleEntity.setSeverity(Integer.parseInt(info.getLevelTwoSeverity()));
            ruleEntity.setAlertFirstCondition(Integer.parseInt(info.getLevelTwoAlertFirstCondition()));
            ruleEntity.setFirstThreshold(info.getLevelTwoFirstThreshold());
            ruleEntity.setExpressionMore(info.getLevelTwoExpressionMore());
            ruleEntity.setAlertSecondCondition(Integer.parseInt(info.getLevelTwoAlertSecondCondition()));
            if (null != info.getLevelTwoSecondThreshold()){
                ruleEntity.setSecondThreshold(info.getLevelTwoSecondThreshold());
            }
        }
        return ruleEntity;

    }
}
