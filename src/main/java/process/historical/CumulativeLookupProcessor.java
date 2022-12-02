package process.historical;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;

import process.CumulativeStatsProcessor;
import utils.ConfigUtils;
import utils.FileUtils;
import utils.JsoupUtils;

public class CumulativeLookupProcessor {

	private static Logger log = Logger.getLogger(CumulativeLookupProcessor.class);

	private static String BASE_URL;
	private static String GAMECAST_DIRECTORY;
	private static String CUMULATIVE_FILE_OUTPUT_LOCATION;
	private static Map<String, List<String>> gamecastsMap; // by game date

	static {
		try {
			gamecastsMap = new HashMap<>();
			BASE_URL = ConfigUtils.getProperty("espn.com.womens.college.basketball");

			GAMECAST_DIRECTORY = ConfigUtils.getBASE_OUTPUT_PATH()/**/
					+ File.separator + ConfigUtils.getProperty("base.output.child.data.path.season.2021.2022");

			CUMULATIVE_FILE_OUTPUT_LOCATION = ConfigUtils.getBASE_OUTPUT_PATH() /**/
					+ File.separator + ConfigUtils.getProperty("base.output.child.data.path.season.2021.2022")/**/
					+ File.separator + ConfigUtils.getProperty("file.cumulative.stats");

		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
			System.exit(99);
		}
	}

	public static void go() throws Exception {

		try {
			collectGamecastData();
			processGameIds();
		} catch (Exception e) {
			throw e;
		}

		return;
	}

	public static void processGameIds() throws Exception {

		try {
			for (String gameDate : gamecastsMap.keySet()) {
				String fileLocation = CUMULATIVE_FILE_OUTPUT_LOCATION + "_" + gameDate;
				log.info(fileLocation);

				try (BufferedWriter writer = new BufferedWriter(new FileWriter(CUMULATIVE_FILE_OUTPUT_LOCATION + "_" + gameDate, false))) {

					List<String> dataForThisDate = gamecastsMap.get(gameDate);
					for (String dataForThisGame : dataForThisDate) {
						String gameId = Arrays.asList(dataForThisGame.split("\\,")).stream().filter(f -> f.contains("gameId")).findFirst().get().split("=")[1];
						String boxscoreUrl = BASE_URL + "boxscore/_/gameId/" + gameId;
						log.info("Processing: " + boxscoreUrl);

						Document doc = JsoupUtils.jsoupExtraction(boxscoreUrl);
						if (doc == null) {
							log.warn("No html data for this box score request");
							continue;
						}

						String roadTeamId = Arrays.asList(dataForThisGame.split("\\,")).stream().filter(f -> f.contains("roadTeamId")).findFirst().get().split("=")[1];
						String roadConferenceId = Arrays.asList(dataForThisGame.split("\\,")).stream().filter(f -> f.contains("roadTeamConferenceId")).findFirst().get().split("=")[1];
						String homeTeamId = Arrays.asList(dataForThisGame.split("\\,")).stream().filter(f -> f.contains("homeTeamId")).findFirst().get().split("=")[1];
						String homeConferenceId = Arrays.asList(dataForThisGame.split("\\,")).stream().filter(f -> f.contains("homeTeamConferenceId")).findFirst().get().split("=")[1];

						CumulativeStatsProcessor.generateCumulativeStats(doc, gameId, gameDate, writer, /**/
								roadTeamId, roadConferenceId, homeTeamId, homeConferenceId);
					}
				} catch (Exception e) {
					throw e;
				}

			}
		} catch (Exception e) {
			throw e;
		}

		return;
	}

	public static void collectGamecastData() throws Exception {

		try {
			Set<String> files = FileUtils.getFileListFromDirectory(GAMECAST_DIRECTORY, "gamecast_stats");

			files.forEach(file -> {
				String gameDate = file.split("_")[2];
				log.info(file);
				log.info(gameDate);
				log.info("stop");
				try {
					gamecastsMap.put(gameDate, FileUtils.readFileLines(GAMECAST_DIRECTORY + File.separator + file));
				} catch (Exception e) {
					log.error(e.getMessage());
					e.printStackTrace();
					System.exit(99);
				}
			});
		} catch (Exception e) {
			throw e;
		}

		return;
	}

}
