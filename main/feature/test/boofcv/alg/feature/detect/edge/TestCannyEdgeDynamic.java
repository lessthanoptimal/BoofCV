/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.edge;

import boofcv.abst.filter.blur.BlurFilter;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.factory.feature.detect.edge.FactoryEdgeDetectors;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestCannyEdgeDynamic {

	int width = 150;
	int height = 200;

	/**
	 * Test the pathological case where the input image has no texture.  The threshold will be zero and the
	 * edge intensity will be zero everywhere.
	 */
	@Test
	public void canHandleNoTexture() {
		GrayU8 input = new GrayU8(width,height);
		GrayU8 output = new GrayU8(width,height);

		ImageMiscOps.fill(output,2);

		CannyEdge<GrayU8,GrayS16> alg =
				FactoryEdgeDetectors.canny(2, false, true, GrayU8.class, GrayS16.class);

		alg.process(input,0.075f,0.3f,output);

		for( int i = 0; i < output.data.length; i++ ) {
			assertEquals(0,output.data[i]);
		}

		// try it with a trace now
		alg = FactoryEdgeDetectors.canny(2, true, true, GrayU8.class, GrayS16.class);
		ImageMiscOps.fill(output,2);
		alg.process(input,0.075f,0.3f,output);

		List<EdgeContour> contour = alg.getContours();
		assertTrue(contour.size() == 0);

		for( int i = 0; i < output.data.length; i++ )
			assertEquals(0,output.data[i]);
	}

	/**
	 * Just checks to see if it computes a reasonable threshold given fractional parameters
	 */
	@Test
	public void basicTestPoints() {

		GrayU8 input = new GrayU8(width,height);

		ImageMiscOps.fillRectangle(input, 50, 20, 30, 40, 50);

		BlurFilter<GrayU8> blur = FactoryBlurFilter.gaussian(GrayU8.class, -1, 1);
		ImageGradient<GrayU8,GrayS16> gradient = FactoryDerivative.sobel(GrayU8.class, GrayS16.class);

		CannyEdgeDynamic<GrayU8,GrayS16> alg = new CannyEdgeDynamic<>(blur, gradient, true);

		alg.process(input,0.2f,0.4f,null);

		List<EdgeContour> contour = alg.getContours();

		int totalPass = 0;
		for( EdgeContour e : contour ) {
			int total = 0;
			for( EdgeSegment s : e.segments ) {
				// really should check to see if the points are near the rectangle....
				total += s.points.size();
			}
			if( total >= 2 * 50 + 2 * 38 )
				totalPass++;
		}

		assertEquals(1,totalPass);
	}

}
