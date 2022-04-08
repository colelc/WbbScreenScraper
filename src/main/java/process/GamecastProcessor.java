package process;

import java.io.BufferedWriter;

import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import utils.CalendarUtils;
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

		String venueName = null;
		String venueCity = null;
		String venueState = null;
		String networkCoverage = null;
		String gameAttendance = null;
		String venueCapacity = null;
		String gamePercentageFull = null;
		String referees = null;

		try {
			Document doc = JsoupUtils.jsoupExtraction(gamecastUrl);

			Element gameInformationElement = JsoupUtils.nullElementCheck(doc.select("article.game-information"), "article.game-information").first();
			if (gameInformationElement == null) {
				log.info(gameId + ": There is no game information element");
				return;
			}

			Element gameDateTimeElement = gameInformationElement.getElementsByAttributeValue("class", "game-date-time").first();
			String gameTimeUTC = CalendarUtils.parseUTCTime(gameDateTimeElement.getElementsByAttribute("data-date").first().getElementsByTag("span").first().attr("data-date"));

			Elements attendanceElements = gameInformationElement.getElementsByAttributeValue("class", "game-info-note capacity");
			if (attendanceElements != null && attendanceElements.first() != null) {
				Element attendanceElement = attendanceElements.first();
				String content = attendanceElement.text();
				if (content.contains("Attendance")) {
					gameAttendance = gameAttendance == null ? attendanceElement.text() : gameAttendance;
					gameAttendance = gameAttendance != null ? parseValue(gameAttendance) : gameAttendance;
				}

				gameAttendance = gameAttendance == null ? "" : gameAttendance;
			} else {
				log.warn("No attendance information is available");
				gameAttendance = "";
			}

			Elements venueCapacityElements = gameInformationElement.getElementsByAttributeValue("class", "game-info-note capacity");
			if (venueCapacityElements != null && venueCapacityElements.first() != null) {
				Element venueCapacityElement = venueCapacityElements.last();
				venueCapacity = parseValue(venueCapacityElement.text());
			} else {
				log.warn("No venue capacity information is available");
				venueCapacity = "";
			}

			Elements gamePercentageElements = gameInformationElement.getElementsByAttributeValue("class", "percentage");
			if (gamePercentageElements != null && gamePercentageElements.first() != null) {
				Element gamePercentageElement = gamePercentageElements.first();
				gamePercentageFull = gamePercentageFull == null ? gamePercentageElement.text() : gamePercentageFull;
				gamePercentageFull = gamePercentageFull == null ? "" : gamePercentageFull;
			} else {
				log.warn("No attendance percentage full data is available");
				gamePercentageFull = "";
			}

			Element venueElement = gameInformationElement.getElementsByAttributeValue("class", "game-field").first();

			// if there is a picture element, get the venue name from it
			Elements pictureElements = venueElement.getElementsByAttributeValue("class", "caption-wrapper");
			if (pictureElements != null && pictureElements.first() != null) {
				venueName = pictureElements.first().getElementsByAttributeValue("class", "caption-wrapper").text();
			}

			Elements locationElements = gameInformationElement.getElementsByAttributeValue("class", "game-location");
			if (locationElements != null) {
				for (Element locationElement : locationElements) {
					String[] venueLocationTokens = locationElement.text().split(", ");
					if (venueLocationTokens.length == 2) {
						venueCity = venueLocationTokens[0];
						venueState = venueLocationTokens[1];
					} else if (venueLocationTokens.length == 1) {
						venueName = venueLocationTokens[0];
					}
				}
			} else {
				log.warn("No location elements available");
			}

			if (venueName == null || venueCity == null || venueState == null) {
				log.warn("Could not acquire venue data");
				venueName = venueName == null ? "" : venueName;
				venueCity = venueCity == null ? "" : venueCity;
				venueState = venueState == null ? "" : venueState;
			}

			Elements refereeElements = gameInformationElement.getElementsByAttributeValue("class", "game-info-note__container");
			if (refereeElements != null && refereeElements.first() != null) {
				referees = refereeElements.first().getElementsByTag("span").first().text();
				referees = referees == null ? "" : referees;
			} else {
				referees = "";
				log.warn("No referee data available");
			}

			Elements networkElements = gameInformationElement.getElementsByAttributeValue("class", "game-network");
			if (networkElements == null || networkElements.first() == null) {
				networkCoverage = "";
			} else {
				networkCoverage = networkElements.first().text();
				networkCoverage = parseValue(networkCoverage).replaceAll("\\|", ", ");
			}

			String status = JsoupUtils.nullElementCheck(doc.select("span.game-time"), "span.game-time").first().text();
			if (status == null) {
				log.info(gameId + ": There is no status element");
				return;
			}

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
				log.info(gameInformationElement.toString());
				return;
			}

			// log.info(data);
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
