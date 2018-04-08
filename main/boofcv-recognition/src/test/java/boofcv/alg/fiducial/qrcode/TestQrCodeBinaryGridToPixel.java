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

package boofcv.alg.fiducial.qrcode;

import georegression.struct.point.Point2D_F32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestQrCodeBinaryGridToPixel {
	@Test
	public void simple() {

		QrCode qr = new QrCodeEncoder().setVersion(2).addNumeric("12340324").fixate();

		QrCodeGeneratorImage generator = new QrCodeGeneratorImage(4);
		int border = generator.borderModule*4;
		generator.render(qr);

		QrCodeBinaryGridToPixel alg = new QrCodeBinaryGridToPixel();
		alg.addAllFeatures(qr);
		alg.computeTransform();

		check(alg,0,0,border,border);
		check(alg,7,0,border+7*4,border);
		check(alg,7,7,border+7*4,border+7*4);

	}

	private void check(QrCodeBinaryGridToPixel transformGrid ,
					   float x , float y ,
					   float expectedX , float expectedY )
	{
		Point2D_F32 found = new Point2D_F32();

		transformGrid.gridToImage(y,x,found);

		assertEquals(expectedX,found.x,1e-4f);
		assertEquals(expectedY,found.y,1e-4f);
	}
}