create or replace procedure case_a is
  "x" number := 1;
  x   number := 2;
begin
  say('"x"='||"x"||' x='||x||' "X"='||"X");
end;
/
create or replace procedure case_b is
  X   number := 1;
  "X" number := 2;
begin
  say('x='||x);
end;
/
create or replace procedure case_c is
  "my var" number := 3;
  "My Var" number := 4;
begin
  say('"my var"='||"my var"||' "My Var"='||"My Var");
end;
/
create or replace procedure case_d is
begin
  <<"lbl">>
  declare
    x number := 1;
  begin
    say('lbl.x='||lbl.x);
  end;
end;
/
create or replace procedure case_e is
begin
  <<"LBL">>
  declare
    x number := 1;
  begin
    say('lbl.x='||lbl.x);
  end;
end;
/
