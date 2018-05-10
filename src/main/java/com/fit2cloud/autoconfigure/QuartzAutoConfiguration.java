package com.fit2cloud.autoconfigure;

import com.fit2cloud.autoconfigure.util.QuartzBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableConfigurationProperties(QuartzProperties.class)
@ConditionalOnBean(DataSource.class)
public class QuartzAutoConfiguration {
    private DataSource dataSource;

    private QuartzProperties properties;

    public QuartzAutoConfiguration(ObjectProvider<DataSource> dataSourceProvider, QuartzProperties properties) {
        this.dataSource = dataSourceProvider.getIfAvailable();
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "quartz", value = "enabled", havingValue = "true")
    public SchedulerStarter schedulerStarter() {
        return new SchedulerStarter();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "quartz", value = "enabled", havingValue = "true")
    public QuartzBeanFactory quartzBeanFactory() {
        return new QuartzBeanFactory();
    }


    @Bean
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnProperty(prefix = "quartz", value = "enabled", havingValue = "true")
    public SchedulerFactoryBean clusterSchedulerFactoryBean() {
        SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean();
        schedulerFactoryBean.setDataSource(this.dataSource);
        schedulerFactoryBean.setApplicationContextSchedulerContextKey("applicationContextKey");
        schedulerFactoryBean.setOverwriteExistingJobs(true);
        schedulerFactoryBean.setStartupDelay(60);// 60 秒之后开始执行定时任务
        Properties props = new Properties();
        props.put("org.quartz.scheduler.instanceName", "clusterScheduler");
        props.put("org.quartz.scheduler.instanceId", "AUTO"); // 集群下的instanceId 必须唯一
        props.put("org.quartz.scheduler.instanceIdGenerator.class", QuartzInstanceIdGenerator.class.getName());// instanceId 生成的方式
        props.put("org.quartz.jobStore.class", "org.quartz.impl.jdbcjobstore.JobStoreTX");
        props.put("org.quartz.jobStore.driverDelegateClass", "org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
        props.put("org.quartz.jobStore.tablePrefix", "QRTZ_");
        props.put("org.quartz.jobStore.isClustered", "true");
        props.put("org.quartz.jobStore.clusterCheckinInterval", "20000");
        props.put("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
        props.put("org.quartz.threadPool.threadCount", "10");
        props.put("org.quartz.threadPool.threadPriority", "5");
        props.put("org.quartz.threadPool.threadsInheritContextClassLoaderOfInitializingThread", "true");
        schedulerFactoryBean.setQuartzProperties(props);
        if (!StringUtils.isEmpty(this.properties.getSchedulerName())) {
            schedulerFactoryBean.setBeanName(this.properties.getSchedulerName());
        }
        return schedulerFactoryBean;
    }
}
