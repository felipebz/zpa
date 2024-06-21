-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/plsql-name-resolution.html
CREATE OR REPLACE PACKAGE pkg1 AUTHID DEFINER AS
  m NUMBER;
  TYPE t1 IS RECORD (a NUMBER);
  v1 t1;
  TYPE t2 IS TABLE OF t1 INDEX BY PLS_INTEGER;
  v2 t2; 
  FUNCTION f1 (p1 NUMBER) RETURN t1;
  FUNCTION f2 (q1 NUMBER) RETURN t2;
END pkg1;
/
CREATE OR REPLACE PACKAGE BODY pkg1 AS
  FUNCTION f1 (p1 NUMBER) RETURN t1 IS
    n NUMBER;
  BEGIN
     n := m;             -- Unqualified variable name
     n := pkg1.m;        -- Variable name qualified by package name
     n := pkg1.f1.p1;    -- Parameter name qualified by function name,
                         --  which is qualified by package name
     n := v1.a;          -- Variable name followed by component name
     n := pkg1.v1.a;     -- Variable name qualified by package name
                         --  and followed by component name
     n := v2(10).a;      -- Indexed name followed by component name
     n := f1(10).a;      -- Function invocation followed by component name
     n := f2(10)(10).a;  -- Function invocation followed by indexed name
                         --  and followed by component name
     n := hr.pkg1.f2(10)(10).a;  -- Schema name, package name,
                                 -- function invocation, index, component name
     v1.a := p1;
     RETURN v1;
   END f1;

   FUNCTION f2 (q1 NUMBER) RETURN t2 IS
     v_t1 t1;
     v_t2 t2;
   BEGIN
     v_t1.a := q1;
     v_t2(1) := v_t1;
     RETURN v_t2;
   END f2;
END pkg1;
/