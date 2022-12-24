package driver;

import org.apache.log4j.Logger;

import process.DataProcessor;

public class ScreenScrapeDriver {

	private static Logger log = Logger.getLogger(ScreenScrapeDriver.class);

	public static void main(String[] args) {
		log.info("THIS IS THE SCREEN SCRAPE DRIVER");

		try {
			DataProcessor.go();
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}

		log.info("THIS CONCLUDES ALL SCREEN SCRAPING");
	}

}
