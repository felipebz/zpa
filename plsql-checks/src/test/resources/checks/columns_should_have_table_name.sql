begin
  select col, -- violation
         tab.col,
         func(col)
    from tab, tab2;
  
  -- do not create issue when the select has one table
  select col,
         tab.col,
         func(col)
    from tab;
end;