package com.gy.monitorConfig.util;

/**
 * Created by gy on 2018/12/16.
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestTemplate;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ProperUtil {

    public static final String CONF_FILE_NAME = "application.properties";
    public static final String DB_USER_NAME = null;
    public static final String DB_PASSWORD = null;
    public static final String DB_URL = null;
    private static final String IP="47.94.157.199";

    //    private static final String IP="172.31.105.232";
    private static final String ETCD_PORT="2379";
    private static final String PATH_SERVICE_INFO="v2/keys/registry/services/specs/default/";
    private static final String PATH_RESOURCE_MONITOR="prometheus/resource_monitor";
    private static final String HTTP="http://";


    private ProperUtil() {
    }

    private static class ProperiesUtil {
        private static final ProperUtil INSTANCE = new ProperUtil();
    }

    public static ProperUtil getInstance() {
        return ProperiesUtil.INSTANCE;
    }

    static Properties prop;

    static {
        prop = new Properties();
    }

    public Properties getProperties() {
        return prop;
    }

    public void GetProperties() {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(ProperUtil.CONF_FILE_NAME);
        try {
            prop.load(in);

        } catch (Exception e) {
            e.getMessage();
        } finally {
            try {
                in.close();
            } catch (Exception e) {
                e.getMessage();
            }
        }

    }


    public static void SetConfInfo() {

        FileOutputStream fos = null;
        Map<String, String> confMaps = null;


        prop.clear();
        try {
            fos = new FileOutputStream(CONF_FILE_NAME, true);
            confMaps = new HashMap<String, String>();

            String dbUserName = System.getenv("DB_USERNAME");
            if (!"".equals(dbUserName) && null != dbUserName) {
                confMaps.put("spring.datasource.username", dbUserName);
            }

            String dbPassword = System.getenv("DB_PASSWORD");
            if (!"".equals(dbPassword) && null != dbPassword) {
                confMaps.put("spring.datasource.password", dbPassword);
            }

            String dbEndpoint =System.getenv("DB_ENDPOINT");

            String []str =dbEndpoint.split(":");
            String dbName =System.getenv("DATABASE_NAME");

//            String ip = getClusterIpByServiceName(str[0]);
//            System.out.println(ip);
            String dbUrl = "jdbc:mysql://"+dbEndpoint+"/"+dbName+"?characterEncoding=utf8&useSSL=false";
//
//
//            String dbUrl = "jdbc:mysql://"+ip+":"+str[1]+"/"+dbName+"?characterEncoding=utf8&useSSL=false";
            if (!"".equals(dbUrl) && null != dbUrl) {
                confMaps.put("spring.datasource.url", dbUrl);
            }
            confMaps.put("spring.datasource.driver-class-name","com.mysql.jdbc.Driver");

            prop.putAll(confMaps);
            prop.store(fos, CONF_FILE_NAME);


        } catch (IOException e) {
            System.out.println("Write DataBase driver config file faild." + e.getMessage());
        } catch (Exception e) {
            System.out.println("Loading DataBase driver " + CONF_FILE_NAME + " error." + e.getMessage());
        } finally {
            if (null != fos) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String getClusterIpByServiceName(String serviceName) throws IOException {
        String prefix = HTTP+IP + ":" + ETCD_PORT + "/";
        ObjectMapper objectMapper = new ObjectMapper();
        RestTemplate rest = new RestTemplate();
        String response = rest.getForObject(prefix+PATH_SERVICE_INFO+serviceName,String.class);
        Map<String,Object> resmap = objectMapper.readValue(response,HashMap.class);
        Map<String,String> nodeMap = (Map<String, String>) resmap.get("node");
        String cont =  nodeMap.get("value");
        Map<String,Object> contMap = objectMapper.readValue(cont,HashMap.class);
        Map<String,String> specMap = (Map<String, String>) contMap.get("spec");
        String clusterIP = specMap.get("clusterIP");
        return clusterIP;
    }
}

