package com.gy.monitorConfig.controller;

import com.gy.monitorConfig.entity.TestEntity;
import com.gy.monitorConfig.service.MonitorConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by gy on 2018/3/31.
 */
@RestController
@RequestMapping("monitorConfig")
public class MonitorConfigController {

    @Autowired
    private MonitorConfigService service;

    @RequestMapping("jpa")
    @ResponseBody
    public TestEntity testJPA(){
//        TestEntity entity = new TestEntity();
//        entity.setId("sasada");
//        entity.setName("gygy");
//        return entity;
        return service.getJPAInfo();
    }
}
