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

import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.so.Quaternion_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

/// Implementation of [RotationNumerics] for [Quaternion_F64] using Hamiltonian convention.
public class QuaternionNumericsHam implements RotationNumerics {
	private static final double DEFAULT_SMALL_ANGLE = 1e-6;

	Quaternion_F64 quat = new Quaternion_F64();

	// workspace
	DMatrixRMaj h = new DMatrixRMaj(4, 4);
	double[] mult = new double[4];

	double[] qc = new double[4];
	DMatrixRMaj invAB_da = new DMatrixRMaj(4, 4);
	DMatrixRMaj invAB_db = new DMatrixRMaj(4, 4);

	/// Used to determine when the small angle approximation should be used. Radians.
	@Getter @Setter double smallAngleTol = DEFAULT_SMALL_ANGLE;

	/// If true it will turn on extra checks to help debug
	@Getter @Setter boolean debug = false;

	@Override public void set( double[] array, int offset ) {
		if (debug && !isNormalized(array, offset))
			throw new IllegalArgumentException("Not normalized");

		quat.w = array[offset++];
		quat.x = array[offset++];
		quat.y = array[offset++];
		quat.z = array[offset];

		if (quat.w == 0.0 && quat.x == 0.0 && quat.y == 0.0 && quat.z == 0.0)
			throw new IllegalArgumentException("Uninitialized quaternion, all zeros");

//        double norm = Math.sqrt(quat.w*quat.w + quat.x*quat.x + quat.y*quat.y + quat.z*quat.z);
//        if (Math.abs(1.0 - norm) > 5e-7)
//            throw new IllegalArgumentException("Not normalized " + norm);
	}

	@Override public void get( double[] array, int offset ) {
		array[offset++] = quat.w;
		array[offset++] = quat.x;
		array[offset++] = quat.y;
		array[offset] = quat.z;
	}

	@Override public void identity( double[] array, int offset ) {
		array[offset++] = 1;
		array[offset++] = 0;
		array[offset++] = 0;
		array[offset] = 0;
	}

	@Override public boolean isValid( double[] array, int offset ) {
		double norm = 0;
		for (int i = 0; i < 4; i++) {
			double v = array[i + offset];
			norm += v*v;
		}
		norm = Math.sqrt(norm);
		return Math.abs(1.0 - norm) <= 5e-7;
	}

	@Override public void eulerXyzToParam( double rotX, double rotY, double rotZ, double[] params ) {
		double ca = Math.cos(rotX*0.5);
		double sa = Math.sin(rotX*0.5);
		double cb = Math.cos(rotY*0.5);
		double sb = Math.sin(rotY*0.5);
		double cc = Math.cos(rotZ*0.5);
		double sc = Math.sin(rotZ*0.5);

		double w = ca*cb*cc + sa*sb*sc;
		double x = cb*cc*sa - ca*sb*sc;
		double y = ca*cc*sb + cb*sa*sc;
		double z = -cc*sa*sb + ca*cb*sc;

		params[0] = w;
		params[1] = x;
		params[2] = y;
		params[3] = z;
	}

