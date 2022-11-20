package process;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class GamestatElementProcessor {

	private static Logger log = Logger.getLogger(GamestatElementProcessor.class);

	protected static String extractPlayerId(Element element) throws Exception {

		try {
			Elements playerAnchorTagEls = element.getElementsByTag("a");
			if (playerAnchorTagEls == null || playerAnchorTagEls.first() == null) {
				// log.warn("Cannot acquire playerId");
				return "";
			}

			Elements anchorHrefEls = playerAnchorTagEls.get(0).getElementsByAttribute("href");
			if (anchorHrefEls == null || anchorHrefEls.first() == null) {
				log.warn("Cannot acquire player anchor href");
				return "";
			}

			String playerHref = anchorHrefEls.first().attr("href");
			String playerId = Arrays.asList(playerHref.split("/")).stream().filter(f -> NumberUtils.isCreatable(f)).collect(Collectors.toList()).get(0);
			return playerId;
		} catch (Exception e) {
			throw e;
		}
	}

	protected static String extractPlayerAttempted(Element element, int ix) throws Exception {

		try {
			Elements playerStatEls = element.getElementsByTag("td");
			if (playerStatEls == null || playerStatEls.size() == 0) {
				log.warn("Cannot acquire player Attempted");
				return "";
			}

			Element playerAttemptedEl = playerStatEls.get(ix);
			if (playerAttemptedEl == null) {
				log.warn("Player attempted element is not obtainable");
				return "";
			}

			String[] tokens = playerAttemptedEl.text().split("-");
			if (tokens == null || tokens.length != 2) {
				log.warn("Cannot acquire tokens for attempted");
				return "";
			}

			String playerAttempted = tokens[0];
			return playerAttempted;

		} catch (Exception e) {
			throw e;
		}
	}

	protected static String extractPlayerMade(Element element, int ix) throws Exception {

		try {
			Elements playerStatEls = element.getElementsByTag("td");
			if (playerStatEls == null || playerStatEls.size() == 0) {
				log.warn("Cannot acquire player Made");
				return "";
			}

			Element playerMadeEl = playerStatEls.get(ix);
			if (playerMadeEl == null) {
				log.warn("Player made element is not obtainable");
				return "";
			}

			String[] tokens = playerMadeEl.text().split("-");
			if (tokens == null || tokens.length != 2) {
				log.warn("Cannot acquire tokens for made");
				return "";
			}

			String playerMade = tokens[1];
			return playerMade;

		} catch (Exception e) {
			throw e;
		}

	}

	protected static String extractPlayerData(Element element, int ix) throws Exception {

		try {
			Elements playerStatEls = element.getElementsByTag("td");
			if (playerStatEls == null || playerStatEls.size() == 0) {
				log.warn("Cannot acquire player data");
				return "";
			}

			Element playerEl = playerStatEls.get(ix);
			if (playerEl == null) {
				log.warn("Player element is not obtainable");
				return "";
			}

			String playerData = playerEl.text();
			return playerData;
		} catch (Exception e) {
			throw e;
		}
	}

}
