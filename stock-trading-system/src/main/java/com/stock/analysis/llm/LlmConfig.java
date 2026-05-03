package com.stock.analysis.llm;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 加载 LLM 配置（API Key / Model 等），从 classpath:config.properties 读取。
 */
public class LlmConfig {

    private static final String CONFIG_FILE = "config.properties";
    private static final Properties props = new Properties();

    static {
        try (InputStream in = LlmConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException ignored) {
        }
    }

    public static String getApiKey() {
        String key = System.getenv("ANTHROPIC_API_KEY");
        if (key != null && !key.isBlank()) {
            return key;
        }
        return props.getProperty("anthropic.api.key", "");
    }

    public static String getModel() {
        return props.getProperty("anthropic.model", "claude-haiku-4-5-20251001");
    }

    public static boolean isConfigured() {
        return !getApiKey().isBlank();
    }
}
