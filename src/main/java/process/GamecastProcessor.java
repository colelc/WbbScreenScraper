package process;

import java.io.BufferedWriter;
import java.util.Arrays;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import service.ConferenceTeamPlayerService;
import utils.JsoupUtils;

public class GamecastProcessor {

	private static Logger log = Logger.getLogger(GamecastProcessor.class);

	public static void generateGamecastData(String gamecastUrl, /**/
			String gameId, /**/
			String gameDate, /**/
			String homeTeamId, /**/
			String homeTeamConferenceId, /**/
			String roadTeamId, /**/
			String roadTeamConferenceId, /**/
			BufferedWriter writer) throws Exception {

		try {
			Document doc = JsoupUtils.acquire(gamecastUrl);

			Elements gameInfoElements = JsoupUtils.nullElementCheck(doc.select("section.GameInfo"));// .first();
			if (gameInfoElements == null) {
				log.info(gameId + ": There is no game information element");
				return;
			}

			Element gameInfoElement = gameInfoElements.first();
			// log.info(gameInfoElement.toString());

			String gameTimeUtc = GamecastElementProcessor.extractGametime(gameInfoElement, gameId, gameDate);
			String networkCoverage = GamecastElementProcessor.extractNetworkCoverages(gameInfoElement, gameId, gameDate);
			String gameAttendance = GamecastElementProcessor.extractAttendance(gameInfoElement, gameId, gameDate);
			String venueCapacity = GamecastElementProcessor.extractVenueCapacity(gameInfoElement, gameId, gameDate);
			String venuePercentageFull = GamecastElementProcessor.extractVenuePercentageFull(gameInfoElement, gameId, gameDate);
			String venueName = GamecastElementProcessor.extractVenueName(gameInfoElement, gameId, gameDate);
			String venueCity = GamecastElementProcessor.extractVenueCity(gameInfoElement, gameId, gameDate);
			String venueState = GamecastElementProcessor.extractVenueState(gameInfoElement, gameId, gameDate);
			String referees = GamecastElementProcessor.extractReferees(gameInfoElement, gameId, gameDate);
			String status = GamecastElementProcessor.extractGameStatus(doc);

			// write record to file
			String idValue = gameId + homeTeamId + homeTeamConferenceId + roadTeamId + roadTeamConferenceId;

			String data = "[id]=" + idValue /**/
					+ ",[status]=" + status/**/
					+ ",[gameId]=" + gameId/**/
					+ ",[homeTeamId]=" + homeTeamId/**/
					+ ",[homeTeamConferenceId]=" + homeTeamConferenceId/**/
					+ ",[roadTeamId]=" + roadTeamId/**/
					+ ",[roadTeamConferenceId]=" + roadTeamConferenceId/**/
					+ ",[gameTimeUTC]=" + gameTimeUtc/**/
					+ ",[networkCoverage]=" + networkCoverage/**/
					+ ",[venueName]=" + venueName/**/
					+ ",[venueCity]=" + venueCity /**/
					+ ",[venueState]=" + venueState/**/
					+ ",[venueCapacity]=" + venueCapacity /**/
					+ ",[gameAttendance]=" + gameAttendance/**/
					+ ",[gamePercentageFull]=" + venuePercentageFull/**/
					+ ",[referees]=" + referees/**/
			;

			if (data.contains("null")) {
				log.warn("NULL value: " + data);
				return;
			}

			// log.info(data);
			writer.write(data + "\n");

		} catch (Exception e) {
			throw e;
		}
	}

	public static String acquireTeamId(Document doc, String className) throws Exception {

		try {
			Elements teamElements = doc.getElementsByClass(className);
			if (teamElements == null || teamElements.first() == null) {
				return "";
			}

			Elements teamEls = teamElements.first().getElementsByAttribute("data-clubhouse-uid");
			if (teamEls == null || teamEls.first() == null) {
				return "";
			}

			String teamId = Arrays.asList(teamEls.first().attr("data-clubhouse-uid").split(":")).stream().reduce((first, second) -> second).orElse("");
			if (teamId == null || teamId.trim().length() == 0) {
				return "";
			}

			return teamId;
		} catch (Exception e) {
			throw e;
		}
	}

	public static String acquireTeamMapValue(String teamId, String key) throws Exception {
		if (teamId == null || teamId.trim().length() == 0) {
			return "";
		}

		try {
			Map<String, String> map = ConferenceTeamPlayerService.getTeamMap().get(Integer.valueOf(teamId));
			if (map == null) {
				log.warn("Cannot acquire map for teamId = " + teamId);
				return "";
			}

			String value = map.get(key);
			if (value == null) {
				log.warn("Cannot acquire value for " + key + " from team map for team with teamId = " + teamId);
				return "";
			}
			return value;
		} catch (Exception e) {
			throw e;
		}
	}
}
