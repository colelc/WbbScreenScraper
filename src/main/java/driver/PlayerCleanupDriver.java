package driver;

import org.apache.log4j.Logger;

import process.historical.PlayerCleanupProcessor;
import utils.ConfigUtils;

public class PlayerCleanupDriver {

	private static Logger log = Logger.getLogger(PlayerCleanupDriver.class);

	public static void main(String[] args) {
		log.info("Cleaning up the data extracted by the player lookup processor ");

		try {
			String season = ConfigUtils.getProperty("season");
			if (season == null || season.trim().length() == 0) {
				log.error("We do not know for which season !  Adjust the season value   if you want to run for all players");
				season = null;
				// return;
			} else {
				log.info("Season value is: " + season);
			}

			// PlayerCleanupProcessor.consolidate(season);
			PlayerCleanupProcessor.eliminate(season);
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}

		log.info("This concludes player cleanup");
	}

}
