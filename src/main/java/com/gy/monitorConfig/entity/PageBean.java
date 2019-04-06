package com.gy.monitorConfig.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Created by gy on 2019/1/29.
 */
@Getter
@Setter
public class PageBean<T> {

    //当前页  从1开始
    private int pageNum;

    //每页显示的数据条数
    private int pageSize;

    //总的记录条数
    private int totalRecord;

    //计算得来 一共多少页
    private int totalPage;

    //开始索引 从0开始
    private int startIndex;



    List<T> list;

    public PageBean(int pageNum, int pageSize, int totalRecord) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.totalRecord = totalRecord;
        if (totalRecord % pageSize == 0) {
            this.totalPage = totalRecord / pageSize;
        } else {
            this.totalPage = totalRecord / pageSize + 1;
        }
        this.startIndex = (pageNum-1)*pageSize;
    }
}
