package com.gy.monitorConfig.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gy.monitorConfig.entity.TestEntity;
import com.gy.monitorConfig.entity.metric.NewTemplateView;
import com.gy.monitorConfig.entity.metric.ResMetricInfo;
import com.gy.monitorConfig.service.MonitorConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Created by gy on 2018/3/31.
 */
@RestController
@RequestMapping("monitorConfig")
public class MonitorConfigController {

    @Autowired
    private MonitorConfigService service;

    @Autowired
    private ObjectMapper mapper;

    @RequestMapping("jpa")
    @ResponseBody
    public TestEntity testJPA(HttpServletRequest request){
//        TestEntity entity = new TestEntity();
//        entity.setId("sasada");
//        entity.setName("gygy");
//        return entity;
        return service.getJPAInfo();
    }

    @RequestMapping("avlRuleMonitor")
    @ResponseBody
    public String getAvlRuleMonitor(String name) throws Exception{
        return mapper.writeValueAsString(service.getAvlRuleMonitor(name));
    }

    @RequestMapping("perfRuleMonitor")
    @ResponseBody
    public String getPerfRuleMonitor(String name) throws Exception{
        return mapper.writeValueAsString(service.getPerfRuleMonitor(name));
    }

    @RequestMapping("getMetricInfo")
    @ResponseBody
    public ResMetricInfo getMetricInfo(String lightType, String monitorMode) {
        return service.getMetricInfo(lightType,monitorMode);
    }

    @RequestMapping("isTemplateNameDup")
    @ResponseBody
    public boolean isTemplateNameDup(String name){
        //返回true 未重复
        return service.isTemplateNameDup(name);
    }

    @RequestMapping("addTemplate")
    @ResponseBody
    public boolean addTemplate(@RequestBody String data) throws IOException {
        NewTemplateView view = mapper.readValue(data,NewTemplateView.class);
        return service.addTemplate(view);
    }
}
