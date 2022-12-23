package process.historical;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import utils.ConfigUtils;
import utils.FileUtils;
import utils.JsoupUtils;
import utils.StringUtils;

public class PlayerLookupProcessor {

	// private static String season;
	private static String allPlayersUrlFile;
	private static String playerHitsFile;
	private static String playerMissesFile;
	private static String url = null;
	private static String BASE_URL;

	private static String playerName;
	private static String playerFirstName;
	private static String playerMiddleName;
	private static String playerLastName;
	private static boolean active = false;
	private static String classYear;
	private static String homeCity;
	private static String homeState;
	private static String heightFeet;
	private static String heightInches;
	private static String heightCm;
	private static String playerNumber;
	private static String position;

	private static Logger log = Logger.getLogger(PlayerLookupProcessor.class);

	static {
		try {
			BASE_URL = ConfigUtils.getProperty("espn.com.womens.college.basketball");
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
			System.exit(99);
		}
	}

	public static void go(String season) throws Exception {

		try {

			allPlayersUrlFile = ConfigUtils.getProperty("base.all.players.url.file.path")/**/
					+ File.separator + ConfigUtils.getProperty("file.data.all.players.url");

			playerHitsFile = ConfigUtils.getProperty("base.output.file.path") /**/
					+ File.separator + season/**/
					+ File.separator + ConfigUtils.getProperty("file.data.players");

			playerMissesFile = ConfigUtils.getProperty("base.output.file.path") /**/
					+ File.separator + season/**/
					+ File.separator + ConfigUtils.getProperty("file.data.players.not.found");

			String singlePlayerId = ConfigUtils.getProperty("single.player.id");
			if (singlePlayerId == null || singlePlayerId.trim().length() == 0) {
				// collectPlayerUrls(season);
				rebuildPlayerFile(season);
			} else {
				// collectPlayerUrlSinglePlayer(singlePlayerId);
				processSinglePlayer(singlePlayerId, season);
			}
		} catch (Exception e) {
			throw e;
		}

		return;
	}

	private static void processSinglePlayer(String playerId, String season) throws Exception {

		try {
			log.info("Processing single playerId: " + playerId);

			String playerUrl = BASE_URL + "player/stats/_/id/" + playerId;
			log.info(playerUrl);
			Document document = JsoupUtils.parseStringToDocument(JsoupUtils.jsoupExtraction(playerUrl).toString());
			String teamId = calculateTeamId(document, season);
			process(season, playerId, playerUrl, teamId, document, null);
		} catch (Exception e) {
			throw e;
		}
	}

	private static void rebuildPlayerFile(String season) throws Exception {

		try (BufferedWriter playerFileWriter = new BufferedWriter(new FileWriter(playerHitsFile, false)); /**/
				BufferedWriter playerNotFoundWriter = new BufferedWriter(new FileWriter(playerMissesFile, false));) {

			FileUtils.readFileLines(allPlayersUrlFile).forEach(url -> {
				try {
					String playerId = Arrays.asList(url.split("/")).stream().filter(f -> NumberUtils.isCreatable(f)).collect(Collectors.toList()).get(0);

					Thread.sleep(150l);
					String statsUrl = BASE_URL + "player/stats/_/id/" + playerId;
					log.info(statsUrl);

					Document document = acquire(statsUrl);

					if (document != null) {
						// log.info(document);
						String teamId = calculateTeamId(document, season);
						if (teamId == null) {
							playerNotFoundWriter.write(url + "\n");
						} else {
							process(season, playerId, url, teamId, document, playerFileWriter);
						}
					}
				} catch (Exception e) {
					log.error(e.getMessage());
					e.printStackTrace();
				}
			});
		} catch (Exception e) {
			throw e;
		}
	}

	private static Document acquire(String playerUrl) throws Exception {
		boolean noDoc = true;
		int tries = 0;
		int MAX_TRIES = 3;

		while (noDoc) {
			try {
				++tries;
				if (tries > MAX_TRIES) {
					log.warn("We have tried " + MAX_TRIES + " times and cannot acquire this document, for player: " + playerUrl);
					return null;
				}

				Document doc = getDocument(playerUrl);
				if (doc == null) {
					log.warn("Cannot acquire document for " + playerUrl);
					return null;
				}

				return doc;
			} catch (InterruptedException e) {
				log.info(url + " -> Interrupted Exception");
				log.error(e.getMessage());
				e.printStackTrace();
			} catch (FileNotFoundException fnfe) {
				log.info(url + " -> No page");
				return null;
			} catch (Exception e) {
				log.info(url + " -> a 503?");
				log.error(e.getMessage());
				Thread.sleep(15000l);
			} // try catch
		} // while
		return null;
	}

