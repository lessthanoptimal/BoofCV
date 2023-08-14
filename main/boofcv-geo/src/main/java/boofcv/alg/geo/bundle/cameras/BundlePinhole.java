/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.bundle.cameras;

import boofcv.abst.geo.bundle.BundleAdjustmentCamera;
import boofcv.struct.calib.CameraPinhole;
import georegression.struct.point.Point2D_F64;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

import static boofcv.misc.BoofMiscOps.getOrThrow;

/**
 * Formulas for {@link boofcv.struct.calib.CameraPinhole}.
 *
 * @author Peter Abeles
 */
public class BundlePinhole implements BundleAdjustmentCamera {

	// parameters for the camera model
	public boolean zeroSkew = true;
	public double fx, fy, skew, cx, cy;

	public BundlePinhole( boolean zeroSkew ) {
		this.zeroSkew = zeroSkew;
	}

	public BundlePinhole() {}

	public BundlePinhole setTo( CameraPinhole src ) {
		setK(src.fx, src.fy, src.skew, src.cx, src.cy);
		return this;
	}

	public BundlePinhole setK( double fx, double fy, double skew, double cx, double cy ) {
		this.fx = fx;
		this.fy = fy;
		this.skew = skew;
		this.cx = cx;
		this.cy = cy;
		this.zeroSkew = skew == 0.0;
		return this;
	}

	@Override
	public void setIntrinsic( double[] parameters, int offset ) {
		fx = parameters[offset];
		fy = parameters[offset + 1];
		cx = parameters[offset + 2];
		cy = parameters[offset + 3];

		if (!zeroSkew) {
			skew = parameters[offset + 4];
		} else {
			skew = 0;
		}
	}

	@Override
	public void getIntrinsic( double[] parameters, int offset ) {
		parameters[offset] = fx;
		parameters[offset + 1] = fy;
		parameters[offset + 2] = cx;
		parameters[offset + 3] = cy;
		if (!zeroSkew) {
			parameters[offset + 4] = skew;
		}
	}

	@Override
	public void project( double camX, double camY, double camZ, Point2D_F64 output ) {
		double nx = camX/camZ;
		double ny = camY/camZ;

		output.x = fx*nx + skew*ny + cx;
		output.y = fy*ny + cy;
	}

	@Override
	public void jacobian( double camX, double camY, double camZ,
						  double[] inputX, double[] inputY, boolean computeIntrinsic,
						  @Nullable double[] calibX, @Nullable double[] calibY ) {
		double nx = camX/camZ;
		double ny = camY/camZ;

		inputX[0] = fx/camZ;
		inputY[0] = 0;
		inputX[1] = skew/camZ;
		inputY[1] = fy/camZ;
		inputX[2] = -(fx*camX + skew*camY)/(camZ*camZ);
		inputY[2] = -fy*camY/(camZ*camZ);

		if (!computeIntrinsic || calibX == null || calibY == null)
			return;

		calibX[0] = nx;
		calibY[0] = 0;
		calibX[1] = 0;
		calibY[1] = ny;
		calibX[2] = 1;
		calibY[2] = 0;
		calibX[3] = 0;
		calibY[3] = 1;
		if (!zeroSkew) {
			calibX[4] = ny;
			calibY[4] = 0;
		}
	}

	@Override
	public int getIntrinsicCount() {
		return zeroSkew ? 4 : 5;
	}

	@Override public BundleAdjustmentCamera setTo( Map<String, Object> map ) {
		try {
			fx = getOrThrow(map, "fx");
			fy = getOrThrow(map, "fy");
			skew = (double)map.getOrDefault("skew", 0.0);
			cx = getOrThrow(map, "cx");
			cy = getOrThrow(map, "cy");
			return this;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override public Map<String, Object> toMap() {
		var map = new HashMap<String, Object>();
		map.put("fx", fx);
		map.put("fy", fy);
		map.put("skew", skew);
		map.put("cx", cx);
		map.put("cy", cy);
		return map;
	}

	@Override
	public String toString() {
		return "BundlePinhole{" +
				"fx=" + fx +
				", fy=" + fy +
				", skew=" + skew +
				", cx=" + cx +
				", cy=" + cy +
				'}';
	}
}
