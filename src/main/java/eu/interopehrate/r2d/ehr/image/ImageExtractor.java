package eu.interopehrate.r2d.ehr.image;

import java.io.File;

public interface ImageExtractor {

	File createReducedFile(String requestId, String inputFileName) throws Exception;
}