	private static Document getDocument(String playerUrl) throws Exception {
		try {
			Document doc = JsoupUtils.jsoupExtraction(playerUrl);
			if (doc == null) {
				log.warn("No html data for this player request");
				return null;
			}
			return doc;
		} catch (Exception e) {
			throw e;
		}
	}

	private static void process(String season, String playerId, String playerUrl, String teamId, Document document, BufferedWriter playerFileWriter) throws Exception {

		try {
			identifyActiveStatus(document);

			assignPlayerName(document);
			log.info(playerFirstName + " " + playerMiddleName + " " + playerLastName);
			assignPlayerNumberAndPosition(document);

			String data = "[id]=" + playerId /**/
					+ ",[teamId]=" + teamId/**/
					+ ",[playerUrl]=" + playerUrl/**/
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
				log.info(data);
				playerFileWriter.write(data + "\n");
			} else {
				log.info(data);
			}

		} catch (Exception e) {
			throw e;
		}
	}

	private static String calculateTeamId(Document document, String season) {
		String teamId = null;

		Elements trEls = document.getElementsByAttributeValue("class", "Table__TR Table__TR--sm Table__even");
		if (trEls == null || trEls.size() == 0) {
			// log.warn("Cannot assign a teamId");
			return null;
		}

		if (!trEls.toString().contains(season)) {
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
				if (tdEl.text().compareTo(season) == 0) {
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
		Elements els = acquire(document, "class", "PlayerHeader__Team_Info list flex pt1 pr4 min-w-0 flex-basis-0 flex-shrink flex-grow nowrap");
		if (els == null) {
			log.warn("Cannot acquire player position or player number: these will be left blank");
		} else {
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
			playerMiddleName = firstMiddleLastTokens[1].trim();
			playerLastName = firstMiddleLastTokens[2].trim();
		} else {
			log.warn("What is this? " + firstMiddleLastTokens.toString());
		}

	}

	private static void identifyActiveStatus(Document document) {
		active = false;
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

	private static Elements acquire(Document document, String attribute, String attributeValue) {
		Elements els = document.getElementsByAttributeValue(attribute, attributeValue);
		if (els != null && els.first() != null) {
			return els;
		}

		return null;
	}

	private static void collectPlayerUrls(String season) throws Exception {
		try (BufferedWriter playerUrlFileWriter = new BufferedWriter(new FileWriter(allPlayersUrlFile, false));) {

			int startId = Integer.valueOf(ConfigUtils.getProperty("player.lookup.start.id")).intValue();
			int endId = Integer.valueOf(ConfigUtils.getProperty("player.lookup.end.id")).intValue();
			log.info("startId = " + startId + ", endId = " + endId);

			for (int playerId = startId; playerId < endId; ++playerId) {
				read(String.valueOf(playerId), playerUrlFileWriter);

			}
		} catch (Exception e) {
			throw e;
		}
	}

	private static void read(String playerId, BufferedWriter playerUrlFileWriter) throws Exception {

		int tries = 0;
		int MAX_TRIES = 10;

		while (true) {

			try {
				++tries;
				if (tries > MAX_TRIES) {
					log.warn("We have tried 10 times and cannot acquire this url, for player: " + playerId);
					return;
				}

				Thread.sleep(150l);
				url = BASE_URL + "player/_/id/" + playerId;
				String html = JsoupUtils.getHttpDoc(url);
				if (StringUtils.isPopulated(html)) {
					log.info(url + " -> H I T ! !");
					playerUrlFileWriter.write(url + "\n");
					return;
				}
			} catch (InterruptedException e) {
				log.info(url + " -> Interrupted Exception");
				log.error(e.getMessage());
				e.printStackTrace();
			} catch (FileNotFoundException fnfe) {
				log.info(url + " -> No page");
				return;
			} catch (Exception e) {
				log.info(url + " -> a 503?");
				log.error(e.getMessage());
				Thread.sleep(30000l);
			}
		}
	}

}
