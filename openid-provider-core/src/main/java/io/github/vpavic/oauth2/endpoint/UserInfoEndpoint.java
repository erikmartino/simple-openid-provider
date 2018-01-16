package io.github.vpavic.oauth2.endpoint;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.nimbusds.oauth2.sdk.GeneralException;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.ServletUtils;
import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.openid.connect.sdk.UserInfoErrorResponse;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;
import com.nimbusds.openid.connect.sdk.UserInfoSuccessResponse;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import io.github.vpavic.oauth2.authentication.AccessTokenClaimsResolver;
import io.github.vpavic.oauth2.claim.ClaimHelper;
import io.github.vpavic.oauth2.claim.ClaimSource;

/**
 * OpenID Connect 1.0 compatible UserInfo Endpoint implementation.
 *
 * @author Vedran Pavic
 * @see <a href="https://openid.net/specs/openid-connect-core-1_0.html">OpenID Connect Core 1.0</a>
 */
@RequestMapping(path = UserInfoEndpoint.PATH_MAPPING)
public class UserInfoEndpoint {

	public static final String PATH_MAPPING = "/oauth2/userinfo";

	private final AccessTokenClaimsResolver accessTokenClaimsResolver;

	private final ClaimSource claimSource;

	private String accessTokenScopeClaim = "scp";

	private Map<Scope.Value, List<String>> scopeClaims = new HashMap<>();

	public UserInfoEndpoint(AccessTokenClaimsResolver accessTokenClaimsResolver, ClaimSource claimSource) {
		Objects.requireNonNull(accessTokenClaimsResolver, "accessTokenClaimsResolver must not be null");
		Objects.requireNonNull(claimSource, "claimSource must not be null");
		this.accessTokenClaimsResolver = accessTokenClaimsResolver;
		this.claimSource = claimSource;
	}

	public void setAccessTokenScopeClaim(String accessTokenScopeClaim) {
		this.accessTokenScopeClaim = accessTokenScopeClaim;
	}

	public void setScopeClaims(Map<Scope.Value, List<String>> scopeClaims) {
		this.scopeClaims = scopeClaims;
	}

	@RequestMapping(method = { RequestMethod.GET, RequestMethod.POST })
	@SuppressWarnings("unchecked")
	public void getUserInfo(HttpServletRequest request, HttpServletResponse response) throws IOException {
		HTTPRequest httpRequest = ServletUtils.createHTTPRequest(request);
		UserInfoResponse userInfoResponse;

		try {
			UserInfoRequest userInfoRequest = UserInfoRequest.parse(httpRequest);
			AccessToken accessToken = userInfoRequest.getAccessToken();

			Map<String, Object> accessTokenClaims = this.accessTokenClaimsResolver.resolveClaims(accessToken);
			Subject subject = new Subject((String) accessTokenClaims.get("sub"));
			Scope scope = Scope.parse((List<String>) accessTokenClaims.get(this.accessTokenScopeClaim));
			Set<String> claims = ClaimHelper.resolveClaims(scope, this.scopeClaims);
			UserInfo userInfo = this.claimSource.load(subject, claims);

			userInfoResponse = new UserInfoSuccessResponse(userInfo);
		}
		catch (GeneralException e) {
			userInfoResponse = new UserInfoErrorResponse(e.getErrorObject());
		}

		ServletUtils.applyHTTPResponse(userInfoResponse.toHTTPResponse(), response);
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Methods", "GET, POST");
	}

}
