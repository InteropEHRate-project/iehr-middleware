package eu.interopehrate.r2d.ehr.interceptors;

import org.hl7.fhir.r4.model.CapabilityStatement;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.ResponseDetails;

@Interceptor
public class CapabilityStatementCustomizer {

	@Hook(value = Pointcut.SERVER_OUTGOING_RESPONSE)
	public void customizeCapabilityStatement(RequestDetails theRequest, ResponseDetails theResponse) {
		if ("metadata".equals(theRequest.getOperation())) {

			// Cast to the appropriate version
			CapabilityStatement cs = (CapabilityStatement) theResponse.getResponseResource();

			// Customize the CapabilityStatement as desired
			cs
			.addInstantiates("http://www.interopehrate.eu/r2d/capabilitystatement")
			.setPublisher("The InteropEHRate project -  www.interopehrate.eu")
			.getSoftware()
			.setName("R2D EHR Middleware")
			.setVersion("1.0");
		}
	}

}