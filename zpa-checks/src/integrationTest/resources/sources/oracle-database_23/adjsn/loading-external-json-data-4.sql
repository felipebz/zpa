-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/loading-external-json-data.html
INSERT INTO purchaseorders SELECT * FROM json_file_contents;