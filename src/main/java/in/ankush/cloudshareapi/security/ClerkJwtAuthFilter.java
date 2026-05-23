package in.ankush.cloudshareapi.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class ClerkJwtAuthFilter extends OncePerRequestFilter {

    @Value("${clerk.issuer}")
    private String clerkIssuer;

    private final ClerkJwksProvider jwksProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // ==============================
        // IMPORTANT: Allow OPTIONS request
        // ==============================
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            filterChain.doFilter(request, response);
            return;
        }

        // ==============================
        // PUBLIC ROUTES
        // ==============================
        if (
                requestURI.startsWith("/webhooks") ||
                requestURI.startsWith("/files/public") ||
                requestURI.startsWith("/files/download") ||
                requestURI.startsWith("/files/view") ||
                requestURI.startsWith("/auth")
        ) {
            filterChain.doFilter(request, response);
            return;
        }

        // ==============================
        // AUTH HEADER CHECK
        // ==============================
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {

            response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "Authorization header missing or invalid"
            );
            return;
        }

        try {

            String token = authHeader.substring(7);

            // ==============================
            // JWT HEADER PARSE
            // ==============================
            String[] chunks = token.split("\\.");

            if (chunks.length != 3) {
                response.sendError(
                        HttpServletResponse.SC_FORBIDDEN,
                        "Invalid JWT format"
                );
                return;
            }

            String headerJson = new String(
                    Base64.getUrlDecoder().decode(chunks[0])
            );

            ObjectMapper mapper = new ObjectMapper();

            JsonNode headerNode = mapper.readTree(headerJson);

            if (!headerNode.has("kid")) {

                response.sendError(
                        HttpServletResponse.SC_FORBIDDEN,
                        "JWT missing kid"
                );
                return;
            }

            String kid = headerNode.get("kid").asText();

            // ==============================
            // GET PUBLIC KEY
            // ==============================
            PublicKey publicKey = jwksProvider.getPublicKey(kid);

            // ==============================
            // VERIFY JWT
            // ==============================
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .requireIssuer(clerkIssuer)
                    .setAllowedClockSkewSeconds(60)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // ==============================
            // GET USER ID
            // ==============================
            String clerkId = claims.getSubject();

            // ==============================
            // CREATE AUTH TOKEN
            // ==============================
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            clerkId,
                            null,
                            Collections.singletonList(
                                    new SimpleGrantedAuthority("ROLE_USER")
                            )
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource()
                            .buildDetails(request)
            );

            // ==============================
            // SET SECURITY CONTEXT
            // ==============================
            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

            // ==============================
            // CONTINUE REQUEST
            // ==============================
            filterChain.doFilter(request, response);

        } catch (Exception e) {

            e.printStackTrace();

            response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "Invalid JWT: " + e.getMessage()
            );
        }
    }
}
