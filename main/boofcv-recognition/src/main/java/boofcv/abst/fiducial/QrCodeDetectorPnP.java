/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.alg.fiducial.qrcode.QrPose3DUtils;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.metric.Intersection2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.ejml.UtilEjml;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Wrapper around {@link QrCodeDetector} which allows the 3D pose of a QR Code to be detected using
 * {@link FiducialDetectorPnP}. The marker width defaults to 1. If all your qr codes have a width of one
 * and it's up to the user to multiply the translation vector by the actual width.
 *
 * The 3D coordinate system of a QR Code is shown below. +x right, +y up, and +z up.
 *
 * <center>
 * <img src="doc-files/qrcode3D.png"/>
 * </center>
 *
 * @author Peter Abeles
 */
public class QrCodeDetectorPnP<T extends ImageGray<T>> extends FiducialDetectorPnP<T> {

	QrCodeDetector<T> detector;
	ImageType<T> imageType;

	QrPose3DUtils poseUtils = new QrPose3DUtils();
	double sideWidth = 1.0;

	public QrCodeDetectorPnP( QrCodeDetector<T> detector ) {
		this.detector = detector;
		imageType = ImageType.single(detector.getImageType());
	}

	@Override
	public void setLensDistortion( @Nullable LensDistortionNarrowFOV distortion, int width, int height ) {
		super.setLensDistortion(distortion, width, height);
		if (distortion == null) {
			poseUtils.setLensDistortion(null, null);

			// Yes this shouldn't be hard coded to that type. It feels dirty adding lens distortion to the
			// generic QR Code detector... deal with this later if there is more than one detector

			((QrCodePreciseDetector)detector).setLensDistortion(width, height, null);
		} else {
			Point2D_F64 test = new Point2D_F64();
			Point2Transform2_F64 undistToDist = distortion.distort_F64(true, true);
			undistToDist.compute(0, 0, test);
			poseUtils.setLensDistortion(
					distortion.undistort_F64(true, false),
					undistToDist);

			// If there's no actual distortion don't undistort the image while processing. Faster this way
			if (test.norm() <= UtilEjml.TEST_F32) {
				((QrCodePreciseDetector)detector).setLensDistortion(width, height, null);
			} else {
				((QrCodePreciseDetector)detector).setLensDistortion(width, height, distortion);
			}
		}
	}

	@Override
	public double getSideWidth( int which ) {
		return sideWidth;
	}

	@Override
	public double getSideHeight( int which ) {
		return sideWidth;
	}

	@Override
	public boolean getFiducialToCamera( int which, Se3_F64 fiducialToCamera ) {
		// need to scale solution since marker coordinates are scaled to
		// gave values from -1 to 1, which is a width of 2.0
		if (super.getFiducialToCamera(which, fiducialToCamera)) {
			fiducialToCamera.T.scale(sideWidth/2.0);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public List<PointIndex2D_F64> getDetectedControl( int which ) {
		QrCode qr = detector.getDetections().get(which);
		return poseUtils.getLandmarkByIndex(qr);
	}

	@Override
	protected List<Point2D3D> getControl3D( int which ) {
		QrCode qr = detector.getDetections().get(which);
		return poseUtils.getLandmark2D3D(qr);
	}

	@Override
	public void detect( T input ) {
		detector.process(input);
	}

	@Override
	public int totalFound() {
		return detector.getDetections().size();
	}

	@Override
	public void getCenter( int which, Point2D_F64 location ) {
		// use intersections being invariant under perspective distoriton
		QrCode qr = detector.getDetections().get(which);

		// find the intersection of two lines which are closer to the origin to reduce error
		Intersection2D_F64.intersection(
				qr.ppDown.get(0), qr.ppDown.get(1),
				qr.ppRight.get(0), qr.ppRight.get(3), location);

		// need one more intersection. again pick corners close to center
		Intersection2D_F64.intersection(qr.ppCorner.get(2), location, qr.ppDown.get(1), qr.ppRight.get(3), location);
	}

	@Override
	public Polygon2D_F64 getBounds( int which, @Nullable Polygon2D_F64 storage ) {
		if (storage == null)
			storage = new Polygon2D_F64();
		storage.setTo(detector.getDetections().get(which).bounds);
		return storage;
	}

	@Override
	public long getId( int which ) {
		return detector.getDetections().get(which).message.hashCode();
	}

	@Override
	public String getMessage( int which ) {
		return detector.getDetections().get(which).message;
	}

	@Override
	public double getWidth( int which ) {
		return sideWidth;
	}

	public void setMarkerWidth( double markerWidth ) {
		this.sideWidth = markerWidth;
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
