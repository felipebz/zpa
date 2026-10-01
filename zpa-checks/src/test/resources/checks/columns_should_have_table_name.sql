declare
  my_var number;
begin
  select col, -- Noncompliant {{Specify the table of column "col".}}
--       ^^^
         tab.col,
         func(col),
         'text'
    from tab, tab2;
  
  -- do not create issue when the select has one table
  select col,
         tab.col,
         func(col),
         'text'
    from tab;
    
  -- do not report error in rownum
  select rownum
    from tab, tab2;
  
  -- do not report error when the column is a known variable
  insert into tab (foo, bar, baz)
    (select other.foo,
            my_var, -- compliant
            other.baz 
       from other, other2);

  select my_var; -- do not report error in queries without tables
end;

-- DML in scripts should be checked too
select col, -- Noncompliant {{Specify the table of column "col".}}
--     ^^^
       tab.col,
       func(col),
       'text'
  from tab, tab2
/

select id, -- Noncompliant
       value, -- Noncompliant
       rowid, -- Noncompliant
       "SYSDATE" -- Noncompliant
  from tab, tab2;

select rownum, sysdate, systimestamp, current_date, current_timestamp,
       localtimestamp, dbtimezone, sessiontimezone, user, uid
  from tab, tab2;

select level, connect_by_isleaf, connect_by_iscycle
  from tab, tab2 connect by nocycle level < 3;
