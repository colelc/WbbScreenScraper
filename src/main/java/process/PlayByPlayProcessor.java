package process;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import utils.CalendarUtils;
import utils.ConfigUtils;
import utils.JsoupUtils;
import utils.StringUtils;

public class PlayByPlayProcessor {

	private static List<String> skipWords;

	private static Logger log = Logger.getLogger(PlayByPlayProcessor.class);

	static {
		try {
			skipWords = Arrays.asList(ConfigUtils.getProperty("play.by.play.skip.words").split(",")).stream().map(m -> m.trim()).collect(Collectors.toList());
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
			System.exit(99);
		}
	}

	public static void processPlayByPlay(String url, /**/
			String gameId, /**/
			String gameDate, /**/
			String homeTeamId, /**/
			String homeTeamConferenceId, /**/
			String roadTeamId, /**/
			String roadTeamConferenceId, /**/
			Map<Integer, Map<String, String>> playerMap, /**/
			BufferedWriter writer) throws Exception {

		try {
			Document doc = JsoupUtils.jsoupExtraction(url);
			if (doc == null) {
				log.warn("No html data for this play-by-play request");
				return;
			}

			String seconds = null;
			String playByPlayText = null;

			int quarter = 1;
			String mmss = null;

			Elements tables = JsoupUtils.nullElementCheck(doc.select("table"), "table");
			if (tables == null) {
				return;
			}

			for (Element table : tables) {
				if (JsoupUtils.filterByAttributeAndValue(table, "class", "network")) {
					continue;
				}

				if (!JsoupUtils.filterByTag(table, "tbody")) {
					continue;
				}

				if (!JsoupUtils.filterByTag(table, "tr")) {
					continue;
				}

				Elements plays = table.getElementsByTag("tr");
				for (Element play : plays) {
					if (JsoupUtils.filterByTag(play, "th")) {
						continue;
					}

					Elements metaElements = JsoupUtils.nullElementCheck(play.getElementsByTag("td"), "td");
					if (metaElements == null) {
						continue;
					}

					for (Element metaElement : metaElements) {
						mmss = mmss == null ? JsoupUtils.extractionByAttributeAndValue(metaElement, "class", "time-stamp") : mmss;
						if (mmss != null && seconds == null) {
							seconds = seconds == null ? String.valueOf(CalendarUtils.playByPlayTimeTranslation(mmss, quarter)) : seconds;
						}

						playByPlayText = playByPlayText == null ? JsoupUtils.extractionByAttributeAndValue(metaElement, "class", "game-details") : playByPlayText;

						if (playByPlayText != null && playByPlayText.contains("End of") && playByPlayText.contains("Quarter")) {
							++quarter;
						}

						if (seconds != null && playByPlayText != null) {

							List<String> sentences = new ArrayList<>(Arrays.asList(playByPlayText.split("\\.")));

							for (String sentence : sentences) {
								Integer playerId = extractPlayerId(sentence.trim(), playerMap);
								if (playerId == null) {
									continue;
								}

								Map<String, String> map = playerMap.get(playerId);
								if (map != null) {
									String idValue = gameId + gameDate + homeTeamId + homeTeamConferenceId + roadTeamId + roadTeamConferenceId + seconds;
									String data = "[id]=" + idValue/**/
											+ ",[gameId]=" + gameId/**/
											+ ",[gameDate]=" + gameDate/**/
											+ ",[homeTeamId]=" + homeTeamId/**/
											+ ",[homeTeamConferenceId]=" + homeTeamConferenceId/**/
											+ ",[roadTeamId]=" + roadTeamId/**/
											+ ",[roadTeamConferenceId]=" + roadTeamConferenceId/**/
											+ ",[seconds]=" + seconds/**/
											+ ",[playerId]=" + playerId/**/
											+ ",[play]=" + sentence/**/
									;
									writer.write(data + "\n");
									// log.info(data);
								}
							}

							mmss = null;
							playByPlayText = null;
							seconds = null;

						}
					}
				}

				// we just want the game time and the game details
			}

		} catch (Exception e) {
			throw e;
		}
	}

	private static Integer extractPlayerId(String text, Map<Integer, Map<String, String>> playerMap) throws Exception {
		Integer playerId = null;

		if (!StringUtils.isPopulated(text)) {
			return playerId;
		}

		// log.info(text);
		try {
			List<String> textTokens = Arrays.asList(text.split(" ")).stream().map(m -> m.trim()).collect(Collectors.toList());

			if (textTokens.removeAll(skipWords) && textTokens.size() == 2) {
				Optional<Integer> opt = playerMap.entrySet().stream()/**/
						.filter(f -> f.getValue().get("playerName").compareTo(textTokens.stream().collect(Collectors.joining(" "))) == 0)/**/
						.map(m -> m.getKey())/**/
						.findFirst();

				if (opt.isPresent()) {
					playerId = opt.get();
				} else {
					// log.warn("Cannot acquire playerId: " + text);
				}
			} else {
				if (textTokens.size() > 0) {
					// log.warn("Insufficient filter: " + textTokens.toString());
				}
			}

		} catch (Exception e) {

			throw e;
		}
		return playerId;
	}

}
