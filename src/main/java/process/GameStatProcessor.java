package process;

import java.io.BufferedWriter;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import utils.JsoupUtils;

public class GameStatProcessor {

	private static Logger log = Logger.getLogger(GameStatProcessor.class);

	public static void processGameStats(String url, /**/
			String gameId, /**/
			String gameDate, /**/
			String homeTeamId, /**/
			String homeTeamConferenceId, /**/
			String roadTeamId, /**/
			String roadTeamConferenceId, /**/
			BufferedWriter writer) throws Exception {

		try {
			Document doc = JsoupUtils.jsoupExtraction(url);
			if (doc == null) {
				log.warn("No html data for this box score request");
				return;
			}

			Elements boxScoreTables = JsoupUtils.nullElementCheck(doc.select("table.mod-data"), "table.mod-data");
			if (boxScoreTables == null) {
				log.warn("No box score for this game - perhaps it was cancelled or postponed?");
				return;
			}

			for (Element boxScoreTable : boxScoreTables) {
				if (!JsoupUtils.filterByAttributeAndValue(boxScoreTable, "class", "name")) {
					continue;
				}

				Elements playerRows = JsoupUtils.nullElementCheck(boxScoreTable.getElementsByTag("tr"), "tr");
				if (playerRows == null) {
					// log.error("No player rows");
					System.exit(99);
				}

				for (Element playerRow : playerRows) {

					Elements playerColumns = JsoupUtils.nullElementCheck(playerRow.getElementsByTag("td"), "td");
					if (playerColumns == null) {
						// log.warn("No columns found");
						// log.info(playerRow.toString());
						continue;
					}

					String playerId = null;
					String playerMinutes = null;
					String playerFgAttempted = null;
					String playerFgMade = null;
					String playerFg3Attempted = null;
					String playerFg3Made = null;
					String playerFtAttempted = null;
					String playerFtMade = null;
					String playerOffensiveRebounds = null;
					String playerDefensiveRebounds = null;
					String playerTotalRebounds = null;
					String playerAssists = null;
					String playerSteals = null;
					String playerBlocks = null;
					String playerTurnovers = null;
					String playerFouls = null;
					String playerPointsScored = null;

					for (Element playerColumn : playerColumns) {
						// log.info(playerColumn.toString());

						Elements ePlayerElements = JsoupUtils.nullElementCheck(playerColumn.getElementsByTag("a"), "a");
						if (ePlayerElements != null) {
							playerId = Arrays.asList(ePlayerElements.first().attr("href").split("/")).stream().filter(f -> NumberUtils.isCreatable(f)).collect(Collectors.toList()).get(0);
							// log.info(playerId);
							continue;
						}

						playerMinutes = playerMinutes == null ? JsoupUtils.extractionByAttributeAndValue(playerColumn, "class", "min") : playerMinutes;

						String[] madeOfAttempts = extractMadeOfAttempts(playerColumn, "class", "fg");
						if (madeOfAttempts != null) {
							playerFgMade = playerFgMade == null ? madeOfAttempts[0] : playerFgMade;
							playerFgAttempted = playerFgAttempted == null ? madeOfAttempts[1] : playerFgAttempted;
						}

						madeOfAttempts = extractMadeOfAttempts(playerColumn, "class", "3pt");
						if (madeOfAttempts != null) {
							playerFg3Made = playerFg3Made == null ? madeOfAttempts[0] : playerFg3Made;
							playerFg3Attempted = playerFg3Attempted == null ? madeOfAttempts[1] : playerFg3Attempted;
						}

						madeOfAttempts = extractMadeOfAttempts(playerColumn, "class", "ft");
						if (madeOfAttempts != null) {
							playerFtMade = playerFtMade == null ? madeOfAttempts[0] : playerFtMade;
							playerFtAttempted = playerFtAttempted == null ? madeOfAttempts[1] : playerFtAttempted;
						}

						playerOffensiveRebounds = playerOffensiveRebounds == null ? JsoupUtils.extractionByAttributeAndValue(playerColumn, "class", "oreb") : playerOffensiveRebounds;
						playerDefensiveRebounds = playerDefensiveRebounds == null ? JsoupUtils.extractionByAttributeAndValue(playerColumn, "class", "dreb") : playerDefensiveRebounds;
						playerTotalRebounds = playerTotalRebounds == null ? JsoupUtils.extractionByAttributeAndValue(playerColumn, "class", "reb") : playerTotalRebounds;
						playerAssists = playerAssists == null ? JsoupUtils.extractionByAttributeAndValue(playerColumn, "class", "ast") : playerAssists;
						playerSteals = playerSteals == null ? JsoupUtils.extractionByAttributeAndValue(playerColumn, "class", "stl") : playerSteals;
						playerBlocks = playerBlocks == null ? JsoupUtils.extractionByAttributeAndValue(playerColumn, "class", "blk") : playerBlocks;
						playerTurnovers = playerTurnovers == null ? JsoupUtils.extractionByAttributeAndValue(playerColumn, "class", "to") : playerTurnovers;
						playerFouls = playerFouls == null ? JsoupUtils.extractionByAttributeAndValue(playerColumn, "class", "pf") : playerFouls;
						playerPointsScored = playerPointsScored == null ? JsoupUtils.extractionByAttributeAndValue(playerColumn, "class", "pts") : playerPointsScored;

						if (playerId != null /**/
								&& playerMinutes != null /**/
								&& playerFgAttempted != null /**/
								&& playerFgMade != null /**/
								&& playerFg3Attempted != null /**/
								&& playerFg3Made != null /**/
								&& playerFtAttempted != null /**/
								&& playerFtMade != null /**/
								&& playerOffensiveRebounds != null /**/
								&& playerDefensiveRebounds != null /**/
								&& playerTotalRebounds != null /**/
								&& playerAssists != null /**/
								&& playerSteals != null /**/
								&& playerBlocks != null /**/
								&& playerTurnovers != null /**/
								&& playerFouls != null /**/
								&& playerPointsScored != null /**/
						) {
							// write record to file
							String idValue = gameId + gameDate + playerId + homeTeamId + homeTeamConferenceId + roadTeamId + roadTeamConferenceId;

							String data = "[id]=" + idValue /**/
									+ ",[gameId]=" + gameId/**/
									+ ",[gameDate]=" + gameDate/**/
									+ ",[playerId]=" + playerId/**/
									+ ",[homeTeamId]=" + homeTeamId/**/
									+ ",[homeTeamConferenceId]=" + homeTeamConferenceId/**/
									+ ",[roadTeamId]=" + roadTeamId/**/
									+ ",[roadTeamConferenceId]=" + roadTeamConferenceId/**/
									+ ",[playerMinutes]=" + playerMinutes/**/
									+ ",[playerFgAttempted]=" + playerFgAttempted/**/
									+ ",[playerFgMade]=" + playerFgMade/**/
									+ ",[playerFg3Attempted]=" + playerFg3Attempted/**/
									+ ",[playerFg3Made]=" + playerFg3Made/**/
									+ ",[playerFtAttempted]=" + playerFtAttempted/**/
									+ ",[playerFtMade]=" + playerFtMade/**/
									+ ",[playerOffensiveRebounds]=" + playerOffensiveRebounds/**/
									+ ",[playerDefensiveRebounds]=" + playerDefensiveRebounds/**/
									+ ",[playerTotalRebounds]=" + playerTotalRebounds/**/
									+ ",[playerAssists]=" + playerAssists/**/
									+ ",[playerSteals]=" + playerSteals/**/
									+ ",[playerBlocks]=" + playerBlocks/**/
									+ ",[playerTurnovers]=" + playerTurnovers/**/
									+ ",[playerFouls]=" + playerFouls/**/
									+ ",[playerPointsScored]=" + playerPointsScored/**/
							;
							writer.write(data + "\n");
							// log.info(data);

						}
					}
				}
			}
		} catch (Exception e) {
			throw e;
		}
	}

	private static String[] extractMadeOfAttempts(Element playerColumn, String attribute, String value) throws Exception {
		try {
			if (JsoupUtils.filterByAttributeAndValue(playerColumn, attribute, value)) {
				String text = playerColumn.getElementsByAttributeValue(attribute, value).first().text();
				String[] tokens = text.split("-");
				if (tokens != null && tokens.length == 2) {
					// String made = tokens[0].trim();
					// String attempted = tokens[1].trim();
					return tokens;
				}
			}
		} catch (Exception e) {
			throw e;
		}
		return null;
	}
}
