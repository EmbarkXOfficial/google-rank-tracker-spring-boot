-- Sample history for the demo profile: 7 days per keyword, the last one today. NULL = not in the top 100.

-- Climbs into the top 10 today -> "entered the top 10" alert
INSERT INTO ranking (keyword, checked_on, position)
SELECT 'spring boot tutorial', DATEADD('DAY', n - 7, CURRENT_DATE), p
FROM UNNEST(ARRAY[16, 15, 14, 13, 12, 11, 8]) WITH ORDINALITY AS t(p, n);

-- Falls 6 places today -> "moved down 6 places" alert
INSERT INTO ranking (keyword, checked_on, position)
SELECT 'java microservices course', DATEADD('DAY', n - 7, CURRENT_DATE), p
FROM UNNEST(ARRAY[9, 9, 8, 9, 10, 9, 15]) WITH ORDINALITY AS t(p, n);

-- Drops out of the top 100 today -> "dropped out of the top 100" alert
INSERT INTO ranking (keyword, checked_on, position)
SELECT 'spring ai tutorial', DATEADD('DAY', n - 7, CURRENT_DATE), p
FROM UNNEST(ARRAY[45, 42, 40, 41, 39, 38, CAST(NULL AS INT)]) WITH ORDINALITY AS t(p, n);

-- Small, steady moves -> arrows but no alert
INSERT INTO ranking (keyword, checked_on, position)
SELECT 'learn java online', DATEADD('DAY', n - 7, CURRENT_DATE), p
FROM UNNEST(ARRAY[26, 25, 25, 24, 23, 22, 20]) WITH ORDINALITY AS t(p, n);
