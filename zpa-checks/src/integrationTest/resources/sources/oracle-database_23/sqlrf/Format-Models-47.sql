-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Format-Models.html
SELECT TO_CHAR(TO_DATE('0207', 'fxmm/yy'), 'mm/yy') FROM DUAL;
SELECT TO_CHAR(TO_DATE('0207', 'fxmm/yy'), 'mm/yy') FROM DUAL;
                       *
ERROR at line 1:
ORA-01861: literal does not match format string