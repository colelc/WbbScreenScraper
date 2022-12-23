package process;

import java.io.BufferedWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.NumberUtils;
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

	public static void processPlayByPlaySingleGame(String url) throws Exception {

		try {
			Document doc = getDocument(url);
			if (doc == null) {
				log.warn("Cannot acquire document for this url: " + url);
				return;
			}
			String gameId = Arrays.asList(url.split("/")).stream().filter(f -> NumberUtils.isCreatable(f)).collect(Collectors.toList()).get(0);
			String gameDate = GamecastElementProcessor.extractGameDate(doc, gameId);

			String homeTeamId = GamecastProcessor.acquireTeamId(doc, "Gamestrip__Team--home");
			if (homeTeamId == null || homeTeamId.trim().length() == 0) {
				log.warn(url + " -> Cannot acquire the home team id from data-clubhouse-uid attribute value");
				return;
			}

			String homeTeamConferenceId = GamecastProcessor.acquireTeamMapValue(homeTeamId, "conferenceId");

			String roadTeamId = GamecastProcessor.acquireTeamId(doc, "Gamestrip__Team--away");
			if (roadTeamId == null || roadTeamId.trim().length() == 0) {
				log.warn(url + " -> Cannot acquire the road team id from data-clubhouse-uid attribute value");
				return;
			}

			String roadTeamConferenceId = GamecastProcessor.acquireTeamMapValue(roadTeamId, "conferenceId");

			processPlayByPlay(doc, /**/
					gameId, gameDate, /**/
					homeTeamId, homeTeamConferenceId, roadTeamId, roadTeamConferenceId, /**/
					DataProcessor.getPlayerMap(), /**/
					null);
		} catch (Exception e) {
			throw e;
		}
	}

	public static Document getDocument(String url) throws Exception {
		try {
			Document doc = JsoupUtils.jsoupExtraction(url);
			if (doc == null) {
				return null;
			}
			return doc;
		} catch (Exception e) {
			throw e;
		}
	}

	public static boolean processPlayByPlay(Document doc, /**/
			String gameId, /**/
			String gameDate, /**/
			String homeTeamId, /**/
			String homeTeamConferenceId, /**/
			String roadTeamId, /**/
			String roadTeamConferenceId, /**/
			Map<Integer, Map<String, String>> playerMap, /**/
			BufferedWriter writer) throws Exception {

		try {
			if (doc == null) {
				log.warn("Playbyplay document is null - cannot process this game");
				return false;
			}
			Elements pEls = doc.select("p");
			if (pEls != null) {
				Optional<Element> noPlayOpt = pEls.stream().filter(f -> f.text().compareTo("No Plays Available") == 0).findFirst();
				if (noPlayOpt.isPresent()) {
					log.info("There is no play-by-play data for this game ");
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

			// slice out the players from the player map who are on the road & home teams
			Map<Integer, Map<String, String>> players = playerMap.entrySet().stream()/**/
					.filter(entry -> entry.getValue().get("teamId").compareTo(roadTeamId) == 0 || entry.getValue().get("teamId").compareTo(homeTeamId) == 0)/**/
					.collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));

			for (Integer key : players.keySet()) {
				Map<String, String> player = players.get(key);
				log.info(player.toString());
			}

			// spin through each quarter
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
					List<Integer> playerIdList = PlayByPlayElementProcessor.extractPlayerIdsForThisPlay(playObj, players /* playerMap */);
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
							if (writer != null) {
								writer.write(data + "\n");
							} else {
								log.info(data);
							}
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
