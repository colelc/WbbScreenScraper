package driver;

import org.apache.log4j.Logger;

import service.ConferenceTeamPlayerService;

public class ConferenceTeamPlayerDriver {

	private static Logger log = Logger.getLogger(ConferenceTeamPlayerDriver.class);

	public static void main(String[] args) {
		log.info("THIS IS THE DRIVER FOR GENERATING CONFERENCES AND TEAMS AND PLAYERS");

		try {
			ConferenceTeamPlayerService.go();
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}

		log.info("THIS CONCLUDES THE GENERATION OF THE CONFERENCES TEAMS AND PLAYERS DATA");
	}

}
