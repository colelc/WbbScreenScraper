package process;

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

import utils.ConfigUtils;
import utils.FileUtils;
import utils.JsoupUtils;
import utils.StringUtils;

public class BaseProcessor {

	private static String BASE_OUTPUT_PATH;
	private static String ESPN_HOME;
	private static String SEASON;

	private static String dateTrackerFile;
	private static String conferenceFile;
	private static String teamFile;
	private static String playerFile;
	private static String gameStatOutputFile;
	private static String playByPlayOutputFile;
	private static String gamecastFile;
	private static String cumulativeStatsFile;

	private static Set<String> skipDates;

	private static Logger log = Logger.getLogger(BaseProcessor.class);

	public static void go() throws Exception {
		try {
			ESPN_HOME = ConfigUtils.getESPN_HOME();
			BASE_OUTPUT_PATH = ConfigUtils.getProperty("base.output.file.path");
			SEASON = ConfigUtils.getProperty("season");

			conferenceFile = BASE_OUTPUT_PATH + File.separator + SEASON + File.separator + ConfigUtils.getProperty("file.data.conferences");
			teamFile = BASE_OUTPUT_PATH + File.separator + SEASON + File.separator + ConfigUtils.getProperty("file.data.teams");
			playerFile = BASE_OUTPUT_PATH + File.separator + SEASON + File.separator + ConfigUtils.getProperty("file.data.players");

			gameStatOutputFile = BASE_OUTPUT_PATH + File.separator + SEASON + File.separator + ConfigUtils.getProperty("file.data.game.stats");
			playByPlayOutputFile = BASE_OUTPUT_PATH + File.separator + SEASON + File.separator + ConfigUtils.getProperty("file.data.playbyplay.stats");
			gamecastFile = BASE_OUTPUT_PATH + File.separator + SEASON + File.separator + ConfigUtils.getProperty("file.data.gamecast.stats");

			cumulativeStatsFile = BASE_OUTPUT_PATH + File.separator + SEASON + File.separator + ConfigUtils.getProperty("file.cumulative.stats");

			// loadConferencesTeams(); // these files are already built for 2021-22
			String singleGamecastUrl = ConfigUtils.getProperty("single.gamecast.url");
			singleGamecastUrl = singleGamecastUrl == null || singleGamecastUrl.trim().length() == 0 ? null : singleGamecastUrl;

			String singlePlaybyplayUrl = ConfigUtils.getProperty("single.playbyplay.url");
			singlePlaybyplayUrl = singlePlaybyplayUrl == null || singlePlaybyplayUrl.trim().length() == 0 ? null : singlePlaybyplayUrl;

			if (singleGamecastUrl == null && singlePlaybyplayUrl == null) {
				dateTrackerFile = BASE_OUTPUT_PATH + File.separator + SEASON + File.separator + ConfigUtils.getProperty("file.data.dates.processed");
				skipDates = (!FileUtils.createFileIfDoesNotExist(dateTrackerFile)) ? FileUtils.readFileLines(dateTrackerFile).stream().collect(Collectors.toSet()) : new HashSet<>();
				skipDates.forEach(d -> log.info(d + " -> " + "will not process this date"));
				DataProcessor.generateGameDataFiles(skipDates, dateTrackerFile, /**/
						conferenceFile, teamFile, playerFile, /**/
						gameStatOutputFile, playByPlayOutputFile, gamecastFile, /**/
						cumulativeStatsFile);
				return;
			}

			if (singleGamecastUrl != null) {
				DataProcessor.loadDataFiles(conferenceFile, teamFile, playerFile);
				GamecastProcessor.generateGamecastDataSingleUrl(singleGamecastUrl);
				CumulativeStatsProcessor.generateCumulativeStatsSingleGame(singleGamecastUrl);
			}

			if (singlePlaybyplayUrl != null) {
				String thisSeason = ConfigUtils.getProperty("season");
				if (thisSeason == null || thisSeason.trim().length() == 0) {
					log.error("We have a single playbyplay url to process, but we do not know for which season !  Adjust the season value");
					return;
				}

				conferenceFile = BASE_OUTPUT_PATH + File.separator + thisSeason + File.separator + ConfigUtils.getProperty("file.data.conferences");
				teamFile = BASE_OUTPUT_PATH + File.separator + thisSeason + File.separator + ConfigUtils.getProperty("file.data.teams");
				playerFile = BASE_OUTPUT_PATH + File.separator + thisSeason + File.separator + ConfigUtils.getProperty("file.data.players");

				DataProcessor.loadDataFiles(conferenceFile, teamFile, playerFile);
				PlayByPlayProcessor.processPlayByPlaySingleGame(singlePlaybyplayUrl);
			}

		} catch (Exception e) {
			throw e;
		}
	}

