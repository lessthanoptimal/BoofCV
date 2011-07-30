/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.tracker.pklt;

import gecv.alg.filter.convolve.FactoryKernelGaussian;
import gecv.alg.filter.derivative.GradientSobel;
import gecv.alg.misc.ImageTestingOps;
import gecv.alg.tracker.klt.KltTracker;
import gecv.alg.tracker.klt.TestKltTracker;
import gecv.alg.transform.pyramid.PyramidUpdateIntegerDown;
import gecv.core.image.ImageGenerator;
import gecv.core.image.border.BorderIndex1D_Extend;
import gecv.core.image.border.ImageBorder1D_F32;
import gecv.core.image.inst.FactoryImageGenerator;
import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.image.ImageFloat32;
import gecv.struct.pyramid.ImagePyramid;
import gecv.struct.pyramid.ImagePyramidI;
import gecv.struct.pyramid.PyramidUpdater;

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
	ImagePyramid<ImageFloat32> pyramid;
	ImagePyramid<ImageFloat32> derivX;
	ImagePyramid<ImageFloat32> derivY;
	PyramidKltTracker<ImageFloat32,ImageFloat32> tracker = createDefaultTracker();

	int cornerX = 20;
	int cornerY = 22;

	public void setup() {
		setup(1,2,2);
	}

	public void setup( int ...scales ) {
		pyramid = createPyramid(scales);
		derivX = createPyramid(scales);
		derivY = createPyramid(scales);
		ImageTestingOps.randomize(image,rand,0,1);
		ImageTestingOps.fillRectangle(image,100,cornerX,cornerY,20,20);
		pyramid.update(image);

		for( int i = 0; i < pyramid.getNumLayers(); i++ ) {

			GradientSobel.process(pyramid.getLayer(i),
					derivX.getLayer(i),derivY.getLayer(i),new ImageBorder1D_F32(BorderIndex1D_Extend.class));
		}
	}

	private ImagePyramid<ImageFloat32> createPyramid( int ...scales ) {

		Kernel1D_F32 kernel = FactoryKernelGaussian.gaussian1D_F32(2,true);
		PyramidUpdater<ImageFloat32> updater = new PyramidUpdateIntegerDown<ImageFloat32>(kernel,ImageFloat32.class);

		ImagePyramidI<ImageFloat32> ret = new ImagePyramidI<ImageFloat32>(false,updater,scales);
		ret.declareLayers((ImageGenerator<ImageFloat32>)FactoryImageGenerator.create(image.getClass()),width,height);

		return ret;
	}

	private PyramidKltTracker<ImageFloat32,ImageFloat32> createDefaultTracker() {
		KltTracker<ImageFloat32, ImageFloat32> klt = TestKltTracker.createDefaultTracker();

		return new PyramidKltTracker<ImageFloat32,ImageFloat32>(klt);
	}
}
