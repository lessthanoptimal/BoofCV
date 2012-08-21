/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.intensity;

import boofcv.alg.feature.detect.intensity.impl.ImplFastCorner12_F32;
import boofcv.alg.feature.detect.intensity.impl.ImplFastCorner12_Table_F32;
import boofcv.alg.misc.ImageTestingOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class BenchmarkFeatureIntensity< T extends ImageSingleBand> {
	static int imgWidth = 640;
	static int imgHeight = 480;
	static long TEST_TIME = 1000;

	T input;
	ImageFloat32 intensity;

	public BenchmarkFeatureIntensity( Class<T> imageType ) {
		input = GeneralizedImageOps.createSingleBand(imageType,imgWidth,imgHeight);
		intensity = new ImageFloat32(input.width,input.height);

		Random rand = new Random(234);
		ImageTestingOps.randomize((ImageFloat32)input, rand, 0, 255);
	}

	public class FAST_TABLE_F32 extends PerformerBase {
		ImplFastCorner12_Table_F32 corner = new ImplFastCorner12_Table_F32(50,12);

		@Override
		public void process() {
			corner.process((ImageFloat32)input,intensity);
		}
	}

	public class FAST_F32 extends PerformerBase {
		ImplFastCorner12_F32 corner = new ImplFastCorner12_F32(50,12);

		@Override
		public void process() {
			corner.process((ImageFloat32)input,intensity);
		}
	}

	public void evaluate() {
		System.out.println("=========  Profile Image Size " + imgWidth + " x " + imgHeight + " ==========");
		System.out.println();

		ProfileOperation.printOpsPerSec(new FAST_TABLE_F32(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new FAST_F32(), TEST_TIME);

	}

	public static void main( String args[] ) {
		BenchmarkFeatureIntensity benchmark = new BenchmarkFeatureIntensity(ImageFloat32.class);

		benchmark.evaluate();
	}
}
