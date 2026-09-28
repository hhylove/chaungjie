package com.chuangjie.module.im.framework.web.config;

import com.chuangjie.framework.swagger.config.ChuangjieSwaggerAutoConfiguration;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * im 模块的 web 组件的 Configuration
 */
@Configuration(proxyBeanMethods = false)
public class ImWebConfiguration {

    /**
     * im 模块的 API 分组
     */
    @Bean
    public GroupedOpenApi imGroupedOpenApi() {
        return ChuangjieSwaggerAutoConfiguration.buildGroupedOpenApi("im");
    }

}
