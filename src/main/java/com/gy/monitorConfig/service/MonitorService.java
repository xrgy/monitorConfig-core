package com.gy.monitorConfig.service;

import com.gy.monitorConfig.entity.monitor.*;

import java.util.List;

/**
 * Created by gy on 2018/5/5.
 */
public interface MonitorService {


    /**
     * 根据监控模板id获取网络设备监控实体
     * @param uuid
     * @return
     */
    List<NetworkMonitorEntity> getNetworkMonitorRecordByTemplateId(String uuid);

    /**
     * 根据监控模板id获取网络设备监控实体
     * @param uuid
     * @return
     */
    List<TomcatMonitorEntity> getTomcatRecordByTemplateId(String uuid);

    /**
     * 根据监控模板id获取网络设备监控实体
     * @param uuid
     * @return
     */
    List<DBMonitorEntity> getDbMonitorRecordByTemplateId(String uuid);

    /**
     * 根据监控模板id获取网络设备监控实体
     * @param uuid
     * @return
     */
    List<CasMonitorEntity> getCasMonitorRecordByTemplateId(String uuid);

    /**
     * 根据监控模板id获取网络设备监控实体
     * @param uuid
     * @return
     */
    List<HostMonitorEntity> getHostMonitorRecordByTemplateId(String uuid);

    /**
     * 根据监控模板id获取网络设备监控实体
     * @param uuid
     * @return
     */
    List<VmMonitorEntity> getVmMonitorRecordByTemplateId(String uuid);

    /**
     * 根据监控模板id获取网络设备监控实体
     * @param uuid
     * @return
     */
    List<K8sMonitorEntity> getK8sMonitorRecordByTemplateId(String uuid);

    /**
     * 根据监控模板id获取网络设备监控实体
     * @param uuid
     * @return
     */
    List<K8snodeMonitorEntity> getK8snodeMonitorRecordByTemplateId(String uuid);

    /**
     * 根据监控模板id获取网络设备监控实体
     * @param uuid
     * @return
     */
    List<K8scontainerMonitorEntity> getK8scontainerMonitorRecordByTemplateId(String uuid);

}
