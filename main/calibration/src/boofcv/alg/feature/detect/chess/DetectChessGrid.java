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

package boofcv.alg.feature.detect.chess;

import boofcv.abst.feature.detect.extract.FeatureExtractor;
import boofcv.abst.feature.detect.extract.GeneralFeatureDetector;
import boofcv.abst.feature.detect.intensity.GeneralFeatureIntensity;
import boofcv.alg.feature.detect.grid.UtilCalibrationGrid;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.BorderType;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.detect.intensity.FactoryIntensityPoint;
import boofcv.struct.ImageRectangle;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import georegression.geometry.UtilPoint2D_I32;
import georegression.metric.Intersection2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I16;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Polygon2D_F64;
import org.ejml.alg.dense.linsol.LinearSolver;
import org.ejml.alg.dense.linsol.LinearSolverFactory;
import org.ejml.alg.dense.linsol.LinearSolverSafe;
import org.ejml.data.DenseMatrix64F;
import pja.sorting.QuickSort_F64;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * The found control points are ensured to be returned in a row-major format with the correct number of rows and columns,
 * with a counter clockwise ordering.  Note that when viewed on the monitor this will appear to be clockwise because
 * the y-axis points down.
 *
 * @author Peter Abeles
 */
public class DetectChessGrid<T extends ImageSingleBand, D extends ImageSingleBand>
{
	D derivX;
	D derivY;

	int numColsGrid;
	int numRowsGrid;

	int numColsPoints;
	int numRowsPoints;

	FindChessBoundBinary findBound;

	GeneralFeatureIntensity<T,D> intensityAlg;
	GeneralFeatureDetector<T,D> detectorAlg;


	List<Point2D_I32> points;
	List<Point2D_F64> subpixel;

	// number of points it expects to observer in the target
	int expectedPoints;
	
	ImageUInt8 binary = new ImageUInt8(1,1);
	ImageUInt8 eroded = new ImageUInt8(1,1);
	
	// used for sub-pixel refinement
	LinearSolver<DenseMatrix64F> solver;
	DenseMatrix64F M = new DenseMatrix64F(9,6);
	DenseMatrix64F X = new DenseMatrix64F(6,1);
	DenseMatrix64F Y = new DenseMatrix64F(9,1);

	public DetectChessGrid(int numCols, int numRows, int radius, Class<T> imageType) {
		Class<D> derivType = GImageDerivativeOps.getDerivativeType(imageType);
		this.numColsGrid = numCols;
		this.numRowsGrid = numRows;

		this.numColsPoints = 2*(numCols-1);
		this.numRowsPoints = 2*(numRows-1);

		expectedPoints = numColsPoints*numRowsPoints;

		derivX = GeneralizedImageOps.createSingleBand(derivType,1,1);
		derivY = GeneralizedImageOps.createSingleBand(derivType,1,1);

		intensityAlg = FactoryIntensityPoint.klt(radius,true,derivType);

		FeatureExtractor extractor = FactoryFeatureExtractor.nonmax(radius,20,radius,false,true);
		detectorAlg = new GeneralFeatureDetector<T, D>(intensityAlg,extractor,0);


		findBound = new FindChessBoundBinary(numRows,numCols,20*4);

		// set up matrix for solving the quadratic
		int index = 0;
		for( int i = -1; i <= 1; i++ ) {
			for( int j = -1; j <= 1; j++ , index++ ) {
				M.set(index,0,j*j);
				M.set(index,1,i*j);
				M.set(index,2,i*i);
				M.set(index,3,j);
				M.set(index,4,i);
				M.set(index,5,1);
			}
		}

		solver = new LinearSolverSafe<DenseMatrix64F>(LinearSolverFactory.leastSquares(9,6));
	}

	public boolean process( T gray ) {
		binary.reshape(gray.width,gray.height);
		eroded.reshape(gray.width, gray.height);

		// TODO try different thresholds
		GThresholdImageOps.threshold(gray, binary, 89, true);

		// erode to make the squares separated
		BinaryImageOps.erode8(binary, eroded);
//		BinaryImageOps.erode8(eroded, binary);

		if( !findBound.process(eroded) )
			return false;

		List<Point2D_I32> boundary = findBound.getBoundingQuad();

		// find image rectangle and detect features inside
		ImageRectangle rect = findImageRectangle(boundary);

		T subGray = (T)gray.subimage(rect.x0,rect.y0,rect.x1,rect.y1);
		derivX.reshape(subGray.width,subGray.height);
		derivY.reshape(subGray.width,subGray.height);

		// extended border to avoid false positives
		GImageDerivativeOps.sobel(subGray, derivX, derivY, BorderType.EXTENDED);

		// detect interest points
		detectorAlg.process(gray,derivX,derivY, null,null,null);

		QueueCorner corners = detectorAlg.getFeatures();

		// put points into original image coordinates
		points = convert(corners,rect.x0,rect.y0);

		// prune features not inside the bounding quadrilateral
		pruneOutside(points,boundary);

		// select N brightest features
		points = selectBrightest(points, intensityAlg.getIntensity(),rect.x0,rect.y0);

		// put points into grid order
		List<Point2D_I32> predicted =
				PredictChessPoints.predictPoints(findBound.getCornerBlobs().get(0),
						numRowsPoints, numColsPoints);
		points = orderPoints(predicted,points);

		subpixel = new ArrayList<Point2D_F64>();
		for( Point2D_I32 p : points )
			subpixel.add( refineSubpixel(p,rect.x0,rect.y0,intensityAlg.getIntensity()));

		UtilCalibrationGrid.enforceClockwiseOrder(subpixel,numRowsPoints, numColsPoints);

		// todo refine pixel estimate

		return true;
	}

