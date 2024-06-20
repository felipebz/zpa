-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/JSON_ARRAYAGG.html
SELECT JSON_ARRAYAGG(id ORDER BY id RETURNING VARCHAR2(100)) ID_NUMBERS
  FROM id_table;