	@Override public void eulerXyzToJacobian( double rotX, double rotY, double rotZ, DMatrixRMaj jacobian ) {
		double ca = Math.cos(rotX*0.5);
		double sa = Math.sin(rotX*0.5);
		double cb = Math.cos(rotY*0.5);
		double sb = Math.sin(rotY*0.5);
		double cc = Math.cos(rotZ*0.5);
		double sc = Math.sin(rotZ*0.5);

		// Compute derivatives of functions above
		double dca = -0.5*sa;
		double dsa = 0.5*ca;
		double dcb = -0.5*sb;
		double dsb = 0.5*cb;
		double dcc = -0.5*sc;
		double dsc = 0.5*cc;

		jacobian.reshape(4, 3);

		jacobian.set(0, 0, dca*cb*cc + dsa*sb*sc);
		jacobian.set(0, 1, ca*dcb*cc + sa*dsb*sc);
		jacobian.set(0, 2, ca*cb*dcc + sa*sb*dsc);

		jacobian.set(1, 0, cb*cc*dsa - dca*sb*sc);
		jacobian.set(1, 1, dcb*cc*sa - ca*dsb*sc);
		jacobian.set(1, 2, cb*dcc*sa - ca*sb*dsc);

		jacobian.set(2, 0, dca*cc*sb + cb*dsa*sc);
		jacobian.set(2, 1, ca*cc*dsb + dcb*sa*sc);
		jacobian.set(2, 2, ca*dcc*sb + cb*sa*dsc);

		jacobian.set(3, 0, -cc*dsa*sb + dca*cb*sc);
		jacobian.set(3, 1, -cc*sa*dsb + ca*dcb*sc);
		jacobian.set(3, 2, -dcc*sa*sb + ca*cb*dsc);
	}

	@Override public void rodriguesToParam( double rx, double ry, double rz, double[] params ) {
		double theta = Math.sqrt(rx*rx + ry*ry + rz*rz);

		if (theta < smallAngleTol) {
			/// use small angle approximation for better stability
			params[0] = 1.0;
			params[1] = rx/2.0;
			params[2] = ry/2.0;
			params[3] = rz/2.0;

			// Ensure it's a unit quaternion
			double norm = 0.0;
			for (int i = 0; i < 4; i++) {
				norm += params[i]*params[i];
			}
			norm = Math.sqrt(norm);
			for (int i = 0; i < 4; i++) {
				params[i] /= norm;
			}
		} else {
			params[0] = Math.cos(theta/2.0);

			double s = Math.sin(theta/2.0);
			params[1] = rx*s/theta;
			params[2] = ry*s/theta;
			params[3] = rz*s/theta;
		}
	}

	@Override public void rodriguesToJacobian( double rx, double ry, double rz, DMatrixRMaj jacobian ) {
		jacobian.reshape(4, 3);
		double inner = rx*rx + ry*ry + rz*rz;
		double theta = Math.sqrt(inner);

		if (theta < smallAngleTol) {
			double qw = 1.0;
			double qx = rx/2.0;
			double qy = ry/2.0;
			double qz = rz/2.0;

			double n = Math.sqrt(qw*qw + qx*qx + qy*qy + qz*qz);
			double n3 = n*n*n;

			jacobian.set(0, 0, -0.5*qx/n3);
			jacobian.set(0, 1, -0.5*qy/n3);
			jacobian.set(0, 2, -0.5*qz/n3);

			jacobian.set(1, 0, 0.5/n - 0.5*qx*qx/n3);
			jacobian.set(1, 1, -0.5*qx*qy/n3);
			jacobian.set(1, 2, -0.5*qx*qz/n3);

			jacobian.set(2, 0, -0.5*qy*qx/n3);
			jacobian.set(2, 1, 0.5/n - 0.5*qy*qy/n3);
			jacobian.set(2, 2, -0.5*qy*qz/n3);

			jacobian.set(3, 0, -0.5*qz*qx/n3);
			jacobian.set(3, 1, -0.5*qz*qy/n3);
			jacobian.set(3, 2, 0.5/n - 0.5*qz*qz/n3);
		} else {
			double theta_dx = rx/theta;
			double theta_dy = ry/theta;
			double theta_dz = rz/theta;

			double c = Math.cos(theta/2.0);
			double s = Math.sin(theta/2.0);

			jacobian.set(0, 0, -0.5*theta_dx*s);
			jacobian.set(0, 1, -0.5*theta_dy*s);
			jacobian.set(0, 2, -0.5*theta_dz*s);

			jacobian.set(1, 0, s/theta + 0.5*rx*rx*c/inner - rx*rx*s/(theta*inner));
			jacobian.set(1, 1, 0.5*rx*ry*c/inner - rx*ry*s/(theta*inner));
			jacobian.set(1, 2, 0.5*rx*rz*c/inner - rx*rz*s/(theta*inner));

			jacobian.set(2, 0, 0.5*ry*rx*c/inner - ry*rx*s/(theta*inner));
			jacobian.set(2, 1, s/theta + 0.5*ry*ry*c/inner - ry*ry*s/(theta*inner));
			jacobian.set(2, 2, 0.5*ry*rz*c/inner - ry*rz*s/(theta*inner));

			jacobian.set(3, 0, 0.5*rz*rx*c/inner - rz*rx*s/(theta*inner));
			jacobian.set(3, 1, 0.5*rz*ry*c/inner - rz*ry*s/(theta*inner));
			jacobian.set(3, 2, s/theta + 0.5*rz*rz*c/inner - rz*rz*s/(theta*inner));
		}
	}

