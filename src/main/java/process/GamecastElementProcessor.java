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

public class GamecastElementProcessor {

	private static Logger log = Logger.getLogger(GamecastElementProcessor.class);

	protected static String extractGameStatus(Document doc) throws Exception {

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

	protected static String extractReferees(Element gameInfoElement) throws Exception {
		if (gameInfoElement == null) {
			log.warn("There is no game Info element");
			return "";
		}

		String referees = "";

		try {
			Elements refEls = gameInfoElement.getElementsByAttributeValue("class", "GameInfo__List__Item");
			if (refEls == null || refEls.size() == 0) {
				log.warn("Cannot acquire referees");
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

	protected static String extractVenueState(Element gameInfoElement) throws Exception {
		if (gameInfoElement == null) {
			log.warn("There is no game Info element");
			return "";
		}

		String venueState = "";

		try {
			Elements els = gameInfoElement.getElementsByAttributeValue("class", "Location__Text");
			if (els == null || els.first() == null) {
				log.warn("Cannot acquire venue state");
				return "";
			}

			Element venueStateElement = els.first();

			String[] tokens = venueStateElement.text().split(",");
			if (tokens == null || tokens.length != 2) {
				log.warn("Cannot acquire venue state");
				return "";
			}

			venueState = tokens[1].trim();
		} catch (Exception e) {
			throw e;
		}
		return venueState;
	}

	protected static String extractVenueCity(Element gameInfoElement) throws Exception {
		if (gameInfoElement == null) {
			log.warn("There is no game Info element");
			return "";
		}

		String venueCity = "";

		try {
			Elements els = gameInfoElement.getElementsByAttributeValue("class", "Location__Text");
			if (els == null || els.first() == null) {
				log.warn("Cannot acquire venue city");
				return "";
			}

			Element venueCityElement = els.first();

			String[] tokens = venueCityElement.text().split(",");
			if (tokens == null || tokens.length != 2) {
				log.warn("Cannot acquire venue city");
				return "";
			}

			venueCity = tokens[0].trim();
		} catch (Exception e) {
			throw e;
		}
		return venueCity;
	}

	protected static String extractVenueName(Element gameInfoElement) throws Exception {
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
					log.warn("Cannot acquire venue name");
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

	protected static String extractVenuePercentageFull(Element gameInfoElement) throws Exception {
		if (gameInfoElement == null) {
			log.warn("There is no game Info element");
			return "";
		}

		String pctFull = "";

		try {
			Elements els = gameInfoElement.getElementsByAttributeValue("class", "n3 flex-expand Attendance__Percentage");
			if (els == null || els.first() == null) {
				log.warn("Cannot acquire venue percentage full");
				return "";
			}

			Element percentageFullElement = els.first();
			pctFull = percentageFullElement.text().trim();
		} catch (Exception e) {
			throw e;
		}
		return pctFull;
	}

	protected static String extractVenueCapacity(Element gameInfoElement) throws Exception {
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
					log.warn("Cannot acquire venue capacity");
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

	protected static String extractAttendance(Element gameInfoElement) throws Exception {
		if (gameInfoElement == null) {
			log.warn("There is no game Info element");
			return "";
		}

		String attendance = "";

		try {
			Elements els = gameInfoElement.getElementsByAttributeValue("class", "Attendance__Numbers");
			if (els == null || els.first() == null) {
				log.warn("Cannot acquire attendance");
				return "";
			}

			Element attendanceElement = els.first();
			attendance = attendanceElement.text().replace("Attendance:", "").replace(",", "").trim();
		} catch (Exception e) {
			throw e;
		}
		return attendance;
	}

	protected static String extractNetworkCoverages(Element gameInfoElement) throws Exception {
		if (gameInfoElement == null) {
			log.warn("There is no game Info element");
			return "";
		}

		String networkCoverages = "";

		try {
			Elements networkCoverageElements = gameInfoElement.getElementsByAttributeValue("class", "n8 GameInfo__Meta");// .first();
			if (networkCoverageElements == null || networkCoverageElements.first() == null) {
				log.warn("Cannot acquire network coverage elements");
				return "";
			}

			Elements els = networkCoverageElements.first().getElementsByTag("span");
			if (els == null || els.first() == null || els.size() < 2) { // don't want first = last
				log.warn("Cannot acquire network coverages");
				return "";
			}

			Element networkCoveragesEl = els.last();
			networkCoverages = networkCoveragesEl.text().toString().replace("Coverage:", "").trim();

		} catch (Exception e) {
			throw e;
		}
		return networkCoverages;
	}

	protected static String extractGameDate(Element gameInfoElement) throws Exception {
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

			Element gameDateEl = els.last();
			List<String> dateElements = Arrays.asList(gameDateEl.text().split("\\,")).stream().filter(f -> !f.contains(":")).collect(Collectors.toList());
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
//			Optional<String> opt = Arrays.asList(gameDateEl.text().split(",")).stream().findFirst();
//			if (opt.isEmpty()) {
//				log.warn("Cannot acquire game time from game time element");
//				return "";
//			}

//			gameDate = CalendarUtils.parseUTCTime2(opt.get());

		} catch (Exception e) {
			throw e;
		}
		return gameDate;
	}

	protected static String extractGametime(Element gameInfoElement) throws Exception {
		if (gameInfoElement == null) {
			log.warn("There is no game Info element");
			return "";
		}

		String gameTimeUtc = "";

		try {
			Element dtElement = gameInfoElement.getElementsByAttributeValue("class", "n8 GameInfo__Meta").first();

			Elements els = dtElement.getElementsByTag("span");
			if (els == null || els.first() == null) {
				log.warn("Cannot acquire game time");
				return "";
			}

			Element gameTimeEl = els.first();
			Optional<String> opt = Arrays.asList(gameTimeEl.text().split(",")).stream().filter(f -> f.contains("AM") || f.contains("PM")).findFirst();
			if (opt.isEmpty()) {
				log.warn("Cannot acquire game time from game time element");
				return "";
			}

			gameTimeUtc = CalendarUtils.parseUTCTime2(opt.get());

		} catch (Exception e) {
			throw e;
		}
		return gameTimeUtc;
	}

}