	private Point2D_F64 refineSubpixel( Point2D_I32 pt , 
										int x0 , int y0 , 
										ImageFloat32 intensity ) 
	{
		int index = 0;
		for( int i = -1; i <= 1; i++ ) {
			for( int j = -1; j <= 1; j++ , index++ ) {
				double value = intensity.get(pt.x-x0+j,pt.y-y0+i);
				
				Y.set(index,0,value);
			}
		}

		if( !solver.setA(M) )
			return new Point2D_F64(pt.x,pt.y);
		
		solver.solve(Y,X);
		
		double a = X.data[0];
		double b = X.data[1];
		double c = X.data[2];
		double d = X.data[3];
		double e = X.data[4];

		double var0 = b-4*a*c/b;
		double var1 = 2*a*e/b-d;
		
		double y = var1/var0;
		double x = (-2*c*y-e)/b;
		
		return new Point2D_F64(pt.x+x,pt.y+y);
	}
	
	private List<Point2D_I32> orderPoints( List<Point2D_I32> predicted , List<Point2D_I32> detected )
	{
		List<Point2D_I32> ret = new ArrayList<Point2D_I32>();
		
		for( Point2D_I32 p : predicted ) {
			double bestDist = Double.MAX_VALUE;
			Point2D_I32 best = null;
			
			for( Point2D_I32 d : detected ) {
				double dist = UtilPoint2D_I32.distance(p,d);
			
				if( dist < bestDist ) {
					bestDist = dist;
					best = d;
				}
			}
			
			ret.add(best);
		}

		return ret;
	}
	
	
	private ImageRectangle findImageRectangle( List<Point2D_I32> quad )
	{
		int x0,y0,x1,y1;
		
		Point2D_I32 p = quad.get(0);
		x0=x1=p.x;
		y0=y1=p.y;
		
		for( int i = 1; i < 4; i++ ) {
			p = quad.get(i);
			if( p.x < x0 )
				x0 = p.x;
			else if( p.x > x1 )
				x1 = p.x;

			if( p.y < y0 )
				y0 = p.y;
			else if( p.y > y1 )
				y1 = p.y;
		}
		
		return new ImageRectangle(x0,y0,x1,y1);
	}

	private void pruneOutside( List<Point2D_I32> corners , List<Point2D_I32> quad ) {
		Polygon2D_F64 poly = new Polygon2D_F64(4);
		for( int i = 0; i < 4; i++ ) {
			Point2D_I32 p = quad.get(i);
			poly.vertexes[i].set(p.x,p.y);
		}

		Iterator<Point2D_I32> iter = corners.iterator();

		Point2D_F64 a = new Point2D_F64();
		while( iter.hasNext() ) {
			Point2D_I32 p = iter.next();
			a.set(p.x,p.y);

			if( !Intersection2D_F64.containConvex(poly,a) )
				iter.remove();
		}
	}

	private List<Point2D_I32> convert(QueueCorner found , int x0 , int y0 ) {
		List<Point2D_I32> points = new ArrayList<Point2D_I32>();
		for( int i = 0; i < found.size(); i++ ) {
			Point2D_I16 c = found.get(i);
			points.add(new Point2D_I32(c.x+x0, c.y+y0));
		}
		return points;
	}

	private List<Point2D_I32> selectBrightest( List<Point2D_I32> points , ImageFloat32 intensity ,
											   int offX , int offY ) {
		if( points.size() == expectedPoints )
			return points;


		double values[] = new double[points.size()];
		int indexes[] = new int[points.size()];

		for( int i = 0; i < points.size(); i++ ) {
			Point2D_I32 p = points.get(i);

			values[i] = -intensity.get(p.x-offX, p.y-offY);
		}

		new QuickSort_F64().sort(values,points.size(),indexes);

		List<Point2D_I32> ret = new ArrayList<Point2D_I32>();

		for( int i = 0; i < expectedPoints; i++ ) {
			ret.add(points.get(indexes[i]));
		}

		return ret;
	}

	public FindChessBoundBinary getFindBound() {
		return findBound;
	}

	public List<Point2D_F64> getPoints() {
		return subpixel;
	}

	public ImageUInt8 getBinary() {
		return eroded;
	}
}
