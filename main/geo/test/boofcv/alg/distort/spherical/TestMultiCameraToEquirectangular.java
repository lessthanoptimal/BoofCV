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

package boofcv.alg.distort.spherical;

import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.LensDistortionWideFOV;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.border.BorderType;
import boofcv.factory.distort.FactoryDistort;
import boofcv.struct.distort.Point2Transform3_F32;
import boofcv.struct.distort.Point2Transform3_F64;
import boofcv.struct.distort.Point3Transform2_F32;
import boofcv.struct.distort.Point3Transform2_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.metric.UtilAngle;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point3D_F32;
import georegression.struct.se.Se3_F64;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Unit tests below handle the following:
 * 1) masking due to an explicit mask
 * 2) masking due to non-invertible function
 *
 * Do not check the following:
 * 1) Return of NaN in forward or reverse direction
 * 2) Transform goes outside of the input image
 *
 * @author Peter Abeles
 */
public class TestMultiCameraToEquirectangular {

	private int inputHeight = 150;
	private int inputWidth = 200;

	private int equiHeight = 200;
	private int equiWidth = 250;

	@Test
	public void all() {
		MultiCameraToEquirectangular<GrayF32> alg = createAlgorithm();

		alg.addCamera(new Se3_F64(),new HelperDistortion(),inputWidth,inputHeight);

		Se3_F64 cam2_to_1 = new Se3_F64();
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,Math.PI,0,0,cam2_to_1.R);
		alg.addCamera(cam2_to_1,new HelperDistortion(),inputWidth,inputHeight);

		GrayF32 image0 = new GrayF32(inputWidth,inputHeight);
		GrayF32 image1 = new GrayF32(inputWidth,inputHeight);

		ImageMiscOps.fill(image0,200);
		ImageMiscOps.fill(image1,100);

		List<GrayF32> images = new ArrayList<>();
		images.add(image0);
		images.add(image1);

		alg.render(images);

		GrayF32 found = alg.getRenderedImage();

		double totalError = 0;
		for (int y = 0; y < equiHeight; y++) {
			for (int x = 0; x < equiWidth; x++) {
				if( y < equiHeight/2 ) {
					totalError += Math.abs(200-found.get(x,y));
				} else {
					totalError += Math.abs(100-found.get(x,y));
				}
			}
		}

		double errorFraction = totalError/(equiWidth*equiHeight*100);
		assertTrue(errorFraction<0.05);
	}

	@Test
	public void addCamera_implicit_mask() {
		MultiCameraToEquirectangular<GrayF32> alg = createAlgorithm();

		alg.addCamera(new Se3_F64(),new HelperDistortion(),inputWidth,inputHeight);

		MultiCameraToEquirectangular.Camera c = alg.cameras.get(0);

		// should be masked off by the passed in mask and because values are repeated
		int correct = 0;

		for (int y = 0; y < inputHeight; y++) {
			for (int x = 0; x < inputWidth; x++) {

				if( y<inputHeight/2 && c.mask.get(x,y) > 0 ) {
					correct++;
				}
			}
		}

		double found = Math.abs(1.0 - correct/(inputWidth*inputHeight/2.0));
		assertTrue(found <= 0.05 );
	}

	@Test
	public void addCamera_explicit_mask() {
		MultiCameraToEquirectangular<GrayF32> alg = createAlgorithm();

		// mask out the right part of the image
		GrayU8 mask = new GrayU8(inputWidth,inputHeight);
		for (int y = 0; y < inputHeight; y++) {
			for (int x = 0; x < inputWidth/2; x++) {
				mask.set(x,y,1);
			}
		}

		alg.addCamera(new Se3_F64(),new HelperDistortion(),mask);

		MultiCameraToEquirectangular.Camera c = alg.cameras.get(0);

		// should be masked off by the passed in mask and because values are repeated
		int correct = 0;

		for (int y = 0; y < inputHeight; y++) {
			for (int x = 0; x < inputWidth; x++) {
				boolean valid = y<inputHeight/2 && x < inputWidth/2;

				if( valid && c.mask.get(x,y) > 0 ) {
					correct++;
				}
			}
		}

		double found = Math.abs(1.0 - correct/(inputWidth*inputHeight/4.0));
		assertTrue(found <= 0.05 );
	}

	private MultiCameraToEquirectangular<GrayF32> createAlgorithm() {
		ImageType<GrayF32> imageType = ImageType.single(GrayF32.class);
		ImageDistort<GrayF32,GrayF32> distort = FactoryDistort.
				distort(false, InterpolationType.BILINEAR, BorderType.ZERO,imageType,imageType);
		MultiCameraToEquirectangular<GrayF32> alg = new MultiCameraToEquirectangular<>(distort,
				equiWidth, equiHeight, imageType);
		alg.setMaskToleranceAngle(UtilAngle.radian(2)); // increase tolerance due to resolution
		return alg;
	}

	private class HelperDistortion implements LensDistortionWideFOV {

		@Override
		public Point3Transform2_F64 distortStoP_F64() {return null;}

		@Override
		public Point3Transform2_F32 distortStoP_F32() {
			return new HelperTransform();
		}

		@Override
		public Point2Transform3_F64 undistortPtoS_F64() {return null;}

		@Override
		public Point2Transform3_F32 undistortPtoS_F32() {
			return new HelperTransform();
		}
	}

	/**
	 * Transform where multiple pixels in input image map to the same value
	 */
	private class HelperTransform implements Point3Transform2_F32, Point2Transform3_F32
	{
		EquirectangularTools_F32 tools = new EquirectangularTools_F32();

		public HelperTransform() {
			this.tools.configure(inputWidth, inputHeight);
		}

		@Override
		public void compute(float x, float y, Point3D_F32 out) {
			tools.equiToNormFV(x,y,out);
		}

		@Override
		public void compute(float x, float y, float z, Point2D_F32 out) {
			// multiple normal angles map to the same pixel.  This will effectively make the top and bottom
			// half of the image map to the same pixels.  This should cause it to get masked out
			tools.normToEquiFV(x,y,-Math.abs(z),out);
		}
	}
}
