package process;

import java.io.BufferedWriter;
import java.util.Arrays;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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
			Document doc = JsoupUtils.jsoupExtraction(gamecastUrl);

			Elements gameInfoElements = JsoupUtils.nullElementCheck(doc.select("section.GameInfo"), "section.GameInfo");// .first();
			if (gameInfoElements == null || gameInfoElements.first() == null) {
				log.info(gameId + ": There is no game information element");
				return;
			}

			Element gameInfoElement = gameInfoElements.first();
			// log.info(gameInfoElement.toString());

			String gameTimeUtc = GamecastElementProcessor.extractGametime(gameInfoElement);
			String networkCoverage = GamecastElementProcessor.extractNetworkCoverages(gameInfoElement);
			String gameAttendance = GamecastElementProcessor.extractAttendance(gameInfoElement);
			String venueCapacity = GamecastElementProcessor.extractVenueCapacity(gameInfoElement);
			String venuePercentageFull = GamecastElementProcessor.extractVenuePercentageFull(gameInfoElement);
			String venueName = GamecastElementProcessor.extractVenueName(gameInfoElement);
			String venueCity = GamecastElementProcessor.extractVenueCity(gameInfoElement);
			String venueState = GamecastElementProcessor.extractVenueState(gameInfoElement);
			String referees = GamecastElementProcessor.extractReferees(gameInfoElement);
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
				log.warn("NULL value: ");
				log.info(data);
				log.info(gameInfoElement.toString());
				return;
			}

			// log.info(data);
			writer.write(data + "\n");

		} catch (Exception e) {
			throw e;
		}
	}

	public static void generateGamecastDataSingleUrl(String gamecastUrl) throws Exception {
		// String gameId, /**/
		// String gameDate, /**/
		// String homeTeamId, /**/
		// String homeTeamConferenceId, /**/
		// String roadTeamId, /**/
		// String roadTeamConferenceId, /**/
		// BufferedWriter writer) throws Exception {

		log.info("We are processing a single gamecast url: " + gamecastUrl);

		try {
			Document doc = JsoupUtils.jsoupExtraction(gamecastUrl);

			if (doc == null || doc.toString().trim().length() == 0) {
				log.warn(gamecastUrl + " -> There is no gamecast page data for this game: ");
				return;
			}

			String gameId = Arrays.asList(gamecastUrl.split("/")).stream().reduce((first, second) -> second).get();

			Elements gameInfoElements = JsoupUtils.nullElementCheck(doc.select("section.GameInfo"), "section.GameInfo");// .first();
			if (gameInfoElements == null || gameInfoElements.first() == null) {
				log.info(gameId + ": There is no game information element");
				return;
			}

			Element gameInfoElement = gameInfoElements.first();
			String gameDate = GamecastElementProcessor.extractGameDate(gameInfoElement);

			String homeTeamId = acquireTeamId(gamecastUrl, doc, "Gamestrip__Team--home");
			if (homeTeamId == null || homeTeamId.trim().length() == 0) {
				log.warn(gamecastUrl + " -> Cannot acquire the home team id from data-clubhouse-uid attribute value");
				return;
			}

			String homeTeamConferenceId = acquireTeamMapValue(homeTeamId, "conferenceId");
			String homeTeamName = acquireTeamMapValue(homeTeamId, "teamName");

			String roadTeamId = acquireTeamId(gamecastUrl, doc, "Gamestrip__Team--away");
			if (roadTeamId == null || roadTeamId.trim().length() == 0) {
				log.warn(gamecastUrl + " -> Cannot acquire the road team id from data-clubhouse-uid attribute value");
				return;
			}

			String roadTeamConferenceId = acquireTeamMapValue(roadTeamId, "conferenceId");
			String roadTeamName = acquireTeamMapValue(roadTeamId, "teamName");

			String title = roadTeamName.compareTo("") == 0 ? "NA"
					: roadTeamName /**/
							+ " (" /**/
							+ (roadTeamConferenceId.compareTo("") == 0 ? "NA" : DataProcessor.getConferenceMap().get(Integer.valueOf(roadTeamConferenceId)).get("shortName") + ")")/**/
							+ " at " /**/
							+ (homeTeamId.compareTo("") == 0 ? "NA" : homeTeamName)/**/
							+ (homeTeamConferenceId.compareTo("") == 0 ? "NA" : " (" + DataProcessor.getConferenceMap().get(Integer.valueOf(homeTeamConferenceId)).get("shortName") + ")")
			// +
			// DataProcessor.getConferenceMap().get(Integer.valueOf(homeTeamConferenceId)).get("shortName")
			// + ")";
			;

			log.info(title);
			// log.info(homeTeamElements.toString());

			// log.info(doc.toString());
			// log.info(gameInfoElement.toString());

			String gameTimeUtc = GamecastElementProcessor.extractGametime(gameInfoElement);
			String networkCoverage = GamecastElementProcessor.extractNetworkCoverages(gameInfoElement);
			String gameAttendance = GamecastElementProcessor.extractAttendance(gameInfoElement);
			String venueCapacity = GamecastElementProcessor.extractVenueCapacity(gameInfoElement);
			String venuePercentageFull = GamecastElementProcessor.extractVenuePercentageFull(gameInfoElement);
			String venueName = GamecastElementProcessor.extractVenueName(gameInfoElement);
			String venueCity = GamecastElementProcessor.extractVenueCity(gameInfoElement);
			String venueState = GamecastElementProcessor.extractVenueState(gameInfoElement);
			String referees = GamecastElementProcessor.extractReferees(gameInfoElement);
			String status = GamecastElementProcessor.extractGameStatus(doc);

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

			log.info(data);
		} catch (Exception e) {
			throw e;
		}
	}

	public static String acquireTeamId(String gamecastUrl, Document doc, String className) throws Exception {

		try {
			Elements teamElements = doc.getElementsByClass(className);
			if (teamElements == null || teamElements.first() == null) {
				return null;
			}

			Elements teamEls = teamElements.first().getElementsByAttribute("data-clubhouse-uid");
			if (teamEls == null || teamEls.first() == null) {
				return null;
			}

			String teamId = Arrays.asList(teamEls.first().attr("data-clubhouse-uid").split(":")).stream().reduce((first, second) -> second).orElse("");
			if (teamId == null || teamId.trim().length() == 0) {
				return null;
			}

			return teamId;
		} catch (Exception e) {
			throw e;
		}
	}

	public static String acquireTeamMapValue(String teamId, String key) throws Exception {
		try {
			Map<String, String> map = DataProcessor.getTeamMap().get(Integer.valueOf(teamId));
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
