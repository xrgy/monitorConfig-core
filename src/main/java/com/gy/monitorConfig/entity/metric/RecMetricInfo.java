package com.gy.monitorConfig.entity.metric;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Created by gy on 2018/6/9.
 */
@Getter
@Setter
public class RecMetricInfo {

    private String type;

    private List<MetricInfo> data;
}
