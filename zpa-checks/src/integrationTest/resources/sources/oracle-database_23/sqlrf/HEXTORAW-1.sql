-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/HEXTORAW.html
SELECT UTL_RAW.CAST_TO_VARCHAR2(HEXTORAW('4041424344'))
  FROM DUAL;