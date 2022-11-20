package process;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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

	protected static List<Integer> extractPlayerId(JsonObject playObj, Map<Integer, Map<String, String>> playerMap) throws Exception {

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

					Integer playerId = parse(textTokens, playerMap);
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

	private static Integer parse(List<String> textTokens, Map<Integer, Map<String, String>> playerMap) throws Exception {
		try {
			Optional<Integer> opt = playerMap.entrySet().stream()/**/
					.filter(f -> f.getValue().get("playerName").compareTo(textTokens.stream().collect(Collectors.joining(" "))) == 0)/**/
					.map(m -> m.getKey())/**/
					.findFirst();

			if (opt.isPresent()) {
				return opt.get();
			} else {
				// log.warn("Cannot acquire playerId: " + textTokens.toString());
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
