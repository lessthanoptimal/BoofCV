/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.app.fiducials;

import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayU8;
import org.apache.commons.io.FilenameUtils;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Generates the QR Code PDF Document
 *
 * @author Peter Abeles
 */
public abstract class CreateFiducialDocumentImage {

	String documentName;

	public int markerWidth;
	// If > 0 then it specifies the height, otherwise a square marker is assumed.
	public int markerHeight = -1;

	protected CreateFiducialDocumentImage( String documentName ) {
		this.documentName = documentName;
	}

	protected void save( GrayU8 fiducial, String name ) {
		String fileName;
		String ext = FilenameUtils.getExtension(documentName);
		if (ext.length() == 0) {
			fileName = documentName + ".png";
		} else {
			File f = new File(documentName);
			String n = f.getName();
			fileName = new File(f.getParentFile(), n.substring(0, n.length() - ext.length() - 1) + name + "." + ext).getPath();
		}
		BufferedImage output = new BufferedImage(fiducial.width, fiducial.height, BufferedImage.TYPE_INT_RGB);
		ConvertBufferedImage.convertTo(fiducial, output);

		System.out.println("Saving " + fileName);
		UtilImageIO.saveImage(output, fileName);
	}
}
