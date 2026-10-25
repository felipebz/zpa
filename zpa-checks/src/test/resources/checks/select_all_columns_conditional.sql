-- Conditional compilation anywhere in a file leaves its file-level lookups (type names included) undecided, so it has
-- its own fixture.
create or replace procedure conditional_destination is
  r emp%rowtype;
  $if $$plain $then
  r number;
  $end
begin
  -- conditional compilation may declare another "r": nothing is proven
  select * into r from emp;
end;
/
