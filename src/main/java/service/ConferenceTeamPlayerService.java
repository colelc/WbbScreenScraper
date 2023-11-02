package service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import process.historical.PlayerLookupProcessor;
import utils.ConfigUtils;
import utils.FileUtilities;
import utils.JsoupUtils;
import utils.StringUtils;

public class ConferenceTeamPlayerService {

	// private static String PROJECT_PATH_OUTPUT_DATA;
	private static String ESPN_HOME;
	private static String ESPN_TEAM_ROSTER_URL;
	private static String SEASON;

	private static String conferenceFile;
	private static String teamFile;
	private static String playerFile;

	private static Map<Integer, Map<String, String>> conferenceMap;
	private static Map<Integer, Map<String, String>> teamMap;
	private static Map<Integer, Map<String, String>> playerMap;
	private static Set<String> teamNamesList;
	private static String homeTeamId;
	private static String roadTeamId;

	private static Logger log = Logger.getLogger(ConferenceTeamPlayerService.class);

	static {
		try {
			ESPN_HOME = ConfigUtils.getESPN_HOME();
			ESPN_TEAM_ROSTER_URL = ConfigUtils.getProperty("espn.com.womens.team.roster.page");
			// PROJECT_PATH_OUTPUT_DATA =
			// ConfigUtils.getProperty("project.path.output.data");
			SEASON = ConfigUtils.getProperty("season");

			String conferenceTeamPlayerDir = (ConfigUtils.getProperty("project.path.conference.team.player")).replace("_SEASON_", SEASON);
			FileUtilities.createDirectoryIfNotExists(conferenceTeamPlayerDir);

			conferenceFile = conferenceTeamPlayerDir + File.separator + ConfigUtils.getProperty("file.conferences.txt");
			teamFile = conferenceTeamPlayerDir + File.separator + ConfigUtils.getProperty("file.teams.txt");
			playerFile = conferenceTeamPlayerDir + File.separator + ConfigUtils.getProperty("file.players.txt");

			log.info("Season ->          " + SEASON);
			log.info("Conference file -> " + conferenceFile);
			log.info("Team file ->       " + teamFile);
			log.info("Player file ->     " + playerFile);

			// conferenceMap = new HashMap<>();
			// teamMap = new HashMap<>();
			// playerMap = new HashMap<>();
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
			System.exit(99);
		}
	}

	public static void go() throws Exception {
		try {
			generateConferenceTeamFiles();
			generatePlayerFile();
			return;
		} catch (Exception e) {
			throw e;
		}
	}

	public static Map<Integer, Map<String, String>> filterPlayers(String homeTeam, String roadTeam) throws Exception {
		try {
			Map<Integer, Map<String, String>> players = getPlayerMap().entrySet().stream()/**/
					.filter(entry -> entry.getValue().get("teamId").compareTo(homeTeam) == 0 || entry.getValue().get("teamId").compareTo(roadTeam) == 0)/**/
					.collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));

