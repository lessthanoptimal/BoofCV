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

package boofcv.alg.distort;

import boofcv.alg.distort.impl.DistortSupport;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.core.image.border.BorderType;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.core.image.border.ImageBorder;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.struct.distort.ImageDistort;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.homo.Homography2D_F32;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class BenchmarkImageDistort {
	public static final int imgWidth = 640;
	public static final int imgHeight = 480;

	public static final int TEST_TIME = 1000;

	public static final ImageFloat32 src_F32 = new ImageFloat32(imgWidth,imgHeight);
	public static final ImageFloat32 dst_F32 = new ImageFloat32(imgWidth,imgHeight);


	public static class HomographyBilinear_F32 extends PerformerBase {
		ImageDistort<ImageFloat32> alg;

		public HomographyBilinear_F32(Homography2D_F32 affine) {
			PixelTransform_F32 tran = new PixelTransformHomography_F32(affine);
			InterpolatePixel<ImageFloat32> interp = FactoryInterpolation.bilinearPixel(ImageFloat32.class);
			ImageBorder<ImageFloat32> border = FactoryImageBorder.general(ImageFloat32.class, BorderType.EXTENDED);
			
			alg = DistortSupport.createDistort(ImageFloat32.class,tran,interp,border);
		}

		@Override
		public void process() {
			alg.apply(src_F32, dst_F32);
		}
	}

	public static class HomographyBilinearCrop_F32 extends PerformerBase {
		ImageDistort<ImageFloat32> alg;

		public HomographyBilinearCrop_F32(Homography2D_F32 affine) {
			PixelTransform_F32 tran = new PixelTransformHomography_F32(affine);
			InterpolatePixel<ImageFloat32> interp = FactoryInterpolation.bilinearPixel(ImageFloat32.class);
			ImageBorder<ImageFloat32> border = FactoryImageBorder.general(ImageFloat32.class, BorderType.EXTENDED);

			alg = DistortSupport.createDistort(ImageFloat32.class,tran,interp,border);
		}

		@Override
		public void process() {
			alg.apply(src_F32, dst_F32,0,0,imgWidth,imgHeight);
		}
	}

	public static class MapBilinear_F32 extends PerformerBase {
		ImageDistort<ImageFloat32> alg;

		public MapBilinear_F32( Homography2D_F32 affine ) {
			PixelTransform_F32 tran = new PixelTransformHomography_F32(affine);
			InterpolatePixel<ImageFloat32> interp = FactoryInterpolation.bilinearPixel(ImageFloat32.class);
			ImageBorder<ImageFloat32> border = FactoryImageBorder.general(ImageFloat32.class, BorderType.EXTENDED);

			alg = new ImageDistortMap(imgWidth,imgHeight,interp,border);
			alg.setModel(tran);
		}

		@Override
		public void process() {
			alg.apply(src_F32, dst_F32,0,0,imgWidth,imgHeight);
		}
	}


	public static void main( String args[] ) {
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
}
