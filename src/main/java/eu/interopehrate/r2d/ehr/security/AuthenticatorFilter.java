package eu.interopehrate.r2d.ehr.security;

import java.io.IOException;
import java.util.Base64;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class AuthenticatorFilter implements Filter {
	private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticatorFilter.class);
	
	
	private static final String R2D_SERVICE_CREDENTIALS_INIT_PARAM = "R2D_SERVICE_CREDENTIALS";
	
	private String r2dServiceCredentials;
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		Optional<String> o = Optional.fromNullable(filterConfig.getInitParameter(R2D_SERVICE_CREDENTIALS_INIT_PARAM));
		if (!o.isPresent())
			throw new ServletException("Missing " + R2D_SERVICE_CREDENTIALS_INIT_PARAM + " config parameter, cannot start!");
		r2dServiceCredentials = o.get();
	}

	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
				
		HttpServletRequest hReq = (HttpServletRequest)request;
		HttpServletResponse hRes = (HttpServletResponse)response;
		LOGGER.info("Received request: " + hReq.getRequestURL());

		// Get the Authorization header parameter
		Optional<String> o = Optional.fromNullable(hReq.getHeader(SecurityConstants.AUTH_HEADER));
		if (!o.isPresent()) {
			LOGGER.error("The request lacks the Authorization header parameter, thus cannot be processed.");
			hRes.sendError(HttpStatus.SC_UNAUTHORIZED, "The request lacks the Authorization header parameter, thus cannot be processed.");
			return;			
		}
	
		String authHeaderParam = o.get();		
		if (authHeaderParam.startsWith(SecurityConstants.BASIC_PREFIX)) {
			// If basic authorization, only the EHR_SERVICE is authorized
			LOGGER.debug("Verifying Basic Auth...");
			String base64Credentials = authHeaderParam.substring(SecurityConstants.BASIC_PREFIX.length()).trim();
			final byte[] decodedBytes  = Base64.getDecoder().decode(base64Credentials.getBytes());
		    final String credentials = new String(decodedBytes);

		    if (!credentials.equals(r2dServiceCredentials)) {
				LOGGER.error("The provided credentials are not valid! The request cannot be processed.");
				hRes.sendError(HttpStatus.SC_UNAUTHORIZED, "The provided credentials are not valid! The request cannot be processed.");			
				return;
		    } 
			LOGGER.info("Request authenticated to R2D Server.");
		} else {
			LOGGER.error("Authorization header is not used properly! The request cannot be processed.");
			hRes.sendError(HttpStatus.SC_UNAUTHORIZED, "Authorization header is not used properly! The request cannot be processed.");
			return;
		}
		
	    chain.doFilter(request, response);
	}
	
	@Override
	public void destroy() {}

}
