package com.chuangjie.module.trade.framework.web.config;

import com.chuangjie.framework.swagger.config.ChuangjieSwaggerAutoConfiguration;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * trade 模块的 web 组件的 Configuration
 *
 * @author hhy
 */
@Configuration(proxyBeanMethods = false)
public class TradeWebConfiguration {

    /**
     * trade 模块的 API 分组
     */
    @Bean
    public GroupedOpenApi tradeGroupedOpenApi() {
        return ChuangjieSwaggerAutoConfiguration.buildGroupedOpenApi("trade");
    }

}
