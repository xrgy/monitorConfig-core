package com.gy.monitorConfig.dao.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gy.monitorConfig.dao.EtcdDao;
import com.gy.monitorConfig.entity.etcd.RuleGroup;
import com.gy.monitorConfig.entity.etcd.RuleGroups;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Created by gy on 2018/10/23.
 */
@Repository
public class EtcdDaoImpl implements EtcdDao {

//    private static final String IP="47.105.64.176";
    private static final String IP="47.94.157.199";

    //    private static final String IP="172.31.105.232";
    private static final String ETCD_PORT="2379";
    private static final String ETCD_PREFIX="v2/keys/gy";
    private static final String ALERT_ETCD="alert";
    private static final String HTTP="http://";



    private String etcdPrefix() {
        return HTTP+IP + ":" + ETCD_PORT + "/" + ETCD_PREFIX + "/";
    }

    @Bean
    public RestTemplate rest() {
        return new RestTemplate();
    }

    @Autowired
    ObjectMapper objectMapper;

    @Override
    public boolean insertEtcdAlert(String uuid, RuleGroups groups) throws JsonProcessingException {
        rest().put(etcdPrefix()+ALERT_ETCD+"/{1}?value={2}",null,uuid,objectMapper.writeValueAsString(groups));
        return true;
    }

    @Override
    public boolean delEtcdAlert(String uuid) {
        rest().delete(etcdPrefix()+ALERT_ETCD+"/{1}",uuid);
        return false;
    }

}
