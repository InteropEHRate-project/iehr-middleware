package eu.interopehrate.r2d.ehr;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class EHRContextProvider implements ApplicationContextAware {

	private static ApplicationContext applicationContext;
	
	public static ApplicationContext getApplicationContext() {
        return EHRContextProvider.applicationContext;
    }

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		EHRContextProvider.applicationContext = applicationContext;
	}

}
