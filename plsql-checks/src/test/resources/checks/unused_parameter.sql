create procedure foo(a number, b number) is -- Noncompliant {{Remove this unused "b" parameter.}} [[sc=32;ec=40]]
begin
  print(a);
end;
/

create function bar(a number, b number) return number is  -- Noncompliant [[sc=31;ec=39]]
begin
  return a;
end;
/

create package test is
  procedure foo(a number, b number); -- don't report violation on headings
  
   procedure foo(a number, b number) is -- Noncompliant [[sc=28;ec=36]]
     cursor cur(x number) is -- Noncompliant {{Remove this unused "x" parameter.}} [[sc=17;ec=25]]
       select 1 from dual;
   begin
     print(a);
   end;
end;
/
