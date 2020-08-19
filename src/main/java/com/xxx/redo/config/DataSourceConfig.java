package com.xxx.redo.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * sys数据源
 */
@Configuration
@MapperScan(basePackages = {"com.xxx.redo.dao"}, sqlSessionTemplateRef = "sqlSessionTemplate")
public class DataSourceConfig {



	@Bean(name = "dataSource")
	@ConfigurationProperties(prefix = "spring.datasource")
	public DruidDataSource getDataSource() {
		return new RedoDruidDataSource();
	}


	@Bean(name = "sqlSessionFactory")
	public SqlSessionFactory sysSqlSessionFactory(@Qualifier("dataSource") DataSource dataSource  )
			throws Exception {
		SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
		bean.setDataSource(dataSource);
		bean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:mappers/**/*.xml"));
		bean.getObject().getConfiguration().setMapUnderscoreToCamelCase(true);
		bean.setTypeAliasesPackage("com.xxx.redo.model");
		return bean.getObject();
	}

	@Bean(name = "transactionManager")
	public DataSourceTransactionManager sysTransactionManager(@Qualifier("dataSource") DataSource dataSource) {
		return new DataSourceTransactionManager(dataSource);
	}

	@Bean(name = "sqlSessionTemplate")
	public SqlSessionTemplate sysSqlSessionTemplate(
			@Qualifier("sqlSessionFactory") SqlSessionFactory sqlSessionFactory){
		return new SqlSessionTemplate(sqlSessionFactory);
	}

//	@ConditionalOnProperty(prefix = "spring.datasource.sys",name ="enabled" ,havingValue="true" )
//	@Bean(name = "sysSsoDataSource")
//	@ConfigurationProperties(prefix = "spring.datasource.sys")
//	public DataSource sysDataSource() {
//		return new SysDruidDataSource();
//	}
//
//	@Bean
//	@ConditionalOnBean(name = "sysSsoDataSource")
//	public JdbcTemplate springSessionJdbcOperations(@Qualifier("sysSsoDataSource") DataSource sysSsoDataSource) {
//		return new JdbcTemplate(sysSsoDataSource);
//	}
//
//	@Bean
//	@ConditionalOnBean(name = "sysSsoDataSource")
//	public PlatformTransactionManager transactionManager(@Qualifier("sysSsoDataSource") DataSource sysSsoDataSource) {
//		return new DataSourceTransactionManager(sysSsoDataSource);
//	}

}
