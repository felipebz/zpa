begin
  select *
    from tab x; -- Noncompliant {{This statement has only one reference to the table "tab". Remove the alias "x" to improve the readability.}}
--           ^
    
  select * from tab x, tab2; -- Noncompliant
--                  ^
  
  select * from tab x where exists (select 1 from tab2); -- Noncompliant
--                  ^
   
  select (select 1 from tab x) from tab2; -- Noncompliant
--                          ^
    
  update tab x set foo = bar where exists (select 1 from tab2); -- Noncompliant
--           ^
   
  delete tab x where exists (select 1 from tab2); -- Noncompliant
--           ^
    
  -- correct
  select *
    from tab x,
         tab;
  
  select *
    from tab x,
         tab y;
  
  select *
    from tab,
         tab2;
         
  select *
    from tab
   where exists (select 1 from tab x);
   
  update tab
     set foo = bar
   where exists (select 1 from tab x);
   
  delete tab
   where exists (select 1 from tab x); 
end;
/