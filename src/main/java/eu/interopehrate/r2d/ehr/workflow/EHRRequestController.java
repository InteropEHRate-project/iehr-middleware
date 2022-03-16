package eu.interopehrate.r2d.ehr.workflow;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Autowired;

import eu.interopehrate.r2d.ehr.model.EHRRequest;

public class EHRRequestController {
	@Autowired(required = true)
	private Executor executor;
	
	public void startRequestProcessing(EHRRequest ehrRequest) {
		executor.execute(new EHRRequestProcessor(ehrRequest));
	}
}
