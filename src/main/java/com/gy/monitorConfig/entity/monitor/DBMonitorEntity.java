package com.gy.monitorConfig.entity.monitor;

import lombok.Getter;
import lombok.Setter;


/**
 * Created by gy on 2018/5/5.
 */
@Getter
@Setter
public class DBMonitorEntity {

    private String uuid;

    private String name;

    private String ip;

    private String databasename;

    private String username;

    private String password;

    private String port;

    private String monitorType;

    private String scrapeInterval;

    private String scrapeTimeout;

    private String templateId;

}
