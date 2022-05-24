package eu.interopehrate.r2d.ehr;

import java.io.IOException;
import java.util.Properties;

public final class Configuration {

	public static final String EHR_MW_CREDENTIALS = "ehrmw.credentials";
	public static final String R2DA_ENDPOINT = "r2da.endpoint";
	public static final String R2DA_R2D_CONTEXT = "r2da.r2d.context";
	public static final String R2DA_SERVICES_CONTEXT = "r2da.services.context";
	public static final String IHS_ENDPOINT = "ihs.endpoint";
	public static final String EHR_ENDPOINT = "ehr.endpoint";
	
	private static Properties config = new Properties();
	
	
	static {
		try {
			config.load(Configuration.class.getClassLoader().getResourceAsStream("application.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String getProperty(String name) {
		return config.getProperty(name);	
	}
	
	public static String getR2DServicesContextPath() {
		return config.getProperty(R2DA_ENDPOINT) + "/" + config.getProperty(R2DA_SERVICES_CONTEXT);
	}
	
	public static String getDBPath() {
		return Configuration.getProperty("ehrmw.storage.path");
	}
	
	public static String getR2DADBPath() {
		return Configuration.getProperty("r2da.storage.path");
	}
}
