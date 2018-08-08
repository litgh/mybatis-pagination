package com.github.litgh.mybatis.domain;

import org.apache.ibatis.session.RowBounds;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class PageBounds extends RowBounds implements Serializable {
    private static final long serialVersionUID = 9038196263661426097L;

    private int         page;
    private long        total;
    private List<Order> orders;
    private boolean count = true;

    public PageBounds() {
    }

    public PageBounds(int page) {
        this(page, 25);
    }

    public PageBounds(Integer page, Integer limit) {
        super((page == null ? 0 : page - 1) * (limit == null ? 10 : limit), limit == null ? 10 : limit);
        this.page = page == null ? 1 : page;
    }

    public PageBounds(RowBounds rowBounds) {
        super(rowBounds.getOffset(), rowBounds.getLimit());
        this.page = rowBounds.getOffset() / rowBounds.getLimit() + 1;
    }

    public static PageBounds order(Order... orders) {
        PageBounds pageBounds = new PageBounds(0);
        pageBounds.setOrders(Arrays.asList(orders));
        return pageBounds;
    }

    public static PageBounds page(int page, int limit) {
        return new PageBounds(page, limit);
    }

    public static PageBounds page(Integer page) {
        return new PageBounds(page, 10);
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public List<Order> getOrders() {
        return orders;
    }

    public void setOrders(List<Order> orders) {
        this.orders = orders;
    }

    @Override
    public int getOffset() {
        if (page >= 1) {
            return (page - 1) * getLimit();
        }
        return 0;
    }

    public PageBounds count(boolean count) {
        this.count = count;
        return this;
    }

    public boolean count() {
        return count;
    }
}
