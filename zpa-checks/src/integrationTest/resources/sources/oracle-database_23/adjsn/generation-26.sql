-- https://docs.oracle.com/en/database/oracle/oracle-database/23/adjsn/generation.html
CREATE OR REPLACE TYPE my_mailing_address_type
  AS OBJECT(Street VARCHAR2(80),
            City   VARCHAR2(80),
            State  CHAR(2),
            Zip    VARCHAR2(10));

CREATE TABLE t1 (col1 my_mailing_address_type);

INSERT INTO t1 VALUES (my_mailing_address_type('street1', 'city1', 'CA',
    '12345'));

SELECT json_object(col1) FROM t1;