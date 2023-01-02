package process;

import java.io.BufferedWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import service.ConferenceTeamPlayerService;
import utils.ConfigUtils;
import utils.StringUtils;

public class PlayByPlayProcessor {

	private static List<String> skipWords;
	private static Logger log = Logger.getLogger(PlayByPlayProcessor.class);

	static {
		try {
			skipWords = Arrays.asList(ConfigUtils.getProperty("play.by.play.skip.words").split(",")).stream().map(m -> m.toLowerCase().trim()).collect(Collectors.toList());
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
			System.exit(99);
		}
	}

	public static boolean processPlayByPlay(Document doc, /**/
			String gameUrl, /**/
			String gameId, /**/
			String gameDate, /**/
			String homeTeamId, /**/
			String homeTeamConferenceId, /**/
			String roadTeamId, /**/
			String roadTeamConferenceId, /**/
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
			Map<Integer, Map<String, String>> players = ConferenceTeamPlayerService.getPlayerMap().entrySet().stream()/**/
					.filter(entry -> entry.getValue().get("teamId").compareTo(homeTeamId) == 0 || entry.getValue().get("teamId").compareTo(roadTeamId) == 0)/**/
					.collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));

			// build out the team name tokens (for eliminating team play by play items)
			Set<String> teamNameTokens = ConferenceTeamPlayerService.getTeamNamesAsTokens(roadTeamId, homeTeamId);

			// begin to iterate through the play-by-plays, by quarter
			List<String> quarters = Arrays.asList(scriptData.substring(playGrpsIx + 12).split("]")).stream().limit(4l).collect(Collectors.toList());
			for (String quarter : quarters) {
				if (quarter.trim().length() < 10) {
					log.warn("We appear to be missing play-by-play data for this game - we can continue, but the play-by-play will be incomplete");
					continue;
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
					// log.info(playObj.toString());
					// playObj.keySet().forEach(k -> log.info(k + " -> " + playObj.get(k)));
					List<Integer> playerIdList = PlayByPlayElementProcessor.extractPlayerIdsForThisPlay(gameUrl, playObj, /**/
							players, /**/
							skipWords, /**/
							homeTeamConferenceId, roadTeamConferenceId, /**/
							gameId, gameDate);

					if (playerIdList == null || playerIdList.size() == 0) {

						Set<String> playTokens = Arrays.asList(playObj.get("text").getAsString().split(" ")).stream()/**/
								.map(m -> StringUtils.specialCharStripper(m).toLowerCase().trim())/**/
								.filter(f -> f.trim().length() > 0)/**/
								.collect(Collectors.toSet());

						playTokens.removeAll(teamNameTokens);
						playTokens.removeAll(skipWords);
						if (!playTokens.isEmpty()) {
							log.warn("Cannot identify players associated with a play: " + playObj.get("text"));
						}
						// break;
						continue;
					}
					String seconds = PlayByPlayElementProcessor.extractSeconds(playObj);
					String playerText = playObj.get("text").getAsString();

					if (playerIdList != null && seconds != null && playerText != null) {
						for (Integer playerId : playerIdList) {
							String playerIdString = playerId == -1 ? "" : String.valueOf(playerId);
							String idValue = (gameId + gameDate + homeTeamId + homeTeamConferenceId + roadTeamId + roadTeamConferenceId + playerIdString + seconds).trim();

							String data = "[id]=" + idValue/**/
									+ ",[gameId]=" + gameId.trim()/**/
									+ ",[gameDate]=" + gameDate.trim()/**/
									+ ",[homeTeamId]=" + homeTeamId.trim()/**/
									+ ",[homeTeamConferenceId]=" + homeTeamConferenceId.trim()/**/
									+ ",[roadTeamId]=" + roadTeamId.trim()/**/
									+ ",[roadTeamConferenceId]=" + roadTeamConferenceId.trim()/**/
									+ ",[seconds]=" + seconds/**/
									+ ",[playerId]=" + playerIdString/**/
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
