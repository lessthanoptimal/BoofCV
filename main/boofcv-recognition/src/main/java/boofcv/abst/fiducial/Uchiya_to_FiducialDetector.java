/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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
import georegression.geometry.UtilPoint2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.struct.shapes.Rectangle2D_F64;
import georegression.transform.homography.HomographyPointOps_F64;
import gnu.trove.map.hash.TIntDoubleHashMap;
import lombok.Getter;
import org.ddogleg.struct.FastQueue;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Wrapper around {@link UchiyaMarkerTracker} for {@link FiducialDetector}. To add documents call
 * {@link #createDocument(List)}
 *
 * @author Peter Abeles
 */
public class Uchiya_to_FiducialDetector<T extends ImageGray<T>> extends FiducialDetectorPnP<T>
implements FiducialTracker<T>
{
	ImageType<T> imageType;

	@Getter UchiyaMarkerImageTracker<T> tracker;

	// Storage
	final FastQueue<Point2D3D> control3D = new FastQueue<>(Point2D3D::new);

	// Compute and save the radius of each document
	final TIntDoubleHashMap docToRadius = new TIntDoubleHashMap();

	// Local work space
	final Point2D_F64 center = new Point2D_F64();
	final Rectangle2D_F64 rectangle = new Rectangle2D_F64();
	final Point2D_F64 norm = new Point2D_F64();

	public Uchiya_to_FiducialDetector(UchiyaMarkerImageTracker<T> tracker, ImageType<T> imageType ) {
		this.tracker = tracker;
		this.imageType = imageType;
	}

	@Override
	public void detect(T input) {
		tracker.detect(input);
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
	public void getCenter(int which, Point2D_F64 location) {
		UchiyaMarkerTracker.Track track = tracker.getTracks().get(which);
		HomographyPointOps_F64.transform(track.doc_to_imagePixel,0,0,location);
	}

	@Override
	public Polygon2D_F64 getBounds(int which, @Nullable Polygon2D_F64 storage) {
		if( storage == null )
			storage = new Polygon2D_F64(4);
		else
			storage.vertexes.resize(4);

		UchiyaMarkerTracker.Track track = tracker.getTracks().get(which);

		UtilPoint2D_F64.bounding(track.predicted.toList(),rectangle);

		storage.get(0).set(rectangle.p0);
		storage.get(1).set(rectangle.p0.x,rectangle.p1.y);
		storage.get(2).set(rectangle.p1);
		storage.get(3).set(rectangle.p1.x,rectangle.p0.y);

		return storage;
	}

	@Override
	public long getId(int which) {
		UchiyaMarkerTracker.Track track = tracker.getTracks().get(which);
		return track.globalDoc.documentID;
	}

	@Override
	public String getMessage(int which) {
		return "UCHIYA";
	}

	/**
	 * Use the average distance from the center as the object's width
	 * @param which Fiducial's index
	 */
	@Override
	public double getWidth(int which) {
		UchiyaMarkerTracker.Track track = tracker.getTracks().get(which);

		// Use precomputed value if available
		if( docToRadius.containsKey(track.globalDoc.documentID) ) {
			return docToRadius.get(track.globalDoc.documentID);
		} else {
			FastQueue<Point2D_F64> landmarks = track.globalDoc.landmarks;

			UtilPoint2D_F64.mean(landmarks.toList(), center);
			double width = 0;
			for (int i = 0; i < landmarks.size; i++) {
				width += landmarks.get(i).distance(center);
			}
			width *= 2.0/landmarks.size;

			docToRadius.put(track.globalDoc.documentID,width);

			return width;
		}
	}


	@Override
	public double getSideWidth(int which) {
		return getWidth(which);
	}

	@Override
	public double getSideHeight(int which) {
		return getWidth(which);
	}

	@Override
	public List<PointIndex2D_F64> getDetectedControl(int which) {
		UchiyaMarkerTracker.Track track = tracker.getTracks().get(which);
		return track.observed.toList();
	}

	@Override
	protected List<Point2D3D> getControl3D(int which) {
		UchiyaMarkerTracker.Track track = tracker.getTracks().get(which);
		control3D.reset();

		for (int dotIdx = 0; dotIdx < track.observed.size; dotIdx++) {
			PointIndex2D_F64 dot = track.observed.get(dotIdx);
			Point2D_F64 landmark = track.globalDoc.landmarks.get(dot.index);
			pixelToNorm.compute(dot.x,dot.y, norm);
			control3D.grow().set(norm.x,norm.y,landmark.x,landmark.y,0);
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
	 * Convenience function for creating a document
	 */
	public LlahDocument createDocument(List<Point2D_F64> locations2D ) {
		return getLlahOperations().createDocument(locations2D);
	}

	public LlahOperations getLlahOperations() {
		return tracker.getTracker().getLlahOps();
	}
}
