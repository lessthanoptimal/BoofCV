/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.feature.detect.calibgrid;

import boofcv.core.image.FactoryGeneralizedSingleBand;
import boofcv.core.image.GImageSingleBand;
import boofcv.struct.FastQueue;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.line.LineParametric2D_F64;
import georegression.struct.point.Point2D_F64;

/**
 * <p>
 * Computes a corner's location by finding the intersection of lines defined at each pixel using their
 * location and gradient.  Each pixel defines  line based on its location and the gradient, where its location
 * is a point on the line and the gradient is tangential to the line's slope.   If a pixel has a gradient of zero
 * then it is ignored.  The gradient's magnitude determine's its weight when computing the solution. In theory noise
 * will have a much smaller magnitude and average out.
 * </p>
 *
 * <p>
 * The basic assumption that each pixel represents a line that intersects at the corner is broken at corner pixels.
 * Here the gradient will be pointed at an acute angle relative to the corner's sides.  This introduces an error
 * into the solution, even when the gradient is computed from a noise free image.
 * </p>
 *
 * <p>
 * How the gradient is computed can also introduce a bias in the corner location.  For example a 3x3 gradient kernel
 * will cause a shift of 1/2 a pixel.
 * </p>
 *
 * @author Peter Abeles
 */
public class RefineCornerEstimate<D extends ImageSingleBand> {

	// where the corner was found to be
	private Point2D_F64 found = new Point2D_F64();

	// algorithm to compute corner location
	private FastQueue<LineParametric2D_F64> lines = new FastQueue<LineParametric2D_F64>(50,LineParametric2D_F64.class,true);

	// gradient, using a horrible slow image interface
	private GImageSingleBand derivX,derivY;

	// the edge detector will cause a shift from the true edge location
	// this offset will counteract that offset
	private double edgeOffset;

	public RefineCornerEstimate(double edgeOffset) {
		this.edgeOffset = edgeOffset;
	}

	public void setInputs( D derivX , D derivY ) {
		this.derivX = FactoryGeneralizedSingleBand.wrap(derivX);
		this.derivY = FactoryGeneralizedSingleBand.wrap(derivY);
	}
	
	public boolean process( int x0 , int y0 , int x1, int y1 )
	{
		lines.reset();
		for( int y = y0; y < y1; y++ ) {
			for( int x = x0; x < x1; x++ ) {
				double dx = derivX.get(x,y).doubleValue();
				double dy = derivY.get(x,y).doubleValue();

				// skip pixels with no gradient since they contain no information
				if( dx == 0 && dy == 0 )
					continue;

				LineParametric2D_F64 l = lines.pop();
				l.setPoint(x,y);
				l.setSlope(-dy,-dx);
				System.out.println(l.getX()+" "+l.getY()+"  slope = "+l.getSlopeX()+" "+l.getSlopeY());
			}
		}

		IntersectLinesLinear estimator = new IntersectLinesLinear();
		if( !estimator.process(lines.toList()) ) {
			return false;
		}

		found.set(estimator.getPoint());

		found.x += edgeOffset;
		found.y += edgeOffset;

		return true;
	}

	public double getX() {
		return found.x;
	}

	public double getY() {
		return found.y;
	}
}
