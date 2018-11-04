package com.gy.monitorConfig.dao;


import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Created by gy on 2018/3/31.
 */
public interface EtcdDao {

    public boolean insertEtcdAlert(String uuid,String str) throws JsonProcessingException ;

    public boolean delEtcdAlert(String uuid) ;

}
