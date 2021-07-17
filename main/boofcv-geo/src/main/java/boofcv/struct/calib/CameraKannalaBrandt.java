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

import boofcv.misc.BoofMiscOps;
import lombok.Getter;
import lombok.Setter;

/**
 * A camera model for pinhole, wide angle, and fisheye cameras up to a FOV of 180 degrees. The full camera model
 * from [1] has been implemented. While mathematically very similar to the model presented in [1], this has been
 * modified to extend a pinhole camera model and has one additional degree of freedom. In the original formulation
 * skew was implicitly 0. (mu, mv) = (fx, fy). This was done primarily to maximize code reuse and integration with
 * existing code.
 *
 * <p>[1] Kannala, J., and Brandt, S. S. (2006). A generic camera model and calibration method for conventional,
 * wide-angle, and fish-eye lenses. IEEE transactions on pattern analysis and machine intelligence,
 * 28(8), 1335-1340.</p>
 *
 * @author Peter Abeles
 */
public class CameraKannalaBrandt extends CameraPinhole {

	/** Coeffients for radially symmetric model */
	@Getter @Setter public double[] coefSymm;

	/** Coefficients for distortion terms in radial direction */
	@Getter @Setter public double[] coefRad, coefRadTrig;

	/** Coefficients for distortion terms in tangential direction */
	@Getter @Setter public double[] coefTan, coefTanTrig;

	/**
	 * Constructor which allows the order of all distortion coefficients to be specified
	 * @param numRadSym Number of terms in 'k' radially symmetric model. Standard is 5
	 * @param numDistRad Number of terms in 'l' and 'm' the distortion polynomial terms Standard is 3
	 * @param numDistTrig Number of terms in 'i'  and 'j' the distortion trigonometric polynomial terms. Standard is 4
	 */
	public CameraKannalaBrandt( int numRadSym, int numDistRad , int numDistTrig )
	{
		coefSymm = new double[numRadSym];
		coefRad = new double[numDistRad];
		coefTan = new double[numDistRad];
		coefRadTrig = new double[numDistTrig];
		coefTanTrig = new double[numDistTrig];
	}

	/**
	 * Copy constructor
	 */
	public CameraKannalaBrandt( CameraKannalaBrandt src ) {
		setTo(src);
	}

	/**
	 * With no coefficients specified.
	 */
	public CameraKannalaBrandt()
	{
		this(0,0,0);
	}

	public CameraKannalaBrandt fsetK(double fx, double fy,
									 double skew,
									 double cx, double cy) {
		super.fsetK(fx, fy, skew, cx, cy);
		return this;
	}

	public CameraKannalaBrandt fsetShape( int width, int height ) {
		this.width = width;
		this.height = height;
		return this;
	}

	public CameraKannalaBrandt fsetSymmetric( double... coefs ) {
		this.coefSymm = coefs.clone();
		return this;
	}

	public CameraKannalaBrandt fsetRadial( double... coefs ) {
		this.coefRad = coefs.clone();
		return this;
	}

	public CameraKannalaBrandt fsetTangent( double... coefs ) {
		this.coefTan = coefs.clone();
		return this;
	}

	public CameraKannalaBrandt fsetRadialTrig( double... coefs ) {
		BoofMiscOps.checkTrue(coefs.length==0 || coefs.length==4);
		this.coefRadTrig = coefs.clone();
		return this;
	}

	public CameraKannalaBrandt fsetTangentTrig( double... coefs ) {
		BoofMiscOps.checkTrue(coefs.length==0 || coefs.length==4);
		this.coefTanTrig = coefs.clone();
		return this;
	}


	/**
	 * Returns true if it's a symmetric model. That is, no radial or tangential distortion
	 */
	public boolean isSymmetricModel() {
		boolean noRadial = true;
		for (int i = 0; i < coefRad.length; i++) {
			if (coefRad[i] != 0) {
				noRadial = false;
				break;
			}
		}
		boolean noTangential = true;
		for (int i = 0; i < coefTan.length; i++) {
			if (coefTan[i] != 0) {
				noTangential = false;
				break;
			}
		}
		return noRadial && noTangential;
	}

	/**
	 * Returns true if there are coefficients that could result in a non-zero distortion for the
	 * non-symmetric terms. This does not check to see if the coefficients are zero.
	 */
	public boolean hasNonSymmetricCoefficients() {
		if (coefRad.length != 0 && coefRadTrig.length == 4)
			return true;
		if (coefTan.length != 0 && coefTanTrig.length == 4)
			return true;

		return false;
	}

	public void setTo( CameraKannalaBrandt src ) {
		super.setTo(src);

		this.coefSymm = src.coefSymm.clone();
		this.coefRad = src.coefRad.clone();
		this.coefRadTrig = src.coefRadTrig.clone();
		this.coefTan = src.coefTan.clone();
		this.coefTanTrig = src.coefTanTrig.clone();
	}

	@Override
	public <T extends CameraModel> T createLike() {
		return (T)new CameraKannalaBrandt(coefSymm.length, coefRad.length, coefRadTrig.length);
	}

	@Override
	public void print() {
		super.print();
		printArray("symmetric", coefSymm);
		printArray("radial", coefRad);
		printArray("tangential", coefTan);
	}

	private static void printArray( String name, double[] coefs ) {

		if( coefs.length > 0 ) {
			System.out.print(name +" = [ ");
			for( int i = 0; i < coefs.length; i++ ) {
				System.out.printf("%6.2e ",coefs[i]);
			}
			System.out.println("]");
		} else {
			System.out.println("No "+name);
		}
	}
}
