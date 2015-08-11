create function foo(x in number,
                    y out number) -- violation
       return number is
begin
  null;
end;
/
create function foo(x in number,
                    y in out number) -- violation
       return number is
begin
  null;
end;
/

create package pck is
  function foo(x in number,
               y out number) -- violation
         return number is
  begin
    null;
  end;
  function foo(x in number,
               y in out number) -- violation
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