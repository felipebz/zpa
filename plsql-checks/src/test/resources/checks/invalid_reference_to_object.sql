begin
  find_alert('foo');
  find_alert('invalid'); -- Noncompliant {{Invalid reference to the object "invalid" in this FIND_ALERT call.}}
--           ^^^^^^^^^

  set_alert_button_property('foo', alert_button1, label, 'label');
  set_alert_button_property('invalid', alert_button1, label, 'label'); -- Noncompliant {{Invalid reference to the object "invalid" in this SET_ALERT_BUTTON_PROPERTY call.}}
--                          ^^^^^^^^^

  set_alert_property('foo', title, 'title');
  set_alert_property('invalid', title, 'title'); -- Noncompliant {{Invalid reference to the object "invalid" in this SET_ALERT_PROPERTY call.}}
--                   ^^^^^^^^^

  show_alert('foo');
  show_alert('invalid'); -- Noncompliant {{Invalid reference to the object "invalid" in this SHOW_ALERT call.}}
--           ^^^^^^^^^

  show_lov('foo');
  show_lov('invalid'); -- Noncompliant {{Invalid reference to the object "invalid" in this SHOW_LOV call.}}
--         ^^^^^^^^^
end;