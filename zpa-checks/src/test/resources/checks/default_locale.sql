ALTER SESSION SET PLSQL_OPTIMIZE_LEVEL = 2;

declare
  type r_item IS RECORD (a number);
  n_item number;
begin
  select 1 INTO :x from dual;
  INSERT INTO t_item VALUES (1); -- Noncompliant
  -- the name written in upper case is the variable declared in lower case
  INSERT INTO t_item VALUES N_ITEM; -- Noncompliant
end;
/
