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

package boofcv.alg.distort.pinhole;

import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.distort.Point2Transform2_F64;
import georegression.struct.point.Point2D_F64;

/**
 * Converts normalized pixel coordinate into pixel coordinate.
 *
 * @author Peter Abeles
 */
public class PinholeNtoP_F64 implements Point2Transform2_F64 {

	// camera calibration matrix
	private double fx, fy, skew, cx, cy;

	public PinholeNtoP_F64( PinholeNtoP_F64 original ) {
		this.fx = original.fx;
		this.fy = original.fy;
		this.skew = original.skew;
		this.cx = original.cx;
		this.cy = original.cy;
	}

	public PinholeNtoP_F64( CameraPinhole pinhole ) {
		setK(pinhole);
	}

	public PinholeNtoP_F64() {}

	public PinholeNtoP_F64 setK( CameraPinhole pinhole ) {
		return setK(pinhole.fx, pinhole.fy, pinhole.skew, pinhole.cx, pinhole.cy);
	}

	public PinholeNtoP_F64 setK( /**/double fx, /**/double fy, /**/double skew, /**/double cx, /**/double cy ) {
		this.fx = (double)fx;
		this.fy = (double)fy;
		this.skew = (double)skew;
		this.cx = (double)cx;
		this.cy = (double)cy;
		return this;
	}

	@Override
	public void compute( double x, double y, Point2D_F64 out ) {
		out.x = fx*x + skew*y + cx;
		out.y = fy*y + cy;
	}

	@Override
	public PinholeNtoP_F64 copyConcurrent() {
		return new PinholeNtoP_F64(this);
	}
}
