create or replace procedure case_a is
  x date := sysdate;
begin
  declare
    w x%type;
    x number := 1;
  begin
    say('w uses outer x?');
  end;
end;
/
create or replace procedure case_b is
  x date := sysdate;
begin
  declare
    x number := 1;
    w x%type := 5;
  begin
    say('w='||w);
  end;
end;
/
