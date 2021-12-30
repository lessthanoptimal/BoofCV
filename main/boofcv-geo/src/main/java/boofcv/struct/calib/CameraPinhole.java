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

package boofcv.struct.calib;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.ejml.data.DMatrixRMaj;

/**
 * <p>
 * Intrinsic camera parameters for a pinhole camera. Specifies the calibration
 * matrix K and distortion parameters.
 * </p>
 *
 * <pre>
 *     [ fx skew  cx ]
 * K = [  0   fy  cy ]
 *     [  0    0   1 ]
 * </pre>
 *
 * @author Peter Abeles
 */
@EqualsAndHashCode(callSuper = false)
public class CameraPinhole extends CameraModel {
	/** focal length along x and y axis (units: pixels) */
	@Getter @Setter public double fx, fy;
	/** skew parameter, typically 0 (units: pixels) */
	@Getter @Setter public double skew;
	/** image center (units: pixels) */
	@Getter @Setter public double cx, cy;

	public CameraPinhole() {}

	/** Copy constructor */
	public CameraPinhole( CameraPinhole param ) {
		setTo(param);
	}

	public CameraPinhole( double fx, double fy,
						  double skew,
						  double cx, double cy,
						  int width, int height ) {
		fsetK(fx, fy, skew, cx, cy, width, height);
	}

	/**
	 * Sets all variables to zero.
	 */
	public void reset() {
		fx = fy = skew = cx = cy = 0.0;
		width = height = 0;
	}

	public CameraPinhole fsetK( double fx, double fy,
								double skew,
								double cx, double cy ) {
		this.fx = fx;
		this.fy = fy;
		this.skew = skew;
		this.cx = cx;
		this.cy = cy;

		return this;
	}

	public CameraPinhole fsetK( DMatrixRMaj K ) {
		this.fx = K.unsafe_get(0, 0);
		this.fy = K.unsafe_get(1, 1);
		this.skew = K.unsafe_get(0, 1);
		this.cx = K.unsafe_get(0, 2);
		this.cy = K.unsafe_get(1, 2);

		return this;
	}

	public CameraPinhole fsetK( double fx, double fy,
								double skew,
								double cx, double cy,
								int width, int height ) {
		this.fx = fx;
		this.fy = fy;
		this.skew = skew;
		this.cx = cx;
		this.cy = cy;
		this.width = width;
		this.height = height;

		return this;
	}

	public CameraPinhole fsetShape( int width, int height ) {
		this.width = width;
		this.height = height;
		return this;
	}

	public void setTo( CameraPinhole param ) {
		this.fx = param.fx;
		this.fy = param.fy;
		this.skew = param.skew;
		this.cx = param.cx;
		this.cy = param.cy;
		this.width = param.width;
		this.height = param.height;
	}

	/**
	 * Is the pixel coordinate inside the image. For floating points numbers a pixel is inside the image if it is
	 * less than width or height. Justification for this is if you converted it to an int it would round down and
	 * be inside.
	 *
	 * @param x pixel location x-axis
	 * @param y pixel location y-axis
	 * @return true if inside or false if not
	 */
	public boolean isInside( double x, double y ) {
		return x >= 0 && y >= 0 && x < width && y < height;
	}

	/**
	 * Returns true if the pixel coordinate is inside the image
	 *
	 * @param x pixel location x-axis
	 * @param y pixel location y-axis
	 * @return true if inside or false if not
	 */
	public boolean isInside( int x, int y ) {
		return x >= 0 && y >= 0 && x < width && y < height;
	}

	@Override
	public void print() {
		System.out.println("Shape " + width + " " + height);
		System.out.printf("center %7.2f %7.2f\n", cx, cy);
		System.out.println("fx = " + fx);
		System.out.println("fy = " + fy);
		System.out.println("skew = " + skew);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends CameraModel> T createLike() {
		return (T)new CameraPinhole();
	}

	@Override
	public String toString() {
		return "CameraPinhole{" +
				"fx=" + fx +
				", fy=" + fy +
				", skew=" + skew +
				", cx=" + cx +
				", cy=" + cy +
				", width=" + width +
				", height=" + height +
				'}';
	}

	public boolean isEquals( CameraPinhole param, double tol ) {
		if (Math.abs(fx - param.fx) > tol)
			return false;
		if (Math.abs(fy - param.fy) > tol)
			return false;
		if (Math.abs(skew - param.skew) > tol)
			return false;
		if (Math.abs(cx - param.cx) > tol)
			return false;
		if (Math.abs(cy - param.cy) > tol)
			return false;
		if (width != param.width)
			return false;
		return height == param.height;
	}
}
