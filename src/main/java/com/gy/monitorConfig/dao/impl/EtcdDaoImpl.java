package com.gy.monitorConfig.dao.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gy.monitorConfig.dao.EtcdDao;
import com.gy.monitorConfig.entity.etcd.RuleGroups;
import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.requests.EtcdKeyPutRequest;
import mousio.etcd4j.responses.EtcdAuthenticationException;
import mousio.etcd4j.responses.EtcdException;
import mousio.etcd4j.responses.EtcdResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeoutException;

/**
 * Created by gy on 2018/10/23.
 */
@Repository
public class EtcdDaoImpl implements EtcdDao {

    //    private static final String IP="47.105.64.176";
    private static final String IP = "47.94.157.199";

    //    private static final String IP="172.31.105.232";
    private static final String ETCD_PORT = "2379";
    private static final String ETCD_PREFIX = "v2/keys/gy";

    private static final String ALERT_ETCD = "gy/alert";
    private static final String HTTP = "http://";


    private String etcdPrefix() {
        return HTTP + IP + ":" + ETCD_PORT + "/" + ETCD_PREFIX + "/";
    }

    @Bean
    public RestTemplate rest() {
        return new RestTemplate();
    }

    @Autowired
    ObjectMapper objectMapper;

    @Override
    public boolean insertEtcdAlert(String uuid, RuleGroups groups) throws IOException, EtcdAuthenticationException, TimeoutException, EtcdException {
//        RestTemplate rest = new RestTemplate();
//        PoolingHttpClientConnectionManager pollingConnectionManager = new PoolingHttpClientConnectionManager(30, TimeUnit.SECONDS);
//
////最大连接数
//
//        pollingConnectionManager.setMaxTotal(1000);
//
////单路由的并发数
//
//        pollingConnectionManager.setDefaultMaxPerRoute(1000);
//
//        HttpClientBuilder httpClientBuilder = HttpClients.custom();
//
//        httpClientBuilder.setConnectionManager(pollingConnectionManager);
//
//// 重试次数2次，并开启
//
//        httpClientBuilder.setRetryHandler(new DefaultHttpRequestRetryHandler(2, true));
//
//// 保持长连接配置，需要在头添加Keep-Alive
//
//        httpClientBuilder.setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy());
//
//        HttpClient httpClient = httpClientBuilder.build();
//
//// httpClient连接底层配置clientHttpRequestFactory
//
//        HttpComponentsClientHttpRequestFactory clientHttpRequestFactory =
//
//                new HttpComponentsClientHttpRequestFactory(httpClient);
//
//// 连接超时时长配置
//
//        clientHttpRequestFactory.setConnectTimeout(50000);
//
//// 数据读取超时时长配置
//
//        clientHttpRequestFactory.setReadTimeout(50000);
//
//// 连接不够用的等待时间，不宜过长，必须设置，比如连接不够用时，时间过长将是灾难性的
//
//        clientHttpRequestFactory.setConnectionRequestTimeout(200);
//
//// 缓冲请求数据，默认值是true。通过POST或者PUT大量发送数据时，建议将此属性更改为false，以免耗尽内存。
//
//        clientHttpRequestFactory.setBufferRequestBody(false);
//        rest.setRequestFactory(clientHttpRequestFactory);
//        rest.setErrorHandler(new DefaultResponseErrorHandler());
//        rest.postForObject(etcdPrefix() + ALERT_ETCD + "/{1}", objectMapper.writeValueAsString(groups),String.class, uuid);
//

        EtcdClient client = new EtcdClient(URI.create(HTTP+IP+":"+ETCD_PORT));
        EtcdKeyPutRequest request = client.put(ALERT_ETCD+"/"+uuid,objectMapper.writeValueAsString(groups));
        EtcdResponse response = request.send().get();
             return true;
    }

    @Override
    public boolean delEtcdAlert(String uuid) {
        rest().delete(etcdPrefix() + ALERT_ETCD + "/{1}", uuid);
        return false;
    }

}
