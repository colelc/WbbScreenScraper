package process;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
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

import service.ConferenceTeamPlayerService;
import service.DirtyDataService;
import utils.CalendarUtils;
import utils.ConfigUtils;
import utils.FileUtilities;
import utils.JsoupUtils;

public class DataProcessor {

	private static String SEASON;
	private static String ESPN_SCOREBOARD_URL;
	private static String ESPN_WBB_HOME_URL;
	private static String now;
	// private static String NOT_AVAILABLE = null;
	private static String PROJECT_PATH_OUTPUT_DATA;

	private static String dateTrackerFile;
	private static Set<String> skipDates;

	private static String gameStatsOutputDirectory;
	private static String playByPlayOutputDirectory;
	private static String gamecastOutputDirectory;
	private static String cumulativeStatsDirectory;
//	private static String dirtyDataDirectory;

	private static String gameStats;
	private static String playbyplayStats;
	private static String gamecastStats;
	private static String cumulativeStats;
	// private static String dirtyData;

	private static Logger log = Logger.getLogger(DataProcessor.class);

	static {
		try {
			ESPN_SCOREBOARD_URL = ConfigUtils.getProperty("espn.com.womens.scoreboard").trim();
			ESPN_WBB_HOME_URL = ConfigUtils.getProperty("espn.com.womens.college.basketball");

			now = LocalDate.ofInstant(Instant.now(), ZoneId.systemDefault()).toString().replace("-", "");

			PROJECT_PATH_OUTPUT_DATA = ConfigUtils.getProperty("project.path.output.data");

			SEASON = ConfigUtils.getProperty("season");

			gameStats = ConfigUtils.getProperty("game.stats");
			playbyplayStats = ConfigUtils.getProperty("play.by.play.stats");
			gamecastStats = ConfigUtils.getProperty("gamecast.stats");
			cumulativeStats = ConfigUtils.getProperty("cumulative.stats");

			gameStatsOutputDirectory = PROJECT_PATH_OUTPUT_DATA + File.separator + SEASON + File.separator + gameStats;
			playByPlayOutputDirectory = PROJECT_PATH_OUTPUT_DATA + File.separator + SEASON + File.separator + playbyplayStats;
			gamecastOutputDirectory = PROJECT_PATH_OUTPUT_DATA + File.separator + SEASON + File.separator + gamecastStats;
			cumulativeStatsDirectory = PROJECT_PATH_OUTPUT_DATA + File.separator + SEASON + File.separator + cumulativeStats;

			dateTrackerFile = ConfigUtils.getProperty("project.path.date.tracker") + File.separator + SEASON + File.separator + ConfigUtils.getProperty("file.date.tracker.txt");
			skipDates = (!FileUtilities.createFileIfDoesNotExist(dateTrackerFile)) ? FileUtilities.readFileLines(dateTrackerFile).stream().collect(Collectors.toSet()) : new HashSet<>();
			skipDates.forEach(d -> log.info(d + " -> " + "will not process this date"));

		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
			System.exit(99);
		}
	}

	public static void go() throws Exception {

		try {
			// load conference and team maps (and player maps)
			ConferenceTeamPlayerService.loadDataFiles(true);

			Set<String> datesProcessed = extractGameData();

			// capture the dates just processed
			FileUtilities.writeAllLines(dateTrackerFile, datesProcessed, skipDates.size(), true);

			DirtyDataService.writeOutDirtyDataFile();
		} catch (Exception e) {
			throw e;
		}

		return;
	}

