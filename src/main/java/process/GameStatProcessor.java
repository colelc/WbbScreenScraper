package process;

import java.io.BufferedWriter;

import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import utils.JsoupUtils;

public class GameStatProcessor {

	private static Logger log = Logger.getLogger(GameStatProcessor.class);

	public static Document processGameStats(String url, /**/
			String gameId, /**/
			String gameDate, /**/
			String homeTeamId, /**/
			String homeTeamConferenceId, /**/
			String roadTeamId, /**/
			String roadTeamConferenceId, /**/
			BufferedWriter writer) throws Exception {

		try {
			Document doc = JsoupUtils.acquire(url);
			if (doc == null) {
				log.warn("No html data for this box score request");
				return null;
			}

			Elements boxScoreTables = doc.getElementsByAttributeValue("class", "Wrapper");

			if (boxScoreTables == null || (boxScoreTables.first() == null && boxScoreTables.last() == null)) {
				log.warn("No box score for this game - perhaps it was cancelled or postponed?");
				return doc;
			}

			for (Element boxScoreTableEl : boxScoreTables) {
				int maxDataIdx = JsoupUtils.getMaxDataIdxValue(boxScoreTableEl);
				if (maxDataIdx == -1) {
					continue;
				}

				for (int i = 1; i <= maxDataIdx; i++) {
					if (i == 6) {
						continue; // 1-5 are starters, 6 is the bench header
					}
					Elements els = boxScoreTableEl.getElementsByAttributeValue("data-idx", String.valueOf(i));
					if (els == null || els.size() == 0) {
						log.warn("Cannot acquire elements for data-idx = " + String.valueOf(i));
						continue;
					}

					if (els.size() != 2) {
						log.warn("Scraping issue with respect to player box scores... bailing");
						break;
					}

					String playerId = GamestatElementProcessor.extractPlayerId(els.get(0));
					if (playerId.compareTo("") == 0) {
						continue;
					}
					String playerMinutes = GamestatElementProcessor.extractPlayerData(els.get(1), 0);
					String playerFgAttempted = GamestatElementProcessor.extractPlayerAttempted(els.get(1), 1);
					String playerFgMade = GamestatElementProcessor.extractPlayerMade(els.get(1), 1);
					String playerFg3Attempted = GamestatElementProcessor.extractPlayerAttempted(els.get(1), 2);
					String playerFg3Made = GamestatElementProcessor.extractPlayerMade(els.get(1), 2);
					String playerFtAttempted = GamestatElementProcessor.extractPlayerAttempted(els.get(1), 3);
					String playerFtMade = GamestatElementProcessor.extractPlayerMade(els.get(1), 3);
					String playerOffensiveRebounds = GamestatElementProcessor.extractPlayerData(els.get(1), 4);
					String playerDefensiveRebounds = GamestatElementProcessor.extractPlayerData(els.get(1), 5);
					String playerTotalRebounds = GamestatElementProcessor.extractPlayerData(els.get(1), 6);
					String playerAssists = GamestatElementProcessor.extractPlayerData(els.get(1), 7);
					String playerSteals = GamestatElementProcessor.extractPlayerData(els.get(1), 8);
					String playerBlocks = GamestatElementProcessor.extractPlayerData(els.get(1), 9);
					String playerTurnovers = GamestatElementProcessor.extractPlayerData(els.get(1), 10);
					String playerFouls = GamestatElementProcessor.extractPlayerData(els.get(1), 11);
					String playerPointsScored = GamestatElementProcessor.extractPlayerData(els.get(1), 12);

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

			return doc;
		} catch (Exception e) {
			throw e;
		}
	}

}
