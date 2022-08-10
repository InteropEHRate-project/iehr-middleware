package eu.interopehrate.r2d.ehr;

import java.text.NumberFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: utility class to print memory status.
 */
public class MemoryLogger {
	private static final Logger LOGGER = LoggerFactory.getLogger(MemoryLogger.class);
	private static final double MEGABYTE = 1024D * 1024D;
	
	public static void logMemory() {
		Runtime runtime = Runtime.getRuntime();
		
        NumberFormat format = NumberFormat.getInstance();
        double maxMemory = runtime.maxMemory();
        double readyMemory = runtime.totalMemory();
        double usedMemory = readyMemory - runtime.freeMemory();
        double freeMemory = maxMemory - usedMemory;

		LOGGER.info("Max: {} Mb, Used: {} Mb, Free: {} Mb", 
				format.format(maxMemory / MEGABYTE), 
				format.format(usedMemory / MEGABYTE),
				format.format(freeMemory / MEGABYTE));
	}

}
