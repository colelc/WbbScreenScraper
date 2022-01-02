package process;

import java.io.BufferedWriter;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import utils.JsoupUtils;

public class GamecastProcessor {

	private static Logger log = Logger.getLogger(GamecastProcessor.class);

	public static void generateGamecastData(String gamecastUrl, /**/
			String gameId, /**/
			String gameName, /**/
			String gameDate, /**/
			String gameTimeUTC, /**/
			JsonObject venueObject, /**/
			JsonArray networkArray, /**/
			String homeTeamId, /**/
			String homeTeamConferenceId, /**/
			String roadTeamId, /**/
			String roadTeamConferenceId, /**/
			String status, /**/
			BufferedWriter writer) throws Exception {

		String venueName = null;
		String venueCity = null;
		String venueState = null;
		String networkCoverage = null;
		String gameAttendance = null;
		String venueCapacity = null;
		String gamePercentageFull = null;
		String referees = null;

		try {
			// log.info(gameInfoElement.toString());

			venueCity = venueObject.get("address").getAsJsonObject().get("city").getAsString();
			venueState = venueObject.get("address").getAsJsonObject().get("state").getAsString();
			venueName = venueObject.get("fullName").getAsString();
			venueCapacity = venueObject.get("capacity").getAsString();

			networkCoverage = networkArray.size() > 0 ? networkArray.get(0).getAsJsonObject().get("media").getAsJsonObject().get("shortName").getAsString() : "";

			Document doc = JsoupUtils.jsoupExtraction(gamecastUrl);
			Elements gameInformationElements = JsoupUtils.nullElementCheck(doc.select("article.game-information"), "article.game-information");
			if (gameInformationElements == null) {
				log.info(gameId + ": There are no game information elements");
				return;
			}

			Element gameInfoElement = gameInformationElements.first();

			Elements attendanceElements = gameInfoElement.getElementsByAttributeValue("class", "game-info-note capacity");
			if (attendanceElements != null && attendanceElements.first() != null) {
				String content = attendanceElements.first().text();
				if (content.contains("Attendance")) {
					gameAttendance = gameAttendance == null ? attendanceElements.first().text() : gameAttendance;
					gameAttendance = gameAttendance != null ? parseValue(gameAttendance) : gameAttendance;
				}
			}
			gameAttendance = gameAttendance == null ? "" : gameAttendance;

			Elements gamePercentageElements = gameInfoElement.getElementsByAttributeValue("class", "percentage");
			if (gamePercentageElements != null && gamePercentageElements.first() != null) {
				gamePercentageFull = gamePercentageFull == null ? gamePercentageElements.first().text() : gamePercentageFull;
			}
			gamePercentageFull = gamePercentageFull == null ? "" : gamePercentageFull;

			Elements refereeElements = gameInfoElement.getElementsByAttributeValue("class", "game-info-note__container");
			if (refereeElements != null && refereeElements.size() == 1) {
				referees = refereeElements.first().getElementsByTag("span").first().text();
			}
			referees = referees == null ? "" : referees;

			// write record to file
			String idValue = gameId + homeTeamId + homeTeamConferenceId + roadTeamId + roadTeamConferenceId;

			String data = "[id]=" + idValue /**/
					+ ",[status]=" + status/**/
					+ ",[gameId]=" + gameId/**/
					+ ",[homeTeamId]=" + homeTeamId/**/
					+ ",[homeTeamConferenceId]=" + homeTeamConferenceId/**/
					+ ",[roadTeamId]=" + roadTeamId/**/
					+ ",[roadTeamConferenceId]=" + roadTeamConferenceId/**/
					+ ",[gameTimeUTC]=" + gameTimeUTC/**/
					+ ",[networkCoverage]=" + networkCoverage/**/
					+ ",[venueName]=" + venueName/**/
					+ ",[venueCity]=" + venueCity /**/
					+ ",[venueState]=" + venueState/**/
					+ ",[venueCapacity]=" + venueCapacity /**/
					+ ",[gameAttendance]=" + gameAttendance/**/
					+ ",[gamePercentageFull]=" + (gamePercentageFull == null ? "" : gamePercentageFull)/**/
					+ ",[referees]=" + referees/**/
			;

			if (data.contains("null")) {
				log.warn("NULL value: ");
				log.info(data);
				log.info(gameInfoElement.toString());
				return;
			}

			writer.write(data + "\n");

		} catch (Exception e) {
			throw e;
		}
	}

