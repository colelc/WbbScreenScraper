package utils;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import https.service.HttpsClientService;

public class JsoupUtils {
	private static Logger log = Logger.getLogger(JsoupUtils.class);

	public static String extractionByAttributeAndValue(Element element, String attribute, String value) throws Exception {
		try {
			if (JsoupUtils.filterByAttributeAndValue(element, attribute, value)) {
				return element.getElementsByAttributeValue(attribute, value).first().text();
			}
		} catch (Exception e) {
			throw e;
		}
		return null;
	}

	public static Document parseStringToDocument(String text) throws Exception {

		if (!StringUtils.isPopulated(text)) {
			log.warn("No text provided for Jsoup parsing");
			return null;
		}

		try {
			return Jsoup.parse(text);
		} catch (Exception e) {
			throw e;
		}
	}

	public static Elements getElementsByTagName(Document document, String tagName) throws Exception {

		if (document == null) {
			log.warn("NULL Jsoup document object");
			return null;
		}

		try {
			Elements elements = document.getElementsByTag(tagName);

//			elements.forEach(e -> {
//				log.info(e.text() + " -> " + e.attr("data-url"));
//			});

			return elements;
		} catch (Exception e) {
			throw e;
		}
	}

	public static Document jsoupExtraction(String url) throws Exception {
		try {
			url = url.contains("http://") ? url.replace("http://", "https://") : url;
			String html = FileUtils.streamHttpsUrlConnection(HttpsClientService.getHttpsURLConnection(url), false);

			if (contentCheck(html, url)) {
				return parseStringToDocument(html);
			} else {
				return null;
			}

		} catch (Exception e) {
			throw e;
		}
	}

	public static boolean filterByClass(Element element, String target) {
		Elements filteredElements = element.getElementsByClass(target);
		if (filteredElements != null && filteredElements.first() != null) {
			return true;
		}
		return false;
	}

	public static boolean filterByTag(Element element, String target) {
		Elements filteredElements = element.getElementsByTag(target);
		if (filteredElements != null && filteredElements.first() != null) {
			return true;
		}
		return false;
	}

	public static boolean filterByAttributeAndValue(Element element, String attribute, String value) {
		Elements filteredElements = element.getElementsByAttributeValue(attribute, value);
		if (filteredElements != null && filteredElements.first() != null) {
			return true;
		}
		return false;
	}

	private static boolean contentCheck(String html, String url) {
		if (!StringUtils.isPopulated(html)) {
			log.warn(url + " : no HTML text obtained - perhaps a redirect took place ?");
			return false;
		}
		return true;
	}

	public static Elements nullElementCheck(Elements elements, String name) {
		if (elements == null || elements.first() == null) {
			log.info(name + " -> there is no Element object for this name");
			return null;
		}
		return elements;
	}

}
