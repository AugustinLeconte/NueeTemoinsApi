CREATE TABLE friendships (
    id            BIGSERIAL PRIMARY KEY,
    requester_id  VARCHAR(255) NOT NULL,
    addressee_id  VARCHAR(255) NOT NULL,
    status        VARCHAR(10)  NOT NULL DEFAULT 'pending',  -- 'pending' | 'accepted'
    created_at    TIMESTAMP    NOT NULL DEFAULT now(),
    CHECK (requester_id <> addressee_id)
);

-- Une seule relation par paire, quel que soit le sens (A→B ou B→A)
CREATE UNIQUE INDEX ux_friendships_pair
    ON friendships (LEAST(requester_id, addressee_id), GREATEST(requester_id, addressee_id));

CREATE INDEX ix_friendships_addressee ON friendships (addressee_id);