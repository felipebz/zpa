begin
  select col, -- violation
         tab.col,
         func(col),
         'text'
    from tab, tab2;
  
  -- do not create issue when the select has one table
  select col,
         tab.col,
         func(col),
         'text'
    from tab;
    
  -- do not report error in rownum
  select rownum
    from tab, tab2;
end;