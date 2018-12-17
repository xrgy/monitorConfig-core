package com.gy.monitorConfig;

import com.gy.monitorConfig.base.BaseRepositoryFactoryBean;
import com.gy.monitorConfig.util.ProperUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Created by gy on 2018/3/31.
 */
@SpringBootApplication
//@EnableAutoConfiguration
@EnableJpaRepositories(repositoryFactoryBeanClass = BaseRepositoryFactoryBean.class)
public class BingoApplication {
    public static void main(String[] args){
        ProperUtil.SetConfInfo();
        SpringApplication.run(BingoApplication.class,args);
    }
}
