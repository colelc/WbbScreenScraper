package process;

import java.io.BufferedWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import utils.JsoupUtils;

public class PlayByPlayProcessor {

	private static Logger log = Logger.getLogger(PlayByPlayProcessor.class);

	public static boolean processPlayByPlay(String url, /**/
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
			// log.info(doc.toString());
			if (doc == null) {
				log.warn("No html data for this play-by-play request");
				return false;
			}

			Elements pEls = doc.select("p");
			if (pEls != null) {
				Optional<Element> noPlayOpt = pEls.stream().filter(f -> f.text().compareTo("No Plays Available") == 0).findFirst();
				if (noPlayOpt.isPresent()) {
					log.info("There is no play-by-play data for this game -> " + url);
					return false;
				}
			}

			Elements scripts = doc.select("script");
			if (scripts.size() < 4) {
				log.warn("Cannot acquire play-by-play data from script elements");
				return false;
			}

			String scriptData = scripts.get(3).data();
			int playGrpsIx = scriptData.indexOf("\"playGrps\":[");
			if (playGrpsIx == -1) {
				log.warn("Unable to acquire playGrps data");
				return false;
			}

			List<String> quarters = Arrays.asList(scriptData.substring(playGrpsIx + 12).split("]")).stream().limit(4l).collect(Collectors.toList());
			for (String quarter : quarters) {
				if (quarter.trim().length() < 10) {
					log.info("stop");
				}
				String sanitized = (quarter.substring(1).charAt(0) != '[') ? "[" + quarter.substring(1) + "]" : quarter.substring(1) + "]";
				JsonArray jsonQuarter = new Gson().fromJson(sanitized, JsonArray.class);

				Iterator<JsonElement> playbyplayIterator = jsonQuarter.iterator();
				while (playbyplayIterator.hasNext()) {
					JsonElement play = playbyplayIterator.next();
					if (!play.isJsonObject()) {
						log.warn("Issue with play-by-play iteration");
						return false;
					}

					JsonObject playObj = play.getAsJsonObject();
					// playObj.keySet().forEach(k -> log.info(k + " -> " + playObj.get(k)));
					List<Integer> playerIdList = PlayByPlayElementProcessor.extractPlayerId(playObj, playerMap);
					if (playerIdList == null) {
						log.warn("Cannot identify players associated with a play");
						break;
					}
					String seconds = PlayByPlayElementProcessor.extractSeconds(playObj);
					String playerText = playObj.get("text").getAsString();

					if (playerIdList != null && seconds != null && playerText != null) {
						for (Integer playerId : playerIdList) {
							String idValue = gameId + gameDate + homeTeamId + homeTeamConferenceId + roadTeamId + roadTeamConferenceId + String.valueOf(playerId) + seconds;
							String data = "[id]=" + idValue/**/
									+ ",[gameId]=" + gameId/**/
									+ ",[gameDate]=" + gameDate/**/
									+ ",[homeTeamId]=" + homeTeamId/**/
									+ ",[homeTeamConferenceId]=" + homeTeamConferenceId/**/
									+ ",[roadTeamId]=" + roadTeamId/**/
									+ ",[roadTeamConferenceId]=" + roadTeamConferenceId/**/
									+ ",[seconds]=" + seconds/**/
									+ ",[playerId]=" + String.valueOf(playerId)/**/
									+ ",[play]=" + playerText/**/
							;
							writer.write(data + "\n");
							// log.info(data);
						}
					}

				}
			}
			return true;
		} catch (Exception e) {
			throw e;
		}
	}

}
