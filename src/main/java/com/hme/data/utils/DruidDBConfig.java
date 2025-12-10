package com.hme.data.utils;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author hme
 * @create 2024-02-27-17:51
 */
@Configuration
@EnableTransactionManagement
public class DruidDBConfig {
    // main datasource connect info
    @Value("${spring.datasource.url}")
    private String dbUrl;
    @Value("${spring.datasource.username}")
    private String username;
    @Value("${spring.datasource.password}")
    private String password;
    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;


    @Bean
    @Primary
    @Qualifier("mainDataSource")
    public DataSource dataSource() throws SQLException {
        DruidDataSource datasource = new DruidDataSource();
        // basic info
        datasource.setUrl(this.dbUrl);
        datasource.setUsername(username);
        datasource.setPassword(password);
        datasource.setDriverClassName(driverClassName);
        // connect pool
        datasource.setInitialSize(1);
        datasource.setMinIdle(5);
        datasource.setMaxActive(20);
        datasource.setMaxWait(60000);
        datasource.setConnectionErrorRetryAttempts(5);
        datasource.setPoolPreparedStatements(true); //是否缓存preparedStatement，也就是PSCache。PSCache对支持游标的数据库性能提升巨大，比如说oracle。在mysql下建议关闭。
        datasource.setMaxPoolPreparedStatementPerConnectionSize(20);
        datasource.setTestOnBorrow(true); //申请连接时执行validationQuery检测连接是否有效，这里建议配置为TRUE，防止取到的连接不可用
        datasource.setTestWhileIdle(true);//建议配置为true，不影响性能，并且保证安全性。申请连接的时候检测，如果空闲时间大于timeBetweenEvictionRunsMillis，执行validationQuery检测连接是否有效。
        String validationQuery = "select 1";
        datasource.setValidationQuery(validationQuery); //用来检测连接是否有效的sql，要求是一个查询语句。如果validationQuery为null，testOnBorrow、testOnReturn、testWhileIdle都不会起作用。
        datasource.setFilters("stat,wall");//属性类型是字符串，通过别名的方式配置扩展插件，常用的插件有：监控统计用的filter:stat日志用的filter:log4j防御sql注入的filter:wall
        datasource.setTimeBetweenEvictionRunsMillis(60000); //配置间隔多久才进行一次检测，检测需要关闭的空闲连接，单位是毫秒
        datasource.setMinEvictableIdleTimeMillis(180000); //配置一个连接在池中最小生存的时间，单位是毫秒，这里配置为3分钟180000
        datasource.setMaxEvictableIdleTimeMillis(25200000);
        datasource.setKeepAlive(true); //打开druid.keepAlive之后，当连接池空闲时，池中的minIdle数量以内的连接，空闲时间超过minEvictableIdleTimeMillis，则会执行keepAlive操作，即执行druid.validationQuery指定的查询SQL，一般为select * from dual，只要minEvictableIdleTimeMillis设置的小于防火墙切断连接时间，就可以保证当连接空闲时自动做保活检测，不会被防火墙切断
        datasource.setRemoveAbandoned(true); //是否移除泄露的连接/超过时间限制是否回收。
        datasource.setRemoveAbandonedTimeout(3600); //泄露连接的定义时间(要超过最大事务的处理时间)；单位为秒。这里配置为1小时
        datasource.setLogAbandoned(true);
        return datasource;
    }

    @Bean(name = "dynamicDataSource")
    @Qualifier("dynamicDataSource")
    public DynamicDataSource dynamicDataSource() throws SQLException {
        DynamicDataSource dynamicDataSource = new DynamicDataSource();
        // DefaultTargetDataSource
        dynamicDataSource.setDefaultTargetDataSource(dataSource());
        Map<Object, Object> targetDataSources = new HashMap<Object, Object>();
        //TargetDataSources
        targetDataSources.put("mainDataSource", dataSource());
        dynamicDataSource.setTargetDataSources(targetDataSources);
        return dynamicDataSource;
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        MybatisSqlSessionFactoryBean sqlSessionFactoryBean = new MybatisSqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dynamicDataSource());
        sqlSessionFactoryBean.setTypeAliasesPackage("com.knt.**.domain,com.knt.**.model,com.knt.**.entity");
        // setup mybatis info
        ResourcePatternResolver resolver = new
                PathMatchingResourcePatternResolver();
        sqlSessionFactoryBean.setMapperLocations(
                new PathMatchingResourcePatternResolver()
                        .getResources("classpath:mapper/*.xml"));
        Resource mybatisConfigXml = resolver.getResource("classpath:mybatis/mybatis-config.xml");
        sqlSessionFactoryBean.setConfigLocation(mybatisConfigXml);

        Interceptor[] plugins = {new MyBatisPlusConfig().mybatisPlusInterceptor()};

        sqlSessionFactoryBean.setPlugins(plugins);
        return sqlSessionFactoryBean.getObject();
    }
}
