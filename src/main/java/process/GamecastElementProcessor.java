package process;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import utils.CalendarUtils;
import utils.JsoupUtils;

public class GamecastElementProcessor {

	private static Logger log = Logger.getLogger(GamecastElementProcessor.class);

	public static String extractGameStatus(Document doc) throws Exception {

		String status = "";

		try {
			Elements elements = doc.select("div.ScoreCell__Time Gamestrip__Time h9 clr-gray-01");
			if (elements == null) {
				// log.warn("There is no element from which to extract game status");
				return "Final";
			}

			if (elements == null || elements.first() == null) {
				// log.warn("Cannot acquire game status - will assume Final");
				return "Final";
			}

			status = elements.first().text();
			if (status == null || status.trim().length() == 0) {
				return "Final";
			} else {
				return status;
			}
		} catch (Exception e) {
			throw e;
		}

	}

	public static String extractReferees(Element gameInfoElement, String gameId, String gameDate) throws Exception {
		if (gameInfoElement == null) {
			log.warn("There is no game Info element");
			return "";
		}

		String referees = "";

		try {
			Elements refEls = gameInfoElement.getElementsByAttributeValue("class", "GameInfo__List__Item");
			if (refEls == null || refEls.size() == 0) {
				log.warn(gameDate + " -> " + gameId + " " + "Cannot acquire referees");
				return "";
			}

			List<String> refereeList = new ArrayList<>();
			for (Element refEl : refEls) {
				refereeList.add(refEl.text());
			}
			referees = refereeList.stream().collect(Collectors.joining(", "));

		} catch (Exception e) {
			throw e;
		}

		return referees;
	}

	public static String extractVenueState(Element gameInfoElement, String gameId, String gameDate) throws Exception {
		if (gameInfoElement == null) {
			log.warn("There is no game Info element");
			return "";
		}

		String venueState = "";

		try {
			Elements els = gameInfoElement.getElementsByAttributeValue("class", "Location__Text");
			if (els == null || els.first() == null) {
				log.warn(gameDate + " -> " + gameId + " " + "Cannot acquire venue state");
				return "";
			}

			Element venueStateElement = els.first();

			String[] tokens = venueStateElement.text().split(",");
			if (tokens == null || tokens.length != 2) {
				log.warn(gameDate + " -> " + gameId + " " + "Cannot acquire venue state");
				return "";
			}

			venueState = tokens[1].trim();
		} catch (Exception e) {
			throw e;
		}
		return venueState;
	}

	public static String extractVenueCity(Element gameInfoElement, String gameId, String gameDate) throws Exception {
		if (gameInfoElement == null) {
			log.warn("There is no game Info element");
			return "";
		}

		String venueCity = "";

		try {
			Elements els = gameInfoElement.getElementsByAttributeValue("class", "Location__Text");
			if (els == null || els.first() == null) {
				log.warn(gameDate + " -> " + gameId + " " + "Cannot acquire venue city");
				return "";
			}

			Element venueCityElement = els.first();

			String[] tokens = venueCityElement.text().split(",");
			if (tokens == null || tokens.length != 2) {
				log.warn(gameDate + " -> " + gameId + " " + "Cannot acquire venue city");
				return "";
			}

			venueCity = tokens[0].trim();
		} catch (Exception e) {
			throw e;
		}
		return venueCity;
	}

	public static String extractVenueName(Element gameInfoElement, String gameId, String gameDate) throws Exception {
		if (gameInfoElement == null) {
			log.warn("There is no game Info element");
			return "";
		}

		String venueName = "";

		try {
			Elements els = gameInfoElement.getElementsByClass("GameInfo__Location__Name");
			if (els == null || els.first() == null) {
				// try another tag
				els = gameInfoElement.getElementsByClass("GameInfo__Location__Name--noImg");
				if (els == null || els.first() == null) {
					log.warn(gameDate + " -> " + gameId + " " + "Cannot acquire venue name");
					return "";
				}
			}

			Element venueNameElement = els.first();
			venueName = venueNameElement.text().trim();
		} catch (Exception e) {
			throw e;
		}
		return venueName;
	}

	public static String extractVenuePercentageFull(Element gameInfoElement, String gameId, String gameDate) throws Exception {
		if (gameInfoElement == null) {
			log.warn("There is no game Info element");
			return "";
		}

		String pctFull = "";

		try {
			Elements els = gameInfoElement.getElementsByAttributeValue("class", "n3 flex-expand Attendance__Percentage");
			if (els == null || els.first() == null) {
				log.warn(gameDate + " -> " + gameId + " " + "Cannot acquire venue percentage full");
				return "";
			}

			Element percentageFullElement = els.first();
			pctFull = percentageFullElement.text().trim();
		} catch (Exception e) {
			throw e;
		}
		return pctFull;
	}

	public static String extractVenueCapacity(Element gameInfoElement, String gameId, String gameDate) throws Exception {
		if (gameInfoElement == null) {
			log.warn("There is no game Info element");
			return "";
		}

		String capacity = "";

		try {
			Elements els = gameInfoElement.getElementsByAttributeValue("class", "Attendance__Capacity h10");
			if (els == null || els.first() == null) {
				els = gameInfoElement.getElementsByClass("Attendance__Capacity");
				if (els == null || els.first() == null) {
					log.warn(gameDate + " -> " + gameId + " " + "Cannot acquire venue capacity");
					return "";
				}

				capacity = els.first().text().replace("Capacity:", "").replace(",", "").trim();
				return capacity;
			}

			Element capacityElement = els.first();
			capacity = capacityElement.text().replace("Capacity:", "").replace(",", "").trim();
		} catch (Exception e) {
			throw e;
		}
		return capacity;
	}

