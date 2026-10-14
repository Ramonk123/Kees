CREATE UNIQUE INDEX uk_sources_collection_sha256_active
    ON sources (collection_id, sha256)
    WHERE status <> 'FAILED';
