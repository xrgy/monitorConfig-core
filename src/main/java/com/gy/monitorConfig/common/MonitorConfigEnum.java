package com.gy.monitorConfig.common;

/**
 * Created by gy on 2018/6/8.
 */
public interface MonitorConfigEnum {

    enum MetricGroupEnum{
        GROUP_AVAILABLE("available"),
        GROUP_PERFORMANCE("performance");

        private String value;

        MetricGroupEnum(String msg) {
            this.value = msg;
        }

        public String value() {
            return this.value;
        }
    }
    enum VelocityEnum{
        SERVERITY("severity"),
        MONITOR_ID("monitorId"),
        FIRST_CONDITION("firstCondition"),
        FIRST_THRESHOLD("firstThreshold"),
        EXPRESSION_MORE("expressionMore"),
        SECOND_CONDITION("secondCondition"),
        SECOND_THRESHOLD("secondThreshold"),
        RULE_NAME("rulename"),
        DESCRIPTION("description"),
        UNIT("unit"),
        EXPRESSION("expression"),
        CONDITION("condition");
        private String value;

        VelocityEnum(String msg) {
            this.value = msg;
        }

        public String value() {
            return this.value;
        }
    }



}