			return players;
		} catch (Exception e) {
			throw e;
		}
	}

	private static Map<String, String> generateConferenceTeamFiles() throws Exception {
		try {
			Map<String, String> conferenceUrlMap = new HashMap<>(generateConferenceUrlMap());
			generateConferenceTeamDataFiles(conferenceUrlMap);
			return conferenceUrlMap;
		} catch (Exception e) {
			throw e;
		}
	}

	private static void generatePlayerFile() throws Exception {
		try (BufferedWriter playerWriter = new BufferedWriter(new FileWriter(playerFile, false));) {
			loadDataFiles(false); // loads conferences and teams only

			for (Integer teamId : teamMap.keySet()) {
				String rosterUrl = ESPN_TEAM_ROSTER_URL + String.valueOf(teamId);
				log.info(rosterUrl + " -> " + teamMap.get(teamId));

				Document rosterDoc = JsoupUtils.acquire(rosterUrl);
				if (rosterDoc == null) {
					log.warn(rosterUrl + " -> " + "We cannot acquire the roster document");
					continue;
				}

				Elements rosterEls = JsoupUtils.nullElementCheck(rosterDoc.getElementsByAttributeValue("class", "ResponsiveTable Team Roster"));
				if (rosterEls == null) {
					log.warn(rosterUrl + " -> " + "Cannot acquire the roster elements");
					continue;
				}

				Element rosterEl = rosterEls.first();

				Elements playerTrs = JsoupUtils.nullElementCheck(rosterEl.getElementsByAttribute("data-idx"));
				if (playerTrs == null) {
					log.warn(rosterUrl + " -> " + "Cannot acquire the player table rows");
					continue;
				}

				for (Element playerTr : playerTrs) {
					Elements playerTds = JsoupUtils.nullElementCheck(playerTr.getElementsByTag("td"));
					if (playerTds == null || playerTds.size() != 6) {
						log.warn(rosterUrl + " -> " + "Cannot acquire the td elements for this player");
						break;
					}

					String playerUrl = playerTds.get(0).getElementsByTag("a").first().attr("href");
					String playerId = Arrays.asList(playerUrl.split("/")).stream().filter(f -> NumberUtils.isCreatable(f)).findFirst().get();

					// String completePlayerName =
					// playerTds.get(0).getElementsByTag("a").first().text();
					String completePlayerName = playerTds.get(0).getElementsByTag("img").attr("title");
					String[] firstMiddleLastTokens = completePlayerName.split(" ");
					if (firstMiddleLastTokens == null || firstMiddleLastTokens.length < 2) {
						log.warn(rosterUrl + " -> " + "Cannot acquire the name of this player ");
						break;
					}

					String playerFirstName = "";
					String playerMiddleName = "";
					String playerLastName = "";

					playerFirstName = firstMiddleLastTokens[0].trim();
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

					String playerNumber = "";
					Elements playerNumberEls = JsoupUtils.nullElementCheck(playerTds.get(1).getElementsByTag("span"));
					if (playerNumberEls == null) {
						log.warn(completePlayerName + " -> Cannot acquire player number");
					} else {
						playerNumber = playerNumberEls.first().text();
					}
					String playerPosition = playerTds.get(2).getElementsByTag("div").first().text();

					String heightFeet = "";
					String heightInches = "";
					String heightCm = "";
					String heightData = playerTds.get(3).getElementsByTag("div").first().text();
					String[] heightTokens = heightData.split(" ");
					if (heightTokens == null || heightTokens.length != 2) {
						log.warn(playerId + " -> Cannot acquire player height");
					} else {
						heightFeet = heightTokens[0].replace("\'", "").trim();
						heightInches = heightTokens[1].replace("\"", "").trim();
						heightCm = StringUtils.inchesToCentimeters(heightFeet, heightInches);
					}

					String classYear = playerTds.get(4).getElementsByTag("div").first().text();

					String homeCity = "";
					String homeState = "";

					String[] cityStateTokens = playerTds.get(5).getElementsByTag("div").first().text().split(", ");
					if (cityStateTokens == null || cityStateTokens.length != 2) {
						log.warn(playerId + " -> Cannot acquire player home city and state");
					} else {
						homeCity = cityStateTokens[0];
						homeState = cityStateTokens[1];
					}

					// log.info(playerId + " -> " + playerNumber + " " + playerPosition + " " +
					// playerFirstName + " " + playerMiddleName + " " + playerLastName/**/
					// + " " + heightFeet + "\'" + " " + heightInches + "\"" + " " + heightCm + "cm"
					// + " " + homeCity + ", " + homeState + " " + playerUrl);

					String data = "[id]=" + playerId /**/
							+ ",[teamId]=" + teamId/**/
							+ ",[playerUrl]=" + playerUrl/**/
							+ ",[playerName]=" + completePlayerName/**/
							+ ",[playerFirstName]=" + playerFirstName/**/
							+ ",[playerMiddleName]=" + (playerMiddleName.trim().length() == 0 ? "" : playerMiddleName)/**/
							+ ",[playerLastName]=" + playerLastName.trim()/**/
							+ ",[uniformNumber]=" + playerNumber/**/
							+ ",[position]=" + playerPosition/**/
							+ ",[heightFeet]=" + heightFeet/**/
							+ ",[heightInches]=" + heightInches/**/
							+ ",[heightCm]=" + heightCm/**/
							+ ",[classYear]=" + classYear/**/
							+ ",[homeCity]=" + homeCity/**/
							+ ",[homeState]=" + homeState/**/
					;

					playerWriter.write(data + "\n");
					log.info(data);
				}
			}
		} catch (Exception e) {
			throw e;
		}
	}

	private static void generatePlayerFileBruteForceMethod() throws Exception {
		try {
			PlayerLookupProcessor.go(SEASON);
		} catch (Exception e) {
			throw e;
		}
	}

	private static Map<String, String> generateConferenceUrlMap() throws Exception {

		Map<String, String> conferenceUrlMap = new HashMap<>();

		try {
			Document doc = JsoupUtils.acquire(ConfigUtils.getProperty("espn.com.womens.team.page"));
			if (doc == null) {
				log.error("No content.... exiting");
				System.exit(99);
			}

			doc.getElementsByTag("option").forEach(e -> conferenceUrlMap.put(e.text(), ESPN_HOME + e.attr("data-url")));
		} catch (Exception e) {
			throw e;
		}

		return conferenceUrlMap;
	}

	private static String generateConferenceTeamDataFiles(Map<String, String> conferenceUrlMap) throws Exception {

		FileUtilities.createFileIfDoesNotExist(conferenceFile);
		FileUtilities.createFileIfDoesNotExist(teamFile);

		try (BufferedWriter conferenceWriter = new BufferedWriter(new FileWriter(conferenceFile, false)); /**/
				BufferedWriter teamWriter = new BufferedWriter(new FileWriter(teamFile, false)); /**/

		) {/**/

			conferenceUrlMap.forEach((conferenceShortName, url) -> {
				try {
					if (conferenceShortName.compareTo("All Conferences") != 0 && conferenceShortName.compareTo("hidden") != 0) {
						log.info(conferenceShortName + " -> " + url);
						Document doc = JsoupUtils.acquire(url);

						// conferences
						String conferenceId = url.substring(url.lastIndexOf("/")).replace("/", "");

						Elements elements = JsoupUtils.nullElementCheck(doc.select("div.headline"));
						if (elements == null) {
							log.warn("We have no div.headline elements for this conference short name: " + conferenceShortName);

						} else {
							String data = "[id]=" + conferenceId + ",[shortName]=" + conferenceShortName + ",[longName]=" + elements.first().text();
							conferenceWriter.append(data + "\n");

							// teams
							String teamId = null;
							String teamName = null;

							elements = JsoupUtils.nullElementCheck(doc.select("div.ContentList__Item"));
							if (elements != null) {

								for (Element e : elements) {
									Elements teamNameElements = JsoupUtils.nullElementCheck(e.getElementsByAttributeValue("class", "di clr-gray-01 h5"));
									if (teamNameElements == null) {
										continue;
									}

									teamName = teamNameElements.first().text();

									Elements teamAnchors = JsoupUtils.nullElementCheck(e.getElementsByAttributeValue("class", "AnchorLink"));
									if (teamAnchors == null) {
										continue;
									}

									String teamUrl = teamAnchors.first().getElementsByAttribute("href").first().attr("href");
									if (!StringUtils.isPopulated(teamUrl) || !teamUrl.contains("/")) {
										log.error("Cannot acquire team data");
										System.exit(99);
									}

									teamId = Arrays.asList(teamUrl.split("/")).stream().filter(f -> NumberUtils.isCreatable(f)).collect(Collectors.toList()).get(0);
									data = "[id]=" + teamId + ",[conferenceId]=" + conferenceId + ",[teamName]=" + teamName;
									teamWriter.append(data + "\n");

									// get the roster url
									Optional<Element> optRosterAnchor = teamAnchors.stream().filter(f -> f.text().compareTo("Roster") == 0).findFirst();
									if (optRosterAnchor.isEmpty()) {
										log.error("No roster link available");
										System.exit(99);
									}
								}
							}
						}
					}
				} catch (Exception e) {
					log.error(e.getMessage());
					e.printStackTrace();
					System.exit(99);
				}
			});

		} catch (

		Exception e) {
			throw e;
		}

		return conferenceFile;
	}

	public static void loadDataFiles(boolean includePlayers) throws Exception {

		try {
			if (conferenceMap == null) {
				conferenceMap = FileUtilities.fileDataToMap(conferenceFile);
			}

			if (teamMap == null) {
				teamMap = FileUtilities.fileDataToMap(teamFile);
			}

			if (includePlayers) {
				loadPlayerFile();
			}
		} catch (Exception e) {
			throw e;
		}

		return;
	}

	public static void loadPlayerFile() throws Exception {

		try {
			if (conferenceMap == null) {
				conferenceMap = FileUtilities.fileDataToMap(conferenceFile);
			}

			if (teamMap == null) {
				teamMap = FileUtilities.fileDataToMap(teamFile);
			}

			if (playerMap == null) {
				playerMap = FileUtilities.fileDataToMap(playerFile);
			}
		} catch (Exception e) {
			throw e;
		}

		return;
	}

	public static String getAConferenceId(String teamId) throws Exception {
		try {
			if (teamId == null || teamId.trim().length() == 0) {
				// log.warn("There is no teamId, therefore a conferenceId lookup is not
				// possible");
				return "";
			}
			Map<String, String> thisTeamMap = getTeamMap().get(Integer.valueOf(teamId));
			if (thisTeamMap != null) {
				String conferenceId = getTeamMap().get(Integer.valueOf(teamId)).get("conferenceId");
				if (conferenceId != null) {
					return conferenceId;
				} else {
					return "";
				}
			} else {
				// log.warn("No entry in team map for teamId=" + teamId + " - cannot assign
				// conferenceId");
				return "";
			}
		} catch (Exception e) {
			throw e;
		}
	}

	public static Map<Integer, Map<String, String>> getConferenceMap() {
		return conferenceMap;
	}

	public static Map<Integer, Map<String, String>> getTeamMap() {
		try {
			if (teamMap == null) {
				loadDataFiles(false);
			}
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
			System.exit(99);
		}
		return teamMap;
	}

	public static Map<Integer, Map<String, String>> getPlayerMap() {
		return playerMap;
	}

	public static Set<String> getTeamNamesList() {
		if (teamNamesList != null) {
			return teamNamesList;
		}

		teamNamesList = new HashSet<>(teamMap.values().stream().map(m -> m.get("teamName")).collect(Collectors.toSet()));
		return teamNamesList;
	}

	public static Set<String> getTeamNamesAsTokens(String roadId, String homeId) {
		Set<String> retTokens = new HashSet<>();

		if (roadId == null) {
			roadTeamId = "";
		} else {
			roadTeamId = roadId;
		}

		if (homeId == null) {
			homeTeamId = "";
		} else {
			homeTeamId = homeId;
		}

		// getTeamMap().forEach((k, v) -> log.info(k + " -> " + v.toString()));
		// log.info("homeTeamId = " + homeTeamId);
		// log.info("roadTeamId = " + roadTeamId);

		Set<String> teams = getTeamMap().entrySet().stream()/**/
				.filter(entry -> String.valueOf(entry.getKey()).compareTo(homeTeamId) == 0 || String.valueOf(entry.getKey()).compareTo(roadTeamId) == 0)/**/
				.map(entry -> entry.getValue().get("teamName").toLowerCase().trim())/**/
				.collect(Collectors.toSet());

		for (String teamToken : teams) {
			for (String token : Arrays.asList(teamToken.split(" "))) {
				retTokens.add(token.trim());
			}
		}

		return retTokens;
	}

}
