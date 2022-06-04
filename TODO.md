# TODOs and ideas

- reduce amount of integration tests

- test outgoing http adapters with Wiremock
  - MastodonBotHttpClient
  - MatrixMonitor
  - WebDavSyncTask

- introduce Lombok or Java records where useful

- package structure:
  - app vs. application vs. core
  - core/mode vs. domain

- full (incoming) model validation

- introduce Repository classes to hide JDBI DAOs?

- reintroduce multistage docker build

- harden docker
  - user
  - readonly filesystem

- WebDavSyncTask
  - error handling (check status codes)
  - use directory listing instead of HEAD request for every file

```bash
curl --user 'rsapi:...' -i -X PROPFIND https://cloud.railway-stations.org/remote.php/webdav/VsionAI/verpixelt/ --upload-file - -H "Depth: 1" <<end
<?xml version="1.0"?>
<a:propfind xmlns:a="DAV:">
<a:prop><a:resourcetype/></a:prop>
</a:propfind>
end
HTTP/1.1 100 Continue

HTTP/1.1 207 Multi-Status
Date: Sat, 04 Jun 2022 15:49:12 GMT
Server: Apache/2.4.38 (Debian)
Strict-Transport-Security: max-age=63072000; includeSubDomains
Referrer-Policy: no-referrer
Expires: Thu, 19 Nov 1981 08:52:00 GMT
Cache-Control: no-store, no-cache, must-revalidate
Pragma: no-cache
Content-Security-Policy: default-src 'none';
Vary: Brief,Prefer
DAV: 1, 3, extended-mkcol
Content-Length: 559
Content-Type: application/xml; charset=utf-8

<?xml version="1.0"?>
<d:multistatus xmlns:d="DAV:" xmlns:s="http://sabredav.org/ns" xmlns:oc="http://owncloud.org/ns" xmlns:nc="http://nextcloud.org/ns"><d:response><d:href>/remote.php/webdav/VsionAI/verpixelt/</d:href><d:propstat><d:prop><d:resourcetype><d:collection/></d:resourcetype></d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat></d:response><d:response><d:href>/remote.php/webdav/VsionAI/verpixelt/Readme.md</d:href><d:propstat><d:prop><d:resourcetype/></d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat></d:response></d:multistatus>
```

