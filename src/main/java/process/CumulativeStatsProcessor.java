package process;

import java.io.BufferedWriter;

import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import utils.JsoupUtils;

public class CumulativeStatsProcessor {

	private static Integer homeFgAttempted = 0;
	private static Integer homeFgMade = 0;
	private static Integer homeFg3Attempted = 0;
	private static Integer homeFg3Made = 0;
	private static Integer homeFtAttempted = 0;
	private static Integer homeFtMade = 0;

	private static Integer homeOffensiveRebounds = 0;
	private static Integer homeDefensiveRebounds = 0;
	private static Integer homeTotalRebounds = 0;
	private static Integer homeAssists = 0;
	private static Integer homeSteals = 0;
	private static Integer homeBlocks = 0;
	private static Integer homeTurnovers = 0;
	private static Integer homeFouls = 0;
	private static Integer homePointsScored = 0;

	private static Integer awayFgAttempted = 0;
	private static Integer awayFgMade = 0;
	private static Integer awayFg3Attempted = 0;
	private static Integer awayFg3Made = 0;
	private static Integer awayFtAttempted = 0;
	private static Integer awayFtMade = 0;

	private static Integer awayOffensiveRebounds = 0;
	private static Integer awayDefensiveRebounds = 0;
	private static Integer awayTotalRebounds = 0;
	private static Integer awayAssists = 0;
	private static Integer awaySteals = 0;
	private static Integer awayBlocks = 0;
	private static Integer awayTurnovers = 0;
	private static Integer awayFouls = 0;
	private static Integer awayPointsScored = 0;

	private static Logger log = Logger.getLogger(CumulativeStatsProcessor.class);

	public static void generateCumulativeStatsSingleGame(String boxscoreUrl) throws Exception {

		try {
			Document doc = JsoupUtils.acquire(boxscoreUrl);

			if (doc == null || doc.toString().trim().length() == 0) {
				log.warn(boxscoreUrl + " -> There is no boxscore page data for this game: ");
				return;
			}

			// String gameId = Arrays.asList(boxscoreUrl.split("/")).stream().reduce((first,
			// second) -> second).get();

			Elements boxScoreTables = doc.getElementsByAttributeValue("class", "Wrapper");

			if (boxScoreTables == null || (boxScoreTables.first() == null && boxScoreTables.last() == null)) {
				log.warn("No box score for this game - perhaps it was cancelled or postponed?");
				return;
			}

			if (boxScoreTables.size() != 2) {
				log.warn("Inconsistent format for box score tables.... we are punting");
				return;
			}
			if (!collectStats(boxScoreTables.get(0), "away")) {
				return;
			}

			collectStats(boxScoreTables.get(1), "home");
		} catch (Exception e) {
			throw e;
		}
	}

	public static void generateCumulativeStats(Document doc, String gameId, String gameDate, BufferedWriter writer, /**/
			String roadTeamId, String roadConferenceId, String homeTeamId, String homeConferenceId) throws Exception {

		try {
			Elements boxScoreTables = doc.getElementsByAttributeValue("class", "Wrapper");

			if (boxScoreTables == null || (boxScoreTables.first() == null && boxScoreTables.last() == null)) {
				log.warn("No box score for this game - perhaps it was cancelled or postponed?");
				return;
			}

			if (boxScoreTables.size() != 2) {
				log.warn("Inconsistent format for box score tables.... we are punting");
				return;
			}

			if (!collectStats(boxScoreTables.get(0), "away")) {
				return;
			}

			if (collectStats(boxScoreTables.get(1), "home")) {
				writeToFile(gameId, "0", gameDate, writer, roadTeamId, roadConferenceId);
				writeToFile(gameId, "1", gameDate, writer, homeTeamId, homeConferenceId);
			}
		} catch (Exception e) {
			throw e;
		}
	}

