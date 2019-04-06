package com.gy.monitorConfig.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @Column(name = "light_type")
    @JsonProperty("light_type")
    private String lightType;

    @Column(name = "template_name")
    @JsonProperty("template_name")
    private String templateName;

    @Column(name = "monitor_mode")
    @JsonProperty("monitor_mode")
    private String monitorMode;

    @Column(name = "template_type")
    @JsonProperty("template_type")
    private int templateType;

    @Column(name = "create_time")
    @JsonProperty("create_time")
    private Date createTime;

}
