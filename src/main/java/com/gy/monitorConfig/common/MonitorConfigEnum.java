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

}
