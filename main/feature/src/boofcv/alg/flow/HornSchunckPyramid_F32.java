/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.flow;

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.struct.image.ImageFloat32;

/**
 * Implementation of {@link boofcv.alg.flow.HornSchunckPyramid} for {@link boofcv.struct.image.ImageUInt8}.
 *
 * @author Peter Abeles
 */
public class HornSchunckPyramid_F32 extends HornSchunckPyramid<ImageFloat32,ImageFloat32>{


	public HornSchunckPyramid_F32(float alpha, float w, int maxIterations, float convergeTolerance,
								  InterpolatePixelS<ImageFloat32> interp) {
		super(alpha, w, maxIterations, convergeTolerance, interp);
	}

	/**
	 * Computes the flow for a layer using Taylor series expansion and Successive Over-Relaxation linear solver.
	 * Flow estimates from previous layers are feed into this by setting initFlow and flow to their values.
	 */
	@Override
	protected void processLayer( ImageFloat32 image1 , ImageFloat32 image2 , ImageFloat32 derivX2 , ImageFloat32 derivY2) {

		float uf,vf;

		// outer Taylor expansion iterations
		for( int iter = 0; iter < maxIterations; iter++ ) {

			float error = 0;

			// inner SOR iteration.
			for( int y = 0; y < image1.height; y++ ) {
				int pixelIndex = y*image1.width;
				for (int x = 0; x < image1.width; x++, pixelIndex++ ) {
					float ui = initFlowX.data[pixelIndex];
					float vi = initFlowY.data[pixelIndex];

					float u = flowX.data[pixelIndex];
					float v = flowY.data[pixelIndex];

					float I1 = image1.data[pixelIndex];
					float I2 = image2.data[pixelIndex];

					float I2x = derivX2.data[pixelIndex];
					float I2y = derivY2.data[pixelIndex];

					float AU = A(x,y,flowX);
					float AV = A(x,y,flowY);

					flowX.data[pixelIndex] = uf = (1-w)*u + w*((I1-I2+I2x*ui - I2y*(v-vi))*I2x + alpha2*AU)/(I2x*I2x + alpha2);
					flowY.data[pixelIndex] = vf = (1-w)*v + w*((I1-I2+I2y*vi - I2x*(u-ui))*I2y + alpha2*AV)/(I2y*I2y + alpha2);

					if( Float.isNaN(uf) || Float.isNaN(vf)) {
						System.out.println();
					}

					error += (uf - u)*(uf - u) + (vf - v)*(vf - v);
				}
			}

			// see if it has converged to a solution
			if( error < convergeTolerance*image1.width*image1.height) {
				break;
			}
		}
	}

}
