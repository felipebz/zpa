-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/release-changes.html
SELECT TEXT FROM USER_SOURCE WHERE NAME='HELLO';
DBMS_OUTPUT.PUT_LINE('Hello there');
END;