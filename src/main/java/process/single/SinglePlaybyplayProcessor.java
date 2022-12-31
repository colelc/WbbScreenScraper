package process.single;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;

import process.GamecastElementProcessor;
import process.GamecastProcessor;
import process.PlayByPlayProcessor;
import service.ConferenceTeamPlayerService;
import utils.ConfigUtils;
import utils.JsoupUtils;

public class SinglePlaybyplayProcessor {

	private static Logger log = Logger.getLogger(SinglePlaybyplayProcessor.class);

	public static void go() throws Exception {
		try {

			String singlePlaybyplayUrl = ConfigUtils.getProperty("single.playbyplay.url");
			singlePlaybyplayUrl = singlePlaybyplayUrl == null || singlePlaybyplayUrl.trim().length() == 0 ? null : singlePlaybyplayUrl;

			if (singlePlaybyplayUrl == null) {
				log.warn("There is no single play-by-play URL.");
				return;
			}

			String thisSeason = ConfigUtils.getProperty("season");
			if (thisSeason == null || thisSeason.trim().length() == 0) {
				log.error("We have a single playbyplay url to process, but we do not know for which season !  Adjust the season value");
				return;
			}

			ConferenceTeamPlayerService.loadDataFiles(true);
			processPlayByPlaySingleGame(singlePlaybyplayUrl);

		} catch (Exception e) {
			throw e;
		}
	}

	public static void processPlayByPlaySingleGame(String url) throws Exception {

		try {
			Document doc = JsoupUtils.acquire(url);
			if (doc == null) {
				log.warn("Cannot acquire document for this url: " + url);
				return;
			}
			String gameId = Arrays.asList(url.split("/")).stream().filter(f -> NumberUtils.isCreatable(f)).collect(Collectors.toList()).get(0);
			String gameDate = GamecastElementProcessor.extractGameDate(doc, gameId);

			String homeTeamId = GamecastProcessor.acquireTeamId(doc, "Gamestrip__Team--home");
			if (homeTeamId == null || homeTeamId.trim().length() == 0) {
				log.warn(url + " -> Cannot acquire the home team id from data-clubhouse-uid attribute value");
				// return;
			}

			String homeTeamConferenceId = GamecastProcessor.acquireTeamMapValue(homeTeamId, "conferenceId");

			String roadTeamId = GamecastProcessor.acquireTeamId(doc, "Gamestrip__Team--away");
			if (roadTeamId == null || roadTeamId.trim().length() == 0) {
				log.warn(url + " -> Cannot acquire the road team id from data-clubhouse-uid attribute value");
				// return;
			}

			String roadTeamConferenceId = GamecastProcessor.acquireTeamMapValue(roadTeamId, "conferenceId");

			PlayByPlayProcessor.processPlayByPlay(doc, /**/
					url, /**/
					gameId, gameDate, /**/
					homeTeamId, homeTeamConferenceId, roadTeamId, roadTeamConferenceId, /**/
					null);
		} catch (Exception e) {
			throw e;
		}
	}

}
