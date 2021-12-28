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

import boofcv.alg.feature.describe.llah.LlahDocument;
import boofcv.alg.feature.describe.llah.LlahOperations;
import boofcv.alg.fiducial.dots.UchiyaMarkerImageTracker;
import boofcv.alg.fiducial.dots.UchiyaMarkerTracker;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.transform.homography.HomographyPointOps_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.List;
import java.util.Objects;

/**
 * Wrapper around {@link UchiyaMarkerTracker} for {@link FiducialDetector}. To add documents call
 * {@link #addMarker(List)}
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class Uchiya_to_FiducialDetector<T extends ImageGray<T>> extends FiducialDetectorPnP<T>
		implements FiducialTracker<T> {
	ImageType<T> imageType;

	@Getter UchiyaMarkerImageTracker<T> tracker;

	/** If not null timing information will be printed */
	@Setter PrintStream printTiming;

	// Width and height of the marker
	final double markerWidth;
	final double markerHeight;

	// Storage
	final DogArray<Point2D3D> control3D = new DogArray<>(Point2D3D::new);

	// Local work space
	final Point2D_F64 norm = new Point2D_F64();

	public Uchiya_to_FiducialDetector( UchiyaMarkerImageTracker<T> tracker,
									   double markerWidth, double markerHeight,
									   ImageType<T> imageType ) {
		this.tracker = tracker;
		this.markerWidth = markerWidth;
		this.markerHeight = markerHeight;
		this.imageType = imageType;
	}

	@Override
	public void detect( T input ) {
		tracker.detect(input);

		final PrintStream out = this.printTiming;
		if (out != null) {
			double timeTrack = tracker.getTracker().getTimeTrack();
			double timeDetect = tracker.getTracker().getTimeDetect();
			double timeUpdate = tracker.getTracker().getTimeUpdate();

			out.printf(" Uchiya: BI %5.1f EL %5.1f ER %5.1f TR %5.1f DET %5.1f UP %5.1f\n",
					tracker.getTimeBinary(), tracker.getTimeEllipse(), tracker.getTimeReject(),
					timeTrack, timeDetect, timeUpdate);
		}
	}

	@Override
	public void reset() {
		tracker.getTracker().resetTracking();
	}

	@Override
	public int totalFound() {
		return tracker.getTracks().size;
	}

	@Override
	public void getCenter( int which, Point2D_F64 location ) {
		UchiyaMarkerTracker.Track track = tracker.getTracks().get(which);
		HomographyPointOps_F64.transform(track.doc_to_imagePixel, 0, 0, location);
	}

	@Override
	public Polygon2D_F64 getBounds( int which, @Nullable Polygon2D_F64 storage ) {
		if (storage == null)
			storage = new Polygon2D_F64(4);
		else
			storage.vertexes.resize(4);

		UchiyaMarkerTracker.Track track = tracker.getTracks().get(which);

		double rx = markerWidth/2.0;
		double ry = markerHeight/2.0;
		HomographyPointOps_F64.transform(track.doc_to_imagePixel, -rx, -ry, storage.get(0));
		HomographyPointOps_F64.transform(track.doc_to_imagePixel, rx, -ry, storage.get(1));
		HomographyPointOps_F64.transform(track.doc_to_imagePixel, rx, ry, storage.get(2));
		HomographyPointOps_F64.transform(track.doc_to_imagePixel, -rx, ry, storage.get(3));

		return storage;
	}

	@Override
	public long getId( int which ) {
		UchiyaMarkerTracker.Track track = tracker.getTracks().get(which);
		return track.globalDoc.documentID;
	}

	@Override
	public String getMessage( int which ) {
		return "UCHIYA";
	}

	@Override
	public double getWidth( int which ) {
		return Math.max(markerWidth, markerHeight);
	}

	@Override
	public double getSideWidth( int which ) {
		return markerWidth;
	}

	@Override
	public double getSideHeight( int which ) {
		return markerHeight;
	}

	@Override
	public List<PointIndex2D_F64> getDetectedControl( int which ) {
		UchiyaMarkerTracker.Track track = tracker.getTracks().get(which);
		return track.observed.toList();
	}

	@Override
	protected List<Point2D3D> getControl3D( int which ) {
		Objects.requireNonNull(pixelToNorm);
		UchiyaMarkerTracker.Track track = tracker.getTracks().get(which);
		control3D.reset();

		for (int dotIdx = 0; dotIdx < track.observed.size; dotIdx++) {
			PointIndex2D_F64 dot = track.observed.get(dotIdx);
			Point2D_F64 landmark = track.globalDoc.landmarks.get(dot.index);
			pixelToNorm.compute(dot.p.x, dot.p.y, norm);
			control3D.grow().setTo(norm.x, norm.y, landmark.x, -landmark.y, 0);
		}

		return control3D.toList();
	}

	@Override
	public boolean hasID() {
		return true;
	}

	@Override
	public boolean hasMessage() {
		return false;
	}

	@Override
	public ImageType<T> getInputType() {
		return imageType;
	}

	/**
	 * Creates a document from a set of points.
	 */
	public LlahDocument addMarker( List<Point2D_F64> locations2D ) {

		// sanity check the document
		double radiusX = markerWidth/2.0;
		double radiusY = markerHeight/2.0;

		for (int i = 0; i < locations2D.size(); i++) {
			Point2D_F64 p = locations2D.get(i);
			if (p.x < -radiusX || p.x > radiusX || p.y < -radiusY || p.y > radiusY)
				throw new IllegalArgumentException(
						"Marker size is (" + markerWidth + "," + markerHeight + ") and " + p + " is out of bounds");
		}

		return getLlahOperations().createDocument(locations2D);
	}

	public LlahOperations getLlahOperations() {
		return tracker.getTracker().getLlahOps();
	}

	public DogArray<UchiyaMarkerTracker.Track> getTracks() {
		return tracker.getTracks();
	}
}
