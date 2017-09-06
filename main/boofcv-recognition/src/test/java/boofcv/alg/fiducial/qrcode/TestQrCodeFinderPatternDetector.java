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


import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.shapes.polygon.DetectPolygonBinaryGrayRefine;
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.struct.image.GrayF32;
import georegression.struct.shapes.Polygon2D_F64;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestQrCodeFinderPatternDetector {
	@Test
	public void easy() {
		fail("Implement");
	}

	@Test
	public void just_two() {
		fail("Implement");
	}

	/**
	 * Simple positive example
	 */
	@Test
	public void considerConnect_positive() {
		fail("Implement");
	}

	/**
	 * The two patterns are rotated 45 degrees relative to each other
	 */
	@Test
	public void considerConnect_negative_rotated() {
		fail("Implement");
	}


	@Test
	public void checkPositionPatternAppearance() {
		GrayF32 image = render(40,60,70);

		QrCodeFinderPatternDetector<GrayF32> alg = createAlg();

		Polygon2D_F64 square = new Polygon2D_F64(4);
		square.get(0).set(40,60);
		square.get(1).set(110,60);
		square.get(2).set(110,130);
		square.get(3).set(40,130);

		alg.interpolate.setImage(image);

		assertTrue(alg.checkPositionPatternAppearance(square,100));

		ImageMiscOps.fillRectangle(image,0,40,60,70,30);
		assertFalse(alg.checkPositionPatternAppearance(square,100));
	}

	@Test
	public void positionSquareIntensityCheck() {

		float positive[] = new float[]{10,200,10,10,10,200,10};

		assertTrue(QrCodeFinderPatternDetector.positionSquareIntensityCheck(positive,100));

		for (int i = 0; i < 7; i++) {
			float negative[] = positive.clone();

			negative[i] = negative[i] < 100 ? 200 : 10;
			assertFalse(QrCodeFinderPatternDetector.positionSquareIntensityCheck(negative,100));
		}
	}

	private QrCodeFinderPatternDetector<GrayF32> createAlg() {

		ConfigPolygonDetector config = new ConfigPolygonDetector(4,4);
		config.detector.clockwise = false;
		DetectPolygonBinaryGrayRefine<GrayF32> squareDetector =
				FactoryShapeDetector.polygon(config, GrayF32.class);

		return new QrCodeFinderPatternDetector<>(squareDetector,2);
	}


	private GrayF32 render( int x0 , int y0 , int width ) {
		GrayF32 image = new GrayF32(300,350);
		ImageMiscOps.fill(image,255);

		ImageMiscOps.fillRectangle(image,0,x0,y0,width,width);
		ImageMiscOps.fillRectangle(image,255,x0+width/7,y0+width/7,width*5/7,width*5/7);
		ImageMiscOps.fillRectangle(image,0,x0+width*2/7,y0+width*2/7,width*3/7,width*3/7);

		return image;
	}


}