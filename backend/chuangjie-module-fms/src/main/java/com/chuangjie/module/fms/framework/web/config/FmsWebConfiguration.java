package com.chuangjie.module.fms.framework.web.config;

import com.chuangjie.framework.swagger.config.ChuangjieSwaggerAutoConfiguration;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * fms 模块的 web 组件的 Configuration
 *
 * @author hhy
 */
@Configuration(proxyBeanMethods = false)
public class FmsWebConfiguration {

    /**
     * fms 模块的 API 分组
     *
     * @return fms 模块的 OpenAPI 分组
     */
    @Bean
    public GroupedOpenApi fmsGroupedOpenApi() {
        return ChuangjieSwaggerAutoConfiguration.buildGroupedOpenApi("fms");
    }

}
