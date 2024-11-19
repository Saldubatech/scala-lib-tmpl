CREATE TABLE IF NOT EXISTS items(
  recordid varchar primary key not null,
  entityid varchar not null,
  recordedat bigint NOT NULL,
  effectiveat bigint not null,
  name VARCHAR NOT NULL,
  price NUMERIC(21, 2) NOT NULL
);

create table if not exists items_evo(
  rId varchar primary key not null,
  name VARCHAR NOT NULL,
  price NUMERIC(21, 2) NOT NULL
);

create table if not exists sample_journal(
  discriminator varchar,
  rId varchar primary key not null,
  journalId varchar not null,
  eId varchar not null,
  recordedAt bigint NOT NULL,
  effectiveAt bigint NOT NULL,
  name varchar not null,
  price NUMERIC(21, 2) NOT NULL,
  previous varchar
);
