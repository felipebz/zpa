-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/DEPRECATE-pragma.html
CREATE PACKAGE pack7 AUTHID DEFINER AS
  PROCEDURE foo;
  PRAGMA DEPRECATE (foo, 'pack7.foo is deprecated, use pack7.bar instead.');
  PROCEDURE bar;
END pack7;