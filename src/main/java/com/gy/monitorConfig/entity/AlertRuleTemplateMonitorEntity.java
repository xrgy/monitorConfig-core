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
@Table(name = "tbl_alert_rule_template_monitor")
public class AlertRuleTemplateMonitorEntity {
    @Id
    @Column(name = "uuid")
    private String uuid;

    @Column(name = "monitor_uuid")
    private String monitorUuid;

    @Column(name = "template_uuid")
    private String templateUuid;

}
