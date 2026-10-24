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
create function mle_inline(a number, b number) return number as mle language javascript {{ return a; }}; -- don't report violation, the MLE body is opaque
/
create function mle_module(a number, b number) return number as mle module m signature 'f'; -- don't report violation, the MLE body is opaque
/
create procedure mle_module_env(a number) as mle module m env e signature 'p'; -- don't report violation, the MLE body is opaque
/

-- The formal name of a named-notation call is not a use of the caller's parameter.
create procedure named_call(x number) is -- Noncompliant
--                          ^^^^^^^^
begin
  other_proc(x => 1);
end;
/

-- Used only by a nested subprogram.
create procedure used_inside(x number) is
  procedure nested is
  begin
    print(x);
  end;
begin
  nested;
end;
/

-- An OUT parameter that is only assigned: the assignment is a use.
create procedure only_assigned(x out number) is
begin
  x := 1;
end;
/

-- Used through the subprogram's name.
create procedure qualified_use(x number) is
begin
  print(qualified_use.x);
end;
/

-- In the query, a column of the same name may take the name: not provably unused without the table's columns.
create procedure cursor_column is
  cursor c(x number) is
    select count(*) from some_table where x = 1;
begin
  open c(1);
end;
/

-- An external routine receives the parameters of a call specification.
create procedure external_call(x number) as language java name 'A.run(int)';
/

-- SELF in a member method.
create type body t2 as
  member function value_of(self in t2) return number is
  begin
    return 1;
  end;
end;
/

-- Oracle substitutes a SQL macro's parameters by name into the text it returns.
create function date_string(dat date) return varchar2 sql_macro(scalar) is
begin
  return q'{ to_char(dat, 'YYYY-MM-DD') }';
end;
/

create function rows_of(t dbms_tf.table_t, n number) return varchar2 sql_macro(table) is
begin
  return 'select * from t fetch first n rows only';
end;
/

create package body macros as
  function budget(job varchar2) return varchar2 sql_macro is
  begin
    return 'select * from employees where job_id = job';
  end;
end;
/
