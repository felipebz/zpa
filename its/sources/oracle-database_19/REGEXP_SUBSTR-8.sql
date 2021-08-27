-- https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/REGEXP_SUBSTR.html
with strings as (    
  select 'LHRJFK/010315/JOHNDOE' str from dual union all    
  select 'CDGLAX/050515/JANEDOE' str from dual union all    
  select 'LAXCDG/220515/JOHNDOE' str from dual union all    
  select 'SFOJFK/010615/JANEDOE' str from dual    
)    
  SELECT regexp_substr(str, '[A-Z]{6}') String_of_6_characters,   
         regexp_substr(str, '[0-9]+') First_Matching_Numbers,   
         regexp_substr(str, '[A-Z].*$') Letter_by_other_characters,     
         regexp_substr(str, '/[A-Z].*$') Slash_letter_and_characters     
  FROM strings;