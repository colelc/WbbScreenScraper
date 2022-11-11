package driver;

import org.apache.log4j.Logger;

import random.player.generator.RandomPlayerProcessor;

public class RandomPlayerDriver {

	private static Logger log = Logger.getLogger(RandomPlayerDriver.class);

	public static void main(String[] args) {
		log.info("We are going to pick a random conference, then random team, then random player.  Who will we get?");

		try {
			RandomPlayerProcessor.go();
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}

		log.info("This concludes random player selection");
	}

}
