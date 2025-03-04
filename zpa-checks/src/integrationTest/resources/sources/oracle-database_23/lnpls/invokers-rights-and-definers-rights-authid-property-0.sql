-- https://docs.oracle.com/en/database/oracle/oracle-database/23/lnpls/invokers-rights-and-definers-rights-authid-property.html
GRANT INHERIT PRIVILEGES ON current_user TO PUBLIC
GRANT INHERIT PRIVILEGES ON current_user TO unit_owner
GRANT INHERIT ANY PRIVILEGES TO unit_owner