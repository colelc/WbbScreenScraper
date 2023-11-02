package utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;

import https.service.HttpsClientService;

public class FileUtilities {

	private static Integer id = null;
	private static Logger log = Logger.getLogger(FileUtilities.class);

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

	public static void createDirectoryIfNotExists(String dirName) throws Exception {
		try {
			Path dirPath = Paths.get(dirName);
			if (!Files.exists(dirPath)) {
				log.info("Creating directory: " + dirName);
				Files.createDirectory(dirPath);
			}
		} catch (Exception e) {
			throw e;
		}
	}

	public static boolean createFileIfDoesNotExist(String fileName) throws Exception {
		try {
			if (!FileUtilities.doesFileExist(fileName)) {
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

	public static Map<String, String> fileDataToMap(String targetFile, String blah) throws Exception {

		Map<String, String> retMap = new HashMap<>();

		try {
			List<String> dataList = readFileLines(targetFile);

			dataList.forEach(data -> {
				List<String> attributes = Arrays.asList(data.split(","));

				attributes.forEach(attribute -> {
					String[] tokens = attribute.replace("[", "").replace("]", "").split("=");
					if (tokens != null && tokens.length == 2) {
						retMap.put(tokens[0].toLowerCase().trim(), tokens[1].toLowerCase().trim());
					} else if (tokens.length == 1) {
						String key = tokens[0].trim();
						String value = "";
						retMap.put(key, value);
					}
				});
			});
		} catch (Exception e) {
			throw e;
		}

		// retMap.forEach((key, value) -> log.info(key + " -> " + value)));

		return retMap;
	}

	public static Map<Integer, Map<String, String>> fileDataToMap(String targetFile) throws Exception {

		Map<Integer, Map<String, String>> retMap = new HashMap<>();

		try {
			List<String> dataList = readFileLines(targetFile);

			dataList.forEach(data -> {
				List<String> attributes = Arrays.asList(data.split(","));

				Map<String, String> map = new HashMap<>();

				attributes.forEach(attribute -> {
					String[] tokens = attribute.replace("[", "").replace("]", "").split("=");
					if (tokens != null && tokens.length == 2) {
						String key = tokens[0].trim();
						String value = tokens[1].trim();

						if (key.compareTo("id") == 0) {
							id = Integer.valueOf(value);
						} else {
							map.put(key, value);
						}
					} else if (tokens.length == 1) {
						String key = tokens[0].trim();
						String value = "";
						map.put(key, value);
					}
				});
				retMap.put(id, map);
			});
		} catch (Exception e) {
			throw e;
		}

		// retMap.forEach((key, map) -> log.info(key + " -> " + map.toString()));

		return retMap;
	}

}
