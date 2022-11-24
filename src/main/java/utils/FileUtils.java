package utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;

import https.service.HttpsClientService;

public class FileUtils {

	private static Logger log = Logger.getLogger(FileUtils.class);

	public static String streamHttpsUrlConnection(HttpsURLConnection httpsUrlConnection, boolean debug) throws Exception {

		String returnText = "";

		try {

			try (InputStream inputStream = httpsUrlConnection.getInputStream()) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

				String line = null;
				while ((line = reader.readLine()) != null) {
					if (debug) {
						System.out.println(line);
					}
					returnText += line;
				}
			}

			HttpsClientService.closeHttpsURLConnection();
		} catch (Exception e) {
			throw e;
		}

		return returnText;
	}

	public static String writeMapToFile(String fileName, Map<String, String> map, boolean debug) throws Exception {

		String returnText = "";

		if (map == null || map.size() == 0) {
			log.warn("Map is empty");
			return returnText;
		}

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, false))) {
			map.entrySet().forEach(entry -> {
				try {
					String line = "[" + entry.getKey() + "]=" + entry.getValue();
					writer.append(line + "\n");

					if (debug) {
						log.info(line);
					}
				} catch (IOException e) {
					log.error(e.getMessage());
					e.printStackTrace();
					System.exit(99);
				}
			});
		}

		return returnText;
	}

	public static boolean createFileIfDoesNotExist(String fileName) throws Exception {
		try {
			if (!FileUtils.doesFileExist(fileName)) {
				File file = new File(fileName);
				if (file.createNewFile()) {
					log.info(fileName + " has been created");
					return true;
				}
			}
		} catch (Exception e) {
			throw e;
		}
		return false;
	}

	public static List<String> readFileLines(String fileName) throws Exception {
		try {
			return Files.readAllLines(Paths.get(fileName));
		} catch (Exception e) {
			throw e;
		}
	}

	public static void writeAllLines(String fileName, Set<String> lines, int numberOfExistingSkipDates, boolean debug) throws Exception {
		if (lines == null || lines.size() == 0) {
			log.warn("No lines to write");
			return;
		}

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) {
			lines.forEach(line -> {
				try {
					writer.append((numberOfExistingSkipDates == 0 ? line + "\n" : "\n" + line));
					if (debug) {
						log.info(line);
					}
				} catch (IOException e) {
					log.error(e.getMessage());
					e.printStackTrace();
					System.exit(99);
				}
			});
		}

	}

	public static boolean doesFileExist(String fileName) throws Exception {
		try {
			if (Files.exists(Paths.get(fileName))) {
				return true;
			}
		} catch (Exception e) {
			throw e;
		}
		return false;
	}

	public static Set<String> getFileListFromDirectory(String directory, String targetFileName) throws Exception {
		try {
			return Stream.of(new File(directory).listFiles())/**/
					.filter(f -> f.getName().startsWith(targetFileName))/**/
					.map(File::getName)/**/
					.collect(Collectors.toSet());
		} catch (Exception e) {
			throw e;
		}
	}

	public static List<String> readFileIntoList(String directory, String fileName) throws Exception {
		try {
			String filePath = directory + File.separator + fileName;
			List<String> lines = Files.readAllLines(Paths.get(filePath));
			return lines;
		} catch (Exception e) {
			throw e;
		}
	}

}