	public static String extractAttendance(Element gameInfoElement, String gameId, String gameDate) throws Exception {
		if (gameInfoElement == null) {
			log.warn("There is no game Info element");
			return "";
		}

		String attendance = "";

		try {
			Elements els = gameInfoElement.getElementsByAttributeValue("class", "Attendance__Numbers");
			if (els == null || els.first() == null) {
				log.warn(gameDate + " -> " + gameId + " " + "Cannot acquire attendance");
				return "";
			}

			Element attendanceElement = els.first();
			attendance = attendanceElement.text().replace("Attendance:", "").replace(",", "").trim();
		} catch (Exception e) {
			throw e;
		}
		return attendance;
	}

	public static String extractNetworkCoverages(Element gameInfoElement, String gameId, String gameDate) throws Exception {
		if (gameInfoElement == null) {
			log.warn("There is no game Info element");
			return "";
		}

		String networkCoverages = "";

		try {
			Elements networkCoverageElements = gameInfoElement.getElementsByAttributeValue("class", "n8 GameInfo__Meta");// .first();
			if (networkCoverageElements == null || networkCoverageElements.first() == null) {
				log.warn(gameDate + " -> " + gameId + " " + "Cannot acquire network coverage elements");
				return "";
			}

			Elements els = networkCoverageElements.first().getElementsByTag("span");
			if (els == null || els.first() == null || els.size() < 2) { // don't want first = last
				log.warn(gameDate + " -> " + gameId + " " + "Cannot acquire network coverages");
				return "";
			}

			Element networkCoveragesEl = els.last();
			networkCoverages = networkCoveragesEl.text().toString().replace("Coverage:", "").trim();

		} catch (Exception e) {
			throw e;
		}
		return networkCoverages;
	}

	public static String extractGameDate(Document doc, String gameId) throws Exception {

		Element gameInfoElement = extractGameInfoElement(doc, gameId);

		if (gameInfoElement == null) {
			log.warn("There is no game Info element");
			return null;
		}

		String gameDate = "";

		try {
			Element dtElement = gameInfoElement.getElementsByAttributeValue("class", "n8 GameInfo__Meta").first();

			Elements els = dtElement.getElementsByTag("span");
			if (els == null || els.first() == null) {
				log.warn("Cannot acquire game date");
				return "";
			}

			// Element gameDateEl = els.last();
			List<Element> spanList = els.stream().filter(el -> el.text().contains(":")).collect(Collectors.toList());
			if (spanList == null || spanList.size() == 0) {
				log.warn("Cannot acquire gameDate from span list");
				return "";
			}

			Element gameDateEl = spanList.get(0);

			List<String> dateElements = Arrays.asList(gameDateEl.text().split("\\,")).stream()/**/
					.filter(f -> !f.contains(":"))/**/
					.map(m -> m.trim())/**/
					.collect(Collectors.toList());
			if (dateElements == null || dateElements.size() != 2) {
				log.warn("Cannot acquire date elements needed to calculate game date");
				return null;
			}

			List<String> monthDayTokens = Arrays.asList(dateElements.get(0).split(" ")).stream().filter(f -> f.trim().length() > 0).collect(Collectors.toList());
			if (monthDayTokens == null || monthDayTokens.size() != 2) {
				log.warn("Cannot parse month and day tokens for calculating game date");
				return null;
			}

			int mm = CalendarUtils.computeMonth(monthDayTokens.get(0)).getValue();
			String month = "";
			if (mm < 10) {
				month = "0" + String.valueOf(mm);
			} else {
				month = String.valueOf(mm);
			}

			int dd = Integer.valueOf(monthDayTokens.get(1)).intValue();
			String day = "";
			if (dd < 10) {
				day = "0" + String.valueOf(dd);
			} else {
				day = String.valueOf(dd);
			}

			String year = dateElements.get(1);
			gameDate = year + month + day;

		} catch (Exception e) {
			throw e;
		}
		return gameDate;
	}

	public static Element extractGameInfoElement(Document doc, String gameId) throws Exception {
		try {
			Elements gameInfoElements = JsoupUtils.nullElementCheck(doc.select("section.GameInfo"));// .first();
			if (gameInfoElements == null) {
				log.info(gameId + ": There is no game information element");
				return null;
			}

			Element gameInfoElement = gameInfoElements.first();

			if (gameInfoElement == null) {
				log.warn("There is no game Info element");
				return null;
			}
			return gameInfoElement;
		} catch (Exception e) {
			throw e;
		}
	}

	public static String extractGametime(Element gameInfoElement, String gameId, String gameDate) throws Exception {
		if (gameInfoElement == null) {
			log.warn("There is no game Info element");
			return "";
		}

		String gameTimeUtc = "";

		try {
			Element dtElement = gameInfoElement.getElementsByAttributeValue("class", "n8 GameInfo__Meta").first();

			Elements els = dtElement.getElementsByTag("span");
			if (els == null || els.first() == null) {
				log.warn(gameDate + " -> " + gameId + " " + "Cannot acquire game time");
				return "";
			}

			Element gameTimeEl = els.first();
			Optional<String> opt = Arrays.asList(gameTimeEl.text().split(",")).stream().filter(f -> f.contains("AM") || f.contains("PM")).findFirst();
			if (opt.isEmpty()) {
				log.warn(gameDate + " -> " + gameId + " " + "Cannot acquire game time from game time element");
				return "";
			}

			gameTimeUtc = CalendarUtils.parseUTCTime2(opt.get());

		} catch (Exception e) {
			throw e;
		}
		return gameTimeUtc;
	}

}
