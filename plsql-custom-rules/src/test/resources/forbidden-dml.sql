select * 
  from user u inner join profile p on (u.id = p.user_id); -- Noncompliant {{Replace this query by a function of the USER_WRAPPER package.}}
--     ^^^^