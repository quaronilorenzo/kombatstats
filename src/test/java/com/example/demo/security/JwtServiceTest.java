package com.example.demo.security;

import com.example.demo.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    // 32 bytes -> HS256. Test-only value.
    private static final String SECRET = Encoders.BASE64.encode("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));
    private static final String OTHER_SECRET = Encoders.BASE64.encode("fedcba9876543210fedcba9876543210".getBytes(StandardCharsets.UTF_8));
    private static final int EXPIRATION_MINUTES = 15;

    private final JwtService jwtService = new JwtService(EXPIRATION_MINUTES, SECRET);
    private final SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));

    private static User userWithId(long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private Jws<Claims> parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
    }

    private static Date inMinutes(long minutes) {
        return Date.from(Instant.now().plusSeconds(minutes * 60));
    }

    private static String base64Url(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    @Nested
    @DisplayName("generateToken")
    class GenerateToken {

        @Test
        @DisplayName("round trip: the id put in is the id read back")
        void shouldReturnSameUserId_whenTokenIsReadBack() {
            String token = jwtService.generateToken(userWithId(42L));

            assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
        }

        @Test
        @DisplayName("payload carries only sub, iat and exp: no personal data")
        void shouldContainOnlySubIatExp() {
            User user = userWithId(7L);
            user.setEmail("someone@example.com");
            user.setFirstName("Some");

            Claims claims = parse(jwtService.generateToken(user)).getPayload();

            assertThat(claims.keySet()).containsExactlyInAnyOrder("sub", "iat", "exp");
            assertThat(claims.getSubject()).isEqualTo("7");
        }

        @Test
        @DisplayName("lifetime equals the configured minutes")
        void shouldExpireAfterConfiguredMinutes() {
            Claims claims = parse(jwtService.generateToken(userWithId(1L))).getPayload();

            long lifetimeSeconds = (claims.getExpiration().getTime() - claims.getIssuedAt().getTime()) / 1000;
            // iat and exp come from two Instant.now() calls truncated to seconds: they may straddle a second.
            assertThat(lifetimeSeconds).isBetween(EXPIRATION_MINUTES * 60L, EXPIRATION_MINUTES * 60L + 1);
        }

        @Test
        @DisplayName("a 32-byte secret signs with HS256")
        void shouldSignWithHs256_whenSecretIs32Bytes() {
            String token = jwtService.generateToken(userWithId(1L));

            assertThat(parse(token).getHeader().getAlgorithm()).isEqualTo("HS256");
        }
    }

    @Nested
    @DisplayName("extractUserId - rejected tokens")
    class RejectedTokens {

        @Test
        @DisplayName("payload changed, original signature kept")
        void shouldReject_whenPayloadIsTampered() {
            String[] parts = jwtService.generateToken(userWithId(1L)).split("\\.");
            String forgedPayload = base64Url("{\"sub\":\"2\",\"exp\":" + (Instant.now().getEpochSecond() + 600) + "}");
            String tampered = parts[0] + "." + forgedPayload + "." + parts[2];

            assertThatThrownBy(() -> jwtService.extractUserId(tampered)).isInstanceOf(JwtException.class);
        }

        @Test
        @DisplayName("signature changed")
        void shouldReject_whenSignatureIsTampered() {
            String token = jwtService.generateToken(userWithId(1L));
            int signatureStart = token.lastIndexOf('.') + 1;
            // The first signature char carries 6 full bits; the last one may carry padding bits only.
            char replacement = token.charAt(signatureStart) == 'A' ? 'B' : 'A';
            String tampered = token.substring(0, signatureStart) + replacement + token.substring(signatureStart + 1);

            assertThatThrownBy(() -> jwtService.extractUserId(tampered)).isInstanceOf(JwtException.class);
        }

        @Test
        @DisplayName("signed with another key")
        void shouldReject_whenSignedWithAnotherKey() {
            String foreign = new JwtService(EXPIRATION_MINUTES, OTHER_SECRET).generateToken(userWithId(1L));

            assertThatThrownBy(() -> jwtService.extractUserId(foreign)).isInstanceOf(JwtException.class);
        }

        @Test
        @DisplayName("expired")
        void shouldReject_whenExpired() {
            String expired = Jwts.builder().subject("1")
                    .issuedAt(inMinutes(-30))
                    .expiration(inMinutes(-1))
                    .signWith(key)
                    .compact();

            assertThatThrownBy(() -> jwtService.extractUserId(expired)).isInstanceOf(ExpiredJwtException.class);
        }

        @Test
        @DisplayName("unsigned token (alg=none)")
        void shouldReject_whenTokenIsUnsigned() {
            String unsigned = Jwts.builder().subject("1").expiration(inMinutes(10)).compact();

            assertThatThrownBy(() -> jwtService.extractUserId(unsigned)).isInstanceOf(JwtException.class);
        }

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {"not-a-jwt", "abc.def.ghi", "a.b", "...."})
        @DisplayName("string that is not a JWT")
        void shouldReject_whenTokenIsMalformed(String token) {
            assertThatThrownBy(() -> jwtService.extractUserId(token)).isInstanceOf(JwtException.class);
        }

        @ParameterizedTest(name = "\"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("null, empty or blank: IllegalArgumentException, which the filter catches")
        void shouldThrowIllegalArgument_whenTokenIsMissing(String token) {
            assertThatThrownBy(() -> jwtService.extractUserId(token)).isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest(name = "sub=\"{0}\"")
        @ValueSource(strings = {"abc", "1.5", "99999999999999999999"})
        @DisplayName("validly signed but sub is not a long: IllegalArgumentException, which the filter catches")
        void shouldThrowIllegalArgument_whenSubjectIsNotALong(String subject) {
            String token = Jwts.builder().subject(subject).expiration(inMinutes(10)).signWith(key).compact();

            assertThatThrownBy(() -> jwtService.extractUserId(token)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("validly signed but no sub")
        void shouldThrowIllegalArgument_whenSubjectIsMissing() {
            String token = Jwts.builder().expiration(inMinutes(10)).signWith(key).compact();

            assertThatThrownBy(() -> jwtService.extractUserId(token)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @Disabled("""
                Not decided yet: today a validly signed token without exp is accepted forever, \
                because JJWT checks exp only when present. Not exploitable without the secret \
                (generateToken always sets exp), but a leaked secret would mint tokens that \
                never expire. Enable once the parser requires exp.""")
        @DisplayName("validly signed but no exp")
        void shouldReject_whenExpirationIsMissing() {
            String token = Jwts.builder().subject("1").signWith(key).compact();

            assertThatThrownBy(() -> jwtService.extractUserId(token)).isInstanceOf(JwtException.class);
        }
    }

    @Nested
    @DisplayName("constructor - secret validation")
    class SecretValidation {

        @Test
        @DisplayName("secret shorter than 32 bytes is refused at startup")
        void shouldRefuse_whenSecretIsTooShort() {
            String shortSecret = Encoders.BASE64.encode("only-16-bytes!!!".getBytes(StandardCharsets.UTF_8));

            assertThatThrownBy(() -> new JwtService(EXPIRATION_MINUTES, shortSecret)).isInstanceOf(JwtException.class);
        }

        @Test
        @DisplayName("secret that is not Base64 is refused at startup")
        void shouldRefuse_whenSecretIsNotBase64() {
            assertThatThrownBy(() -> new JwtService(EXPIRATION_MINUTES, "%%% not base64 %%%")).isInstanceOf(RuntimeException.class);
        }
    }
}
