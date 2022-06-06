package eu.interopehrate.r2d.ehr;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.cors.CorsConfiguration;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.CorsInterceptor;
import eu.interopehrate.r2d.ehr.providers.EncounterResourceProvider;
import eu.interopehrate.r2d.ehr.providers.PatientResourceProvider;
import eu.interopehrate.r2d.ehr.security.R2DRequestValidator;
import eu.interopehrate.r2d.ehr.workflow.EHRRequestController;

public class EHRMWServer extends RestfulServer {

	public static FhirContext FHIR_CONTEXT;

	private static final long serialVersionUID = 7367855477396438198L;
	private static final Logger logger = LoggerFactory.getLogger(EHRMWServer.class);

	public EHRMWServer() {
		super(FhirContext.forR4());
	}

	
	@Override
	protected void initialize() throws ServletException {
		if (logger.isDebugEnabled())
			logger.debug("Starting EHR-MW Server version: {} ", Configuration.getVersion());

		FHIR_CONTEXT = getFhirContext();

		/*
		 *  Creates folder for storing files produced during request processing
		 */
		try {
			Path path = Paths.get(Configuration.getDBPath());
			if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS))
				Files.createDirectories(path);
		} catch (IOException e) {
			throw new ServletException("Error while creating EHRMW DB folder!", e);
		}
		
		/*
		 * Two resource providers are defined. Each one handles a specific
		 * type of resource.
		 */
		List<IResourceProvider> providers = new ArrayList<>();
		providers.add(EHRContextProvider.getApplicationContext().getBean(EncounterResourceProvider.class));
		providers.add(EHRContextProvider.getApplicationContext().getBean(PatientResourceProvider.class));
		setResourceProviders(providers);
		
		/*
		 * Use a narrative generator. This is a completely optional step, 
		 * but can be useful as it causes HAPI to generate narratives for
		 * resources which don't otherwise have one.
		INarrativeGenerator narrativeGen = new DefaultThymeleafNarrativeGenerator();
		getFhirContext().setNarrativeGenerator(narrativeGen);
		 */

		/*
		 * Enable CORS
		 */
		CorsConfiguration config = new CorsConfiguration();
		CorsInterceptor corsInterceptor = new CorsInterceptor(config);
		config.addAllowedHeader("Accept");
		config.addAllowedHeader("Content-Type");
		config.addAllowedOrigin("*");
		config.addExposedHeader("Location");
		config.addExposedHeader("Content-Location");
		//config.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE","OPTIONS"));
		config.setAllowedMethods(Arrays.asList("GET"));
		registerInterceptor(corsInterceptor);

		/*
		 * This server interceptor causes the server to return nicely
		 * formatter and coloured responses instead of plain JSON/XML if
		 * the request is coming from a browser window. It is optional,
		 * but can be nice for testing.
		 */
		//registerInterceptor(new CapabilityStatementCustomizer());
		registerInterceptor(new R2DRequestValidator());
		
		// Starts RequestProcesso Thread
		if (logger.isDebugEnabled())
			logger.debug("Starting RequestProcessingThread...");
		Thread t = new Thread(
			(EHRRequestController) EHRContextProvider.getApplicationContext().getBean("requestController"),
			"RequestProcessingThread");
		t.start();
		
	}

	protected String createPoweredByHeaderComponentName() {
		return "";
	}

	protected String createPoweredByHeaderProductName() {
		return "EHR Middleware";
	}

	
	protected String createPoweredByHeaderProductVersion() {
		return Configuration.getVersion();
	}

}
