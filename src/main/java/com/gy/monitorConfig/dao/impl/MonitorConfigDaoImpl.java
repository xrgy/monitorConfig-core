package com.gy.monitorConfig.dao.impl;

import com.gy.monitorConfig.dao.MonitorConfigDao;
import com.gy.monitorConfig.entity.TestEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Created by gy on 2018/3/31.
 */
@Repository
public class MonitorConfigDaoImpl implements MonitorConfigDao  {

    @Autowired
    @Qualifier("database")
    Executor executor;

    @Autowired
    @PersistenceContext
    EntityManager em;

    @Override
    public TestEntity getJPAInfo() {
        List<TestEntity> result = em.createQuery("FROM TestEntity",TestEntity.class)
                .getResultList();
        if (result.size() == 0){
            return null;
        }
        return result.get(0);
    }
}
