package eu.interopehrate.r2d.ehr.image;

import java.io.File;
import java.io.FilenameFilter;

public class DicomUtilities {

	private static final String DICOM_STANDARD_FOLDER = "DICOM";

	public static boolean isDicomDir(File dicomFolder) {
		// #1 determines the structure of a DICOM file
		String[] dicomDir = dicomFolder.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				if (dir.isDirectory() && name.equals(DICOM_STANDARD_FOLDER))
					return true;

				return false;
			}
		});
		
		return dicomDir.length == 1;
	}
	
}
