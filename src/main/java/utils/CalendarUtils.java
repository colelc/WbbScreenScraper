package utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

public class CalendarUtils {
	private static Logger log = Logger.getLogger(CalendarUtils.class);
	private static List<Month> winterMonths = new ArrayList<>(Arrays.asList(Month.OCTOBER, Month.NOVEMBER, Month.DECEMBER));
	private static List<Month> springMonths = new ArrayList<>(Arrays.asList(Month.JANUARY, Month.FEBRUARY, Month.MARCH, Month.APRIL));

	private static final int secondsPerMinute = 60;
	private static final int minutesPerQuarter = 10;
	private static final int quartersPerGame = 4;
	private static final int secondsPerGame = secondsPerMinute * minutesPerQuarter * quartersPerGame;

	public static List<String> generateDates(String startYyyymmdd, String endYyyymmdd) throws Exception {

		if (!StringUtils.isPopulated(startYyyymmdd) || !StringUtils.isPopulated(endYyyymmdd)) {
			throw new Exception("No values for start or end season dates");
		}

		if (startYyyymmdd.length() != 8 || endYyyymmdd.length() != 8) {
			throw new Exception("Ill-formed dates: " + startYyyymmdd + " or " + endYyyymmdd);
		}

		try {
			LocalDate startLd = LocalDate.of(Integer.valueOf(startYyyymmdd.substring(0, 4)).intValue(), computeAsInt(startYyyymmdd.substring(4, 6)), computeAsInt(startYyyymmdd.substring(6)));
			LocalDate endLd = LocalDate.of(Integer.valueOf(endYyyymmdd.substring(0, 4)).intValue(), computeAsInt(endYyyymmdd.substring(4, 6)), computeAsInt(endYyyymmdd.substring(6)));

			return startLd.datesUntil(endLd).map(m -> m.toString().replace("-", "")).collect(Collectors.toList());
		} catch (Exception e) {
			throw e;
		}
	}

	public static Integer playByPlayTimeTranslation(String in, int quarter) throws Exception {
		Integer retValue = null;

		// log.info("seconds per game: " + secondsPerGame);
		if (!StringUtils.isPopulated(in)) {
			throw new Exception("Missing value for game mm:ss");
		}

		try {
			List<String> tokens = Arrays.asList(in.split(":"));
			if (tokens == null || tokens.size() != 2) {
				throw new Exception("Invalid format for game mm:ss");
			}

			int mm = Integer.valueOf(tokens.get(0)).intValue();
			int ss = Integer.valueOf(tokens.get(1)).intValue();

			int minutesInSeconds = mm * 60;
			int totalSecondsIntoGame = (secondsPerMinute * minutesPerQuarter * quarter) - (minutesInSeconds + ss);

			retValue = totalSecondsIntoGame;
		} catch (Exception e) {
			throw e;
		}

		return retValue;
	}

	public static boolean hasGameBeenPlayed(String yyyymmdd, String todayYyyymmdd) throws Exception {
		try {
			return yyyymmdd.compareTo(todayYyyymmdd) < 0;
		} catch (Exception e) {
			throw e;
		}

	}

	public static LocalDate getThisInLocalDateFormat(int winterYear, int springYear, String dayOfWeekMonthAndDate) throws Exception {
		try {
			if (!StringUtils.isPopulated(dayOfWeekMonthAndDate)) {
				return null;
			}

			Month month = null;
			int year = 0;
			int dayOfMonth = 0;

			String[] tokens = dayOfWeekMonthAndDate.split(",");
			if (tokens != null && tokens.length == 2) {

				String[] monthAndDayTokens = tokens[1].trim().split(" ");
				if (monthAndDayTokens != null && monthAndDayTokens.length == 2) {
					month = computeMonth(monthAndDayTokens[0].trim());
					if (winterMonths.contains(month)) {
						year = winterYear;
					} else if (springMonths.contains(month)) {
						year = springYear;
					}

					dayOfMonth = Integer.valueOf(monthAndDayTokens[1].trim()).intValue();

					return LocalDate.of(year, month, dayOfMonth);
				}
			}
		} catch (Exception e) {
			log.warn("Cannot calculate date: " + dayOfWeekMonthAndDate);
			return null;
		}
		return null;
	}

	private static DayOfWeek computeDayOfWeek(String in) throws Exception {
		try {
			DayOfWeek dow = null;

			switch (in) {
			case "Sun":
				dow = DayOfWeek.SUNDAY;
				break;
			case "Mon":
				dow = DayOfWeek.MONDAY;
				break;
			case "Tue":
				dow = DayOfWeek.TUESDAY;
				break;
			case "Wed":
				dow = DayOfWeek.WEDNESDAY;
				break;
			case "Thu":
				dow = DayOfWeek.THURSDAY;
				break;
			case "Fri":
				dow = DayOfWeek.FRIDAY;
				break;
			case "Sat":
				dow = DayOfWeek.SATURDAY;
				break;
			default:
				throw new Exception("Unknown day of week: " + in);
			}

			return dow;
		} catch (Exception e) {
			throw e;
		}
	}

