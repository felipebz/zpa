with strings as (   
  select 'ABC123' str from dual union all   
  select 'A1B2C3' str from dual union all   
  select '123ABC' str from dual union all   
  select '1A2B3C' str from dual   
)   
  select regexp_substr(str, '[0-9]') First_Occurrence_of_Number,    
         regexp_substr(str, '[0-9].*') Num_Followed_by_String,   
         regexp_substr(str, '[A-Z][0-9]') Letter_Followed_by_String   
  from strings;