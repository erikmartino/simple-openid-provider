package io.github.vpavic.op.security.web.authentication.logout;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.nimbusds.openid.connect.sdk.rp.OIDCClientInformation;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import io.github.vpavic.op.client.ClientRepository;
import io.github.vpavic.op.config.OpenIdProviderProperties;

public class OpenIdLogoutSuccessHandler implements LogoutSuccessHandler {

	private static final String REDIRECT_URI_PARAMETER = "post_logout_redirect_uri";

	private static final String STATE_PARAMETER = "state";

	// @formatter:off
	private static final String LOGOUT_PAGE_HTML_TEMPLATE = "<!DOCTYPE html>"
			+ "<html>"
			+ "<head>"
			+ "<title>end_session_endpoint</title>"
			+ "<script>"
			+ "window.onload = function() {"
			+ "window.location.href = ':redirectURI';"
			+ "}"
			+ "</script>"
			+ "</head>"
			+ "<body>"
			+ ":iframes"
			+ "</body>"
			+ "</html>";
	// @formatter:on

	private static final String LOGOUT_PAGE_IFRAME_TEMPLATE = "<iframe style=\"display:block; visibility:hidden\" "
			+ "src=\"${clientLogoutUrl}\"></iframe>";

	private final OpenIdProviderProperties properties;

	private final ClientRepository clientRepository;

	public OpenIdLogoutSuccessHandler(OpenIdProviderProperties properties, ClientRepository clientRepository) {
		this.properties = properties;
		this.clientRepository = Objects.requireNonNull(clientRepository);
	}

	@Override
	public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
			throws IOException, ServletException {
		String logoutPageHtml = generateLogoutPageHtml(request, authentication);
		response.setContentType("text/html;charset=UTF-8");
		response.getWriter().write(logoutPageHtml);
	}

	private String generateLogoutPageHtml(HttpServletRequest request, Authentication authentication) {
		String redirectURI = request.getParameter(REDIRECT_URI_PARAMETER);

		List<OIDCClientInformation> clients = this.clientRepository.findAll();

		if (this.properties.isSessionManagementOrFrontChannelLogoutEnabled() && StringUtils.hasText(redirectURI)) {
			// @formatter:off
			Set<String> redirectURIs = clients.stream()
					.flatMap(client -> Optional.ofNullable(client.getOIDCMetadata().getPostLogoutRedirectionURIs())
							.orElse(Collections.emptySet()).stream())
					.filter(Objects::nonNull)
					.map(URI::toString)
					.collect(Collectors.toSet());
			// @formatter:on

			if (redirectURIs.contains(redirectURI)) {
				String state = request.getParameter(STATE_PARAMETER);

				if (state != null) {
					// @formatter:off
					redirectURI = UriComponentsBuilder.fromHttpUrl(redirectURI)
							.queryParam("state", state)
							.toUriString();
					// @formatter:on
				}
			}
			else {
				redirectURI = defaultRedirectURI(request);
			}
		}
		else {
			redirectURI = defaultRedirectURI(request);
		}

		StringBuilder iframes = new StringBuilder();

		if (this.properties.isFrontChannelLogoutEnabled()) {
			// @formatter:off
			Set<String> logoutURIs = clients.stream()
					.map(client -> client.getOIDCMetadata().getFrontChannelLogoutURI())
					.filter(Objects::nonNull)
					.map(URI::toString)
					.collect(Collectors.toSet());
			// @formatter:on

			String sessionId = request.getSession().getId();

			for (String clientLogoutURI : logoutURIs) {
				// @formatter:off
				clientLogoutURI = UriComponentsBuilder.fromHttpUrl(clientLogoutURI)
						.queryParam("iss", this.properties.getIssuer())
						.queryParam("sid", sessionId)
						.toUriString();
				// @formatter:on

				iframes.append(LOGOUT_PAGE_IFRAME_TEMPLATE.replace(":clientLogoutUrl", clientLogoutURI));
			}
		}

		return LOGOUT_PAGE_HTML_TEMPLATE.replace(":iframes", iframes.toString()).replace(":redirectURI", redirectURI);
	}

	private static String defaultRedirectURI(HttpServletRequest request) {
		// @formatter:off
		return UriComponentsBuilder.fromHttpUrl(request.getRequestURL().toString())
				.replacePath(request.getContextPath())
				.path("login")
				.query("logout")
				.toUriString();
		// @formatter:on
	}

}
