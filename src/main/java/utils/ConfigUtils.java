package utils;

import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

public class ConfigUtils {
	private static Logger log = Logger.getLogger(ConfigUtils.class);

	private static Properties config = new Properties();

	private static String ESPN_HOME;

	static {
		try {
			loadProperties();
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
			System.exit(99);
		}
	}

	public static void loadProperties() throws Exception {

		try (InputStream in = ConfigUtils.class.getResourceAsStream("/wbbScreenScraper.properties");) {
			config.load(in);
			config.forEach((k, v) -> log.info(k + " -> " + v));
		} catch (Exception e) {
			throw e;
		}
	}

	/**
	 * @return the config
	 */
	public static Properties getConfig() {
		return config;
	}

	public static String getProperty(String key) throws Exception {
		String value = null;
		try {
			if (key == null || key.trim().length() == 0) {
				throw new Exception("Unknown property key");
			}
			value = config.getProperty(key);
			if (value == null) {
				throw new Exception("Unknown property value for key: " + key);
			}
		} catch (Exception e) {
			throw e;
		}
		return value;
	}

	public static int getPropertyAsInt(String key) throws Exception {
		try {
			String value = getProperty(key);
			return Integer.valueOf(value).intValue();
		} catch (Exception e) {
			throw e;
		}
	}

//	public static String getBASE_OUTPUT_PATH() {
//		return BASE_OUTPUT_PATH;
//	}

	public static String getESPN_HOME() throws Exception {
		try {
			return getProperty("espn.home");
		} catch (Exception e) {
			throw e;
		}
		// return ESPN_HOME;
	}

}
