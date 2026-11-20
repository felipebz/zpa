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

declare
  col number;
begin
  -- a local of the same name: a column "col" of tab or tab2 would win over it, but the query may read the local
  select col into col
    from tab, tab2;
end;
/

-- Every join form reads more than one source.
select a.id, label -- Noncompliant
  from bin_crate a
  join bin_kind b on b.id = a.kind_id;

select a.id, label -- Noncompliant
  from bin_crate a
 inner join bin_kind b on b.id = a.kind_id;

select a.id, label -- Noncompliant
  from bin_crate a
 cross join bin_kind b;

select a.id, label -- Noncompliant
  from bin_crate a
  left outer join bin_kind b on b.id = a.kind_id;

select a.id, label -- Noncompliant
  from bin_crate a
  full join bin_kind b on b.id = a.kind_id;

select a.id, label -- Noncompliant
  from bin_crate a
 cross apply (select k.name from bin_kind k where k.id = a.kind_id) b;

select a.id, label -- Noncompliant
  from bin_crate a
 outer apply (select k.name from bin_kind k where k.id = a.kind_id) b;

select a.id, label -- Noncompliant
  from (bin_crate a join bin_kind b on b.id = a.kind_id);

-- Oracle rejects a qualifier on a column of the USING list (ORA-25154).
select id, label -- Noncompliant
  from bin_crate
  join bin_kind using (id);

-- Oracle rejects a qualifier on a column a natural join shares (ORA-25155), and which they are is not known.
select id, label
  from bin_crate
natural join bin_kind;

-- One source, and a subquery with its own FROM.
select label
  from bin_crate a;

select label
  from (select c.label from bin_crate c join bin_kind k on k.id = c.kind_id);

select label
  from (bin_crate);
