package eu.interopehrate.r2d.ehr.converter.cda;

import java.io.File;

public class Tester {

	public static void main(String[] args) {
		
		CDAEncounterEverythingConverter converter = new CDAEncounterEverythingConverter();
		
		try {
			converter.convertToFile(new File("Patient_TEN_EncEnv_6601652.xml"), new File("alessio.json"));
			// converter.convertToFile(new File("Patient_SEVEN_EncEnv_6882310.xml"), new File("alessio.json"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
