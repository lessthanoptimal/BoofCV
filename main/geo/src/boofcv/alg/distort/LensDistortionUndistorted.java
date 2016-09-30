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

import boofcv.struct.calib.PinholeIntrinsic;
import boofcv.struct.distort.DoNothingTransform_F32;
import boofcv.struct.distort.DoNothingTransform_F64;
import boofcv.struct.distort.PointTransform_F32;
import boofcv.struct.distort.PointTransform_F64;

/**
 * Projection when there is no lens distortion
 *
 * @author Peter Abeles
 */
public class LensDistortionUndistorted implements LensDistortionPinhole{

	PinholeIntrinsic<?> p;

	public LensDistortionUndistorted(PinholeIntrinsic<?> p) {
		this.p = p;
	}

	@Override
	public PointTransform_F64 distort_F64(boolean pixelIn, boolean pixelOut) {
		if( pixelIn ) {
			if( pixelOut ) {
				return new DoNothingTransform_F64();
			} else {
				PixelToNormalized_F64 p2n = new PixelToNormalized_F64();
				p2n.set(p.fx,p.fy,p.skew,p.cx,p.cy);
				return p2n;
			}
		} else {
			if( pixelOut ) {
				NormalizedToPixel_F64 n2p = new NormalizedToPixel_F64();
				n2p.set(p.fx, p.fy, p.skew, p.cx, p.cy);
				return n2p;
			} else {
				return new DoNothingTransform_F64();
			}
		}
	}

	@Override
	public PointTransform_F64 undistort_F64(boolean pixelIn, boolean pixelOut) {
		return distort_F64(pixelIn,pixelOut);
	}

	@Override
	public PointTransform_F32 distort_F32(boolean pixelIn, boolean pixelOut) {
		if( pixelIn ) {
			if( pixelOut ) {
				return new DoNothingTransform_F32();
			} else {
				PixelToNormalized_F32 p2n = new PixelToNormalized_F32();
				p2n.set(p.fx,p.fy,p.skew,p.cx,p.cy);
				return p2n;
			}
		} else {
			if( pixelOut ) {
				NormalizedToPixel_F32 n2p = new NormalizedToPixel_F32();
				n2p.set(p.fx, p.fy, p.skew, p.cx, p.cy);
				return n2p;
			} else {
				return new DoNothingTransform_F32();
			}
		}
	}

	@Override
	public PointTransform_F32 undistort_F32(boolean pixelIn, boolean pixelOut) {
		return distort_F32(pixelIn,pixelOut);
	}
}
