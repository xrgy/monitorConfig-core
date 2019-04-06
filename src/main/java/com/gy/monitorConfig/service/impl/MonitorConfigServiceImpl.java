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
import java.util.stream.Collectors;


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
    private static final String AVL_RULE_NAME = "_avl";
    private static final String ONE_LEVEL_PERF = "_one_level_perf";
    private static final String TWO_LEVEL_PERF = "_two_level_perf";
    private static final String CONFIG_TEMPLATE_PERF_ANME = "perf";
    private static final String MONITOR_STATUS = "monitorstatus";
    private static final String RULE_ANME_START = "rule_";

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
    public void addAlertTemplateToEtcd(String lightType, String templateId, RuleMonitorEntity ruleMonitorEntity) {
        List<Metrics> metricList = dao.getMetricsByLightType(lightType);
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
        group.setName("/" + ruleMonitorEntity.getTemplateMonitorEntity().getUuid() + ".rules");
        List<Rule> rules = new ArrayList<>();
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
            etcdDao.insertEtcdAlert(ruleMonitorEntity.getTemplateMonitorEntity().getUuid(), groups);
        } catch (Exception e) {
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
//        AlertRuleTemplateMonitorEntity templateMonitorEntity = dao.getTemplateMonitorByMonitorUuid(uuid);
//        boolean delTemp = dao.delTemplateMonitorByMonitorUuid(uuid);
//        boolean delAvl = dao.delAvlMonitorByMonitorUuid(uuid);
//        boolean delPerf = dao.delPerfMonitorByMonitorUuid(uuid);
        //  2018/10/22 根据templateMonitorEntity.getuuid从etcd中删除  url=”/alert/uuid” wsrequest.delete()
        etcdDao.delEtcdAlert(uuid);
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
                        AlertPerfRuleEntity perfRuleEntity = setPerfInsertRule(perf, LEVEL_ONE, templateEntity.getUuid());
                        dao.addPerfRule(perfRuleEntity);
                        if (null != perf.getLevelTwoFirstThreshold()) {
                            AlertPerfRuleEntity perfRuleEntity2 = setPerfInsertRule(perf, LEVEL_TWO, templateEntity.getUuid());
                            dao.addPerfRule(perfRuleEntity2);
                        }
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
//        if (avl && perf) {
        //删除alertruletemplate
        dao.delTemplateByTemplateUuid(id);
//        }
    }

    @Override
    public boolean updateTemplate(UpTemplateView view) {
        AlertRuleTemplateEntity templateEntity = dao.getTemplateByUuid(view.getUuid());
        List<AlertAvlRuleEntity> avlRuleList = dao.getAvlRuleByTemplateId(view.getUuid());
        List<AlertPerfRuleEntity> perfRuleList = dao.getPerfRuleByTemplateId(view.getUuid());
        List<AlertPerfRuleEntity> newPerfRule = new ArrayList<>();
        //avl是全部加进数据库的 但是perf 不是全部都加进数据库的
        avlRuleList.forEach(avl -> {
            Optional<UpAvaliable> avlOpt = view.getAvailable().stream()
                    .filter(x -> x.getUuid().equals(avl.getUuid())).findFirst();
            avlOpt.ifPresent(upAvaliable -> avl.setSeverity(Integer.parseInt(upAvaliable.getSeverity())));
        });

        view.getPerformance().forEach(perf -> {
            Optional<AlertPerfRuleEntity> perfOneOpt = perfRuleList.stream()
                    .filter(x -> null != perf.getLevelOneUuid() && x.getUuid().equals(perf.getLevelOneUuid())).findFirst();
            if (perfOneOpt.isPresent()) {
                if (null == perf.getLevelOneFirstThreshold() || "".equals(perf.getLevelOneFirstThreshold())) {
                    //需要删除之前数据库中的规则
//                    dao.delPerfByUuid(perfOneOpt.get().getUuid());
                    dao.delPerfByTemAndMetric(view.getUuid(),perfOneOpt.get().getMetricUuid());
                } else {
                    perfOneOpt.get().setSeverity(Integer.parseInt(perf.getLevelOneSeverity()));
                    perfOneOpt.get().setAlertFirstCondition(Integer.parseInt(perf.getLevelOneAlertFirstCondition()));
                    perfOneOpt.get().setFirstThreshold(perf.getLevelOneFirstThreshold());
                    perfOneOpt.get().setExpressionMore(perf.getLevelOneExpressionMore());
                    perfOneOpt.get().setAlertSecondCondition(Integer.parseInt(perf.getLevelOneAlertSecondCondition()));
                    if (null != perf.getLevelOneSecondThreshold() || "".equals(perf.getLevelOneSecondThreshold())) {
                        perfOneOpt.get().setSecondThreshold(perf.getLevelOneSecondThreshold());
                    }


                Optional<AlertPerfRuleEntity> perfTwoOpt = perfRuleList.stream()
                        .filter(x -> null != perf.getLevelTwoUuid() && x.getUuid().equals(perf.getLevelTwoUuid())).findFirst();
                if (perfTwoOpt.isPresent()) {
                    if (null == perf.getLevelTwoFirstThreshold() || "".equals(perf.getLevelTwoFirstThreshold())) {
                        dao.delPerfByUuid(perfTwoOpt.get().getUuid());
                    } else {
                        perfTwoOpt.get().setSeverity(Integer.parseInt(perf.getLevelTwoSeverity()));
                        perfTwoOpt.get().setAlertFirstCondition(Integer.parseInt(perf.getLevelTwoAlertFirstCondition()));
                        perfTwoOpt.get().setFirstThreshold(perf.getLevelTwoFirstThreshold());
                        perfTwoOpt.get().setExpressionMore(perf.getLevelTwoExpressionMore());
                        perfTwoOpt.get().setAlertSecondCondition(Integer.parseInt(perf.getLevelTwoAlertSecondCondition()));
                        if (null != perf.getLevelTwoSecondThreshold() || "".equals(perf.getLevelTwoSecondThreshold())) {
                            perfTwoOpt.get().setSecondThreshold(perf.getLevelTwoSecondThreshold());
                        }
                    }
                } else {
                    //一级以前存在 二级不存在，现在新增二级
                    //
                    if (null != perf.getLevelTwoFirstThreshold() || "".equals(perf.getLevelTwoFirstThreshold())) {
                        MetricInfo info = new MetricInfo();
                        BeanUtils.copyProperties(perf, info);
                        info.setUuid(perf.getMetricUuid());
                        AlertPerfRuleEntity perfRuleEntity2 = setPerfInsertRule(info, LEVEL_TWO, templateEntity.getUuid());
                        newPerfRule.add(perfRuleEntity2);
                    }
                }
            }
            }
            if (!perfOneOpt.isPresent()) {

//                if (!perfOneOpt.isPresent() && !perfTwoOpt.isPresent()) {
                //都不存在，则添加
                if (null != perf.getLevelOneFirstThreshold() || "".equals(perf.getLevelOneFirstThreshold())) {
                    MetricInfo info1 = new MetricInfo();
                    BeanUtils.copyProperties(perf, info1);
                    info1.setUuid(perf.getMetricUuid());
                    AlertPerfRuleEntity a = setPerfInsertRule(info1, LEVEL_ONE, templateEntity.getUuid());
                    newPerfRule.add(a);
                    if (null != perf.getLevelTwoFirstThreshold() || "".equals(perf.getLevelTwoFirstThreshold())) {
                        MetricInfo info2 = new MetricInfo();
                        BeanUtils.copyProperties(perf, info2);
                        info2.setUuid(perf.getMetricUuid());
                        AlertPerfRuleEntity b = setPerfInsertRule(info2, LEVEL_TWO, templateEntity.getUuid());
                        newPerfRule.add(b);
                    }
                }

            }
        });
        perfRuleList.addAll(newPerfRule);
        templateEntity.setMonitorMode(view.getMonitorMode());
        templateEntity.setTemplateName(view.getTemplateName());
        boolean addTem = dao.addTemplate(templateEntity);
        avlRuleList.forEach(x -> {
            dao.addAvlRule(x);
        });
        perfRuleList.forEach(x -> {
            dao.addPerfRule(x);
        });

        //找出使用该模板的monitor record
        String lightType = view.getLightType();
        List<String> monitoruuids = new ArrayList<>();
        if (lightType.equals(MonitorEnum.LightTypeEnum.SWITCH.value()) || lightType.equals(MonitorEnum.LightTypeEnum.ROUTER.value())
                || lightType.equals(MonitorEnum.LightTypeEnum.LB.value()) || lightType.equals(MonitorEnum.LightTypeEnum.FIREWALL.value())) {
            monitorService.getNetworkMonitorRecordByTemplateId(view.getUuid()).forEach(x -> {
                monitoruuids.add(x.getUuid());
            });
        } else if (lightType.equals(MonitorEnum.LightTypeEnum.MYSQL.value())) {
            monitorService.getDbMonitorRecordByTemplateId(view.getUuid()).forEach(x -> {
                monitoruuids.add(x.getUuid());
            });
        } else if (lightType.equals(MonitorEnum.LightTypeEnum.TOMCAT.value())) {
            monitorService.getTomcatRecordByTemplateId(view.getUuid()).forEach(x -> {
                monitoruuids.add(x.getUuid());
            });
        } else if (lightType.equals(MonitorEnum.LightTypeEnum.CAS.value())) {
            monitorService.getCasMonitorRecordByTemplateId(view.getUuid()).forEach(x -> {
                monitoruuids.add(x.getUuid());
            });
        } else if (lightType.equals(MonitorEnum.LightTypeEnum.CVK.value())) {
            monitorService.getHostMonitorRecordByTemplateId(view.getUuid()).forEach(x -> {
                monitoruuids.add(x.getUuid());
            });
        } else if (lightType.equals(MonitorEnum.LightTypeEnum.VIRTUALMACHINE.value())) {
            monitorService.getVmMonitorRecordByTemplateId(view.getUuid()).forEach(x -> {
                monitoruuids.add(x.getUuid());
            });
        } else if (lightType.equals(MonitorEnum.LightTypeEnum.K8S.value())) {
            monitorService.getK8sMonitorRecordByTemplateId(view.getUuid()).forEach(x -> {
                monitoruuids.add(x.getUuid());
            });
        } else if (lightType.equals(MonitorEnum.LightTypeEnum.K8SNODE.value())) {
            monitorService.getK8snodeMonitorRecordByTemplateId(view.getUuid()).forEach(x -> {
                monitoruuids.add(x.getUuid());
            });
        } else if (lightType.equals(MonitorEnum.LightTypeEnum.K8SCONTAINER.value())) {
            monitorService.getK8scontainerMonitorRecordByTemplateId(view.getUuid()).forEach(x -> {
                monitoruuids.add(x.getUuid());
            });
        }


        if (addTem) {
            monitoruuids.forEach(monitorId -> {
                List<AlertAvlRuleMonitorEntity> avlMonitorList = new ArrayList<>();
                List<AlertPerfRuleMonitorEntity> perfMonitorList = new ArrayList<>();
                avlRuleList.forEach(x -> {
                    AlertAvlRuleMonitorEntity entity = new AlertAvlRuleMonitorEntity();
                    String id = (monitorId + x.getUuid()).replaceAll("-", "");
                    entity.setUuid(id);
                    entity.setAlertRuleName(RULE_ANME_START + id + AVL_RULE_NAME);
                    entity.setMonitorUuid(monitorId);
                    entity.setAvlRuleUuid(x.getUuid());
                    avlMonitorList.add(entity);
                });

                perfRuleList.forEach(x -> {
                    AlertPerfRuleMonitorEntity entity = new AlertPerfRuleMonitorEntity();
                    String id = (monitorId + x.getUuid()).replaceAll("-", "");
                    entity.setUuid(id);
                    entity.setMonitorUuid(monitorId);
                    entity.setPerfRuleUuid(x.getUuid());
                    if (x.getAlertLevel().equals(LEVEL_ONE)) {
                        entity.setAlertRuleName(RULE_ANME_START + id + ONE_LEVEL_PERF);
                    } else if (x.getAlertLevel().equals(LEVEL_TWO)) {
                        entity.setAlertRuleName(RULE_ANME_START + id + TWO_LEVEL_PERF);
                    }
                    perfMonitorList.add(entity);
                });
                AlertRuleTemplateMonitorEntity templateMonitorEntity = new AlertRuleTemplateMonitorEntity();
                String temuuid = (monitorId + view.getUuid()).replaceAll("-", "");
                templateMonitorEntity.setUuid(temuuid);
                templateMonitorEntity.setMonitorUuid(monitorId);
                templateMonitorEntity.setTemplateUuid(view.getUuid());

                //合成ru
                RuleMonitorEntity ruleMonitorEntity = new RuleMonitorEntity();
                ruleMonitorEntity.setAvlRuleMonitorList(avlMonitorList);
                ruleMonitorEntity.setPerfRuleMonitorList(perfMonitorList);
                ruleMonitorEntity.setTemplateMonitorEntity(templateMonitorEntity);
                addAlertTemplateToEtcd(view.getLightType(), view.getUuid(), ruleMonitorEntity);
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
//        List<MetricsCollection> collectionList = dao.getMetricsCollection();
//        List<MetricsGroup> groupList = dao.getMetricsGroup();
//        List<MetricsType> typeList = dao.getMetricsType();

        AlertRuleTemplateEntity templateEntity = dao.getTemplateByUuid(uuid);
        List<AlertAvlRuleEntity> avlRuleList = dao.getAvlRuleByTemplateId(uuid);
        List<AlertPerfRuleEntity> perfRuleList = dao.getPerfRuleByTemplateId(uuid);
        List<Metrics> metricList = dao.getMetricByTypeAndMode(templateEntity.getLightType(), templateEntity.getMonitorMode());
        UpTemplateView view = new UpTemplateView();
        BeanUtils.copyProperties(templateEntity, view);
        List<UpAvaliable> avaliableList = new ArrayList<>();
        Map<String, UpAvaliable> mappAvl = new HashMap<>();
        avlRuleList.forEach(avl -> {
            UpAvaliable avaliable = new UpAvaliable();
            BeanUtils.copyProperties(avl, avaliable);
            String strseve = String.valueOf(avl.getSeverity());
            avaliable.setSeverity(strseve);
            Optional<Metrics> metrics = metricList.stream().filter(x -> x.getUuid().equals(avl.getMetricUuid())).findFirst();
//            Metrics metrics = dao.getMetricsByUuid(avl.getMetricUuid());
            if (metrics.isPresent()) {
                MetricInfo metricInfo = new MetricInfo();
                metricInfo.setSeverity(strseve);
                metricInfo.setAvlUuid(avl.getUuid());
                BeanUtils.copyProperties(metrics.get(), metricInfo);
                avaliable.setQuotaInfo(metricInfo);
                avaliableList.add(avaliable);
                mappAvl.put(avaliable.getMetricUuid(), avaliable);
            }
        });
        List<Metrics> lastAvlMetric = metricList.stream().filter(x -> x.getMetricGroup().equals("available")
                && !mappAvl.keySet().contains(x.getUuid())).collect(Collectors.toList());
        lastAvlMetric.forEach(m -> {
            UpAvaliable avl = new UpAvaliable();
            MetricInfo metricInfo = new MetricInfo();
            BeanUtils.copyProperties(m, metricInfo);
            avl.setQuotaInfo(metricInfo);
            avl.setTemplateUuid(templateEntity.getUuid());
            avl.setMetricUuid(m.getUuid());
            avaliableList.add(avl);
        });
        view.setAvailable(avaliableList);
        Map<String, UpPerformance> mapp = new HashMap<>();
        List<UpPerformance> performanceList = new ArrayList<>();
        perfRuleList.forEach(perf -> {
            UpPerformance performance = null;
            if (mapp.containsKey(perf.getMetricUuid())) {
                performance=performanceList.stream().filter(x->x.getQuotaInfo().getUuid().equals(perf.getMetricUuid())).findFirst().get();
//                performance = mapp.get(perf.getMetricUuid());
            } else {
                performance = new UpPerformance();
                BeanUtils.copyProperties(perf, performance);
                Optional<Metrics> metrics = metricList.stream().filter(x -> x.getUuid().equals(perf.getMetricUuid())).findFirst();
//                Metrics metrics = dao.getMetricsByUuid(perf.getMetricUuid());
                if (metrics.isPresent()) {
                    MetricInfo metricInfo = new MetricInfo();
                    BeanUtils.copyProperties(metrics.get(), metricInfo);
                    performance.setQuotaInfo(metricInfo);
                    mapp.put(perf.getMetricUuid(), performance);
                }
                performanceList.add(performance);
            }
            if (perf.getAlertLevel().equals(LEVEL_ONE)) {
//                performance.setLevelOneUuid(perf.getUuid());
                //performance.setLevelOneSeverity(perf.getSeverity() + "");
                performance.getQuotaInfo().setLevelOneUuid(perf.getUuid());
                performance.getQuotaInfo().setLevelOneSeverity(perf.getSeverity() + "");
                performance.getQuotaInfo().setLevelOneAlertFirstCondition(perf.getAlertFirstCondition() + "");
                performance.getQuotaInfo().setLevelOneFirstThreshold(perf.getFirstThreshold());
                performance.getQuotaInfo().setLevelOneExpressionMore(perf.getExpressionMore());
                performance.getQuotaInfo().setLevelOneAlertSecondCondition(perf.getAlertSecondCondition() + "");
                performance.getQuotaInfo().setLevelOneSecondThreshold(perf.getSecondThreshold());
//                performance.setLevelOneAlertFirstCondition(perf.getAlertFirstCondition() + "");
//                performance.setLevelOneFirstThreshold(perf.getFirstThreshold());
//                performance.setLevelOneExpressionMore(perf.getExpressionMore());
//                performance.setLevelOneAlertSecondCondition(perf.getAlertSecondCondition() + "");
//                performance.setLevelOneSecondThreshold(perf.getSecondThreshold());
            } else if (perf.getAlertLevel().equals(LEVEL_TWO)) {
//                performance.setLevelTwoUuid(perf.getUuid());
                performance.getQuotaInfo().setLevelTwoUuid(perf.getUuid());
                performance.getQuotaInfo().setLevelTwoSeverity(perf.getSeverity() + "");
                performance.getQuotaInfo().setLevelTwoAlertFirstCondition(perf.getAlertFirstCondition() + "");
                performance.getQuotaInfo().setLevelTwoFirstThreshold(perf.getFirstThreshold());
                performance.getQuotaInfo().setLevelTwoExpressionMore(perf.getExpressionMore());
                performance.getQuotaInfo().setLevelTwoAlertSecondCondition(perf.getAlertSecondCondition() + "");
                performance.getQuotaInfo().setLevelTwoSecondThreshold(perf.getSecondThreshold());
//                performance.setLevelTwoSeverity(perf.getSeverity() + "");
//                performance.setLevelTwoAlertFirstCondition(perf.getAlertFirstCondition() + "");
//                performance.setLevelTwoFirstThreshold(perf.getFirstThreshold());
//                performance.setLevelTwoExpressionMore(perf.getExpressionMore());
//                performance.setLevelTwoAlertSecondCondition(perf.getAlertSecondCondition() + "");
//                performance.setLevelTwoSecondThreshold(perf.getSecondThreshold());
            }

        });
        List<Metrics> lastMetric = metricList.stream().filter(x -> x.getMetricGroup().equals("performance")
                && !mapp.keySet().contains(x.getUuid())).collect(Collectors.toList());
        lastMetric.forEach(m -> {
            UpPerformance performance = new UpPerformance();
            MetricInfo metricInfo = new MetricInfo();
            BeanUtils.copyProperties(m, metricInfo);
            performance.setQuotaInfo(metricInfo);
            performance.setTemplateUuid(templateEntity.getUuid());
            performance.setMetricUuid(m.getUuid());
            performanceList.add(performance);
        });
        view.setPerformance(performanceList);
        return view;
    }

    @Override
    public PageBean getAllTemplate(PageData view, String type) {
        List<String> lightTypeList = new ArrayList<>();
        if (type.equals(MonitorEnum.MiddleTypeEnum.NETWORK_DEVICE.value())) {
            lightTypeList.add(MonitorEnum.LightTypeEnum.SWITCH.value());
            lightTypeList.add(MonitorEnum.LightTypeEnum.ROUTER.value());
            lightTypeList.add(MonitorEnum.LightTypeEnum.LB.value());
            lightTypeList.add(MonitorEnum.LightTypeEnum.FIREWALL.value());
        } else if (type.equals(MonitorEnum.MiddleTypeEnum.DATABASE.value())) {
            lightTypeList.add(MonitorEnum.LightTypeEnum.MYSQL.value());
        } else if (type.equals(MonitorEnum.MiddleTypeEnum.MIDDLEWARE.value())) {
            lightTypeList.add(MonitorEnum.LightTypeEnum.TOMCAT.value());
        } else if (type.equals(MonitorEnum.MiddleTypeEnum.VIRTUALIZATION.value())) {
            lightTypeList.add(MonitorEnum.LightTypeEnum.CAS.value());
            lightTypeList.add(MonitorEnum.LightTypeEnum.CVK.value());
            lightTypeList.add(MonitorEnum.LightTypeEnum.VIRTUALMACHINE.value());
        } else if (type.equals(MonitorEnum.MiddleTypeEnum.CONTAINER.value())) {
            lightTypeList.add(MonitorEnum.LightTypeEnum.K8S.value());
            lightTypeList.add(MonitorEnum.LightTypeEnum.K8SNODE.value());
            lightTypeList.add(MonitorEnum.LightTypeEnum.K8SCONTAINER.value());
        }
        //lightypelist size为0 则为all
        List<AlertRuleTemplateEntity> list = dao.getAllTemplate(lightTypeList);
        PageBean pageBean = new PageBean(view.getPageIndex(), view.getPageSize(), list.size());
        List<AlertRuleTemplateEntity> mylist = dao.getTemplateByPage(pageBean.getStartIndex(), view.getPageSize(), lightTypeList);
        pageBean.setList(mylist);
        return pageBean;
    }

    @Override
    public AlertAvlRuleEntity getAvlRuleByRuleUuid(String uuid) {
        List<AlertAvlRuleEntity> avls = dao.getAvlRuleByRuleUuid(uuid);
        if (avls.size() > 0) {
            return avls.get(0);
        }
        return null;
    }

    @Override
    public AlertPerfRuleEntity getPerfRuleByRuleUuid(String uuid) {
        List<AlertPerfRuleEntity> perfs = dao.getPerfRuleByRuleUuid(uuid);
        if (perfs.size() > 0) {
            return perfs.get(0);
        }
        return null;
    }

    @Override
    public Metrics getMetricByUuid(String uuid) {
        return dao.getMetricsByUuid(uuid);
    }

    @Override
    public Metrics getMetricInfoByRule(String type, String ruleId) {
        String metricUuid = "";
        if (type.equals("avl")) {
            List<AlertAvlRuleEntity> avls = dao.getAvlRuleByRuleUuid(ruleId);
            if (avls.size() > 0) {
                metricUuid = avls.get(0).getMetricUuid();
            }
        } else {
            List<AlertPerfRuleEntity> perfs = dao.getPerfRuleByRuleUuid(ruleId);
            if (perfs.size() > 0) {
                metricUuid = perfs.get(0).getMetricUuid();
            }
        }
        Metrics metric = dao.getMetricsByUuid(metricUuid);
        return metric;
    }

    private IssueAvlMonitorRuleEntity convertMonitorPerf2IssueAvl(AlertAvlRuleEntity avlRuleEntity, AlertAvlRuleMonitorEntity avlMonitor) {
        IssueAvlMonitorRuleEntity avlMonitorRuleEntity = new IssueAvlMonitorRuleEntity();
        avlMonitorRuleEntity.setUuid(avlRuleEntity.getUuid());
        avlMonitorRuleEntity.setRuleName(avlMonitor.getAlertRuleName());
        avlMonitorRuleEntity.setMonitorUuid(avlMonitor.getMonitorUuid());
        avlMonitorRuleEntity.setSeverity(convertServerityDB(avlRuleEntity.getSeverity()));
        avlMonitorRuleEntity.setDescription(avlRuleEntity.getDescription());
        return avlMonitorRuleEntity;
    }


    private Rule convertAvlToRuleYaml(IssueAvlMonitorRuleEntity ruleEntity) {
        Rule rule = new Rule();
        rule.setAlert(ruleEntity.getRuleName());
        if (null != ruleEntity.getCondition()) {
            String expr = ruleEntity.getCondition() + "!=1 OR " + ruleEntity.getExpression() + "!=1";
            rule.setExpr(expr);
        }
        Map<String, String> labels = new HashMap<>();
        labels.put("severity", ruleEntity.getSeverity());
        labels.put("rule_id", ruleEntity.getUuid());
        rule.setLabels(labels);
        Map<String, String> annotations = new HashMap<>();
        annotations.put("description", ruleEntity.getDescription());
        annotations.put("current_value", "{{$value}}");
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


    private Rule convertPerfToRuleYaml(IssuePerfMonitorRuleEntity ruleEntity) {
        Rule rule = new Rule();
        rule.setAlert(ruleEntity.getRuleName());
        String expr = ruleEntity.getExpression() + " " + ruleEntity.getFirstCondition() + " " + ruleEntity.getFirstThreshold();
        if (null != ruleEntity.getSecondCondition() && null != ruleEntity.getMoreExpression()) {
            expr += " " + ruleEntity.getMoreExpression() + " " + ruleEntity.getExpression() + " " + ruleEntity.getSecondCondition() + " " + ruleEntity.getSecondThreshold();
        }
        rule.setExpr(expr);
        Map<String, String> labels = new HashMap<>();
        labels.put("severity", ruleEntity.getSeverity());
        labels.put("rule_id", ruleEntity.getUuid());
        rule.setLabels(labels);
        Map<String, String> annotations = new HashMap<>();
        annotations.put("description", ruleEntity.getDescription());
        annotations.put("threshold", ruleEntity.getFirstThreshold());
        annotations.put("current_value", "{{$value}}");
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
        return name + "{job=" + "'" + instanceId + "'" + "}";
    }


    private IssuePerfMonitorRuleEntity convertMonitorPerf2IssuePerf(AlertPerfRuleEntity alertPerfRuleEntity,
                                                                    AlertPerfRuleMonitorEntity alertPerfRuleMonitorEntity,
                                                                    int level, String levelOneFirstThreshold, int levelOneFirstCondition) {

        IssuePerfMonitorRuleEntity issuePerfMonitorRuleEntity = new IssuePerfMonitorRuleEntity();
        issuePerfMonitorRuleEntity.setUuid(alertPerfRuleEntity.getUuid());
        if (1 == level) {
            issuePerfMonitorRuleEntity.setSeverity(convertServerityDB(alertPerfRuleEntity.getSeverity()));
            issuePerfMonitorRuleEntity.setFirstCondition(convertConditionDB(alertPerfRuleEntity.getAlertFirstCondition()));
            issuePerfMonitorRuleEntity.setFirstThreshold(alertPerfRuleEntity.getFirstThreshold());
            issuePerfMonitorRuleEntity.setRuleName(alertPerfRuleMonitorEntity.getAlertRuleName());
            if (null != alertPerfRuleEntity.getSecondThreshold() && !("".equals(alertPerfRuleEntity.getSecondThreshold()))) {
                issuePerfMonitorRuleEntity.setMoreExpression(alertPerfRuleEntity.getExpressionMore());
                issuePerfMonitorRuleEntity.setSecondCondition(convertConditionDB(alertPerfRuleEntity.getAlertSecondCondition()));
                issuePerfMonitorRuleEntity.setSecondThreshold(alertPerfRuleEntity.getSecondThreshold());
            }
        } else if (2 == level) {
            issuePerfMonitorRuleEntity.setSeverity(convertServerityDB(alertPerfRuleEntity.getSeverity()));
            issuePerfMonitorRuleEntity.setFirstCondition(convertConditionDB(alertPerfRuleEntity.getAlertFirstCondition()));
            issuePerfMonitorRuleEntity.setFirstThreshold(alertPerfRuleEntity.getFirstThreshold());
            issuePerfMonitorRuleEntity.setRuleName(alertPerfRuleMonitorEntity.getAlertRuleName());
            if (null != alertPerfRuleEntity.getSecondThreshold() && !("".equals(alertPerfRuleEntity.getSecondThreshold()))) {
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

//    @Override
//    public AlertAvlRuleMonitorEntity getAvlRuleMonitor(String name) {
//        return dao.getAvlRuleMonitor(name);
//    }
//
//    @Override
//    public AlertPerfRuleMonitorEntity getPerfRuleMonitor(String name) {
//        return dao.getPerfRuleMonitor(name);
//    }

    @Override
    public ResMetricInfo getMetricInfo(String lightType, String monitorMode) {
        List<Metrics> infos = dao.getMetricByTypeAndMode(lightType, monitorMode);
        ResMetricInfo resMetricInfo = new ResMetricInfo();
        resMetricInfo.setMonitorMode(monitorMode);
        List<MetricInfo> available = new ArrayList<>();
        List<MetricInfo> performance = new ArrayList<>();
        infos.forEach(info -> {
            MetricInfo metricInfo = new MetricInfo();
            BeanUtils.copyProperties(info, metricInfo);
            String groupName = info.getMetricGroup();
            if (groupName.equals(MonitorConfigEnum.MetricGroupEnum.GROUP_AVAILABLE.value())) {
                available.add(metricInfo);
            } else if (groupName.equals(MonitorConfigEnum.MetricGroupEnum.GROUP_PERFORMANCE.value())) {
                performance.add(metricInfo);
            }
        });
        resMetricInfo.setAvailable(available);
        resMetricInfo.setPerformance(performance);
        return resMetricInfo;

    }

    @Override
    public boolean isTemplateNameDup(String name,String templateUuid) {
        List<AlertRuleTemplateEntity> entityList = dao.isTemplateNameDup(name);
        if (entityList.size() > 0) {
            Optional<AlertRuleTemplateEntity> rule = entityList.stream().filter(x->x.getUuid().equals(templateUuid)).findFirst();
            if (rule.isPresent()){
                return true;
            }else {
                return false;
            }
        } else {
            return true;
        }
    }

    @Override
    public String getTemplateByLightType(String lightType) throws JsonProcessingException {
        MonitorTemplate monitorTemplate = new MonitorTemplate();
//        List<LightTypeEntity> lightTypeList = monitorService.getLightTypeEntity();
//        Optional<LightTypeEntity> mainLightTypeEntity = lightTypeList.stream().filter(x -> x.getName().equals(lightType)).findFirst();
        List<AlertRuleTemplateEntity> mainRuleList = dao.getTemplateByLightType(lightType);
        if (lightType.equals(MonitorEnum.LightTypeEnum.SWITCH.value()) || lightType.equals(MonitorEnum.LightTypeEnum.ROUTER.value())
                || lightType.equals(MonitorEnum.LightTypeEnum.FIREWALL.value()) || lightType.equals(MonitorEnum.LightTypeEnum.LB.value())) {
            //网络设备 monitormode需要分snmp_v1,snmp_v2
            List<RuleTemplate> v1templates = new ArrayList<>();
            List<RuleTemplate> v2templates = new ArrayList<>();
            mainRuleList.forEach(rule -> {
                RuleTemplate v = setTemple(rule, lightType);
                if (rule.getMonitorMode().equals("snmp_v1")) {
                    v1templates.add(v);
                } else if (rule.getMonitorMode().equals("snmp_v2")) {
                    v2templates.add(v);
                }
            });
            monitorTemplate.setSnmp_v1(v1templates);
            monitorTemplate.setSnmp_v2(v2templates);
        } else if (lightType.equals(MonitorEnum.LightTypeEnum.CAS.value())) {
            List<RuleTemplate> casRule = new ArrayList<>();
            mainRuleList.forEach(rule -> {
                casRule.add(setTemple(rule, lightType));
            });
            monitorTemplate.setCas(casRule);
            //不要cluster了
//            List<AlertRuleTemplateEntity> casClusterRuleList = dao.getTemplateByLightType(lightType);
//            List<RuleTemplate> casClusterRule = new ArrayList<>();
//            casClusterRuleList.forEach(rule -> {
//                casClusterRule.add(setTemple(rule, lightType));
//            });
//            monitorTemplate.setCascluster(casClusterRule);

            List<AlertRuleTemplateEntity> cvkRuleList = dao.getTemplateByLightType(MonitorEnum.LightTypeEnum.CVK.value());
            List<RuleTemplate> cvkRule = new ArrayList<>();
            cvkRuleList.forEach(rule -> {
                cvkRule.add(setTemple(rule, lightType));
            });
            monitorTemplate.setCvk(cvkRule);

            List<AlertRuleTemplateEntity> vmRuleList = dao.getTemplateByLightType(MonitorEnum.LightTypeEnum.VIRTUALMACHINE.value());
            List<RuleTemplate> vmRule = new ArrayList<>();
            vmRuleList.forEach(rule -> {
                vmRule.add(setTemple(rule, lightType));
            });
            monitorTemplate.setVirtualMachine(vmRule);
        } else if (lightType.equals(MonitorEnum.LightTypeEnum.K8S.value())) {
            List<RuleTemplate> k8sRule = new ArrayList<>();
            mainRuleList.forEach(rule -> {
                k8sRule.add(setTemple(rule, lightType));
            });
            monitorTemplate.setK8s(k8sRule);

            List<AlertRuleTemplateEntity> k8snRuleList = dao.getTemplateByLightType(MonitorEnum.LightTypeEnum.K8SNODE.value());
            List<RuleTemplate> k8snRule = new ArrayList<>();
            k8snRuleList.forEach(rule -> {
                k8snRule.add(setTemple(rule, lightType));
            });
            monitorTemplate.setK8sn(k8snRule);

            List<AlertRuleTemplateEntity> k8scRuleList = dao.getTemplateByLightType(MonitorEnum.LightTypeEnum.K8SCONTAINER.value());
            List<RuleTemplate> k8scRule = new ArrayList<>();
            k8scRuleList.forEach(rule -> {
                k8scRule.add(setTemple(rule, lightType));
            });
            monitorTemplate.setK8sc(k8scRule);
        } else {
            List<RuleTemplate> others = new ArrayList<>();
            mainRuleList.forEach(rule -> {
                others.add(setTemple(rule, lightType));
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

//    @Override
//    public boolean addAvlRuleMonitorList(List<AlertAvlRuleMonitorEntity> avlRuleMonitorList) {
//        avlRuleMonitorList.forEach(x -> {
//            dao.addAvlRuleMonitor(x);
//        });
//        return true;
//    }
//
//    @Override
//    public boolean addPerfRuleMonitorList(List<AlertPerfRuleMonitorEntity> perfRuleMonitorList) {
//        perfRuleMonitorList.forEach(x -> {
//            dao.addPerfRuleMonitor(x);
//        });
//        return true;
//    }
//
//    @Override
//    public boolean addTemplateMonitor(AlertRuleTemplateMonitorEntity templateMonitorEntity) {
//        return dao.addTemplateMonitor(templateMonitorEntity);
//    }

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
