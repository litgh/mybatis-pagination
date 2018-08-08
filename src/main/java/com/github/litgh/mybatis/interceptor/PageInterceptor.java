package com.github.litgh.mybatis.interceptor;


import com.github.litgh.mybatis.dialect.Dialect;
import com.github.litgh.mybatis.dialect.MysqlDialect;
import com.github.litgh.mybatis.domain.PageBounds;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.jdbc.ConnectionLogger;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;

/**
 * mybatis 分页插件
 *
 * @author lizheng
 */
@Intercepts({@Signature(method = "query", type = Executor.class, args = {
        MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})})
public class PageInterceptor implements Interceptor {
    /**
     * 数据库类型, 目前仅支持mysql
     */
    private String databaseType;

    @Override
    @SuppressWarnings("unchecked")
    public Object intercept(Invocation invocation) throws Throwable {
        final Executor        executor  = (Executor) invocation.getTarget();
        final Object[]        args      = invocation.getArgs();
        final MappedStatement ms        = (MappedStatement) args[0];
        final Object          parameter = args[1];
        final RowBounds       rowBounds = (RowBounds) args[2];
        final PageBounds pageBounds = rowBounds instanceof PageBounds ? (PageBounds) rowBounds
                : new PageBounds(rowBounds);

        if (pageBounds.getLimit() == RowBounds.NO_ROW_LIMIT) {
            return invocation.proceed();
        }

        Dialect dialect = new MysqlDialect(ms, parameter, pageBounds);
        args[0] = dialect.getMappedStatement();
        args[1] = dialect.getParameterObject();
        args[2] = RowBounds.DEFAULT;

        // 是否需要count结果数
        if (!pageBounds.count()) {
            return invocation.proceed();
        }

        Cache    cache    = ms.getCache();
        BoundSql countSql = dialect.getCountBoundSql();
        if (cache != null && ms.isUseCache() && ms.getConfiguration().isCacheEnabled()) {
            CacheKey cacheKey = executor.createCacheKey(ms, parameter, new PageBounds(), countSql);
            Long     count    = (Long) cache.getObject(cacheKey);
            if (count == null) {
                count = getCount(executor, ms, countSql);
                cache.putObject(cacheKey, count);
                pageBounds.setTotal(count);
            }
        } else {
            pageBounds.setTotal(getCount(executor, ms, countSql));
        }
        if (pageBounds.getTotal() == 0 || pageBounds.getLimit() == 0) {
            return new ArrayList(0);
        }

        return invocation.proceed();
    }

    private Long getCount(Executor executor, MappedStatement ms, BoundSql countSql) throws SQLException {
        Log        log  = ms.getStatementLog();
        Connection conn = executor.getTransaction().getConnection();
        if (log.isDebugEnabled()) {
            conn = ConnectionLogger.newInstance(conn, log, 1);
        }
        PreparedStatement       countStmt = conn.prepareStatement(countSql.getSql());
        DefaultParameterHandler handler   = new DefaultParameterHandler(ms, countSql.getParameterObject(), countSql);
        handler.setParameters(countStmt);

        ResultSet rs = null;
        try {
            rs = countStmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ignore) {
                }
            }
            if (countStmt != null) {
                try {
                    countStmt.close();
                } catch (SQLException ignore) {
                }
            }
        }
        return 0L;
    }

    @Override
    public Object plugin(Object o) {
        return Plugin.wrap(o, this);
    }

    @Override
    public void setProperties(Properties properties) {
        this.databaseType = properties.getProperty("databaseType", "mysql");
    }
}
