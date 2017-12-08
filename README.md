# LogParser
LogParser


download the project to folder P:\project-space\LogParser

goto P:\project-space\LogParser

edit the db.properties with the MYsql credentials and driver details

run the commands from ip_threshold.sql
the sql scripts created for database named 'Log'

in case u need to change database change the scripts and java code for schema name changes.

sample compile run cmd

javac -classpath P:\project-space\LogParser\commons-cli-1.4.jar;P:\project-space\LogParser\mysql-connector-java-6.0.2.jar; P:\project-space\LogParser\src\com\ef\Parser.java

sample cmd line run

java -cp P:\project-space\LogParser\commons-cli-1.4.jar;P:\project-space\LogParser\mysql-connector-java-6.0.2.jar;.\src\ com.ef.Parser --startDate=2017-01-01.15:00:00 --duration=hourly --threshold=200 --input-file=P:\project-space\LogParser\access.log --config=P:\project-space\LogParser\db.properties

java -cp P:\project-space\LogParser\commons-cli-1.4.jar;P:\project-space\LogParser\mysql-connector-java-6.0.2.jar;.\src\ com.ef.Parser --startDate=2017-01-01.00:00:00 --duration=daily --threshold=500 --input-file=P:\project-space\LogParser\access.log --config=P:\project-space\LogParser\db.properties


After code execution has completed successfuly.
execute the select sqls to find the results.

hourly should return 2 records
daily should return 15 records
