# --- !Ups
ALTER TABLE TASK ADD STEPS_LEFT TINYINT NOT NULL DEFAULT '1';

# --- !Downs
ALTER TABLE TASK DROP STEPS_LEFT;