/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.calibration;

import georegression.struct.point.Point2D_F64;

/**
 * Parameters for adjusting the usual parameters and observed point location for zhang 98
 * 
 * @author Peter Abeles
 */
public class ParametersPointsZhang99 extends Zhang99Parameters {
	// number of calibration points in each view
	int numCalibrationPoints;
	
	// location of each point in the image
	Point2D_F64[][] points;

	public ParametersPointsZhang99( boolean assumeZeroSkew , int numDistort, int numViews, int numCalibrationPoints) {
		super(assumeZeroSkew,numDistort, numViews);
		this.numCalibrationPoints = numCalibrationPoints;
		
		points = new Point2D_F64[numViews][];
		for( int i = 0; i < numViews; i++ ) {
			Point2D_F64[] v = points[i] = new Point2D_F64[numCalibrationPoints];
			
			for( int j = 0; j < v.length; j++ ) {
				v[j] = new Point2D_F64();
			}
		}
	}

	@Override
	public ParametersPointsZhang99 createNew() {
		return new ParametersPointsZhang99(assumeZeroSkew,distortion.length,views.length,numCalibrationPoints);
	}

	@Override
	public ParametersPointsZhang99 copy() {
		ParametersPointsZhang99 ret = createNew();
		ret.a = a;
		ret.b = b;
		ret.c = c;
		ret.x0 = x0;
		ret.y0 = y0;

		for( int i = 0; i < distortion.length; i++ ) {
			ret.distortion[i] = distortion[i];
		}

		for( int i = 0; i < views.length; i++ ) {
			View a = views[i];
			View b = ret.views[i];

			b.rotation.unitAxisRotation.set(a.rotation.unitAxisRotation);
			b.rotation.theta = a.rotation.theta;
			b.T.set(a.T);
			
			Point2D_F64 pts[] = points[i];
			Point2D_F64 ptsb[] = ret.points[i];
			
			for( int j = 0; j < numCalibrationPoints; j++ ) {
				ptsb[j].set(pts[j]);
			}
		}
		
		return ret;
	}
	
	@Override
	public int size() {
		return 5+distortion.length+(4+3+numCalibrationPoints)*views.length;
	}

	@Override
	public void setFromParam( double param[] ) {
		super.setFromParam(param);
		
		int index = super.size();
		for( int viewIndex = 0; viewIndex < views.length; viewIndex++ ) {
			Point2D_F64[] pts = points[viewIndex];
			
			for( int i = 0; i < numCalibrationPoints; i++ ) {
				pts[i].set(param[index++],param[index++]);
			}
		}
	}

	@Override
	public void convertToParam( double param[] ) {
		super.convertToParam(param);

		int index = super.size();
		for( int viewIndex = 0; viewIndex < views.length; viewIndex++ ) {
			Point2D_F64[] pts = points[viewIndex];

			for( int i = 0; i < numCalibrationPoints; i++ ) {
				Point2D_F64 p = pts[i];
				param[index++] = p.x;
				param[index++] = p.y;
			}
		}
	}
}
