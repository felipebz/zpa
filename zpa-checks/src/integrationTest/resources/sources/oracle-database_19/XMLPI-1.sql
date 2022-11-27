-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/XMLPI.html
SELECT XMLPI(NAME "Order analysisComp", 'imported, reconfigured, disassembled')
   AS "XMLPI" FROM DUAL;