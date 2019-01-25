package com.gy.monitorConfig.dao;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.gy.monitorConfig.entity.etcd.RuleGroups;
import mousio.etcd4j.responses.EtcdAuthenticationException;
import mousio.etcd4j.responses.EtcdException;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Created by gy on 2018/3/31.
 */
public interface EtcdDao {

    public boolean insertEtcdAlert(String uuid,RuleGroups groups) throws IOException, EtcdAuthenticationException, TimeoutException, EtcdException;

    public boolean delEtcdAlert(String uuid) ;

}