	/// Performs quaternion multiplication and normalization
	@Override public void concatAB( double[] paramsB, int offsetB, double[] result, int offsetResult ) {
		concat(quat.w, quat.x, quat.y, quat.z,
				paramsB[offsetB++], paramsB[offsetB++], paramsB[offsetB++], paramsB[offsetB],
				result, offsetResult);
	}

	@Override public void concatABJacobian( double[] paramsB, int offsetB, DMatrixRMaj outputA, DMatrixRMaj outputB ) {
		concatJacobian(quat.w, quat.x, quat.y, quat.z,
				paramsB[offsetB++], paramsB[offsetB++], paramsB[offsetB++], paramsB[offsetB],
				outputA, outputB);
	}

	/// Performs quaternion multiplication and normalization
	@Override public void concatBA( double[] paramsB, int offsetB, double[] result, int offsetResult ) {
		concat(paramsB[offsetB++], paramsB[offsetB++], paramsB[offsetB++], paramsB[offsetB],
				quat.w, quat.x, quat.y, quat.z, result, offsetResult);
	}

	@Override public void concatBAJacobian( double[] paramsB, int offsetB, DMatrixRMaj outputA, DMatrixRMaj outputB ) {
		outputA.reshape(4, 4);
		outputB.reshape(4, 4);
		concatJacobian(
				paramsB[offsetB++], paramsB[offsetB++], paramsB[offsetB++], paramsB[offsetB],
				quat.w, quat.x, quat.y, quat.z,
				outputB, outputA);
	}

	private void concatJacobian( double w1, double x1, double y1, double z1,
								 double w2, double x2, double y2, double z2,
								 DMatrixRMaj outputA, DMatrixRMaj outputB ) {
		//@formatter:off
		// Jacobian of (rw, rx, ry, rz) relative to 'a'
		h.data[0] =  w2; h.data[1] = -x2; h.data[2]  = -y2; h.data[3] =  -z2;
		h.data[4] =  x2; h.data[5] =  w2; h.data[6]  =  z2; h.data[7] =  -y2;
		h.data[8] =  y2; h.data[9] = -z2; h.data[10] =  w2; h.data[11] =  x2;
		h.data[12] = z2; h.data[13] = y2; h.data[14] = -x2; h.data[15] =  w2;

		// Store the quaternion multiplication in an array
		/* rw */ mult[0] = w1*w2 - x1*x2 - y1*y2 - z1*z2;
		/* rx */ mult[1] = w1*x2 + x1*w2 + y1*z2 - z1*y2;
		/* ry */ mult[2] = w1*y2 - x1*z2 + y1*w2 + z1*x2;
		/* rz */ mult[3] = w1*z2 + x1*y2 - y1*x2 + z1*w2;

		computeConcatJacobian(outputA);

		// Jacobian of (rw, rx, ry, rz) relative to 'b'
		h.data[0]  = w1; h.data[1]  = -x1; h.data[2]  = -y1; h.data[3]  = -z1;
		h.data[4]  = x1; h.data[5]  =  w1; h.data[6]  = -z1; h.data[7]  =  y1;
		h.data[8]  = y1; h.data[9]  =  z1; h.data[10] =  w1; h.data[11] = -x1;
		h.data[12] = z1; h.data[13] = -y1; h.data[14] =  x1; h.data[15] =  w1;
		//@formatter:on

		computeConcatJacobian(outputB);
	}

