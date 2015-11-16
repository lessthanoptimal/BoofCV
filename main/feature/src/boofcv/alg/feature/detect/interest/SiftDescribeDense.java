/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.interest;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.core.image.border.BorderType;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageFloat32;
import org.ddogleg.struct.GrowQueue_F64;

import java.util.Arrays;

/**
 * @author Peter Abeles
 */
// TODO move orientation estimator into its own class in orientation package
	// orientation histogram dense
public class SiftDescribeDense {

	float orientationSigmaScale;
	int numBinsOri;

	ImageGradient<ImageFloat32,ImageFloat32> gradient = FactoryDerivative.three_F32();

	ImageFloat32 input;
	ImageFloat32 derivX = new ImageFloat32(1,1);
	ImageFloat32 derivY = new ImageFloat32(1,1);
	InterpolatePixelS<ImageFloat32> interpX =
			FactoryInterpolation.bilinearPixelS(ImageFloat32.class, BorderType.EXTENDED);
	InterpolatePixelS<ImageFloat32> interpY =
			FactoryInterpolation.bilinearPixelS(ImageFloat32.class, BorderType.EXTENDED);

	public void setInput( ImageFloat32 input ) {
		this.input = input;

		derivX.reshape(input.width,input.height);
		derivY.reshape(input.width,input.height);

		gradient.process(input,derivX,derivY);

		interpX.setImage(derivX);
		interpY.setImage(derivY);

	}

	public void computeDescriptor( float c_x , float c_y , float sigma , float orientation ) {

	}

	public void selectOrientations( double histogram[] , GrowQueue_F64 output ) {
		double peakValue = 0;

		for (int i = 0; i < numBinsOri; i++) {
			double d = histogram[i];

			if( d > peakValue) {
				peakValue = d;
			}
		}

		peakValue *= 0.8;

		for (int i = numBinsOri-2,j=numBinsOri-1,k=0; k < numBinsOri; i=j,j=k,k++) {
			double v0 = histogram[i];
			double v1 = histogram[j];
			double v2 = histogram[k];

			if( v1 >= peakValue && v1 > v0 && v1 > v2 ) {
				double angle = j*Math.PI/numBinsOri;

//				double total =
//
//				// TODO do some sort of interpolation here
//				output.add( estimatedValue );
			}
		}
	}

	public void orientationHistogram( float c_x , float c_y , float sigma , double histogram[] ) {

		// initialize the histogram to all zeros
		Arrays.fill(histogram,0);

		// adjust sigma for scale computation
		sigma *= orientationSigmaScale;
		// how many pixels away it needs to sample
		float r = 3*sigma;

		int x0 = (int)Math.floor(c_x - r    );
		int y0 = (int)Math.floor(c_y - r    );
		int x1 = (int)Math.ceil( c_x - r + 1);
		int y1 = (int)Math.ceil( c_y - r + 1);

		if( x0 < 0 ) x0 = 0;
		if( y0 < 0 ) y0 = 0;
		if( x1 >= input.width ) x1 = input.width-1;
		if( y1 >= input.height ) y1 = input.height-1;

		for (int y = y0; y < y1; y++) {
			double dy = y-c_y;
			for (int x = x0; x < x1; x++) {
				int imageIndex = derivX.getIndex(x,y);

				double dx = x-c_x;
				double distance = Math.sqrt(dx*dx + dy*dy);
				double weight = gaussian(distance, sigma);
				double dX = derivX.data[imageIndex];
				double dY = derivY.data[imageIndex];

				double m = Math.sqrt(dX*dX + dY*dY);
				double theta = Math.atan2(dY, dX)+Math.PI;

				int bin = (int)(theta*numBinsOri/(2.0*Math.PI));
				if( bin == numBinsOri )
					bin--;

				histogram[bin] += weight*m;
			}
		}
	}

	private double gaussian( double distance , double sigma ) {
		return Math.exp(-distance/(2.0f*sigma*sigma));
	}
}
