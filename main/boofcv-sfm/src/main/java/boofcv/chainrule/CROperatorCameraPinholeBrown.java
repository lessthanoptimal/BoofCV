/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.chainrule;

import boofcv.abst.geo.calibration.ConfigCalibrateBrown;
import boofcv.alg.geo.bundle.cameras.BundlePinholeBrown;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraModel;
import boofcv.struct.calib.CameraPinholeBrown;
import georegression.struct.point.Point4D_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.chainrule.CRArray;
import org.ddogleg.chainrule.CRMatrix;
import org.ddogleg.chainrule.CROperatorBackwardsJac;
import org.ddogleg.chainrule.MathCRMatrixD;
import org.ejml.data.DMatrixRMaj;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/// Block that implements a [CameraPinholeBrown]
///
/// * input\[0\] is a 3D or homogenous point in camera coordinate system.
/// * input\[1\] is the camera's intrinsic parameters
/// * output Is project pixels
public class CROperatorCameraPinholeBrown implements CROperatorBackwardsJac<double[]>, CRCameraModel<double[]> {
	/// True if it is expecting homogeneous points otherwise 3D points
	@Getter @Setter boolean homogeneous;

	/// true if it assumes the skew is zero
	@Getter @Setter boolean zeroSkew;

	/// true if tangential terms are included
	@Getter @Setter boolean tangential;

	/// if ratio > 0 then the aspect ratio is fixed with `fy = fx/aspectRatio` and fy is not a parameter
	@Getter @Setter double aspectRatio = -1;

	// Passed in point coordinate in 3D space
	protected Point4D_F64 point = new Point4D_F64();

	// Parameters
	public final CameraPinholeBrown model = new CameraPinholeBrown(0);

	// Workspace for jacobian calculation
	final CRMatrix<double[]> workJac;

	final protected MathCRMatrixD math = new MathCRMatrixD();

	public CROperatorCameraPinholeBrown( boolean homogeneous, boolean zeroSkew, int numRadial, boolean tangential,
										 double aspectRatio ) {
		BoofMiscOps.checkTrue(numRadial >= 0, "numRadial must be >= 0");

		this.homogeneous = homogeneous;
		this.zeroSkew = zeroSkew;
		this.tangential = tangential;
		this.aspectRatio = aspectRatio;
		this.model.radial = new double[numRadial];

		workJac = CRMatrix.createD(2, getIntrinsicsSize());
	}

	public CROperatorCameraPinholeBrown( boolean homogeneous, boolean zeroSkew, int numRadial, boolean tangential ) {
		this(homogeneous, zeroSkew, numRadial, tangential, -1);
	}

	/// Creates camera with the following defaults; homogeneous=false, zeroSkew=true, numRadial=2, tangential=false.
	public CROperatorCameraPinholeBrown() {
		this(false, true, 2, false);
	}

	@Override public CROperatorCameraPinholeBrown setTo( CameraModel m ) {
		if (m instanceof CameraPinholeBrown) {
			return setTo((CameraPinholeBrown)m);
		}
		throw new IllegalArgumentException("Model must be an instance of CameraBrownZoom");
	}

	public CROperatorCameraPinholeBrown setTo( CameraPinholeBrown src ) {
		setK(src.fx, src.fy, src.skew, src.cx, src.cy);
		if (src.radial != null)
			setRadial(src.radial);
		else
			model.radial = new double[0];
		setTangential(src.t1, src.t2);
		return this;
	}

	public CROperatorCameraPinholeBrown setTo( BundlePinholeBrown src ) {
		setK(src.fx, src.fy, src.skew, src.cx, src.cy);
		if (src.radial != null)
			setRadial(src.radial);
		else
			model.radial = new double[0];
		setTangential(src.t1, src.t2);
		this.zeroSkew = src.zeroSkew;
		this.tangential = src.tangential;
		this.aspectRatio = src.aspectRatio;
		return this;
	}

	public BundlePinholeBrown toBoof() {
		var out = new BundlePinholeBrown();
		out.setTo(model);
		out.zeroSkew = zeroSkew;
		out.tangential = tangential;
		out.aspectRatio = aspectRatio;
		return out;
	}
	public CROperatorCameraPinholeBrown setK( DMatrixRMaj K ) {
		model.fx = K.unsafe_get(0,0);
		model.fy = K.unsafe_get(1,1);
		model.skew = K.unsafe_get(0,1);
		model.cx = K.unsafe_get(0,2);
		model.cy = K.unsafe_get(1,2);
		return this;
	}

