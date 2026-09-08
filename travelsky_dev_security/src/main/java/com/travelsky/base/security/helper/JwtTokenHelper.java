package com.travelsky.base.security.helper;

import com.travelsky.base.utils.CompressUtil;
import com.travelsky.base.utils.json.JsonUtils;
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

    private String secretKey;

    private Long expirationTime;

    public JwtTokenHelper(String secretKey, Long expirationTime) {
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
                .setSubject(authentication.getName())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime * 1000))
                .signWith(key(secretKey))
                .addClaims(map)
                .compact();
    }

    private Key key(String secretKey) {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    public Authentication extractAuthentication(String token) {
        Claims claims = extractAllClaims(token);
        List<String> roles = JsonUtils.jsonToEntity(ArrayList.class, String.class, decompress(claims.get("roles", String.class)));
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
            throw new IllegalStateException("Failed to decompress jwt roles claim", e);
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
