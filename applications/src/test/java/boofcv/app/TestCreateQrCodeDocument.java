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

package boofcv.app;

import boofcv.abst.fiducial.QrCodeDetector;
import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayF32;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Tests the create calibration target application. Invokes the application to create a target. Then converts
 * the postscript document into an image. After the image has been generated it is then processed by the fiducial
 * detector.
 *
 * @author Peter Abeles
 */
public class TestCreateQrCodeDocument extends CommonFiducialPdfChecks {

	public void createDocument( String args ) throws IOException, InterruptedException {
		CreateQrCodeDocument.main(args.split("\\s+"));
	}

	@Test
	public void single() throws IOException, InterruptedException {
		createDocument("-t http://boofcv.org -p LETTER -w 5 -o target.pdf");
		BufferedImage image = loadImage();

		GrayF32 gray = new GrayF32(image.getWidth(),image.getHeight());
		ConvertBufferedImage.convertFrom(image,gray);

//		ShowImages.showWindow(image,"Rendered", true);
//		BoofMiscOps.sleep(10000);
		UtilImageIO.saveImage(image,"test.png");

		QrCodeDetector<GrayF32> detector = FactoryFiducial.qrcode(null,GrayF32.class);

		detector.process(gray);
		List<QrCode> found = detector.getDetections();

		checkFound(found,"http://boofcv.org");
	}

	@Test
	public void two() throws IOException, InterruptedException {
		createDocument("-t http://boofcv.org -t second -p LETTER -w 5 -o target.pdf");
		BufferedImage image = loadImage();

		GrayF32 gray = new GrayF32(image.getWidth(),image.getHeight());
		ConvertBufferedImage.convertFrom(image,gray);

//		ShowImages.showWindow(image,"Rendered", true);
//		BoofMiscOps.sleep(10000);

		QrCodeDetector<GrayF32> detector = FactoryFiducial.qrcode(null,GrayF32.class);

		detector.process(gray);
		List<QrCode> found = detector.getDetections();

		assertEquals(2,found.size());

		checkFound(found,"http://boofcv.org", "second");

	}

	@Test
	public void gridAndCustomDocument() throws IOException, InterruptedException {
		createDocument("-t http://boofcv.org -t second -p 14cm:14cm --GridFill -w 5 -o target.pdf");
		BufferedImage image = loadImage();

		GrayF32 gray = new GrayF32(image.getWidth(),image.getHeight());
		ConvertBufferedImage.convertFrom(image,gray);

//		ShowImages.showWindow(image,"Rendered", true);
//		BoofMiscOps.sleep(10000);

		QrCodeDetector<GrayF32> detector = FactoryFiducial.qrcode(null,GrayF32.class);

		detector.process(gray);
		List<QrCode> found = detector.getDetections();

		assertEquals(4,found.size());

		checkFound(found,"http://boofcv.org", "second");
	}

	private void checkFound( List<QrCode> found , String ...messages ) {
		boolean matches[] = new boolean[messages.length];
		int total = 0;

		for (int i = 0; i < found.size(); i++) {
			int which = -1;
			for (int j = 0; j < messages.length; j++) {
				if( messages[j].equals(found.get(i).message.toString())) {
					which = j;
				}
			}

			if( which >= 0 ) {
				if( !matches[which]) {
					total++;
				}
				matches[which] = true;
			}
		}

		assertEquals(messages.length,total);
	}
}