	private void concat( double w1, double x1, double y1, double z1,
						 double w2, double x2, double y2, double z2,
						 double[] result, int offset ) {

		double rw = w1*w2 - x1*x2 - y1*y2 - z1*z2;
		double rx = w1*x2 + x1*w2 + y1*z2 - z1*y2;
		double ry = w1*y2 - x1*z2 + y1*w2 + z1*x2;
		double rz = w1*z2 + x1*y2 - y1*x2 + z1*w2;

		double norm = Math.sqrt(rw*rw + rx*rx + ry*ry + rz*rz);

		result[offset++] = rw/norm;
		result[offset++] = rx/norm;
		result[offset++] = ry/norm;
		result[offset++] = rz/norm;
	}

	private void computeConcatJacobian( DMatrixRMaj output ) {
		// compute the scale used to ensure the new quaternion has a norm of 1
		double norm = 0;
		for (int i = 0; i < 4; i++) {
			norm += mult[i]*mult[i];
		}
		norm = Math.sqrt(norm);

		// Needed later as a divisor
		double norm3 = norm*norm*norm;

		// hand computed to show you what the formula below is doing
		//
		// double o11 = w2/norm - rw*(w2*rw + x2*rx + y2*ry + z2*rz)/(norm*norm*norm);
		// double o12 = -x2/norm - rw*(-x2*rw + w2*rx - z2*ry + y2*rz)/(norm*norm*norm);

		// Compute the Jacobian
		output.reshape(4, 4);
		for (int row = 0; row < 4; row++) {
			for (int col = 0; col < 4; col++) {
				double left = h.unsafe_get(row, col)/norm;
				double right = 0;
				for (int i = 0; i < 4; i++) {
					right += h.unsafe_get(i, col)*mult[i];
				}
				right = mult[row]*right/norm3;

				output.unsafe_set(row, col, left - right);
			}
		}
	}

	@Override public void toMatrix( DMatrixRMaj r ) {
		ConvertRotation3D_F64.quaternionToMatrix(quat, r);
	}

	@Override public void matrixToParam( DMatrixRMaj r, double[] array, int offset ) {
		// Use a temporary 'quat' here so that we don't modify the state of this object
		var quat = new Quaternion_F64();

		// Perform the conversion
		ConvertRotation3D_F64.matrixToQuaternion(r, quat);

		// copy the results into the array
		array[offset] = quat.w;
		array[offset + 1] = quat.x;
		array[offset + 2] = quat.y;
		array[offset + 3] = quat.z;

		if (!isValid(array, offset)) {
			double det = CommonOps_DDRM.det(r);
			ConvertRotation3D_F64.matrixToQuaternion(r, quat);
			throw new RuntimeException("Invalid here");
		}
	}

