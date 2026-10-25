-- Conditional compilation anywhere in the file may hide a declaration that the file's top-level blocks and
-- statements see: nothing is proven there.
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

select col
  from tab, tab2;
