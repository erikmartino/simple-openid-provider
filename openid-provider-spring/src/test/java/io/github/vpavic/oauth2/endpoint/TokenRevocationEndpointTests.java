package io.github.vpavic.oauth2.endpoint;

import java.util.Map;

import com.nimbusds.oauth2.sdk.id.Issuer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import io.github.vpavic.oauth2.client.ClientRepository;
import io.github.vpavic.oauth2.grant.refresh.RefreshTokenStore;

import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TokenRevocationEndpoint}.
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration
public class TokenRevocationEndpointTests {

	@Autowired
	private WebApplicationContext wac;

	private MockMvc mvc;

	@BeforeEach
	public void setUp() {
		this.mvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
	}

	@Test
	public void test() {
		// TODO
	}

	@Configuration
	@EnableWebMvc
	static class Config {

		@Bean
		@SuppressWarnings("unchecked")
		public TokenHandler tokenHandler() {
			return new TokenHandler(mock(Map.class), mock(RefreshTokenStore.class), new Issuer("http://example.com"),
					mock(ClientRepository.class));
		}

		@Bean
		public TokenRevocationEndpoint tokenRevocationEndpoint() {
			return new TokenRevocationEndpoint(tokenHandler());
		}

	}

}
