package com.github.litgh.mybatis.dialect;

import com.alibaba.druid.sql.PagerUtils;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlSelectParser;
import com.alibaba.druid.sql.visitor.SQLASTOutputVisitor;
import com.alibaba.druid.util.JdbcConstants;
import com.github.litgh.mybatis.domain.Order;
import com.github.litgh.mybatis.domain.PageBounds;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.RowBounds;

/**
 * @author lizheng
 */
public class MysqlDialect extends Dialect {
    public MysqlDialect(MappedStatement mappedStatement, Object parameter, PageBounds pageBounds) {
        super(mappedStatement, parameter, pageBounds);
    }

    @Override
    public MappedStatement getMappedStatement() {
        if (pageBounds.getOrders() != null && !pageBounds.getOrders().isEmpty()) {
            orderBy();
        }

        if (pageBounds.getLimit() != RowBounds.NO_ROW_LIMIT || pageBounds.getOffset() != RowBounds.NO_ROW_OFFSET) {
            StringBuilder buffer = new StringBuilder(this.pageSql);
            if (pageBounds.getOffset() > 0) {
                buffer.append(" LIMIT ?, ?");
                setPageParameter("__offset", pageBounds.getOffset(), Integer.class);
                setPageParameter("__limit", pageBounds.getLimit(), Integer.class);
            } else {
                buffer.append(" LIMIT ?");
                setPageParameter("__limit", pageBounds.getLimit(), Integer.class);
            }
            this.pageSql = buffer.toString();
        }

        SqlSource sqlSource = new BoundSqlSqlSource(new BoundSql(mappedStatement.getConfiguration(), this.pageSql,
                parameterMappings, pageParameters));

        MappedStatement.Builder builder = new MappedStatement.Builder(mappedStatement.getConfiguration(),
                mappedStatement.getId(), sqlSource, mappedStatement.getSqlCommandType());
        builder.resource(mappedStatement.getResource())
                .cache(mappedStatement.getCache())
                .timeout(mappedStatement.getTimeout())
                .useCache(mappedStatement.isUseCache())
                .fetchSize(mappedStatement.getFetchSize())
                .resultMaps(mappedStatement.getResultMaps())
                .parameterMap(mappedStatement.getParameterMap())
                .keyGenerator(mappedStatement.getKeyGenerator())
                .statementType(mappedStatement.getStatementType())
                .resultSetType(mappedStatement.getResultSetType())
                .flushCacheRequired(mappedStatement.isFlushCacheRequired());

        if (mappedStatement.getKeyProperties() != null && mappedStatement.getKeyProperties().length != 0) {
            StringBuilder keyProperties = new StringBuilder();
            for (int i = 0; i < mappedStatement.getKeyProperties().length; i++) {
                if (i > 0) {
                    keyProperties.append(",");
                }
                keyProperties.append(mappedStatement.getKeyProperties()[i]);
            }
            builder.keyProperty(keyProperties.toString());
        }

        return builder.build();
    }

    @Override
    public BoundSql getCountBoundSql() {
        String countSql = PagerUtils.count(boundSql.getSql(), JdbcConstants.MYSQL);
        return new BoundSql(mappedStatement.getConfiguration(), countSql, boundSql.getParameterMappings(), boundSql.getParameterObject());
    }

    public void orderBy() {
        MySqlSelectParser   sqlSelectParser     = new MySqlSelectParser(boundSql.getSql().trim());
        SQLSelectQueryBlock sqlSelectQueryBlock = (SQLSelectQueryBlock) sqlSelectParser.select().getQuery();
        StringBuilder       buf                 = new StringBuilder("SELECT ");

        SQLASTOutputVisitor visitor = SQLUtils.createFormatOutputVisitor(buf, null, JdbcConstants.MYSQL);
        for (int i = 0, l = sqlSelectQueryBlock.getSelectList().size(); i < l; i++) {
            if (i > 0) {
                buf.append(", ");
            }
            sqlSelectQueryBlock.getSelectList().get(i).accept(visitor);
        }
        buf.append(" FROM ");
        sqlSelectQueryBlock.getFrom().accept(visitor);
        if (sqlSelectQueryBlock.getWhere() != null) {
            buf.append(" WHERE ");
            sqlSelectQueryBlock.getWhere().accept(visitor);
        }

        if (sqlSelectQueryBlock.getGroupBy() != null) {
            buf.append(" ");
            sqlSelectQueryBlock.getGroupBy().accept(visitor);
        }

        if (sqlSelectQueryBlock.getOrderBy() != null) {
            buf.append(" ");
            for (Order order : pageBounds.getOrders()) {
                sqlSelectQueryBlock.getOrderBy().addItem(SQLUtils.toOrderByItem(order.toString(), JdbcConstants.MYSQL));
            }
            sqlSelectQueryBlock.getOrderBy().accept(visitor);
        } else {
            buf.append(" ORDER　BY ");
            for (int i = 0; i < pageBounds.getOrders().size(); i++) {
                if (i > 0) {
                    buf.append(", ");
                }
                buf.append(pageBounds.getOrders().get(i).toString());
            }
        }
        this.pageSql = buf.toString();
    }

    public static class BoundSqlSqlSource implements SqlSource {
        BoundSql boundSql;

        public BoundSqlSqlSource(BoundSql boundSql) {
            this.boundSql = boundSql;
        }

        @Override
        public BoundSql getBoundSql(Object parameterObject) {
            return boundSql;
        }
    }

}
