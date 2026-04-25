package com.uni.uai.mcp.utils;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import com.uni.ubag.common.log.Logger;
import com.uni.ubag.common.log.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class YmlConfigUtil {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	private static YmlConfigUtil instance = new YmlConfigUtil();
	private static final Map<String, Properties> profileProperties = new HashMap<>();
	private static String activeProfile;
	private static Properties defaultProperties;

	public static YmlConfigUtil getInstance() {
		return instance;
	}

	static {
		try {
			// 加载默认配置
			defaultProperties = loadYamlProperties("application.yml");

			// 获取激活的profile
			activeProfile = System.getProperty("spring.profiles.active");
			if (!StringUtils.hasText(activeProfile)) {
				activeProfile = defaultProperties.getProperty("spring.profiles.active");
			}
			if (!StringUtils.hasText(activeProfile)) {
				activeProfile = "dev"; // 默认使用dev环境
			}

			// 加载对应profile的配置
			String profileConfig = String.format("application-%s.yml", activeProfile);
			Properties profileProps = loadYamlProperties(profileConfig);
			profileProperties.put(activeProfile, profileProps);

		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize YmlConfigUtil", e);
		}
	}

	private static Properties loadYamlProperties(String resourcePath) {
		YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
		Resource resource = new ClassPathResource(resourcePath);
		if (resource.exists()) {
			yaml.setResources(resource);
			return yaml.getObject();
		}
		return new Properties();
	}

	public String getYmlConfigValue(String key) {
		// 优先从profile特定配置中获取
		Properties profileProps = profileProperties.get(activeProfile);
		if (profileProps != null && profileProps.containsKey(key)) {
			return profileProps.getProperty(key);
		}

		// 如果profile中没有，从默认配置中获取
		if (defaultProperties != null && defaultProperties.containsKey(key)) {
			return defaultProperties.getProperty(key);
		}

		return null;
	}

	public String getCurrentProfile() {
		return activeProfile;
	}
}
