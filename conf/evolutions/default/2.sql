# --- !Ups
ALTER TABLE TASK ADD NEXT_DATE DATETIME DEFAULT NULL;
ALTER TABLE TASK ADD OWNER VARCHAR(100);

# --- !Downs
ALTER TABLE TASK DROP NEXT_DATE;
ALTER TABLE TASK DROP OWNER;