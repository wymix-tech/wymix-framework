package com.swcares.sample.configuration;

import com.travelsky.base.core.configuration.AbstractAsyncScheduleConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;

/*
启用异步
相关配置见 application-async.yml
 */
@Configuration
public class AsyncConfiguration extends AbstractAsyncScheduleConfiguration {

    /*
    任务装饰器，提交给线程池前，可以设置一些线程变量，如将HttpServletRequest中的信息设置进去，以供实际Runnable使用
     */
    @Override
    public TaskDecorator taskDecorator() {
        return null;
    }
}
