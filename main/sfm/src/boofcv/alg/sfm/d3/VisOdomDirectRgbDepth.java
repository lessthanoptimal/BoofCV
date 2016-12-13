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

package boofcv.alg.sfm.d3;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.abst.sfm.ImagePixelTo3D;
import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.core.image.FactoryGImageMultiBand;
import boofcv.core.image.GImageMultiBand;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point3D_F32;
import georegression.struct.se.Se3_F32;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F32;
import org.ddogleg.struct.FastQueue;
import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.LinearSolverFactory;
import org.ejml.interfaces.linsol.LinearSolver;

/**
 * TODO Fill in
 *
 * @author Peter Abeles
 */
public class VisOdomDirectRgbDepth<I extends ImageBase<I>, D extends ImageBase<D>>
{
	ImageType<I> imageType;
	ImageType<D> derivType;

	LinearSolver<DenseMatrix64F> solver;
	DenseMatrix64F A = new DenseMatrix64F(1,1);
	DenseMatrix64F y = new DenseMatrix64F(1,1);
	DenseMatrix64F twistMatrix = new DenseMatrix64F(6,1);

	ImageGradient<I,D> computeD;

	InterpolatePixelMB<I> interpI;

	GImageMultiBand wrapI;
	GImageMultiBand wrapDerivX;
	GImageMultiBand wrapDerivY;

	float current[];
	float dx[];
	float dy[];


	// gradient of key-frame
	D derivX, derivY;

	FastQueue<Pixel> keypixels;

	/** focal length along x and y axis (units: pixels) */
	float fx,fy;
	/** image center (units: pixels) */
	float cx,cy;

	boolean hasKeyFrame = false;

	public VisOdomDirectRgbDepth(ImageType<I> imageType , ImageType<D> derivType ) {

		this.imageType = imageType;
		this.derivType = derivType;

		wrapI = FactoryGImageMultiBand.create(imageType);
		wrapDerivX = FactoryGImageMultiBand.create(derivType);
		wrapDerivY = FactoryGImageMultiBand.create(derivType);

		final int numBands = imageType.getNumBands();

		current = new float[ numBands ];
		dx = new float[ numBands ];
		dy = new float[ numBands ];

		keypixels = new FastQueue<Pixel>() {
			@Override
			protected Pixel createInstance() {
				return new Pixel(numBands);
			}
		};
	}

	public void reset() {

	}

	public boolean process(I input , ImagePixelTo3D pixelTo3D , Se3_F64 hint ) {
		if( solver == null ) {
			solver = LinearSolverFactory.qr(input.width*input.height,6);
		}

		// TODO Use the initial hint

		// TODO How to select when a new keyframe is needed?

		if( hasKeyFrame ) {
			// TODO Convert into a pyramid approach
			// TODO iterate
			constructLinearSystem(input);
			if( !solveSystem())
				return false;
		} else {
			setKeyFrame(input, pixelTo3D);
		}
		return true;
	}

	private void constructLinearSystem(I input ) {
		computeD.process(input,derivX,derivY);
		wrapDerivX.wrap(derivX);
		wrapDerivY.wrap(derivY);

		wrapI.wrap(input);
		int N = input.width*input.height;

		int numBands = imageType.getNumBands();
		A.reshape(N*numBands,6);
		y.reshape(N*numBands,1);

		Se3_F32 g = new Se3_F32();
		Point3D_F32 S = new Point3D_F32();

		int row = 0;
		for (int i = 0; i < keypixels.size(); i++) {
			Pixel p = keypixels.data[i];

			// Apply the known warp
			SePointOps_F32.transform(g,p.X,S);

			float Sx = S.x;
			float Sy = S.y;
			float Sz = S.z;

			// pi matrix derivative relative to S
			float ZZ   = Sz*Sz;
			float dP11 = fx/Sz;
			float dP13 = -Sx*fx/ZZ;
			float dP22 = fy/Sz;
			float dP23 = -Sy*fy/ZZ;

			wrapI.get(p.x,p.y, current);
			wrapDerivX.get(p.x,p.y, dx);
			wrapDerivY.get(p.x,p.y, dy);

			for (int band = 0; band < numBands; band++, row++) {
				float bandDx = dx[band];
				float bandDy = dy[band];

				// B = grad^T * dPI/dt = shape(1,3)
				float b1 = bandDx*dP11;
				float b2 = bandDy*dP22;
				float b3 = bandDx*dP13 + bandDy*dP23;


				// C * A(S'(x)) = shape(1,6)
				int indexA = row*6;
				A.data[indexA++] = -b2*Sz + b2*Sy;
				A.data[indexA++] =  b1*Sz - b3*Sx;
				A.data[indexA++] = -b1*Sy + b2*Sx;
				A.data[indexA++] = b1;
				A.data[indexA++] = b2;
				A.data[indexA  ] = b3;

				y.data[row] = -(current[band] - p.bands[band]);
			}
		}
	}

	private boolean solveSystem() {
		if( !solver.setA(A))
			return false;

		solver.solve(y,twistMatrix);

		// TODO twist to SE(3)

		return true;
	}

	private void setKeyFrame(I input, ImagePixelTo3D pixelTo3D) {
		derivX.reshape(input.width,input.height);
		derivY.reshape(input.width,input.height);

		wrapI.wrap(input);
		keypixels.reset();

		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {
				// See if there's a valid 3D point at this location
				if( !pixelTo3D.process(x,y) ) {
					continue;
				}

				float P_x = (float)pixelTo3D.getX();
				float P_y = (float)pixelTo3D.getY();
				float P_z = (float)pixelTo3D.getZ();
				float P_w = (float)pixelTo3D.getW();

				// skip point if it's at infinity or has a negative value
				if( P_w <= 0 )
					continue;

				// save the results
				Pixel p = keypixels.grow();
				wrapI.get(x,y,p.bands);

				p.x = x;
				p.y = y;
				p.X.set(P_x/P_w,P_y/P_w,P_z/P_w);
			}
		}
	}

	private static class Pixel {
		float bands[]; // pixel intensity in each band
		int x,y; // pixel coordinate
		Point3D_F32 X = new Point3D_F32(); // world coordinate

		public Pixel( int numBands ) {
			bands = new float[numBands];
		}
	}

	public ImageType<I> getImageType() {
		return imageType;
	}

	public ImageType<D> getDerivType() {
		return derivType;
	}
}
