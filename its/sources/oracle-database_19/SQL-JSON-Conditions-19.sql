SELECT name FROM t
  WHERE JSON_EXISTS(name, '$[*].last');