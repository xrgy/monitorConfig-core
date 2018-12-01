package com.gy.monitorConfig.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gy.monitorConfig.common.MonitorConfigEnum;
import com.gy.monitorConfig.common.MonitorEnum;
import com.gy.monitorConfig.dao.EtcdDao;
import com.gy.monitorConfig.dao.MonitorConfigDao;
import com.gy.monitorConfig.entity.*;
import com.gy.monitorConfig.entity.etcd.Rule;
import com.gy.monitorConfig.entity.etcd.RuleGroup;
import com.gy.monitorConfig.entity.etcd.RuleGroups;
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
import org.w3c.dom.ls.LSInput;
import sun.security.timestamp.TSRequest;

import java.io.IOException;
import java.io.StringWriter;
import java.security.acl.Group;
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
    EtcdDao etcdDao;

    @Autowired
    ObjectMapper objectMapper;
    private static final String ALERT_RULE_TEMPLATE_PATH = "/template/";
    private static final String LEVEL_ONE = "one";
    private static final String LEVEL_TWO = "two";
    private static final String ONE_LEVEL_PERF = "one_level_perf";
    private static final String TWO_LEVEL_PERF = "two_level_perf";
    private static final String CONFIG_TEMPLATE_PERF_ANME = "perf";
    private static final String CONFIG_TEMPLATE_AVL_ANME = "avl";
    private static final String MONITOR_STATUS = "monitorstatus";
    private static final String RULE_ANME_START="rule_";

    @Override
    public TestEntity getJPAInfo() {
        return dao.getJPAInfo();
    }

    @Override
    public CompletionStage<String> initAlertRule(String configTemplateName, List<Map<String, Object>> param) {
        return CompletableFuture.supplyAsync(() -> {
            final StringWriter sw = new StringWriter();
            try {
//                final String loadPath = this.getClass().getResource("/").getPath();
                final String vmFilePath = ALERT_RULE_TEMPLATE_PATH;
                System.out.println(vmFilePath);
                final VelocityEngine ve = new VelocityEngine();
                ve.setProperty(VelocityEngine.FILE_RESOURCE_LOADER_PATH, vmFilePath);
                ve.setProperty("directive.set.null.allowed", true);
                ve.init();
                final Template velocityTemplate = ve.getTemplate(configTemplateName + ".vm", "UTF-8");
                if (null == velocityTemplate) {
                    return sw.toString();
                }
                final VelocityContext velocityContext = new VelocityContext();
                velocityContext.put("param", param);
                velocityTemplate.merge(velocityContext, sw);
                sw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return sw.toString();
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
        //List<Map<String, Object>> perfParams = new ArrayList<>();
        RuleGroups groups = new RuleGroups();
        RuleGroup group = new RuleGroup();
        group.setName("/"+ruleMonitorEntity.getTemplateMonitorEntity().getUuid()+".rules");
        List<Rule> rules =new ArrayList<>();
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
                rules.add(convertPerfToRuleYaml(ruleEntity));
                //perfParams.add(convertToPerfEtcdParamList(ruleEntity));
            }
        });
        //CompletionStage<String> perfStr = initAlertRule(CONFIG_TEMPLATE_PERF_ANME, perfParams);

        //组装可用性
        List<Map<String, Object>> avlParams = new ArrayList<>();
        Map<String, AlertAvlRuleEntity> avlRuleMap = new HashMap<>();
        List<AlertAvlRuleEntity> avlRuleEntityList = dao.getAvlRuleByTemplateId(templateId);
        avlRuleEntityList.forEach(x -> {
            avlRuleMap.put(x.getUuid(), x);
        });
        ruleMonitorEntity.getAvlRuleMonitorList().forEach(avlMonitor -> {
            AlertAvlRuleEntity avlRuleEntity = avlRuleMap.get(avlMonitor.getAvlRuleUuid());
            if (metricsMap.containsKey(avlRuleEntity.getMetricUuid())) {

                IssueAvlMonitorRuleEntity avlRuleP = convertMonitorPerf2IssueAvl(avlRuleEntity, avlMonitor);
                Metrics myMetric = metricsMap.get(avlRuleEntity.getMetricUuid());
                avlRuleP.setExpression(convertToVelocityExpression(myMetric.getName(), avlMonitor.getMonitorUuid()));
                if (avlRuleP.getExpression().contains(MONITOR_STATUS)) {
                    avlRuleP.setCondition(avlRuleP.getExpression().replaceFirst("(.*)monitorstatus", "up"));
                }
                //avlParams.add(convertToAvlEtcdParamList(avlRuleP));
                rules.add(convertAvlToRuleYaml(avlRuleP));
            }

        });
        //CompletionStage<String> avlStr = initAlertRule(CONFIG_TEMPLATE_AVL_ANME, avlParams);

        group.setRules(rules);
        List<RuleGroup> tempGroup = new ArrayList<>();
        tempGroup.add(group);
        groups.setGroups(tempGroup);
        try {
            etcdDao.insertEtcdAlert(ruleMonitorEntity.getTemplateMonitorEntity().getUuid(),groups);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        /*perfStr.thenCombine(avlStr, (perf, avl) -> {
            // 2018/10/16 将模板string下发到etcd模板监控实体id对应的value中，
            try {
                etcdDao.insertEtcdAlert(ruleMonitorEntity.getTemplateMonitorEntity().getUuid(),perf+"\n"+avl);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            return null;
        });*/
    }

    @Override
    public void delAlertMonitorRule(String uuid) {
        //从数据库根据monitiruuid删除 templatemonitorentity avlmonitorentity perfmonitorentity
        AlertRuleTemplateMonitorEntity templateMonitorEntity = dao.getTemplateMonitorByMonitorUuid(uuid);
        boolean delTemp =dao.delTemplateMonitorByMonitorUuid(uuid);
        boolean delAvl = dao.delAvlMonitorByMonitorUuid(uuid);
        boolean delPerf = dao.delPerfMonitorByMonitorUuid(uuid);
        if (delTemp && delAvl && delPerf){
            //  2018/10/22 根据templateMonitorEntity.getuuid从etcd中删除  url=”/alert/uuid” wsrequest.delete()
            etcdDao.delEtcdAlert(templateMonitorEntity.getUuid());
        }
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
                        AlertPerfRuleEntity perfRuleEntity = setPerfInsertRule(perf,LEVEL_ONE,templateEntity.getUuid());
                        dao.addPerfRule(perfRuleEntity);
                    }
                    if (null != perf.getLevelTwoFirstThreshold()) {
                        AlertPerfRuleEntity perfRuleEntity2 = setPerfInsertRule(perf,LEVEL_TWO,templateEntity.getUuid());
                        dao.addPerfRule(perfRuleEntity2);
                    }
                });
            });

            return true;
        }
        return false;
    }

    @Override
    public void detTemplate(String id) {
        //删除可用性
        boolean avl = dao.delAvlByTemplateUuid(id);
        //删除性能
        boolean perf = dao.delPerfByTemplateUuid(id);
        if (avl && perf) {
            //删除alertruletemplate
            dao.delTemplateByTemplateUuid(id);
        }
    }

    @Override
    public boolean updateTemplate(UpTemplateView view) {
        AlertRuleTemplateEntity templateEntity = dao.getTemplateByUuid(view.getUuid());
        List<AlertAvlRuleEntity> avlRuleList = dao.getAvlRuleByTemplateId(view.getUuid());
        List<AlertPerfRuleEntity> perfRuleList = dao.getPerfRuleByTemplateId(view.getUuid());
        List<AlertPerfRuleEntity> newPerfRule = new ArrayList<>();
        //avl是全部加进数据库的 但是perf 不是全部都加进数据库的
        avlRuleList.forEach(avl->{
            Optional<UpAvaliable> avlOpt = view.getAvailable().stream()
                    .filter(x->x.getUuid().equals(avl.getUuid())).findFirst();
            avlOpt.ifPresent(upAvaliable -> avl.setSeverity(Integer.parseInt(upAvaliable.getSeverity())));
        });

        view.getPerformance().forEach(perf->{
            Optional<AlertPerfRuleEntity> perfOneOpt = perfRuleList.stream()
                    .filter(x->x.getUuid().equals(perf.getLevelOneUuid())).findFirst();
            if (perfOneOpt.isPresent()){
                perfOneOpt.get().setSeverity(Integer.parseInt(perf.getLevelOneSeverity()));
                perfOneOpt.get().setAlertFirstCondition(Integer.parseInt(perf.getLevelOneAlertFirstCondition()));
                perfOneOpt.get().setFirstThreshold(perf.getLevelOneFirstThreshold());
                perfOneOpt.get().setExpressionMore(perf.getLevelOneExpressionMore());
                perfOneOpt.get().setAlertSecondCondition(Integer.parseInt(perf.getLevelOneAlertSecondCondition()));
                if (null != perf.getLevelOneSecondThreshold()) {
                    perfOneOpt.get().setSecondThreshold(perf.getLevelOneSecondThreshold());
                }
            }
            Optional<AlertPerfRuleEntity> perfTwoOpt = perfRuleList.stream()
                    .filter(x->x.getUuid().equals(perf.getLevelTwoUuid())).findFirst();
            if (perfTwoOpt.isPresent()){
                perfTwoOpt.get().setSeverity(Integer.parseInt(perf.getLevelTwoSeverity()));
                perfTwoOpt.get().setAlertFirstCondition(Integer.parseInt(perf.getLevelTwoAlertFirstCondition()));
                perfTwoOpt.get().setFirstThreshold(perf.getLevelTwoFirstThreshold());
                perfTwoOpt.get().setExpressionMore(perf.getLevelTwoExpressionMore());
                perfTwoOpt.get().setAlertSecondCondition(Integer.parseInt(perf.getLevelTwoAlertSecondCondition()));
                if (null != perf.getLevelTwoSecondThreshold()) {
                    perfTwoOpt.get().setSecondThreshold(perf.getLevelTwoSecondThreshold());
                }
            }
            if (!perfOneOpt.isPresent() && !perfTwoOpt.isPresent()){
                //都不存在，则添加
                if (null != perf.getLevelOneFirstThreshold()) {
                    MetricInfo info1 = new MetricInfo();
                    BeanUtils.copyProperties(perf,info1);
                    AlertPerfRuleEntity a = setPerfInsertRule(info1,LEVEL_ONE,templateEntity.getUuid());
                    newPerfRule.add(a);

                }
                if (null != perf.getLevelTwoFirstThreshold()) {
                    MetricInfo info2 = new MetricInfo();
                    BeanUtils.copyProperties(perf,info2);
                    AlertPerfRuleEntity b = setPerfInsertRule(info2,LEVEL_TWO,templateEntity.getUuid());
                    newPerfRule.add(b);
                }
            }
        });
        perfRuleList.addAll(newPerfRule);
        templateEntity.setMonitorMode(view.getMonitorMode());
        templateEntity.setTemplateName(view.getTemplateName());
        avlRuleList.forEach(x->{
            dao.addAvlRule(x);
        });
        perfRuleList.forEach(x->{
            dao.addPerfRule(x);
        });

        boolean addTem = dao.addTemplate(templateEntity);
        if (addTem){
            //  更新使用该监控模板的具体监控对象策略
            List<AlertRuleTemplateMonitorEntity> templateMonitorEntity = dao.getTemplateMonitorByTemplateUuid(templateEntity.getUuid());
            List<String> monitorIdList = new ArrayList<>();
            templateMonitorEntity.forEach(x -> {
                monitorIdList.add(x.getMonitorUuid());

            });
            monitorIdList.forEach(monitorId->{
                List<AlertAvlRuleMonitorEntity> avlMonitorList = dao.getAvlRuleMonitorByMonitorId(monitorId);
                List<AlertPerfRuleMonitorEntity> perfMonitorList = dao.getPerfRuleMonitorByMonitorId(monitorId);
                //  找出新添加perfmonitor
                List<AlertPerfRuleMonitorEntity> newPerfMonitor = new ArrayList<>();
                newPerfRule.forEach(newPerf->{
                    AlertPerfRuleMonitorEntity entity = new AlertPerfRuleMonitorEntity();
                    String id = UUID.randomUUID().toString().replaceAll("-","");
                    entity.setUuid(id);
                    entity.setMonitorUuid(monitorId);
                    entity.setPerfRuleUuid(newPerf.getUuid());
                    if (newPerf.getAlertLevel().equals(LEVEL_ONE)){
                        entity.setAlertRuleName(RULE_ANME_START+id+ONE_LEVEL_PERF);
                    }else if(newPerf.getAlertLevel().equals(LEVEL_TWO)){
                        entity.setAlertRuleName(RULE_ANME_START+id+TWO_LEVEL_PERF);
                    }
                    newPerfMonitor.add(entity);
                });
                boolean addPerf = addPerfRuleMonitorList(newPerfMonitor);
                if (addPerf){
                    perfMonitorList.addAll(newPerfMonitor);
                    //合成ru
                    RuleMonitorEntity ruleMonitorEntity = new RuleMonitorEntity();
                    ruleMonitorEntity.setAvlRuleMonitorList(avlMonitorList);
                    ruleMonitorEntity.setPerfRuleMonitorList(perfMonitorList);
                    ruleMonitorEntity.setTemplateMonitorEntity(templateMonitorEntity.stream().filter(x->x.getMonitorUuid().equals(monitorId)).findFirst().get());
                    addAlertTemplateToEtcd(view.getResourceUuid(),view.getUuid(),ruleMonitorEntity);
                }
            });
        }
        return addTem;
    }

    private AlertPerfRuleEntity setPerfInsertRule(MetricInfo perf, String level, String templateId) {
        AlertPerfRuleEntity perfRuleEntity = new AlertPerfRuleEntity();
        perfRuleEntity.setUuid(UUID.randomUUID().toString());
        perfRuleEntity.setTemplateUuid(templateId);
        perfRuleEntity.setMetricUuid(perf.getUuid());
        BeanUtils.copyProperties(convert2CommonRule(perf, level), perfRuleEntity);
        perfRuleEntity.setAlertLevel(level);
        perfRuleEntity.setDescription(perf.getDescription());
        return perfRuleEntity;

    }

    @Override
    public UpTemplateView getOpenTemplateData(String uuid) {
        List<MetricsCollection> collectionList = dao.getMetricsCollection();
        List<MetricsGroup> groupList = dao.getMetricsGroup();
        List<MetricsType> typeList = dao.getMetricsType();

        AlertRuleTemplateEntity templateEntity = dao.getTemplateByUuid(uuid);
        List<AlertAvlRuleEntity> avlRuleList = dao.getAvlRuleByTemplateId(uuid);
        List<AlertPerfRuleEntity> perfRuleList = dao.getPerfRuleByTemplateId(uuid);
        UpTemplateView view = new UpTemplateView();
        BeanUtils.copyProperties(templateEntity,view);
        List<UpAvaliable> avaliableList = new ArrayList<>();
        avlRuleList.forEach(avl->{
            UpAvaliable avaliable = new UpAvaliable();
            BeanUtils.copyProperties(avl,avaliable);
            Metrics metrics = dao.getMetricsByUuid(avl.getMetricUuid());
            MetricInfo metricInfo = new MetricInfo();
            BeanUtils.copyProperties(metrics,metricInfo);
            Optional<MetricsCollection> collection = collectionList.stream().filter(x->x.getUuid().equals(metrics.getMetricCollectionId())).findFirst();
            collection.ifPresent(metricsCollection -> metricInfo.setCollectionName(metricsCollection.getName()));
            Optional<MetricsGroup> group = groupList.stream().filter(x->x.getUuid().equals(metrics.getMetricGroupId())).findFirst();
            group.ifPresent(metricsGroup -> metricInfo.setGroupName(metricsGroup.getName()));
            Optional<MetricsType> type = typeList.stream().filter(x->x.getUuid().equals(metrics.getMetricTypeId())).findFirst();
            type.ifPresent(metricsType -> metricInfo.setTypeName(metricsType.getName()));
            avaliable.setQuotaInfo(metricInfo);
            avaliableList.add(avaliable);
        });
        view.setAvailable(avaliableList);
        Map<String,UpPerformance> mapp = new HashMap<>();
        perfRuleList.forEach(perf->{
            UpPerformance performance = null;
            if (mapp.containsKey(perf.getMetricUuid())){
                performance = mapp.get(perf.getMetricUuid());
            }else {
                performance = new UpPerformance();
                BeanUtils.copyProperties(perf,performance);
                Metrics metrics = dao.getMetricsByUuid(perf.getMetricUuid());
                MetricInfo metricInfo = new MetricInfo();
                BeanUtils.copyProperties(metrics,metricInfo);
                Optional<MetricsCollection> collection = collectionList.stream().filter(x->x.getUuid().equals(metrics.getMetricCollectionId())).findFirst();
                collection.ifPresent(metricsCollection -> metricInfo.setCollectionName(metricsCollection.getName()));
                Optional<MetricsGroup> group = groupList.stream().filter(x->x.getUuid().equals(metrics.getMetricGroupId())).findFirst();
                group.ifPresent(metricsGroup -> metricInfo.setGroupName(metricsGroup.getName()));
                Optional<MetricsType> type = typeList.stream().filter(x->x.getUuid().equals(metrics.getMetricTypeId())).findFirst();
                type.ifPresent(metricsType -> metricInfo.setTypeName(metricsType.getName()));
                performance.setQuotaInfo(metricInfo);
                mapp.put(perf.getMetricUuid(),performance);
            }
            if (perf.getAlertLevel().equals(LEVEL_ONE)){
                performance.setLevelOneUuid(perf.getUuid());
                performance.setLevelOneSeverity(perf.getSeverity()+"");
                performance.setLevelOneAlertFirstCondition(perf.getAlertFirstCondition()+"");
                performance.setLevelOneFirstThreshold(perf.getFirstThreshold());
                performance.setLevelOneExpressionMore(perf.getExpressionMore());
                performance.setLevelOneAlertSecondCondition(perf.getAlertSecondCondition()+"");
                performance.setLevelOneSecondThreshold(perf.getSecondThreshold());
            }else if(perf.getAlertLevel().equals(LEVEL_TWO)) {
                performance.setLevelTwoUuid(perf.getUuid());
                performance.setLevelTwoSeverity(perf.getSeverity()+"");
                performance.setLevelTwoAlertFirstCondition(perf.getAlertFirstCondition()+"");
                performance.setLevelTwoFirstThreshold(perf.getFirstThreshold());
                performance.setLevelTwoExpressionMore(perf.getExpressionMore());
                performance.setLevelTwoAlertSecondCondition(perf.getAlertSecondCondition()+"");
                performance.setLevelTwoSecondThreshold(perf.getSecondThreshold());
            }
        });
        return view;
    }

    private IssueAvlMonitorRuleEntity convertMonitorPerf2IssueAvl(AlertAvlRuleEntity avlRuleEntity, AlertAvlRuleMonitorEntity avlMonitor) {
        IssueAvlMonitorRuleEntity avlMonitorRuleEntity = new IssueAvlMonitorRuleEntity();
        avlMonitorRuleEntity.setRuleName(avlMonitor.getAlertRuleName());
        avlMonitorRuleEntity.setMonitorUuid(avlMonitor.getMonitorUuid());
        avlMonitorRuleEntity.setSeverity(convertServerityDB(avlRuleEntity.getSeverity()));
        avlMonitorRuleEntity.setDescription(avlRuleEntity.getDescription());
        return avlMonitorRuleEntity;
    }


    private Rule convertAvlToRuleYaml(IssueAvlMonitorRuleEntity ruleEntity){
        Rule rule = new Rule();
        rule.setAlert(ruleEntity.getRuleName());
        if (null!=ruleEntity.getCondition()){
            String expr = ruleEntity.getCondition()+"!=1 OR "+ruleEntity.getExpression()+"!=1";
            rule.setExpr(expr);
        }
        Map<String,String> labels = new HashMap<>();
        labels.put("severity",ruleEntity.getSeverity());
        rule.setLabels(labels);
        Map<String,String> annotations = new HashMap<>();
        annotations.put("description",ruleEntity.getDescription());
        annotations.put("current_value","{{$value}}");
        rule.setAnnotations(annotations);
        return rule;
    }

    private Map<String, Object> convertToAvlEtcdParamList(IssueAvlMonitorRuleEntity ruleEntity) {
        Map<String, Object> param = new HashMap<>();
        param.put(MonitorConfigEnum.VelocityEnum.SERVERITY.value(), ruleEntity.getSeverity());
        param.put(MonitorConfigEnum.VelocityEnum.EXPRESSION.value(), ruleEntity.getExpression());
        param.put(MonitorConfigEnum.VelocityEnum.MONITOR_ID.value(), ruleEntity.getMonitorUuid());
        param.put(MonitorConfigEnum.VelocityEnum.DESCRIPTION.value(), ruleEntity.getDescription());
        param.put(MonitorConfigEnum.VelocityEnum.RULE_NAME.value(), ruleEntity.getRuleName());
        if (null != ruleEntity.getCondition()) {
            param.put(MonitorConfigEnum.VelocityEnum.CONDITION.value(), ruleEntity.getCondition());
        }
        return param;
    }



    private Rule convertPerfToRuleYaml(IssuePerfMonitorRuleEntity ruleEntity){
        Rule rule = new Rule();
        rule.setAlert(ruleEntity.getRuleName());
        String expr = ruleEntity.getExpression()+" "+ruleEntity.getFirstCondition()+" "+ruleEntity.getFirstThreshold();
        if (null!=ruleEntity.getSecondCondition() && null!=ruleEntity.getMoreExpression()){
            expr+=" "+ruleEntity.getMoreExpression()+" "+ruleEntity.getExpression()+" "+ruleEntity.getSecondCondition()+" "+ruleEntity.getSecondThreshold();
        }
        rule.setExpr(expr);
        Map<String,String> labels = new HashMap<>();
        labels.put("severity",ruleEntity.getSeverity());
        rule.setLabels(labels);
        Map<String,String> annotations = new HashMap<>();
        annotations.put("description",ruleEntity.getDescription());
        annotations.put("threashold",ruleEntity.getFirstThreshold()+ruleEntity.getUnit());
        annotations.put("current_value","{{$value}}"+ruleEntity.getUnit());
        rule.setAnnotations(annotations);
        return rule;
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
            if ( null!= alertPerfRuleEntity.getSecondThreshold() && !("".equals(alertPerfRuleEntity.getSecondThreshold()))) {
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
    public String getTemplateByLightType(String lightType) throws JsonProcessingException {
        MonitorTemplate monitorTemplate = new MonitorTemplate();
        List<LightTypeEntity> lightTypeList = monitorService.getLightTypeEntity();
        Optional<LightTypeEntity> mainLightTypeEntity = lightTypeList.stream().filter(x -> x.getName().equals(lightType)).findFirst();
        List<AlertRuleTemplateEntity> mainRuleList = mainLightTypeEntity.map(light -> dao.getTemplateByLightType(light.getUuid())).orElse(null);
        if (lightType.equals(MonitorEnum.LightTypeEnum.SWITCH.value()) || lightType.equals(MonitorEnum.LightTypeEnum.ROUTER.value())
                || lightType.equals(MonitorEnum.LightTypeEnum.FIREWALL.value()) || lightType.equals(MonitorEnum.LightTypeEnum.LB.value())) {
            //网络设备 monitormode需要分snmp_v1,snmp_v2
            List<RuleTemplate> v1templates = new ArrayList<>();
            List<RuleTemplate> v2templates = new ArrayList<>();
            mainRuleList.forEach(rule -> {
                RuleTemplate v =setTemple(rule,lightType);
                if (rule.getMonitorMode().equals("snmp_v1")) {
                    v1templates.add(v);
                } else if (rule.getMonitorMode().equals("snmp_v2")) {
                    v2templates.add(v);
                }
            });
            monitorTemplate.setSnmp_v1(v1templates);
            monitorTemplate.setSnmp_v2(v2templates);
        }else if (lightType.equals(MonitorEnum.LightTypeEnum.CAS.value())){
            List<RuleTemplate> casRule = new ArrayList<>();
            mainRuleList.forEach(rule->{
                casRule.add(setTemple(rule,lightType));
            });
            monitorTemplate.setCas(casRule);
            Optional<LightTypeEntity> casClusterLightTypeEntity = lightTypeList.stream().filter(x -> x.getName().equals(MonitorEnum.LightTypeEnum.CASCLUSTER.value())).findFirst();
            List<AlertRuleTemplateEntity> casClusterRuleList = casClusterLightTypeEntity.map(light -> dao.getTemplateByLightType(light.getUuid())).orElse(null);
            List<RuleTemplate> casClusterRule = new ArrayList<>();
            casClusterRuleList.forEach(rule->{
                casClusterRule.add(setTemple(rule,lightType));
            });
            monitorTemplate.setCascluster(casClusterRule);

            Optional<LightTypeEntity> cvkLightTypeEntity = lightTypeList.stream().filter(x -> x.getName().equals(MonitorEnum.LightTypeEnum.CVK.value())).findFirst();
            List<AlertRuleTemplateEntity> cvkRuleList = cvkLightTypeEntity.map(light -> dao.getTemplateByLightType(light.getUuid())).orElse(null);
            List<RuleTemplate> cvkRule = new ArrayList<>();
            cvkRuleList.forEach(rule->{
                cvkRule.add(setTemple(rule,lightType));
            });
            monitorTemplate.setCvk(cvkRule);

            Optional<LightTypeEntity> vmLightTypeEntity = lightTypeList.stream().filter(x -> x.getName().equals(MonitorEnum.LightTypeEnum.CVK.value())).findFirst();
            List<AlertRuleTemplateEntity> vmRuleList = vmLightTypeEntity.map(light -> dao.getTemplateByLightType(light.getUuid())).orElse(null);
            List<RuleTemplate> vmRule = new ArrayList<>();
            vmRuleList.forEach(rule->{
                vmRule.add(setTemple(rule,lightType));
            });
            monitorTemplate.setVirtualMachine(vmRule);
        }else if(lightType.equals(MonitorEnum.LightTypeEnum.K8S.value())){
            List<RuleTemplate> k8sRule = new ArrayList<>();
            mainRuleList.forEach(rule->{
                k8sRule.add(setTemple(rule,lightType));
            });
            monitorTemplate.setCas(k8sRule);

            Optional<LightTypeEntity> k8snLightTypeEntity = lightTypeList.stream().filter(x -> x.getName().equals(MonitorEnum.LightTypeEnum.K8SNODE.value())).findFirst();
            List<AlertRuleTemplateEntity> k8snRuleList = k8snLightTypeEntity.map(light -> dao.getTemplateByLightType(light.getUuid())).orElse(null);
            List<RuleTemplate> k8snRule = new ArrayList<>();
            k8snRuleList.forEach(rule->{
                k8snRule.add(setTemple(rule,lightType));
            });
            monitorTemplate.setK8sn(k8snRule);

            Optional<LightTypeEntity> k8scLightTypeEntity = lightTypeList.stream().filter(x -> x.getName().equals(MonitorEnum.LightTypeEnum.K8SCONTAINER.value())).findFirst();
            List<AlertRuleTemplateEntity> k8scRuleList = k8scLightTypeEntity.map(light -> dao.getTemplateByLightType(light.getUuid())).orElse(null);
            List<RuleTemplate> k8scRule = new ArrayList<>();
            k8snRuleList.forEach(rule->{
                k8scRule.add(setTemple(rule,lightType));
            });
            monitorTemplate.setK8sc(k8scRule);
        }else {
            List<RuleTemplate> others = new ArrayList<>();
            mainRuleList.forEach(rule->{
                others.add(setTemple(rule,lightType));
            });
            monitorTemplate.setOther(others);
        }
        return objectMapper.writeValueAsString(monitorTemplate);
    }

    private RuleTemplate setTemple(AlertRuleTemplateEntity rule, String lightType) {
        RuleTemplate t = new RuleTemplate();
        t.setLightType(lightType);
        t.setUuid(rule.getUuid());
        t.setTemplateName(rule.getTemplateName());
        return t;
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
