package process.historical;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import utils.ConfigUtils;
import utils.JsoupUtils;
import utils.StringUtils;

public class PlayerLookupProcessor {

	private static String SEASON;
	private static String ESPN_WBB_HOME_URL;

	private static String playerName;
	private static String playerFirstName;
	private static String playerMiddleName;
	private static String playerLastName;

	private static String classYear;
	private static String homeCity;
	private static String homeState;
	private static String heightFeet;
	private static String heightInches;
	private static String heightCm;
	private static String playerNumber;
	private static String position;

	private static boolean processingSinglePlayer = false;

	private static List<Integer> specialIds = new ArrayList<>();

	private static Logger log = Logger.getLogger(PlayerLookupProcessor.class);

	static {
		try {
			ESPN_WBB_HOME_URL = ConfigUtils.getProperty("espn.com.womens.college.basketball");
			specialIds.add(6961);
			specialIds.add(12038);
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
			System.exit(99);
		}
	}

	public static void go(String season) throws Exception {

		try {
			SEASON = season;
			String singlePlayerId = ConfigUtils.getProperty("single.player.id");
			if (singlePlayerId == null || singlePlayerId.trim().length() == 0) {
				lookups();
			} else {
				processingSinglePlayer = true;
				processSinglePlayer(singlePlayerId);
			}
		} catch (Exception e) {
			throw e;
		}

		return;
	}

	private static void lookups() throws Exception {
		String playerFile = ConfigUtils.getProperty("project.path.players") /**/
				+ File.separator + SEASON/**/
				+ File.separator + ConfigUtils.getProperty("file.players.txt");

		String playerNotFoundFile = ConfigUtils.getProperty("project.path.players") /**/
				+ File.separator + SEASON/**/
				+ File.separator + ConfigUtils.getProperty("file.players.not.found.txt");

		try (BufferedWriter playerFileWriter = new BufferedWriter(new FileWriter(playerFile, false)); /**/
				BufferedWriter playerNotFoundFileWriter = new BufferedWriter(new FileWriter(playerNotFoundFile, false));) {

			int startId = Integer.valueOf(ConfigUtils.getProperty("player.lookup.start.id")).intValue();
			int endId = Integer.valueOf(ConfigUtils.getProperty("player.lookup.end.id")).intValue();
			log.info("startId = " + startId + ", endId = " + endId);

			for (int playerId = startId; playerId < endId; ++playerId) {
				read(String.valueOf(playerId), playerFileWriter, playerNotFoundFileWriter);
				playerFileWriter.flush();
				playerNotFoundFileWriter.flush();
			}

			for (Integer specialId : specialIds) {
				read(String.valueOf(specialId.intValue()), playerFileWriter, playerNotFoundFileWriter);
				playerFileWriter.flush();
				playerNotFoundFileWriter.flush();
			}
		} catch (Exception e) {
			throw e;
		}
	}

	public static void read(String playerId, BufferedWriter playerFileWriter, BufferedWriter playerNotFoundFileWriter) throws Exception {

		try {
			Thread.sleep(150l);
			String url = ESPN_WBB_HOME_URL + "player/_/id/" + playerId;
			Document document = JsoupUtils.acquire(url);

			if (document != null) {
				process(playerId, url, document, playerFileWriter, playerNotFoundFileWriter);
			} else {
				playerNotFoundFileWriter.write(url + "\n");
			}
		} catch (Exception e) {
			throw e;
		}
	}

	private static void process(/**/
			String playerId, /**/
			String url, /**/
			Document document, /**/
			BufferedWriter playerFileWriter, /**/
			BufferedWriter playerNotFoundFileWriter) throws Exception {

		try {
			String teamId = null;
			if (!processingSinglePlayer) {
				teamId = calculateTeamId(document);
			} else {
				teamId = calculateTeamIdWithoutSeason(document);
			}
			if (teamId == null) {
				log.warn(url + " -> Cannot calculate teamId");
				if (playerNotFoundFileWriter != null) {
					playerNotFoundFileWriter.write("Cannot calculate teamId -> " + url);
				}
				return;
			}

			identifyActiveStatus(document);

			assignPlayerName(document);
			// log.info(playerFirstName + " " + playerMiddleName + " " + playerLastName);
			assignPlayerNumberAndPosition(document);

			String data = "[id]=" + playerId /**/
					+ ",[teamId]=" + teamId/**/
					+ ",[playerUrl]=" + url/**/
					+ ",[playerName]=" + playerName/**/
					+ ",[playerFirstName]=" + playerFirstName/**/
					+ ",[playerMiddleName]=" + (playerMiddleName.trim().length() == 0 ? "" : playerMiddleName)/**/
					+ ",[playerLastName]=" + playerLastName.trim()/**/
					+ ",[uniformNumber]=" + playerNumber/**/
					+ ",[position]=" + position/**/
					+ ",[heightFeet]=" + heightFeet/**/
					+ ",[heightInches]=" + heightInches/**/
					+ ",[heightCm]=" + heightCm/**/
					+ ",[classYear]=" + classYear/**/
					+ ",[homeCity]=" + homeCity/**/
					+ ",[homeState]=" + homeState/**/
			;
			if (playerFileWriter != null) {
				log.info(url + " -> " + data);
				playerFileWriter.write(data + "\n");
			} else {
				log.info(data);
			}

		} catch (Exception e) {
			throw e;
		}
	}

