create or replace procedure case_a is
  e exception;
  e number;
begin
  e := 1;
end;
/
create or replace procedure case_b is
  x number := 1;
begin
  <<x>>
  begin
    null;
  end;
  say('label+var same region, unreferenced');
end;
/
create or replace procedure case_c is
  x number := 1;
begin
  <<x>>
  begin
    null;
  end;
  say('x='||x);
end;
/
