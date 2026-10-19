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

import boofcv.alg.geo.bundle.cameras.BundlePinhole;
import boofcv.struct.calib.CameraModel;
import boofcv.struct.calib.CameraPinhole;
import georegression.struct.point.Point4D_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.chainrule.CRArray;
import org.ddogleg.chainrule.CRMatrix;
import org.ddogleg.chainrule.CROperatorBackwardsGrad;
import org.ddogleg.chainrule.CROperatorBackwardsJac;
import org.ddogleg.chainrule.MathCRMatrixD;

import java.util.List;
import java.util.Map;

/// Pinhole camera. Parameters are the camera model. Input is a point in 3D or homogeneous coordinates.
public class CROperatorCameraPinhole implements CRCameraModel<double[]>,
		CROperatorBackwardsGrad<double[]>, CROperatorBackwardsJac<double[]> {
	CameraPinhole camera = new CameraPinhole();
	Point4D_F64 point = new Point4D_F64();

	/// True if it is expecting homogeneous points otherwise 3D points
	@Getter @Setter boolean homogeneous;

	/// true if it assumes the skew is zero
	@Getter @Setter boolean zeroSkew = true;

	/// if ratio > 0 then the aspect ratio is fixed with `fy = fx/aspectRatio` and fy is not a parameter
	@Getter @Setter double aspectRatio = -1;

	// Workspace for jacobian calculation
	final CRMatrix<double[]> workJac = CRMatrix.createD(5, 5);

	MathCRMatrixD math = new MathCRMatrixD();

	public CROperatorCameraPinhole( boolean homogeneous, boolean zeroSkew, double aspectRatio ) {
		this.homogeneous = homogeneous;
		this.zeroSkew = zeroSkew;
		this.aspectRatio = aspectRatio;
	}

	public CROperatorCameraPinhole( boolean homogeneous, boolean zeroSkew ) {
		this(homogeneous, zeroSkew, -1);
	}

	public CROperatorCameraPinhole( boolean homogeneous ) {
		this(homogeneous, true);
	}

	public CROperatorCameraPinhole() {}

	public static int encode( CameraPinhole model, boolean zeroSkew,
							  double aspectRatio, double[] params, int offset ) {
		params[offset++] = model.fx;
		if (aspectRatio <= 0)
			params[offset++] = model.fy;
		params[offset++] = model.cx;
		params[offset++] = model.cy;

		if (!zeroSkew) {
			params[offset++] = model.skew;
		}

		return offset;
	}

	public static int decode( double[] params, int offset,
							  CameraPinhole model, boolean zeroSkew,
							  double aspectRatio ) {

		model.fx = params[offset++];
		if (aspectRatio <= 0)
			model.fy = params[offset++];
		else
			model.fy = model.fx/aspectRatio;
		model.cx = params[offset++];
		model.cy = params[offset++];

		if (!zeroSkew) {
			model.skew = params[offset++];
		} else {
			model.skew = 0;
		}
		return offset;
	}

	@Override public CROperatorCameraPinhole setTo( CameraModel m ) {
		if (m instanceof CameraPinhole) {
			return setTo(m);
		}
		throw new IllegalArgumentException("Model must be an instance of CameraBrownZoom");
	}

	public CROperatorCameraPinhole setTo( CameraPinhole src ) {
		camera.fx = src.fx;
		camera.fy = src.fy;
		camera.cx = src.cx;
		camera.cy = src.cy;

		if (zeroSkew)
			camera.skew = 0.0;
		else
			camera.skew = src.skew;
		return this;
	}

	public CROperatorCameraPinhole setTo( BundlePinhole src ) {
		this.zeroSkew = src.zeroSkew;
		camera.fx = src.fx;
		camera.fy = src.fy;
		camera.cx = src.cx;
		camera.cy = src.cy;

		if (zeroSkew)
			camera.skew = 0.0;
		else
			camera.skew = src.skew;
		return this;
	}

	public BundlePinhole toBoof() {
		var out = new BundlePinhole();
		out.fx = camera.fx;
		out.fy = camera.fy;
		out.cx = camera.cx;
		out.cy = camera.cy;
		out.zeroSkew = zeroSkew;

		if (zeroSkew)
			out.skew = 0.0;
		else
			out.skew = camera.skew;
		return out;
	}

	/// Encode the camera parameters into an array
	@Override public void encodeCamera( CRArray<double[]> dst ) {
		encode(camera, zeroSkew, aspectRatio, dst.a, dst.offset);
	}

	@Override public void setInput( int which, CRArray<double[]> input ) {
		switch (which) {
			case 0 -> point.setTo(input.d(0), input.d(1), input.d(2), homogeneous ? input.d(3) : 1.0);
			case 1 -> setParameters(input);
			default -> throw new IndexOutOfBoundsException("Unexpected input " + which);
		}
	}

	public void setParameters( CRArray<double[]> src ) {
		decode(src.a, src.offset, camera, zeroSkew, aspectRatio);
	}

	@Override public Map<String, String> getSettings() {
		return Map.of("homogeneous", "" + homogeneous, "zero_skew", "" + zeroSkew,
				"aspect_ratio", "" + aspectRatio);
	}

	@Override public void setSettings( Map<String, String> settings ) {
		settings.forEach(( key, value ) -> {
			switch (key) {
				case "homogeneous" -> homogeneous = Boolean.parseBoolean(value);
				case "zero_skew" -> zeroSkew = Boolean.parseBoolean(value);
				case "aspect_ratio" -> aspectRatio = Double.parseDouble(value);
				default -> throw new IllegalArgumentException("Unknown key: " + key);
			}
		});
	}

	/// @param output Projected pixel
	@Override public void forward( CRArray<double[]> output ) {
		double nx = point.x/point.z;
		double ny = point.y/point.z;

		// Compute projected pixel coordinates (x,y)
		output.d(0, camera.fx*nx + camera.skew*ny + camera.cx);
		output.d(1, camera.fy*ny + camera.cy);
	}

	@Override public void gradient( CRArray<double[]> dout, List<CRArray<double[]>> gradientInputs ) {
		double camX = point.x;
		double camY = point.y;
		double camZ = point.z;

		double nx = camX/camZ;
		double ny = camY/camZ;

		// to reduce verbosity, create local copies of the two values
		final double dout0 = dout.d(0);
		final double dout1 = dout.d(1);

		if (!gradientInputs.get(0).isInvalid()) {
			// gradient for the output. This done an in place multiplication of the 2x3 Jacobian and dout
			CRArray<double[]> gradient = gradientInputs.get(0);
			int giOff = gradient.offset;
			gradient.a[giOff] = dout0*camera.fx/camZ;
			gradient.a[giOff + 1] = dout0*camera.skew/camZ;
			gradient.a[giOff + 1] += dout1*camera.fy/camZ;
			gradient.a[giOff + 2] = dout0*-(camera.fx*camX + camera.skew*camY)/(camZ*camZ);
			gradient.a[giOff + 2] += dout1*-camera.fy*camY/(camZ*camZ);

			if (homogeneous)
				gradient.a[giOff + 3] = 0;
		}

		if (!gradientInputs.get(1).isInvalid()) {
			CRArray<double[]> gradient = gradientInputs.get(1);
			int loc = 0;
			if (aspectRatio <= 0) {
				gradient.d(loc++, dout0*nx);
				gradient.d(loc++, dout1*ny);
			} else {
				// fy = fx/aspectRatio, so the fy derivative folds into the fx element
				gradient.d(loc++, dout0*nx + dout1*ny/aspectRatio);
			}
			gradient.d(loc++, dout0);
			gradient.d(loc++, dout1);

			if (!zeroSkew) {
				gradient.d(loc, dout0*ny);
			}
		}
	}

	@Override public Class<double[]> getArrayType() {
		return double[].class;
	}

	@Override public void jacobian( CRMatrix<double[]> dout, List<CRMatrix<double[]>> jacobianInputs ) {
		double camX = point.x;
		double camY = point.y;
		double camZ = point.z;

		double nx = camX/camZ;
		double ny = camY/camZ;

		if (!jacobianInputs.get(0).isInvalid()) {
			// Compute Jacobian relative to the input
			int loc = 0;

			// Jacobian is defined as @output[i] / @ param[j], where i = row and j = column
			workJac.rows = 2;
			workJac.cols = homogeneous ? 4 : 3;
			workJac.data[loc++] = camera.fx/camZ;              // @ pixelX / @ camX
			workJac.data[loc++] = camera.skew/camZ;            // @ pixelX / @ camY
			workJac.data[loc++] = -(camera.fx*camX + camera.skew*camY)/(camZ*camZ);
			if (homogeneous) // the w in (x,y,z,w) changes the scale, which doesn't change projection
				workJac.data[loc++] = 0;
			workJac.data[loc++] = 0;                           // @ pixelY / @ camX
			workJac.data[loc++] = camera.fy/camZ;              // @ pixelY / @ camY
			workJac.data[loc++] = -camera.fy*camY/(camZ*camZ); // @ pixelY / @ camZ
			if (homogeneous)
				workJac.data[loc] = 0;

			// (@ ??? / @output ) * (@output / @input)
			math.mult(dout, workJac, jacobianInputs.get(0));
		}

		if (!jacobianInputs.get(1).isInvalid()) {
			// Jacobian relative to parameters
			workJac.rows = 2;
			workJac.cols = getInputSize(1);

			int loc = 0;
			workJac.data[loc++] = nx;     // @ pixelX / @ fx
			if (aspectRatio <= 0) {
				workJac.data[loc++] = 0;  // @ pixelX / @ fy
			}
			workJac.data[loc++] = 1;      // @ pixelX / @ cx
			workJac.data[loc++] = 0;      // @ pixelX / @ cy
			if (!zeroSkew) {
				workJac.data[loc++] = ny;
			}

			if (aspectRatio <= 0) {
				workJac.data[loc++] = 0;  // @ pixelY / @ fx
				workJac.data[loc++] = ny; // @ pixelY / @ fy
			} else {
				// fy = fx/aspectRatio, so the fy derivative folds into the fx column
				workJac.data[loc++] = ny/aspectRatio; // @ pixelY / @ fx
			}
			workJac.data[loc++] = 0;      // @ pixelY / @ cx
			workJac.data[loc++] = 1;      // @ pixelY / @ cy
			if (!zeroSkew) {
				workJac.data[loc] = 0;
			}

			// (@ ??? / @output ) * (@output / @parameter)
			math.mult(dout, workJac, jacobianInputs.get(1));
		}
	}

	@Override public int getInputCount() {return 2;}

	@Override public int getInputSize( int which ) {
		return switch (which) {
			case 0 -> homogeneous ? 4 : 3;
			case 1 -> (aspectRatio > 0 ? 3 : 4) + (zeroSkew ? 0 : 1);
			default -> throw new IndexOutOfBoundsException("Unexpected input");
		};
	}

	@Override public int getOutputSize() {return 2;}
}