	public CROperatorCameraPinholeBrown setK( double fx, double fy, double skew, double cx, double cy ) {
		model.fx = fx;
		model.fy = fy;
		model.skew = skew;
		model.cx = cx;
		model.cy = cy;
		return this;
	}

	public CROperatorCameraPinholeBrown setRadial( double... radial ) {
		model.radial = radial.clone();
		return this;
	}

	public CROperatorCameraPinholeBrown setTangential( double t1, double t2 ) {
		model.t1 = t1;
		model.t2 = t2;
		return this;
	}

	@Override public void setInput( int which, CRArray<double[]> input ) {
		switch (which) {
			case 0 -> point.setTo(input.d(0), input.d(1), input.d(2), homogeneous ? input.d(3) : 1.0);
			case 1 -> decodeCamera(input, model, tangential, zeroSkew, aspectRatio);
			default -> throw new IndexOutOfBoundsException("Unexpected input " + which);
		}
	}

	public static void decodeCamera( CRArray<double[]> src,
									 CameraPinholeBrown model, boolean tangential, boolean zeroSkew,
									 double aspectRatio ) {
		decode(src.a, src.offset, model, tangential, zeroSkew, aspectRatio);
	}

	/// Where the radial distortion coefficients start inside an encoded camera
	public static int offsetRadial( ConfigCalibrateBrown config ) {
		return config.aspectRatio > 0 ? 3 : 4;
	}

	/// Where the tangential pair starts inside an encoded camera
	public static int offsetTangential( ConfigCalibrateBrown config ) {
		return offsetRadial(config) + config.numRadial;
	}

	public static int decode( double[] params, int offset,
							  CameraPinholeBrown model, ConfigCalibrateBrown config) {
		return decode(params, offset, model, config.tangential, config.zeroSkew, config.aspectRatio);
	}

	public static int decode( double[] params, int offset,
							  CameraPinholeBrown model, boolean tangential, boolean zeroSkew,
							  double aspectRatio ) {

		model.fx = params[offset++];
		if (aspectRatio <= 0)
			model.fy = params[offset++];
		else
			model.fy = model.fx/aspectRatio;
		model.cx = params[offset++];
		model.cy = params[offset++];

		final double[] radial = radial(model);
		for (int i = 0; i < radial.length; i++) {
			radial[i] = params[offset++];
		}
		if (tangential) {
			model.t1 = params[offset++];
			model.t2 = params[offset++];
		} else {
			model.t1 = model.t2 = 0.0;
		}

		if (!zeroSkew) {
			model.skew = params[offset++];
		} else {
			model.skew = 0;
		}
		return offset;
	}

	@Override public void encodeCamera( CRArray<double[]> dst ) {
		encodeCamera(model, tangential, zeroSkew, aspectRatio, dst);
	}

	/// Encode the camera parameters into an array
	public static void encodeCamera( CameraPinholeBrown model, boolean tangential, boolean zeroSkew,
									 double aspectRatio, CRArray<double[]> dst ) {
		encode(model, tangential, zeroSkew, aspectRatio, dst.a, dst.offset);
	}

	public static int encode( CameraPinholeBrown model, ConfigCalibrateBrown config,
							  double[] params, int offset ) {
		return encode(model, config.tangential, config.zeroSkew, config.aspectRatio, params, offset);
	}

	public static int encode( CameraPinholeBrown model, boolean tangential, boolean zeroSkew,
							  double aspectRatio, double[] params, int offset ) {
		params[offset++] = model.fx;
		if (aspectRatio <= 0)
			params[offset++] = model.fy;
		params[offset++] = model.cx;
		params[offset++] = model.cy;

		final double[] radial = radial(model);
		for (int i = 0; i < radial.length; i++) {
			params[offset++] = radial[i];
		}

		if (tangential) {
			params[offset++] = model.t1;
			params[offset++] = model.t2;
		}

		if (!zeroSkew) {
			params[offset++] = model.skew;
		}

		return offset;
	}

