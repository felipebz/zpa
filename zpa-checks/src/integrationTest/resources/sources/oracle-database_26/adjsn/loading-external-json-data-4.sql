-- https://docs.oracle.com/en/database/oracle/oracle-database/26/adjsn/loading-external-json-data.html
CREATE JSON COLLECTION TABLE purchaseorders;
INSERT INTO purchaseorders SELECT * FROM json_file_contents;