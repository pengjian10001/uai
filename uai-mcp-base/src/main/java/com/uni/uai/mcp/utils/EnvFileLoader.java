package com.uni.uai.mcp.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import com.uni.ubag.common.log.Logger;
import com.uni.ubag.common.log.LoggerFactory;

/**
 * 本地 .env 文件加载器。
 * <p>
 * Java 进程不会像 Node.js / Docker Compose 那样自动读取 .env。
 * 本类在 {@link com.uni.uai.mcp.llm.ChatModelFactory} 初始化前被调用，
 * 将 .env 中的 {@code UAI_LLM_*} 变量写入 System Property（{@code uai.llm.*}），
 * 供 {@code ChatModelFactory.resolveConfig()} 统一解析。
 * </p>
 * <p>
 * 不覆盖已存在的 OS 环境变量；也不覆盖已设置的 JVM System Property。
 * </p>
 */
public class EnvFileLoader {
	private static final Logger logger = LoggerFactory.getLogger(EnvFileLoader.class);

	/** .env 中的环境变量名 → ChatModelFactory 使用的 JVM 属性名 */
	private static final Map<String, String> ENV_TO_PROPERTY = Map.of(
		"UAI_LLM_API_KEY", "uai.llm.api-key",
		"UAI_LLM_BASE_URL", "uai.llm.base-url",
		"UAI_LLM_MODEL_NAME", "uai.llm.model-name"
	);

	private static volatile boolean loaded;

	/**
	 * 若存在 .env 则加载一次（幂等，线程安全）。
	 * 通常在 ChatModelFactory 静态块最开始调用。
	 */
	public static void loadIfPresent() {
		if (loaded) {
			return;
		}
		synchronized (EnvFileLoader.class) {
			if (loaded) {
				return;
			}
			Path envFile = findEnvFile();
			if (envFile == null) {
				loaded = true;
				return;
			}
			try {
				load(envFile);
				logger.info("Loaded env file: " + envFile.toAbsolutePath());
			} catch (IOException e) {
				logger.warn("Failed to load env file: " + envFile.toAbsolutePath(), e);
			}
			loaded = true;
		}
	}

	/**
	 * 从 user.dir 开始向上查找 .env，最多向上 6 层。
	 * 兼容在子模块目录（如 uai-mcp-server）下执行 mvn jetty:run 的场景。
	 */
	private static Path findEnvFile() {
		Path dir = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
		for (int i = 0; i < 6 && dir != null; i++) {
			Path candidate = dir.resolve(".env");
			if (Files.isRegularFile(candidate)) {
				return candidate;
			}
			dir = dir.getParent();
		}
		return null;
	}

	/** 逐行解析 KEY=VALUE，跳过空行与 # 注释 */
	private static void load(Path envFile) throws IOException {
		List<String> lines = Files.readAllLines(envFile, StandardCharsets.UTF_8);
		for (String rawLine : lines) {
			String line = rawLine.trim();
			if (line.isEmpty() || line.startsWith("#")) {
				continue;
			}
			int idx = line.indexOf('=');
			if (idx <= 0) {
				continue;
			}
			String key = line.substring(0, idx).trim();
			String value = stripQuotes(line.substring(idx + 1).trim());
			if (value.isEmpty()) {
				continue;
			}
			apply(key, value);
		}
	}

	/**
	 * 将 .env 条目写入 System Property。
	 * OS 环境变量优先级更高：若 {@code System.getenv(key)} 已有值则跳过。
	 */
	private static void apply(String key, String value) {
		if (System.getenv(key) != null && !System.getenv(key).trim().isEmpty()) {
			return;
		}
		String propertyKey = ENV_TO_PROPERTY.getOrDefault(key, toPropertyKey(key));
		if (System.getProperty(propertyKey) == null || System.getProperty(propertyKey).trim().isEmpty()) {
			System.setProperty(propertyKey, value);
		}
	}

	/** 未在 ENV_TO_PROPERTY 中声明的 key，按小写+下划线转点号规则映射 */
	private static String toPropertyKey(String envKey) {
		return envKey.toLowerCase().replace('_', '.');
	}

	/** 去掉值两侧的单引号或双引号 */
	private static String stripQuotes(String value) {
		if (value.length() >= 2) {
			char first = value.charAt(0);
			char last = value.charAt(value.length() - 1);
			if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
				return value.substring(1, value.length() - 1);
			}
		}
		return value;
	}
}
