-- avatar_url is what clients render; avatar_key is the storage object behind it. Keeping the key
-- lets a replacement upload delete the previous object instead of orphaning it in the bucket.
ALTER TABLE users
    ADD COLUMN avatar_key VARCHAR(255);
