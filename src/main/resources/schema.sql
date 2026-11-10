-- One row per keyword per day. The primary key makes sure a keyword is never checked (or paid for) twice on the same day.
CREATE TABLE IF NOT EXISTS ranking (
    keyword    VARCHAR(255) NOT NULL,
    checked_on DATE         NOT NULL,
    position   INT, -- 1-100, or NULL when the site isn't in the top 100
    PRIMARY KEY (keyword, checked_on)
    );