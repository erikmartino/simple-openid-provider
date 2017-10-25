package io.github.vpavic.oauth2.token;

import java.util.Objects;

import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.openid.connect.sdk.rp.OIDCClientInformation;

public final class AccessTokenRequest {

	private final String principal;

	private final OIDCClientInformation client;

	private final Scope scope;

	private final AccessTokenClaimsMapper accessTokenClaimsMapper;

	public AccessTokenRequest(String principal, OIDCClientInformation client, Scope scope,
			AccessTokenClaimsMapper accessTokenClaimsMapper) {
		Objects.requireNonNull(principal, "principal must not be null");
		Objects.requireNonNull(client, "client must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
		Objects.requireNonNull(accessTokenClaimsMapper, "accessTokenClaimsMapper must not be null");

		this.principal = principal;
		this.client = client;
		this.scope = scope;
		this.accessTokenClaimsMapper = accessTokenClaimsMapper;
	}

	public String getPrincipal() {
		return this.principal;
	}

	public OIDCClientInformation getClient() {
		return this.client;
	}

	public Scope getScope() {
		return this.scope;
	}

	public AccessTokenClaimsMapper getAccessTokenClaimsMapper() {
		return this.accessTokenClaimsMapper;
	}

}
