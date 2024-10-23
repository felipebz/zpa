-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/create-domain.html
CREATE SEQUENCE IF NOT EXISTS email_seq;
CREATE DOMAIN email AS VARCHAR2(30)  
    DEFAULT ON NULL email_seq.NEXTVAL || '@domain.com'
    CONSTRAINT EMAIL_C CHECK (REGEXP_LIKE (email, '^(\S+)\@(\S+)\.(\S+)$'))
    DISPLAY '---' || SUBSTR(email, INSTR(email, '@') + 1);