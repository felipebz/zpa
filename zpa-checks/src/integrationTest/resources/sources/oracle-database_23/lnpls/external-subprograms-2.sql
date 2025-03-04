-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/external-subprograms.html
CREATE PROCEDURE java_sleep (
  milli_seconds IN NUMBER
) AS LANGUAGE JAVA NAME 'java.lang.Thread.sleep(long)';
/
CREATE OR REPLACE PROCEDURE sleep (
  milli_seconds IN NUMBER
) AUTHID DEFINER IS
BEGIN
  DBMS_OUTPUT.PUT_LINE(DBMS_UTILITY.get_time());
  java_sleep (milli_seconds);
  DBMS_OUTPUT.PUT_LINE(DBMS_UTILITY.get_time());
END;
/