/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.chess;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.filter.derivative.DerivativeType;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.border.BorderType;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;

/**
 * @author Peter Abeles
 */
public class FindTwoXCornerLines {
	// computer gradient for all pixels inside NxN region
	// for all lines compute the sum. Use abs of dot product of gradient and line normal
	// Find two best lines that form an X
	// Compute total gradient absorbed by the two lines

	final ImageGradient<GrayF32,GrayF32> gradient;

	// Size of local square region
	final int width,radius;

	// Storage for local gradient
	final GrayF32 derivX = new GrayF32(1,1);
	final GrayF32 derivY = new GrayF32(1,1);

	// Storage for sub images. Needed to handle image border
	final GrayF32 input_sub = new GrayF32(1,1);
	final GrayF32 derivX_sub = new GrayF32(1,1);
	final GrayF32 derivY_sub = new GrayF32(1,1);

	// Used to sample gradient at sub pixel
	final InterpolatePixelS<GrayF32> interpX = FactoryInterpolation.bilinearPixelS(GrayF32.class,BorderType.EXTENDED);
	final InterpolatePixelS<GrayF32> interpY = FactoryInterpolation.bilinearPixelS(GrayF32.class,BorderType.EXTENDED);

	int numberOfLines = 20;
	float[] tcos = new float[numberOfLines];
	float[] tsin = new float[numberOfLines];

	float[] lineStrength = new float[numberOfLines];

	public FindTwoXCornerLines( int radius ) {
		ImageType<GrayF32> imageType = ImageType.single(GrayF32.class);
		ImageType<GrayF32> derivType = GImageDerivativeOps.getDerivativeType(imageType);

		this.width = 2*radius+1;
		this.radius = radius;
		this.gradient = FactoryDerivative.gradient(DerivativeType.SOBEL, imageType,derivType);

		this.derivX.reshape(width,width);
		this.derivY.reshape(width,width);

		this.interpX.setImage(derivX);
		this.interpY.setImage(derivY);

		for (int i = 0; i < numberOfLines; i++) {
			double theta = Math.PI*i/(double)(numberOfLines) - Math.PI/2;
			tcos[i] = (float)Math.cos(theta);
			tsin[i] = (float)Math.sin(theta);
		}
	}

	public void process( GrayF32 input , int cx , int cy ) {

		computeLocalGradient(input, cx, cy);


		for (int lineIdx = 0; lineIdx < numberOfLines; lineIdx++) {
			float c = tcos[lineIdx];
			float s = tsin[lineIdx];

			float xx = radius - c*radius;
			float yy = radius - s*radius;

			// magnitude of gradient
			float magnitude = 0;

			for (int i = 0; i < width; i++) {
				// get gradient at this point along the line
				float dx = interpX.get(xx,yy);
				float dy = interpY.get(xx,yy);

				// compute magnitude of dot product along the line's tangent
				magnitude += Math.abs(dx*s - dy*c);

				xx += c;
				yy += s;
			}

			lineStrength[lineIdx] = magnitude;
		}

		System.out.println("Done");
	}

	private void computeLocalGradient(GrayF32 input, int cx, int cy) {
		int x0 = cx-radius, y0 = cy-radius;
		int x1 = cx+radius+1, y1 = cy+radius+1;

		int offX=0,offY=0;
		if( x0 < 0 ) {
			offX = -x0;
			x0 = 0;
		}
		if( y0 < 0 ) {
			offY = -y0;
			y0 = 0;
		}
		if( x1 > input.width )
			x1 = input.width;
		if( y1 > input.height )
			y1 = input.height;
		int lengthX = x1-x0;
		int lengthY = y1-y0;

		// force pixels outside the image to have a gradient of zero
		if( lengthX != width || lengthY != width ) {
			GImageMiscOps.fill(derivX,0);
			GImageMiscOps.fill(derivY,0);
		}

		// create the sub images and compute the gradient
		input.subimage(x0,y0,x1,y1,input_sub);
		derivX.subimage(offX,offY,offX+lengthX,offY+lengthY,derivX_sub);
		derivY.subimage(offX,offY,offX+lengthX,offY+lengthY,derivY_sub);
		this.gradient.process(input_sub,derivX_sub,derivY_sub);
	}
}
