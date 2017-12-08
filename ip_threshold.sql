CREATE DATABASE log;

USE DATABASE log;

DROP TABLE IF EXISTS ip_threshold;  


CREATE TABLE ip_threshold(           
id INT NOT NULL AUTO_INCREMENT PRIMARY KEY ,          
ip_address VARCHAR(100) NOT NULL,   
request_count INT NOT NULL,   
comments VARCHAR(500) NOT NULL,   
duration_type VARCHAR(20) NOT NULL,   
start_date VARCHAR(20) NOT NULL   )   