	@Override public void matrixJacobians( DogArray<DMatrixRMaj> output ) {
		output.resize(4);

		final double w = quat.w;
		final double x = quat.x;
		final double y = quat.y;
		final double z = quat.z;

		DMatrixRMaj dw = output.get(0);
		dw.reshape(3, 3);
		dw.data[0] = 2*w;           // (0,0)
		dw.data[1] = -2.0*z;        // (0,1)
		dw.data[2] = 2.0*y;         // (0,2)

		dw.data[3] = 2.0*z;         // (1,0)
		dw.data[4] = 2.0*w;         // (1,1)
		dw.data[5] = -2.0*x;        // (1,2)

		dw.data[6] = -2.0*y;        // (2,0)
		dw.data[7] = 2.0*x;         // (2,1)
		dw.data[8] = 2.0*w;         // (2,2)

		DMatrixRMaj dx = output.get(1);
		dx.reshape(3, 3);
		dx.data[0] = 2.0*x;         // (0,0)
		dx.data[1] = 2.0*y;         // (0,1)
		dx.data[2] = 2.0*z;         // (0,2)

		dx.data[3] = 2.0*y;         // (1,0)
		dx.data[4] = -2.0*x;        // (1,1)
		dx.data[5] = -2.0*w;        // (1,2)

		dx.data[6] = 2.0*z;         // (2,0)
		dx.data[7] = 2.0*w;         // (2,1)
		dx.data[8] = -2.0*x;        // (2,2)

		DMatrixRMaj dy = output.get(2);
		dy.reshape(3, 3);
		dy.data[0] = -2.0*y;        // (0,0)
		dy.data[1] = 2.0*x;         // (0,1)
		dy.data[2] = 2.0*w;         // (0,2)

		dy.data[3] = 2.0*x;         // (1,0)
		dy.data[4] = 2.0*y;         // (1,1)
		dy.data[5] = 2.0*z;         // (1,2)

		dy.data[6] = -2.0*w;        // (2,0)
		dy.data[7] = 2.0*z;         // (2,1)
		dy.data[8] = -2.0*y;        // (2,2)

		DMatrixRMaj dz = output.get(3);
		dz.reshape(3, 3);
		dz.data[0] = -2.0*z;        // (0,0)
		dz.data[1] = -2.0*w;        // (0,1)
		dz.data[2] = 2.0*x;         // (0,2)

		dz.data[3] = 2.0*w;         // (1,0)
		dz.data[4] = -2.0*z;        // (1,1)
		dz.data[5] = 2.0*y;         // (1,2)

		dz.data[6] = 2.0*x;         // (2,0)
		dz.data[7] = 2.0*y;         // (2,1)
		dz.data[8] = 2.0*z;         // (2,2)
	}

	@Override public void normalizeParams( double[] params, int offset ) {
		quat.w = params[offset];
		quat.x = params[offset + 1];
		quat.y = params[offset + 2];
		quat.z = params[offset + 3];

		quat.normalize();

		params[offset] = quat.w;
		params[offset + 1] = quat.x;
		params[offset + 2] = quat.y;
		params[offset + 3] = quat.z;

		if (!isValid(params, offset)) {
			throw new RuntimeException("BAD HERE");
		}
	}

	/// Computes the angle difference between two unit quaternions
	@Override public double angleError( double[] paramsB ) {
		if (debug && !isNormalized(paramsB, 0))
			throw new IllegalArgumentException("Not normalized");

		double dot = quat.w*paramsB[0] + quat.x*paramsB[1] + quat.y*paramsB[2] + quat.z*paramsB[3];

		// domain is from -1 to 1.
		if (dot >= 1.0) {
			return 0.0;
		} else if (dot <= -1.0) {
			return 2.0*Math.PI;
		}

		return 2.0*Math.acos(dot);
	}

	@Override public void angleErrorJacobian( double[] paramsB, DMatrixRMaj outputA, DMatrixRMaj outputB ) {
		outputA.reshape(1, 4);
		outputB.reshape(1, 4);

		double dot = quat.w*paramsB[0] + quat.x*paramsB[1] + quat.y*paramsB[2] + quat.z*paramsB[3];

		// There is no error and all derivatives are zero. Pathological case
		if (Math.abs(dot) >= 1.0) {
			CommonOps_DDRM.fill(outputA, 0.0);
			CommonOps_DDRM.fill(outputB, 0.0);
			return;
		}

		// @ acos(x) / @x = -(1 - x*x)^-0.5
		double dAcos = -1.0/Math.sqrt(1.0 - dot*dot);

		// @ |x| / @x
		double scale = 2.0*dAcos;

		// partial relative to paramsA
		for (int i = 0; i < 4; i++) {
			outputA.data[i] = scale*paramsB[i];
		}

		// partial relative to paramsB
		outputB.data[0] = scale*quat.w;
		outputB.data[1] = scale*quat.x;
		outputB.data[2] = scale*quat.y;
		outputB.data[3] = scale*quat.z;
	}

