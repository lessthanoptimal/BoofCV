/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.distort.radtan.*;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.distort.PointTransform_F32;
import boofcv.struct.distort.PointTransform_F64;

/**
 * Radial-Tangential lens distortion model point transforms.
 *
 * @author Peter Abeles
 */
public class LensDistortionRadialTangential implements LensDistortionPinhole{

	IntrinsicParameters p;

	public LensDistortionRadialTangential(IntrinsicParameters p) {
		this.p = p;
	}

	@Override
	public PointTransform_F64 distort_F64(boolean pixelIn, boolean pixelOut) {
		if( pixelIn ) {
			if( pixelOut ) {
				PointTransform_F64 p_to_n = new AddRadialPtoN_F64().setK(p.fx,p.fy,p.skew,p.cx,p.cy).setDistortion(p.radial, p.t1, p.t2);
				return new TransformThenPixel_F64(p_to_n).set(p.fx, p.fy, p.skew, p.cx, p.cy);
			} else {
				return new AddRadialPtoN_F64().setK(p.fx,p.fy,p.skew,p.cx,p.cy).setDistortion(p.radial,p.t1,p.t2);
			}
		} else {
			if( pixelOut ) {
				AddRadialNtoN_F64 n_to_n = new AddRadialNtoN_F64().setDistortion(p.radial, p.t1, p.t2);
				return new TransformThenPixel_F64(n_to_n).set(p.fx, p.fy, p.skew, p.cx, p.cy);
			} else {
				return new AddRadialNtoN_F64().setDistortion(p.radial, p.t1, p.t2);
			}
		}
	}

	@Override
	public PointTransform_F64 undistort_F64(boolean pixelIn, boolean pixelOut) {
		if( pixelIn ) {
			if( pixelOut ) {
				PointTransform_F64 p_to_n = new RemoveRadialPtoN_F64().setK(p.fx,p.fy,p.skew,p.cx,p.cy).setDistortion(p.radial, p.t1, p.t2);
				return new TransformThenPixel_F64(p_to_n).set(p.fx, p.fy, p.skew, p.cx, p.cy);
			} else {
				return new RemoveRadialPtoN_F64().setK(p.fx,p.fy,p.skew,p.cx,p.cy).setDistortion(p.radial,p.t1,p.t2);
			}
		} else {
			if( pixelOut ) {
				RemoveRadialNtoN_F64 n_to_n = new RemoveRadialNtoN_F64().setDistortion(p.radial, p.t1, p.t2);
				return new TransformThenPixel_F64(n_to_n).set(p.fx, p.fy, p.skew, p.cx, p.cy);
			} else {
				return new RemoveRadialNtoN_F64().setDistortion(p.radial, p.t1, p.t2);
			}
		}
	}

	@Override
	public PointTransform_F32 distort_F32(boolean pixelIn, boolean pixelOut) {
		if( pixelIn ) {
			if( pixelOut ) {
				PointTransform_F32 p_to_n = new AddRadialPtoN_F32().setK(p.fx,p.fy,p.skew,p.cx,p.cy).setDistortion(p.radial, p.t1, p.t2);
				return new TransformThenPixel_F32(p_to_n).set(p.fx, p.fy, p.skew, p.cx, p.cy);
			} else {
				return new AddRadialPtoN_F32().setK(p.fx,p.fy,p.skew,p.cx,p.cy).setDistortion(p.radial,p.t1,p.t2);
			}
		} else {
			if( pixelOut ) {
				AddRadialNtoN_F32 n_to_n = new AddRadialNtoN_F32().setDistortion(p.radial, p.t1, p.t2);
				return new TransformThenPixel_F32(n_to_n).set(p.fx, p.fy, p.skew, p.cx, p.cy);
			} else {
				return new AddRadialNtoN_F32().setDistortion(p.radial, p.t1, p.t2);
			}
		}
	}

	@Override
	public PointTransform_F32 undistort_F32(boolean pixelIn, boolean pixelOut) {
		if( pixelIn ) {
			if( pixelOut ) {
				PointTransform_F32 p_to_n = new RemoveRadialPtoN_F32().setK(p.fx,p.fy,p.skew,p.cx,p.cy).setDistortion(p.radial, p.t1, p.t2);
				return new TransformThenPixel_F32(p_to_n).set(p.fx, p.fy, p.skew, p.cx, p.cy);
			} else {
				return new RemoveRadialPtoN_F32().setK(p.fx,p.fy,p.skew,p.cx,p.cy).setDistortion(p.radial,p.t1,p.t2);
			}
		} else {
			if( pixelOut ) {
				RemoveRadialNtoN_F32 n_to_n = new RemoveRadialNtoN_F32().setDistortion(p.radial, p.t1, p.t2);
				return new TransformThenPixel_F32(n_to_n).set(p.fx, p.fy, p.skew, p.cx, p.cy);
			} else {
				return new RemoveRadialNtoN_F32().setDistortion(p.radial, p.t1, p.t2);
			}
		}
	}
}
