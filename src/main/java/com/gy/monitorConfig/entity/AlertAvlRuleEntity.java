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
@Table(name = "tbl_alert_avl_rule")
public class AlertAvlRuleEntity {
    @Id
    @Column(name = "uuid")
    private String uuid;

    @Column(name = "quota_uuid")
    private String metricUuid;

    @Column(name = "template_uuid")
    private String templateUuid;

    @Column(name = "severity")
    private int severity;

    @Column(name = "description")
    private String description;

}