	@Override public void residual( double[] paramsB, int offsetB, double[] residuals, int offset ) {
		if (debug && !isNormalized(paramsB, offsetB))
			throw new IllegalArgumentException("Not normalized");

		// Assume unit quaternions for input
		// inv(a)*b
		// inv(a) == w - x*i - y*j - z*k, where (i,j,k) are coordinate axis
		// inv(a)*b = (wc, xc, yc, zc)

		double wa = quat.w, xa = quat.x, ya = quat.y, za = quat.z;
		double wb = paramsB[offsetB], xb = paramsB[offsetB + 1], yb = paramsB[offsetB + 2], zb = paramsB[offsetB + 3];

		// Multiply inverse of 'a' with quaternion 'b'
		// The resulting quaternion will be a unit quaternion, to within floating point precision
		double wc = wa*wb + xa*xb + ya*yb + za*zb;
		double xc = wa*xb - xa*wb - ya*zb + za*yb;
		double yc = wa*yb + xa*zb - ya*wb - za*xb;
		double zc = wa*zb - xa*yb + ya*xb - za*wb;

		// handle numerical rounding error. It can be slightly above or below 1.0 and mess things up badly
		if (wc > 1.0) wc = 1.0;
		if (wc < -1.0) wc = -1.0;

		// normalization for (xc, yc, yz).
		double norm = Math.sqrt(xc*xc + yc*yc + zc*zc);
		// Note: this formulation seems to produce smaller numbers than sqrt(1 - wc*wc) and has fewer issues
		//       A more exhaustive study might be worth it.

		// the formula becomes unstable for very small norm values, this is almost zero
		if (norm < 1e-8) {
			// This simplified formula comes from assuming that theta/norm ~= 1.0
			residuals[offset] = 2.0*xc;
			residuals[offset + 1] = 2.0*yc;
			residuals[offset + 2] = 2.0*zc;
			return;
		}

		double theta = 2.0*Math.acos(wc);
		residuals[offset] = xc*theta/norm;
		residuals[offset + 1] = yc*theta/norm;
		residuals[offset + 2] = zc*theta/norm;
	}

