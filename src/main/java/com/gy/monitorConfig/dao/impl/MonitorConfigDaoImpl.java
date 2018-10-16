package com.gy.monitorConfig.dao.impl;

import com.gy.monitorConfig.dao.MonitorConfigDao;
import com.gy.monitorConfig.entity.*;
import com.gy.monitorConfig.entity.metric.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

/**
 * Created by gy on 2018/3/31.
 */
@Repository
public class MonitorConfigDaoImpl implements MonitorConfigDao {

    @Autowired
    @Qualifier("database")
    Executor executor;

    @Autowired
    @PersistenceContext
    EntityManager em;

    @Override
    public TestEntity getJPAInfo() {
        List<TestEntity> result = em.createQuery("FROM TestEntity", TestEntity.class)
                .getResultList();
        if (result.size() == 0) {
            return null;
        }
        return result.get(0);
    }

    @Override
    public AlertAvlRuleMonitorEntity getAvlRuleMonitor(String name) {
        String sql = "From AlertAvlRuleMonitorEntity Where alertRuleName = :name";
        List<AlertAvlRuleMonitorEntity> list = em.createQuery(sql, AlertAvlRuleMonitorEntity.class)
                .setParameter("name", name)
                .getResultList();
        if (list.size() > 0) {
            return list.get(0);
        }
        return null;
    }

    @Override
    public AlertPerfRuleMonitorEntity getPerfRuleMonitor(String name) {
        String sql = "From AlertPerfRuleMonitorEntity Where alertRuleName = :name";
        List<AlertPerfRuleMonitorEntity> list = em.createQuery(sql, AlertPerfRuleMonitorEntity.class)
                .setParameter("name", name)
                .getResultList();
        if (list.size() > 0) {
            return list.get(0);
        }
        return null;
    }

    @Override
    public List<MetricsCollection> getMetricsCollection() {
        String sql = "From MetricsCollection";
        return em.createQuery(sql, MetricsCollection.class)
                .getResultList();
    }

    @Override
    public List<MetricsGroup> getMetricsGroup() {
        String sql = "From MetricsGroup";
        return em.createQuery(sql, MetricsGroup.class)
                .getResultList();
    }

    @Override
    public List<MetricsType> getMetricsType() {
        String sql = "From MetricsType";
        return em.createQuery(sql, MetricsType.class)
                .getResultList();
    }

    @Override
    public List<Metrics> getMetricByTypeAndMode(String lightType, String monitorMode) {
        String sql = "From Metrics Where metricLightTypeId =:lightTypeId AND metricCollectionId =:collectionId";
        return em.createQuery(sql, Metrics.class)
                .setParameter("lightTypeId", lightType)
                .setParameter("collectionId", monitorMode)
                .getResultList();
    }

    @Override
    public boolean isTemplateNameDup(String name) {
        String sql = "From AlertRuleTemplateEntity Where templateName = :name";
        List<AlertRuleTemplateEntity> entityList = em.createQuery(sql, AlertRuleTemplateEntity.class)
                .setParameter("name", name)
                .getResultList();
        if (entityList.size()>0){
            return false;
        }else {
            return true;
        }

    }

    @Override
    @Transactional
    public boolean addTemplate(AlertRuleTemplateEntity templateEntity) {
        try {
            em.merge(templateEntity);
            return true;
        }catch (Exception e){
            return false;
        }
    }

    @Override
    @Transactional
    public boolean addAvlRule(AlertAvlRuleEntity entity) {
        try {
            em.merge(entity);
            return true;
        }catch (Exception e){
            return false;
        }
    }

    @Override
    @Transactional
    public boolean addPerfRule(AlertPerfRuleEntity entity) {
        try {
            em.merge(entity);
            return true;
        }catch (Exception e){
            return false;
        }
    }

    @Override
    public List<AlertRuleTemplateEntity> getTemplateByLightType(String lightTypeUuid, String monitorMode) {
        String sql = "From AlertRuleTemplateEntity Where monitorMode =:monitorMode AND resourceUuid =:resourceUuid";
        return em.createQuery(sql, AlertRuleTemplateEntity.class)
                .setParameter("monitorMode", monitorMode)
                .setParameter("resourceUuid", lightTypeUuid)
                .getResultList();
    }

    @Override
    public List<AlertAvlRuleEntity> getAvlRuleByTemplateId(String templateId) {
        String sql = "From AlertAvlRuleEntity where templateUuid =:templateId";
        return em.createQuery(sql,AlertAvlRuleEntity.class)
                .setParameter("templateId",templateId)
                .getResultList();
    }

    @Override
    public List<AlertPerfRuleEntity> getPerfRuleByTemplateId(String templateId) {
        String sql = "From AlertPerfRuleEntity where templateUuid =:templateId";
        return em.createQuery(sql,AlertPerfRuleEntity.class)
                .setParameter("templateId",templateId)
                .getResultList();
    }

    @Override
    @Transactional
    public boolean addAvlRuleMonitor(AlertAvlRuleMonitorEntity x) {
        try {
            em.merge(x);
            return true;
        }catch (Exception e){
            return false;
        }
    }

    @Override
    @Transactional
    public boolean addPerfRuleMonitor(AlertPerfRuleMonitorEntity x) {
        try {
            em.merge(x);
            return true;
        }catch (Exception e){
            return false;
        }
    }

    @Override
    @Transactional
    public boolean addTemplateMonitor(AlertRuleTemplateMonitorEntity templateMonitorEntity) {
        try {
            em.merge(templateMonitorEntity);
            return true;
        }catch (Exception e){
            return false;
        }
    }

    @Override
    public List<Metrics> getMetricsByLightType(String lightTypeId) {
        String sql = "From Metrics where metricLightTypeId =:lightTypeId";
        return em.createQuery(sql,Metrics.class)
                .setParameter("lightTypeId",lightTypeId)
                .getResultList();
    }


}
