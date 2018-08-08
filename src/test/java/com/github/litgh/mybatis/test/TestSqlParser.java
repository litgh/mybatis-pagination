package com.github.litgh.mybatis.test;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.statement.SQLSelect;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlSelectParser;
import com.alibaba.druid.sql.visitor.SQLASTOutputVisitor;
import com.alibaba.druid.util.JdbcConstants;
import org.junit.Test;

public class TestSqlParser {

    @Test
    public void testParser() {
        String              sql                 = "select id from a,b where a.id=b.id and a.id=1 group by id having count(1) > 0 order by id";
        MySqlSelectParser   sqlSelectParser     = new MySqlSelectParser(sql);
        SQLSelect           sqlSelect           = sqlSelectParser.select();
        SQLSelectQueryBlock sqlSelectQueryBlock = (SQLSelectQueryBlock) sqlSelect.getQuery();
        StringBuilder       buf                 = new StringBuilder("SELECT ");

        SQLASTOutputVisitor visitor = SQLUtils.createFormatOutputVisitor(buf, null, JdbcConstants.MYSQL);
        for (int i = 0, l = sqlSelectQueryBlock.getSelectList().size(); i < l; i++) {
            sqlSelectQueryBlock.getSelectList().get(i).accept(visitor);
            if (i > 0) {
                buf.append(", ");
            }
        }
        buf.append(" FROM ");
        sqlSelectQueryBlock.getFrom().accept(visitor);

        buf.append(" WHERE ");
        sqlSelectQueryBlock.getWhere().accept(visitor);
        if (sqlSelectQueryBlock.getGroupBy() != null) {
            buf.append(" ");
            sqlSelectQueryBlock.getGroupBy().accept(visitor);
        }
        if (sqlSelectQueryBlock.getOrderBy() != null) {
            buf.append(" ");
            sqlSelectQueryBlock.getOrderBy().addItem(SQLUtils.toOrderByItem("name", JdbcConstants.MYSQL));
            sqlSelectQueryBlock.getOrderBy().accept(visitor);
        }
        System.out.println(buf);
    }

}
