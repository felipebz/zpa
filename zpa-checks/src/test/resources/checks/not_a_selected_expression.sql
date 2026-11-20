select distinct emp.name
  from emp
 order by emp.id; -- Noncompliant {{This value does not exist in the SELECT clause. Fix this expression or add this value in the SELECT.}}
--        ^^^^^^

select distinct substr(emp.name, 0, 10) emp_name
  from emp
 order by emp.name; -- Noncompliant
--        ^^^^^^^^

-- Compliant queries
select distinct substr(emp.name, 0, 10) emp_name
  from emp
 order by substr(emp.name, 0, 10);

select distinct substr(emp.name, 0, 10) emp_name
  from emp
 order by emp_name;

select distinct substr(emp.name, 0, 10) emp_name
  from emp
 order by 1;

select distinct emp.name
  from emp
 order by emp.name;

 select distinct name
  from emp
 order by name;

select distinct emp.name
  from emp
 order by name;

select emp.name
  from emp
 order by emp.id;

select distinct emp.name
  from emp;

select distinct emp.name emp_name
  from emp
 order by name;

select distinct name
  from emp
 order by emp.name;

select distinct emp.name
  from emp
 order by length(emp.name);

select distinct (name)
  from emp
 order by substr(name, 2);

-- A column inside a selected function is not selected (ORA-01791).
select distinct upper(r.label) from bin_crate r order by r.label; -- Noncompliant

-- The selected expression, as Oracle compares it.
select distinct upper(r.label) from bin_crate r order by upper(r.label);
select distinct upper(r.label) from bin_crate r order by UPPER( R.LABEL );
select distinct upper(r.label) from bin_crate r order by upper(label);
select distinct r.label from bin_crate r order by label;
select distinct label from bin_crate r order by r.label;
select distinct r.label from bin_crate r order by "R"."LABEL";
select distinct r.label from bin_crate r order by (r.label);
select distinct r.id + 1 from bin_crate r order by 1 + r.id;
select distinct r.id + r.kind_id from bin_crate r order by r.kind_id + r.id;

-- An alias, a position.
select distinct r.label lbl from bin_crate r order by lbl;
select distinct r.label lbl from bin_crate r order by r.label;
select distinct r.label from bin_crate r order by 1;

-- An expression made only of selected expressions, aliases and values that are not columns.
select distinct r.label from bin_crate r order by upper(r.label);
select distinct r.id, r.label from bin_crate r order by r.id || r.label;
select distinct upper(r.label) u from bin_crate r order by lower(u);
select distinct r.id + 1 from bin_crate r order by (r.id + 1) * 2;
select distinct r.label from bin_crate r order by substr(r.label, 1, 2);
select distinct r.id from bin_crate r order by -r.id;
select distinct r.id from bin_crate r order by sysdate;
select distinct r.id from bin_crate r order by dbms_random.value;

-- A column that is not selected, alone or in an expression.
select distinct r.id + 1 from bin_crate r order by r.id; -- Noncompliant
select distinct r.id + 1 from bin_crate r order by r.id + 2; -- Noncompliant
select distinct r.id - 1 from bin_crate r order by 1 - r.id; -- Noncompliant
select distinct r.id from bin_crate r order by r.kind_id; -- Noncompliant
select distinct r.id from bin_crate r order by r.id + r.kind_id; -- Noncompliant
select distinct substr(r.label, 1, 2) from bin_crate r order by substr(r.label, 1, 3); -- Noncompliant

-- A string literal matches only with the same text, case included.
select distinct to_char(created, 'YYYY') y from bin_crate order by to_char(created, 'yyyy'); -- Noncompliant
select distinct to_char(created, 'YYYY') y from bin_crate order by to_char(created, 'YYYY');