	public static void processGamecast(String gameId, /**/
			String teamId, /**/
			String conferenceId, /**/
			String opponentTeamId, /**/
			String opponentConferenceId, /**/
			Map.Entry<Integer, Map<String, String>> gamePlayedMap, /**/
			Document doc, /**/
			BufferedWriter writer) throws Exception {

		String gameTime = null;
		String gameVenue = null;
		String gameLocation = null;
		String networkCoverage = null;
		String gameAttendance = null;
		String gameAttendanceCapacity = null;
		String gamePercentageFull = null;
		String referees = null;

		try {
			if (doc == null) {
				return;
			}

			Elements gameInformationElements = JsoupUtils.nullElementCheck(doc.select("article.game-information"), "article.game-information");
			if (gameInformationElements == null) {
				log.info("There are no game information elements");
				return;
			}

			Element gameInfoElement = gameInformationElements.first();
			// log.info(gameInfoElement.toString());

			gameLocation = gameLocation == null ? JsoupUtils.extractionByAttributeAndValue(gameInfoElement, "class", "icon-font-before icon-location-solid-before") : gameLocation;
			gameVenue = gameVenue == null ? JsoupUtils.extractionByAttributeAndValue(gameInfoElement, "class", "game-location") : gameVenue;
			gameVenue = gameVenue == null ? JsoupUtils.extractionByAttributeAndValue(gameInfoElement, "class", "caption-wrapper") : gameVenue;
			gameTime = gameTime == null ? doc.select("span[data-date]").first().attr("data-date") : gameTime;

			networkCoverage = networkCoverage == null ? JsoupUtils.extractionByAttributeAndValue(gameInfoElement, "class", "game-network") : networkCoverage;
			networkCoverage = networkCoverage != null ? parseValue(networkCoverage) : null;
			networkCoverage = networkCoverage == null ? "NO" : networkCoverage;

			Elements attendanceElements = gameInfoElement.getElementsByAttributeValue("class", "game-info-note capacity");
			if (attendanceElements != null && attendanceElements.first() != null) {
				gameAttendance = gameAttendance == null ? attendanceElements.first().text() : gameAttendance;
				gameAttendance = gameAttendance != null ? parseValue(gameAttendance) : gameAttendance;

				if (attendanceElements.size() >= 2) {
					gameAttendanceCapacity = gameAttendanceCapacity == null ? attendanceElements.eq(1).first().text() : gameAttendanceCapacity;
					gameAttendanceCapacity = gameAttendanceCapacity != null ? parseValue(gameAttendanceCapacity) : gameAttendanceCapacity;
				}
			}
			gameAttendance = gameAttendance == null ? "" : gameAttendance;
			gameAttendanceCapacity = gameAttendanceCapacity == null ? "" : gameAttendanceCapacity;

			Elements gamePercentageElements = gameInfoElement.getElementsByAttributeValue("class", "percentage");
			if (gamePercentageElements != null && gamePercentageElements.first() != null) {
				gamePercentageFull = gamePercentageFull == null ? gamePercentageElements.first().text() : gamePercentageFull;
			}
			gamePercentageFull = gamePercentageFull == null ? "" : gamePercentageFull;

			Elements refereeElements = gameInfoElement.getElementsByAttributeValue("class", "game-info-note__container");
			if (refereeElements != null && refereeElements.size() == 1) {
				referees = refereeElements.first().getElementsByTag("span").first().text();
			}
			referees = referees == null ? "" : referees;

			// write record to file
			String idValue = gameId + teamId + conferenceId + opponentTeamId + opponentConferenceId;

			String data = "[id]=" + idValue /**/
					+ ",[gameId]=" + gameId/**/
					+ ",[teamId]=" + teamId/**/
					+ ",[conferenceId]=" + conferenceId/**/
					+ ",[opponentTeamId]=" + opponentTeamId/**/
					+ ",[opponentConferenceId]=" + opponentConferenceId/**/
					+ ",[gameTime]=" + gameTime/**/
					+ ",[gameVenue]=" + gameVenue/**/
					+ ",[gameLocation]=" + gameLocation /**/
					+ ",[networkCoverage]=" + networkCoverage/**/
					+ ",[gameAttendance]=" + gameAttendance/**/
					+ ",[gameAttendanceCapacity]=" + gameAttendanceCapacity /**/
					+ ",[gamePercentageFull]=" + (gamePercentageFull == null ? "" : gamePercentageFull)/**/
					+ ",[referees]=" + referees/**/
			;

			if (data.contains("null")) {
				log.warn("NULL value: ");
				log.info(data);
				log.info(gameInfoElement.toString());
				return;
			}

			writer.write(data + "\n");

		} catch (Exception e) {
			throw e;
		}
	}

	private static String parseValue(String in) {
		String retValue = null;

		if (in != null) {
			String[] tokens = in.split(":");
			if (tokens != null && tokens.length == 2) {
				retValue = tokens[1].trim().replace(",", "");
			}
		}
		return retValue;
	}
}
