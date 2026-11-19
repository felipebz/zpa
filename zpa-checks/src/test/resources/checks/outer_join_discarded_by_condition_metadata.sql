create procedure run_unqualified is
  n number;
begin
  -- Only the metadata tells that COLOR is a column of CRATE.
  select count(*) into n from shelf s left join crate c on c.shelf_id = s.id where color = 'RED'; -- Noncompliant
end;
/
