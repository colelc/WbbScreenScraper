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
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import utils.CalendarUtils;
import utils.ConfigUtils;
import utils.FileUtils;
import utils.JsoupUtils;

public class DataProcessor {

	private static Integer id = null;
	private static String SCOREBOARD_URL;
	private static String BASE_URL;
	private static Map<Integer, Map<String, String>> conferenceMap;
	private static Map<Integer, Map<String, String>> teamMap;
	private static Map<Integer, Map<String, String>> playerMap;
	private static String now;

	private static Logger log = Logger.getLogger(DataProcessor.class);

	static {
		try {
			conferenceMap = new HashMap<>();
			teamMap = new HashMap<>();
			playerMap = new HashMap<>();
			SCOREBOARD_URL = ConfigUtils.getProperty("espn.com.womens.scoreboard");
			BASE_URL = ConfigUtils.getProperty("espn.com.womens.college.basketball");

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
		String homeTeamId = null;
		String roadTeamId = null;
		String homeConferenceId = null;
		String roadConferenceId = null;

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

				Document htmlDoc = JsoupUtils.parseStringToDocument(JsoupUtils.jsoupExtraction(SCOREBOARD_URL + gameDate).toString());

				int sequence = -1;
				Elements gameElements = JsoupUtils.nullElementCheck(htmlDoc.select("div.Scoreboard__Callouts"), "div.Scoreboard__Callouts");
				if (gameElements != null && gameElements.first() != null) {
					for (Element gameElement : gameElements) {
						++sequence;
						// set of 3 links (Gamecast, Box Score, Highlights) - we only care about the
						// gameId which can be found in any of the 3 links
						String href = gameElement.getElementsByAttribute("href").first().attr("href");
						gameId = Arrays.asList(href.split("/")).stream().reduce((first, second) -> second).get();

						String gamecastUrl = BASE_URL + "game/_/gameId/" + gameId;
						String boxscoreUrl = BASE_URL + "boxscore/_/gameId/" + gameId;
						String playbyplayUrl = BASE_URL + "playbyplay/_/gameId/" + gameId;

						// need the team id's
						Element competitorsElement = JsoupUtils.nullElementCheck(htmlDoc.select("ul.ScoreboardScoreCell__Competitors"), "ul.ScoreboardScoreCell__Competitors").get(sequence);

						String roadTeamUrl = competitorsElement.getElementsByTag("li").first().getElementsByTag("a").first().attr("href");
						roadTeamId = Arrays.asList(roadTeamUrl.split("/")).stream().filter(f -> StringUtils.isNumeric(f)).collect(Collectors.toList()).get(0);
						roadConferenceId = teamMap.get(Integer.valueOf(roadTeamId)).get("conferenceId");

						String homeTeamUrl = competitorsElement.getElementsByTag("li").last().getElementsByTag("a").first().attr("href");
						homeTeamId = Arrays.asList(homeTeamUrl.split("/")).stream().filter(f -> StringUtils.isNumeric(f)).collect(Collectors.toList()).get(0);
						homeConferenceId = teamMap.get(Integer.valueOf(homeTeamId)).get("conferenceId");

						String title = teamMap.get(Integer.valueOf(roadTeamId)).get("teamName") /**/
								+ " (" + conferenceMap.get(Integer.valueOf(roadConferenceId)).get("shortName") + ")" /**/
								+ " at " /**/
								+ teamMap.get(Integer.valueOf(homeTeamId)).get("teamName") /**/
								+ " (" + conferenceMap.get(Integer.valueOf(homeConferenceId)).get("shortName") + ")";

						log.info(gameDate + " -> " + gameId + " " + title);

						GamecastProcessor.generateGamecastData(/**/
								gamecastUrl, /**/
								gameId, /**/
								gameDate, /**/
								homeTeamId, /**/
								homeConferenceId, /**/
								roadTeamId, /**/
								roadConferenceId, /**/
								gamecastWriter);

						GameStatProcessor.processGameStats(boxscoreUrl, gameId, gameDate, homeTeamId, homeConferenceId, roadTeamId, roadConferenceId, gameStatWriter);

						PlayByPlayProcessor.processPlayByPlay(playbyplayUrl, gameId, gameDate, homeTeamId, homeConferenceId, roadTeamId, roadConferenceId, playerMap, playByPlayWriter);
					}
				}
			} catch (Exception e) {
				throw e;
			}

			datesProcessed.add(gameDate);
		}

		return datesProcessed;
	}

	private static void loadDataFiles(String conferenceFile, /**/
			String teamFile, /**/
			String playerFile /**/
	// String scheduleFile/**/
	) throws Exception {

		try {
			conferenceMap = fileDataToMap(FileUtils.readFileLines(conferenceFile), false);
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
