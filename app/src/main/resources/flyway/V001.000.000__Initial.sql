create table TENANT (
  discriminator varchar,
  rId varchar primary key not null,
  journalId varchar not null,
  eId varchar not null,
  recordedAt bigint NOT NULL,
  effectiveAt bigint NOT NULL,
  name varchar not null,
  firstline varchar not null,
  secondline varchar,
  city varchar not null,
  region varchar not null,
  postalcode varchar not null,
  country varchar not null,
  -- price NUMERIC(21, 2) NOT NULL,
  previous varchar
);
