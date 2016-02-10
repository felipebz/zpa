begin
  select *
    from tab x; -- Noncompliant {{This statement has only one reference to the table "tab". Remove the alias "x" to improve the readability.}} [[sc=14;ec=15]]
    
  select *
    from tab x, -- Noncompliant [[sc=14;ec=15]]
         tab2;
         
  select *
    from tab x -- Noncompliant [[sc=14;ec=15]]
   where exists (select 1 from tab2);
   
  select (select 1 from tab x) -- Noncompliant [[sc=29;ec=30]]
    from tab2;
    
  update tab x -- Noncompliant [[sc=14;ec=15]]
     set foo = bar
   where exists (select 1 from tab2);
   
  delete tab x -- Noncompliant [[sc=14;ec=15]]
   where exists (select 1 from tab2);
    
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