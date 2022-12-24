package driver;

import org.apache.log4j.Logger;

import process.single.SinglePlaybyplayProcessor;

public class SinglePlaybyplayDriver {

	private static Logger log = Logger.getLogger(SinglePlaybyplayDriver.class);

	public static void main(String[] args) {
		log.info("THIS IS THE SINGLE PLAY-BY-PLAY DRIVER");

		try {
			SinglePlaybyplayProcessor.go();
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}

		log.info("THIS CONCLUDES ALL WORK");
	}

}
