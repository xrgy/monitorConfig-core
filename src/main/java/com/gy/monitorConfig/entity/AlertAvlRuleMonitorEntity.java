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
@Table(name = "tbl_alert_avl_rule_monitor")
public class AlertAvlRuleMonitorEntity {
    @Id
    @Column(name = "uuid")
    private String uuid;

    @Column(name = "monitor_uuid")
    private String monitorUuid;

    @Column(name = "avl_rule_uuid")
    private String avlRuleUuid;

    @Column(name = "alert_rule_name")
    private String alertRuleName;

}
