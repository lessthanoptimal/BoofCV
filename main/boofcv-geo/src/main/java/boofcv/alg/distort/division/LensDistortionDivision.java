/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.distort.division;

import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.alg.distort.pinhole.PinholeNtoP_F32;
import boofcv.alg.distort.pinhole.PinholeNtoP_F64;
import boofcv.alg.distort.pinhole.PinholePtoN_F32;
import boofcv.alg.distort.pinhole.PinholePtoN_F64;
import boofcv.struct.calib.CameraDivision;
import boofcv.struct.distort.Point2Transform2_F32;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.distort.SequencePoint2Transform2_F32;
import boofcv.struct.distort.SequencePoint2Transform2_F64;

/**
 * {@link CameraDivision Division} lens distortion model point transforms.
 *
 * @author Peter Abeles
 */
public class LensDistortionDivision implements LensDistortionNarrowFOV {

	CameraDivision p;

	public LensDistortionDivision( CameraDivision p ) {
		this.p = p;
	}

	@Override
	public Point2Transform2_F64 distort_F64( boolean pixelIn, boolean pixelOut ) {
		Point2Transform2_F64 n_to_n = new AddDivisionNtoN_F64().setRadial(p.radial);
		if (pixelIn) {
			Point2Transform2_F64 p_to_n = new PinholePtoN_F64().setK(p.fx, p.fy, p.skew, p.cx, p.cy);
			if (pixelOut) {
				PinholeNtoP_F64 n_to_p = new PinholeNtoP_F64().setK(p.fx, p.fy, p.skew, p.cx, p.cy);
				return new SequencePoint2Transform2_F64(p_to_n, n_to_n, n_to_p);
			} else {
				return new SequencePoint2Transform2_F64(p_to_n, n_to_n);
			}
		} else {
			if (pixelOut) {
				PinholeNtoP_F64 n_to_p = new PinholeNtoP_F64().setK(p.fx, p.fy, p.skew, p.cx, p.cy);
				return new SequencePoint2Transform2_F64(n_to_n, n_to_p);
			} else {
				return n_to_n;
			}
		}
	}

	@Override
	public Point2Transform2_F64 undistort_F64( boolean pixelIn, boolean pixelOut ) {
		Point2Transform2_F64 n_to_n = new RemoveDivisionNtoN_F64().setRadial(p.radial);
		if (pixelIn) {
			Point2Transform2_F64 p_to_n = new PinholePtoN_F64().setK(p.fx, p.fy, p.skew, p.cx, p.cy);
			if (pixelOut) {
				PinholeNtoP_F64 n_to_p = new PinholeNtoP_F64().setK(p.fx, p.fy, p.skew, p.cx, p.cy);
				return new SequencePoint2Transform2_F64(p_to_n, n_to_n, n_to_p);
			} else {
				return new SequencePoint2Transform2_F64(p_to_n, n_to_n);
			}
		} else {
			if (pixelOut) {
				PinholeNtoP_F64 n_to_p = new PinholeNtoP_F64().setK(p.fx, p.fy, p.skew, p.cx, p.cy);
				return new SequencePoint2Transform2_F64(n_to_n, n_to_p);
			} else {
				return n_to_n;
			}
		}
	}

	@Override
	public Point2Transform2_F32 distort_F32( boolean pixelIn, boolean pixelOut ) {
		Point2Transform2_F32 n_to_n = new AddDivisionNtoN_F32().setRadial((float)p.radial);
		if (pixelIn) {
			Point2Transform2_F32 p_to_n = new PinholePtoN_F32().setK(p.fx, p.fy, p.skew, p.cx, p.cy);
			if (pixelOut) {
				PinholeNtoP_F32 n_to_p = new PinholeNtoP_F32().setK(p.fx, p.fy, p.skew, p.cx, p.cy);
				return new SequencePoint2Transform2_F32(p_to_n, n_to_n, n_to_p);
			} else {
				return new SequencePoint2Transform2_F32(p_to_n, n_to_n);
			}
		} else {
			if (pixelOut) {
				PinholeNtoP_F32 n_to_p = new PinholeNtoP_F32().setK(p.fx, p.fy, p.skew, p.cx, p.cy);
				return new SequencePoint2Transform2_F32(n_to_n, n_to_p);
			} else {
				return n_to_n;
			}
		}
	}

	@Override
	public Point2Transform2_F32 undistort_F32( boolean pixelIn, boolean pixelOut ) {
		Point2Transform2_F32 n_to_n = new RemoveDivisionNtoN_F32().setRadial((float)p.radial);
		if (pixelIn) {
			Point2Transform2_F32 p_to_n = new PinholePtoN_F32().setK(p.fx, p.fy, p.skew, p.cx, p.cy);
			if (pixelOut) {
				PinholeNtoP_F32 n_to_p = new PinholeNtoP_F32().setK(p.fx, p.fy, p.skew, p.cx, p.cy);
				return new SequencePoint2Transform2_F32(p_to_n, n_to_n, n_to_p);
			} else {
				return new SequencePoint2Transform2_F32(p_to_n, n_to_n);
			}
		} else {
			if (pixelOut) {
				PinholeNtoP_F32 n_to_p = new PinholeNtoP_F32().setK(p.fx, p.fy, p.skew, p.cx, p.cy);
				return new SequencePoint2Transform2_F32(n_to_n, n_to_p);
			} else {
				return n_to_n;
			}
		}
	}

	@Override
	public Point2Transform2_F32 normalized_F32() {
		return new PinholePtoN_F32().setK(p.fx, p.fy, p.skew, p.cx, p.cy);
	}

	@Override
	public Point2Transform2_F64 normalized_F64() {
		return new PinholePtoN_F64().setK(p.fx, p.fy, p.skew, p.cx, p.cy);
	}

	public CameraDivision getIntrinsic() {
		return p;
	}
}
