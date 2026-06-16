-- Omogućava kreiranje dobavljača bez unetog rejtinga
USE event_management_db;

ALTER TABLE dobavljac MODIFY COLUMN rejting DECIMAL(3,2) NULL;
