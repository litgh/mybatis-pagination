package com.github.litgh.mybatis.dialect;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.statement.SQLSelect;
import com.alibaba.druid.sql.ast.statement.SQLSelectOrderByItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlSelectParser;
import com.alibaba.druid.sql.visitor.SQLASTOutputVisitor;
import com.alibaba.druid.util.JdbcConstants;
import com.github.litgh.mybatis.domain.Order;
import com.github.litgh.mybatis.domain.PageBounds;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.property.PropertyTokenizer;
import org.apache.ibatis.reflection.wrapper.ObjectWrapper;
import org.apache.ibatis.type.SimpleTypeRegistry;

import java.util.*;

public abstract class Dialect {
    protected MappedStatement        mappedStatement;
    protected Object                 parameter;
    protected PageBounds             pageBounds;
    protected List<ParameterMapping> parameterMappings;
    protected BoundSql               boundSql;
    protected Map<String, Object> pageParameters = new HashMap<String, Object>();

    protected String pageSql;

    @SuppressWarnings("unchecked")
    public Dialect(MappedStatement mappedStatement, Object parameter, PageBounds pageBounds) {
        this.mappedStatement = mappedStatement;
        this.parameter = parameter;
        this.pageBounds = pageBounds;

        boundSql = mappedStatement.getBoundSql(parameter);
        parameterMappings = new ArrayList(boundSql.getParameterMappings());
        if (parameter instanceof Map) {
            pageParameters.putAll((Map) parameter);
        } else if (parameter != null) {
            Class cls = parameter.getClass();
            if (cls.isPrimitive() || cls.isArray() ||
                    SimpleTypeRegistry.isSimpleType(cls) ||
                    Enum.class.isAssignableFrom(cls) ||
                    Collection.class.isAssignableFrom(cls)) {
                for (ParameterMapping parameterMapping : parameterMappings) {
                    pageParameters.put(parameterMapping.getProperty(), parameter);
                }
            } else {
                MetaObject    metaObject = mappedStatement.getConfiguration().newMetaObject(parameter);
                ObjectWrapper wrapper    = metaObject.getObjectWrapper();
                for (ParameterMapping parameterMapping : parameterMappings) {
                    PropertyTokenizer prop = new PropertyTokenizer(parameterMapping.getProperty());
                    pageParameters.put(parameterMapping.getProperty(), wrapper.get(prop));
                }
            }
        }

        this.pageSql = boundSql.getSql().trim();
    }

    public abstract MappedStatement getMappedStatement();

    public abstract BoundSql getCountBoundSql();

    protected void setPageParameter(String name, Object value, Class type) {
        ParameterMapping parameterMapping = new ParameterMapping.Builder(mappedStatement.getConfiguration(), name, type).build();
        parameterMappings.add(parameterMapping);
        pageParameters.put(name, value);
    }

    public Object getParameterObject() {
        return pageParameters;
    }
}
