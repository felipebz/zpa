create package pkg is
  procedure test is begin null; end;
  function foo return number is begin null; end;
end;
/
create procedure test is begin null; end;
/
create function foo return number is begin null; end;
/