package utils;

import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import https.service.HttpsClientService;

public class JsoupUtils {
	private static Logger log = Logger.getLogger(JsoupUtils.class);

	public static int getMaxDataIdxValue(Element element) throws Exception {

		try {
			Elements idxEls = element.getElementsByAttribute("data-idx");
			if (idxEls == null) {
				log.warn("Cannot acquire idxElements");
				return -1;
			}

			Set<Integer> values = new HashSet<>();
			for (Element idxEl : idxEls) {
				values.add(Integer.valueOf(idxEl.attr("data-idx")));
			}

			int max = values.stream()/**/
					.mapToInt(v -> v)/**/
					.filter(f -> f != 0 && f != 6)/**/
					.max()/**/
					.orElseThrow(NoSuchElementException::new);

			return max;
		} catch (Exception e) {
			throw e;
		}
	}

	public static int getMaxIdxElementValueInDoc(Document doc) throws Exception {
		try {
			Elements idxEls = doc.getElementsByAttribute("data-idx");
			if (idxEls == null) {
				log.warn("Cannot acquire idxElements");
				return -1;
			}

			Set<Integer> values = new HashSet<>();
			for (Element idxEl : idxEls) {
				values.add(Integer.valueOf(idxEl.attr("data-idx")));
			}

			int max = values.stream()/**/
					.mapToInt(v -> v)/**/
					// .filter(f -> f != 0 && f != 6)/**/
					.max()/**/
					.orElseThrow(NoSuchElementException::new);

			return max;
		} catch (Exception e) {
			throw e;
		}
	}

	public static Document acquire(String url) throws Exception {
		boolean noDoc = true;
		int tries = 0;
		int MAX_TRIES = 3;

		while (noDoc) {
			try {
				++tries;
				if (tries > MAX_TRIES) {
					log.warn("We have tried " + MAX_TRIES + " times and cannot acquire this document: " + url);
					return null;
				}

				String html = HttpsClientService.jsoupExtraction(url);
				if (html == null) {
					log.warn("No HTML returned for " + url);
					return null;
				}

				return Jsoup.parse(html);
			} catch (InterruptedException e) {
				log.info(url + " -> Interrupted Exception");
				log.error(e.getMessage());
				e.printStackTrace();
			} catch (FileNotFoundException fnfe) {
				log.info(url + " -> No page");
				return null;
			} catch (Exception e) {
				// log.info(url + " -> a 503?");
				log.error(e.getMessage());
				Thread.sleep(60000l);
			} // try catch
		} // while
		return null;
	}

	public static Elements nullElementCheck(Elements elements) {
		if (elements == null || elements.first() == null) {
			// log.info(name + " -> there is no Element object for this name");
			return null;
		}
		return elements;
	}

}
