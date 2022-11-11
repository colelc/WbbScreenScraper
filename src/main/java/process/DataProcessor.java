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
	private static String NOT_AVAILABLE = "";

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
		String title = null;

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
//						log.info(gamecastUrl);
						log.info("");
						log.info("Processing: " + boxscoreUrl);
//						log.info(playbyplayUrl);

						// need the team id's
						Element competitorsElement = JsoupUtils.nullElementCheck(htmlDoc.select("ul.ScoreboardScoreCell__Competitors"), "ul.ScoreboardScoreCell__Competitors").get(sequence);
						Elements el = competitorsElement.getElementsByTag("li");

						roadTeamId = getARoadTeamId(competitorsElement, el);
						roadConferenceId = getAConferenceId(roadTeamId);

						homeTeamId = getAHomeTeamId(competitorsElement);
						homeConferenceId = getAConferenceId(homeTeamId);

						String thisRoadTeam = "";
						if (roadTeamId != null && roadTeamId.trim().length() > 0) {
							Map<String, String> thisRoadTeamMap = teamMap.get(Integer.valueOf(roadTeamId));
							if (thisRoadTeamMap != null) {
								thisRoadTeam = thisRoadTeamMap.get("teamName");
							}
						}

						title = thisRoadTeam /**/
								+ " (" /**/
								+ (roadConferenceId.compareTo(NOT_AVAILABLE) == 0 ? "NA" : conferenceMap.get(Integer.valueOf(roadConferenceId)).get("shortName") + ")")/**/
								// + conferenceMap.get(Integer.valueOf(roadConferenceId)).get("shortName") + ")"
								// /**/
								+ " at " /**/
								+ (homeTeamId.compareTo(NOT_AVAILABLE) == 0 ? "NA" : teamMap.get(Integer.valueOf(homeTeamId)).get("teamName"))/**/
								// + teamMap.get(Integer.valueOf(homeTeamId)).get("teamName") /**/
								+ (homeConferenceId.compareTo(NOT_AVAILABLE) == 0 ? "NA" : " (" + conferenceMap.get(Integer.valueOf(homeConferenceId)).get("shortName") + ")")
						// + " (" +
						// conferenceMap.get(Integer.valueOf(homeConferenceId)).get("shortName") + ")";
						;

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

	private static String getAHomeTeamId(Element el) throws Exception {
		String teamId = NOT_AVAILABLE;

		try {
			if (el == null || el.getElementsByTag("li") == null || el.getElementsByTag("li").last() == null) {
				// log.warn("Cannot acquire a teamId");
				return teamId;
			}

			Elements theseElements = el.getElementsByTag("li").last().getElementsByTag("a");
			if (theseElements == null || theseElements.first() == null) {
				// log.warn("Cannot acquire a teamId");
				// log.info(theseElements.toString());
				return teamId;
			}

			String teamUrl = theseElements.first().attr("href");
			if (teamUrl == null) {
				// log.warn("Cannot acquire a teamId");
				// log.info(theseElements.first().toString());
				return teamId;
			}

			teamId = Arrays.asList(teamUrl.split("/")).stream().filter(f -> StringUtils.isNumeric(f)).collect(Collectors.toList()).get(0);
			if (teamId != null) {
				return teamId;
			} else {
				// log.warn("Cannot acquire a teamId");
				// log.info(teamUrl);
				return NOT_AVAILABLE;
			}
		} catch (Exception e) {
			throw e;
		}
	}

	private static String getARoadTeamId(Element competitorsElement, Elements el) throws Exception {
		String teamId = NOT_AVAILABLE;

		try {
			if (el == null || el.first() == null || el.first().getElementsByTag("a") == null) {
				// log.warn("el: will not be able to assign team conferenceId and teamId");
				// log.info(competitorsElement.toString());
				return teamId;
			}

			Elements elAnchorElements = el.first().getElementsByTag("a");
			if (elAnchorElements.first() == null || elAnchorElements.first().getElementsByTag("a") == null) {
				// log.warn("elAnchorElements: will not be able to assign team conferenceId and
				// teamId");
				// log.info(el.toString());
				return teamId;
			}

			String teamUrl = elAnchorElements.first().getElementsByTag("a").first().attr("href");
			if (teamUrl == null) {
				// log.warn("teamUrl: will not be able to assign team conferenceId and teamId");
				// log.info(elAnchorElements.first().getElementsByTag("a").first().toString());
				return null;
			}

			teamId = Arrays.asList(teamUrl.split("/")).stream().filter(f -> StringUtils.isNumeric(f)).collect(Collectors.toList()).get(0);
			if (teamId == null) {
				// log.warn("teamId extraction from teamUrl: will not be able to assign team
				// conferenceId and teamId");
				// log.info(teamUrl);
				return NOT_AVAILABLE;
			}

			return teamId;
		} catch (Exception e) {
			throw e;
		}
	}

	private static String getAConferenceId(String teamId) throws Exception {
		try {
			if (teamId == null || teamId.trim().length() == 0) {
				// log.warn("There is no teamId, therefore a conferenceId lookup is not
				// possible");
				return NOT_AVAILABLE;
			}
			Map<String, String> thisTeamMap = teamMap.get(Integer.valueOf(teamId));
			if (thisTeamMap != null) {
				String conferenceId = teamMap.get(Integer.valueOf(teamId)).get("conferenceId");
				if (conferenceId != null) {
					return conferenceId;
				} else {
					return NOT_AVAILABLE;
				}
			} else {
				// log.warn("No entry in team map for teamId=" + teamId + " - cannot assign
				// conferenceId");
				return NOT_AVAILABLE;
			}
		} catch (Exception e) {
			throw e;
		}
	}

	public static void loadDataFiles(String conferenceFile, /**/
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

		// if (debug) {
		// retMap.forEach((key, map) -> log.info(key + " -> " + map.toString()));
		// }
		return retMap;
	}

	public static Integer getId() {
		return id;
	}

	public static String getSCOREBOARD_URL() {
		return SCOREBOARD_URL;
	}

	public static String getBASE_URL() {
		return BASE_URL;
	}

	public static Map<Integer, Map<String, String>> getConferenceMap() {
		return conferenceMap;
	}

	public static Map<Integer, Map<String, String>> getTeamMap() {
		return teamMap;
	}

	public static Map<Integer, Map<String, String>> getPlayerMap() {
		return playerMap;
	}

	public static String getNow() {
		return now;
	}

	public static Logger getLog() {
		return log;
	}

}
