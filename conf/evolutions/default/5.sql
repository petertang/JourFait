# --- !Ups
ALTER TABLE ACCOUNT_PRIVATE ADD SALT VARCHAR(80) NOT NULL DEFAULT 'AC18382ADFF';

# --- !Downs
ALTER TABLE TASK DROP SALT;