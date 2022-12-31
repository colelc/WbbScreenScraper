package driver;

import org.apache.log4j.Logger;

import process.DataProcessor;
import service.DirtyDataService;

public class ScreenScrapeDriver {

	private static Logger log = Logger.getLogger(ScreenScrapeDriver.class);

	public static void main(String[] args) {
		log.info("THIS IS THE SCREEN SCRAPE DRIVER");

		try {
			DataProcessor.go();
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
			try {
				DirtyDataService.restore();
			} catch (Exception e1) {
				log.error(e1.getMessage());
				e1.printStackTrace();
			}
		}

		log.info("THIS CONCLUDES ALL SCREEN SCRAPING");
	}

}
