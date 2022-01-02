package process;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import utils.CalendarUtils;
import utils.ConfigUtils;
import utils.FileUtils;
import utils.JsoupUtils;

public class DataProcessor {

	private static Integer id = null;
	private static String SCOREBOARD_URL;
	private static Map<Integer, Map<String, String>> teamMap;
	private static Map<Integer, Map<String, String>> playerMap;
	private static String now;

	private static Logger log = Logger.getLogger(DataProcessor.class);

	static {
		try {
			teamMap = new HashMap<>();
			playerMap = new HashMap<>();
			SCOREBOARD_URL = ConfigUtils.getProperty("espn.com.womens.scoreboard");

			now = LocalDate.ofInstant(Instant.now(), ZoneId.systemDefault()).toString().replace("-", "");
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
			System.exit(99);
		}
	}

	public static void generateGameDataFiles(Set<String> skipDates, /**/
			String dateTrackerFile, /**/
			String conferenceFile, /**/
			String teamFile, /**/
			String playerFile, /**/
			String gameStatFile, /**/
			String playByPlayFile, /**/
			String gamecastFile) throws Exception {

		try {
			loadDataFiles(conferenceFile, teamFile, playerFile/* , scheduleFile */);

			Set<String> datesProcessed = extractGameData(skipDates, gameStatFile, playByPlayFile, gamecastFile);

			// capture the dates just processed
			FileUtils.writeAllLines(dateTrackerFile, datesProcessed, skipDates.size(), true);
		} catch (Exception e) {
			throw e;
		}

		return;
	}

	private static Set<String> extractGameData(Set<String> skipDates, String gameStatOutputFile, String playByPlayOutputFile, String gamecastOutputFile) throws Exception {

		Set<String> datesProcessed = new HashSet<>();
		String gameId = null;
		String teamId = null;
		String opponentTeamId = null;
		String opponentConferenceId = null;
		String conferenceId = null;

		List<String> seasonDates = new ArrayList<>(CalendarUtils.generateDates(ConfigUtils.getProperty("season.start.date"), ConfigUtils.getProperty("season.end.date")));

		for (String gameDate : seasonDates) {
			if (skipDates.contains(gameDate)) {
				log.info("Skipping day: " + gameDate);
				continue;
			}

			if (!CalendarUtils.hasGameBeenPlayed(gameDate, now)) {
				log.info("Skipping day: " + gameDate);
				continue;
			}

			try (BufferedWriter gameStatWriter = new BufferedWriter(new FileWriter(gameStatOutputFile + "_" + gameDate, false));
					/**/
					BufferedWriter playByPlayWriter = new BufferedWriter(new FileWriter(playByPlayOutputFile + "_" + gameDate, false));
					/**/
					BufferedWriter gamecastWriter = new BufferedWriter(new FileWriter(gamecastOutputFile + "_" + gameDate, false))) {

				log.info(gameDate + " -> " + SCOREBOARD_URL + gameDate);

				String target = StringUtils.substringBetween(/**/
						JsoupUtils.jsoupExtraction(SCOREBOARD_URL + gameDate).toString(), /**/
						"window.espn.scoreboardData", /**/
						"window.espn.scoreboardSettings")/**/
						.replace("\t", "").replace(" = ", "")/**/
				;
				target = target.substring(0, target.length() - 1);

				if (!utils.StringUtils.isPopulated(target)) {
					log.warn(gameDate + " -> " + "Cannot acquire JSON target");
					continue;
				}

				JsonElement jsonElement = new GsonBuilder().setPrettyPrinting().create().fromJson(target, JsonObject.class).get("events");
				if (!jsonElement.isJsonArray()) {
					log.warn(gameDate + " -> " + "Cannot acquire array");
					continue;
				}

				JsonArray events = jsonElement.getAsJsonArray();

				for (JsonElement event : events) {
					if (!event.isJsonObject()) {
						log.warn(gameDate + " -> " + "event is not a JSON Object");
						continue;
					}

					JsonObject gameObject = event.getAsJsonObject();

					gameId = gameObject.get("id").getAsString();
					String gameName = gameObject.get("name").getAsString();

					String status = gameObject.get("status").getAsJsonObject().get("type").getAsJsonObject().get("detail").getAsString();
					log.info(gameDate + " -> " + gameName + (status.compareTo("Final") != 0 ? " -> " + status : ""));

					String[] ids = renderTeamAndConferenceIds(gameName);
					opponentTeamId = ids[0]; // road team
					opponentConferenceId = ids[1];
					teamId = ids[2]; // home team
					conferenceId = ids[3];

					// extract 3 URLs for gamestats, gamecast, and playbyplay files
					JsonArray urlArray = gameObject.get("links").getAsJsonArray();

					for (JsonElement url : urlArray) {
						JsonObject urlObject = url.getAsJsonObject();
						String urlType = urlObject.get("text").getAsString();
						String urlLink = urlObject.get("href").getAsString();

						if (urlType.compareTo("Gamecast") == 0) {
							String gameTimeUTC = CalendarUtils.parseUTCTime(gameObject.get("date").getAsString());
							JsonObject venueObject = gameObject.get("competitions").getAsJsonArray().get(0).getAsJsonObject().get("venue").getAsJsonObject();
							JsonArray networkArray = gameObject.get("competitions").getAsJsonArray().get(0).getAsJsonObject().get("geoBroadcasts").getAsJsonArray();

							GamecastProcessor.generateGamecastData(/**/
									urlLink, /**/
									gameId, /**/
									gameName, /**/
									gameDate, /**/
									gameTimeUTC, /**/
									venueObject, /**/
									networkArray, /**/
									teamId, /**/
									conferenceId, /**/
									opponentTeamId, /**/
									opponentConferenceId, /**/
									status, /**/
									gamecastWriter);
						} else if (urlType.compareTo("Box Score") == 0) {
							GameStatProcessor.processGameStats(urlLink, gameId, gameDate, teamId, conferenceId, opponentTeamId, opponentConferenceId, gameStatWriter);
						} else if (urlType.compareTo("Play-by-Play") == 0) {
							PlayByPlayProcessor.processPlayByPlay(urlLink, gameId, gameDate, teamId, conferenceId, opponentTeamId, opponentConferenceId, playerMap, playByPlayWriter);
						}
					}
				}

			} catch (Exception e) {
				throw e;
			}

			datesProcessed.add(gameDate);
		}

		return datesProcessed;
	}

