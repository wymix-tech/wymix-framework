package org.wymix.framework.security.helper;

import org.wymix.framework.utils.CompressUtil;
import org.wymix.framework.utils.json.JsonUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.io.IOException;
import java.security.Key;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JwtTokenHelper {

    private final String secretKey;

    private final Long expirationTime;

    public JwtTokenHelper(String secretKey, Long expirationTime) {
        if (secretKey == null || secretKey.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("security.jwt.secret must contain at least 32 UTF-8 bytes (or explicitly enable security.jwt.random-secret)");
        }
        if (expirationTime == null || expirationTime <= 0 || expirationTime > Long.MAX_VALUE / 1000) {
            throw new IllegalArgumentException("security.jwt.expired-time must be a positive number of seconds");
        }
        this.secretKey = secretKey;
        this.expirationTime = expirationTime;
    }

    public Long getExpirationTime() {
        return expirationTime;
    }

    public String generateToken(Authentication authentication) {
        HashMap<String, Object> map = new HashMap<>();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        List<String> roles = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        map.put("roles", compress(JsonUtils.entityToJson(roles)));
        return Jwts.builder()
                .id(java.util.UUID.randomUUID().toString())
                .setSubject(authentication.getName())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime * 1000))
                .signWith(key(secretKey))
                .addClaims(map)
                .compact();
    }

    private Key key(String secretKey) {
        return Keys.hmacShaKeyFor(secretKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public Authentication extractAuthentication(String token) {
        Claims claims = extractAllClaims(token);
        String rolesClaim = claims.get("roles", String.class);
        if (rolesClaim == null || claims.getExpiration() == null) throw new IllegalArgumentException("JWT requires exp and roles claims");
        List<String> roles = JsonUtils.jsonToEntity(ArrayList.class, String.class, decompress(rolesClaim));
        if (roles == null || claims.getSubject() == null || claims.getSubject().isBlank()) {
            throw new IllegalArgumentException("Invalid JWT subject or roles");
        }
        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        User principal = new User(claims.getSubject(), "", authorities);
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    public boolean validateToken(String token) {
        boolean flag;
        try {
            Jwts.parser()
                    .setSigningKey(key(secretKey))
                    .build()
                    .parse(token);
            flag = true;
        } catch (Exception e) {
            flag = false;
        }
        return flag;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private String compress(String raw) {
        try {
            return CompressUtil.compress(raw);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to compress jwt roles claim", e);
        }
    }

    private String decompress(String compressed) {
        try {
            return CompressUtil.decompress(compressed);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid JWT roles claim", e);
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(key(secretKey))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
