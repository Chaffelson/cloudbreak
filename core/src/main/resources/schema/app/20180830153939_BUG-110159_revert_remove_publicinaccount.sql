-- // BUG-110159
-- Migration SQL that makes the change goes here.

ALTER TABLE blueprint ADD COLUMN IF NOT EXISTS publicinaccount boolean DEFAULT false;
ALTER TABLE clustertemplate ADD COLUMN IF NOT EXISTS publicinaccount boolean DEFAULT false;
ALTER TABLE constrainttemplate ADD COLUMN IF NOT EXISTS publicinaccount boolean DEFAULT false;
ALTER TABLE credential ADD COLUMN IF NOT EXISTS publicinaccount boolean DEFAULT false;
ALTER TABLE filesystem ADD COLUMN IF NOT EXISTS publicinaccount boolean DEFAULT false;
ALTER TABLE flexsubscription ADD COLUMN IF NOT EXISTS publicinaccount boolean DEFAULT false;
ALTER TABLE imagecatalog ADD COLUMN IF NOT EXISTS publicinaccount boolean DEFAULT false;
ALTER TABLE ldapconfig ADD COLUMN IF NOT EXISTS publicinaccount boolean DEFAULT false;
ALTER TABLE network ADD COLUMN IF NOT EXISTS publicinaccount boolean DEFAULT false;
ALTER TABLE managementpack ADD COLUMN IF NOT EXISTS publicinaccount boolean DEFAULT false;
ALTER TABLE proxyconfig ADD COLUMN IF NOT EXISTS publicinaccount boolean DEFAULT false;
ALTER TABLE rdsconfig ADD COLUMN IF NOT EXISTS publicinaccount boolean DEFAULT false;
ALTER TABLE recipe ADD COLUMN IF NOT EXISTS publicinaccount boolean DEFAULT false;
ALTER TABLE smartsensesubscription ADD COLUMN IF NOT EXISTS publicinaccount boolean DEFAULT false;
ALTER TABLE stack ADD COLUMN IF NOT EXISTS publicinaccount boolean DEFAULT false;
ALTER TABLE securitygroup ADD COLUMN IF NOT EXISTS publicinaccount boolean DEFAULT false;
ALTER TABLE template ADD COLUMN IF NOT EXISTS publicinaccount boolean DEFAULT false;
ALTER TABLE topology ADD COLUMN IF NOT EXISTS publicinaccount boolean DEFAULT false;

-- //@UNDO
-- SQL to undo the change goes here.


