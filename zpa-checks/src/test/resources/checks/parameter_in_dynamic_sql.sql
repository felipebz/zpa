create procedure find_crate(p_code varchar2, p_table varchar2, p_stmt varchar2) is
  n number;
  v_sql varchar2(200) := 'select count(*) from crate where code = ''' || p_code || ''''; -- Noncompliant {{Parameter "p_code" is concatenated into the SQL text as a value: bind it with USING.}}
begin
  execute immediate 'select count(*) from crate where code = ''' || p_code || '''' into n; -- Noncompliant {{Parameter "p_code" is concatenated into the SQL text as a value: bind it with USING.}}
  execute immediate 'select count(*) from ' || p_table into n; -- Noncompliant {{Parameter "p_table" is concatenated into the SQL text: bind a value with USING, or check a name with DBMS_ASSERT.}}
  execute immediate v_sql into n;
  execute immediate p_stmt; -- Noncompliant
  execute immediate 'select count(*) from crate where code = :c' into n using p_code;
  execute immediate 'select count(*) from ' || dbms_assert.simple_sql_name(p_table) into n;
  execute immediate 'select count(*) from ' || dbms_assert.enquote_name(p_table) into n;
  execute immediate 'select count(*) from crate where code = ' || dbms_assert.enquote_literal(p_code) into n;
  execute immediate 'select count(*) from crate where code = ''' || dbms_assert.noop(p_code) || '''' into n; -- Noncompliant
end;
/
create procedure open_crates(p_where varchar2, p_order in out varchar2, p_out out varchar2, p_id number, p_day date) is
  c sys_refcursor;
  v_local varchar2(10) := 'x';
  v_twice varchar2(200);
  v_append varchar2(200) := 'select id from crate where ';
  v_into varchar2(200);
  h integer;
begin
  open c for 'select id from crate where ' || p_where; -- Noncompliant
  open c for 'select id from crate order by ' || p_order; -- Noncompliant
  h := dbms_sql.open_cursor;
  dbms_sql.parse(h, 'select id from crate where ' || p_where, dbms_sql.native); -- Noncompliant
  dbms_sql.parse(c => h, statement => 'select id from crate where code = ''' || p_where || '''', language_flag => dbms_sql.native); -- Noncompliant
  -- An OUT parameter, a number, a date, a local and a constant are not the caller's text.
  open c for 'select id from crate where code = ''' || p_out || '''';
  open c for 'select id from crate where id = ' || p_id;
  open c for 'select id from crate where day = ''' || p_day || '''';
  open c for 'select id from crate where code = ''' || v_local || '''';
  -- A parameter inside another expression is not decided here.
  open c for 'select id from crate where code = ''' || replace(p_where, '''', '''''') || '''';
  open c for 'select id from crate where code = ''' || upper(p_where) || '''';
  -- Variables written more than once, appended to or read into are not followed.
  v_twice := 'select id from crate where ' || p_where;
  v_twice := 'select id from crate';
  open c for v_twice;
  v_append := v_append || p_where;
  open c for v_append;
  select p_where into v_into from dual;
  open c for v_into;
end;
/
create procedure find_glued(p_code varchar2) is
  n number;
  v_prefix varchar2(100) := 'select count(*) from crate where ';
begin
  -- The text does not parse: every parameter operand is still written into it.
  execute immediate v_prefix || 'code = ''' || p_code || '''' into n; -- Noncompliant {{Parameter "p_code" is concatenated into the SQL text: bind a value with USING, or check a name with DBMS_ASSERT.}}
end;
/
create procedure find_logged(p_code varchar2) is
  n number;
  v_pos varchar2(200) := 'select count(*) from crate where code = ''' || p_code || ''''; -- Noncompliant
  v_named varchar2(200) := 'select count(*) from crate where code = ''' || p_code || ''''; -- Noncompliant
  v_inout varchar2(200) := 'select count(*) from crate where code = ''' || p_code || '''';
  v_out varchar2(200) := 'select count(*) from crate where code = ''' || p_code || '''';
  procedure note(p_level number, p_text varchar2) is begin null; end;
  procedure keep(p_text in out varchar2) is begin null; end;
  procedure fill(p_text out varchar2) is begin p_text := null; end;
begin
  -- An IN formal cannot be assigned: the variable keeps its one value.
  note(1, v_pos);
  execute immediate v_pos into n;
  note(p_text => v_named, p_level => 1);
  execute immediate v_named into n;
  keep(v_inout);
  execute immediate v_inout into n;
  fill(v_out);
  execute immediate v_out into n;
end;
/
create procedure find_old(p_code varchar2, p_day varchar2) is
  rec varchar2(100);
begin
  -- Inside a DATE literal the placeholder is still between quotes: a value.
  execute immediate 'select val from crate where code = ''' || p_code || -- Noncompliant {{Parameter "p_code" is concatenated into the SQL text as a value: bind it with USING.}}
                    ''' and created > date ''' || p_day || '''' into rec; -- Noncompliant {{Parameter "p_day" is concatenated into the SQL text as a value: bind it with USING.}}
end;
/