	private static String[] renderTeamAndConferenceIds(String gameName) throws Exception {
		String[] retValue = new String[] { "", "", "", "" };

		try {
			List<String> teamNames = Arrays.asList(gameName.split(" at "));

			// home team
			Optional<Entry<Integer, Map<String, String>>> opt = teamMap.entrySet().stream().filter(f -> f.getValue().get("teamName").compareTo(teamNames.get(1)) == 0).findFirst();

			if (opt.isEmpty()) {
				;// log.warn("No teamId found in teamMap for " + teamNames.get(0));
			} else {
				retValue[2] = String.valueOf(opt.get().getKey());
				retValue[3] = opt.get().getValue().get("conferenceId");
			}

			// road team
			opt = teamMap.entrySet().stream().filter(f -> f.getValue().get("teamName").compareTo(teamNames.get(0)) == 0).findFirst();

			if (opt.isEmpty()) {
				;// log.warn("No opponentTeamId found in teamMap for " + teamNames.get(0));
			} else {
				retValue[0] = String.valueOf(opt.get().getKey());
				retValue[1] = opt.get().getValue().get("conferenceId");
			}
		} catch (Exception e) {
			throw e;
		}
		return retValue;
	}

	private static void loadDataFiles(String conferenceFile, /**/
			String teamFile, /**/
			String playerFile /**/
	// String scheduleFile/**/
	) throws Exception {

		try {
			// conferenceMap = fileDataToMap(FileUtils.readFileLines(conferenceFile),
			// false);
			teamMap = fileDataToMap(FileUtils.readFileLines(teamFile), false);
			playerMap = fileDataToMap(FileUtils.readFileLines(playerFile), false);
			// scheduleMap = fileDataToMap(FileUtils.readFileLines(scheduleFile), false);
		} catch (Exception e) {
			throw e;
		}

		return;
	}

	private static Map<Integer, Map<String, String>> fileDataToMap(List<String> dataList, boolean debug) throws Exception {

		Map<Integer, Map<String, String>> retMap = new HashMap<>();

		try {
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
					}
				});
				retMap.put(id, map);
			});
		} catch (Exception e) {
			throw e;
		}

		if (debug) {
			retMap.forEach((key, map) -> log.info(key + " -> " + map.toString()));
		}
		return retMap;
	}

}
