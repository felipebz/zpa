create procedure foo(a number, b number, "c" number) is -- Noncompliant {{Remove this unused "B" parameter.}}
--                             ^^^^^^^^
begin
  print(a);
  print("c");
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
create function process_table(tab table) -- don't report violation, the implementation package owns the parameters
return table pipelined row polymorphic using process_table_pkg;
/
create function second_max(input number) return number -- don't report violation, the implementation type owns the parameters
    parallel_enable aggregate using second_max_impl;
/
