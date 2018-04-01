package com.gy.monitorConfig.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;


/**
 * Created by gy on 2018/3/31.
 */
@Data
@Entity
@Table(name = "tbl_test")
public class TestEntity {


    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "name")
    private String name;
}
