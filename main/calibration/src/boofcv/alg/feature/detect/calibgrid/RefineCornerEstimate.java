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
import boofcv.numerics.fitting.modelset.DistanceFromModel;
import boofcv.numerics.fitting.modelset.ModelFitter;
import boofcv.numerics.fitting.modelset.ransac.SimpleInlierRansac;
import boofcv.struct.FastQueue;
import boofcv.struct.image.ImageSingleBand;
import georegression.metric.Distance2D_F64;
import georegression.struct.line.LineParametric2D_F64;
import georegression.struct.point.Point2D_F64;

import java.util.List;

/**
 * @author Peter Abeles
 */
public class RefineCornerEstimate<D extends ImageSingleBand> {
	
	Point2D_F64 found = new Point2D_F64();

	FastQueue<LineParametric2D_F64> lines = new FastQueue<LineParametric2D_F64>(50,LineParametric2D_F64.class,true);

	// gradient, using a horrible slow image interface
	GImageSingleBand derivX,derivY;

	public void setInputs( D derivX , D derivY ) {
		this.derivX = FactoryGeneralizedSingleBand.wrap(derivX);
		this.derivY = FactoryGeneralizedSingleBand.wrap(derivY);
	}
	
	public boolean process( int x0 , int y0 , int x1, int y1 ) {
		
		int x_c = 0;//(x1+x0)/2;
		int y_c = 0;//(y1+y0)/2;

		lines.reset();
		for( int y = y0; y < y1; y++ ) {
			for( int x = x0; x < x1; x++ ) {
				double dx = derivX.get(x,y).doubleValue();
				double dy = derivY.get(x,y).doubleValue();

				// skip pixels with no gradient since they contain no information
				if( dx == 0 && dy == 0 )
					continue;
				
//				if( Math.abs(dx) < 100 && Math.abs(dy) < 100 )
//					continue;

				LineParametric2D_F64 l = lines.pop();
				l.setPoint(x-x_c,y-y_c);
				l.setSlope(-dy,-dx);
				System.out.println(l.getX()+" "+l.getY()+"  slope = "+l.getSlopeX()+" "+l.getSlopeY());
			}
		}

		IntersectLinesLinear estimator = new IntersectLinesLinear();
		estimator.process(lines.toList());
		
		System.out.println("BATCH: "+estimator.getPoint().toString());
		
		if( !ransac(1, x_c, y_c))
			return false;

		return true;
	}

	private boolean ransac( double inlierThreshold , int x_c , int y_c ) {

		HelperFitter fitter = new HelperFitter();
		HelperDistance dist = new HelperDistance();

		SimpleInlierRansac<Point2D_F64,LineParametric2D_F64> ransac =
				new SimpleInlierRansac<Point2D_F64, LineParametric2D_F64>(234234,fitter,dist,
						100,2,2,99999,inlierThreshold);

		List<LineParametric2D_F64> list = lines.toList();
		if( !ransac.process(list,null) )
			return false;

		found.set(ransac.getModel());

		found.x += x_c;
		found.y += y_c;
		
		System.out.println("inlier size "+ransac.getMatchSet().size()+"  input "+list.size());

		return true;
	}
	
	public double getX() {
		return found.getX();
	}

	public double getY() {
		return found.getY();
	}

	private static class HelperFitter implements ModelFitter<Point2D_F64,LineParametric2D_F64>
	{
		IntersectLinesLinear estimator = new IntersectLinesLinear();
		
		@Override
		public Point2D_F64 declareModel() {
			return new Point2D_F64();
		}

		@Override
		public boolean fitModel(List<LineParametric2D_F64> dataSet,
								Point2D_F64 initParam,
								Point2D_F64 foundModel) {
			if( !estimator.process(dataSet) )
				return false;
			
			foundModel.set(estimator.getPoint());
			
			return true;
		}

		@Override
		public int getMinimumPoints() {
			return 2;
		}
	}
	
	private static class HelperDistance implements DistanceFromModel<Point2D_F64,LineParametric2D_F64>
	{
		Point2D_F64 model = new Point2D_F64();

		@Override
		public void setModel(Point2D_F64 p) {
			model.set(p);
		}

		@Override
		public double computeDistance(LineParametric2D_F64 pt) {

			return Distance2D_F64.distance(pt,model);
		}

		@Override
		public void computeDistance(List<LineParametric2D_F64> list, double[] distance) {

			for( int i = 0; i < list.size(); i++ ) {
				LineParametric2D_F64 pt = list.get(i);

				distance[i] = Distance2D_F64.distance(pt,model);
			}
		}
	}
	
}