	@Override public void residualJacobian( double[] paramsB, int offsetB, DMatrixRMaj jacA, DMatrixRMaj jacB ) {
		jacA.reshape(3, 4);
		jacB.reshape(3, 4);

		// Handle an edge case
		if (quat.w == 1.0 && paramsB[offsetB] == 1.0) {
			invAB_da.zero();
			invAB_db.zero();
			return;
		}

		double wa = quat.w, xa = quat.x, ya = quat.y, za = quat.z;
		double wb = paramsB[offsetB], xb = paramsB[offsetB + 1], yb = paramsB[offsetB + 2], zb = paramsB[offsetB + 3];

		// Multiply inverse of 'a' with quaternion 'b'
		double wc = qc[0] = wa*wb + xa*xb + ya*yb + za*zb;
		double xc = qc[1] = wa*xb - xa*wb - ya*zb + za*yb;
		double yc = qc[2] = wa*yb + xa*zb - ya*wb - za*xb;
		double zc = qc[3] = wa*zb - xa*yb + ya*xb - za*wb;

		// handle numerical rounding error. It can be slightly above or below 1.0 and mess things up badly
		if (wc > 1.0) wc = 1.0;
		if (wc < -1.0) wc = -1.0;

		// Convert the quaternion above into 3-dof axis-angle
		double theta = 2.0*Math.acos(wc);
		double norm = Math.sqrt(xc*xc + yc*yc + zc*zc);
		double norm3 = norm*norm*norm;

		// @ acos(x) / @x = -(1 - x*x)^-0.5
		double theta_dwc = -2.0/norm;

		// Jacobians of inv(a)*b
		// @formatter:off
		invAB_da.data[0 ] =  wb; invAB_da.data[1 ] =  xb; invAB_da.data[2 ] =  yb; invAB_da.data[3 ] =  zb;
		invAB_da.data[4 ] =  xb; invAB_da.data[5 ] = -wb; invAB_da.data[6 ] = -zb; invAB_da.data[7 ] =  yb;
		invAB_da.data[8 ] =  yb; invAB_da.data[9 ] =  zb; invAB_da.data[10] = -wb; invAB_da.data[11] = -xb;
		invAB_da.data[12] =  zb; invAB_da.data[13] = -yb; invAB_da.data[14] =  xb; invAB_da.data[15] = -wb;

		invAB_db.data[0 ] =  wa; invAB_db.data[1 ] =  xa; invAB_db.data[2 ] =  ya; invAB_db.data[3 ] =  za;
		invAB_db.data[4 ] = -xa; invAB_db.data[5 ] =  wa; invAB_db.data[6 ] =  za; invAB_db.data[7 ] = -ya;
		invAB_db.data[8 ] = -ya; invAB_db.data[9 ] = -za; invAB_db.data[10] =  wa; invAB_db.data[11] =  xa;
		invAB_db.data[12] = -za; invAB_db.data[13] =  ya; invAB_db.data[14] = -xa; invAB_db.data[15] =  wa;
		// @formatter:on

		// See residual() for an explanation of what's going on here
		if (norm < 1e-8) {
			for (int row = 0; row < 3; row++) {
				for (int col = 0; col < 4; col++) {
					jacA.unsafe_set(row, col, 2.0*invAB_da.unsafe_get(row + 1, col));
					jacB.unsafe_set(row, col, 2.0*invAB_db.unsafe_get(row + 1, col));
				}
			}
			return;
		}

		for (int i = 0; i < 4; i++) {
			// partial of theta/norm
			double thetaNorm_da = theta_dwc*invAB_da.unsafe_get(0, i)/norm;
			for (int j = 1; j < 4; j++) {
				thetaNorm_da += -theta*qc[j]/norm3*invAB_da.unsafe_get(j, i);
			}

			double thetaNorm_db = theta_dwc*invAB_db.unsafe_get(0, i)/norm;
			for (int j = 1; j < 4; j++) {
				thetaNorm_db += -theta*qc[j]/norm3*invAB_db.unsafe_get(j, i);
			}

			for (int row = 0; row < 3; row++) {
				double res_da = invAB_da.unsafe_get(row + 1, i)*theta/norm + qc[row + 1]*thetaNorm_da;
				jacA.unsafe_set(row, i, res_da);

				double res_db = invAB_db.unsafe_get(row + 1, i)*theta/norm + qc[row + 1]*thetaNorm_db;
				jacB.unsafe_set(row, i, res_db);
			}
		}
	}

	@Override public void invert( double[] input, int offsetInput, double[] output, int offsetOutput ) {
		output[offsetOutput++] = input[offsetInput++];
		output[offsetOutput++] = -input[offsetInput++];
		output[offsetOutput++] = -input[offsetInput++];
		output[offsetOutput] = -input[offsetInput];
	}

	@Override public void invertJacobian( double[] input, int offset, DMatrixRMaj jac ) {
		jac.reshape(4, 4);
		jac.zero();

		jac.unsafe_set(0, 0, 1);
		for (int i = 1; i < 4; i++) {
			jac.unsafe_set(i, i, -1);
		}
	}

	public static boolean isNormalized( double[] array, int offset ) {
		double sum = 0.0;
		for (int i = 0; i < 4; i++) {
			double d = array[offset + i];
			sum += d*d;
		}
		return Math.abs(sum - 1.0) < 1e-6;
	}

	@Override public int residualLength() {return 3;}

	@Override public int length() {return 4;}
}
