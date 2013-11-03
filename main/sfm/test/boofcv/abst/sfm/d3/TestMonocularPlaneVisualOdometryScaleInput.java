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

package boofcv.abst.sfm.d3;

import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.calib.MonoPlaneParameters;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageType;
import georegression.struct.se.Se3_F64;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestMonocularPlaneVisualOdometryScaleInput {

	int width = 640;
	int height = 320;

	MonoPlaneParameters param;
	ImageFloat32 image;
	ImageType<ImageFloat32> type = ImageType.single(ImageFloat32.class);
	boolean result;
	boolean resetCalled = false;


	@Test
	public void setCalibration() {
		param = null;

		IntrinsicParameters intrinsic = createIntrinsic();
		Se3_F64 extrinsic = new Se3_F64();
		extrinsic.T.x=8;
		Dummy dummy = new Dummy();

		MonocularPlaneVisualOdometry<ImageFloat32> alg = new MonocularPlaneVisualOdometryScaleInput<ImageFloat32>(dummy,0.5);
		assertTrue(this.param == null);
		alg.setCalibration(new MonoPlaneParameters(intrinsic,extrinsic));

		assertEquals(320, this.param.intrinsic.width);
		assertEquals(160, this.param.intrinsic.height);
		assertTrue(this.param.planeToCamera.T.x == extrinsic.T.x);
	}

	@Test
	public void process() {
		image = null;

		IntrinsicParameters intrinsic = createIntrinsic();
		Dummy dummy = new Dummy();

		MonocularPlaneVisualOdometry<ImageFloat32> alg = new MonocularPlaneVisualOdometryScaleInput<ImageFloat32>(dummy,0.5);
		alg.setCalibration(new MonoPlaneParameters(intrinsic, new Se3_F64()));

		ImageFloat32 inputImage = new ImageFloat32(width,height);

		alg.process(inputImage);

		assertTrue(inputImage != image);

		assertEquals(320, image.width);
		assertEquals(160, image.height);
	}

	@Test
	public void getImageType() {

		Dummy dummy = new Dummy();

		MonocularPlaneVisualOdometry<ImageFloat32> alg = new MonocularPlaneVisualOdometryScaleInput<ImageFloat32>(dummy,0.5);

		assertTrue(type == alg.getImageType());
	}

	public IntrinsicParameters createIntrinsic() {
		return new IntrinsicParameters(200,201,0,width/2,height/2,width,height, false, new double[]{0,0});
	}

	protected class Dummy implements MonocularPlaneVisualOdometry<ImageFloat32> {

		@Override
		public void setCalibration(MonoPlaneParameters config ) {
			param = config;
		}

		@Override
		public boolean process(ImageFloat32 l) {
			image = l;
			return result;
		}

		@Override
		public ImageType<ImageFloat32> getImageType() {
			return type;
		}

		@Override
		public void reset() {
			resetCalled = true;
		}

		@Override
		public boolean isFault() {
			return false;
		}

		@Override
		public Se3_F64 getCameraToWorld() {
			return new Se3_F64();
		}

	}

}
