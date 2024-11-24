# PostgreSQL Migration

After setting up new schema in PostgreSQL, empty all tables

```sql
delete from photos ;
delete from inbox ;
delete from stations;
delete from providerapps ;
delete from countries ;
delete from users;
delete from blocked_usernames;
delete from flyway_schema_history ;
delete from oauth2_authorization_consent ;
delete from oauth2_authorization ;
delete from oauth2_registered_client ;
```

Run pgloader:

```shell
pgloader --set "timezone='Europe/Berlin'" mysql://rsapi:rsapi@localhost:3306/rsapi postgresql://rsapi:rsapi@localhost:5432/rsapi
```

Update sequences:

```sql
SELECT setval('users_seq', (select max(id) from users));
SELECT setval('photos_seq', (select max(id) from photos));
SELECT setval('inbox_seq', (select max(id) from inbox));
```

Delete versions 2 and 3 from flyway schema history:

```sql
delete from flyway_schema_history where version in (2,3);
```
