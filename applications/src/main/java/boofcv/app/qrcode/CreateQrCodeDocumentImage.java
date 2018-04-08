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

package boofcv.app.qrcode;

import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.alg.fiducial.qrcode.QrCodeGeneratorImage;
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
public class CreateQrCodeDocumentImage {

	int moduleWidthPixels;

	String documentName;

	public CreateQrCodeDocumentImage(String documentName , int moduleWidthPixels ) {
		this.documentName = documentName;
		this.moduleWidthPixels = moduleWidthPixels;
	}

	public void render(java.util.List<QrCode> markers ) {

		for( int i = 0; i < markers.size(); i++ ) {
			QrCodeGeneratorImage generator = new QrCodeGeneratorImage(moduleWidthPixels);
			generator.setBorderModule(2);
			generator.render(markers.get(i));

			String name;
			String ext = FilenameUtils.getExtension(documentName);
			if( ext.length() == 0 ) {
				name = documentName+i+".png";
			} else {
				if( markers.size() > 1 ) {
					File f = new File(documentName);
					String n = f.getName();
					name = new File(f.getParentFile(), n.substring(0, n.length() - ext.length() - 1) + i + "." + ext).getPath();
				} else {
					name = documentName;
				}
			}

			GrayU8 gray = generator.getGray();
			BufferedImage output = new BufferedImage(gray.width,gray.height,BufferedImage.TYPE_INT_RGB);
			ConvertBufferedImage.convertTo(gray,output);

			System.out.println("Saving "+name);
			UtilImageIO.saveImage(output,name);
		}
	}
}
