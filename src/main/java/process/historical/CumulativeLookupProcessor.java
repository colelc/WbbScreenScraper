package process.historical;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;

import process.CumulativeStatsProcessor;
import utils.ConfigUtils;
import utils.FileUtilities;
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

			GAMECAST_DIRECTORY = ConfigUtils.getProperty("project.path.output.data")/**/
					+ File.separator + ConfigUtils.getProperty("season");

			CUMULATIVE_FILE_OUTPUT_LOCATION = ConfigUtils.getProperty("base.project.path.output.data.file.path") /**/
					+ File.separator + ConfigUtils.getProperty("season")/**/
					+ File.separator + ConfigUtils.getProperty("file.cumulative.stats.txt");

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

		for (String gameDate : gamecastsMap.keySet()) {

			try (BufferedWriter writer = new BufferedWriter(new FileWriter(CUMULATIVE_FILE_OUTPUT_LOCATION + "_" + gameDate, false))) {

				List<String> dataForThisDate = gamecastsMap.get(gameDate);

				for (String dataForThisGame : dataForThisDate) {
					String gameId = extractId(dataForThisGame, "gameId");
					String boxscoreUrl = BASE_URL + "boxscore/_/gameId/" + gameId;
					log.info("Processing: " + gameDate + " -> " + boxscoreUrl);

					String roadTeamId = extractId(dataForThisGame, "roadTeamId");
					String roadConferenceId = extractId(dataForThisGame, "roadTeamConferenceId");
					String homeTeamId = extractId(dataForThisGame, "homeTeamId");
					String homeConferenceId = extractId(dataForThisGame, "homeTeamConferenceId");

					Thread.sleep(500l);
					Document doc = JsoupUtils.acquire(boxscoreUrl);
					CumulativeStatsProcessor.generateCumulativeStats(doc, gameId, gameDate, writer, /**/
							roadTeamId, roadConferenceId, homeTeamId, homeConferenceId);

				}
			} catch (Exception e) {
				throw e;
			}

		}

		return;
	}

	private static String extractId(String dataForThisGame, String targetKey) throws Exception {
		try {
			Optional<String> kvPair = Arrays.asList(dataForThisGame.split("\\,")).stream().filter(f -> f.contains(targetKey)).findFirst();// .get().split("=")[1];
			if (kvPair.isPresent()) {
				String[] tokens = kvPair.get().split("=");
				if (tokens == null || tokens.length != 2) {
					if (tokens != null) {
						log.warn(tokens[0] + " -> no value available");
					} else {
						log.warn("Null tokens - cannot extract an id");
					}
					return "";
				}
				return tokens[1];
			}

			log.warn("Cannot acquire an id for key: " + targetKey);
			return "";
		} catch (Exception e) {
			throw e;
		}
	}

	public static void collectGamecastData() throws Exception {

		try {
			Set<String> files = FileUtilities.getFileListFromDirectory(GAMECAST_DIRECTORY, "gamecast_stats");

			files.forEach(file -> {
				String gameDate = file.split("_")[2];
				log.info(file);
				log.info(gameDate);
				log.info("stop");
				try {
					gamecastsMap.put(gameDate, FileUtilities.readFileLines(GAMECAST_DIRECTORY + File.separator + file));
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
