-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-DATABASE-LINK.html
UPDATE employees@remote
   SET salary=salary*1.1
   WHERE last_name = 'Baer';