/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.checker;

import boofcv.abst.feature.detect.extract.FeatureExtractor;
import boofcv.abst.feature.detect.extract.GeneralFeatureDetector;
import boofcv.abst.feature.detect.intensity.GeneralFeatureIntensity;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.BorderType;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.detect.intensity.FactoryIntensityPoint;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import georegression.geometry.UtilPoint2D_I32;
import georegression.struct.point.Point2D_I16;
import georegression.struct.point.Point2D_I32;
import org.ejml.data.DenseMatrix64F;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class DetectCheckeredGrid<T extends ImageSingleBand, D extends ImageSingleBand>
{
	D derivX;
	D derivY;

	int numCols;
	int numRows;

	GeneralFeatureIntensity<T,D> intensityAlg;
	GeneralFeatureDetector<T,D> detectorAlg;
	
	DenseMatrix64F distMatrix;

	List<PointInfo> points;
	List<PointInfo> cornerCandidates;

	public DetectCheckeredGrid( int numCols , int numRows , int radius , Class<T> imageType ) {
		Class<D> derivType = GImageDerivativeOps.getDerivativeType(imageType);
		this.numCols = numCols;
		this.numRows = numRows;

		int maxPoints = numCols*numRows+5;
		distMatrix = new DenseMatrix64F(maxPoints,maxPoints);

		derivX = GeneralizedImageOps.createSingleBand(derivType,1,1);
		derivY = GeneralizedImageOps.createSingleBand(derivType,1,1);

		intensityAlg = FactoryIntensityPoint.klt(radius,true,derivType);

		FeatureExtractor extractor = FactoryFeatureExtractor.nonmax(radius,20,radius,false,true);
		detectorAlg = new GeneralFeatureDetector<T, D>(intensityAlg,extractor,maxPoints);

	}

	public boolean process( T gray ) {
		derivX.reshape(gray.width,gray.height);
		derivY.reshape(gray.width,gray.height);

		// extended border to avoid false positives
		GImageDerivativeOps.sobel(gray, derivX, derivY, BorderType.EXTENDED);

		// detect interest points
		detectorAlg.process(gray,derivX,derivY, null,null,null);

		QueueCorner corners = detectorAlg.getFeatures();
		points = convert(corners);
		
		computePointDistance(points);

		connect(points);

		cornerCandidates = findCorners(points);
		
		// find a valid grid

		// refine the position estimate

		return false;
	}

	private List<PointInfo> convert(QueueCorner found) {
		List<PointInfo> points = new ArrayList<PointInfo>();
		for( int i = 0; i < found.size(); i++ ) {
			Point2D_I16 c = found.get(i);
			points.add(new PointInfo(c.x, c.y));
		}
		return points;
	}
	
	/**
	 * Computes the distance each found point is from each other
	 */
	private void computePointDistance( List<PointInfo> found ) {
		distMatrix.reshape(found.size(), found.size(), false);
		
		for( int i = 0; i < found.size(); i++ ) {
			Point2D_I32 a = found.get(i).p;
			distMatrix.set(i, i, 0);
			for( int j = i+1; j < found.size(); j++ ) {
				Point2D_I32 b = found.get(j).p;
				
				double distance = UtilPoint2D_I32.distance(a.x,a.y,b.x,b.y);
				distMatrix.set(i,j,distance);
				distMatrix.set(j,i,distance);
			}
		}
	}

	// todo simiilar intensity?
	private void connect( List<PointInfo> found ) {
		for( int i = 0; i < found.size(); i++ ) {
			PointInfo p = found.get(i);
			
			double min = Double.MAX_VALUE;
			
			for( int j = 0; j < found.size(); j++ ) {
				if( j == i ) continue;
				
				double d = distMatrix.get(i,j);
				if( d < min ) {
					min = d;
				}
			}
			
			double tol = min*1.5;

			for( int j = 0; j < found.size(); j++ ) {
				if( j == i ) continue;

				double d = distMatrix.get(i,j);
				if( d <= tol ) {
					p.connected.add( j );
				}
			}
			System.out.println(i+"  connected "+p.connected.size());
		}
	}
	
	private List<PointInfo> findCorners( List<PointInfo> found ) {
		List<PointInfo> ret = new ArrayList<PointInfo>();
		
		for( PointInfo p : found ) {
			if( p.connected.size() <= 3 ) {
				ret.add(p);
			}
		}

		return ret;
	}

	public List<Point2D_I32> getAllPoints() {
		List<Point2D_I32> ret = new ArrayList<Point2D_I32>();

		for( int i = 0; i < points.size(); i++ ) {
			ret.add(points.get(i).p);
		}
		return ret;
	}

	public List<Point2D_I32> getCornerCandidates() {
		List<Point2D_I32> ret = new ArrayList<Point2D_I32>();
		
		for( int i = 0; i < cornerCandidates.size(); i++ ) {
			ret.add(cornerCandidates.get(i).p);
		}
		return ret;
	}

	public ImageFloat32 getIntensity() {
		return intensityAlg.getIntensity();
	}

	public static class PointInfo
	{
		Point2D_I32 p;
		List<Integer> connected = new ArrayList<Integer>();

		private PointInfo(int x , int y) {
			this.p = new Point2D_I32(x,y);
		}
	}
}
