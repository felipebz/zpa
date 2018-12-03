create procedure foo(a number, b number) is -- Noncompliant {{Remove this unused "b" parameter.}}
--                             ^^^^^^^^
begin
  print(a);
end;
/

create function bar(a number, b number) return number is  -- Noncompliant
--                            ^^^^^^^^
begin
  return a;
end;
/

create package test is
  procedure foo(a number, b number); -- don't report violation on declaration
  cursor bar(a number) return my_type; -- don't report violation on declaration
  
   procedure foo(a number, b number) is -- Noncompliant
--                         ^^^^^^^^
     cursor cur(x number) is -- Noncompliant {{Remove this unused "x" parameter.}}
--              ^^^^^^^^
       select 1 from dual;
   begin
     print(a);
   end;
end;
/
create type t under super_t (
  overriding member procedure foo(a number, b number); -- don't report violation on declaration
)
/
create type body t as
  not overriding member procedure foo(a number) as -- Noncompliant
--                                    ^^^^^^^^
  begin
    null;
  end;

  overriding member procedure foo(a number, b number) as -- don't report violation on overriding member
  begin
    null;
  end;
end;
/
