package com.gy.monitorConfig.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Created by gy on 2018/10/14.
 */
@Getter
@Setter
public class RuleMonitorEntity {

    List<AlertAvlRuleMonitorEntity> avlRuleMonitorList;

    List<AlertPerfRuleMonitorEntity> perfRuleMonitorList;

    AlertRuleTemplateMonitorEntity templateMonitorEntity;
}
