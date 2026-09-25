-- https://docs.oracle.com/en/database/oracle/oracle-database/26/lnpls/overview-polymorphic-table-functions.html
SELECT ENAME, ECHO_ENAME
FROM implicit_echo_package.implicit_echo(SCOTT.EMP, COLUMNS(SCOTT.ENAME));