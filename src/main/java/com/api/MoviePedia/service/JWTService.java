package com.api.MoviePedia.service;

import com.api.MoviePedia.repository.model.RefreshTokenEntity;
import com.api.MoviePedia.repository.model.UserEntity;

import java.util.Map;

public interface JWTService {
    String generateAccessToken(Long userId, String role);

    String createRefreshToken(UserEntity userEntity);

    Map<String, Object> validateToken(String token);
    Long extractUserIdFromToken(String token);
    String extractRoleFromToken(String token);

    RefreshTokenEntity findRefreshTokenByToken(String refreshToken);

    RefreshTokenEntity verifyRefreshTokenExpiration(RefreshTokenEntity refreshTokenEntity);

    Boolean checkIfRefreshTokenExistsByUserId(Long userId);
}