	private static Map<String, String> loadConferencesTeams() throws Exception {
		try {
			Map<String, String> conferenceUrlMap = new HashMap<>(generateConferenceUrlMap());
			generateConferenceTeamDataFiles(conferenceUrlMap);
			return conferenceUrlMap;
		} catch (Exception e) {
			throw e;
		}
	}

	private static Map<String, String> generateConferenceUrlMap() throws Exception {

		Map<String, String> conferenceUrlMap = new HashMap<>();

		try {
			Document doc = JsoupUtils.jsoupExtraction(ConfigUtils.getProperty("espn.com.womens.team.page"));
			if (doc == null) {
				log.error("No content.... exiting");
				System.exit(99);
			}
			JsoupUtils.getElementsByTagName(doc, "option").forEach(e -> conferenceUrlMap.put(e.text(), ESPN_HOME + e.attr("data-url")));
		} catch (Exception e) {
			throw e;
		}

		return conferenceUrlMap;
	}

	private static String generateConferenceTeamDataFiles(Map<String, String> conferenceUrlMap) throws Exception {

		try (BufferedWriter conferenceWriter = new BufferedWriter(new FileWriter(conferenceFile, false)); /**/
				BufferedWriter teamWriter = new BufferedWriter(new FileWriter(teamFile, false)); /**/
		// BufferedWriter playerWriter = new BufferedWriter(new FileWriter(playerFile,
		// false));/**/
		) {/**/

			conferenceUrlMap.forEach((conferenceShortName, url) -> {
				try {
					if (conferenceShortName.compareTo("All Conferences") != 0 && conferenceShortName.compareTo("hidden") != 0) {
						log.info(conferenceShortName + " -> " + url);
						Document doc = JsoupUtils.jsoupExtraction(url);

						// conferences
						String conferenceId = url.substring(url.lastIndexOf("/")).replace("/", "");

						Elements elements = JsoupUtils.nullElementCheck(doc.select("div.headline"), "div.headline");
						if (elements == null || elements.first() == null) {
							log.warn("We have no div.headline elements for this conference short name: " + conferenceShortName);

						} else {
							String data = "[id]=" + conferenceId + ",[shortName]=" + conferenceShortName + ",[longName]=" + elements.first().text();
							conferenceWriter.append(data + "\n");

							// teams
							String teamId = null;
							String teamName = null;

							elements = JsoupUtils.nullElementCheck(doc.select("div.ContentList__Item"), "div.ContentList__Item");
							if (elements != null) {

								for (Element e : elements) {
									Elements teamNameElements = JsoupUtils.nullElementCheck(e.getElementsByAttributeValue("class", "di clr-gray-01 h5"), "di clr-gray-01 h5");
									if (teamNameElements == null) {
										continue;
									}

									teamName = teamNameElements.first().text();

									Elements teamAnchors = JsoupUtils.nullElementCheck(e.getElementsByAttributeValue("class", "AnchorLink"), "AnchorLink");
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

									/** String playerOutputFile = */
									// Document document = JsoupUtils.jsoupExtraction(ConfigUtils.getESPN_HOME() +
									// optRosterAnchor.get().attr("href"));
									// PlayerProcessor.generatePlayerFile(playerWriter, teamId, document);
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

}
