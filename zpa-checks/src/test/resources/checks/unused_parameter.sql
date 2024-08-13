create procedure foo(a number, b number) is -- Noncompliant {{Remove this unused "B" parameter.}}
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
     cursor cur(x number) is -- Noncompliant {{Remove this unused "X" parameter.}}
--              ^^^^^^^^
       select 1 from dual;
   begin
     print(a);
   end;
end;
/
create type foo as object ( -- don't report violation on declarations
  constructor function foo(x number) return self as result,
  member procedure foo(a number, b number);
)
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

  member procedure print(self in out nocopy t) is -- don't report violation on SELF parameter
  begin
    null;
  end;
end;
/
procedure foo is
  cursor cur(x number) is
  select 1 from dual where cur.x = 1; -- don't report violation because "cur.x" refers to the cursor parameter
begin
  null;
end;
/
