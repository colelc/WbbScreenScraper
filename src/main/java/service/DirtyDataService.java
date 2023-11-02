package service;

import java.io.File;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import utils.ConfigUtils;
import utils.FileUtilities;

public class DirtyDataService {

	private static String PROJECT_PATH_OUTPUT_DATA;
	private static String SEASON;

	private static String dirtyData;
	private static String dirtyDataFile;
	private static String backup;
	private static Map<String, String> dirtyDataMap;

	private static Logger log = Logger.getLogger(DirtyDataService.class);

	static {
		try {
			PROJECT_PATH_OUTPUT_DATA = ConfigUtils.getProperty("project.path.output.data");
			SEASON = ConfigUtils.getProperty("season");

			dirtyData = ConfigUtils.getProperty("dirty.data");
			String dirtyDataDirectory = PROJECT_PATH_OUTPUT_DATA /**/
					+ File.separator + SEASON /**/
					+ File.separator + dirtyData;

			dirtyDataFile = dirtyDataDirectory + File.separator + dirtyData + ".txt";

			FileUtilities.createDirectoryIfNotExists(dirtyDataDirectory);

			backup = new String(dirtyDataFile.replace(dirtyData + ".txt", "backup.txt"));

			dirtyDataMap = generateDirtyDataMap();

			backup();
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
			System.exit(99);
		}
	}

	private static void backup() throws Exception {
		try {
			log.info("Backing up the dirty data file before processing begins...");
			FileUtils.copyFile(new File(dirtyDataFile), new File(backup));
		} catch (Exception e) {
			throw e;
		}
	}

	public static void restore() throws Exception {
		try {
			log.info("Restoring the dirty data file to its start state...");
			FileUtils.copyFile(new File(backup), new File(dirtyDataFile));
		} catch (Exception e) {
			throw e;
		}
	}

	private static Map<String, String> generateDirtyDataMap() throws Exception {
		try {
			FileUtilities.createFileIfDoesNotExist(dirtyDataFile);

			return FileUtilities.fileDataToMap(dirtyDataFile, "");
		} catch (Exception e) {
			throw e;
		}
	}

	public static Map<String, String> getDirtyDataMap() {
		return dirtyDataMap;
	}

	public static void writeOutDirtyDataFile() throws Exception {
		try {
			log.info("Writing out dirty data to the dirty data file...");
			FileUtilities.writeMapToFile(dirtyDataFile, dirtyDataMap, false);
		} catch (Exception e) {
			throw e;
		}
	}

}
