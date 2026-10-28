-- Conditional compilation may hide a declaration in the unit it is in, and only there: each CREATE, block and
-- statement of the file is compiled on its own.
create procedure conditional_col is
  $if $$extra $then
  col number;
  $end
  v number;
begin
  -- conditional compilation may declare "col": nothing is proven
  select col into v
    from tab, tab2;
end;
/

select col -- Noncompliant {{Specify the table of column "col".}}
  from tab, tab2;
