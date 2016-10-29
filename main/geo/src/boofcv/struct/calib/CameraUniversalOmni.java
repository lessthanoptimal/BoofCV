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

package boofcv.struct.calib;

/**
 * <p>Camera model for omnidirectional single viewpoint sensors [1].  Designed to work with parabolic,
 * hyperbolic, wide-angle, and spherical sensors.  The FOV that this model can describe is dependent
 * on the mirror parameter &xi;.  See [1] for details, but for example &xi;=0 is a pinhole camera,
 * &xi;=1 can describe fisheye cameras, but a value larger than 1 is limited to 180 degrees due to
 * multiple points on the unit sphere intersecting the same projection line.  This is the same model as
 * {@link CameraPinholeRadial} except that there is a change in reference frame which allows it to model wider FOV.</p>
 *
 * Forward Projection
 * <ol>
 * <li>Given a 3D point X=(x,y,z) in camera (mirror) coordinates</li>
 * <li>Project onto unit sphere X<sub>s</sub>=X/||X||</li>
 * <li>Change reference frame X'=(x',y',z') = (x<sub>s</sub>,y<sub>s</sub>,z<sub>s</sub> + &xi;)</li>
 * <li>Compute normalized image coordinates (u,v)=(x'/z', y'/z')</li>
 * <li>Apply radial and tangential distortion (see below)</li>
 * <li>Convert into pixels p = K*distort([u;v])</li>
 * </ol>
 *
 * <pre>
 * Camera Projection
 *     [ fx  skew cx ]
 * K = [  0   fy  cy ]
 *     [  0    0   1 ]
 * </pre>
 *
 * <p>
 * Radial and Tangential Distortion:<br>
 * x<sub>d</sub> = x<sub>n</sub> + x<sub>n</sub>[k<sub>1</sub> r<sup>2</sup> + ... + k<sub>n</sub> r<sup>2n</sup>]<br>
 * dx<sub>u</sub> = [ 2t<sub>1</sub> u v + t<sub>2</sub>(r<sup>2</sup> + 2u<sup>2</sup>)] <br>
 * dx<sub>v</sub> = [ t<sub>1</sub>(r<sup>2</sup> + 2v<sup>2</sup>) + 2 t<sub>2</sub> u v]<br>
 * <br>
 * r<sup>2</sup> = u<sup>2</sup> + v<sup>2</sup><br>
 * where x<sub>d</sub> is the distorted normalized image coordinates, x<sub>n</sub>=(u,v) is
 * undistorted normalized image coordinates.
 * </p>
 *
 * <p>NOTE: The only difference from [1] is that skew is used instead of fx*alpha.</p>
 * <p>
 * [1] Christopher Mei, and Patrick Rives. "Single view point omnidirectional camera calibration
 *     from planar grids." ICRA 2007.
 * </p>
 * @author Peter Abeles
 */
public class CameraUniversalOmni extends CameraPinhole {
	/** Mirror offset distance. &xi; */
	public double mirrorOffset;

	/** radial distortion parameters: k<sub>1</sub>,...,k<sub>n</sub> */
	public double radial[];
	/** tangential distortion parameters */
	public double t1, t2;

	/**
	 * Constructor for specifying number of radial distortion
	 *
	 * @param numRadial Number of radial distortion parameters
	 */
	public CameraUniversalOmni( int numRadial ) {
		this.radial = new double[ numRadial ];
	}

	/**
	 * Copy constructor
	 * @param original Model which is to be copied
	 */
	public CameraUniversalOmni( CameraUniversalOmni original ) {
		set(original);
	}

	public CameraUniversalOmni fsetMirror( double mirrorOffset ) {
		this.mirrorOffset = mirrorOffset;
		return this;
	}

	public CameraUniversalOmni fsetRadial(double ...radial ) {
		this.radial = radial.clone();
		return this;
	}

	public CameraUniversalOmni fsetTangental(double t1 , double t2) {
		this.t1 = t1;
		this.t2 = t2;
		return this;
	}

	/**
	 * Assigns this model to be identical to the passed in model
	 * @param original Model which is to be copied
	 */
	public void set( CameraUniversalOmni original ) {
		super.set(original);

		this.mirrorOffset = original.mirrorOffset;

		if( radial.length != original.radial.length )
			radial = new double[ original.radial.length ];
		System.arraycopy(original.radial,0,radial,0,radial.length);

		this.t1 = original.t1;
		this.t2 = original.t2;
	}

	public double[] getRadial() {
		return radial;
	}

	public void setRadial(double[] radial) {
		this.radial = radial;
	}

	public double getT1() {
		return t1;
	}

	public void setT1(double t1) {
		this.t1 = t1;
	}

	public double getT2() {
		return t2;
	}

	public void setT2(double t2) {
		this.t2 = t2;
	}

	public double getMirrorOffset() {
		return mirrorOffset;
	}

	public void setMirrorOffset(double mirrorOffset) {
		this.mirrorOffset = mirrorOffset;
	}
}
