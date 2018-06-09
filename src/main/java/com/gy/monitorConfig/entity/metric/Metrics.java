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
@Table(name = "tbl_monitor_metrics")
public class Metrics {

    @Id
    @Column(name = "uuid")
    private String uuid;

    @Column(name = "name")
    private String name;

    @Column(name = "metric_type_id")
    private String metricTypeId;

    @Column(name = "metric_group_id")
    private String metricGroupId;

    @Column(name = "metric_collection_id")
    private String metricCollectionId;

    @Column(name = "metric_light_type_id")
    private String metricLightTypeId;

    @Column(name = "metric_unit")
    private String metricUnit;

    @Column(name = "metric_display_unit")
    private String metricDisplayUnit;

    @Column(name = "metric_query_expression")
    private String metricQueryExpression;

    @Column(name = "description")
    private String description;

}
