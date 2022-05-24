package eu.interopehrate.r2d.ehr.workflow;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Autowired;

import eu.interopehrate.r2d.ehr.model.EHRRequest;

public class EHRRequestController implements Runnable {
	//@Autowired(required = true)
	//private Executor executor;
	
	private BlockingQueue<EHRRequest> requestQueue = new ArrayBlockingQueue<EHRRequest>(20, true);
	
	
	public void startRequestProcessing(EHRRequest ehrRequest) {
		try {
			requestQueue.put(ehrRequest);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// executor.execute(new EHRRequestProcessor(ehrRequest));
	}


	@Override
	public void run() {
		try {
			while (true) {
				EHRRequest ehrRequest = requestQueue.take();
				EHRRequestProcessor requestProcessor = new EHRRequestProcessor(ehrRequest);
				requestProcessor.run();
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
}
