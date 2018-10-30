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

package boofcv.abst.fiducial;

import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.metric.Intersection2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around {@link QrCodeDetector} which allows the 3D pose of a QR Code to be detected using
 * {@link FiducialDetectorPnP}. The marker width defaults to 1.  If all your qr codes are the same width
 * then you can set the width to another value. If it varies then you should determine the width
 * and scale the translation by the appropriate amount.
 *
 * @author Peter Abeles
 */
public class QrCodeDetectorPnP<T extends ImageGray<T>> extends FiducialDetectorPnP<T> {

	QrCodeDetector<T> detector;
	ImageType<T> imageType;

	List<PointIndex2D_F64> pixelControl = new ArrayList<>();
	List<Point2D3D> point23 = new ArrayList<>();

	double markerWidth = 1.0;

	public QrCodeDetectorPnP(QrCodeDetector<T> detector) {
		this.detector = detector;
		imageType = ImageType.single(detector.getImageType());

		for (int i = 0; i < 12; i++) {
			pixelControl.add( new PointIndex2D_F64(0,0,i) );
			point23.add( new Point2D3D() );
		}
	}

	private void setPair(int which, int row, int col, int N , Point2D_F64 pixel ) {
		double gridX = col*markerWidth/N-markerWidth/2;
		double gridY = markerWidth/2-row*markerWidth/N;

		point23.get(which).location.set(gridX,gridY,0);
		pixelToNorm.compute(pixel.x,pixel.y,point23.get(which).observation);
	}

	@Override
	public double getSideWidth(int which) {
		return markerWidth;
	}

	@Override
	public double getSideHeight(int which) {
		return markerWidth;
	}

	@Override
	public List<PointIndex2D_F64> getDetectedControl(int which) {
		QrCode qr = detector.getDetections().get(which);

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

	@Override
	protected List<Point2D3D> getControl3D(int which) {
		QrCode qr = detector.getDetections().get(which);
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

	@Override
	public void detect(T input) {
		detector.process(input);
	}

	@Override
	public int totalFound() {
		return detector.getDetections().size();
	}

	@Override
	public void getCenter(int which, Point2D_F64 location) {
		// use intersections being invariant under perspective distoriton
		QrCode qr = detector.getDetections().get(which);

		// find the intersection of two lines which are closer to the origin to reduce error
		Intersection2D_F64.intersection(
				qr.ppDown.get(0),qr.ppDown.get(1),
				qr.ppRight.get(0),qr.ppRight.get(3),location);

		// need one more intersection. again pick corners close to center
		Intersection2D_F64.intersection(qr.ppCorner.get(2),location,qr.ppDown.get(1),qr.ppRight.get(3),location);
	}

	@Override
	public Polygon2D_F64 getBounds(int which, @Nullable Polygon2D_F64 storage) {
		if( storage == null )
			storage = new Polygon2D_F64();
		storage.set(detector.getDetections().get(which).bounds);
		return storage;
	}

	@Override
	public long getId(int which) {
		return detector.getDetections().get(which).message.hashCode();
	}

	@Override
	public String getMessage(int which) {
		return detector.getDetections().get(which).message;
	}

	@Override
	public double getWidth(int which) {
		return markerWidth;
	}

	public void setMarkerWidth(double markerWidth) {
		this.markerWidth = markerWidth;
	}

	@Override
	public boolean hasID() {
		return true;
	}

	@Override
	public boolean hasMessage() {
		return true;
	}

	@Override
	public ImageType<T> getInputType() {
		return imageType;
	}

	public QrCodeDetector<T> getDetector() {
		return detector;
	}
}
