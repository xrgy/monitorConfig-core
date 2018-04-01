package com.gy.monitorConfig.base;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.io.Serializable;

/**
 * Created by gy on 2018/4/1.
 */
@NoRepositoryBean
public interface BaseRepository <T, ID extends Serializable>
        extends PagingAndSortingRepository<T, ID> {

    boolean support(String modelType);
}
