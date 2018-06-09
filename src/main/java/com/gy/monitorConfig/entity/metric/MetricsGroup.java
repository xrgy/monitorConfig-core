package com.gy.monitorConfig.entity.metric;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Created by gy on 2018/6/8.
 */
@Data
@Entity
@Table(name = "tbl_monitor_metric_group")
public class MetricsGroup {

    @Id
    @Column(name = "uuid")
    private String uuid;

    @Column(name = "name")
    private String name;
}
