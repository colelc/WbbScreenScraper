package process.player;

import java.io.BufferedWriter;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import utils.JsoupUtils;
import utils.StringUtils;

public class PlayerProcessor {

	private static Logger log = Logger.getLogger(PlayerProcessor.class);

	public static void generatePlayerFile(BufferedWriter playerWriter, String teamId, Document doc) throws Exception {

		String playerId = "";
		String playerName = "";
		String playerFirstName = "";
		String playerMiddleName = "";
		String playerLastName = "";
		String playerNumber = "";
		String position = "";
		String heightFeet = "";
		String heightInches = "";
		String heightCm = "";
		String classYear = "";
		String homeCity = "";
		String homeState = "";
		String playerUrl = "";

		try {
			// Document doc = JsoupUtils.jsoupExtraction(ConfigUtils.getESPN_HOME() +
			// url);// .attr("href"));
			// Document doc = JsoupUtils.jsoupExtraction(url);

			if (doc == null) {
				log.error("No content.... exiting");
				System.exit(99);
			}

			Elements playerElements = JsoupUtils.nullElementCheck(doc.select("tr.Table__TR"), "tr.Table__TR");
			if (playerElements == null) {
				return;
			}

			for (Element playerElement : playerElements) {
				Elements playerDetails = JsoupUtils.nullElementCheck(playerElement.getElementsByTag("td"), "td");
				if (playerDetails == null || playerDetails.first() == null) {
					continue;
				}

				if (playerDetails.size() != 5) {
					log.warn("Not 5 TD cells in this player detail row... skipping");
					break;
				}

				boolean completePlayerProfile = true;

				for (int i = 0; i < playerDetails.size(); i++) {
					Element playerDetail = playerDetails.get(i);

					switch (i) {
					case 0:
						playerUrl = JsoupUtils.nullElementCheck(playerDetail.getElementsByTag("a"), "a").first().attr("href");
						if (playerUrl == null) {
							log.warn("No player url found... skipping");
							completePlayerProfile = false;
							break;
						}

						playerName = JsoupUtils.nullElementCheck(playerDetail.getElementsByTag("a"), "a").first().text();
						String[] playerNameTokens = playerName.split(" ");
						if (playerNameTokens == null || playerNameTokens.length == 0) {
							completePlayerProfile = false;
							break;
						}
						playerFirstName = playerNameTokens[0].trim();

						if (playerNameTokens.length == 2) {
							playerLastName = playerNameTokens[1].trim();
						} else if (playerNameTokens.length == 3) {
							playerMiddleName = playerNameTokens[1].trim();
							playerLastName = playerNameTokens[2].trim();
						} else {
							log.warn("What to do with playerNameTokens!! " + playerNameTokens.toString());
							return;
						}

						playerId = Arrays.asList(playerUrl.split("/")).stream().filter(f -> NumberUtils.isCreatable(f)).collect(Collectors.toList()).get(0);

						Elements playerNumberElements = JsoupUtils.nullElementCheck(playerDetail.getElementsByAttributeValue("class", "pl2 n10"), "pl2 n10");
						if (playerNumberElements == null) {
							// log.warn("No player number detected... skipping");
							completePlayerProfile = false;
							break;
						}
						playerNumber = playerNumberElements.first().text();
						// log.info("Player Number: " + playerNumber);
						break;
					case 1:
						if (completePlayerProfile) {
							position = playerDetail.text();
							// log.info("Position: " + position);
						}
						break;
					case 2:
						if (completePlayerProfile) {
							String feetInches = playerDetail.text();
							// log.info("Height: " + feetInches);
							String[] heightTokens = feetInches.split(" ");
							if (heightTokens == null || heightTokens.length != 2) {
								completePlayerProfile = false;
								break;
							}
							heightFeet = heightTokens[0].replace("\'", "").trim();
							heightInches = heightTokens[1].replace("\"", "").trim();
							heightCm = StringUtils.inchesToCentimeters(heightFeet, heightInches);
						}
						break;
					case 3:
						if (completePlayerProfile) {
							classYear = playerDetail.text();
							// log.info("Class Year: " + classYear);
						}
						break;
					case 4:
						if (completePlayerProfile) {
							String homeTown = playerDetail.text();
							// log.info("Hometown: " + homeTown);
							String[] homeTokens = homeTown.split(",");
							if (homeTokens == null || homeTokens.length != 2) {
								completePlayerProfile = false;
								break;
							}
							homeCity = homeTokens[0].trim();
							homeState = homeTokens[1].trim();
						}
						break;
					default:
						log.warn("something else");
					}
				}

				if (completePlayerProfile) {
					// write it to file.
					String data = "[id]=" + playerId /**/
							+ ",[teamId]=" + teamId/**/
							+ ",[playerUrl]=" + playerUrl/**/
							+ ",[playerName]=" + playerName/**/
							+ ",[playerFirstName]=" + playerFirstName/**/
							+ ",[playerMiddleName]=" + playerMiddleName/**/
							+ ",[playerLastName]=" + playerLastName/**/
							+ ",[uniformNumber]=" + playerNumber/**/
							+ ",[position]=" + position/**/
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

		return;
	}

}
