/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.distort.pinhole;

import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.distort.DoNothing2Transform2_F32;
import boofcv.struct.distort.DoNothing2Transform2_F64;
import boofcv.struct.distort.Point2Transform2_F32;
import boofcv.struct.distort.Point2Transform2_F64;

/**
 * Projection when there is no lens distortion
 *
 * @author Peter Abeles
 */
public class LensDistortionPinhole implements LensDistortionNarrowFOV {

	CameraPinhole p;

	public LensDistortionPinhole( CameraPinhole p ) {
		this.p = p;
	}

	@Override
	public Point2Transform2_F64 distort_F64( boolean pixelIn, boolean pixelOut ) {
		if (pixelIn) {
			if (pixelOut) {
				return new DoNothing2Transform2_F64();
			} else {
				return new PinholePtoN_F64().setK(p.fx, p.fy, p.skew, p.cx, p.cy);
			}
		} else {
			if (pixelOut) {
				return new PinholeNtoP_F64().setK(p.fx, p.fy, p.skew, p.cx, p.cy);
			} else {
				return new DoNothing2Transform2_F64();
			}
		}
	}

	@Override
	public Point2Transform2_F64 undistort_F64( boolean pixelIn, boolean pixelOut ) {
		return distort_F64(pixelIn, pixelOut);
	}

	@Override
	public Point2Transform2_F32 distort_F32( boolean pixelIn, boolean pixelOut ) {
		if (pixelIn) {
			if (pixelOut) {
				return new DoNothing2Transform2_F32();
			} else {
				return new PinholePtoN_F32().setK(p.fx, p.fy, p.skew, p.cx, p.cy);
			}
		} else {
			if (pixelOut) {
				return new PinholeNtoP_F32().setK(p.fx, p.fy, p.skew, p.cx, p.cy);
			} else {
				return new DoNothing2Transform2_F32();
			}
		}
	}

	@Override
	public Point2Transform2_F32 undistort_F32( boolean pixelIn, boolean pixelOut ) {
		return distort_F32(pixelIn, pixelOut);
	}

	@Override
	public Point2Transform2_F32 normalized_F32() {
		return new PinholePtoN_F32().setK(p.fx, p.fy, p.skew, p.cx, p.cy);
	}

	@Override
	public Point2Transform2_F64 normalized_F64() {
		return new PinholePtoN_F64().setK(p.fx, p.fy, p.skew, p.cx, p.cy);
	}

	public CameraPinhole getIntrinsic() {
		return p;
	}
}
