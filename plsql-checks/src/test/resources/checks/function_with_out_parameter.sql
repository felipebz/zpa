create function foo(x in number,
                    y out number) -- Noncompliant {{Rewrite this function to not depend on OUT parameters.}}
       return number is
begin
  null;
end;
/
create function foo(x in number,
                    y in out number) -- Noncompliant
       return number is
begin
  null;
end;
/

create package pck is
  function foo(x in number,
               y out number) -- Noncompliant
         return number is
  begin
    null;
  end;
  function foo(x in number,
               y in out number) -- Noncompliant
         return number is
  begin
    null;
  end;
end;
/

-- correct
create function foo return number is
begin
  null;
end;
/