package process;

import java.io.BufferedWriter;
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
	private static List<String> headacheNames;
	private static List<String> alreadyFiguredThisOut = new ArrayList<>();
	private static Logger log = Logger.getLogger(PlayByPlayElementProcessor.class);

	static {
		try {
			skipWords = Arrays.asList(ConfigUtils.getProperty("play.by.play.skip.words").split(",")).stream().map(m -> m.toLowerCase().trim()).collect(Collectors.toList());
			headacheNames = Arrays.asList(ConfigUtils.getProperty("player.names.that.give.me.headaches").split(",")).stream().map(m -> m.toLowerCase().trim()).collect(Collectors.toList());
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
			System.exit(99);
		}
	}

	protected static List<Integer> extractPlayerIdsForThisPlay(String gameUrl, JsonObject playObj, Map<Integer, Map<String, String>> playerMap, /**/
			BufferedWriter dirtyDataWriter) throws Exception {

		List<Integer> retList = new ArrayList<>();

		JsonElement el = playObj.get("text");
		if (el == null) {
			log.warn("Element not populated");
			return null;
		}

		String playText = el.getAsString();
		if (!StringUtils.isPopulated(playText)) {
			return null;
		}

		try {
			List<String> sentences = new ArrayList<>(Arrays.asList(playText.split("\\.")));

			for (String sentence : sentences) {
				List<String> textTokens = Arrays.asList(sentence.split(" ")).stream()/**/
						.map(m -> StringUtils.specialCharStripper(m).toLowerCase().trim())/**/
						.filter(f -> f.trim().length() > 0)/**/
						.collect(Collectors.toList());

				// then remove skip words
				if (textTokens.removeAll(skipWords)) {
					if (textTokens.size() == 0) {
						// log.info("No tokens from sentence -> " + sentence);
						return retList;
					}

					Integer playerId = parsePlayerId(gameUrl, textTokens, playerMap, dirtyDataWriter);
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

	private static Integer parsePlayerId(String gameUrl, List<String> textTokens, Map<Integer, Map<String, String>> playerMap, BufferedWriter dirtyDataWriter) throws Exception {
		try {
			String target = textTokens.stream().collect(Collectors.joining(""));
			// log.info(target);
			// log.info(playerMap.toString());
			Optional<Entry<Integer, Map<String, String>>> optEntry = playerMap.entrySet().stream()/**/
					.filter(idToMap -> nameParse(idToMap, target))/**/
					.findFirst();

			if (optEntry.isPresent()) {
				// String playerId = String.valueOf(optEntry.get().getKey());
				// log.info(playerId);
				return optEntry.get().getKey();
				// return Integer.valueOf(optMap.get().get("playerId"));
			} else {
				if (!alreadyFiguredThisOut.contains(textTokens.toString())) {
					alreadyFiguredThisOut.add(textTokens.toString());
					log.warn("Cannot acquire playerId: " + textTokens.toString());
					if (dirtyDataWriter != null) {
						dirtyDataWriter.write(target + " -> " + gameUrl + "\n");
					}
				}

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

	private static boolean nameParse(Entry<Integer, Map<String, String>> idToMap, String target) {
		// log.info(target + " <---> " + idToMap.toString());
		String first = idToMap.getValue().get("playerFirstName");
		String middle = idToMap.getValue().get("playerMiddleName");
		String last = idToMap.getValue().get("playerLastName");
		String full = idToMap.getValue().get("playerName");

//		if (target.toLowerCase().endsWith("bond")) {
//			log.info("");
//		}

		// try 1st middle last
		String name = StringUtils.specialCharStripper(first + middle + last);

		if (target.toLowerCase().compareTo(name.toLowerCase()) == 0) {
			return true;
		}

		// try full name
		name = StringUtils.specialCharStripper(full);
		if (target.toLowerCase().compareTo(name.toLowerCase()) == 0) {
			return true;
		}
		// try flipping middle and last name
		name = StringUtils.specialCharStripper(first + last + middle);

		if (target.toLowerCase().compareTo(name.toLowerCase()) == 0) {
			if (!alreadyFiguredThisOut.contains(target)) {
				alreadyFiguredThisOut.add(target);
				log.warn(target + " -> using for " + name);
			}
			return true;
		}

		// try flipping first and last name
		name = StringUtils.specialCharStripper(last + first);

		if (target.toLowerCase().compareTo(name.toLowerCase()) == 0) {
			if (!alreadyFiguredThisOut.contains(target)) {
				alreadyFiguredThisOut.add(target);
				log.warn(target + " -> using for " + name);
			}
			return true;
		}

		// try if 1st name starts with ...
		if (first.length() >= 3 && target.toLowerCase().startsWith(first.substring(0, 3).toLowerCase())) {
			name = first.substring(0, 3) + StringUtils.specialCharStripper(middle + last);

			if (target.toLowerCase().compareTo(name.toLowerCase()) == 0) {
				if (!alreadyFiguredThisOut.contains(target)) {
					alreadyFiguredThisOut.add(target);
					log.warn(target + " -> using for " + name);
				}
				return true;
			}
		}

		// if first letter of first name + last name ....
		if (!headacheNames.contains(target)) {
			name = first.substring(0, 1) + last;
			if (target.endsWith(last.toLowerCase()) && target.startsWith(first.substring(0, 1).toLowerCase())) {
				if (!alreadyFiguredThisOut.contains(target)) {
					alreadyFiguredThisOut.add(target);
					log.warn(target + " -> using for " + name);
				}
				return true;
			}
		}

		// first + middle
		name = StringUtils.specialCharStripper(first + middle);

		if (target.toLowerCase().compareTo(name.toLowerCase()) == 0) {
			if (!alreadyFiguredThisOut.contains(target)) {
				alreadyFiguredThisOut.add(target);
				log.warn(target + " -> using for " + name);
			}

			return true;
		}

		return false;
	}

}
