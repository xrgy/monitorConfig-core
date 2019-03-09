package com.gy.monitorConfig.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gy.monitorConfig.common.MonitorEnum;
import com.gy.monitorConfig.entity.monitor.*;
import com.gy.monitorConfig.service.MonitorService;
import com.gy.monitorConfig.util.EtcdUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.io.IOException;
import java.util.List;

/**
 * Created by gy on 2018/5/5.
 */
@Service
public class MonitorServiceImpl implements MonitorService {

//    private String ip = "http://127.0.0.1";

//    private String IP = "http://172.17.5.135";

//    private String IP = "http://172.31.105.232";
    private String PORT = "8084";
//    private String PORT = "30004";
    private String PREFIX = "monitor";
    private String Light_PATH = "getLightType";
    private static final String GET_MONITOR_RECORD_BY_TEMPLATE= "getMonitorRecordByTemplateId";
    private static final String HTTP="http://";

    @Autowired
    ObjectMapper mapper;


    @Bean
    public RestTemplate rest(){
        return new RestTemplate();
    }

    private String monitorPrefix(){
        String ip = "";
        try {
//            ip="127.0.0.1";
            ip = EtcdUtil.getClusterIpByServiceName("monitor-core-service");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return HTTP+ip+":"+PORT+"/"+PREFIX+"/";
    }

    @Override
    public List<NetworkMonitorEntity> getNetworkMonitorRecordByTemplateId(String uuid) {
        ResponseEntity<String> response = rest().getForEntity(monitorPrefix()+GET_MONITOR_RECORD_BY_TEMPLATE+"?uuid={1}&lightType={2}",String.class,uuid, MonitorEnum.LightTypeEnum.FIREWALL.value());
        try {
            return mapper.readValue(response.getBody(),new TypeReference<List<NetworkMonitorEntity>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<TomcatMonitorEntity> getTomcatRecordByTemplateId(String uuid) {
        ResponseEntity<String> response = rest().getForEntity(monitorPrefix()+GET_MONITOR_RECORD_BY_TEMPLATE+"?uuid={1}&lightType={2}",String.class,uuid, MonitorEnum.LightTypeEnum.TOMCAT.value());
        try {
            return mapper.readValue(response.getBody(),new TypeReference<List<TomcatMonitorEntity>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<DBMonitorEntity> getDbMonitorRecordByTemplateId(String uuid) {
        ResponseEntity<String> response = rest().getForEntity(monitorPrefix()+GET_MONITOR_RECORD_BY_TEMPLATE+"?uuid={1}&lightType={2}",String.class,uuid, MonitorEnum.LightTypeEnum.MYSQL.value());
        try {
            return mapper.readValue(response.getBody(),new TypeReference<List<DBMonitorEntity>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<CasMonitorEntity> getCasMonitorRecordByTemplateId(String uuid) {
        ResponseEntity<String> response = rest().getForEntity(monitorPrefix()+GET_MONITOR_RECORD_BY_TEMPLATE+"?uuid={1}&lightType={2}",String.class,uuid, MonitorEnum.LightTypeEnum.CAS.value());
        try {
            return mapper.readValue(response.getBody(),new TypeReference<List<CasMonitorEntity>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<HostMonitorEntity> getHostMonitorRecordByTemplateId(String uuid) {
        ResponseEntity<String> response = rest().getForEntity(monitorPrefix()+GET_MONITOR_RECORD_BY_TEMPLATE+"?uuid={1}&lightType={2}",String.class,uuid, MonitorEnum.LightTypeEnum.CVK.value());
        try {
            return mapper.readValue(response.getBody(),new TypeReference<List<HostMonitorEntity>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<VmMonitorEntity> getVmMonitorRecordByTemplateId(String uuid) {
        ResponseEntity<String> response = rest().getForEntity(monitorPrefix()+GET_MONITOR_RECORD_BY_TEMPLATE+"?uuid={1}&lightType={2}",String.class,uuid, MonitorEnum.LightTypeEnum.VIRTUALMACHINE.value());
        try {
            return mapper.readValue(response.getBody(),new TypeReference<List<VmMonitorEntity>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<K8sMonitorEntity> getK8sMonitorRecordByTemplateId(String uuid) {
        ResponseEntity<String> response = rest().getForEntity(monitorPrefix()+GET_MONITOR_RECORD_BY_TEMPLATE+"?uuid={1}&lightType={2}",String.class,uuid, MonitorEnum.LightTypeEnum.K8S.value());
        try {
            return mapper.readValue(response.getBody(),new TypeReference<List<K8sMonitorEntity>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<K8snodeMonitorEntity> getK8snodeMonitorRecordByTemplateId(String uuid) {
        ResponseEntity<String> response = rest().getForEntity(monitorPrefix()+GET_MONITOR_RECORD_BY_TEMPLATE+"?uuid={1}&lightType={2}",String.class,uuid, MonitorEnum.LightTypeEnum.K8SNODE.value());
        try {
            return mapper.readValue(response.getBody(),new TypeReference<List<K8snodeMonitorEntity>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<K8scontainerMonitorEntity> getK8scontainerMonitorRecordByTemplateId(String uuid) {
        ResponseEntity<String> response = rest().getForEntity(monitorPrefix()+GET_MONITOR_RECORD_BY_TEMPLATE+"?uuid={1}&lightType={2}",String.class,uuid, MonitorEnum.LightTypeEnum.K8SCONTAINER.value());
        try {
            return mapper.readValue(response.getBody(),new TypeReference<List<K8scontainerMonitorEntity>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
