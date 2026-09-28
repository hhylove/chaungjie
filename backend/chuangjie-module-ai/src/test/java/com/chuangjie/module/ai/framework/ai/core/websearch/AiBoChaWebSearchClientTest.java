package com.chuangjie.module.ai.framework.ai.core.websearch;

import com.chuangjie.framework.common.util.json.JsonUtils;
import com.chuangjie.module.ai.framework.ai.core.webserch.AiWebSearchRequest;
import com.chuangjie.module.ai.framework.ai.core.webserch.AiWebSearchResponse;
import com.chuangjie.module.ai.framework.ai.core.webserch.bocha.AiBoChaWebSearchClient;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * {@link AiBoChaWebSearchClient} 集成测试类
 *
 * @author hhy
 */
public class AiBoChaWebSearchClientTest {

    private final AiBoChaWebSearchClient webSearchClient = new AiBoChaWebSearchClient(
            System.getenv("AIBOCHA_API_KEY"));

    @Test
    @Disabled
    public void testSearch() {
        AiWebSearchRequest request = new AiWebSearchRequest()
                .setQuery("阿里巴巴")
                .setCount(3);
        AiWebSearchResponse response = webSearchClient.search(request);
        System.out.println(JsonUtils.toJsonPrettyString(response));
    }

}