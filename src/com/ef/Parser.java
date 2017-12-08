package com.ef;

import org.apache.commons.cli.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Parser {

//    cd P:\project-space\LogParser
//    p:
//    C:\Users\kanodiap\AppData\Local\Microsoft\AppV\Client\Integration\2438946D-451F-4C8D-8AF7-9BFE9990B0DB\Root\VFS\ProgramFilesX64\Java\jdk1.8.0_40\bin\java -cp P:\project-space\LogParser\commons-cli-1.4.jar;.\src\ com.ef.Parser --startDate=2017-01-01.15:00:00 --duration=hourly --threshold=200 --input-file=P:\project-space\LogParser\access.logz --config=P:\project-space\LogParser\db.properties
//    C:\Users\kanodiap\AppData\Local\Microsoft\AppV\Client\Integration\2438946D-451F-4C8D-8AF7-9BFE9990B0DB\Root\VFS\ProgramFilesX64\Java\jdk1.8.0_40\bin\java -cp P:\project-space\LogParser\commons-cli-1.4.jar;.\src\ com.ef.Parser --startDate=2017-01-01.00:00:00 --duration=daily --threshold=500 --input-file=P:\project-space\LogParser\access.log --config=P:\project-space\LogParser\db.properties
//    C:\Users\kanodiap\AppData\Local\Microsoft\AppV\Client\Integration\2438946D-451F-4C8D-8AF7-9BFE9990B0DB\Root\VFS\ProgramFilesX64\Java\jdk1.8.0_40\bin\javac -classpath P:\project-space\LogParser\commons-cli-1.4.jar;P:\project-space\LogParser\jconn3-jconn3.jar  P:\project-space\LogParser\src\com\ef\Parser.java


//    TABLE SPECS
//    DROP TABLE ip_threshold;
//    CREATE TABLE ip_threshold(
//            id INT IDENTITY PRIMARY KEY ,
//            ip_address VARCHAR(100) NOT NULL,
//    request_count INT NOT NULL,
//    comments VARCHAR(500) NOT NULL,
//    duration_type VARCHAR(20) NOT NULL,
//    start_date VARCHAR(20) NOT NULL
//    )
//    select * from ip_threshold

    private static final String INSERT_SQL = "INSERT INTO ip_threshold"
            + "(ip_address ,request_count ,comments ,duration_type ,start_date) VALUES"
            + "(?,?,?,?,?)";
    private static final String DATE_FORMAT = "yyyy-MM-dd.HH:mm:ss";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);
    private static final DateTimeFormatter FILE_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String DURATION_HOURLY = "hourly";
    private static final String DURATION_DAILY = "daily";
    private static final String START_DATE_CMD_OPTION = "startDate";
    private static final String THRESHOLD_CMD_OPTION = "threshold";
    private static final String DURATION_CMD_OPTION = "duration";
    private static final String INPUT_FILE_CMD_OPTION = "input-file";
    private static final String DB_PROPERTIES_FILE_CMD_OPTION = "config";
    private static final String HELP_CMD_OPTION = "help";
    private static final String DELIMITER = "\\|";
    private static final String COMMENTS = "This IP has breached the %s Limit of : %s";


    public static void main(String[] cmdLineArgs) throws Exception {
        long start = System.currentTimeMillis();
        CommandLineParser cmdLineParser = new DefaultParser();
        Options options = buildOptions();
        CommandLine cmdLineGlobal = null;
        try {
            cmdLineGlobal = cmdLineParser.parse(options, cmdLineArgs);
        } catch (ParseException e) {
            printHelp();
            return;
        }
        if (cmdLineGlobal.hasOption(HELP_CMD_OPTION)) {
            printHelp();
            return;
        }
        String startDate = null;
        String duration = null;
        String thresholdAsString = null;
        Integer threshold = null;
        LocalDateTime startDateTime = null;
        File inputFile = null;
        File dbConfigFile = null;
        Properties dbProperties = null;
        StringBuilder exception = new StringBuilder("");
        try {
            startDate = (String) cmdLineGlobal.getParsedOptionValue(START_DATE_CMD_OPTION);
            startDateTime = LocalDateTime.parse(startDate, DATE_TIME_FORMATTER);
        } catch (Exception e) {
            exception.append("\n").append(buildOptions().getOption(START_DATE_CMD_OPTION).getDescription());
        }
        duration = (String) cmdLineGlobal.getParsedOptionValue(DURATION_CMD_OPTION);
        if (!(duration.equals(DURATION_DAILY) || duration.equals(DURATION_HOURLY))) {
            exception.append("\n").append(buildOptions().getOption(DURATION_CMD_OPTION).getDescription());
        }
        try {
            thresholdAsString = (String) cmdLineGlobal.getParsedOptionValue(THRESHOLD_CMD_OPTION);
            threshold = Integer.parseInt(thresholdAsString);
        } catch (Exception e) {
            exception.append("\n").append(buildOptions().getOption(THRESHOLD_CMD_OPTION).getDescription());
        }
        try {
            inputFile = (File) cmdLineGlobal.getParsedOptionValue(INPUT_FILE_CMD_OPTION);
        } catch (Exception e) {
            exception.append("\n").append(buildOptions().getOption(INPUT_FILE_CMD_OPTION).getDescription());
        }
        try {
            dbConfigFile = (File) cmdLineGlobal.getParsedOptionValue(DB_PROPERTIES_FILE_CMD_OPTION);
            dbProperties = new Properties();
            dbProperties.load(new FileInputStream(dbConfigFile));
        } catch (Exception e) {
            exception.append("\n").append(buildOptions().getOption(DB_PROPERTIES_FILE_CMD_OPTION).getDescription());
        }
        System.out.println("");
        System.out.println("---------- Input ------------");
        System.out.println(START_DATE_CMD_OPTION + " : " + startDate);
        System.out.println(DURATION_CMD_OPTION + " : " + duration);
        System.out.println(THRESHOLD_CMD_OPTION + " : " + thresholdAsString);
        System.out.println(INPUT_FILE_CMD_OPTION + " : " + inputFile);
        System.out.println(DB_PROPERTIES_FILE_CMD_OPTION + " : " + dbConfigFile);
        System.out.println("");
        System.out.println("---------- Output ------------");
        if (exception.length() > 0) {
            System.out.println(exception.append("\n").toString());
            printHelp();
            return;
        }

        BufferedReader in = null;
        HashMap<String, Integer> ipCounter = new HashMap<String, Integer>();
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), "UTF8"));
            String line;
            while ((line = in.readLine()) != null) {
                String[] lineSplit = line.split(DELIMITER);
                if (lineSplit.length == 5) {
                    String date = lineSplit[0];
                    if (isValidDate(LocalDateTime.parse(date, FILE_DATE_TIME_FORMATTER), startDateTime, duration)) {
                        String ip = lineSplit[1];
                        Integer counter = 0;
                        if (ipCounter.containsKey(ip)) {
                            counter = ipCounter.get(ip);
                        }
                        ipCounter.put(ip, ++counter);
                    }
                }
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (in != null) {
                in.close();
            }
        }
        System.out.println("Below listed Ips have breached the " + duration + " threshold of " + threshold);
        int i = 0;
        String comments = String.format(COMMENTS,duration , threshold);

        Connection connection = getConnection(dbProperties);
        PreparedStatement preparedStatement = connection.prepareStatement(INSERT_SQL);
        for (Map.Entry<String, Integer> entry : ipCounter.entrySet()) {
            if (entry.getValue() >= threshold) {
                System.out.println(++i + ") " + entry.getKey());
                preparedStatement.setString(1,entry.getKey());
                preparedStatement.setInt(2,entry.getValue());
                preparedStatement.setString(3,comments);
                preparedStatement.setString(4,duration);
                preparedStatement.setString(5,startDate);
                preparedStatement.addBatch();
            }
        }
        preparedStatement.executeBatch();
        connection.commit();
        long end = System.currentTimeMillis();
        System.out.println(" time taken : " + (end - start));
    }

    private static boolean isValidDate(LocalDateTime inputDateTime, LocalDateTime startDateTime, String duration) {
        LocalDateTime endDateTime = null;
        if (DURATION_DAILY.equals(duration)) {
            endDateTime = startDateTime.plusHours(24L);
        }
        if (DURATION_HOURLY.equals(duration)) {
            endDateTime = startDateTime.plusHours(1L);
        }
        if ((inputDateTime.isBefore(endDateTime) && inputDateTime.isAfter(startDateTime))
                || inputDateTime.isEqual(startDateTime)) {
            return true;
        }
        return false;
    }

    private static Options buildOptions() {
        Options options = new Options();
        Option durationOption = Option.builder().longOpt(DURATION_CMD_OPTION).required(true).desc("Duration. Valid values are \"hourly\" or \"daily\"").hasArg().type(String.class).valueSeparator('=').build();
        options.addOption(durationOption);
        Option startDateOption = Option.builder().longOpt(START_DATE_CMD_OPTION).required(true).desc("Start date. Expected format " + DATE_FORMAT).hasArg().type(String.class).valueSeparator('=').build();
        options.addOption(startDateOption);
        Option thresholdOption = Option.builder().longOpt(THRESHOLD_CMD_OPTION).required(true).desc("Threshold. Valid value any integer").hasArg().type(String.class).valueSeparator('=').build();
        options.addOption(thresholdOption);
        Option inputFileOption = Option.builder().longOpt(INPUT_FILE_CMD_OPTION).required(true).desc("Input File. Valid log file").hasArg().type(File.class).valueSeparator('=').build();
        options.addOption(inputFileOption);
        Option dbConfigOption = Option.builder().longOpt(DB_PROPERTIES_FILE_CMD_OPTION).required(true).desc("Input File. Valid log file").hasArg().type(File.class).valueSeparator('=').build();
        options.addOption(dbConfigOption);
        Option helpOption = Option.builder().longOpt(HELP_CMD_OPTION).optionalArg(true).desc("Help").build();
        options.addOption(helpOption);
        return options;
    }

    private static void printHelp() {
        System.out.println("");
        System.out.println("");
        System.out.println("-------------------------------");
        new HelpFormatter().printHelp("Log Parser", buildOptions());
        System.out.println("-------------------------------");
        System.out.println("");
        System.out.println("");
    }


    private static Connection getConnection(Properties dbProperties) throws Exception {
        String driver = dbProperties.getProperty("jdbc.driver");
        String url = dbProperties.getProperty("jdbc.url");
        String username = dbProperties.getProperty("jdbc.username");
        String password = dbProperties.getProperty("jdbc.password");
        Class.forName(driver);
        return DriverManager.getConnection(url, username, password);
    }

}
