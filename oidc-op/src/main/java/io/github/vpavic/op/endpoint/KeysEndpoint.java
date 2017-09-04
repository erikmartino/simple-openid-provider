package io.github.vpavic.op.endpoint;

import java.util.List;
import java.util.Objects;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.vpavic.op.key.KeyService;

/**
 * Endpoint that publishes server's public RSA keys as a JSON Web Key (JWK) set.
 *
 * @author Vedran Pavic
 */
@RestController
@RequestMapping(path = KeysEndpoint.PATH_MAPPING)
public class KeysEndpoint {

	public static final String PATH_MAPPING = "/oauth2/keys";

	private static final MediaType JWK_SET = MediaType.parseMediaType(JWKSet.MIME_TYPE);

	private final KeyService keyService;

	public KeysEndpoint(KeyService keyService) {
		Objects.requireNonNull(keyService, "keyService must not be null");

		this.keyService = keyService;
	}

	@GetMapping
	public ResponseEntity<String> getJwkSet() {
		List<JWK> keys = this.keyService.findAll();
		JWKSet jwkSet = new JWKSet(keys);

		// @formatter:off
		return ResponseEntity.ok()
				.contentType(JWK_SET)
				.body(jwkSet.toJSONObject().toJSONString());
		// @formatter:on
	}

}
