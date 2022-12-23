package process.historical;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import utils.ConfigUtils;
import utils.FileUtils;

public class PlayerCleanupProcessor {

	// private static String BASE_URL;
	private static Logger log = Logger.getLogger(PlayerCleanupProcessor.class);

	static {
		try {
			// BASE_URL = ConfigUtils.getProperty("espn.com.womens.college.basketball");
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
			System.exit(99);
		}
	}

	public static void eliminate(String season) throws Exception {

		String allPlayersUrlFile = ConfigUtils.getProperty("base.all.players.url.file.path")/**/
				+ File.separator + ConfigUtils.getProperty("file.data.all.players.url");

		List<String> all = FileUtils.readFileLines(allPlayersUrlFile);
		log.info(allPlayersUrlFile + " has " + all.size() + " URLs");

		// previously not found
		String notFoundUrlFile = ConfigUtils.getProperty("project.path")/**/
				+ File.separator + "players_not_found.txt";

		List<String> notFound = FileUtils.readFileLines(notFoundUrlFile);
		log.info(notFoundUrlFile + " has " + notFound.size() + " URLs");

		// 503s
		String _503OutputFile = ConfigUtils.getProperty("project.path")/**/
				+ File.separator + "tempOutput.txt";

		List<String> _503s = FileUtils.readFileLines(_503OutputFile)/**/
				.stream()/**/
				.filter(f -> f.contains("We have tried"))/**/
				.map(m -> m.split(" ")[18].replace("/stats", ""))/**/
				.collect(Collectors.toList());/**/

		log.info(_503OutputFile + " has " + _503s.size() + " URLs");

		// filter out the notFounds
		Set<String> filteredForNotFound = all.stream().distinct().filter(f -> !notFound.contains(f)).collect(Collectors.toSet());
		log.info("all list filtered for notFounds has " + filteredForNotFound.size() + " URLs");

		Set<String> filteredForNotFoundAndNot503s = filteredForNotFound.stream().filter(f -> !_503s.contains(f)).collect(Collectors.toSet());
		log.info("all list filtered for 503s and filtered for not founds has " + filteredForNotFoundAndNot503s.size() + " URLs");

		// pull into new file
		String allPlayersFilteredUrlFile = ConfigUtils.getProperty("project.path") /**/
				+ File.separator + "allPlayersFilteredUrl.txt";

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(allPlayersFilteredUrlFile, false))) {
			for (String data : filteredForNotFoundAndNot503s) {
				writer.write(data + "\n");
			}
		} catch (Exception e) {
			throw e;
		}

		return;
	}

	public static void consolidate(String season) throws Exception {

		// input
		String tempPlayerOutputFile1 = ConfigUtils.getProperty("project.path")/**/
				+ File.separator + "tempPlayerOutput_1.txt";

		String tempPlayerOutputFile2 = ConfigUtils.getProperty("project.path")/**/
				+ File.separator + "tempPlayerOutput_2.txt";

		String tempPlayerOutputFile3 = ConfigUtils.getProperty("project.path")/**/
				+ File.separator + "tempPlayerOutput_3.txt";

		// output
		String tempPlayerUrlFile1 = ConfigUtils.getProperty("project.path") /**/
				+ File.separator + "tempPlayerUrl_1.txt";

		String tempPlayerUrlFile2 = ConfigUtils.getProperty("project.path") /**/
				+ File.separator + "tempPlayerUrl_2.txt";

		String tempPlayerUrlFile3 = ConfigUtils.getProperty("project.path") /**/
				+ File.separator + "tempPlayerUrl_3.txt";

		// 503s missed for 1st file only (output file) but will count them for all files
		String tempPlayer503File1 = ConfigUtils.getProperty("project.path")/**/
				+ File.separator + "tempPlayer503_1.txt";

		String tempPlayer503File2 = ConfigUtils.getProperty("project.path")/**/
				+ File.separator + "tempPlayer503_2.txt";

		String tempPlayer503File3 = ConfigUtils.getProperty("project.path")/**/
				+ File.separator + "tempPlayer503_3.txt";

		Set<String> inputs = new HashSet<>();
		inputs.add(tempPlayerOutputFile1);
		inputs.add(tempPlayerOutputFile2);
		inputs.add(tempPlayerOutputFile3);

		for (String inputFile : inputs) {
			log.info("Input file: " + inputFile);

			String urlFile = inputFile.contains("Output_1") ? tempPlayerUrlFile1 : (inputFile.contains("Output_2") ? tempPlayerUrlFile2 : tempPlayerUrlFile3);
			String _503File = inputFile.contains("Output_1") ? tempPlayer503File1 : (inputFile.contains("Output_2") ? tempPlayer503File2 : tempPlayer503File3);

			try (BufferedWriter playerUrlFileWriter = new BufferedWriter(new FileWriter(urlFile, false)); /**/
					BufferedWriter player503FileWriter = new BufferedWriter(new FileWriter(_503File, false));) {

				List<String> hits = filterTempFile(inputFile, "H I T");
				log.info("number of hits = " + hits.size());
				// hits.forEach(h -> log.info(h));

				List<String> _503s = filterTempFile(inputFile, "503?");
				// _503s.forEach(h -> log.info(h));
				log.info("number of 503s = " + _503s.size());

				for (String url : hits) {
					playerUrlFileWriter.write(url + "\n");
				}

				for (String url : _503s) {
					player503FileWriter.write(url + "\n");
				}

				List<String> other = FileUtils.readFileLines(inputFile)/**/
						.stream()/**/
						.filter(f -> !f.contains("No page") && !f.contains("H I T") && !f.contains("503?") && !f.contains("Server returned HTTP response code: 503"))/**/
						.collect(Collectors.toList());/**/
				log.info("Other size is " + other.size());
				// other.forEach(o -> log.info(o));
			} catch (Exception e) {
				throw e;
			}
		}

		// pull into single file
		log.info("Consolidating");
		String finalPlayerUrlFile = ConfigUtils.getProperty("project.path")/**/
				+ File.separator + "allPlayersUrl.txt";

		Set<String> dataFiles = new HashSet<>();
		dataFiles.add(tempPlayerUrlFile1);
		dataFiles.add(tempPlayerUrlFile2);
		dataFiles.add(tempPlayerUrlFile3);
		dataFiles.add(tempPlayer503File1); // adding this one due to previous bug

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(finalPlayerUrlFile, false))) {
			for (String dataFile : dataFiles) {
				for (String url : FileUtils.readFileLines(dataFile)) {
					writer.write(url + "\n");
				}
			}
		} catch (Exception e) {
			throw e;
		}

		return;
	}

	private static List<String> filterTempFile(String playerUrlFile, String targetString) throws Exception {

		try {
			return FileUtils.readFileLines(playerUrlFile)/**/
					.stream()/**/
					.filter(f -> f.contains(targetString))/**/
					.map(m -> m.split(" ")[6])/**/
					.collect(Collectors.toList());/**/
		} catch (Exception e) {
			throw e;
		}

	}

}
