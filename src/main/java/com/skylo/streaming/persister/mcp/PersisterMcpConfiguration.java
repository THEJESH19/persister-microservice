package com.skylo.streaming.persister.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Configuration registering MCP tools with Spring AI McpServer.
 */
@Configuration
public class PersisterMcpConfiguration {

    @Bean
    public ToolCallbackProvider persisterMcpToolCallbackProvider(PersisterMcpTools persisterMcpTools) {
        return () -> ToolCallbacks.from(persisterMcpTools);
    }
}
