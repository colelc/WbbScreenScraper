package driver;

import org.apache.log4j.Logger;

import process.historical.CumulativeLookupProcessor;

public class CumulativeLookupDriver {

	private static Logger log = Logger.getLogger(CumulativeLookupDriver.class);

	public static void main(String[] args) {
		log.info("Collecting cumulative home/away team stats from past years ");

		try {
			CumulativeLookupProcessor.go();
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}

		log.info("This concludes cumulative home/away team stats lookups");
	}

}
