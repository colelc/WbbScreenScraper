package driver;

import org.apache.log4j.Logger;

import process.historical.PlayerLookupProcessor;
import utils.ConfigUtils;

public class PlayerLookupDriver {

	private static Logger log = Logger.getLogger(PlayerLookupDriver.class);

	public static void main(String[] args) {
		log.info("Looking for players from past years ");

		try {
			String season = ConfigUtils.getProperty("season");
			if (season == null || season.trim().length() == 0) {
				log.error("W do not know for which season !  Adjust the season value   if you want to run for all players");
				season = null;
				// return;
			} else {
				log.info("Season value is: " + season);
			}

			// PlayerLookupProcessor.go(season);
			PlayerLookupProcessor.consolidateLogOutput(season);
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}

		log.info("This concludes player lookups");
	}

}
