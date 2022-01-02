package process;

import java.io.BufferedWriter;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import utils.CalendarUtils;
import utils.ConfigUtils;
import utils.JsoupUtils;

public class ScheduleProcessor {

	private static int winterYear;
	private static int springYear;
	private static Logger log = Logger.getLogger(ScheduleProcessor.class);

	static {
		try {
			winterYear = ConfigUtils.getPropertyAsInt("year.winter");
			springYear = ConfigUtils.getPropertyAsInt("year.spring");
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
			System.exit(99);
		}
	}

	public static void generateScheduleFile(BufferedWriter scheduleWriter, String teamId, Element scheduleAnchor) throws Exception {

		Boolean homeGame = null;
		LocalDate gameDate = null;
		DayOfWeek gameDayOfWeek = null;
		String opponentTeamId = null;
		String gameId = null;

		LocalDate prevGameDate = null;

		try {
			Document doc = JsoupUtils.jsoupExtraction(ConfigUtils.getESPN_HOME() + scheduleAnchor.attr("href"));

			if (doc == null) {
				log.error("No content.... exiting");
				System.exit(99);
			}

			Elements games = JsoupUtils.nullElementCheck(doc.select("td.Table__TD"), "td.Table__TD");
			if (games == null || games.first() == null) {
				// continue;
			}

			for (int i = 0; i < games.size(); i++) {

				Element game = games.get(i);
				// log.info("-----------------------------------------------------------------------------------------------------------");
				// log.info(game.toString());

				if (JsoupUtils.filterByClass(game, "Table_Headers") || JsoupUtils.filterByClass(game, "Table__Title")) {
					continue;
				}

				// date of game .... delineates new row in the schedule for this team
				if (gameDate == null && gameDayOfWeek == null) {
					if (JsoupUtils.filterByTag(game, "span")) {
						gameDate = CalendarUtils.getThisInLocalDateFormat(winterYear, springYear, game.getElementsByTag("span").first().text());
						if (gameDate == null) {
							continue;
						}
						gameDayOfWeek = DayOfWeek.from(gameDate);

						if (prevGameDate == null) {
							prevGameDate = gameDate;
						} else if (prevGameDate != null && gameDate.compareTo(prevGameDate) != 0) {
							prevGameDate = gameDate;
							homeGame = null;
							opponentTeamId = null;
							gameId = null;
						} else {
							continue;
						}
					}
				}

				// home game or away game
				if (homeGame == null) {
					if (JsoupUtils.filterByAttributeAndValue(game, "class", "pr2")) {
						if (game.getElementsByAttributeValue("class", "pr2").first().text().compareTo("@") == 0) {
							homeGame = Boolean.FALSE;
						} else {
							homeGame = Boolean.TRUE;
						}
						// log.info("Home Game? " + homeGame.toString());
					}
				}

				// opponent teamId ...
				if (opponentTeamId == null) {
					if (JsoupUtils.filterByAttributeAndValue(game, "class", "tc pr2")) {
						Elements anchorTags = game.getElementsByAttributeValue("class", "tc pr2").first().getElementsByAttribute("href");
						if (anchorTags != null && anchorTags.first() != null) {
							String opposingTeamUrl = anchorTags.first().getElementsByAttribute("href").first().attr("href");
							opponentTeamId = Arrays.asList(opposingTeamUrl.split("/")).stream().filter(f -> NumberUtils.isCreatable(f)).collect(Collectors.toList()).get(0);
						}
					}
				}

				// gameId
				if (gameId == null) {
					if (JsoupUtils.filterByTag(game, "a")) {
						Elements anchorTags = game.getElementsByTag("a");
						if (anchorTags != null && anchorTags.first() != null) {
							String gameIdUrl = anchorTags.first().getElementsByAttribute("href").first().attr("href");
							if (gameIdUrl.contains("gameId")) {
								gameId = Arrays.asList(gameIdUrl.split("/")).stream().filter(f -> NumberUtils.isCreatable(f)).collect(Collectors.toList()).get(0);
							}
						}
					}
				}

				if (gameDate != null && gameDayOfWeek != null && homeGame != null && opponentTeamId != null && gameId != null) {

					String data = "[id]=" + gameId/**/
							+ ",[teamId]=" + teamId /**/
							+ ",[opponentTeamId]=" + opponentTeamId/**/
							+ ",[homeGame]=" + homeGame.toString()/**/
							+ ",[gameDayOfWeek]=" + gameDayOfWeek/**/
							+ ",[gameDate]=" + gameDate/**/
					;
					scheduleWriter.write(data + "\n");
					// log.info(data);

					prevGameDate = gameDate;
					gameDate = null;
					gameDayOfWeek = null;
				}

			}

		} catch (

		Exception e) {
			throw e;
		}

		return;
	}

}
