-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/XMLROOT.html
SELECT XMLROOT ( XMLType('<poid>143598</poid>'), VERSION '1.0', STANDALONE YES)
   AS "XMLROOT" FROM DUAL;