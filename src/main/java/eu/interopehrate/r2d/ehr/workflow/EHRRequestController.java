package eu.interopehrate.r2d.ehr.workflow;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import eu.interopehrate.r2d.ehr.model.EHRRequest;

public class EHRRequestController implements Runnable {
	// @Autowired(required = true)
	// private Executor executor;
	
	private final Logger logger = LoggerFactory.getLogger(EHRRequestController.class);	
	private BlockingQueue<EHRRequest> requestQueue = new ArrayBlockingQueue<EHRRequest>(20, true);
	
	
	public void startRequestProcessing(EHRRequest ehrRequest) {
		try {
			requestQueue.put(ehrRequest);
		} catch (InterruptedException e) {
			logger.error("Error while putting request into queue!", e);
			throw new IllegalStateException(e);
		}
		// executor.execute(new EHRRequestProcessor(ehrRequest));
	}

	
	public void shutdown() {
		EHRRequest shutdownRequest = new EHRRequest();
		shutdownRequest.setR2dRequestId("shutdown");
		requestQueue.add(shutdownRequest);
	}

	
	@Override
	public void run() {
		try {
			while (true) {
				EHRRequest ehrRequest = requestQueue.take();
				// Checks if Thread has been interrupted
				if (ehrRequest.getR2dRequestId().equals("shutdown") || Thread.currentThread().isInterrupted()) {
					logger.debug("Thread has been interrupted, Thread closing run method.");
					return;
				}
				
				EHRRequestProcessor requestProcessor = new EHRRequestProcessor(ehrRequest);
				requestProcessor.run();
			}
		} catch (InterruptedException e) {
			if (Thread.currentThread().isInterrupted()) {
				return;
			}
		}
	}
	
	
}
