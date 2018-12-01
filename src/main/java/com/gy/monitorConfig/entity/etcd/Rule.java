package com.gy.monitorConfig.entity.etcd;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Created by gy on 2018/11/15.
 */
@Getter
@Setter
public class Rule {
    private String alert;

    private String expr;

    private Map<String,String> labels;

    private Map<String,String> annotations;

}
