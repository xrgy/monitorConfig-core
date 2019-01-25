package com.gy.monitorConfig.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gy.monitorConfig.entity.*;
import com.gy.monitorConfig.entity.metric.NewTemplateView;
import com.gy.monitorConfig.entity.metric.ResMetricInfo;
import com.gy.monitorConfig.entity.metric.UpTemplateView;
import com.gy.monitorConfig.service.MonitorConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

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
    public String testJPA(HttpServletRequest request, HttpServletResponse response) {
//        TestEntity entity = new TestEntity();
//        entity.setId("sasada");
//        entity.setName("gygy");
//        return entity;
//        return service.getJPAInfo();
        response.setHeader("Access-Control-Allow-Origin", "*");
        return "hello world";
    }

    @RequestMapping("testjsonp")
    @ResponseBody
    public String testJsonp(HttpServletRequest request, HttpServletResponse response) {

        String callback = request.getParameter("callback");
        return callback + "('hello world')";
    }

    @RequestMapping("testxss")
    @ResponseBody
    public String testXss(HttpServletRequest request, HttpServletResponse response) {

//        String callback = request.getParameter("callback");
//        return callback + "('hello world')";
//        return request.getParameter("xsstext");
        return "<script>alert('ddd')</script>";
    }


//    @RequestMapping("avlRuleMonitor")
//    @ResponseBody
//    public String getAvlRuleMonitor(String name) throws Exception {
//        return mapper.writeValueAsString(service.getAvlRuleMonitor(name));
//    }
//
//    @RequestMapping("perfRuleMonitor")
//    @ResponseBody
//    public String getPerfRuleMonitor(String name) throws Exception {
//        return mapper.writeValueAsString(service.getPerfRuleMonitor(name));
//    }

    @RequestMapping("getAvlRuleByRuleUuid")
    @ResponseBody
    public String getAvlRuleByRuleUuid(String uuid) throws Exception {
        return mapper.writeValueAsString(service.getAvlRuleByRuleUuid(uuid));
    }

    @RequestMapping("getPerfRuleByRuleUuid")
    @ResponseBody
    public String getPerfRuleByRuleUuid(String uuid) throws Exception {
        return mapper.writeValueAsString(service.getPerfRuleByRuleUuid(uuid));
    }

    @RequestMapping("getMetricInfo")
    @ResponseBody
    public ResMetricInfo getMetricInfo(String lightType, String monitorMode) {
        return service.getMetricInfo(lightType, monitorMode);
    }

    @RequestMapping("isTemplateNameDup")
    @ResponseBody
    public boolean isTemplateNameDup(String name) {
        //返回true 未重复
        return service.isTemplateNameDup(name);
    }

    @RequestMapping(value = "template",method = RequestMethod.POST)
    @ResponseBody
    public boolean addTemplate(@RequestBody String data) throws IOException {
        NewTemplateView view = mapper.readValue(data, NewTemplateView.class);
        return service.addTemplate(view);
    }

    @RequestMapping(value = "updateTemplate",method = RequestMethod.POST)
    @ResponseBody
    public boolean updateTemplate(@RequestBody String data) throws IOException {
        UpTemplateView view = mapper.readValue(data, UpTemplateView.class);
        return service.updateTemplate(view);
    }

    @RequestMapping(value = "template/{id}",method = RequestMethod.DELETE)
    @ResponseBody
    public String delTemplate(@PathVariable String id){
        service.detTemplate(id);
        return "SUCCESS";
    }

    @RequestMapping("getTemplate")
    @ResponseBody
    public String getTemplateByLightType(String lightType) throws JsonProcessingException {
        return service.getTemplateByLightType(lightType);
    }

    @RequestMapping("getAllTemplate")
    @ResponseBody
    public String getAllTemplate() throws JsonProcessingException {
        return mapper.writeValueAsString(service.getAllTemplate());
    }


    @RequestMapping("getAvlRule")
    @ResponseBody
    public String getAvlRuleByTemplateId(String templateId) throws JsonProcessingException {
        return mapper.writeValueAsString(service.getAvlRuleByTemplateId(templateId));
    }

    @RequestMapping("getPerfRule")
    @ResponseBody
    public String getPerfRuleByTemplateId(String templateId) throws JsonProcessingException {
        return mapper.writeValueAsString(service.getPerfRuleByTemplateId(templateId));
    }

//    @RequestMapping("addAvlRuleMonitorList")
//    @ResponseBody
//    public boolean addAvlRuleMonitorList(@RequestBody String data) throws IOException {
//        List<AlertAvlRuleMonitorEntity> avlRuleMonitorList = mapper.readValue(data, new TypeReference<List<AlertAvlRuleMonitorEntity>>() {
//        });
//        return service.addAvlRuleMonitorList(avlRuleMonitorList);
//    }
//
//    @RequestMapping("addPerfRuleMonitorList")
//    @ResponseBody
//    public boolean addPerfRuleMonitorList(@RequestBody String data) throws IOException {
//        List<AlertPerfRuleMonitorEntity> perfRuleMonitorList = mapper.readValue(data, new TypeReference<List<AlertPerfRuleMonitorEntity>>() {
//        });
//        return service.addPerfRuleMonitorList(perfRuleMonitorList);
//    }

//    @RequestMapping("addTemplateMonitor")
//    @ResponseBody
//    public boolean addTemplateMonitor(@RequestBody String data) throws IOException {
//        AlertRuleTemplateMonitorEntity templateMonitorEntity = mapper.readValue(data, AlertRuleTemplateMonitorEntity.class);
//        return service.addTemplateMonitor(templateMonitorEntity);
//    }

    @RequestMapping("getMetricsUseLight")
    @ResponseBody
    public String getMetricsByLightType(String lightTypeId) throws JsonProcessingException {
        return mapper.writeValueAsString(service.getMetricsByLightType(lightTypeId));
    }

    @RequestMapping("addAlertTemplateToEtcd")
    @ResponseBody
    public String addAlertTemplateToEtcd(@RequestBody Map<String, Object> map) throws IOException {
        RuleMonitorEntity entity = mapper.readValue((String) map.get("ruleMonitorEntity"), RuleMonitorEntity.class);
        service.addAlertTemplateToEtcd((String) map.get("lightTypeId"), (String) map.get("templateId"), entity);
        return "";
    }

    @RequestMapping(value = "delAlertMonitorRule", method = RequestMethod.DELETE)
    @ResponseBody
    public String delAlertMonitorRule(String uuid) throws JsonProcessingException {
        service.delAlertMonitorRule(uuid);
        return "";
    }


    @RequestMapping(value = "getOpenTemplateData", method = RequestMethod.GET)
    @ResponseBody
    public String getOpenTemplateData(String uuid) throws JsonProcessingException {
        return mapper.writeValueAsString(service.getOpenTemplateData(uuid));
    }

    @RequestMapping("getMetricByUuid")
    @ResponseBody
    public String getMetricByUuid(String uuid) throws JsonProcessingException {
        return mapper.writeValueAsString(service.getMetricByUuid(uuid));
    }
}