	private static Set<String> extractGameData() throws Exception {

		Set<String> datesProcessed = new HashSet<>();
		String gameId = null;
		String homeTeamId = null;
		String roadTeamId = null;
		String homeConferenceId = null;
		String roadConferenceId = null;
		String title = null;

		List<String> seasonDates = new ArrayList<>(CalendarUtils.generateDates(ConfigUtils.getProperty("season.start.date"), ConfigUtils.getProperty("season.end.date")));

		try {

			for (String gameDate : seasonDates) {
				if (skipDates.contains(gameDate)) {
					log.info("Skipping day: " + gameDate);
					continue;
				}

				if (!CalendarUtils.hasGameBeenPlayed(gameDate, now)) {
					log.info("Skipping day: " + gameDate);
					continue;
				}

				String endOfFileName = "_" + gameDate + ".txt";
				String gameStatsFile = gameStatsOutputDirectory + File.separator + gameStats + endOfFileName;
				String playbyplayFile = playByPlayOutputDirectory + File.separator + playbyplayStats + endOfFileName;
				String gamecastFile = gamecastOutputDirectory + File.separator + gamecastStats + endOfFileName;
				String cumulativeFile = cumulativeStatsDirectory + File.separator + cumulativeStats + endOfFileName;
				// String dirtyDataFile = dirtyDataDirectory + File.separator + dirtyData +
				// ".txt";

				try (BufferedWriter gameStatWriter = new BufferedWriter(new FileWriter(gameStatsFile, false));
						/**/
						BufferedWriter playByPlayWriter = new BufferedWriter(new FileWriter(playbyplayFile, false));
						/**/
						BufferedWriter gamecastWriter = new BufferedWriter(new FileWriter(gamecastFile, false));
						/**/
						BufferedWriter cumulativeWriter = new BufferedWriter(new FileWriter(cumulativeFile, false))) {

					String scoreboardUrlForThisDay = ESPN_SCOREBOARD_URL.replace("yyyymmdd", gameDate);
					log.info(gameDate + " -> " + scoreboardUrlForThisDay);

					Document htmlDoc = JsoupUtils.acquire(scoreboardUrlForThisDay);

					int sequence = -1;
					Elements gameElements = JsoupUtils.nullElementCheck(htmlDoc.select("div.Scoreboard__Callouts"));
					if (gameElements != null) {
						for (Element gameElement : gameElements) {
							++sequence;
							// set of 3 links (Gamecast, Box Score, Highlights) - we only care about the
							// gameId which can be found in any of the 3 links
							String href = gameElement.getElementsByAttribute("href").first().attr("href");
							gameId = Arrays.asList(href.split("/")).stream().reduce((first, second) -> second).get();

							String gamecastUrl = ESPN_WBB_HOME_URL + "game/_/gameId/" + gameId;
							String boxscoreUrl = ESPN_WBB_HOME_URL + "boxscore/_/gameId/" + gameId;
							String playbyplayUrl = ESPN_WBB_HOME_URL + "playbyplay/_/gameId/" + gameId;

							// need the team id's
							Element competitorsElement = JsoupUtils.nullElementCheck(htmlDoc.select("ul.ScoreboardScoreCell__Competitors")).get(sequence);
							Elements el = competitorsElement.getElementsByTag("li");

							roadTeamId = getARoadTeamId(competitorsElement, el);
							roadConferenceId = ConferenceTeamPlayerService.getAConferenceId(roadTeamId);

							homeTeamId = getAHomeTeamId(competitorsElement);
							homeConferenceId = ConferenceTeamPlayerService.getAConferenceId(homeTeamId);

							String thisRoadTeam = "";
							if (roadTeamId != null && roadTeamId.trim().length() > 0) {
								Map<String, String> thisRoadTeamMap = ConferenceTeamPlayerService.getTeamMap().get(Integer.valueOf(roadTeamId));
								if (thisRoadTeamMap != null) {
									thisRoadTeam = thisRoadTeamMap.get("teamName");
								}
							}

							title = thisRoadTeam /**/
									+ " (" /**/
									+ (roadConferenceId == null || roadConferenceId.compareTo("") == 0 ? "**NA**"
											: ConferenceTeamPlayerService.getConferenceMap().get(Integer.valueOf(roadConferenceId)).get("shortName") + ")")/**/
									+ " at " /**/
									+ (homeTeamId == null || homeTeamId.compareTo("") == 0 ? "**NA**" : ConferenceTeamPlayerService.getTeamMap().get(Integer.valueOf(homeTeamId)).get("teamName"))/**/
									+ (homeConferenceId == null || homeConferenceId.compareTo("") == 0 ? "**NA**"
											: " (" + ConferenceTeamPlayerService.getConferenceMap().get(Integer.valueOf(homeConferenceId)).get("shortName") + ")");

							log.info(gameDate + " -> " + gameId + " " + title + " " + boxscoreUrl);

							GamecastProcessor.generateGamecastData(/**/
									gamecastUrl, /**/
									gameId, /**/
									gameDate, /**/
									homeTeamId, /**/
									homeConferenceId, /**/
									roadTeamId, /**/
									roadConferenceId, /**/
									gamecastWriter);

							Document gameStatDoc = GameStatProcessor.processGameStats(boxscoreUrl, gameId, gameDate, homeTeamId, homeConferenceId, roadTeamId, roadConferenceId, gameStatWriter);

							Document playbyplayDoc = JsoupUtils.acquire(playbyplayUrl);
							boolean data = PlayByPlayProcessor.processPlayByPlay(playbyplayDoc, boxscoreUrl, gameId, gameDate, /**/
									homeTeamId, /**/
									homeConferenceId, /**/
									roadTeamId, /**/
									roadConferenceId, /**/
									playByPlayWriter);

							if (data) {
								CumulativeStatsProcessor.generateCumulativeStats(gameStatDoc, gameId, gameDate, cumulativeWriter, /**/
										roadTeamId, roadConferenceId, homeTeamId, homeConferenceId);
							}
						}
					}
				} catch (Exception e) {
					throw e;
				}
				datesProcessed.add(gameDate);
			}
		} catch (Exception e) {
			throw e;
		}

		return datesProcessed;
	}

