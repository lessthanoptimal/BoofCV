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
 * A camera model for pinhole, wide angle, and fisheye cameras. The full camera model
 * from [1] has been implemented. While mathematically very similar to the model presented in [1], this has been
 * modified to extend a pinhole camera model and now can model skew. In the original formulation
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

	/** Coefficients for radially symmetric model */
	@Getter @Setter public double[] symmetric;

	/** Coefficients for distortion terms in radial direction */
	@Getter @Setter public double[] radial, radialTrig;

	/** Coefficients for distortion terms in tangential direction */
	@Getter @Setter public double[] tangent, tangentTrig;

	/**
	 * Constructor which allows the order of all distortion coefficients to be specified
	 *
	 * @param numRadSym Number of terms in 'k' radially symmetric model. Standard is 5
	 * @param numDistRad Number of terms in 'l' and 'm' the distortion polynomial terms Standard is 3
	 * @param numDistTrig Number of terms in 'i'  and 'j' the distortion trigonometric polynomial terms. Standard is 4
	 */
	public CameraKannalaBrandt( int numRadSym, int numDistRad, int numDistTrig ) {
		symmetric = new double[numRadSym];
		radial = new double[numDistRad];
		tangent = new double[numDistRad];
		radialTrig = new double[numDistTrig];
		tangentTrig = new double[numDistTrig];
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
	public CameraKannalaBrandt() {
		this(0, 0, 0);
	}

	@Override public CameraKannalaBrandt fsetK( double fx, double fy,
									  double skew,
									  double cx, double cy ) {
		super.fsetK(fx, fy, skew, cx, cy);
		return this;
	}

	@Override public CameraKannalaBrandt fsetShape( int width, int height ) {
		this.width = width;
		this.height = height;
		return this;
	}

	public CameraKannalaBrandt fsetSymmetric( double... coefs ) {
		this.symmetric = coefs.clone();
		return this;
	}

	public CameraKannalaBrandt fsetRadial( double... coefs ) {
		this.radial = coefs.clone();
		return this;
	}

	public CameraKannalaBrandt fsetTangent( double... coefs ) {
		this.tangent = coefs.clone();
		return this;
	}

	public CameraKannalaBrandt fsetRadialTrig( double... coefs ) {
		BoofMiscOps.checkTrue(coefs.length == 0 || coefs.length == 4);
		this.radialTrig = coefs.clone();
		return this;
	}

	public CameraKannalaBrandt fsetTangentTrig( double... coefs ) {
		BoofMiscOps.checkTrue(coefs.length == 0 || coefs.length == 4);
		this.tangentTrig = coefs.clone();
		return this;
	}

	/**
	 * Returns true if it's a symmetric model. That is, no radial or tangential distortion
	 */
	public boolean isSymmetricModel() {
		boolean noRadial = true;
		for (int i = 0; i < radial.length; i++) {
			if (radial[i] != 0) {
				noRadial = false;
				break;
			}
		}
		boolean noTangential = true;
		for (int i = 0; i < tangent.length; i++) {
			if (tangent[i] != 0) {
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
		if (radial.length != 0 && radialTrig.length == 4)
			return true;
		if (tangent.length != 0 && tangentTrig.length == 4)
			return true;

		return false;
	}

	public void setTo( CameraKannalaBrandt src ) {
		super.setTo(src);

		this.symmetric = src.symmetric.clone();
		this.radial = src.radial.clone();
		this.radialTrig = src.radialTrig.clone();
		this.tangent = src.tangent.clone();
		this.tangentTrig = src.tangentTrig.clone();
	}

	public boolean isIdentical( CameraKannalaBrandt src ) {
		if (!super.isEquals(src, 0.0))
			return false;
		if (!isIdentical(symmetric, src.symmetric))
			return false;
		if (!isIdentical(radial, src.radial))
			return false;
		if (!isIdentical(radialTrig, src.radialTrig))
			return false;
		if (!isIdentical(tangent, src.tangent))
			return false;
		if (!isIdentical(tangentTrig, src.tangentTrig))
			return false;
		return true;
	}

	private boolean isIdentical( double[] a, double[] b ) {
		if (a.length != b.length)
			return false;

		for (int i = 0; i < a.length; i++) {
			if (a[i] != b[i])
				return false;
		}
		return true;
	}

	@Override
	public <T extends CameraModel> T createLike() {
		return (T)new CameraKannalaBrandt(symmetric.length, radial.length, radialTrig.length);
	}

	@Override
	public void print() {
		super.print();
		printArray("symmetric", symmetric);
		printArray("radial", radial);
		printArray("tangential", tangent);
		printArray("radial_trig", radialTrig);
		printArray("tangentrig", tangentTrig);
	}

	private static void printArray( String name, double[] coefs ) {

		if (coefs.length > 0) {
			System.out.print(name + " = [ ");
			for (int i = 0; i < coefs.length; i++) {
				System.out.printf("%6.2e ", coefs[i]);
			}
			System.out.println("]");
		} else {
			System.out.println("No " + name);
		}
	}
}
