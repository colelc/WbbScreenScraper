package process.historical;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

	private static String playerUrlFile;
	private static String rebuiltPlayerFile;
	private static String teamFile;
	private static String url = null;
	private static String BASE_URL;

	private static String playerName;
	private static String playerFirstName;
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

			playerUrlFile = ConfigUtils.getBASE_OUTPUT_PATH()/**/
					+ File.separator + ConfigUtils.getProperty("base.output.child.data.path.season.2021.2022")/**/
					+ File.separator + ConfigUtils.getProperty("file.data.2021.2022.player.urls");

			rebuiltPlayerFile = ConfigUtils.getBASE_OUTPUT_PATH() /**/
					+ File.separator + ConfigUtils.getProperty("base.output.child.data.path.season.2021.2022")/**/
					+ File.separator + ConfigUtils.getProperty("file.data.players.2021.2022");

			teamFile = ConfigUtils.getBASE_OUTPUT_PATH() /**/
					+ File.separator + ConfigUtils.getProperty("file.data.teams");
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
			System.exit(99);
		}
	}

	public static void go() throws Exception {

		try {
			// collectPlayerUrls();
			rebuildPlayerFile();
		} catch (Exception e) {
			throw e;
		}

		return;
	}

	private static void collectPlayerUrls() throws Exception {
		try (BufferedWriter playerUrlFileWriter = new BufferedWriter(new FileWriter(playerUrlFile, false));) {
			// String BASE_URL =
			// ConfigUtils.getProperty("espn.com.womens.college.basketball");

			Set<String> playerIdSet = collectPlayerIds();

			playerIdSet.forEach(playerId -> {
				try {
					Thread.sleep(500l);
					url = BASE_URL + "player/_/id/" + playerId;
					JsoupUtils.parseStringToDocument(JsoupUtils.jsoupExtraction(url).toString());
					log.info(url + " -> H I T ! !");
					playerUrlFileWriter.write(url + "\n");
				} catch (InterruptedException e) {
					log.error(e.getMessage());
					e.printStackTrace();
				} catch (FileNotFoundException fnfe) {
					log.info(url + " -> No page");
				} catch (Exception e) {
					log.error(e.getMessage());
					e.printStackTrace();
				}
			});
		} catch (Exception e) {
			throw e;
		}
	}

	private static void rebuildPlayerFile() throws Exception {

		try (BufferedWriter playerFileWriter = new BufferedWriter(new FileWriter(rebuiltPlayerFile, false));) {

			FileUtils.readFileLines(playerUrlFile).forEach(url -> {
				try {
					String playerId = Arrays.asList(url.split("/")).stream().filter(f -> NumberUtils.isCreatable(f)).collect(Collectors.toList()).get(0);

					Thread.sleep(10000l);
					String statsUrl = BASE_URL + "player/stats/_/id/" + playerId;
					Document document = JsoupUtils.jsoupExtraction(statsUrl);

					if (document == null) {
						log.warn("Cannot acquire a stats document for playerId: " + playerId);
					} else {
						// log.info(document);
						process(playerId, url, document, playerFileWriter);
					}

					// PlayerProcessor.generatePlayerFile(playerFileWriter, teamId, document);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
		} catch (Exception e) {
			throw e;
		}
	}

	private static void process(String playerId, String playerUrl, Document document, BufferedWriter playerFileWriter) throws Exception {

		try {
			log.info(playerUrl);

			identifyActiveStatus(document);
			assignPlayerName(document);
			assignPlayerNumberAndPosition(document);

			// acquire teamId
			String teamId = calculateTeamId(document);

			String data = "[id]=" + playerId /**/
					+ ",[teamId]=" + teamId/**/
					+ ",[playerUrl]=" + playerUrl/**/
					+ ",[playerName]=" + playerName/**/
					+ ",[playerFirstName]=" + playerFirstName/**/
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
			playerFileWriter.write(data + "\n");

		} catch (Exception e) {
			throw e;
		}
	}

	private static String calculateTeamId(Document document) {
		String teamId = "";

		Elements teamEls = document.getElementsByAttribute("data-clubhouse-uid");
		if (teamEls == null || teamEls.first() == null) {
			log.warn("Cannot acquire team elements while searching for this players team");
			return "";
		}

		Set<String> values = new HashSet<>();
		for (Element teamEl : teamEls) {
			values.add(teamEl.attr("data-clubhouse-uid"));
		}

		if (values != null && values.size() == 1) {
			teamId = Arrays.asList(teamEls.first().attr("data-clubhouse-uid").split(":")).stream().reduce((first, second) -> second).orElse("");
			log.info("teamId = " + teamId);
			return teamId;
		}

		Elements trEls = document.getElementsByAttributeValue("class", "Table__TR Table__TR--sm Table__even");
		if (trEls == null || trEls.size() == 0) {
			log.warn("Cannot assign a teamId");
			return "";
		}

		Optional<Element> opt = trEls.stream().filter(e -> e.getElementsByAttributeValue("class", "Table__TD").first().text().compareTo("2021-22") == 0).findFirst();
		if (opt.isEmpty()) {
			log.warn("Cannot assign a teamId even though this player has years listed");
			return "";
		}

		boolean weHaveATeamId = false;
		for (Element e : trEls) {
			if (weHaveATeamId) {
				break;
			}
			for (Element td : e.getElementsByAttributeValue("class", "Table__TD")) {
				if (td.text().compareTo("2021-22") == 0) {
					String unparsed = e.getElementsByAttribute("data-clubhouse-uid").first().attr("data-clubhouse-uid");
					teamId = Arrays.asList(unparsed.split(":")).stream().reduce((first, second) -> second).orElse("");
					weHaveATeamId = true;
					break;
				}
			}
		}

		log.info("teamId = " + teamId);
		return teamId;
	}

	public static Set<String> collectPlayerIds() throws Exception {

		Set<String> playerIdSet = new HashSet<>();
		try {
			String inputDirectory = ConfigUtils.getBASE_OUTPUT_PATH() + File.separator + ConfigUtils.getProperty("base.output.child.data.path.season.2021.2022");
			Set<String> files = FileUtils.getFileListFromDirectory(inputDirectory, "playbyplay");

			if (files == null || files.size() == 0) {
				log.warn("Cannot acquire files from input directory");
				return playerIdSet;
			}

			files.forEach(file -> {
				try {
					playerIdSet.addAll(FileUtils.readFileIntoList(inputDirectory, file).stream()/**/
							.map(m -> {
								List<String> tokenList = Arrays.asList(m.split("\\,"));
								String playerId = tokenList.stream()/**/
										.filter(f -> f.contains("playerId"))/**/
										.map(p -> p.split("=")[1]).findFirst().get();
								return playerId;
							}).collect(Collectors.toSet()));
				} catch (Exception e) {
					log.error(e.getMessage());
					e.printStackTrace();
				}
			});
		} catch (Exception e) {
			throw e;
		}

		return playerIdSet;
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
		playerFirstName = playerName.split(" ")[0];
		playerLastName = "";
		String[] lastNameTokens = playerName.split(" ");
		for (int i = 1; i < lastNameTokens.length; i++) {
			playerLastName += lastNameTokens[i] + " ";
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
			log.warn("Will not be able to acquire class year");
		} else {
			for (int i = 0; i < valEls.size(); i++) {
				String whatIsIt = els.get(i).text();
				String itsValue = valEls.get(i).getElementsByTag("div").first().text();

				if (whatIsIt.compareTo("Class") == 0) {
					classYear = itsValue.replace("Freshman", "FR").replace("Sophomore", "SO").replace("Junior", "JR").replace("Senior", "SR");
				} else if (whatIsIt.compareTo("Birthplace") == 0) {
					String[] cityStateTokens = itsValue.split(", ");
					if (cityStateTokens == null || cityStateTokens.length != 2) {
						log.warn("Cannot acquire city state tokens");
					} else {
						homeCity = cityStateTokens[0];
						homeState = cityStateTokens[1];
					}
				} else if (whatIsIt.compareTo("Height") == 0) {
					String[] heightTokens = itsValue.split(" ");
					if (heightTokens == null || heightTokens.length != 2) {
						log.warn("Cannot acquire player height");
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
			}
		}
	}

	private static Elements acquire(Document document, String attribute, String attributeValue) {
		Elements els = document.getElementsByAttributeValue(attribute, attributeValue);
		if (els != null && els.first() != null) {
			return els;
		}

		return null;
	}

}
