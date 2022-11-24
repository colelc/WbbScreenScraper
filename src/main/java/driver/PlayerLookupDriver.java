package driver;

import org.apache.log4j.Logger;

import process.PlayerLookupProcessor;

public class PlayerLookupDriver {

	private static Logger log = Logger.getLogger(PlayerLookupDriver.class);

	public static void main(String[] args) {
		log.info("Looking for players from past years ");

		try {
			PlayerLookupProcessor.go();
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}

		log.info("This concludes player lookups");
	}

}
