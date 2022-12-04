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
					acquire(boxscoreUrl, gameDate, gameId, roadTeamId, roadConferenceId, homeTeamId, homeConferenceId, writer);
				}
			} catch (Exception e) {
				throw e;
			}

		}

		return;
	}

	private static void acquire(String boxscoreUrl, String gameDate, String gameId, /**/
			String roadTeamId, String roadConferenceId, String homeTeamId, String homeConferenceId, /**/
			BufferedWriter writer) throws Exception {

		boolean noDoc = true;
		int tries = 0;
		int MAX_TRIES = 10;

		while (noDoc)
			try {
				++tries;
				if (tries > MAX_TRIES) {
					log.warn("We have tried 10 times and cannot acquire this document, for game: " + boxscoreUrl);
					return;
				}

				Document doc = getDocument(boxscoreUrl);
				if (doc == null) {
					log.warn("Cannot acquire document for " + boxscoreUrl);
				}

				noDoc = false;

				CumulativeStatsProcessor.generateCumulativeStats(doc, gameId, gameDate, writer, /**/
						roadTeamId, roadConferenceId, homeTeamId, homeConferenceId);

			} catch (Exception e) {
				log.error(e.getMessage());
				Thread.sleep(30000l);
			}
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

	private static Document getDocument(String boxscoreUrl) throws Exception {
		try {
			Document doc = JsoupUtils.jsoupExtraction(boxscoreUrl);
			if (doc == null) {
				log.warn("No html data for this box score request");
				return null;
			}
			return doc;
		} catch (Exception e) {
			throw e;
		}
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
