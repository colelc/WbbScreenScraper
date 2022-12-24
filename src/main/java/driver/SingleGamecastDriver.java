package driver;

import org.apache.log4j.Logger;

import process.single.SingleGamecastProcessor;

public class SingleGamecastDriver {

	private static Logger log = Logger.getLogger(SingleGamecastDriver.class);

	public static void main(String[] args) {
		log.info("THIS IS THE SINGLE GAMECAST DRIVER");

		try {
			SingleGamecastProcessor.go();
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}

		log.info("THIS CONCLUDES ALL WORK");
	}

}
