package com.gy.monitorConfig.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by gy on 2019/1/29.
 */
@Component
public class EntityAndDTO {
    /**
     * 泛型的转换接口需要传入需要被转换的对象和转换后的对象的Class
     *
     * @param t
     * @param eClass
     * @param <E>
     * @param <T>
     * @return E
     */
    public static <E, T> E convert(T t, Class<E> eClass) {
        E e = null;
        try {
            e = eClass.newInstance();
            BeanUtils.copyProperties(t, e);

        } catch (InstantiationException e1) {
            e1.printStackTrace();
        } catch (IllegalAccessException e1) {
            e1.printStackTrace();
        }
        return e;
    }

    public static <E, T> List<E> convert(List<T> tList, Class<E> eClass) {

        List<E> eList = new ArrayList<E>();
        for (T t : tList) {
            eList.add(convert(t, eClass));
        }
        return eList;
    }

    /**
     * 将map结果集转换为POJO对象
     * @param tList source
     * @param eClass class
     * @param <E> e
     * @param <T> t
     * @return t
     */
    public static <E, T> List<E> mapConvertToBean(List<T> tList, Class<E> eClass) {
        List<E> eList = new ArrayList<E>();
        try {
            for (T t : tList) {
                Map<String, Object> map = (Map<String, Object>) t;
                eList.add((E) mapConvertToObject(map, eClass));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return eList;
    }

    /**
     * 将map转换为POJO对象
     * @param map map
     * @param beanClass class
     * @return r
     */
    public static Object mapConvertToObject(Map<String, Object> map, Class beanClass) {
        ObjectMapper mapper = new ObjectMapper();
        Object pojo = mapper.convertValue(map, beanClass);
        return pojo;
    }

}
