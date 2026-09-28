package com.chuangjie.module.infra.framework.file.config;

import com.chuangjie.module.infra.framework.file.core.client.FileClientFactory;
import com.chuangjie.module.infra.framework.file.core.client.FileClientFactoryImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 文件配置类
 *
 * @author hhy
 */
@Configuration(proxyBeanMethods = false)
public class ChuangjieFileAutoConfiguration {

    @Bean
    public FileClientFactory fileClientFactory() {
        return new FileClientFactoryImpl();
    }

}
