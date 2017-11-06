package io.github.vpavic.oauth2.userinfo;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.ServletUtils;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.OIDCScopeValue;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import io.github.vpavic.oauth2.jwk.JwkSetLoader;

public class UserInfoAuthenticationFilter extends OncePerRequestFilter {

	private static final Logger logger = LoggerFactory.getLogger(UserInfoAuthenticationFilter.class);

	private final Issuer issuer;

	private final JwkSetLoader jwkSetLoader;

	private JWSAlgorithm jwsAlgorithm = JWSAlgorithm.RS256;

	public UserInfoAuthenticationFilter(Issuer issuer, JwkSetLoader jwkSetLoader) {
		Objects.requireNonNull(issuer, "issuer must not be null");
		Objects.requireNonNull(jwkSetLoader, "jwkSetLoader must not be null");

		this.issuer = issuer;
		this.jwkSetLoader = jwkSetLoader;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		try {
			HTTPRequest httpRequest = ServletUtils.createHTTPRequest(request);
			UserInfoRequest userInfoRequest = UserInfoRequest.parse(httpRequest);

			ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
			JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(this.jwsAlgorithm,
					(jwkSelector, context) -> jwkSelector.select(this.jwkSetLoader.load()));
			jwtProcessor.setJWSKeySelector(keySelector);
			JWTClaimsSet claimsSet = jwtProcessor.process(userInfoRequest.getAccessToken().getValue(), null);

			if (!this.issuer.getValue().equals(claimsSet.getIssuer())) {
				throw new Exception("Invalid issuer");
			}

			if (!claimsSet.getAudience().contains(this.issuer.getValue())) {
				throw new Exception("Invalid audience");
			}

			if (Instant.now().isAfter(claimsSet.getExpirationTime().toInstant())) {
				throw new Exception("Access token has expired");
			}

			List<String> scopes = claimsSet.getStringListClaim(UserInfoEndpoint.CLAIM_SCOPE);

			if (scopes.isEmpty() || !scopes.contains(OIDCScopeValue.OPENID.getValue())) {
				throw new Exception("Invalid scope");
			}

			String username = claimsSet.getSubject();
			PreAuthenticatedAuthenticationToken authentication = new PreAuthenticatedAuthenticationToken(username, "",
					Collections.emptyList());
			authentication.setDetails(claimsSet);
			SecurityContextHolder.getContext().setAuthentication(authentication);
		}
		catch (Exception e) {
			logger.debug("Bearer authentication attempt failed: {}", e.getMessage());
		}

		filterChain.doFilter(request, response);
	}

	public void setJwsAlgorithm(JWSAlgorithm jwsAlgorithm) {
		this.jwsAlgorithm = jwsAlgorithm;
	}

}