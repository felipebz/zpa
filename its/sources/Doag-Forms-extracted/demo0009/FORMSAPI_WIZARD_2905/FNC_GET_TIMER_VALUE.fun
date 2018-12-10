FUNCTION fnc_get_timer_value RETURN VARCHAR2 IS
  -- 07.03.2017 - F.Matz : DateTime from Middle Tier.
BEGIN
  
  RETURN (to_char(webutil_clientinfo.get_date_time, 'DD.MM.YYYY HH24:MI:SS'));
   
END fnc_get_timer_value;