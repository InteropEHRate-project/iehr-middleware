package eu.interopehrate.r2d.zip;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: Utiliti class used by the DicomImageAnonymizer.
 */

public class ZipUtilities {
	
	
	public static boolean isZipFile(File file) throws FileNotFoundException, IOException {
		if (file.isDirectory())
		    return false;
		    
		byte[] bytes = new byte[4];
		try (FileInputStream fIn = new FileInputStream(file)) {
		    if (fIn.read(bytes) != 4)
		        return false;
		}
		
		final int header = bytes[0] + (bytes[1] << 8) + (bytes[2] << 16) + (bytes[3] << 24);
		return 0x04034b50 == header;
	}
	
	
	public static void uncompress(File inputZipFile, File outputFolder) throws Exception {
		// creates the factory
		ArchiveStreamFactory factory = new ArchiveStreamFactory();
		
		// unzip the archive file...
		try (InputStream inputStream = new BufferedInputStream(new FileInputStream(inputZipFile));
			 ArchiveInputStream inputZip = factory.createArchiveInputStream(inputStream)) {
			
			ArchiveEntry entry = null;
			
			// loops over entries of Bundle
			while ((entry = inputZip.getNextEntry()) != null) {
		        if (!inputZip.canReadEntryData(entry)) {
		            continue;
		        }
		        
		        if (entry.getName().startsWith("__MACOSX")) {
		        	continue;
		        }
		        
		        // unzip the file or create the folder...
		        File fileToUnzip = new File(outputFolder.getAbsolutePath() + "/" + entry.getName());
		        if (entry.isDirectory()) {
		            if (!fileToUnzip.isDirectory() && !fileToUnzip.mkdirs()) 
		                throw new IOException("failed to create directory " + fileToUnzip);
		        } else {
			        File unzippedFileParent = fileToUnzip.getParentFile();
		            if (!unzippedFileParent.isDirectory() && !unzippedFileParent.mkdirs())
		                throw new IOException("failed to create directory " + unzippedFileParent);
		            
		            try (OutputStream o = Files.newOutputStream(fileToUnzip.toPath())) {
		                IOUtils.copy(inputZip, o);
		            }
		        }
		    }
		}
	}
	
	
	public static void compress(File inputFolderToZip, File outputZippedFile) {
		
		// File zippedFile = new File(folderToZip.getAbsolutePath() + "/" + name + ".zip");
		// Create zip file stream.
		try (ZipArchiveOutputStream archive = new ZipArchiveOutputStream(new FileOutputStream(outputZippedFile))) {
	 
			// Walk through files, folders & sub-folders.
			Files.walk(inputFolderToZip.toPath()).forEach(p -> {
				File file = p.toFile();
				
				// Directory is not streamed, but its files are streamed into zip file with
				// folder in it's path
				if (!file.isDirectory() && !file.getName().equals(".DS_Store")) {
					String entryName = computeFileName(inputFolderToZip, file);
					ZipArchiveEntry zipEntry = new ZipArchiveEntry(file, entryName);
					try (FileInputStream fis = new FileInputStream(file)) {
						archive.putArchiveEntry(zipEntry);
						IOUtils.copy(fis, archive);
						archive.closeArchiveEntry();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
	 
			// Complete archive entry addition.
			archive.finish();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	private static String computeFileName(File inputFolderToZip, File file) {
		String a = inputFolderToZip.getAbsolutePath();
		if (!a.endsWith("/"))
			a += "/";
		
		String b = file.getAbsolutePath();
		
		return b.substring(a.length());
	}
}