	private static String calculateTeamIdWithoutSeason(Document document) {

		Elements trEls = document.getElementsByAttributeValue("class", "Table__TR Table__TR--sm Table__even");
		if (trEls == null || trEls.size() == 0) {
			// log.warn("Cannot assign a teamId");
			return null;
		}

		Optional<Element> anchorOpt = trEls.stream().filter(trEl -> trEl.getElementsByTag("a") != null).findFirst();
		if (anchorOpt.isEmpty()) {
			log.warn("Cannot find a team id");
			return null;
		}

		Elements anchorEls = anchorOpt.get().getElementsByTag("a");
		if (anchorEls == null || anchorEls.first() == null) {
			log.warn("Cannot acquire anchor elements that point to a teamId");
			return null;
		}

		Optional<String> teamIdOpt = Arrays.asList(anchorEls.first().attr("href").split("/")).stream().filter(f -> NumberUtils.isCreatable(f)).findFirst();
		if (teamIdOpt.isEmpty()) {
			log.warn("Cannot acquire teamId from anchor tag: ");
			log.warn(anchorOpt.get().toString());
			return null;
		}

		// log.info("teamId = " + teamId);
		return teamIdOpt.get();
	}

	private static String calculateTeamId(Document document) {
		String teamId = null;

		Elements trEls = document.getElementsByAttributeValue("class", "Table__TR Table__TR--sm Table__even");
		if (trEls == null || trEls.size() == 0) {
			// log.warn("Cannot assign a teamId");
			return null;
		}

		if (SEASON != null && !trEls.toString().contains(SEASON)) {
			// log.warn("There is no data for the " + season + " season for this player");
			return null;
		}

		for (Element trEl : trEls) {
			// log.info(trEl.toString());
			Elements tdEls = trEl.getElementsByAttributeValue("class", "Table__TD");
			if (tdEls == null || tdEls.first() == null) {
				log.warn("Cannot acquire td elements for calculating teamId");
				return null;
			}

			boolean haveTheTeamId = false;
			for (Element tdEl : tdEls) {
				// log.info(tdEl.toString());
				if (tdEl.text().compareTo(SEASON) == 0) {
					Element nextSibling = tdEl.nextElementSibling();
					if (nextSibling == null) {
						log.warn("Cannot acquire next sibling during teamId calculation... ");
						return null;
					}

					// log.info(nextSibling.toString());

					Elements anchorEls = nextSibling.getElementsByTag("a");
					if (anchorEls == null || anchorEls.first() == null) {
						log.warn("Cannot acquire team anchor elements while calculating teamId");
						return null;
					}

					Optional<String> teamIdOpt = Arrays.asList(anchorEls.first().attr("href").split("/")).stream().filter(f -> NumberUtils.isCreatable(f)).findFirst();
					if (teamIdOpt.isEmpty()) {
						log.warn("Cannot acquire teamId from anchor tag: ");
						log.warn(anchorEls.first().toString());
						return null;
					}

					teamId = teamIdOpt.get();
					haveTheTeamId = true;
					break;
				}
			}
			if (haveTheTeamId) {
				break;
			}
		}

		// log.info("teamId = " + teamId);
		return teamId;
	}

	private static void assignPlayerNumberAndPosition(Document document) {
		playerNumber = "";
		position = "";
		Elements els = document.getElementsByAttributeValue("class", "PlayerHeader__Team_Info list flex pt1 pr4 min-w-0 flex-basis-0 flex-shrink flex-grow nowrap");
		if (els == null || els.first() == null) {
			log.warn("Cannot acquire player position or player number: these will be left blank");
			return;
		}

		Elements lis = els.first().getElementsByTag("li");
		if (lis != null && lis.size() == 2) {
			playerNumber = lis.first().text().replace("#", "");
			position = lis.last().text().replace("Forward", "F").replace("Guard", "G").replace("Center", "C");
		} else if (lis != null && lis.size() == 3) {
			playerNumber = lis.get(1).text().replace("#", "");
			position = lis.get(2).text().replace("Forward", "F").replace("Guard", "G").replace("Center", "C");
		} else {
			log.warn("Cannot acquire player position or player number from li tags: these will be left blank");
		}
	}

