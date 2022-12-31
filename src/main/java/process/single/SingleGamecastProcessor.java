package process.single;

import java.util.Arrays;

import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import process.CumulativeStatsProcessor;
import process.GamecastElementProcessor;
import process.GamecastProcessor;
import service.ConferenceTeamPlayerService;
import utils.ConfigUtils;
import utils.JsoupUtils;

public class SingleGamecastProcessor {

	private static Logger log = Logger.getLogger(SingleGamecastProcessor.class);

	public static void go() throws Exception {
		try {
			String singleGamecastUrl = ConfigUtils.getProperty("single.gamecast.url");
			singleGamecastUrl = singleGamecastUrl == null || singleGamecastUrl.trim().length() == 0 ? null : singleGamecastUrl;

			if (singleGamecastUrl == null) {
				log.warn("There is no gamecast url specified.  I have nothing to do.");
				return;
			}

			ConferenceTeamPlayerService.loadDataFiles(true);
			generateGamecastDataSingleUrl(singleGamecastUrl);
			CumulativeStatsProcessor.generateCumulativeStatsSingleGame(singleGamecastUrl);

		} catch (Exception e) {
			throw e;
		}
	}

	public static void generateGamecastDataSingleUrl(String gamecastUrl) throws Exception {

		log.info("We are processing a single gamecast url: " + gamecastUrl);

		try {
			Document doc = JsoupUtils.acquire(gamecastUrl);

			if (doc == null || doc.toString().trim().length() == 0) {
				log.warn(gamecastUrl + " -> There is no gamecast page data for this game: ");
				return;
			}

			String gameId = Arrays.asList(gamecastUrl.split("/")).stream().reduce((first, second) -> second).get();
			String gameDate = GamecastElementProcessor.extractGameDate(doc, gameId);
			if (gameDate == null) {
				log.warn("Cannot acquire game date for gameId -> " + gameId);
				return;
			}

			String homeTeamId = GamecastProcessor.acquireTeamId(doc, "Gamestrip__Team--home");
			if (homeTeamId == null || homeTeamId.trim().length() == 0) {
				log.warn(gamecastUrl + " -> Cannot acquire the home team id from data-clubhouse-uid attribute value");
				return;
			}

			String homeTeamConferenceId = GamecastProcessor.acquireTeamMapValue(homeTeamId, "conferenceId");
			String homeTeamName = GamecastProcessor.acquireTeamMapValue(homeTeamId, "teamName");

			String roadTeamId = GamecastProcessor.acquireTeamId(doc, "Gamestrip__Team--away");
			if (roadTeamId == null || roadTeamId.trim().length() == 0) {
				log.warn(gamecastUrl + " -> Cannot acquire the road team id from data-clubhouse-uid attribute value");
				return;

			}

			String roadTeamConferenceId = GamecastProcessor.acquireTeamMapValue(roadTeamId, "conferenceId");
			String roadTeamName = GamecastProcessor.acquireTeamMapValue(roadTeamId, "teamName");

			String title = roadTeamName.compareTo("") == 0 ? "NA"
					: roadTeamName /**/
							+ " (" /**/
							+ (roadTeamConferenceId.compareTo("") == 0 ? "NA" : ConferenceTeamPlayerService.getConferenceMap().get(Integer.valueOf(roadTeamConferenceId)).get("shortName") + ")")/**/
							+ " at " /**/
							+ (homeTeamId.compareTo("") == 0 ? "NA" : homeTeamName)/**/
							+ (homeTeamConferenceId.compareTo("") == 0 ? "NA"
									: " (" + ConferenceTeamPlayerService.getConferenceMap().get(Integer.valueOf(homeTeamConferenceId)).get("shortName") + ")");

			log.info(title);

			Element gameInfoElement = GamecastElementProcessor.extractGameInfoElement(doc, gameId);
			if (gameInfoElement == null) {
				log.warn("Cannot acquire game info element for gameId -> " + gameId);
				return;
			}

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

}
