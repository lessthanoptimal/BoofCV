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

package boofcv.alg.feature.peak;

import boofcv.alg.feature.detect.peak.MeanShiftPeak;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.weights.WeightPixelGaussian_F32;
import boofcv.alg.weights.WeightPixel_F32;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F32;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Peter Abeles
 */
public class BenchmarkPeakFinding<T extends ImageGray> {

	Random rand = new Random(234);
	long TEST_TIME = 2000;

	int width = 320;
	int height = 240;

	T image;
	Class<T> imageType;
	List<Point2D_F32> locations = new ArrayList<>();

	public BenchmarkPeakFinding( Class<T> imageType ) {
		this.imageType = imageType;
		image = GeneralizedImageOps.createSingleBand(imageType,width,height);
		GImageMiscOps.fillUniform(image, rand, 0, 200);

		for( int i = 0; i < 3000; i++ ) {
			Point2D_F32 p = new Point2D_F32();
			p.x = rand.nextFloat()*width;
			p.y = rand.nextFloat()*height;
			locations.add(p);
		}
	}

	public class MeanShiftGaussian extends PerformerBase {

		MeanShiftPeak alg;

		public MeanShiftGaussian( int radius ) {
			WeightPixel_F32 weight = new WeightPixelGaussian_F32();
			this.alg = new MeanShiftPeak(30,0.1f,weight,imageType);
			alg.setRadius(radius);
		}

		@Override
		public void process() {
			alg.setImage(image);
			for( int i = 0; i < locations.size(); i++ ) {
				Point2D_F32 p = locations.get(i);
				alg.search(p.x,p.y);
			}
		}
	}

	public class MeanShiftUniform extends PerformerBase {

		MeanShiftPeak alg;

		public MeanShiftUniform( int radius ) {
			WeightPixel_F32 weight = new WeightPixelGaussian_F32();
			this.alg = new MeanShiftPeak(30,0.1f,weight,imageType);
			alg.setRadius(radius);
		}

		@Override
		public void process() {
			alg.setImage(image);
			for( int i = 0; i < locations.size(); i++ ) {
				Point2D_F32 p = locations.get(i);
				alg.search(p.x,p.y);
			}
		}
	}

	public void evaluateAll() {
		System.out.println("=========  Profile Image Size " + width + " x " + height + " ========== "+imageType.getSimpleName());
		System.out.println();

		int radius = 2;

		ProfileOperation.printOpsPerSec(new MeanShiftGaussian(radius), TEST_TIME);
		ProfileOperation.printOpsPerSec(new MeanShiftUniform(radius), TEST_TIME);
	}

	public static void main( String args[] ) {
		BenchmarkPeakFinding<GrayU8>
				benchmark_U8 = new BenchmarkPeakFinding<>(GrayU8.class);

		benchmark_U8.evaluateAll();

		BenchmarkPeakFinding<GrayF32>
				benchmark_F32 = new BenchmarkPeakFinding<>(GrayF32.class);

		benchmark_F32.evaluateAll();
	}
}
