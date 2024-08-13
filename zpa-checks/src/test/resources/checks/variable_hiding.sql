declare
  var number;
  var2 number;
  var3 number;
  exc exception;
  "QUOTEDVAR" number;
  
  procedure test is
    var number; -- Noncompliant {{This variable "VAR" hides the declaration on line 2.}} [[secondary=2]]
--  ^^^
  begin
    null;
  end;
  
  function test return number is
    var2 number; -- Noncompliant {{This variable "VAR2" hides the declaration on line 3.}} [[secondary=3]]
--  ^^^^
  begin
    null;
  end;

  procedure test2 is
    exc exception; -- Noncompliant {{This variable "EXC" hides the declaration on line 5.}} [[secondary=5]]
--  ^^^
  begin
    null;
  end;

begin
  declare
    var3 number; -- Noncompliant {{This variable "VAR3" hides the declaration on line 4.}} [[secondary=4]]
--  ^^^^
    quotedvar number; -- Noncompliant {{This variable "QUOTEDVAR" hides the declaration on line 6.}} [[secondary=6]]
    "QuotedVar" number; -- this is a different variable
  begin
    null;
  end;
end;
/

create package body test is
  var number;
  
  procedure test is
    var number; -- Noncompliant {{This variable "VAR" hides the declaration on line 42.}} [[secondary=42]]
--  ^^^
  begin
    for i in 1..10 loop
      declare
        i number; -- Noncompliant {{This variable "I" hides the declaration on line 48.}} [[secondary=48]]
--      ^
      begin
        null;
      end;
    end loop;
  end;
end;
/

create package test2 is
  var number;
  exc exception;
end;
/
create package body test2 is
  var number; -- Noncompliant {{This variable "VAR" hides the declaration on line 61.}} [[secondary=61]]
  exc exception; -- Noncompliant {{This variable "EXC" hides the declaration on line 62.}} [[secondary=62]]
end;
/

-- correct
declare
  outer_var number;
  "VAR2" number; --|
  "Var2" number; --| These variables are not the same
  "var2" number; --|
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
