create package pkg is
  v number;
end;
/
begin
  select 1 into pkg.v from dual; -- Noncompliant {{scalar destination}}
end;
/
