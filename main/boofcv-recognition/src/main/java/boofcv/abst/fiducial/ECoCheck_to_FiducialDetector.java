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

import boofcv.abst.fiducial.calib.ConfigECoCheckMarkers.MarkerShape;
import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.alg.fiducial.calib.ecocheck.ECoCheckDetector;
import boofcv.alg.fiducial.calib.ecocheck.ECoCheckFound;
import boofcv.alg.fiducial.calib.ecocheck.ECoCheckUtils;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.transform.homography.HomographyPointOps_F64;
import lombok.Getter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastArray;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.fixed.CommonOps_DDF3;
import org.ejml.ops.DConvertMatrixStruct;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Wrapper around {@link ECoCheckDetector} for {@link FiducialDetector}.
 *
 * @author Peter Abeles
 */
public class ECoCheck_to_FiducialDetector<T extends ImageGray<T>> extends FiducialDetectorPnP<T> {

	/** The wrapped detector */
	@Getter ECoCheckDetector<T> detector;

	// workspace for estimating pose. observation is in normalized image coordinates
	private final DogArray<Point2D3D> points2D3D = new DogArray<>(Point2D3D::new);

	// homographies from corner-marker coordinate system to image pixels
	private final DogArray<Homography2D_F64> listMarkerToPixels = new DogArray<>(Homography2D_F64::new);

	// workspace for computing homography
	DMatrixRMaj tmp = new DMatrixRMaj(3, 3);
	private final DogArray<AssociatedPair> pairs = new DogArray<>(AssociatedPair::new);

	// used to retrieve grid shape and marker's physical size
	final FastArray<MarkerShape> markerShapes;

	// list of known detections with known markers
	DogArray_I32 foundToDetection = new DogArray_I32();

	final Point3D_F64 point3 = new Point3D_F64();

	public ECoCheck_to_FiducialDetector( ECoCheckDetector<T> detector, FastArray<MarkerShape> markerShapes ) {
		this.detector = detector;
		this.markerShapes = markerShapes;
	}

	@Override public void detect( T input ) {
		detector.process(input);

		// Find all the known detections
		foundToDetection.reset();
		for (int detectionID = 0; detectionID < detector.getFound().size; detectionID++) {
			ECoCheckFound found = detector.found.get(detectionID);
			if (found.markerID < 0)
				continue;
			foundToDetection.add(detectionID);
		}

		ECoCheckUtils utils = detector.getUtils();
		Estimate1ofEpipolar computeHomography = FactoryMultiView.homographyTLS();

		// Compute homographies for each marker
		listMarkerToPixels.reset();
		for (int knownIdx = 0; knownIdx < foundToDetection.size; knownIdx++) {
			int detectionID = foundToDetection.get(knownIdx);
			ECoCheckFound found = detector.found.get(detectionID);

			int markerID = found.markerID;

			// create a list of pairs from marker coordinates to pixels
			pairs.resetResize(found.corners.size);
			for (int i = 0; i < found.corners.size; i++) {
				PointIndex2D_F64 foundCorner = found.corners.get(i);

				// Get location of corner on marker in marker units
				utils.cornerToMarker3D(markerID, foundCorner.index, point3);

				// Observed pixel coordinate of corner
				Point2D_F64 p = found.corners.get(i).p;

				pairs.get(i).setTo(point3.x, point3.y, p.x, p.y);
			}

			// Find the homography that relates the coordinate systems
			Homography2D_F64 h = listMarkerToPixels.grow();
			if (!computeHomography.process(pairs.toList(), tmp)) {
				// well this is unexpected. Let's just silently fail.
				CommonOps_DDF3.fill(h, 0);
			} else {
				DConvertMatrixStruct.convert(tmp, h);
			}
		}
	}

	@Override public int totalFound() {return foundToDetection.size;}

	@Override public void getCenter( int which, Point2D_F64 location ) {
		HomographyPointOps_F64.transform(listMarkerToPixels.get(which), 0.0, 0.0, location);
	}

	@Override public Polygon2D_F64 getBounds( int which, @Nullable Polygon2D_F64 storage ) {
		if (storage == null)
			storage = new Polygon2D_F64(4);
		else
			storage.vertexes.resize(4);

		HomographyPointOps_F64.transform(listMarkerToPixels.get(which), -0.5, -0.5, storage.get(0));
		HomographyPointOps_F64.transform(listMarkerToPixels.get(which), -0.5, 0.5, storage.get(1));
		HomographyPointOps_F64.transform(listMarkerToPixels.get(which), 0.5, 0.5, storage.get(2));
		HomographyPointOps_F64.transform(listMarkerToPixels.get(which), 0.5, -0.5, storage.get(3));

		return storage;
	}

	@Override public long getId( int which ) {return foundIndexToFound(which).markerID;}

	@Override public String getMessage( int which ) {return getId(which) + "";}

	@Override public double getWidth( int which ) {
		int markerID = foundIndexToFound(which).markerID;
		MarkerShape marker = markerShapes.get(markerID);
		return (marker.getWidth() + marker.getHeight())/2.0;
	}

	@Override public boolean hasID() {return true;}

	@Override public boolean hasMessage() {return false;}

	@Override public ImageType<T> getInputType() {return detector.getImageType();}

	@Override public double getSideWidth( int which ) {
		int markerID = foundIndexToFound(which).markerID;
		return markerShapes.get(markerID).getWidth();
	}

	@Override public double getSideHeight( int which ) {
		int markerID = foundIndexToFound(which).markerID;
		return markerShapes.get(markerID).getHeight();
	}

	@Override public List<PointIndex2D_F64> getDetectedControl( int which ) {
		return foundIndexToFound(which).corners.toList();
	}

	@Override protected List<Point2D3D> getControl3D( int which ) {
		Objects.requireNonNull(pixelToNorm);

		ECoCheckFound found = foundIndexToFound(which);
		MarkerShape marker = markerShapes.get(found.markerID);
		points2D3D.resetResize(found.corners.size);

		ECoCheckUtils utils = detector.getUtils();

		// length of the longest side on the marker
		double markerLength = Math.max(marker.getWidth(), marker.getHeight());

		for (int i = 0; i < found.corners.size; i++) {
			PointIndex2D_F64 corner = found.corners.get(i);
			Point2D3D p23 = points2D3D.get(i);
			utils.cornerToMarker3D(found.markerID, corner.index, p23.location);
			pixelToNorm.compute(corner.p.x, corner.p.y, p23.observation);

			// Convert the units
			p23.location.scale(markerLength);
		}

		return points2D3D.toList();
	}

	private ECoCheckFound foundIndexToFound( int which ) {
		return detector.getFound().get(foundToDetection.get(which));
	}
}
