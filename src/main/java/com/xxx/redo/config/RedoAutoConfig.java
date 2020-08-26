package com.xxx.redo.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;

import javax.sql.DataSource;

/**
 * 自动配置类
 */
@Configuration
@EnableJdbcHttpSession
//@MapperScan(basePackages = {"com.xxx.redo.dao"}, sqlSessionTemplateRef = "redoSqlSessionTemplate")
@MapperScan(basePackages = {"com.xxx.redo.dao"})
public class RedoAutoConfig {


    @Bean(name = "redoDataSource")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DruidDataSource getRedoDataSource() {
        return new RedoDruidDataSource();
    }


    @Bean(name = "redoSqlSessionFactory")
    public SqlSessionFactory redoSqlSessionFactory(@Qualifier("redoDataSource") DataSource dataSource  )
            throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:mappers/**/*.xml"));
        bean.getObject().getConfiguration().setMapUnderscoreToCamelCase(true);
        bean.setTypeAliasesPackage("com.xxx.redo.model");
        return bean.getObject();
    }

    @Bean(name = "redoTransactionManager")
    public DataSourceTransactionManager redoTransactionManager(@Qualifier("redoDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean(name = "redoSqlSessionTemplate")
    public SqlSessionTemplate redoSqlSessionTemplate(
            @Qualifier("redoSqlSessionFactory") SqlSessionFactory sqlSessionFactory){
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
