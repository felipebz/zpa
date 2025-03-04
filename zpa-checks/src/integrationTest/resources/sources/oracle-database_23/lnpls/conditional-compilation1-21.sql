-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/conditional-compilation1.html
CREATE PACKAGE my_pkg AUTHID DEFINERs AS
SUBTYPE my_real IS
BINARY_DOUBLE;
my_pi my_real;
my_e my_real;
END my_pkg;