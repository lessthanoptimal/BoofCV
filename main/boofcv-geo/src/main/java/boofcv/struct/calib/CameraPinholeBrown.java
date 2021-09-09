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

import lombok.Getter;
import lombok.Setter;
import org.ejml.FancyPrint;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Arrays;

/**
 * <p>Adds radial and tangential distortion to the intrinsic parameters of a {@link CameraPinhole pinhole camera}.</p>
 *
 * <p>
 * Radial and Tangential Distortion:<br>
 * x<sub>d</sub> = x<sub>n</sub> + x<sub>n</sub>[k<sub>1</sub> r<sup>2</sup> + ... + k<sub>n</sub> r<sup>2n</sup>] + dx<br>
 * dx<sub>u</sub> = [ 2t<sub>1</sub> u v + t<sub>2</sub>(r<sup>2</sup> + 2u<sup>2</sup>)] <br>
 * dx<sub>v</sub> = [ t<sub>1</sub>(r<sup>2</sup> + 2v<sup>2</sup>) + 2 t<sub>2</sub> u v]<br>
 * <br>
 * r<sup>2</sup> = u<sup>2</sup> + v<sup>2</sup><br>
 * where x<sub>d</sub> is the distorted coordinates, x<sub>n</sub>=(u,v) is undistorted normalized image coordinates.
 * Pixel coordinates are found x = K*[u;v]
 * </p>
 *
 * @author Peter Abeles
 */
public class CameraPinholeBrown extends CameraPinhole implements Serializable {

	/** radial distortion parameters */
	public @Nullable double[] radial;
	/** tangential distortion parameters */
	public @Getter @Setter double t1, t2;

	/**
	 * Default constructor. flipY is false and everything else is zero or null.
	 */
	public CameraPinholeBrown() {
	}

	public CameraPinholeBrown( int numRadial ) {
		radial = new double[numRadial];
	}

	public CameraPinholeBrown( CameraPinholeBrown param ) {
		setTo(param);
	}

	public CameraPinholeBrown( double fx, double fy,
							   double skew,
							   double cx, double cy,
							   int width, int height ) {
		fsetK(fx, fy, skew, cx, cy, width, height);
	}

	@Override
	public void reset() {
		super.reset();
		if (radial != null)
			Arrays.fill(radial, 0.0);
		t1 = t2 = 0.0;
	}

	@Override
	public CameraPinholeBrown fsetK( double fx, double fy,
									 double skew,
									 double cx, double cy,
									 int width, int height ) {
		return (CameraPinholeBrown)super.fsetK(fx, fy, skew, cx, cy, width, height);
	}

	public CameraPinholeBrown fsetRadial( @Nullable double... radial ) {
		if (radial == null) {
			this.radial = null;
		} else if (this.radial == null || this.radial.length != radial.length)
			this.radial = radial.clone();
		else {
			for (int i = 0; i < radial.length; i++) {
				this.radial[i] = radial[i];
			}
		}
		return this;
	}

	public CameraPinholeBrown fsetTangental( double t1, double t2 ) {
		this.t1 = t1;
		this.t2 = t2;
		return this;
	}

	@Override
	public void setTo( CameraPinhole param ) {
		if (param instanceof CameraPinholeBrown) {
			CameraPinholeBrown p = (CameraPinholeBrown)param;

			p.fsetRadial(p.radial);
			if (p.radial != null)
				radial = p.radial.clone();
			else
				radial = null;

			this.t1 = p.t1;
			this.t2 = p.t2;
		} else {
			this.radial = null;
			this.t1 = 0;
			this.t2 = 0;
		}
		super.setTo(param);
	}

	/**
	 * If true then distortion parameters are specified.
	 */
	public boolean isDistorted() {
		if (radial != null && radial.length > 0) {
			for (int i = 0; i < radial.length; i++) {
				if (radial[i] != 0)
					return true;
			}
		}
		return t1 != 0 || t2 != 0;
	}

	public boolean isDistorted( double tol ) {
		if (radial != null && radial.length > 0) {
			for (int i = 0; i < radial.length; i++) {
				if (Math.abs(radial[i]) > tol)
					return true;
			}
		}
		return Math.abs(t1) > tol || Math.abs(t2) > tol;
	}

	public @Nullable double[] getRadial() {
		return radial;
	}

	public void setRadial( double... radial ) {
		this.radial = radial;
	}

	@Override
	public void print() {
		super.print();
		if (radial != null) {
			for (int i = 0; i < radial.length; i++) {
				System.out.printf("radial[%d] = %6.2e\n", i, radial[i]);
			}
		} else {
			System.out.println("No radial");
		}
		if (t1 != 0 || t2 != 0)
			System.out.printf("tangential = ( %6.2e , %6.2e)\n", t1, t2);
		else {
			System.out.println("No tangential");
		}
	}

	@Override
	public String toString() {
		FancyPrint fp = new FancyPrint();
		String txt = "CameraPinholeRadial{" +
				"fx=" + fx +
				", fy=" + fy +
				", skew=" + skew +
				", cx=" + cx +
				", cy=" + cy +
				", width=" + width +
				", height=" + height;
		txt += toStringArray(fp, "r", radial);

		if (t1 != 0 || t2 != 0) {
			txt += ", t1=" + fp.s(t1) + " t2=" + fp.s(t2);
		}
		txt += '}';
		return txt;
	}

	protected static String toStringArray( FancyPrint fp, String name, @Nullable double[] param ) {
		if (param == null || param.length == 0)
			return "";
		String txt = ",";
		for (int i = 0; i < param.length; i++) {
			txt += " " + name + i + "=" + fp.s(param[i]);
		}
		return txt;
	}

	@Override
	public <T extends CameraModel> T createLike() {
		CameraPinholeBrown model = new CameraPinholeBrown();
		if (radial != null)
			model.radial = new double[radial.length];
		return (T)model;
	}
}
