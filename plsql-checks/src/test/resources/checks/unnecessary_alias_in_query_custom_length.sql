begin
  select *
    from tab x; -- violation
    
  select *
    from tab xxxx, -- should be ignored because the accepted length is 4
         tab2;
end;
/