-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/CREATE-USER.html
CREATE USER sidney 
    IDENTIFIED BY out_standing1 
    DEFAULT TABLESPACE example 
    QUOTA 10M ON example 
    TEMPORARY TABLESPACE temp
    QUOTA 5M ON system 
    PROFILE app_user 
    PASSWORD EXPIRE;