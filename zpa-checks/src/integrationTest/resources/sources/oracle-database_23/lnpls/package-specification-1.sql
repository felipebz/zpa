-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/package-specification.html
CREATE OR REPLACE PACKAGE trans_data AUTHID DEFINER AS
  TYPE TimeRec IS RECORD (
    minutes SMALLINT,
    hours   SMALLINT);
  TYPE TransRec IS RECORD (
    category VARCHAR2(10),
    account  INT,
    amount   REAL,
    time_of  TimeRec);
  minimum_balance     CONSTANT REAL := 10.00;
  number_processed    INT;
  insufficient_funds  EXCEPTION;
  PRAGMA EXCEPTION_INIT(insufficient_funds, -4097);
END trans_data;
/