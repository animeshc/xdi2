package xdi2.messaging.target.interceptor.impl.authentication.secrettoken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xdi2.core.Literal;
import xdi2.core.constants.XDIAuthenticationConstants;
import xdi2.messaging.Message;
import xdi2.messaging.MessageResult;
import xdi2.messaging.exceptions.Xdi2AuthenticationException;
import xdi2.messaging.exceptions.Xdi2MessagingException;
import xdi2.messaging.target.ExecutionContext;
import xdi2.messaging.target.MessagingTarget;
import xdi2.messaging.target.Prototype;
import xdi2.messaging.target.interceptor.AbstractInterceptor;
import xdi2.messaging.target.interceptor.MessageInterceptor;
import xdi2.messaging.target.interceptor.MessagingTargetInterceptor;

/**
 * This interceptor looks for a secret token on an incoming XDI message,
 * and invokes an instance of SecretTokenAuthenticator to authenticate the message.
 */
public class AuthenticationSecretTokenInterceptor extends AbstractInterceptor implements MessagingTargetInterceptor, MessageInterceptor, Prototype<AuthenticationSecretTokenInterceptor> {

	private static Logger log = LoggerFactory.getLogger(AuthenticationSecretTokenInterceptor.class.getName());

	private SecretTokenAuthenticator secretTokenAuthenticator;

	/*
	 * Prototype
	 */

	@Override
	public AuthenticationSecretTokenInterceptor instanceFor(PrototypingContext prototypingContext) throws Xdi2MessagingException {

		// create new interceptor

		AuthenticationSecretTokenInterceptor interceptor = new AuthenticationSecretTokenInterceptor();

		// set the authenticator

		interceptor.setSecretTokenAuthenticator(this.getSecretTokenAuthenticator().instanceFor(prototypingContext));

		// done

		return interceptor;
	}

	/*
	 * MessagingTargetInterceptor
	 */

	@Override
	public void init(MessagingTarget messagingTarget) throws Exception {

		this.getSecretTokenAuthenticator().init();
	}

	@Override
	public void shutdown(MessagingTarget messagingTarget) throws Exception {

		this.getSecretTokenAuthenticator().shutdown();
	}

	/*
	 * MessageInterceptor
	 */

	@Override
	public boolean before(Message message, MessageResult messageResult, ExecutionContext executionContext) throws Xdi2MessagingException {

		// look for secret token on the message

		String secretToken = message.getSecretToken();
		if (secretToken == null) return false;

		// authenticate

		if (log.isDebugEnabled()) log.debug("Authenticating via " + this.getSecretTokenAuthenticator().getClass().getSimpleName());

		boolean authenticated = this.getSecretTokenAuthenticator().authenticate(message, secretToken);
		if (! authenticated) throw new Xdi2AuthenticationException("Invalid secret token.", null, executionContext);

		Literal secretTokenValidLiteral = message.getContextNode().setDeepLiteralBoolean(XDIAuthenticationConstants.XRI_S_SECRET_TOKEN_VALID_VALUE, Boolean.valueOf(authenticated));

		if (log.isDebugEnabled()) log.debug(secretTokenValidLiteral.getStatement().toString());

		// done

		return false;
	}

	@Override
	public boolean after(Message message, MessageResult messageResult, ExecutionContext executionContext) throws Xdi2MessagingException {

		return false;
	}

	/*
	 * Getters and setters
	 */

	public SecretTokenAuthenticator getSecretTokenAuthenticator() {

		return this.secretTokenAuthenticator;
	}

	public void setSecretTokenAuthenticator(SecretTokenAuthenticator secretTokenAuthenticator) {

		this.secretTokenAuthenticator = secretTokenAuthenticator;
	}
}
