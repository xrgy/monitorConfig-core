package com.gy.monitorConfig.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * Created by gy on 2018/6/10.
 */
@Getter
@Setter
public class AlertRuleEntity {

    private int severity;

    private int alertFirstCondition;

    private String firstThreshold;

    private String expressionMore;

    private int alertSecondCondition;

    private String secondThreshold;
}
