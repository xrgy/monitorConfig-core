package com.gy.monitorConfig.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * Created by gy on 2018/10/15.
 */
@Getter
@Setter
public class IssueAvlMonitorRuleEntity {

    private String uuid;

    private String severity;

    private String ruleName;

    private String description;

    private String monitorUuid;

    private String condition;

    private String expression;


}
