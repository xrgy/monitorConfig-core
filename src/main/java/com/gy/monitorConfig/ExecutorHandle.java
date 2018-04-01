package com.gy.monitorConfig;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

/**
 * Created by gy on 2018/4/1.
 */
@Configuration
public class ExecutorHandle {

    @Bean
    @Qualifier("database")
    public Executor dataBaseExecutor(){
        return new ThreadPoolExecutor(2,5,1, TimeUnit.MINUTES,
                new LinkedBlockingQueue());
    }
}