	private static boolean collectStats(Element boxScoreTableEl, String homeAway) throws Exception {
		try {
			int maxDataIdx = JsoupUtils.getMaxDataIdxValue(boxScoreTableEl);
			if (maxDataIdx == -1) {
				log.warn("Cannot calculate maxDataIdx.... returning");
				return false;
			}

			if (maxDataIdx == 1) {
				log.warn("There appears to be no data: maxDataIdx has value = 1");
				return false;
			}

			Elements trElements = boxScoreTableEl.getElementsByAttributeValue("data-idx", String.valueOf(maxDataIdx - 1));
			if (trElements == null || trElements.size() == 0) {
				log.warn("Cannot acquire elements for data-idx = " + String.valueOf(maxDataIdx - 1));
				return false;
			}

			Elements divElements = trElements.first().getElementsByClass("Table__customHeader");
			if (divElements == null || divElements.first() == null) {
				// log.warn("No div elements");
				return false;
			}

			if (divElements.first().text().compareTo("team") == 0) {
				Element trElement = trElements.get(1);
				if (trElement == null) {
					log.warn("Cannot acquire trElement for cumulative results");
					return false;
				}

				Elements tdElements = trElement.getElementsByTag("td");
				if (tdElements == null) {
					log.warn("Cannot acquire tdElements for cumulative result");
					return false;
				}

				String test = tdElements.get(0).text() + tdElements.get(1).text();
				if (test.trim().contains("----")) {
					log.warn("There appears to be no cumulative boxscore data... we will skip this game");
					return false;
				}

				if (homeAway.compareTo("away") == 0) {
					String fgs = tdElements.get(1).text();
					awayFgAttempted = Integer.valueOf(fgs.split("-")[0]);
					awayFgMade = Integer.valueOf(fgs.split("-")[1]);

					String fg3s = tdElements.get(2).text();
					awayFg3Attempted = Integer.valueOf(fg3s.split("-")[0]);
					awayFg3Made = Integer.valueOf(fg3s.split("-")[1]);

					String fts = tdElements.get(3).text();
					awayFtAttempted = Integer.valueOf(fts.split("-")[0]);
					awayFtMade = Integer.valueOf(fts.split("-")[1]);

					awayOffensiveRebounds = Integer.valueOf(tdElements.get(4).text());
					awayDefensiveRebounds = Integer.valueOf(tdElements.get(5).text());
					awayTotalRebounds = Integer.valueOf(tdElements.get(6).text());
					awayAssists = Integer.valueOf(tdElements.get(7).text());
					awaySteals = Integer.valueOf(tdElements.get(8).text());
					awayBlocks = Integer.valueOf(tdElements.get(9).text());
					awayTurnovers = Integer.valueOf(tdElements.get(10).text());
					awayFouls = Integer.valueOf(tdElements.get(11).text());
					awayPointsScored = Integer.valueOf(tdElements.get(12).text());
				} else {
					String fgs = tdElements.get(1).text();
					homeFgAttempted = Integer.valueOf(fgs.split("-")[0]);
					homeFgMade = Integer.valueOf(fgs.split("-")[1]);

					String fg3s = tdElements.get(2).text();
					homeFg3Attempted = Integer.valueOf(fg3s.split("-")[0]);
					homeFg3Made = Integer.valueOf(fg3s.split("-")[1]);

					String fts = tdElements.get(3).text();
					homeFtAttempted = Integer.valueOf(fts.split("-")[0]);
					homeFtMade = Integer.valueOf(fts.split("-")[1]);

					homeOffensiveRebounds = Integer.valueOf(tdElements.get(4).text());
					homeDefensiveRebounds = Integer.valueOf(tdElements.get(5).text());
					homeTotalRebounds = Integer.valueOf(tdElements.get(6).text());
					homeAssists = Integer.valueOf(tdElements.get(7).text());
					homeSteals = Integer.valueOf(tdElements.get(8).text());
					homeBlocks = Integer.valueOf(tdElements.get(9).text());
					homeTurnovers = Integer.valueOf(tdElements.get(10).text());
					homeFouls = Integer.valueOf(tdElements.get(11).text());
					homePointsScored = Integer.valueOf(tdElements.get(12).text());
				}
			}
			return true;
		} catch (Exception e) {
			throw e;
		}
	}

	private static void writeToFile(String gameId, String homeAway, String gameDate, BufferedWriter writer, String teamId, String conferenceId) throws Exception {
		try {
			String idValue = gameId + homeAway;
			// write record to file
			if (homeAway.compareTo("0") == 0) {
				String data = "[id]=" + idValue /**/
						+ ",[gameId]=" + gameId/**/
						+ ",[gameDate]=" + gameDate/**/
						+ ",[isHomeTeam]=" + (homeAway == "0" ? "false" : "true")/**/
						+ ",[conferenceId]=" + conferenceId/**/
						+ ",[teamId]=" + teamId/**/
						+ ",[fgAttempted]=" + awayFgAttempted/**/
						+ ",[fgMade]=" + awayFgMade/**/
						+ ",[fg3Attempted]=" + awayFg3Attempted/**/
						+ ",[fg3Made]=" + awayFg3Made/**/
						+ ",[ftAttempted]=" + awayFtAttempted/**/
						+ ",[ftMade]=" + awayFtMade/**/
						+ ",[offensiveRebounds]=" + awayOffensiveRebounds/**/
						+ ",[defensiveRebounds]=" + awayDefensiveRebounds/**/
						+ ",[totalRebounds]=" + awayTotalRebounds/**/
						+ ",[assists]=" + awayAssists/**/
						+ ",[steals]=" + awaySteals/**/
						+ ",[blocks]=" + awayBlocks/**/
						+ ",[turnovers]=" + awayTurnovers/**/
						+ ",[Fouls]=" + awayFouls/**/
						+ ",[pointsScored]=" + awayPointsScored/**/
				;
				writer.write(data + "\n");
				// log.info(data);
			} else {
				String data = "[id]=" + idValue /**/
						+ ",[gameId]=" + gameId/**/
						+ ",[gameDate]=" + gameDate/**/
						+ ",[isHomeTeam]=" + (homeAway == "0" ? "false" : "true")/**/
						+ ",[conferenceId]=" + conferenceId/**/
						+ ",[teamId]=" + teamId/**/
						+ ",[fgAttempted]=" + homeFgAttempted/**/
						+ ",[fgMade]=" + homeFgMade/**/
						+ ",[fg3Attempted]=" + homeFg3Attempted/**/
						+ ",[fg3Made]=" + homeFg3Made/**/
						+ ",[ftAttempted]=" + homeFtAttempted/**/
						+ ",[ftMade]=" + homeFtMade/**/
						+ ",[offensiveRebounds]=" + homeOffensiveRebounds/**/
						+ ",[defensiveRebounds]=" + homeDefensiveRebounds/**/
						+ ",[totalRebounds]=" + homeTotalRebounds/**/
						+ ",[assists]=" + homeAssists/**/
						+ ",[steals]=" + homeSteals/**/
						+ ",[blocks]=" + homeBlocks/**/
						+ ",[turnovers]=" + homeTurnovers/**/
						+ ",[Fouls]=" + homeFouls/**/
						+ ",[pointsScored]=" + homePointsScored/**/
				;
				writer.write(data + "\n");
				// log.info(data);
			}
		} catch (Exception e) {
			throw e;
		}
	}
}
