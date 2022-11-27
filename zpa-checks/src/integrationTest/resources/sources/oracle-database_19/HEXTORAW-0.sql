-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/HEXTORAW.html
CREATE TABLE test (raw_col RAW(10));

INSERT INTO test VALUES (HEXTORAW('7D'));