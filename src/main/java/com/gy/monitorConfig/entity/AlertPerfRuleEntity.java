package com.gy.monitorConfig.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Created by gy on 2018/5/31.
 */
@Data
@Entity
@Table(name = "tbl_alert_perf_rule")
public class AlertPerfRuleEntity {
    @Id
    @Column(name = "uuid")
    private String uuid;

    @Column(name = "quota_uuid")
    private String metricUuid;

    @Column(name = "template_uuid")
    private String templateUuid;

    @Column(name = "severity")
    private int severity;

    @Column(name = "alert_first_condition")
    private int alertFirstCondition;

    @Column(name = "first_threshold")
    private String firstThreshold;

    @Column(name = "expression_more")
    private String expressionMore;

    @Column(name = "alert_second_condition")
    private int alertSecondCondition;

    @Column(name = "second_threshold")
    private String secondThreshold;

    @Column(name = "description")
    private String description;

    @Column(name = "alert_level")
    private String alertLevel;

}
