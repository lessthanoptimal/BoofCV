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

package boofcv.alg.distort;

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.BorderType;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.struct.distort.PixelTransform2_F32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import georegression.struct.homography.Homography2D_F32;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class BenchmarkImageDistort<T extends ImageGray> {
	public static final int imgWidth = 640;
	public static final int imgHeight = 480;

	public static final int TEST_TIME = 1000;

	Class<T> imageType;
	
	public T src_F32;
	public T dst_F32;

	public BenchmarkImageDistort(Class<T> imageType) {
		this.imageType = imageType;
		src_F32 = GeneralizedImageOps.createSingleBand(imageType,imgWidth,imgHeight);
		dst_F32 = GeneralizedImageOps.createSingleBand(imageType,imgWidth,imgHeight);
	}

	public class HomographyBilinear_F32 extends PerformerBase {
		ImageDistort<T,T> alg;

		public HomographyBilinear_F32(Homography2D_F32 affine) {
			PixelTransform2_F32 tran = new PixelTransformHomography_F32(affine);
			InterpolatePixelS<T> interp = FactoryInterpolation.bilinearPixelS(imageType, BorderType.EXTENDED);
			
			alg = FactoryDistort.distortSB(false, interp, imageType);
			alg.setModel(tran);
		}

		@Override
		public void process() {
			alg.apply(src_F32, dst_F32);
		}
	}

	public class HomographyBilinearCrop_F32 extends PerformerBase {
		ImageDistort<T,T> alg;

		public HomographyBilinearCrop_F32(Homography2D_F32 affine) {
			PixelTransform2_F32 tran = new PixelTransformHomography_F32(affine);
			InterpolatePixelS<T> interp = FactoryInterpolation.bilinearPixelS(imageType, BorderType.EXTENDED);

			alg = FactoryDistort.distortSB(false, interp, imageType);
			alg.setModel(tran);
		}

		@Override
		public void process() {
			alg.apply(src_F32, dst_F32,0,0,imgWidth,imgHeight);
		}
	}

	public class MapBilinear_F32 extends PerformerBase {
		ImageDistort<T,T> alg;

		public MapBilinear_F32( Homography2D_F32 homography ) {
			PixelTransform2_F32 tran = new PixelTransformHomography_F32(homography);
			InterpolatePixelS<T> interp = FactoryInterpolation.bilinearPixelS(imageType, BorderType.EXTENDED);

			alg = FactoryDistort.distortSB(true, interp, imageType);
			alg.setModel(tran);
		}

		@Override
		public void process() {
			alg.apply(src_F32, dst_F32,0,0,imgWidth,imgHeight);
		}
	}

	private void benchmark() {
		Random rand = new Random(234);

		Homography2D_F32 affine = new Homography2D_F32((float)rand.nextGaussian(),(float)rand.nextGaussian(),
				(float)rand.nextGaussian(),(float)rand.nextGaussian(),(float)rand.nextGaussian(),
				(float)rand.nextGaussian(),(float)rand.nextGaussian(),(float)rand.nextGaussian(),
				(float)rand.nextGaussian());

		System.out.println("=========  Profile Image Size " + imgWidth + " x " + imgHeight + " ==========");
		System.out.println();

		ProfileOperation.printOpsPerSec(new MapBilinear_F32(affine),TEST_TIME);
		ProfileOperation.printOpsPerSec(new HomographyBilinear_F32(affine),TEST_TIME);
		ProfileOperation.printOpsPerSec(new HomographyBilinearCrop_F32(affine),TEST_TIME);

	}


	public static void main( String args[] ) {
		BenchmarkImageDistort benchmark = new BenchmarkImageDistort(GrayF32.class);

		benchmark.benchmark();
	}
}
