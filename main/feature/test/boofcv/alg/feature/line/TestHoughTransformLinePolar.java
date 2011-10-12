/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.feature.line;

import boofcv.abst.feature.detect.extract.FeatureExtractor;
import boofcv.alg.feature.detect.line.HoughTransformLinePolar;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.struct.FastQueue;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.line.LineParametric2D_F32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestHoughTransformLinePolar {

	int width = 30;
	int height = 40;

	/**
	 * See if it can detect an obvious line in the image
	 */
	@Test
	public void obviousLines() {
		ImageUInt8 image = new ImageUInt8(width,height);

		for( int i = 0; i < height; i++ ) {
			image.set(5,i,1);
		}

		FeatureExtractor extractor = FactoryFeatureExtractor.nonmax(4,5,0, true, true);
		HoughTransformLinePolar alg = new HoughTransformLinePolar(extractor,40,180);

		alg.transform(image);

		FastQueue<LineParametric2D_F32> lines =  alg.extractLines();

		assertEquals(1,lines.size());

		LineParametric2D_F32 l = lines.get(0);
		assertEquals(l.p.x,5,0.1);
		assertEquals(Math.abs(l.slope.x),0,0);
		assertEquals(Math.abs(l.slope.y),1,0.1);
	}
}
