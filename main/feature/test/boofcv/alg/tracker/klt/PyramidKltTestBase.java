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

package boofcv.alg.tracker.klt;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.transform.pyramid.PyramidOps;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.pyramid.PyramidDiscrete;

import java.util.Random;


/**
 * Base class for unit tests of Pyramidal KLT
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class PyramidKltTestBase {
	Random rand = new Random(234);

	int width = 50;
	int height = 60;

	int featureReadius = 2;

	ImageFloat32 image = new ImageFloat32(width,height);
	PyramidDiscrete<ImageFloat32> pyramid;
	ImageFloat32[] derivX;
	ImageFloat32[] derivY;
	PyramidKltTracker<ImageFloat32,ImageFloat32> tracker = createDefaultTracker();

	int cornerX = 20;
	int cornerY = 22;

	public void setup() {
		setup(1,2,4);
	}

	public void setup( int ...scales ) {

		pyramid = FactoryPyramid.discreteGaussian(scales,-1,2,false,ImageFloat32.class);
		ImageMiscOps.fillUniform(image,rand,0,10);
		ImageMiscOps.fillRectangle(image,100,cornerX,cornerY,20,20);
		pyramid.process(image);

		derivX = PyramidOps.declareOutput(pyramid,ImageFloat32.class);
		derivY = PyramidOps.declareOutput(pyramid,ImageFloat32.class);

		ImageGradient<ImageFloat32,ImageFloat32> gradient = FactoryDerivative.sobel(ImageFloat32.class,ImageFloat32.class);
		PyramidOps.gradient(pyramid,gradient,derivX,derivY);
	}


	private PyramidKltTracker<ImageFloat32,ImageFloat32> createDefaultTracker() {
		KltTracker<ImageFloat32, ImageFloat32> klt = TestKltTracker.createDefaultTracker();

		return new PyramidKltTracker<ImageFloat32,ImageFloat32>(klt);
	}
}
