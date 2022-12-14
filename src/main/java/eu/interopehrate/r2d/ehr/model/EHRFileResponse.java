package eu.interopehrate.r2d.ehr.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 *      Author: Engineering Ingegneria Informatica
 *     Project: InteropEHRate - www.interopehrate.eu
 *
 * Description: this class represent a list of responses produced during 
 * the processing of an instance of EHRRequest. In this case the
 * response is stored in list of files.
 * 
 * @SeeAlso: EHRResponse.java. 
 */
public class EHRFileResponse extends EHRResponse {
	
	private List<File> fileList = new ArrayList<File>();
	

	public void addResponseFile(File response) {
		fileList.add(response);
	}
	
	public int getResponseFileSize() {
		return fileList.size();
	}
	
	public File getResponseFile(int index) {
		if (index >= 0 && index <= fileList.size() -1)
			return fileList.get(index);
		else
			return null;
	}
	
	public File getFirstResponseFile() {
		return fileList.get(0);
	}
}
