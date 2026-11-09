package com.nueetemoins.backend.config;

import java.net.URI;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.Ed25519Verifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

@Component
public class NeonJwtVerifier {
    private static final long JWKS_TTL_MS = 10 * 60_000;

    private final String jwksUrl;
    private final String issuer;
    private volatile JWKSet jwkSet;
    private volatile long loadedAt;

    public NeonJwtVerifier(@Value("${neon.auth.url}") String authUrl) {
        URI uri = URI.create(authUrl);
        this.issuer = uri.getScheme() + "://" + uri.getAuthority();
        this.jwksUrl = authUrl + "/.well-known/jwks.json";
    }

    private JWKSet loadJwks(boolean force) throws Exception {
        long now = System.currentTimeMillis();
        if (force || jwkSet == null || now - loadedAt > JWKS_TTL_MS) {
            jwkSet = JWKSet.load(URI.create(jwksUrl).toURL());
            loadedAt = now;
        }
        return jwkSet;
    }

    public String verify(String token) throws Exception {
        SignedJWT jwt = SignedJWT.parse(token);

        if (!JWSAlgorithm.EdDSA.equals(jwt.getHeader().getAlgorithm())) {
            throw new SecurityException("Algorithme inattendu");
        }

        String kid = jwt.getHeader().getKeyID();
        JWK key = loadJwks(false).getKeyByKeyId(kid);
        if (key == null) {
            key = loadJwks(true).getKeyByKeyId(kid); 
        }
        if (!(key instanceof OctetKeyPair okp)) {
            throw new SecurityException("Clé introuvable dans le JWKS pour kid=" + kid);
        }

        if (!jwt.verify(new Ed25519Verifier(okp.toPublicJWK()))) {
            throw new SecurityException("Signature invalide");
        }

        JWTClaimsSet claims = jwt.getJWTClaimsSet();
        Date exp = claims.getExpirationTime();
        if (exp == null || exp.before(new Date())) {
            throw new SecurityException("Token expiré");
        }
        if (!issuer.equals(claims.getIssuer())) {
            throw new SecurityException("Issuer invalide : " + claims.getIssuer());
        }
        return claims.getSubject();
    }
}