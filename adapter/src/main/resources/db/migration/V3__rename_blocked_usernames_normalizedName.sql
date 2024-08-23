ALTER TABLE blocked_usernames
    CHANGE COLUMN `normalizedName`
                   name
                   varchar(100);