	public static Month computeMonth(String in) throws Exception {
		try {
			Month month = null;

			switch (in) {
			case "January":
			case "Jan":
			case "01":
				month = Month.JANUARY;
				break;
			case "February":
			case "Feb":
			case "02":
				month = Month.FEBRUARY;
				break;
			case "March":
			case "Mar":
			case "03":
				month = Month.MARCH;
				break;
			case "April":
			case "Apr":
			case "04":
				month = Month.APRIL;
				break;
			case "May":
			case "05":
				month = Month.MAY;
				break;
			case "June":
			case "Jun":
			case "06":
				month = Month.JUNE;
				break;
			case "July":
			case "Jul":
			case "07":
				month = Month.JULY;
				break;
			case "August":
			case "Aug":
			case "08":
				month = Month.AUGUST;
				break;
			case "September":
			case "Sep":
			case "09":
				month = Month.SEPTEMBER;
				break;
			case "October":
			case "Oct":
			case "10":
				month = Month.OCTOBER;
				break;
			case "November":
			case "Nov":
			case "11":
				month = Month.NOVEMBER;
				break;
			case "December":
			case "Dec":
			case "12":
				month = Month.DECEMBER;
				break;

			default:
				throw new Exception("Unknown month: " + in);
			}

			return month;
		} catch (Exception e) {
			throw e;
		}
	}

	private static int computeAsInt(String in) {

		if (!StringUtils.isPopulated(in)) {
			return -1;
		}

		if (in.length() != 2) {
			return -1;
		}

		if (in.substring(0, 1).compareTo("0") == 0) {
			return Integer.valueOf(in.substring(1)).intValue();
		}

		return Integer.valueOf(in).intValue();
	}

	private static String computeAsStringLength2(int in) {
		String retValue = null;

		if (in < 10) {
			retValue = "0" + String.valueOf(in);
		} else {
			retValue = String.valueOf(in);
		}
		return retValue;
	}

	public static String/* [] */ parseUTCTime(String in) throws Exception {

		// String[] dt = new String[] { "", "" };

		try {
			DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'", Locale.ENGLISH);

			// LocalDate date = LocalDate.parse(in, inputFormatter);
			// dt[0] = DateTimeFormatter.ofPattern("yyy-MM-dd",
			// Locale.ENGLISH).format(date);

			LocalTime tm = LocalTime.parse(in, inputFormatter);
			return DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH).format(tm);
		} catch (Exception e) {
			throw e;
		}
	}

	public static String parseUTCTime2(String in) throws Exception {

		// in has format something like 6:00 PM
		if (in == null || in.trim().length() == 0) {
			log.warn("Cannot acquire gameTimeUTC");
			return "";
		}

		try {
			String[] tokens = in.split(" ");
			if (tokens == null || tokens.length < 2) {
				log.warn("Cannot tokenize date time");
				return "";
			}

			String ampm = tokens[1];
			if (ampm == null || (ampm.trim().toUpperCase().compareTo("PM") != 0 && ampm.trim().toUpperCase().compareTo("AM") != 0)) {
				log.warn("Cannot identify AM/PM in game time");
				return "";
			}

			String[] hhmmTokens = tokens[0].split(":");
			if (hhmmTokens == null || hhmmTokens.length != 2) {
				log.warn("Cannot acquire hh:mm for game time");
				return "";
			}
			int militaryHH = 0;
			int hour = Integer.valueOf(hhmmTokens[0]).intValue();
			if (ampm.trim().toUpperCase().compareTo("PM") == 0 && hour < 12) {
				militaryHH = hour + 12;
			} else {
				militaryHH = hour;
			}

			String hh = null;
			if (militaryHH < 10) {
				hh = "0" + String.valueOf(militaryHH);
			} else {
				hh = String.valueOf(militaryHH);
			}

			String mm = hhmmTokens[1];

			String HHMM = hh + ":" + mm;

			LocalTime lt = LocalTime.parse(HHMM, DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH));
			return lt.toString();
		} catch (Exception e) {
			throw e;
		}

	}

//	public static void main(String[] args) {
//		try {
//			List<String> test = generateDates("20211101", "20220415");
//			log.info(test.toString());
//		} catch (Exception e) {
//			log.error(e.getMessage());
//			e.printStackTrace();
//		}
//	}

}
