/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.flow;

import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.tracker.klt.KltTracker;
import boofcv.core.image.border.BorderType;
import boofcv.factory.tracker.FactoryTrackerAlg;
import boofcv.struct.flow.ImageFlow;
import boofcv.struct.image.ImageFloat32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestDenseOpticalFlowKlt {


	/**
	 * Very simple positive case
	 */
	@Test
	public void positive() {
		ImageFloat32 image0 = new ImageFloat32(30,40);
		ImageFloat32 image1 = new ImageFloat32(30,40);

		ImageFloat32 derivX = new ImageFloat32(30,40);
		ImageFloat32 derivY = new ImageFloat32(30,40);

		ImageMiscOps.fillRectangle(image0,50,10,12,2,2);
		GImageDerivativeOps.sobel(image0,derivX,derivY, BorderType.EXTENDED);

		ImageMiscOps.fillRectangle(image1,50,11,13,2,2);

		KltTracker<ImageFloat32,ImageFloat32> tracker = FactoryTrackerAlg.klt(null,ImageFloat32.class,null);
		DenseOpticalFlowKlt<ImageFloat32,ImageFloat32> alg =
				new DenseOpticalFlowKlt<ImageFloat32, ImageFloat32>(tracker,3);


		ImageFlow flow = new ImageFlow(30,40);
		flow.invalidateAll();

		alg.process(image0,derivX,derivY,image1,flow);

		// no texture in the image so KLT can't do anything
		check(flow.get(0,0),false,0,0);
		check(flow.get(29,39),false,0,0);
		// there is texture at the target
		check(flow.get(10,12),true,1,1);
		check(flow.get(11,12),true,1,1);
		check(flow.get(10,13),true,1,1);
		check(flow.get(11,13),true,1,1);
	}

	private void check( ImageFlow.D flow , boolean valid , float x , float y ) {
		assertEquals(valid,flow.valid);
		if( valid ) {
			assertEquals(x,flow.x,0.05f);
			assertEquals(y,flow.y,0.05f);
		}
	}

	/**
	 * Very simple negative case. The second image is blank so it should fail at tracking
	 */
	@Test
	public void negative() {
		ImageFloat32 image0 = new ImageFloat32(30,40);
		ImageFloat32 image1 = new ImageFloat32(30,40);

		ImageFloat32 derivX = new ImageFloat32(30,40);
		ImageFloat32 derivY = new ImageFloat32(30,40);

		ImageMiscOps.fillRectangle(image0,200,7,9,5,5);
		GImageDerivativeOps.sobel(image0,derivX,derivY, BorderType.EXTENDED);

		KltTracker<ImageFloat32,ImageFloat32> tracker = FactoryTrackerAlg.klt(null,ImageFloat32.class,null);
		DenseOpticalFlowKlt<ImageFloat32,ImageFloat32> alg =
				new DenseOpticalFlowKlt<ImageFloat32, ImageFloat32>(tracker,3);


		ImageFlow flow = new ImageFlow(30,40);

		alg.process(image0,derivX,derivY,image1,flow);

		check(flow.get(10,12),false,1,1);
		check(flow.get(11,12),false,1,1);
		check(flow.get(10,13),false,1,1);
		check(flow.get(11,13),false,1,1);
	}

}
