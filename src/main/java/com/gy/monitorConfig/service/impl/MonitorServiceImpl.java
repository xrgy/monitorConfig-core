package com.gy.monitorConfig.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gy.monitorConfig.entity.monitor.LightTypeEntity;
import com.gy.monitorConfig.service.MonitorService;
import com.gy.monitorConfig.util.EtcdUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.persistence.Id;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

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
    private static final String HTTP="http://";

    @Autowired
    ObjectMapper mapper;


    @Bean
    public RestTemplate rest(){
        return new RestTemplate();
    }

    private String monitorPrefix(){
        String ip = "";
//        try {
            ip="127.0.0.1";
//            ip = EtcdUtil.getClusterIpByServiceName("monitor-core-service");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        return HTTP+ip+":"+PORT+"/"+PREFIX+"/";
    }

//
//    @Override
//    public List<LightTypeEntity> getLightTypeEntity() {
//        ResponseEntity<String> response = rest().getForEntity(monitorPrefix()+Light_PATH,String.class);
//        try {
//            return mapper.readValue(response.getBody(),new TypeReference<List<LightTypeEntity>>(){});
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return null;
//    };

}
