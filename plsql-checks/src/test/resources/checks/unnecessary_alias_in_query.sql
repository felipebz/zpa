begin
  select *
    from tab x; -- violation
    
  select *
    from tab x, -- violation
         tab2;
         
  select *
    from tab x -- violation
   where exists (select 1 from tab2);
   
  select (select 1 from tab x) -- violation
    from tab2;
    
  update tab x -- violation
     set foo = bar
   where exists (select 1 from tab2);
   
  delete tab x -- violation
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