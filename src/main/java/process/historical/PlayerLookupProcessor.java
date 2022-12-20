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

	// private static String season;
	private static String playerUrlFile;
	private static String rebuiltPlayerFile;
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
			// season =
			// ConfigUtils.getProperty("base.output.child.data.path.season.2021.2022");
			BASE_URL = ConfigUtils.getProperty("espn.com.womens.college.basketball");
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
			System.exit(99);
		}
	}

	public static void go(String season) throws Exception {

		try {

			playerUrlFile = ConfigUtils.getProperty("base.output.file.path")/**/
					+ File.separator + season/**/
					+ File.separator + ConfigUtils.getProperty("file.data.player.urls");

			rebuiltPlayerFile = ConfigUtils.getProperty("base.output.file.path") /**/
					+ File.separator + season/**/
					+ File.separator + ConfigUtils.getProperty("file.data.players");

			String singlePlayerId = ConfigUtils.getProperty("single.player.id");
			if (singlePlayerId == null || singlePlayerId.trim().length() == 0) {
				collectPlayerUrls(season);
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

	public static void consolidateLogOutput(String season) throws Exception {

		// input
		String tempPlayerOutputFile1 = ConfigUtils.getProperty("project.path")/**/
				+ File.separator + "tempPlayerOutput_1.txt";

		String tempPlayerOutputFile2 = ConfigUtils.getProperty("project.path")/**/
				+ File.separator + "tempPlayerOutput_2.txt";

		String tempPlayerOutputFile3 = ConfigUtils.getProperty("project.path")/**/
				+ File.separator + "tempPlayerOutput_3.txt";

		// output
		String tempPlayerUrlFile1 = ConfigUtils.getProperty("project.path") /**/
				+ File.separator + "tempPlayerUrl_1.txt";

		String tempPlayerUrlFile2 = ConfigUtils.getProperty("project.path") /**/
				+ File.separator + "tempPlayerUrl_2.txt";

		String tempPlayerUrlFile3 = ConfigUtils.getProperty("project.path") /**/
				+ File.separator + "tempPlayerUrl_3.txt";

		// 503s missed for 1st file only (output file) but will count them for all files
		String tempPlayer503File1 = ConfigUtils.getProperty("project.path")/**/
				+ File.separator + "tempPlayer503_1.txt";

		String tempPlayer503File2 = ConfigUtils.getProperty("project.path")/**/
				+ File.separator + "tempPlayer503_2.txt";

		String tempPlayer503File3 = ConfigUtils.getProperty("project.path")/**/
				+ File.separator + "tempPlayer503_3.txt";

		Set<String> inputs = new HashSet<>();
		inputs.add(tempPlayerOutputFile1);
		inputs.add(tempPlayerOutputFile2);
		inputs.add(tempPlayerOutputFile3);

		for (String inputFile : inputs) {
			log.info("Input file: " + inputFile);

			String urlFile = inputFile.contains("Output_1") ? tempPlayerUrlFile1 : (inputFile.contains("Output_2") ? tempPlayerUrlFile2 : tempPlayerUrlFile3);
			String _503File = inputFile.contains("Output_1") ? tempPlayer503File1 : (inputFile.contains("Output_2") ? tempPlayer503File2 : tempPlayer503File3);

			try (BufferedWriter playerUrlFileWriter = new BufferedWriter(new FileWriter(urlFile, false)); /**/
					BufferedWriter player503FileWriter = new BufferedWriter(new FileWriter(_503File, false));) {

				List<String> hits = filterTempFile(inputFile, "H I T");
				log.info("number of hits = " + hits.size());
				// hits.forEach(h -> log.info(h));

				List<String> _503s = filterTempFile(inputFile, "503?");
				// _503s.forEach(h -> log.info(h));
				log.info("number of 503s = " + _503s.size());

				for (String url : hits) {
					playerUrlFileWriter.write(url + "\n");
				}

				for (String url : _503s) {
					player503FileWriter.write(url + "\n");
				}

				List<String> other = FileUtils.readFileLines(inputFile)/**/
						.stream()/**/
						.filter(f -> !f.contains("No page") && !f.contains("H I T") && !f.contains("503?") && !f.contains("Server returned HTTP response code: 503"))/**/
						.collect(Collectors.toList());/**/
				log.info("Other size is " + other.size());
				// other.forEach(o -> log.info(o));
			} catch (Exception e) {
				throw e;
			}
		}

		// pull into single file
		log.info("Consolidating");
		String finalPlayerUrlFile = ConfigUtils.getProperty("project.path")/**/
				+ File.separator + "allPlayersUrl.txt";

		Set<String> dataFiles = new HashSet<>();
		dataFiles.add(tempPlayerUrlFile1);
		dataFiles.add(tempPlayerUrlFile2);
		dataFiles.add(tempPlayerUrlFile3);
		dataFiles.add(tempPlayer503File1); // adding this one due to previous bug

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(finalPlayerUrlFile, false))) {
			for (String dataFile : dataFiles) {
				for (String url : FileUtils.readFileLines(dataFile)) {
					writer.write(url + "\n");
				}
			}
		} catch (Exception e) {
			throw e;
		}

		return;
	}

	private static List<String> filterTempFile(String playerUrlFile, String targetString) throws Exception {

		try {
			return FileUtils.readFileLines(playerUrlFile)/**/
					.stream()/**/
					.filter(f -> f.contains(targetString))/**/
					.map(m -> m.split(" ")[6])/**/
					.collect(Collectors.toList());/**/
		} catch (Exception e) {
			throw e;
		}

	}

	private static void processSinglePlayer(String playerId, String season) throws Exception {

		try {
			log.info("Processing single playerId: " + playerId);

			String playerUrl = BASE_URL + "player/stats/_/id/" + playerId;
			log.info(playerUrl);
			Document document = JsoupUtils.parseStringToDocument(JsoupUtils.jsoupExtraction(playerUrl).toString());
			process(season, playerId, playerUrl, document, null);
		} catch (Exception e) {
			throw e;
		}
	}

	private static void rebuildPlayerFile(String season) throws Exception {

		try (BufferedWriter playerFileWriter = new BufferedWriter(new FileWriter(rebuiltPlayerFile, false));) {

			FileUtils.readFileLines(playerUrlFile).forEach(url -> {
				try {
					String playerId = Arrays.asList(url.split("/")).stream().filter(f -> NumberUtils.isCreatable(f)).collect(Collectors.toList()).get(0);

					Thread.sleep(1000l);
					String statsUrl = BASE_URL + "player/stats/_/id/" + playerId;
					// Document document = JsoupUtils.jsoupExtraction(statsUrl);//
					Document document = acquire(statsUrl);

					if (document == null) {
						log.warn("Cannot acquire a stats document for playerId: " + playerId);
					} else {
						// log.info(document);
						process(season, playerId, url, document, playerFileWriter);
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

	private static Document acquire(String playerUrl) throws Exception {
		boolean noDoc = true;
		int tries = 0;
		int MAX_TRIES = 10;

		while (noDoc) {
			try {
				++tries;
				if (tries > MAX_TRIES) {
					log.warn("We have tried 10 times and cannot acquire this document, for player: " + playerUrl);
					return null;
				}

				Document doc = getDocument(playerUrl);
				if (doc == null) {
					log.warn("Cannot acquire document for " + playerUrl);
					return null;
				}

				return doc;
			} catch (Exception e) {
				log.error(e.getMessage());
				Thread.sleep(30000l);
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

	private static void process(String season, String playerId, String playerUrl, Document document, BufferedWriter playerFileWriter) throws Exception {

		try {
			identifyActiveStatus(document);
			assignPlayerName(document);
			log.info(playerFirstName + " " + playerMiddleName + " " + playerLastName + " -> " + playerUrl);
			assignPlayerNumberAndPosition(document);

			// acquire teamId
			String teamId = calculateTeamId(document, season);
			if (teamId == null) {
				log.info("This player does not have data for the " + season + " season");
				return;
			}

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
				playerFileWriter.write(data + "\n");
			} else {
				log.info(data);
			}

		} catch (Exception e) {
			throw e;
		}
	}

	private static String calculateTeamId(Document document, String season) {
		String teamId = "";

		Elements trEls = document.getElementsByAttributeValue("class", "Table__TR Table__TR--sm Table__even");
		if (trEls == null || trEls.size() == 0) {
			log.warn("Cannot assign a teamId");
			return "";
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
				return "";
			}

			boolean haveTheTeamId = false;
			for (Element tdEl : tdEls) {
				// log.info(tdEl.toString());
				if (tdEl.text().compareTo(season) == 0) {
					Element nextSibling = tdEl.nextElementSibling();
					if (nextSibling == null) {
						log.warn("Cannot acquire next sibling during teamId calculation... ");
						return "";
					}

					// log.info(nextSibling.toString());

					Elements anchorEls = nextSibling.getElementsByTag("a");
					if (anchorEls == null || anchorEls.first() == null) {
						log.warn("Cannot acquire team anchor elements while calculating teamId");
						return "";
					}

					Optional<String> teamIdOpt = Arrays.asList(anchorEls.first().attr("href").split("/")).stream().filter(f -> NumberUtils.isCreatable(f)).findFirst();
					if (teamIdOpt.isEmpty()) {
						log.warn("Cannot acquire teamId from anchor tag: ");
						log.warn(anchorEls.first().toString());
						return "";
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

//	public static Set<String> collectPlayerIds(String season) throws Exception {
//
//		Set<String> playerIdSet = new HashSet<>();
//		try {
//			String inputDirectory = ConfigUtils.getProperty("base.output.file.path") + File.separator + season;
//			Set<String> files = FileUtils.getFileListFromDirectory(inputDirectory, "playbyplay");
//
//			if (files == null || files.size() == 0) {
//				log.warn("Cannot acquire files from input directory");
//				return playerIdSet;
//			}
//
//			files.forEach(file -> {
//				try {
//					playerIdSet.addAll(FileUtils.readFileIntoList(inputDirectory, file).stream()/**/
//							.map(m -> {
//								List<String> tokenList = Arrays.asList(m.split("\\,"));
//								String playerId = tokenList.stream()/**/
//										.filter(f -> f.contains("playerId"))/**/
//										.map(p -> p.split("=")[1]).findFirst().get();
//								return playerId;
//							}).collect(Collectors.toSet()));
//				} catch (Exception e) {
//					log.error(e.getMessage());
//					e.printStackTrace();
//				}
//			});
//		} catch (Exception e) {
//			throw e;
//		}
//
//		return playerIdSet;
//	}

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

	private static void collectPlayerUrls(String season) throws Exception {
		try (BufferedWriter playerUrlFileWriter = new BufferedWriter(new FileWriter(playerUrlFile, false));) {

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

//	private static Document collectPlayerUrlSinglePlayer(String playerId) throws Exception {
//
//		try {
//			Thread.sleep(1000l);
//			String url = BASE_URL + "player/_/id/" + playerId;
//			Document document = acquire(url);
//			return document;
//		} catch (Exception e) {
//			log.error(e.getMessage());
//			e.printStackTrace();
//		}
//		return null;
//	}

}
