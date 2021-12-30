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
import org.ejml.FancyPrint;

import static boofcv.struct.calib.CameraPinholeBrown.toStringArray;

/**
 * A camera model for pinhole, wide angle, and fisheye cameras. The full camera model
 * from [1] has been implemented. While mathematically very similar to the model presented in [1], this has been
 * modified to extend a pinhole camera model and now can model skew. In the original formulation
 * skew was implicitly 0. (mu, mv) = (fx, fy). This was done primarily to maximize code reuse and integration with
 * existing code.
 *
 * <p>(x,y,z) is a point in 3D space in camera frame</p>
 * <p>&theta; = acos(z/normf([x y z]))</p>
 * <p>&phi; = atan2(y, x)</p>
 * <p>symmetric(&theta;) = k<sub>1</sub>&theta; + k<sub>2</sub>&theta;<sup>3</sup> + ... + &theta;<sub>n</sub>&theta;<sup>2*n-1</sup></p>
 * <p>radial(&theta;,&phi;) = (l<sub>1</sub>&theta; + l<sub>2</sub>&theta;<sup>3</sup> + l<sub>3</sub>&theta;<sup>5</sup>)
 * (i<sub>1</sub>cos(&phi;) + i<sub>2</sub>sin(&phi;) + i<sub>3</sub>cos(2&phi;) + i<sub>4</sub>sin(2&phi;))</p>
 * <p>tangential(&theta;,&phi;) = (m<sub>1</sub>&theta; + m<sub>2</sub>&theta;<sup>3</sup> + m<sub>3</sub>&theta;<sup>5</sup>)
 * (j<sub>1</sub>cos(&phi;) + j<sub>2</sub>sin(&phi;) + j<sub>3</sub>cos(2&phi;) + j<sub>4</sub>sin(2&phi;))</p>
 *
 * Then the distorted normalized coordinates are found:
 * <p>x<sub>d</sub> = (symmetric(&theta;) + radial(&theta;,&phi;))u<sub>r</sub>(&phi;) + tangential(&theta;,&phi;)u<sub>t</sub>(&phi;)</p>
 * <p>where u<sub>r</sub> and u<sub>t</sub> are unit vectors in radial and tangential directions.</p>
 *
 * <p>
 * NOTE: If the number of asymmetric distortion terms is set to zero then there will only be radially symmetric
 * distortion. This is what most libraries refer to as the Kannala-Brandt model as they do not implement the full model.
 * It's also often referred to as an Equidistance model. That's a bit of a misnomer as the Equidistance model is
 * defined as r = f*theta, which is a special case of the symmetric model. BoofCV's naming convention is closer
 * to the original authors in [1] and their source code.
 * </p>
 *
 * <p>[1] Kannala, J., and Brandt, S. S. (2006). A generic camera model and calibration method for conventional,
 * wide-angle, and fish-eye lenses. IEEE transactions on pattern analysis and machine intelligence,
 * 28(8), 1335-1340.</p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
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
	 * @param numSymmetric Number of radially symmetric terms. Standard is 5.
	 * @param numAsymmetric Number of non symmetric terms. If not zero then trig coefficients will be 4.
	 * Standard is 4
	 */
	public CameraKannalaBrandt( int numSymmetric, int numAsymmetric ) {
		configureCoefficients(numSymmetric, numAsymmetric);
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
		this(0, 0);
	}

	/**
	 * Configures the number of coefficients for each distortion term.
	 *
	 * @param numSymmetric Number of radially symmetric terms. Standard is 5.
	 * @param numAsymmetric Number of non symmetric terms. If not zero then trig coefficients will be 4.
	 * Standard is 4
	 */
	public void configureCoefficients( int numSymmetric, int numAsymmetric ) {
		int numTrig = numAsymmetric != 0 ? 4 : 0;

		symmetric = new double[numSymmetric];
		radial = new double[numAsymmetric];
		tangent = new double[numAsymmetric];
		radialTrig = new double[numTrig];
		tangentTrig = new double[numTrig];
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
	public boolean isAsymmetricModel() {
		if (radial.length != 0 && radialTrig.length == 4)
			return true;
		if (tangent.length != 0 && tangentTrig.length == 4)
			return true;

		return false;
	}

	/**
	 * Copies the value of 'src' into 'this'. After this call they will be identical
	 *
	 * @param src (input) Camera model
	 */
	public void setTo( CameraKannalaBrandt src ) {
		super.setTo(src);

		this.symmetric = BoofMiscOps.copySmart(src.symmetric, this.symmetric);
		this.radial = BoofMiscOps.copySmart(src.radial, this.radial);
		this.radialTrig = BoofMiscOps.copySmart(src.radialTrig, this.radialTrig);
		this.tangent = BoofMiscOps.copySmart(src.tangent, this.tangent);
		this.tangentTrig = BoofMiscOps.copySmart(src.tangentTrig, this.tangentTrig);
	}

	/**
	 * Checks to see of the two models are exactly alike
	 */
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

	/**
	 * Checks to see if the two arrays are exactly alike
	 */
	private static boolean isIdentical( double[] a, double[] b ) {
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
		return (T)new CameraKannalaBrandt(symmetric.length, radial.length);
	}

	@Override
	public String toString() {
		FancyPrint fp = new FancyPrint();
		String txt = "CameraKannalaBrandt{" +
				"fx=" + fx +
				", fy=" + fy +
				", skew=" + skew +
				", cx=" + cx +
				", cy=" + cy +
				", width=" + width +
				", height=" + height;
		txt += toStringArray(fp, "s", symmetric);
		txt += toStringArray(fp, "r", radial);
		txt += toStringArray(fp, "rt", radialTrig);
		txt += toStringArray(fp, "t", tangent);
		txt += toStringArray(fp, "tt", tangentTrig);
		txt += '}';
		return txt;
	}

	@Override
	public void print() {
		super.print();
		printArray("symmetric", symmetric);
		printArray("radial", radial);
		printArray("tangential", tangent);
		printArray("radial_trig", radialTrig);
		printArray("tangent_trig", tangentTrig);
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
