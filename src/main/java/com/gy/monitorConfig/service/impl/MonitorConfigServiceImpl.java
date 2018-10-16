package com.gy.monitorConfig.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Autowired
    ObjectMapper objectMapper;
    private static final String ALERT_RULE_TEMPLATE_PATH = "/template/";
    private static final String LEVEL_ONE = "one";
    private static final String LEVEL_TWO = "two";
    private static final String ONE_LEVEL_PERF = "one_level_perf";
    private static final String TWO_LEVEL_PERF = "two_level_perf";
    private static final String CONFIG_TEMPLATE_PERF_ANME="perf";
    private static final String CONFIG_TEMPLATE_AVL_ANME="avl";
    private static final String MONITOR_STATUS="monitorstatus";


    @Override
    public TestEntity getJPAInfo() {
        return dao.getJPAInfo();
    }

    @Override
    public CompletionStage<String> initAlertRule(String configTemplateName, List<Map<String, Object>> param) {
        return CompletableFuture.supplyAsync(() -> {
            final StringWriter sw = new StringWriter();
            try {
                final String loadPath = this.getClass().getResource("/").getPath().replaceAll("/C:/","C:/").replaceAll("/classes/","/resources/");
                final String loadPath2 = loadPath.substring(0,loadPath.length()-1);
                final String vmFilePath = loadPath2 + ALERT_RULE_TEMPLATE_PATH;
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
    public void addAlertTemplateToEtcd(String lightTypeId, String templateId, RuleMonitorEntity ruleMonitorEntity) {
        List<Metrics> metricList = dao.getMetricsByLightType(lightTypeId);
        Map<String, Metrics> metricsMap = new HashMap<>();
        metricList.forEach(x -> {
            metricsMap.put(x.getUuid(), x);
        });
        //组装perf模板
        //通过templateId获取性能模板
        List<AlertPerfRuleEntity> perfRuleList = dao.getPerfRuleByTemplateId(templateId);
        Map<String, AlertPerfRuleEntity> perfRuleMap = new HashMap<>();
        List<Map<String, Object>> perfParams = new ArrayList<>();
        perfRuleList.forEach(x -> {
            perfRuleMap.put(x.getUuid(), x);
        });
        ruleMonitorEntity.getPerfRuleMonitorList().forEach((AlertPerfRuleMonitorEntity perRuleMonitor) -> {
            AlertPerfRuleEntity perfRule = perfRuleMap.get(perRuleMonitor.getPerfRuleUuid());
            String ruleName = perRuleMonitor.getAlertRuleName();
            IssuePerfMonitorRuleEntity ruleEntity = null;
            if (metricsMap.containsKey(perfRule.getMetricUuid())) {
                if (ruleName.endsWith(TWO_LEVEL_PERF)) {
                    if (!("".equals(perfRule.getFirstThreshold()))) {
                        //组装性能二级告警
                        Optional<AlertPerfRuleEntity> onelevel = perfRuleList.stream().
                                filter(perf -> !(perf.getUuid().equals(perfRule.getUuid())) && perf.getMetricUuid().equals(perfRule.getMetricUuid())).findFirst();
                        if (onelevel.isPresent()) {
                            ruleEntity = convertMonitorPerf2IssuePerf(perfRule, perRuleMonitor, 2, onelevel.get().getFirstThreshold(), onelevel.get().getAlertFirstCondition());
                        }
                    }

                } else {
                    //组装性能一级告警
                    ruleEntity = convertMonitorPerf2IssuePerf(perfRule, perRuleMonitor, 1, "", 0);
                }
                Metrics myMetric = metricsMap.get(perfRule.getMetricUuid());
                ruleEntity.setUnit(myMetric.getMetricDisplayUnit());
                ruleEntity.setExpression(convertToVelocityExpression(myMetric.getName(), perRuleMonitor.getMonitorUuid()));
                perfParams.add(convertToPerfEtcdParamList(ruleEntity));
            }
        });
        CompletionStage<String> perfStr = initAlertRule(CONFIG_TEMPLATE_PERF_ANME,perfParams);

        //组装可用性
        List<Map<String, Object>> avlParams = new ArrayList<>();
        Map<String, AlertAvlRuleEntity> avlRuleMap = new HashMap<>();
        List<AlertAvlRuleEntity> avlRuleEntityList = dao.getAvlRuleByTemplateId(templateId);
        avlRuleEntityList.forEach(x -> {
            avlRuleMap.put(x.getUuid(), x);
        });
        ruleMonitorEntity.getAvlRuleMonitorList().forEach(avlMonitor->{
            AlertAvlRuleEntity avlRuleEntity = avlRuleMap.get(avlMonitor.getAvlRuleUuid());
            if (metricsMap.containsKey(avlRuleEntity.getMetricUuid())) {

                IssueAvlMonitorRuleEntity avlRuleP =convertMonitorPerf2IssueAvl(avlRuleEntity,avlMonitor);
                Metrics myMetric = metricsMap.get(avlRuleEntity.getMetricUuid());
                avlRuleP.setExpression(convertToVelocityExpression(myMetric.getName(), avlMonitor.getMonitorUuid()));
                if (avlRuleP.getExpression().contains(MONITOR_STATUS)){
                    avlRuleP.setCondition(avlRuleP.getExpression().replaceFirst("(.*)monitorstatus","up"));
                }
                avlParams.add(convertToAvlEtcdParamList(avlRuleP));
            }

        });
        CompletionStage<String> avlStr = initAlertRule(CONFIG_TEMPLATE_AVL_ANME,avlParams);

        perfStr.thenCombine(avlStr,(perf,avl)->{
            // TODO: 2018/10/16 将模板string下发到etcd模板监控实体id对应的value中，
            return null;
        });
    }

    private IssueAvlMonitorRuleEntity convertMonitorPerf2IssueAvl(AlertAvlRuleEntity avlRuleEntity, AlertAvlRuleMonitorEntity avlMonitor) {
        IssueAvlMonitorRuleEntity avlMonitorRuleEntity = new IssueAvlMonitorRuleEntity();
        avlMonitorRuleEntity.setRuleName(avlMonitor.getAlertRuleName());
        avlMonitorRuleEntity.setMonitorUuid(avlMonitor.getMonitorUuid());
        avlMonitorRuleEntity.setSeverity(convertServerityDB(avlRuleEntity.getSeverity()));
        avlMonitorRuleEntity.setDescription(avlRuleEntity.getDescription());
        return avlMonitorRuleEntity;
    }
    
    private Map<String, Object> convertToAvlEtcdParamList(IssueAvlMonitorRuleEntity ruleEntity) {
        Map<String, Object> param = new HashMap<>();
        param.put(MonitorConfigEnum.VelocityEnum.SERVERITY.value(), ruleEntity.getSeverity());
        param.put(MonitorConfigEnum.VelocityEnum.EXPRESSION.value(), ruleEntity.getExpression());
        param.put(MonitorConfigEnum.VelocityEnum.MONITOR_ID.value(), ruleEntity.getMonitorUuid());
        param.put(MonitorConfigEnum.VelocityEnum.DESCRIPTION.value(), ruleEntity.getDescription());
        param.put(MonitorConfigEnum.VelocityEnum.RULE_NAME.value(), ruleEntity.getRuleName());
        if (null!=ruleEntity.getCondition()){
            param.put(MonitorConfigEnum.VelocityEnum.CONDITION.value(), ruleEntity.getRuleName());
        }
        return param;
    }
    
    private Map<String, Object> convertToPerfEtcdParamList(IssuePerfMonitorRuleEntity ruleEntity) {
        Map<String, Object> param = new HashMap<>();
        param.put(MonitorConfigEnum.VelocityEnum.SERVERITY.value(), ruleEntity.getSeverity());
        param.put(MonitorConfigEnum.VelocityEnum.FIRST_CONDITION.value(), ruleEntity.getFirstCondition());
        param.put(MonitorConfigEnum.VelocityEnum.FIRST_THRESHOLD.value(), ruleEntity.getFirstThreshold());
        param.put(MonitorConfigEnum.VelocityEnum.EXPRESSION_MORE.value(), ruleEntity.getMoreExpression());
        param.put(MonitorConfigEnum.VelocityEnum.SECOND_CONDITION.value(), ruleEntity.getSecondCondition());
        param.put(MonitorConfigEnum.VelocityEnum.SECOND_THRESHOLD.value(), ruleEntity.getSecondThreshold());
        param.put(MonitorConfigEnum.VelocityEnum.EXPRESSION.value(), ruleEntity.getExpression());
        param.put(MonitorConfigEnum.VelocityEnum.MONITOR_ID.value(), ruleEntity.getMonitorUuid());
        param.put(MonitorConfigEnum.VelocityEnum.DESCRIPTION.value(), ruleEntity.getDescription());
        param.put(MonitorConfigEnum.VelocityEnum.UNIT.value(), ruleEntity.getUnit());
        param.put(MonitorConfigEnum.VelocityEnum.RULE_NAME.value(), ruleEntity.getRuleName());
        return param;
    }

    private String convertToVelocityExpression(String name, String instanceId) {
        return name + "{instance_id=" + "'" + instanceId + "'" + "}";
    }


    private IssuePerfMonitorRuleEntity convertMonitorPerf2IssuePerf(AlertPerfRuleEntity alertPerfRuleEntity,
                                                                    AlertPerfRuleMonitorEntity alertPerfRuleMonitorEntity,
                                                                    int level, String levelOneFirstThreshold, int levelOneFirstCondition) {

        IssuePerfMonitorRuleEntity issuePerfMonitorRuleEntity = new IssuePerfMonitorRuleEntity();
        if (1 == level) {
            issuePerfMonitorRuleEntity.setSeverity(convertServerityDB(alertPerfRuleEntity.getSeverity()));
            issuePerfMonitorRuleEntity.setFirstCondition(convertConditionDB(alertPerfRuleEntity.getAlertFirstCondition()));
            issuePerfMonitorRuleEntity.setFirstThreshold(alertPerfRuleEntity.getFirstThreshold());
            issuePerfMonitorRuleEntity.setRuleName(alertPerfRuleMonitorEntity.getAlertRuleName());
            if (!("".equals(alertPerfRuleEntity.getSecondThreshold()))) {
                issuePerfMonitorRuleEntity.setMoreExpression(alertPerfRuleEntity.getExpressionMore());
                issuePerfMonitorRuleEntity.setSecondCondition(convertConditionDB(alertPerfRuleEntity.getAlertSecondCondition()));
                issuePerfMonitorRuleEntity.setSecondThreshold(alertPerfRuleEntity.getSecondThreshold());
            }
        } else if (2 == level) {
            issuePerfMonitorRuleEntity.setSeverity(convertServerityDB(alertPerfRuleEntity.getSeverity()));
            issuePerfMonitorRuleEntity.setFirstCondition(convertConditionDB(alertPerfRuleEntity.getAlertFirstCondition()));
            issuePerfMonitorRuleEntity.setFirstThreshold(alertPerfRuleEntity.getFirstThreshold());
            issuePerfMonitorRuleEntity.setRuleName(alertPerfRuleMonitorEntity.getAlertRuleName());
            if (!("".equals(alertPerfRuleEntity.getSecondThreshold()))) {
                issuePerfMonitorRuleEntity.setMoreExpression(alertPerfRuleEntity.getExpressionMore());
                issuePerfMonitorRuleEntity.setSecondCondition(convertConditionDB(alertPerfRuleEntity.getAlertSecondCondition()));
                issuePerfMonitorRuleEntity.setSecondThreshold(alertPerfRuleEntity.getSecondThreshold());
            } else {
                //当2级的2级阈值不存在时，用1级的1级阈值,条件为1级的1级条件反
                issuePerfMonitorRuleEntity.setMoreExpression("and");
                issuePerfMonitorRuleEntity.setSecondCondition(convertLevelTwoConditionDB(levelOneFirstCondition));
                issuePerfMonitorRuleEntity.setSecondThreshold(levelOneFirstThreshold);
            }
        }
        issuePerfMonitorRuleEntity.setDescription(alertPerfRuleEntity.getDescription());
        issuePerfMonitorRuleEntity.setMonitorUuid(alertPerfRuleMonitorEntity.getMonitorUuid());
        return issuePerfMonitorRuleEntity;
    }

    private String convertLevelTwoConditionDB(int condition) {
        String res = "";
        switch (condition) {
            case 0://">"
                res = "<=";
                break;
            case 3://">="
                res = "<";
                break;
            case 1://=
                res = "!=";
                break;
            case 2://"<"
                res = ">=";
                break;
            case 4://"<="
                res = ">";
                break;
            case 5://!=
                res = "=";
                break;
        }
        return res;
    }

    private String convertConditionDB(int condition) {
        String res = "";
        switch (condition) {
            case 0:
                res = ">";
                break;
            case 3:
                res = ">=";
                break;
            case 1:
                res = "=";
                break;
            case 2:
                res = "<";
                break;
            case 4:
                res = "<=";
                break;
            case 5:
                res = "!=";
                break;
        }
        return res;
    }

    private String convertServerityDB(int severity) {
        String res = "";
        switch (severity) {
            case 0:
                res = "critical";//紧急
                break;
            case 1:
                res = "major";//重要
                break;
            case 2:
                res = "minor";//次要
                break;
            case 3:
                res = "warning";//警告
                break;
            case 4:
                res = "notice";//通知
                break;
        }
        return res;
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
                        BeanUtils.copyProperties(convert2CommonRule(perf, LEVEL_ONE), perfRuleEntity);
                        perfRuleEntity.setAlertLevel(LEVEL_ONE);
                        perfRuleEntity.setDescription(perf.getDescription());
                        dao.addPerfRule(perfRuleEntity);
                    }
                    if (null != perf.getLevelTwoFirstThreshold()) {
                        AlertPerfRuleEntity perfRuleEntity1 = new AlertPerfRuleEntity();
                        perfRuleEntity1.setUuid(UUID.randomUUID().toString());
                        perfRuleEntity1.setTemplateUuid(templateEntity.getUuid());
                        perfRuleEntity1.setMetricUuid(perf.getUuid());
                        BeanUtils.copyProperties(convert2CommonRule(perf, LEVEL_TWO), perfRuleEntity1);
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

    @Override
    public String getTemplateByLightType(String lightType, String monitorMode) throws JsonProcessingException {
        List<LightTypeEntity> lightTypeList = monitorService.getLightTypeEntity();
        Optional<LightTypeEntity> lightTypeEntity = lightTypeList.stream().filter(x -> x.getName().equals(lightType)).findFirst();
        return objectMapper.writeValueAsString(lightTypeEntity.map(lightTypeEntity1 -> dao.getTemplateByLightType(lightTypeEntity1.getUuid(), monitorMode)).orElse(null));

    }

    @Override
    public List<AlertAvlRuleEntity> getAvlRuleByTemplateId(String templateId) {
        return dao.getAvlRuleByTemplateId(templateId);
    }

    @Override
    public List<AlertPerfRuleEntity> getPerfRuleByTemplateId(String templateId) {
        return dao.getPerfRuleByTemplateId(templateId);
    }

    @Override
    public boolean addAvlRuleMonitorList(List<AlertAvlRuleMonitorEntity> avlRuleMonitorList) {
        avlRuleMonitorList.forEach(x -> {
            dao.addAvlRuleMonitor(x);
        });
        return true;
    }

    @Override
    public boolean addPerfRuleMonitorList(List<AlertPerfRuleMonitorEntity> perfRuleMonitorList) {
        perfRuleMonitorList.forEach(x -> {
            dao.addPerfRuleMonitor(x);
        });
        return true;
    }

    @Override
    public boolean addTemplateMonitor(AlertRuleTemplateMonitorEntity templateMonitorEntity) {
        return dao.addTemplateMonitor(templateMonitorEntity);
    }

    @Override
    public List<Metrics> getMetricsByLightType(String lightTypeId) {
        return dao.getMetricsByLightType(lightTypeId);
    }

    private AlertRuleEntity convert2CommonRule(MetricInfo info, String level) {
        AlertRuleEntity ruleEntity = new AlertRuleEntity();
        if (level.equals(LEVEL_ONE)) {
            ruleEntity.setSeverity(Integer.parseInt(info.getLevelOneSeverity()));
            ruleEntity.setAlertFirstCondition(Integer.parseInt(info.getLevelOneAlertFirstCondition()));
            ruleEntity.setFirstThreshold(info.getLevelOneFirstThreshold());
            ruleEntity.setExpressionMore(info.getLevelOneExpressionMore());
            ruleEntity.setAlertSecondCondition(Integer.parseInt(info.getLevelOneAlertSecondCondition()));
            if (null != info.getLevelOneSecondThreshold()) {
                ruleEntity.setSecondThreshold(info.getLevelOneSecondThreshold());
            }
        } else {
            ruleEntity.setSeverity(Integer.parseInt(info.getLevelTwoSeverity()));
            ruleEntity.setAlertFirstCondition(Integer.parseInt(info.getLevelTwoAlertFirstCondition()));
            ruleEntity.setFirstThreshold(info.getLevelTwoFirstThreshold());
            ruleEntity.setExpressionMore(info.getLevelTwoExpressionMore());
            ruleEntity.setAlertSecondCondition(Integer.parseInt(info.getLevelTwoAlertSecondCondition()));
            if (null != info.getLevelTwoSecondThreshold()) {
                ruleEntity.setSecondThreshold(info.getLevelTwoSecondThreshold());
            }
        }
        return ruleEntity;

    }


}
