/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.qrcode;

import boofcv.struct.distort.DoNothing2Transform2_F64;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.geo.PointIndex2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilities when estimating the 3D pose of a QR Code
 *
 * @author Peter Abeles
 */
public class QrPose3DUtils {

	// observed pixel observations with indexes
	public List<PointIndex2D_F64> pixelControl = new ArrayList<>();
	// storage for corner locations in marker reference frame in 3D and normalized image coordinate observations
	public List<Point2D3D> point23 = new ArrayList<>();
	// storage for corner locations in marker reference frame in 3D
	public List<Point3D_F64> point3D = new ArrayList<>();
	// transform from pixel to normalzied image coordinates
	protected Point2Transform2_F64 pixelToNorm = new DoNothing2Transform2_F64();
	// width of QR code
	public double markerWidth = 1.0;

	public QrPose3DUtils() {
		for (int i = 0; i < 12; i++) {
			pixelControl.add( new PointIndex2D_F64(0,0,i) );
			point23.add( new Point2D3D() );
			point3D.add( new Point3D_F64() );
		}
	}

	/**
	 * Converts the corner observations into {@link PointIndex2D_F64} where observations are in pixels
	 */
	public List<PointIndex2D_F64> getLandmarkByIndex( QrCode qr) {
		pixelControl.get(0).set(qr.ppCorner.get(0));
		pixelControl.get(1).set(qr.ppCorner.get(1));
		pixelControl.get(2).set(qr.ppCorner.get(2));
		pixelControl.get(3).set(qr.ppCorner.get(3));

		pixelControl.get(4).set(qr.ppRight.get(0));
		pixelControl.get(5).set(qr.ppRight.get(1));
		pixelControl.get(6).set(qr.ppRight.get(2));
		pixelControl.get(7).set(qr.ppRight.get(3));

		pixelControl.get(8).set(qr.ppDown.get(0));
		pixelControl.get(9).set(qr.ppDown.get(1));
		pixelControl.get(10).set(qr.ppDown.get(2));
		pixelControl.get(11).set(qr.ppDown.get(3));

		return pixelControl;
	}

	/**
	 * Returns a list of {@link Point2D3D}. The 2D observation is the corner in normalized image coordinates.
	 * The 3D location is the location of the corner in the marker's reference frame
	 * @param qr The qr code
	 * @return List of corner points in marker frame and normalized image coordinates
	 */
	public List<Point2D3D> getLandmark2D3D(QrCode qr) {
		int N = qr.getNumberOfModules();

		setPair(0, 0,0,N,qr.ppCorner.get(0));
		setPair(1, 0,7,N,qr.ppCorner.get(1));
		setPair(2, 7,7,N,qr.ppCorner.get(2));
		setPair(3, 7,0,N,qr.ppCorner.get(3));

		setPair(4, 0,N-7,N,qr.ppRight.get(0));
		setPair(5, 0,N,N,qr.ppRight.get(1));
		setPair(6, 7,N,N,qr.ppRight.get(2));
		setPair(7, 7,N-7,N,qr.ppRight.get(3));

		setPair(8, N-7,0,N,qr.ppDown.get(0));
		setPair(9, N-7,7,N,qr.ppDown.get(1));
		setPair(10, N,7,N,qr.ppDown.get(2));
		setPair(11, N,0,N,qr.ppDown.get(3));

		return point23;
	}

	/**
	 * Location of each corner in the QR Code's reference frame in 3D
	 * @param version QR Code's version
	 * @return List. Recycled on each call
	 */
	public List<Point3D_F64> getLandmark3D( int version ) {
		int N = QrCode.totalModules(version);

		set3D( 0,0,N,point3D.get(0));
		set3D( 0,7,N,point3D.get(1));
		set3D( 7,7,N,point3D.get(2));
		set3D( 7,0,N,point3D.get(3));

		set3D( 0,N-7,N,point3D.get(4));
		set3D( 0,N,N,point3D.get(5));
		set3D( 7,N,N,point3D.get(6));
		set3D( 7,N-7,N,point3D.get(7));

		set3D( N-7,0,N,point3D.get(8));
		set3D( N-7,7,N,point3D.get(9));
		set3D( N,7,N,point3D.get(10));
		set3D( N,0,N,point3D.get(11));

		return point3D;
	}

	private void setPair(int which, int row, int col, int N , Point2D_F64 pixel ) {
		double gridX = col*markerWidth/N-markerWidth/2;
		double gridY = markerWidth/2-row*markerWidth/N;

		point23.get(which).location.set(gridX,gridY,0);
		pixelToNorm.compute(pixel.x,pixel.y,point23.get(which).observation);
	}

	private void set3D(int row, int col, int N , Point3D_F64 location ) {
		double gridX = col*markerWidth/N-markerWidth/2;
		double gridY = markerWidth/2-row*markerWidth/N;

		location.set(gridX,gridY,0);
	}

	public void setPixelToNorm(Point2Transform2_F64 pixelToNorm) {
		if( pixelToNorm == null ) {
			this.pixelToNorm = new DoNothing2Transform2_F64();
		} else {
			this.pixelToNorm = pixelToNorm;
		}
	}

	public void setMarkerWidth(double markerWidth) {
		this.markerWidth = markerWidth;
	}

	public Point2Transform2_F64 getPixelToNorm() {
		return pixelToNorm;
	}

	public double getMarkerWidth() {
		return markerWidth;
	}
}