	@Override public Map<String, String> getSettings() {
		return Map.of("homogeneous", "" + homogeneous,
				"zero_skew", "" + zeroSkew, "tangential", "" + tangential,
				"num_radial", "" + radial(model).length,
				"aspect_ratio", "" + aspectRatio);
	}

	@Override public void setSettings( Map<String, String> settings ) {
		settings.forEach(( key, value ) -> {
			switch (key) {
				case "homogeneous" -> homogeneous = Boolean.parseBoolean(value);
				case "zero_skew" -> zeroSkew = Boolean.parseBoolean(value);
				case "tangential" -> tangential = Boolean.parseBoolean(value);
				case "num_radial" -> model.radial = new double[Integer.parseInt(value)];
				case "aspect_ratio" -> aspectRatio = Double.parseDouble(value);
				default -> throw new IllegalArgumentException("Unknown key: " + key);
			}
		});
	}

	@Override public void forward( CRArray<double[]> output ) {
		double camX = point.x;
		double camY = point.y;
		double camZ = point.z;
		// ignore W since it doesn't change the projection

		// compute normalized image coordinates
		double nx = camX/camZ;
		double ny = camY/camZ;

		final double[] radial = radial(model);
		final double t1 = model.t1;
		final double t2 = model.t2;

		// compute radial distortion
		double a = 0;
		double r2 = nx*nx + ny*ny;
		double r2i = r2;
		for (int i = 0; i < radial.length; i++) {
			a += radial[i]*r2i;
			r2i *= r2;
		}

		// Apply distortion
		double x, y;
		if (tangential) {
			x = nx*(1 + a) + 2*t1*nx*ny + t2*(r2 + 2*nx*nx);
			y = ny*(1 + a) + t1*(r2 + 2*ny*ny) + 2*t2*nx*ny;
		} else {
			x = nx*(1 + a);
			y = ny*(1 + a);
		}
		// Convert to pixels (x,y)
		output.d(0, model.fx*x + model.skew*y + model.cx);
		output.d(1, model.fy*y + model.cy);
	}

	@Override public void jacobian( CRMatrix<double[]> dout, List<CRMatrix<double[]>> jacobianInputs ) {
		double camX = point.x;
		double camY = point.y;
		double camZ = point.z;
		// ignore W since it doesn't change the projection

		// Precompute variables that are needed below
		double nx = camX/camZ;
		double ny = camY/camZ;

		double Z = camZ;

		// Apply radial distortion
		double sum = 0;
		double sumdot = 0;

		double r2 = nx*nx + ny*ny;
		double r2i = r2;
		double rdev = 1;

		// Copy into a local variable to keep code more concise
		final double[] radial = radial(model);
		final double t1 = model.t1;
		final double t2 = model.t2;
		final double fx = model.fx;
		final double fy = model.fy;
		final double skew = model.skew;

		for (int i = 0; i < radial.length; i++) {
			sum += radial[i]*r2i;
			sumdot += radial[i]*(i + 1)*rdev;

			r2i *= r2;
			rdev *= r2;
		}

		if (!jacobianInputs.get(0).isInvalid()) {
			// Jacobian is defined as @output[i] / @ param[j], where i = row and j = column
			workJac.shape(getOutputSize(), getInputSize(0));
			math.zero(workJac);

			// X
			double xdot = sumdot*2*nx*nx/Z + (1 + sum)/Z;
			double ydot = sumdot*2*nx*ny/Z;
			if (tangential) {
				xdot += (2*t1*ny + t2*6*nx)/Z;
				ydot += (2*t1*nx + 2*ny*t2)/Z;
			}
			workJac.d(0, 0, fx*xdot + skew*ydot);
			workJac.d(1, 0, fy*ydot);

			// Y
			xdot = sumdot*2*ny*nx/Z;
			ydot = sumdot*2*ny*ny/Z + (1 + sum)/Z;
			if (tangential) {
				xdot += (2*t1*nx + t2*2*ny)/Z;
				ydot += (6*t1*ny + 2*nx*t2)/Z;
			}
			workJac.d(0, 1, fx*xdot + skew*ydot);
			workJac.d(1, 1, fy*ydot);

			// Z
			xdot = -sumdot*2*r2*nx/Z;
			ydot = -sumdot*2*r2*ny/Z;

			xdot += -(1 + sum)*nx/Z;
			ydot += -(1 + sum)*ny/Z;

			if (tangential) {
				xdot += -(4*t1*nx*ny + 6*t2*nx*nx + 2*t2*ny*ny)/Z;
				ydot += -(2*t1*nx*nx + 6*t1*ny*ny + 4*nx*ny*t2)/Z;
			}
			workJac.d(0, 2, fx*xdot + skew*ydot);
			workJac.d(1, 2, fy*ydot);

			// (@ ??? / @output ) * (@output / @input)
			math.mult(dout, workJac, jacobianInputs.get(0));
		}

		if (!jacobianInputs.get(1).isInvalid()) {
			workJac.shape(getOutputSize(), getInputSize(1));
			math.zero(workJac);

			// compute distorted normalized image coordinates
			double x = nx + nx*sum + (tangential ? 2*t1*nx*ny + t2*(r2 + 2*nx*nx) : 0);
			double y = ny + ny*sum + (tangential ? t1*(r2 + 2*ny*ny) + 2*t2*nx*ny : 0);

			jacobianIntrinsic(nx, ny, x, y);
			math.mult(dout, workJac, jacobianInputs.get(1));
		}
	}

