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
end;

-- DML in scripts should be checked too
select col, -- Noncompliant {{Specify the table of column "col".}}
--     ^^^
       tab.col,
       func(col),
       'text'
  from tab, tab2
/