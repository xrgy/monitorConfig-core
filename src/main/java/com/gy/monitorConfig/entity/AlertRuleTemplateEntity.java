package com.gy.monitorConfig.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

/**
 * Created by gy on 2018/5/31.
 */
@Data
@Entity
@Table(name = "tbl_alert_rule_template")
public class AlertRuleTemplateEntity {
    @Id
    @Column(name = "uuid")
    private String uuid;

    @Column(name = "resource_uuid")
    private String resourceUuid;

    @Column(name = "template_name")
    private String templateName;

    @Column(name = "monitor_mode")
    private String monitor_mode;

    @Column(name = "template_type")
    private int templateType;

    @Column(name = "create_time")
    private Date createTime;

}
