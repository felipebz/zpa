-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/DEPRECATE-pragma.html
CREATE PACKAGE pack5 AUTHID DEFINER AS
PRAGMA DEPRECATE(pack5 , 'Package pack5 has been deprecated, use new_pack5 instead.');
 PROCEDURE foo;
 PROCEDURE bar;
END pack5;