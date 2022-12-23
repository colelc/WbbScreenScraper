package process;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import utils.CalendarUtils;
import utils.ConfigUtils;
import utils.StringUtils;

public class PlayByPlayElementProcessor {

	private static List<String> skipWords;
	private static Logger log = Logger.getLogger(PlayByPlayElementProcessor.class);

	static {
		try {
			skipWords = Arrays.asList(ConfigUtils.getProperty("play.by.play.skip.words").split(",")).stream().map(m -> m.trim()).collect(Collectors.toList());
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
			System.exit(99);
		}
	}

	protected static List<Integer> extractPlayerIdsForThisPlay(JsonObject playObj, Map<Integer, Map<String, String>> playerMap) throws Exception {

		List<Integer> retList = new ArrayList<>();

		String playText = playObj.get("text").getAsString();
		if (!StringUtils.isPopulated(playText)) {
			return null;
		}

		try {
			List<String> sentences = new ArrayList<>(Arrays.asList(playText.split("\\.")));

			for (String sentence : sentences) {
				List<String> textTokens = Arrays.asList(sentence.split(" ")).stream()/**/
						.map(m -> m.replace(".", "").trim())/**/
						.filter(f -> f.trim().length() > 0)/**/
						.collect(Collectors.toList());

				if (textTokens.removeAll(skipWords)) {
					if (textTokens.size() == 0) {
						// log.info("No tokens from sentence -> " + sentence);
						return retList;
					}

					Integer playerId = parsePlayerId(textTokens, playerMap);
					if (playerId == null) {
						;// log.warn("Cannot acquire playerId from tokens -> " + textTokens.toString() +
							// ", and sentence is -> " + sentence);
					} else {
						retList.add(playerId);
					}
				} else {
					log.warn("No skip words to remove, what is this? " + textTokens.toString());
				}
			}
		} catch (Exception e) {
			throw e;
		}
		return retList;
	}

	private static Integer parsePlayerId(List<String> textTokens, Map<Integer, Map<String, String>> playerMap) throws Exception {
		try {
			String target = textTokens.stream().collect(Collectors.joining(""));
			// log.info(target);
			// log.info(playerMap.toString());
			Optional<Entry<Integer, Map<String, String>>> optEntry = playerMap.entrySet().stream()/**/
					.filter(idToMap -> {
						// log.info(idToMap.toString());
						String mapPlayerName = idToMap.getValue().get("playerFirstName").replace(" ", "")/**/
								+ idToMap.getValue().get("playerMiddleName").replace(" ", "")/**/
								+ idToMap.getValue().get("playerLastName").replace(" ", "")/**/;
						if (target.compareTo(mapPlayerName) == 0) {
							return true;
						} else {
							return false;
						}
					})/**/
					.findFirst();

			if (optEntry.isPresent()) {
				// String playerId = String.valueOf(optEntry.get().getKey());
				// log.info(playerId);
				return optEntry.get().getKey();
				// return Integer.valueOf(optMap.get().get("playerId"));
			} else {
				log.warn("Cannot acquire playerId: " + textTokens.toString());
				return null;
			}
		} catch (Exception e) {
			throw e;
		}

	}

	protected static String extractSeconds(JsonObject playObj) throws Exception {
		String seconds = null;
		try {
			// have to also know in what quarter of the game this play took place
			JsonElement periodEl = playObj.get("period");
			if (periodEl == null) {
				log.warn("Cannot acquire quarter in which a play took place.");
				return null;
			}
			JsonObject periodObj = periodEl.getAsJsonObject();
			int quarter = periodObj.get("number").getAsInt();

			// now get the clock display at the time of play
			JsonElement clockEl = playObj.get("clock");// .getAsJsonObject();
			if (clockEl == null) {
				log.warn("Cannot acquire time of play in play-by-play extraction");
				return null;
			}

			JsonObject clockObj = clockEl.getAsJsonObject();
			String hhmm = clockObj.get("displayValue").getAsString();

			// and calculate
			seconds = String.valueOf(CalendarUtils.playByPlayTimeTranslation(hhmm, quarter));
		} catch (Exception e) {

			throw e;
		}
		return seconds;
	}

}
