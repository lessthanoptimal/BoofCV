/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.app.markers;

import boofcv.alg.drawing.FiducialImageEngine;
import boofcv.alg.fiducial.square.FiducialSquareGenerator;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayU8;
import org.apache.commons.io.FilenameUtils;
import org.ddogleg.struct.GrowQueue_I64;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 * Generates the QR Code PDF Document
 *
 * @author Peter Abeles
 */
public class CreateSquareMarkerDocumentImage {

	String documentName;
	FiducialSquareGenerator generator;
	FiducialImageEngine render = new FiducialImageEngine();
	int whiteBorderPixels;

	public CreateSquareMarkerDocumentImage(String documentName ) {
		this.documentName = documentName;
		this.generator = new FiducialSquareGenerator(render);
	}

	public FiducialSquareGenerator getGenerator() {
		return generator;
	}

	public void render( java.util.List<String> names , GrowQueue_I64 patterns , int gridWidth ) {
		render.configure(whiteBorderPixels,(int)generator.getMarkerWidth());
		for (int i = 0; i < patterns.size; i++) {
			generator.generate(patterns.get(i),gridWidth);
			save(render.getGray(),names.get(i));
		}
	}

	public void render( java.util.List<String> names , List<GrayU8> patterns ) {
		render.configure(whiteBorderPixels,(int)generator.getMarkerWidth());
		for (int i = 0; i < patterns.size(); i++) {
			generator.generate(patterns.get(i));
			int count = patterns.size() > 1 ? i : -1;
			save(render.getGray(),names.get(i));
		}
	}

	private void save( GrayU8 fiducial , String name ) {
		String fileName;
		String ext = FilenameUtils.getExtension(documentName);
		if( ext.length() == 0 ) {
			fileName = documentName+".png";
		} else {
			File f = new File(documentName);
			String n = f.getName();
			fileName = new File(f.getParentFile(), n.substring(0, n.length() - ext.length() - 1) + name + "." + ext).getPath();
		}
		BufferedImage output = new BufferedImage(fiducial.width,fiducial.height,BufferedImage.TYPE_INT_RGB);
		ConvertBufferedImage.convertTo(fiducial,output);

		System.out.println("Saving "+fileName);
		UtilImageIO.saveImage(output,fileName);
	}

	public void setMarkerWidth( int pixels ) {
		generator.setMarkerWidth(pixels);
	}

	public void setWhiteBorder( int pixels ) {
		whiteBorderPixels = pixels;
	}

}
