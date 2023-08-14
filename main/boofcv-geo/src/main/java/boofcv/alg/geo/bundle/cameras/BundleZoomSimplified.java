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
import boofcv.abst.geo.bundle.BundleCameraState;
import georegression.struct.point.Point2D_F64;
import org.ejml.FancyPrint;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

import static boofcv.misc.BoofMiscOps.getOrThrow;

/**
 * A simplified camera model that assumes the camera's zoom is known as part of the camera state
 *
 * @author Peter Abeles
 */
public class BundleZoomSimplified implements BundleAdjustmentCamera {
	public final static String TYPE_NAME = "ZoomSimplified";

	// Offset for focal length
	public double fOff = 0;
	// Scale for focal length
	public double fScale = 1.0;

	// radial distortion parameters
	public double k1, k2;

	// Current state of the camera's zoom
	private double zoom = Double.NaN;

	public BundleZoomSimplified() {}

	public BundleZoomSimplified( double fOff, double fScale, double k1, double k2 ) {
		this.fOff = fOff;
		this.fScale = fScale;
		this.k1 = k1;
		this.k2 = k2;
	}

	@Override
	public void setIntrinsic( double[] parameters, int offset ) {
		fOff = parameters[offset];
		fScale = parameters[offset + 1];
		k1 = parameters[offset + 2];
		k2 = parameters[offset + 3];
	}

	@Override
	public void getIntrinsic( double[] parameters, int offset ) {
		parameters[offset] = fOff;
		parameters[offset + 1] = fScale;
		parameters[offset + 2] = k1;
		parameters[offset + 3] = k2;
	}

	@Override public void setCameraState( BundleCameraState state ) {
		this.zoom = ((BundleZoomState)state).zoom;
	}

	@Override
	public void project( double camX, double camY, double camZ, Point2D_F64 output ) {
		double focalLength = fOff + zoom*fScale;
		double normX = camX/camZ;
		double normY = camY/camZ;

		double n2 = normX*normX + normY*normY;

		double r = 1.0 + (k1 + k2*n2)*n2;

		output.x = focalLength*r*normX;
		output.y = focalLength*r*normY;
	}

	@Override
	public void jacobian( double X, double Y, double Z,
						  double[] inputX, double[] inputY, boolean computeIntrinsic,
						  @Nullable double[] calibX, @Nullable double[] calibY ) {
		double focalLength = fOff + zoom*fScale;
		double normX = X/Z;
		double normY = Y/Z;

		double n2 = normX*normX + normY*normY;

		double n2_X = 2*normX/Z;
		double n2_Y = 2*normY/Z;
		double n2_Z = -2*n2/Z;

		double r = 1.0 + (k1 + k2*n2)*n2;
		double kk = k1 + 2*k2*n2;

		double r_Z = n2_Z*kk;

		// partial X
		inputX[0] = (focalLength/Z)*(r + 2*normX*normX*kk);
		inputY[0] = focalLength*normY*n2_X*kk;

		// partial Y
		inputX[1] = focalLength*normX*n2_Y*kk;
		inputY[1] = (focalLength/Z)*(r + 2*normY*normY*kk);

		// partial Z
		inputX[2] = focalLength*normX*(r_Z - r/Z);
		inputY[2] = focalLength*normY*(r_Z - r/Z);

		if (!computeIntrinsic || calibX == null || calibY == null)
			return;

		// partial f-off
		calibX[0] = r*normX;
		calibY[0] = r*normY;

		// partial f-scale
		calibX[1] = zoom*r*normX;
		calibY[1] = zoom*r*normY;

		// partial k1
		calibX[2] = focalLength*normX*n2;
		calibY[2] = focalLength*normY*n2;

		// partial k2
		calibX[3] = focalLength*normX*n2*n2;
		calibY[3] = focalLength*normY*n2*n2;
	}

	@Override
	public int getIntrinsicCount() {
		return 4;
	}

	@Override public BundleAdjustmentCamera setTo( Map<String, Object> src ) {
		try {
			fOff = getOrThrow(src, "f-off");
			fScale = getOrThrow(src, "f-scale");
			k1 = getOrThrow(src, "k1");
			k2 = getOrThrow(src, "k2");
			return this;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override public Map<String, Object> toMap() {
		var map = new HashMap<String, Object>();
		map.put("type", TYPE_NAME);
		map.put("f-off", fOff);
		map.put("f-scale", fScale);
		map.put("k1", k1);
		map.put("k2", k2);
		return map;
	}

	@Override
	public String toString() {
		FancyPrint fp = new FancyPrint();

		return "BundleZoomSimplified{" +
				"off=" + fp.s(fOff) +
				", scale=" + fp.s(fScale) +
				", k1=" + fp.s(k1) +
				", k2=" + fp.s(k2) +
				'}';
	}

	public void reset() {
		k1 = k2 = fOff = 0.0;
		fScale = 1.0;
		zoom = Double.NaN;
	}

	public BundleZoomSimplified setTo( BundleZoomSimplified c ) {
		this.fOff = c.fOff;
		this.fScale = c.fScale;
		this.k1 = c.k1;
		this.k2 = c.k2;
		this.zoom = c.zoom;

		return this;
	}

	public BundleZoomSimplified copy() {
		return new BundleZoomSimplified().setTo(this);
	}

	public boolean isIdentical( BundleZoomSimplified c, double tol ) {
		if (Math.abs(fOff - c.fOff) > tol)
			return false;
		if (Math.abs(fScale - c.fScale) > tol)
			return false;
		if (Math.abs(k1 - c.k1) > tol)
			return false;
		if (Math.abs(k2 - c.k2) > tol)
			return false;
		return true;
	}
}
