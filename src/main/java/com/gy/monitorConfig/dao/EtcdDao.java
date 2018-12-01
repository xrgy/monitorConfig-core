package com.gy.monitorConfig.dao;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.gy.monitorConfig.entity.etcd.RuleGroups;

/**
 * Created by gy on 2018/3/31.
 */
public interface EtcdDao {

    public boolean insertEtcdAlert(String uuid,RuleGroups groups) throws JsonProcessingException ;

    public boolean delEtcdAlert(String uuid) ;

}
