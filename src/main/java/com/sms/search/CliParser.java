package com.sms.search;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class CliParser {
	
	String inputFilePath = "./input.txt";
	String outputFilePath = "./output.txt";
	Integer threadsAmount = 4;
	
	public String getInputFilePath() {
		return inputFilePath;
	}

	public String getOutputFilePath() {
		return outputFilePath;
	}

	public Integer getThreadsAmount() {
		return threadsAmount;
	}

	public void parseArguments(String[] args) {
		Options options = new Options();

        Option input = new Option("i", "input", true, "input file path, default value './input.txt'");
        input.setRequired(false);
        options.addOption(input);

        Option output = new Option("o", "output", true, "output file, default value './output.txt'");
        output.setRequired(false);
        options.addOption(output);
        
        Option threads = new Option("t", "threads", true, "amount of threads for prallel http requests,"
        		+ " default value 4");
        threads.setRequired(false);
        threads.setType(Integer.class);
        options.addOption(threads);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);

            System.exit(1);
            return;
        }

        String i = cmd.getOptionValue("input");
        String o = cmd.getOptionValue("output");
        String t = cmd.getOptionValue("threads");
        if (i != null) inputFilePath = i;
        if (o != null) outputFilePath = o;
        try {
        	if (t != null) threadsAmount = Integer.parseInt(t);
        } catch (NumberFormatException e) {
        	System.err.println("Unable to parse 'treads' argument, continue with default value " + threadsAmount);
        }
	}

}