	private static String getAHomeTeamId(Element el) throws Exception {
		String teamId = "";

		try {
			if (el == null || el.getElementsByTag("li") == null || el.getElementsByTag("li").last() == null) {
				// log.warn("Cannot acquire a teamId");
				return "";
			}

			Elements theseElements = el.getElementsByTag("li").last().getElementsByTag("a");
			if (theseElements == null || theseElements.first() == null) {
				// log.warn("Cannot acquire a teamId");
				// log.info(theseElements.toString());
				return "";
			}

			String teamUrl = theseElements.first().attr("href");
			if (teamUrl == null) {
				// log.warn("Cannot acquire a teamId");
				// log.info(theseElements.first().toString());
				return "";
			}

			teamId = Arrays.asList(teamUrl.split("/")).stream().filter(f -> StringUtils.isNumeric(f)).collect(Collectors.toList()).get(0);
			if (teamId != null) {
				return teamId;
			} else {
				// log.warn("Cannot acquire a teamId");
				// log.info(teamUrl);
				return "";
			}
		} catch (Exception e) {
			throw e;
		}
	}

	private static String getARoadTeamId(Element competitorsElement, Elements el) throws Exception {
		String teamId = "";

		try {
			if (el == null || el.first() == null || el.first().getElementsByTag("a") == null) {
				// log.warn("el: will not be able to assign team conferenceId and teamId");
				// log.info(competitorsElement.toString());
				return "";
			}

			Elements elAnchorElements = el.first().getElementsByTag("a");
			if (elAnchorElements.first() == null || elAnchorElements.first().getElementsByTag("a") == null) {
				// log.warn("elAnchorElements: will not be able to assign team conferenceId and
				// teamId");
				// log.info(el.toString());
				return "";
			}

			String teamUrl = elAnchorElements.first().getElementsByTag("a").first().attr("href");
			if (teamUrl == null) {
				// log.warn("teamUrl: will not be able to assign team conferenceId and teamId");
				// log.info(elAnchorElements.first().getElementsByTag("a").first().toString());
				return "";
			}

			teamId = Arrays.asList(teamUrl.split("/")).stream().filter(f -> StringUtils.isNumeric(f)).collect(Collectors.toList()).get(0);
			if (teamId == null) {
				// log.warn("teamId extraction from teamUrl: will not be able to assign team
				// conferenceId and teamId");
				// log.info(teamUrl);
				return "";
			}

			return teamId;
		} catch (Exception e) {
			throw e;
		}
	}
}
