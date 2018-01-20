/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.qrcode;

import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.misc.BoofMiscOps;
import org.junit.Test;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author Peter Abeles
 */
public class TestQrCodeGeneratorImage {
	boolean showImage = false; // use a boolean to make it easier to turn on and off. Don't need to add import

	@Test
	public void showImage() {
		if(!showImage)
			return;

		QrCode qr = new QrCodeEncoder().setVersion(1).
				setError(QrCode.ErrorLevel.M).
				setMask(QrCodeMaskPattern.M011).
				addNumeric("01234567").fixate();

		QrCodeGeneratorImage generator = new QrCodeGeneratorImage(6);

		generator.render(qr);

		BufferedImage output = ConvertBufferedImage.convertTo(generator.gray,null,true);

		// add a border so that qr code readers can decode what is shown
		BufferedImage border = new BufferedImage(output.getWidth()+50,output.getHeight()+50,output.getType());
		Graphics2D g2 = border.createGraphics();
		g2.setColor(Color.WHITE);
		g2.fillRect(0,0,border.getWidth(),border.getHeight());
		g2.drawImage(output,25,25,null);

		ShowImages.showWindow(border,"QR Code", true);
		BoofMiscOps.sleep(100000);
	}
}