package com.api.MoviePedia.service.impl;

import com.api.MoviePedia.exception.TokenRefreshException;
import com.api.MoviePedia.model.RefreshTokenRequest;
import com.api.MoviePedia.repository.RefreshTokenRepository;
import com.api.MoviePedia.repository.UserRepository;
import com.api.MoviePedia.repository.model.RefreshTokenEntity;
import com.api.MoviePedia.repository.model.UserEntity;
import com.api.MoviePedia.service.JWTService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class JWTServiceImpl implements JWTService {
    @Value("${jwt.access-token.expiration-time-ms}")
    private Integer accessTokenExpirationTime;

    @Value("${jwt.refresh-token.expiration-time-ms}")
    private Integer refreshTokenExpirationTime;

    @Value("${jwt.secret.key}")
    private String secretKey;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Override
    public String generateAccessToken(Long userId, String role) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .setIssuer("MoviePedia")
                .setSubject(userId.toString())
                .claim("role", role)
                .setExpiration(new Date((new Date().getTime() + accessTokenExpirationTime)))
                .signWith(key)
                .compact();
    }

    @Override
    public String createRefreshToken(UserEntity userEntity){
        String token = UUID.randomUUID().toString();
        Instant expirationDate = Instant.now();
        expirationDate = expirationDate.plusMillis(refreshTokenExpirationTime);
        refreshTokenRepository.save(new RefreshTokenEntity(null, token, expirationDate, userEntity));
        return token;
    }

    @Override
    public void deleteRefreshTokenByUserId(Long userId){
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new NoSuchElementException("User with id: " + userId + " does not exist"));
        RefreshTokenEntity refreshTokenEntity = refreshTokenRepository.findByUserId(userId).orElseThrow(() -> new NoSuchElementException("User with id: " + userId + " is already logged out"));
        refreshTokenRepository.deleteById(refreshTokenEntity.getId());
    }

    @Override
    public void deleteRefreshTokenByToken(RefreshTokenRequest refreshTokenRequest) {
        try{
            RefreshTokenEntity refreshTokenEntity = findRefreshTokenByToken(refreshTokenRequest.getRefreshToken());
            refreshTokenRepository.deleteById(refreshTokenEntity.getId());
        } catch (Exception ex){

        }
    }

    @Override
    public Map<String, Object> validateToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put("id", extractUserIdFromToken(token));
        claimsMap.put("role", extractRoleFromToken(token));
        return claimsMap;
    }

    @Override
    public Long extractUserIdFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return Long.parseLong((String) claims.get("sub"));
    }

    @Override
    public String extractRoleFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return (String) claims.get("role");
    }

    @Override
    public RefreshTokenEntity findRefreshTokenByToken(String refreshToken){
        return refreshTokenRepository.findByToken(refreshToken).orElseThrow(() -> new NoSuchElementException("Refresh token: " + refreshToken + " does not exist"));
    }

    @Override
    public void verifyRefreshTokenExpiration(RefreshTokenEntity refreshTokenEntity){
        if (refreshTokenEntity.getExpirationDate().isBefore(Instant.now())){
            refreshTokenRepository.deleteById(refreshTokenEntity.getId());
            throw new TokenRefreshException("Refresh token has expired, please log in again");
        }
    }

    @Override
    public Boolean checkIfRefreshTokenExistsByUserId(Long userId){
        return refreshTokenRepository.findByUserId(userId).isPresent();
    }

    @Override
    public RefreshTokenEntity findRefreshTokenByUserId(Long id) {
        return refreshTokenRepository.findByUserId(id).orElseThrow(() -> new NoSuchElementException("Refresh token with user id: " + id + " does not exist"));
    }
}
