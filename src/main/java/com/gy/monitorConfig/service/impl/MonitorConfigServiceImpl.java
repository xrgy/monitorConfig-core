package com.gy.monitorConfig.service.impl;

import com.gy.monitorConfig.common.MonitorConfigEnum;
import com.gy.monitorConfig.dao.MonitorConfigDao;
import com.gy.monitorConfig.entity.AlertAvlRuleMonitorEntity;
import com.gy.monitorConfig.entity.AlertPerfRuleMonitorEntity;
import com.gy.monitorConfig.entity.TestEntity;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    String ALERT_RULE_TEMPLATE_PATH = "/template/alert_rule";

    @Override
    public TestEntity getJPAInfo() {
        return dao.getJPAInfo();
    }

    @Override
    public CompletionStage<String> initAlertRule(String configTemplateName, List<Map<String, Object>> param) {
        return CompletableFuture.supplyAsync(()-> {
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
                                        .filter(x->monitorMode.equals(x.getName())).findFirst();
        List<LightTypeEntity> lightTypeList = monitorService.getLightTypeEntity();
        Optional<LightTypeEntity> lightEntity = lightTypeList.stream()
                                        .filter(x->lightType.equals(x.getName())).findFirst();
        if (collection.isPresent() && lightEntity.isPresent()){
            List<Metrics> infos =dao.getMetricByTypeAndMode(lightEntity.get().getUuid(),collection.get().getUuid());
            ResMetricInfo resMetricInfo = new ResMetricInfo();
            resMetricInfo.setMonitorMode(monitorMode);
            List<MetricInfo> available = new ArrayList<>();
            List<MetricInfo> performance = new ArrayList<>();
            infos.forEach(info->{
                MetricInfo metricInfo = new MetricInfo();
                BeanUtils.copyProperties(info,metricInfo);
                metricInfo.setCollectionName(collection.get().getName());
                Optional<MetricsType> type= typeList.stream().filter(x->info.getMetricTypeId().equals(x.getUuid())).findFirst();
                type.ifPresent(metricsType -> metricInfo.setTypeName(metricsType.getName()));
                Optional<MetricsGroup> group = groupList.stream().filter(x->info.getMetricGroupId().equals(x.getUuid())).findFirst();
                if (group.isPresent()){
                    String groupName = group.get().getName();
                    metricInfo.setGroupName(groupName);
                    if (groupName.equals(MonitorConfigEnum.MetricGroupEnum.GROUP_AVAILABLE.value())){
                        available.add(metricInfo);
                    }else if (groupName.equals(MonitorConfigEnum.MetricGroupEnum.GROUP_PERFORMANCE.value())){
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
}
