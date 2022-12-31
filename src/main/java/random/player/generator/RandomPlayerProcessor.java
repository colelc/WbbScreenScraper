package random.player.generator;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import service.ConferenceTeamPlayerService;

public class RandomPlayerProcessor {

	private static Logger log = Logger.getLogger(RandomPlayerProcessor.class);

	public static void go() throws Exception {
		try {
			ConferenceTeamPlayerService.loadDataFiles(true);
			getTheRandomPlayer();
		} catch (Exception e) {
			throw e;
		}
	}

	private static void getTheRandomPlayer() throws Exception {
		try {

			List<Map<String, String>> teams = ConferenceTeamPlayerService.getTeamMap().values().stream().collect(Collectors.toList());
			List<Map<String, String>> players = ConferenceTeamPlayerService.getPlayerMap().values().stream().collect(Collectors.toList());
			List<Map<String, String>> conferences = ConferenceTeamPlayerService.getConferenceMap().values().stream().collect(Collectors.toList());

			Integer playerIndex = null;

			Integer conferenceIndex = generateRandomNumber(conferences.size() - 1);
			if (conferenceIndex.intValue() > conferences.size() - 1) {
				log.warn("PROBLEM with conferenceId calculation");
			}

			Map<String, String> conferenceMap = conferences.get(conferenceIndex);
			log.info("Conference index: " + String.valueOf(conferenceIndex) + " -> " + conferenceMap.toString());

			Integer conferenceId = ConferenceTeamPlayerService.getConferenceMap().entrySet().stream()/**/
					.filter(entry -> entry.getValue().get("shortName").compareTo(conferenceMap.get("shortName")) == 0 && /**/
							entry.getValue().get("longName").compareTo(conferenceMap.get("longName")) == 0)/**/
					.findFirst()/**/
					.map(m -> m.getKey())/**/
					.get();

			log.info("conferenceId is " + conferenceId);

			List<Map<String, String>> teamsInThisConference = teams.stream()/**/
					.filter(f -> f.get("conferenceId").compareTo(String.valueOf(conferenceId)) == 0)/**/
					.collect(Collectors.toList());

			teamsInThisConference.forEach(entry -> log.info(entry));

			Integer teamIndex = generateRandomNumber(teamsInThisConference.size() - 1);

			Map<String, String> teamMap = teamsInThisConference.get(teamIndex);
			log.info("Team index: " + String.valueOf(teamIndex) + " -> " + teamMap.toString());

			Integer teamId = ConferenceTeamPlayerService.getTeamMap().entrySet().stream()/**/
					.filter(entry -> entry.getValue().get("teamName").compareTo(teamMap.get("teamName")) == 0)/**/
					.findFirst()/**/
					.map(m -> m.getKey())/**/
					.get();

			log.info("teamId is " + teamId);

			List<Map<String, String>> playersOnThisTeam = players.stream()/**/
					.filter(f -> f.get("teamId").compareTo(String.valueOf(teamId)) == 0)/**/
					.collect(Collectors.toList());

			playersOnThisTeam.forEach(entry -> log.info(entry));

			playerIndex = generateRandomNumber(playersOnThisTeam.size() - 1);
			log.info("playerIndex is " + playerIndex);

			Map<String, String> winner = playersOnThisTeam.get(playerIndex);

			log.info("The winner is");
			winner.forEach((k, v) -> log.info(k + " -> " + v));
		} catch (Exception e) {
			throw e;
		}
	}

	public static Integer generateRandomNumber(int maxSize) throws Exception {
		try {
			Random random = new Random();
			int value = random.ints(0, maxSize - 1).findAny().getAsInt();
			return Integer.valueOf(value);
		} catch (Exception e) {
			throw e;
		}
	}

}
