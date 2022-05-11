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

package boofcv.alg.distort;

import boofcv.alg.distort.pinhole.PinholePtoN_F64;
import boofcv.struct.distort.Point2Transform2_F64;
import georegression.struct.point.Point2D_F64;

/**
 * Applies a transform which outputs pixel coordinates, which is then converted into normalized image coordinates
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init", "Duplicates"})
public class Transform2PixelThenNorm_F64 implements Point2Transform2_F64 {
	Point2Transform2_F64 first;
	PinholePtoN_F64 p_to_n = new PinholePtoN_F64();

	public Transform2PixelThenNorm_F64( Point2Transform2_F64 first ) {
		this.first = first;
	}

	Transform2PixelThenNorm_F64() {}

	public Point2Transform2_F64 set( /**/double fx, /**/double fy, /**/double skew, /**/double cx, /**/double cy ) {
		p_to_n.setK(fx, fy, skew, cx, cy);
		return this;
	}

	@Override
	public void compute( double x, double y, Point2D_F64 out ) {
		first.compute(x, y, out);
		p_to_n.compute(out.x, out.y, out);
	}

	@Override
	public Transform2PixelThenNorm_F64 copyConcurrent() {
		var ret = new Transform2PixelThenNorm_F64();
		ret.first = this.first.copyConcurrent();
		ret.p_to_n.a11 = p_to_n.a11;
		ret.p_to_n.a12 = p_to_n.a12;
		ret.p_to_n.a13 = p_to_n.a13;
		ret.p_to_n.a22 = p_to_n.a22;
		ret.p_to_n.a23 = p_to_n.a23;
		return ret;
	}
}
