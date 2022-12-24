package utils;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

public class StringUtils {

	private static List<String> SPECIAL_CHARS_LIST = Arrays.asList(" ,./-&^%$#@!()_?*'\"".split(""));
	private static Logger log = Logger.getLogger(StringUtils.class);

	public static boolean isPopulated(String text) {
		if (text == null || text.trim().length() == 0) {
			return false;
		}
		return true;
	}

	public static String specialCharStripper(String in) {
		if (!isPopulated(in)) {
			return in;
		}

		// log.info(in);
		try {
			String stripped = new String(in);
			for (String c : SPECIAL_CHARS_LIST) {
				stripped = stripped.replace(c, "");
			}
			// log.info(stripped);
			return stripped;
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
			System.exit(99);
		}

		return in;
	}

	public static String inchesToCentimeters(String feet, String inches) {
		if (!isPopulated(feet) || !isPopulated(inches)) {
			return null;
		}

		try {
			Integer totalInches = (Integer.valueOf(feet) * 12) + Integer.valueOf(inches);
			return String.valueOf(Math.round((2.54) * totalInches));
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
			System.exit(99);
		}

		return null;
	}

}
