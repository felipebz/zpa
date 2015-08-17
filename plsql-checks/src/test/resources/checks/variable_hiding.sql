declare
  var number;
  var2 number;
  var3 number;
  
  procedure test is
    var number; -- violation
  begin
    null;
  end;
  
  function test return number is
    var2 number; -- violation
  begin
    null;
  end;
  
begin
  declare
    var3 number; -- violation
  begin
    null;
  end;
end;
/

create package body test is
  var number;
  
  procedure test is
    var number; -- violation
  begin
    null;
  end;
end;
/

-- correct
declare
  outer_var number;  
begin
  declare
    var number;
  begin
    null;
  end;
  
  declare
    var number;
  begin
    null;
  end;
end;
/