	/// @param nx undistorted normalized image coordinates
	/// @param ny undistorted normalized image coordinates
	/// @param dnx distorted normalized image coordinates
	/// @param dny distorted normalized image coordinates
	private void jacobianIntrinsic( double nx, double ny,
									double dnx, double dny ) {
		// Intrinsic parameters
		workJac.shape(getOutputSize(), getIntrinsicsSize());
		math.zero(workJac);

		int col = 0;
		if (aspectRatio <= 0) {
			workJac.d(0, col++, dnx); // fx
			workJac.d(1, col++, dny); // fy
		} else {
			// fy = fx/aspectRatio, so the fy derivative folds into the fx column
			workJac.d(0, col, dnx);
			workJac.d(1, col++, dny/aspectRatio);
		}
		workJac.d(0, col++, 1); // cx
		workJac.d(1, col++, 1); // cy

		// Copy into a local variable to keep code more concise
		final double[] radial = radial(model);
		final double fx = model.fx;
		final double fy = model.fy;
		final double skew = model.skew;

		// Radial
		double r2 = nx*nx + ny*ny;
		double r2i = r2;
		for (int i = 0; i < radial.length; i++) {
			double xdot = nx*r2i;
			double ydot = ny*r2i;

			workJac.d(0, col, fx*xdot + skew*ydot);
			workJac.d(1, col, fy*ydot);
			r2i *= r2;
			col++;
		}

		// Tangential
		if (tangential) {
			double xy2 = 2.0*nx*ny;
			double r2yy = r2 + 2*ny*ny;
			double r2xx = r2 + 2*nx*nx;

			workJac.d(0, col, fx*xy2 + skew*r2yy);
			workJac.d(1, col, fy*r2yy);
			col++;

			workJac.d(0, col, fx*r2xx + skew*xy2);
			workJac.d(1, col, fy*xy2);
			col++;
		}

		if (!zeroSkew) {
			workJac.d(0, col, dny);
		}
	}

	@Override public int getInputSize( int which ) {
		return switch (which) {
			case 0 -> homogeneous ? 4 : 3;
			case 1 -> getIntrinsicsSize();
			default -> throw new IndexOutOfBoundsException("Unexpected input");
		};
	}

	@Override public int getInputCount() {return 2;}

	int getIntrinsicsSize() {return getIntrinsicsSize(model, tangential, zeroSkew, aspectRatio);}

	public static int getIntrinsicsSize( CameraPinholeBrown model, boolean tangential, boolean zeroSkew,
										 double aspectRatio ) {
		return (aspectRatio > 0 ? 3 : 4) + radial(model).length + (tangential ? 2 : 0) + (zeroSkew ? 0 : 1);
	}

	@Override public int getOutputSize() {return 2;}

	@Override public Class<double[]> getArrayType() {return double[].class;}

	/// The radial coefficients, which this operator always assigns
	private static double[] radial( CameraPinholeBrown model ) {
		return Objects.requireNonNull(model.radial);
	}
}
