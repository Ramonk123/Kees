# Manual development documents

This directory contains Markdown documents for exercising Kees locally. The
application does not load these files automatically. Upload them through the
normal HTTP endpoint so the same validation, object storage, parsing, chunking,
and persistence flow is used as for every other source.

## Upload with Postman

1. Create or select a knowledge collection and copy its public UUID.
2. Send a `POST` request to
   `/api/collections/{collectionPublicId}/sources`.
3. Select `Body` -> `form-data`.
4. Add a field named `file`, change its type to `File`, and select one of the
   documents in `seed/markdown`.
5. Send the request and inspect the returned source status.

## Upload all documents

With the backend running, pass an existing collection's public UUID to the
script:

```shell
./seed/upload-all.sh 8a92a775-c7a6-4e80-8428-edf1f982a66b
```

The script uploads every `.md` file in `seed/markdown` sequentially and stops
when an upload fails. The default backend URL is `http://localhost:8080`. It can
be overridden when necessary:

```shell
KEES_BASE_URL=http://localhost:8081 ./seed/upload-all.sh <collection-public-id>
```

The collection can alternatively be supplied through `KEES_COLLECTION_ID`.

The larger documents are intentional: with the current chunk-size setting they
should produce multiple chunks and therefore make useful parser test material.

These files are original learning notes, not copies of the Spring reference
documentation. Links to the relevant official documentation appear at the end
of each document.
