package eu.interopehrate.r2d.ehr.image;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.deident.DeIdentifier;
import org.dcm4che3.io.DicomEncodingOptions;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomInputStream.IncludeBulkData;
import org.dcm4che3.io.DicomOutputStream;

public class DicomAnonymizer {

	private static final String DICOM_ANONYMIZED_FOLDER = "ANON_DICOM";
	private static final String DICOM_STANDARD_FOLDER = "DICOM";


	public void anonymizeDicomImage(File src, File dest) throws IOException {
		DeIdentifier deidentifier = new DeIdentifier(DeIdentifier.Option.RetainUIDsOption);
		transcode(src, dest, deidentifier);
	}

	
	public void anonymizeFolder(File dicomStudyDir) throws Exception {
		DeIdentifier deidentifier = new DeIdentifier(DeIdentifier.Option.RetainUIDsOption);

		if (DicomUtilities.isDicomDir(dicomStudyDir))
			anonymizeDicomDirFolder(dicomStudyDir, deidentifier);
		else
			anonymizeDicomListFolder(dicomStudyDir, deidentifier);
	}
	
	
	private void anonymizeDicomDirFolder(File dicomStudyDir, DeIdentifier deidentifier) throws Exception {
		// #1 folder containing source images to be anonymized
		File dicomDir = new File(dicomStudyDir.getAbsolutePath() + "/" + DICOM_STANDARD_FOLDER);
		
		// #2 creates the folder for anonymized images
		File anonymizedDicomDir = new File(dicomStudyDir.getAbsolutePath() + "/" + DICOM_ANONYMIZED_FOLDER);
		anonymizedDicomDir.mkdirs();
		
		// #3 anonymize each file
		for (File dicomFile : dicomDir.listFiles()) {
			if (dicomFile.getName().equals(".DS_Store"))
				continue;
			
			transcode(dicomFile, new File(anonymizedDicomDir.getAbsolutePath() + "/" + dicomFile.getName()), deidentifier);
		}
		
		// #4 deletes the folder with the source images
		FileUtils.deleteDirectory(dicomDir);
		
		// #5 renames working folder to DICOM 
		anonymizedDicomDir.renameTo(new File(dicomStudyDir.getAbsolutePath() + "/" + DICOM_STANDARD_FOLDER));
	}
	
	
	private void anonymizeDicomListFolder(File dicomStudyDir, DeIdentifier deidentifier) throws Exception {
		Files.walk((dicomStudyDir.toPath()))
    	.filter(Files::isRegularFile)
    	.filter(path -> path.toString().endsWith(".dcm"))
    	.forEach(path -> {
			try {
				String anonFileName = path.toString();
				anonFileName = anonFileName.replace(".dcm", "_anon.dcm");
				transcode(path.toFile(), new File(anonFileName), deidentifier);
				// after anonymization, delete non-anonymized file
				Files.delete(path);
			} catch (IOException e) {
				e.printStackTrace();
			}    	
    	});
	}
	
	
	private void transcode(File src, File dest, DeIdentifier deidentifier) throws IOException {
        Attributes fmi;
        Attributes dataset;
        // reads input stream
        try (DicomInputStream dis = new DicomInputStream(src)) {
            dis.setIncludeBulkData(IncludeBulkData.URI);
            fmi = dis.readFileMetaInformation();
            dataset = dis.readDataset();
        }
        
        // deidentifies
        deidentifier.deidentify(dataset);
        
        // writes output stream
        if (fmi != null)
            fmi = dataset.createFileMetaInformation(fmi.getString(Tag.TransferSyntaxUID));
        try (DicomOutputStream dos = new DicomOutputStream(dest)) {
            dos.setEncodingOptions(DicomEncodingOptions.DEFAULT);
            dos.writeDataset(fmi, dataset);
        }
	}
	
	
}
