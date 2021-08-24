SELECT name FROM t
  WHERE JSON_EXISTS(name, '$[0].first');