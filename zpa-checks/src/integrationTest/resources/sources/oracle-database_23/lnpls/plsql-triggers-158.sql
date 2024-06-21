-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-triggers.html
CREATE TABLE event_table (col VARCHAR2(2030));
DECLARE
  sql_text ora_name_list_t;
  n PLS_INTEGER;
  v_stmt VARCHAR2(2000);
BEGIN
  n := ora_sql_txt(sql_text);

  FOR i IN 1..n LOOP
    v_stmt := v_stmt || sql_text(i);
  END LOOP;

  INSERT INTO event_table VALUES ('text of
    triggering statement: ' || v_stmt);
END;