declare
  cursor cur is -- Noncompliant {{Remove this unused "CUR" cursor.}}
    select 1 from dual;
    
  cursor "cur2" is
    select 1 from dual;
begin
  open "cur2";
end;

create package pkg is
  cursor cur is -- compliant, this is a package specification, we don't know if there are any usages
    select 1 from dual;
end;

create package body pkg is
  cursor cur return custom_type is -- compliant, let's assume that cursors with return type are declared in package spec
    select 1 from dual;
end;
/

-- Used only through %ROWTYPE.
declare
  cursor c_rowtype is select dummy from dual;
  r c_rowtype%rowtype;
begin
  r.dummy := 'x';
end;
/

-- Used only as the source of a cursor FOR loop.
declare
  cursor c_loop is select dummy from dual;
begin
  for r in c_loop loop
    null;
  end loop;
end;
/

-- Used only through an attribute.
declare
  cursor c_attribute is select dummy from dual;
begin
  if c_attribute%isopen then
    null;
  end if;
end;
/

-- A forward declaration with RETURN, and its definition later in the block.
declare
  cursor c_forward return dual%rowtype;
  cursor c_forward return dual%rowtype is select * from dual;
begin
  open c_forward;
end;
/

-- In SQL the name is the column, not the cursor, but only the table's columns could prove it: not reported.
declare
  cursor dummy is select 1 from dual;
  v varchar2(1);
begin
  select dummy into v from dual;
end;
/

-- Never referenced.
declare
  cursor c_unused is select 1 from dual; -- Noncompliant {{Remove this unused "C_UNUSED" cursor.}}
begin
  null;
end;
/
