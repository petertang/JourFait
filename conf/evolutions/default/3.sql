# --- !Ups
ALTER TABLE TASK ADD START_DATE DATETIME;
UPDATE TASK SET START_DATE = CURDATE();
ALTER TABLE TASK MODIFY START_DATE DATETIME NOT NULL;

# --- !Downs
ALTER TABLE TASK DROP START_DATE;
