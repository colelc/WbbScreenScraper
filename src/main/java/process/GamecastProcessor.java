package process;

import java.io.BufferedWriter;

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

}
