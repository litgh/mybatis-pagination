package com.github.litgh.mybatis.domain;

import java.io.Serializable;

public class Order implements Serializable {

    private static final long serialVersionUID = 1280480348165822156L;
    private String field;
    private String order = "ASC";

    public Order(String field) {
        this.field = field;
    }

    public Order desc() {
        this.order = "DESC";
        return this;
    }

    public String toString() {
        return field + " " + order;
    }

}
