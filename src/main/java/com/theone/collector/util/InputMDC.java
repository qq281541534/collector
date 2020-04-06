package com.theone.collector.util;

import org.slf4j.MDC;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * MDC线程变量，这里用于日志中自定义的变量值替换
 *
 * @author liuyu
 */
@Component
public class InputMDC implements EnvironmentAware {

    private static Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        InputMDC.environment = environment;
    }

    public static void putMDC(){
        MDC.put("hostName", NetUtil.getLocalAddress().toString());
        MDC.put("ip", NetUtil.getLocalHost());
        MDC.put("applicationName", environment.getProperty("spring.application.name"));
    }
}
