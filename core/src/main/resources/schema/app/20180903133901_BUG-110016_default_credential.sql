-- // BUG-110016 User should be able to store default credentials in each organization
-- Migration SQL that makes the change goes here.

CREATE TABLE defaultcredential (
    id bigint NOT NULL,
    userprofile_id bigint NOT NULL,
    credential_id bigint NOT NULL
);

CREATE SEQUENCE default_credential_id_seq START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

INSERT INTO defaultcredential(
    id,
    userprofile_id,
    credential_id
)
SELECT nextval('default_credential_id_seq') AS id, userprofile.id AS userprofile_id, userprofile.credential_id AS credential_id FROM userprofile WHERE credential_id IS NOT NULL;

-- //@UNDO
-- SQL to undo the change goes here.

DROP SEQUENCE IF EXISTS default_credential_id_seq;

DROP TABLE IF EXISTS default_credential;

