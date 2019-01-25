package com.gy.monitorConfig.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * Created by gy on 2018/10/15.
 */
@Getter
@Setter
public class IssuePerfMonitorRuleEntity {

    private String uuid;

    private String severity;

    private String firstCondition;

    private String firstThreshold;

    private String ruleName;

    private String moreExpression;

    private String secondCondition;

    private String secondThreshold;

    private String description;

    private String monitorUuid;

    private String unit;

    private String expression;
}