	private static void assignPlayerName(Document document) {
		playerName = "";
		playerFirstName = "";
		playerMiddleName = "";
		playerLastName = "";

		Elements els = document.getElementsByTag("title");
		if (els == null || els.first() == null) {
			log.warn("Cannot acquire title element - we will never know who this player was");
			return;
		}

		String[] nameTokens = els.first().text().split(" Stats");
		if (nameTokens == null || nameTokens.length < 2) {
			log.warn("Cannot acquire player name from name tokens");
			return;
		}

		playerName = nameTokens[0].trim();
		String[] firstMiddleLastTokens = playerName.split(" ");
		if (firstMiddleLastTokens == null || firstMiddleLastTokens.length < 2) {
			log.warn("Cannot acquire this player name from firstMiddleLast tokens");
			return;
		}

		playerFirstName = firstMiddleLastTokens[0];

		if (firstMiddleLastTokens.length == 2) {
			playerLastName = firstMiddleLastTokens[1].trim();
		} else if (firstMiddleLastTokens.length == 3) {
			if (!firstMiddleLastTokens[1].trim().contains(".")) {
				playerMiddleName = firstMiddleLastTokens[1].trim();
				playerLastName = firstMiddleLastTokens[2].trim();
			} else {
				playerMiddleName = "";
				playerLastName = firstMiddleLastTokens[1].trim() + " " + firstMiddleLastTokens[2].trim();
			}
		} else {
			log.warn("What is this? " + firstMiddleLastTokens.toString());
		}

	}

	private static void identifyActiveStatus(Document document) {
		boolean active = false;
		classYear = "";
		homeCity = "";
		homeState = "";
		heightFeet = "";
		heightInches = "";
		heightCm = "";

		Elements els = document.getElementsByAttributeValue("class", "ttu");
		Elements valEls = document.getElementsByAttributeValue("class", "fw-medium clr-black");
		if (els == null || els.first() == null || valEls == null || valEls.first() == null || els.size() != valEls.size()) {
			// log.warn("Cannot acquire class year");
			active = false;
		}

		for (int i = 0; i < valEls.size(); i++) {
			String whatIsIt = els.get(i).text();
			String itsValue = valEls.get(i).getElementsByTag("div").first().text();

			if (whatIsIt.compareTo("Class") == 0) {
				classYear = itsValue.replace("Freshman", "FR").replace("Sophomore", "SO").replace("Junior", "JR").replace("Senior", "SR");
			} else if (whatIsIt.compareTo("Birthplace") == 0) {
				String[] cityStateTokens = itsValue.split(", ");
				if (cityStateTokens == null || cityStateTokens.length != 2) {
					// log.warn("Cannot acquire city state tokens");
				} else {
					homeCity = cityStateTokens[0];
					homeState = cityStateTokens[1];
				}
			} else if (whatIsIt.compareTo("Height") == 0) {
				String[] heightTokens = itsValue.split(" ");
				if (heightTokens == null || heightTokens.length != 2) {
					// log.warn("Cannot acquire player height");
				} else {
					heightFeet = heightTokens[0].replace("\'", "").trim();
					heightInches = heightTokens[1].replace("\"", "").trim();
					heightCm = StringUtils.inchesToCentimeters(heightFeet, heightInches);
				}
			} else if (whatIsIt.compareTo("Status") == 0) {
				if (itsValue.compareTo("Active") == 0) {
					active = true;
				}
				// log.info("Status is -> " + itsValue);
			}
		}

		if (active) {
			// we need to subtract off a year
			classYear = classYear.replace("SO", "FR").replace("JR", "SO").replace("SR", "JR");
			log.info("Active");
		}
	}

	private static void processSinglePlayer(String playerId) throws Exception {

		try {
			log.info("Processing single playerId: " + playerId);

			String playerUrl = ESPN_WBB_HOME_URL + "player/stats/_/id/" + playerId;
			log.info(playerUrl);
			Document document = JsoupUtils.acquire(playerUrl);
			process(playerId, playerUrl, document, null, null);
		} catch (Exception e) {
			throw e;
		}
	}
}
