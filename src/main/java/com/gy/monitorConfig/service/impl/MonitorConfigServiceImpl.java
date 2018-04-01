package com.gy.monitorConfig.service.impl;

import com.gy.monitorConfig.dao.MonitorConfigDao;
import com.gy.monitorConfig.entity.TestEntity;
import com.gy.monitorConfig.service.MonitorConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * Created by gy on 2018/3/31.
 */
@Service
public class MonitorConfigServiceImpl implements MonitorConfigService {

    @Autowired
    MonitorConfigDao dao;

    @Override
    public TestEntity getJPAInfo() {
        return dao.getJPAInfo();
    }
}
