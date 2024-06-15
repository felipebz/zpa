-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/domain_check.html
SELECT DOMAIN_CHECK (three_chars, 'ab') two_chars,
       DOMAIN_CHECK (three_chars, 'abc') three_chars,
       DOMAIN_CHECK (three_chars, 'abcd') four_chars;