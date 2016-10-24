package com.sms.search;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MainClass {

	public static void main(String [] args) {
		
		// parse cli arguments
		CliParser cliParser = new CliParser();
		cliParser.parseArguments(args);
		
		List<String> wordList = null;
		try {
			// TODO? if the file is really huge need to read it by some chunks and process one by one
			// but it also means the intermediate result 'linksSet' should be stored in a file (or DB) too
			Path inputPath = FileSystems.getDefault().getPath(cliParser.getInputFilePath());
			wordList = Files.readAllLines(inputPath, StandardCharsets.UTF_8);
		} catch (IOException e) {
			System.err.println("Unable to read input file: " + cliParser.getInputFilePath());
			return;
		}

		// invoke search request in parallel
		// amount of simultaneous http request is the same as the thread pool size
		ExecutorService executor = Executors.newFixedThreadPool(cliParser.getThreadsAmount());
		List<Future<List<String>>> searchResult = null;
		try {
			searchResult = executor.invokeAll(
					wordList.stream().map(toCallable()).collect(Collectors.toList()));
			executor.shutdown();
		} catch (InterruptedException e) {
			System.err.println("Threads execution was interrupted");
			return;
		}

		// store all found links in the set to eliminate duplicates
		Set<String> linksSet = new HashSet<>();
		for (Future<List<String>> future : searchResult) {
			try {
				linksSet.addAll(future.get());
			} catch (InterruptedException | ExecutionException e) {
				System.err.println("Unable to get search result");
			}
		}

		// built the final output
		Map<String, Integer> domainsMap = new HashMap<>();
		for(String link : linksSet) {
			try {
				String domain = exstract2DomainFromLink(link);
				Integer amount = domainsMap.get(domain);
				if (amount == null) {
					domainsMap.put(domain, 1);
				} else {
					domainsMap.put(domain, ++amount);
				}
			} catch (URISyntaxException e) {
				System.err.println("Unable to extract domain from link, the link is skipped: " + link);
			}
		}
		
		// build and write pretty json
		try {
			String json = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(domainsMap);
			System.out.println(json);
			PrintWriter writer = new PrintWriter(cliParser.getOutputFilePath(), "UTF-8");
			writer.print(json);
			writer.close();
		} catch (JsonProcessingException | UnsupportedEncodingException | FileNotFoundException e) {
			System.err.println("Unable to write result to the file: " + cliParser.getOutputFilePath());
			return;
		}
	}
	
	private static <T> Function<String ,Callable<List<String>>> toCallable() {
		return word -> () -> {
			// do not process an empty lines
			if (word.trim().isEmpty()) return Collections.emptyList();
			
			CloseableHttpClient httpclient = HttpClients.createDefault();
			HttpGet httpGet = new HttpGet("https://blogs.yandex.ru/search.rss?text=" + word);
			CloseableHttpResponse response = httpclient.execute(httpGet);
			try {
				HttpEntity entity = response.getEntity();

				XmlLinkParser parser = new XmlLinkParser();
				List<String> links = parser.parse(entity.getContent());
				// ensure the body is fully consumed
				EntityUtils.consume(entity);
				return links;
			} finally {
				response.close();
			}
	    };
	}
	
	/**
	 * 
	 * @param link - the url
	 * @return a string built from 1st and 2nd level domains
	 * @throws URISyntaxException
	 */
	private static String exstract2DomainFromLink(String link) throws URISyntaxException{
		URI uri = new URI(link);
		String[] domains = uri.getHost().split("\\.");
		return (domains[domains.length - 2] + "." + domains[domains.length - 1]);
	}
}
