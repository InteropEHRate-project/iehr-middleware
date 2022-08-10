package eu.interopehrate.r2d.ehr.interceptors;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CodeType;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.ResponseDetails;
import eu.interopehrate.r2d.ehr.Configuration;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: class used to customize the default implementation of the 
 * CapabilityStatement provided by HAPI FHIR.
 */

@Interceptor
public class CapabilityStatementCustomizer {

	@Hook(value = Pointcut.SERVER_OUTGOING_RESPONSE)
	public void customizeCapabilityStatement(RequestDetails theRequest, ResponseDetails theResponse) {
		if ("metadata".equals(theRequest.getOperation())) {

			// Cast to the appropriate version
			CapabilityStatement cs = (CapabilityStatement) theResponse.getResponseResource();

			List<CodeType> supportedFormats = new ArrayList<CodeType>();
			supportedFormats.add(new CodeType("application/json"));

			// Customize the CapabilityStatement as desired
			cs
			.addInstantiates("http://www.interopehrate.eu/r2d/capabilitystatement")
			.setPublisher("The InteropEHRate project -  www.interopehrate.eu")
			.setFormat(supportedFormats)
			.getSoftware()
			.setName("R2D EHR Middleware - " + Configuration.getProperty("ehr.name"))
			.setVersion(Configuration.getVersion());
		}
	}

}