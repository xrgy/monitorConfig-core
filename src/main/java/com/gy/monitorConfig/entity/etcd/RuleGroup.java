package com.gy.monitorConfig.entity.etcd;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Created by gy on 2018/11/15.
 */
@Getter
@Setter
public class RuleGroup {
    private String name;

    private List<Rule> rules;
}
