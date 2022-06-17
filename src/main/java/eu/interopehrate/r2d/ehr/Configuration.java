package eu.interopehrate.r2d.ehr;

import java.io.IOException;
import java.util.Properties;

public final class Configuration {

	public static final String EHR_MW_CREDENTIALS = "ehrmw.credentials";
	public static final String EHR_MW_STORAGE_PATH = "ehrmw.storage.path";
	public static final String EHR_MW_VERSION = "ehrmw.version";

	public static final String R2DA_ENDPOINT = "r2da.endpoint";
	public static final String R2DA_SERVICES_CONTEXT = "r2da.services.context";
	public static final String R2DA_STORAGE_PATH = "r2da.storage.path";

	public static final String IHS_ENDPOINT = "ihs.endpoint";
	public static final String IHS_TIMEOUT = "ihs.timeoutInMinutes";
	public static final String IHS_MAPPING_CODES = "ihs.mapping.codes";

	public static final String EHR_PROTOCOL = "ehr.protocol";
	public static final String EHR_HOST = "ehr.host";
	public static final String EHR_PORT = "ehr.port";
	public static final String EHR_CONTEXT_PATH = "ehr.contextPath";
	public static final String EHR_TIMEOUT = "ehr.timeoutInMinutes";
	public static final String EHR_MIME = "ehr.mime";
	public static final String EHR_NAME = "ehr.name";
	public static final String EHR_FILE_EXT = "ehr.fileExtension";
	public static final String EHR_DELETE_TEMP_FILES = "ehr.deleteTmpFiles";
	public static final String EHR_HEADER = "ehr.header";
	public static final String EHR_LANGUAGE = "ehr.language";
	public static final String EHR_IMAGE_EXTRACTOR = "ehr.imageExtractor.bean";
	
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
		String tmp = Configuration.getProperty(EHR_MW_STORAGE_PATH);
		if (!tmp.endsWith("/"))
			tmp += "/";
		
		return tmp;
	}
	
	public static String getR2DADBPath() {
		String tmp =  Configuration.getProperty(R2DA_STORAGE_PATH);
		if (!tmp.endsWith("/"))
			tmp += "/";
		
		return tmp;
	}
	
	public static String getVersion() {
		return Configuration.getProperty(EHR_MW_VERSION);
	}

}
