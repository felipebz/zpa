-- https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/domain_check_type.html
SELECT DOMAIN_CHECK_TYPE (three_chars, 'ab') two_chars,
       DOMAIN_CHECK_TYPE (three_chars, 'abc') three_chars,
       DOMAIN_CHECK_TYPE (three_chars, 'abcd') four_chars;