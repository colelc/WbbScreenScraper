package driver;

import org.apache.log4j.Logger;

import process.historical.PlayerLookupProcessor;
import utils.ConfigUtils;

public class PlayerDriver {

	private static Logger log = Logger.getLogger(PlayerDriver.class);

	public static void main(String[] args) {
		log.info("THIS IS THE PLAYER DRIVER - BY SEASON ");

		try {
			String season = ConfigUtils.getProperty("season");
			if (season == null || season.trim().length() == 0) {
				log.error("We do not know for which season !  Adjust the season value   if you want to run for all players");
				season = null;
				// return;
			} else {
				log.info("Season value is: " + season);
			}

			PlayerLookupProcessor.go(season);
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}

		log.info("This concludes player lookups");
	}

}
