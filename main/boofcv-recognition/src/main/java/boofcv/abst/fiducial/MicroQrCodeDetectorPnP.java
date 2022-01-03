/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.fiducial.microqr.MicroQrCode;
import boofcv.alg.fiducial.microqr.MicroQrPose3DUtils;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.transform.homography.HomographyPointOps_F64;
import lombok.Getter;
import org.ejml.UtilEjml;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Wrapper around {@link MicroQrCodeDetector} which allows the 3D pose of a Micro QR Code to be detected using
 * {@link FiducialDetectorPnP}. The marker width defaults to 1. If all your qr codes have a width of one
 * and it's up to the user to multiply the translation vector by the actual width.
 *
 * The 3D coordinate system of a Micro QR Code is shown below. +x right, +y up, and +z up.
 *
 * <center>
 * <img src="doc-files/microqr3D.png"/>
 * </center>
 *
 * @author Peter Abeles
 */
public class MicroQrCodeDetectorPnP<T extends ImageGray<T>> extends FiducialDetectorPnP<T> {
	@Getter MicroQrCodeDetector<T> detector;
	ImageType<T> imageType;

	MicroQrPose3DUtils poseUtils = new MicroQrPose3DUtils();
	double sideWidth = 1.0;

	public MicroQrCodeDetectorPnP( MicroQrCodeDetector<T> detector ) {
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

			((MicroQrCodePreciseDetector)detector).setLensDistortion(width, height, null);
		} else {
			Point2D_F64 test = new Point2D_F64();
			Point2Transform2_F64 undistToDist = distortion.distort_F64(true, true);
			undistToDist.compute(0, 0, test);
			poseUtils.setLensDistortion(distortion.undistort_F64(true, false), undistToDist);

			// If there's no actual distortion don't undistort the image while processing. Faster this way
			if (test.norm() <= UtilEjml.TEST_F32) {
				((MicroQrCodePreciseDetector)detector).setLensDistortion(width, height, null);
			} else {
				((MicroQrCodePreciseDetector)detector).setLensDistortion(width, height, distortion);
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
		MicroQrCode qr = detector.getDetections().get(which);
		return poseUtils.getLandmarkByIndex(qr);
	}

	@Override
	protected List<Point2D3D> getControl3D( int which ) {
		MicroQrCode qr = detector.getDetections().get(which);
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
		// Use the marker's homography to determine where the center is since there are no landmarks
		// that can be used
		MicroQrCode qr = detector.getDetections().get(which);

		// Number of modules wide the marker is
		int halfModules = qr.getNumberOfModules()/2;

		HomographyPointOps_F64.transform(qr.Hinv, halfModules + 0.5, halfModules + 0.5, location);
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
}
