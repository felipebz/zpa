create procedure on_event(a number, b number) is 
--                             
begin
  print(a);
end;
/
create procedure fullmatch(a number, b number) is 
--                             
begin
  print(a);
end;
/
create function on_event(p_event in integer)
    return integer
as
begin
   return 1;
end;
/
create function bar(a number, b number) return number is  -- Noncompliant
--                            ^^^^^^^^
begin
  return a;
end;
/

