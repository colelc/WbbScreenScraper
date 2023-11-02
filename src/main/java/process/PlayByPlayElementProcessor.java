package process;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import service.DirtyDataService;
import utils.CalendarUtils;
import utils.ConfigUtils;
import utils.StringUtils;

public class PlayByPlayElementProcessor {

	private static List<String> headacheNames;
	// private static List<String> alreadyFiguredThisOut = new ArrayList<>();
	private static Logger log = Logger.getLogger(PlayByPlayElementProcessor.class);

	static {
		try {
			headacheNames = Arrays.asList(ConfigUtils.getProperty("player.names.that.give.me.headaches").split(",")).stream().map(m -> m.toLowerCase().trim()).collect(Collectors.toList());
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
			System.exit(99);
		}
	}

	protected static List<Integer> extractPlayerIdsForThisPlay(String gameUrl, JsonObject playObj, /**/
			Map<Integer, Map<String, String>> playerMap, /**/
			List<String> skipWords, /**/
			String homeConferenceId, String roadConferenceId, /**/
			String gameId, String gameDate) throws Exception {

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
			List<String> sentences = new ArrayList<>(Arrays.asList(playText.toLowerCase().split("assisted by")));
			if (playText.toLowerCase().contains("assisted by")) {
				String lastSentence = sentences.stream().reduce((first, second) -> second).orElse(null);
				if (lastSentence == null) {
					log.warn("Code logic error with dealing with assists in play by play processor");
				} else {
					lastSentence = "Assisted by " + lastSentence;
					sentences.remove(sentences.size() - 1);
					sentences.add(lastSentence);
				}
			}

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

					Integer playerId = parsePlayerId(gameUrl, textTokens, playerMap, gameId, gameDate);
					if (playerId != null) {
						retList.add(playerId);
						continue;
					}

					if (roadConferenceId == null || roadConferenceId.trim().compareTo("") == 0) {
						// log.warn("Deliberate skip - this is likely a road team player not in Division
						// 1: " + textTokens.toString());
						retList.add(-1);
					}

				}

			}
		} catch (Exception e) {
			throw e;
		}
		return retList;
	}

	private static Integer parsePlayerId(String gameUrl, List<String> textTokens, Map<Integer, Map<String, String>> playerMap, String gameId, String gameDate) throws Exception {
		try {
			String target = textTokens.stream().collect(Collectors.joining(""));

			Set<Integer> candidateSet = playerMap.entrySet().stream()/**/
					.filter(idToMap -> nameParse(idToMap, target, gameId, gameDate, true))/**/
					.map(m -> m.getKey())/**/
					.collect(Collectors.toSet());

			if (candidateSet == null || candidateSet.size() == 0) {
				// log.warn("Cannot acquire playerId: " + target + " -> " + gameUrl);
				DirtyDataService.getDirtyDataMap().put(target, gameUrl);
				return null;
			}

			if (candidateSet.size() == 1) {
				return candidateSet.stream().reduce((prev, next) -> next).orElse(null);
			}

			for (Integer playerId : candidateSet) {
				Entry<Integer, Map<String, String>> thisPlayerMap = playerMap.entrySet().stream().filter(entry -> entry.getKey().compareTo(playerId) == 0).findFirst().get();
				if (nameParse(thisPlayerMap, target, gameId, gameDate, false)) {
					log.info("PRECISE MATCH: " + textTokens.toString() + " -> " + thisPlayerMap.toString());
					return playerId;
				}
			}

			log.warn("no player could be identified");
			return null;
		} catch (Exception e) {
			throw e;
		}
	}

	private static boolean nameParse(Entry<Integer, Map<String, String>> idToMap, String target, String gameId, String gameDate, boolean includeFuzzyMatch) {
		// log.info(target + " <---> " + idToMap.toString());
		String first = idToMap.getValue().get("playerFirstName");
		String middle = idToMap.getValue().get("playerMiddleName");
		String last = idToMap.getValue().get("playerLastName");
		String full = idToMap.getValue().get("playerName");

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

		if (includeFuzzyMatch) {
			if (fuzzyNameParse(target, first, middle, last)) {
				return true;
			} else {
				// put back in the play tokens? try again?
				// log.warn(idToMap.toString());
			}
		}
		return false;
	}

	private static boolean fuzzyNameParse(String target, String first, String middle, String last) {
		// try flipping middle and last name
		String name = StringUtils.specialCharStripper(last + first);
		if (target.toLowerCase().compareTo(name.toLowerCase()) == 0) {
			log.warn("(fuzzy) " + target + " -> flipped middle and last names -> " + last + " " + middle);
			return true;
		}

		// try flipping first and last name
		name = StringUtils.specialCharStripper(last + first);

		if (target.toLowerCase().compareTo(name.toLowerCase()) == 0) {
			log.warn("(fuzzy) " + target + " -> flipped first and last names -> " + last + " " + first);
			return true;
		}

		// try if 1st name starts with ...
		if (first.length() >= 3 && target.toLowerCase().startsWith(first.substring(0, 3).toLowerCase())) {
			name = first.substring(0, 3) + StringUtils.specialCharStripper(middle + last);

			if (target.toLowerCase().compareTo(name.toLowerCase()) == 0) {
				log.warn("(fuzzy) " + target + " -> did fuzzy match on first name -> " + first + " " + last);
				return true;
			}
		}

		// if first letter of first name + last name ....
//		if (!headacheNames.contains(target)) {
//			name = first.substring(0, 1) + last;
//			if (target.endsWith(last.toLowerCase()) && target.startsWith(first.substring(0, 1).toLowerCase())) {
//				log.warn("(fuzzy) " + target + " -> used 1st letter of first name + last name -> " + first + " " + last);
//				return true;
//			}
//		}

		// first + middle
		name = StringUtils.specialCharStripper(first + middle);

		if (target.toLowerCase().compareTo(name.toLowerCase()) == 0) {
			log.warn("(fuzzy) " + target + " -> used first +  middle names -> " + name);
			return true;
		}

		return false;
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
