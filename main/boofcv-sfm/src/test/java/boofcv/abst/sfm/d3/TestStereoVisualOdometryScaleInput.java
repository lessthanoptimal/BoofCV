/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.se.Se3_F64;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.io.PrintStream;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestStereoVisualOdometryScaleInput extends BoofStandardJUnit {

	int width = 640;
	int height = 320;

	StereoParameters parameters;
	GrayF32 leftImage;
	GrayF32 rightImage;
	ImageType<GrayF32> type = ImageType.single(GrayF32.class);
	boolean result;
	boolean resetCalled = false;

	@Test void setCalibration() {
		StereoParameters p = createStereoParam();
		Dummy dummy = new Dummy();

		StereoVisualOdometryScaleInput<GrayF32> alg = new StereoVisualOdometryScaleInput<>(dummy, 0.5);
		alg.setCalibration(p);

		assertEquals(320, parameters.left.width);
		assertEquals(320, parameters.right.width);
		assertEquals(160, parameters.left.height);
		assertEquals(160, parameters.right.height);
	}

	@Test void process() {
		StereoParameters p = createStereoParam();
		Dummy dummy = new Dummy();

		StereoVisualOdometryScaleInput<GrayF32> alg = new StereoVisualOdometryScaleInput<>(dummy, 0.5);
		alg.setCalibration(p);

		GrayF32 left = new GrayF32(width, height);
		GrayF32 right = new GrayF32(width, height);

		alg.process(left, right);

		assertNotSame(left, leftImage);
		assertNotSame(right, rightImage);

		assertEquals(320, leftImage.width);
		assertEquals(320, rightImage.width);
		assertEquals(160, leftImage.height);
		assertEquals(160, rightImage.height);
	}

	@Test void getImageType() {
		Dummy dummy = new Dummy();

		StereoVisualOdometryScaleInput<GrayF32> alg = new StereoVisualOdometryScaleInput<>(dummy, 0.5);

		assertSame(type, alg.getImageType());
	}

	public StereoParameters createStereoParam() {
		StereoParameters ret = new StereoParameters();

		ret.setRightToLeft(new Se3_F64());
		ret.getRightToLeft().getT().setTo(-0.2, 0.001, -0.012);
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0.001, -0.01, 0.0023, ret.getRightToLeft().getR());

		ret.left = new CameraPinholeBrown(200, 201, 0, width/2, height/2, width, height).fsetRadial(0, 0);
		ret.right = new CameraPinholeBrown(199, 200, 0, width/2 + 2, height/2 - 6, width, height).fsetRadial(0, 0);

		return ret;
	}

	protected class Dummy implements StereoVisualOdometry<GrayF32> {
		long frameID = -1;

		@Override
		public void setCalibration( StereoParameters p ) {
			parameters = p;
		}

		@Override
		public boolean process( GrayF32 l, GrayF32 r ) {
			frameID++;
			leftImage = l;
			rightImage = r;
			return result;
		}

		@Override
		public ImageType<GrayF32> getImageType() {
			return type;
		}

		@Override
		public void reset() {
			resetCalled = true;
			frameID = -1;
		}

		@Override
		public boolean isFault() {
			return false;
		}

		@Override
		public Se3_F64 getCameraToWorld() {
			return new Se3_F64();
		}

		@Override
		public long getFrameID() { return frameID; }

		@Override
		public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {}
	}
}
