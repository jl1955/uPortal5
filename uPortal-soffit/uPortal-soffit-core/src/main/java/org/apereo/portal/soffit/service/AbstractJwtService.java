/**
 * Licensed to Apereo under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership. Apereo
 * licenses this file to you under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the License at the
 * following location:
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apereo.portal.soffit.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;
import java.util.UUID;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.apereo.portal.soffit.ITokenizable;
import org.jasypt.util.text.BasicTextEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

/**
 * Base class for services that produce JASON Web Tokens.
 *
 * @since 5.0
 */
public class AbstractJwtService {

    public static final String JWT_ISSUER = "Soffit";

    public static final String SIGNATURE_KEY_PROPERTY = "org.apereo.portal.soffit.jwt.signatureKey";
    public static final String DEFAULT_SIGNATURE_KEY = "CHANGEME";

    public static final String ENCRYPTION_PASSWORD_PROPERTY =
            "org.apereo.portal.soffit.jwt.encryptionPassword";
    public static final String DEFAULT_ENCRYPTION_PASSWORD = "CHANGEME";

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${" + SIGNATURE_KEY_PROPERTY + ":" + DEFAULT_SIGNATURE_KEY + "}")
    private String signatureKey;

    @Value("${" + ENCRYPTION_PASSWORD_PROPERTY + ":" + DEFAULT_ENCRYPTION_PASSWORD + "}")
    private String encryptionPassword;

    /*
     * NOTE:  There is also a StrongTextEncryptor, but it requires each deployment
     * to download and install the "Java Cryptography Extension (JCE) Unlimited
     * Strength Jurisdiction Policy Files," which sounds like a tremendous PITA.
     * The BasicTextEncryptor supports "normal-strength encryption of texts,"
     * which should be satisfactory for our needs.
     */
    final BasicTextEncryptor textEncryptor = new BasicTextEncryptor();

    @PostConstruct
    public void init() {

        // Signature Key
        if (StringUtils.isBlank(signatureKey)) {
            logger.error("The value of required property {} is blank", SIGNATURE_KEY_PROPERTY);
            throw new IllegalStateException("Missing property " + SIGNATURE_KEY_PROPERTY);
        } else if (DEFAULT_SIGNATURE_KEY.equals(signatureKey)) {
            logger.warn(
                    "Property {} is using the deafult value;  please change it",
                    SIGNATURE_KEY_PROPERTY);
        }

        // Encryption Passowrd
        if (StringUtils.isBlank(encryptionPassword)) {
            logger.error(
                    "The value of required property {} is blank", ENCRYPTION_PASSWORD_PROPERTY);
            throw new IllegalStateException("Missing property " + ENCRYPTION_PASSWORD_PROPERTY);
        } else if (DEFAULT_ENCRYPTION_PASSWORD.equals(encryptionPassword)) {
            logger.warn(
                    "Property {} is using the deafult value;  please change it",
                    ENCRYPTION_PASSWORD_PROPERTY);
        }
        textEncryptor.setPassword(encryptionPassword);
    }

    protected Claims createClaims(
            Class<? extends ITokenizable> clazz, String username, Date expires) {

        // Registered claims
        final Claims rslt =
                Jwts.claims()
                        .setIssuer(JWT_ISSUER)
                        .setSubject(username)
                        .setExpiration(expires)
                        .setIssuedAt(new Date())
                        .setId(UUID.randomUUID().toString());

        // Deserialization class
        rslt.put(JwtClaims.CLASS.getName(), clazz.getName());

        return rslt;
    }

    protected String generateEncryptedToken(Claims claims) {

        final String jwt =
                Jwts.builder()
                        .setClaims(claims)
                        .signWith(SignatureAlgorithm.HS512, signatureKey)
                        .compact();

        // Encryption
        final String rslt = textEncryptor.encrypt(jwt);

        return rslt;
    }

    protected Jws<Claims> parseEncrypteToken(
            String encryptedToken, Class<? extends ITokenizable> clazz) {

        // Decryption
        final String jwt = textEncryptor.decrypt(encryptedToken);

        final Jws<Claims> rslt = Jwts.parser().setSigningKey(signatureKey).parseClaimsJws(jwt);

        // Token expired?
        final Date expires = rslt.getBody().getExpiration();
        if (expires.before(new Date())) {
            final String msg = "The specified token is expired:  " + rslt;
            throw new SecurityException(msg);
        }

        // Sanity check
        final String s = (String) rslt.getBody().get(JwtClaims.CLASS.getName());
        if (!clazz.getName().equals(s)) {
            // Opportunity for future versioning of the data model... needs work
            String msg =
                    "Token class mismatch;  expected '" + clazz.getName() + "' but was '" + s + "'";
            throw new RuntimeException(msg);
        }

        return rslt;
    }
}
