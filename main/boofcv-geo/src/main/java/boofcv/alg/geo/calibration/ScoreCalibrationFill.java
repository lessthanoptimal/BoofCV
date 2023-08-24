/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

import boofcv.misc.BoofMiscOps;
import boofcv.struct.ConfigLength;
import boofcv.struct.geo.PointIndex2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Rectangle2D_I32;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_B;

import java.util.List;

/**
 * Computes a score for amount of coverage across the image, with independent scores for the border region and inner
 * image. All the regions which are not filled can be computed also. Score is simply fraction of specified region filled.
 * A region is filled if a single point occupies it.
 *
 * Size of regions along the border and inner image can be specified different. Regions will have a rectangular shape
 * as the scale factor is determined by the image's aspect ratio.
 */
public class ScoreCalibrationFill {
	/**
	 * How close to the edge a point needs is to be considered along the image border. If relative, then it's
	 * relative to the average side length
	 */
	@Getter public final ConfigLength borderExtent = ConfigLength.relative(0.04, 5);

	/** Number of regions the border along each side will be broken up into */
	@Getter @Setter public int regionsBorder = 15;

	/** Number of regions along one axis the inner image is broken up into */
	@Getter @Setter public int regionsInner = 10;

	/** The fill score. 0 = not filled. 1.0 = perfect */
	@Getter double scoreBorder;

	/** The fill score. 0 = not filled. 1.0 = perfect */
	@Getter double scoreInner;

	/** Indicates if the region along a border is occupied or not */
	final DogArray_B occupiedBorder = new DogArray_B();

	/** Indicates if the region along inside the inner image is occupied or not */
	final DogArray_B occupiedInner = new DogArray_B();

	/** actual distance from border of control points */
	public int actualBorderPx;

	/** Storage for unoccupied regions. Must be updated by calling {@link #updateUnoccupied} */
	public final @Getter DogArray<RegionInfo> unoccupiedRegions = new DogArray<>(RegionInfo::new, RegionInfo::reset);

	/** Shape of expected image */
	protected int imageWidth, imageHeight;

	/** image shape minus the border */
	protected int innerWidth, innerHeight;

	/**
	 * Resets and initializes for an image of the specified shape
	 */
	public void initialize( int width, int height ) {
		if (width <= 0 || height <= 0)
			throw new IllegalArgumentException("Image width or height can't be zero or less. " + width + "x" + height);
		this.imageWidth = width;
		this.imageHeight = height;
		scoreBorder = 0.0;
		scoreInner = 0.0;

		// offset from image border that target points should be
		actualBorderPx = BoofMiscOps.thresholdByImageSizeI(borderExtent, width, height);

		// Mark all regions as not occupied
		occupiedBorder.reset().resize(regionsBorder*4, false);
		occupiedInner.reset().resize(regionsInner*regionsInner, false);

		innerWidth = imageWidth - actualBorderPx*2;
		innerHeight = imageHeight - actualBorderPx*2;
	}

	/**
	 * See if any observed calibration points hit a target. if so remove the target.
	 */
	public void addObservation( List<PointIndex2D_F64> obs ) {
		if (this.imageWidth <= 0)
			throw new IllegalArgumentException("You must call initialize first.");

		for (int obsIdx = 0; obsIdx < obs.size(); obsIdx++) {
			Point2D_F64 o = obs.get(obsIdx).p;

			if (isNearBorder(o.x, o.y, imageWidth, imageHeight)) {
				int index;
				if (o.y <= actualBorderPx) {
					index = (int)(regionsBorder*o.x/imageWidth);
				} else if (o.y >= imageHeight - actualBorderPx) {
					index = (int)(regionsBorder*o.x/imageWidth) + regionsBorder*2;
				} else {
					// Have the regions start below the top border so that they don't overlap
					double adjY = o.y - actualBorderPx;
					index = (int)(regionsBorder*adjY/innerHeight);
					if (o.x >= imageWidth - actualBorderPx) {
						index += regionsBorder;
					} else {
						index += regionsBorder*3;
					}
				}
				occupiedBorder.set(index, true);
			} else {
				// Compute grid coordinate inside inner region
				int row = (int)(regionsInner*(o.y - actualBorderPx)/innerHeight);
				int col = (int)(regionsInner*(o.x - actualBorderPx)/innerWidth);
				int index = row*regionsInner + col;
				occupiedInner.set(index, true);
			}
		}

		// Score based on fraction of regions contained
		scoreBorder = occupiedBorder.count(true)/(double)occupiedBorder.size;
		scoreInner = occupiedInner.count(true)/(double)occupiedInner.size;
	}

	/** Updates list of unoccupied regions */
	public void updateUnoccupied() {
		unoccupiedRegions.reset();
		findUnoccupiedTop();
		findUnoccupiedRight();
		findUnoccupiedBottom();
		findUnoccupiedLeft();
		findUnoccupiedInner();
	}

	private void findUnoccupiedTop() {
		for (int i = 0; i < regionsBorder; i++) {
			if (occupiedBorder.get(i))
				continue;
			RegionInfo r = unoccupiedRegions.grow();
			r.inner = false;
			r.region.x0 = i*imageWidth/regionsBorder;
			r.region.x1 = (i + 1)*imageWidth/regionsBorder;
			r.region.y0 = 0;
			r.region.y1 = actualBorderPx;
		}
	}

	private void findUnoccupiedRight() {
		for (int i = regionsBorder; i < 2*regionsBorder; i++) {
			if (occupiedBorder.get(i))
				continue;
			RegionInfo r = unoccupiedRegions.grow();
			r.inner = false;

			int loc = i - regionsBorder;
			r.region.x0 = imageWidth - actualBorderPx;
			r.region.x1 = imageWidth;
			r.region.y0 = actualBorderPx + loc*innerHeight/regionsBorder;
			r.region.y1 = actualBorderPx + (loc + 1)*innerHeight/regionsBorder;
		}
	}

	private void findUnoccupiedBottom() {
		for (int i = 2*regionsBorder; i < 3*regionsBorder; i++) {
			if (occupiedBorder.get(i))
				continue;
			RegionInfo r = unoccupiedRegions.grow();
			r.inner = false;

			int loc = i - 2*regionsBorder;
			r.region.x0 = loc*imageWidth/regionsBorder;
			r.region.x1 = (loc + 1)*imageWidth/regionsBorder;
			r.region.y0 = imageHeight - actualBorderPx;
			r.region.y1 = imageHeight;
		}
	}

	private void findUnoccupiedLeft() {
		for (int i = 3*regionsBorder; i < 4*regionsBorder; i++) {
			if (occupiedBorder.get(i))
				continue;
			RegionInfo r = unoccupiedRegions.grow();
			r.inner = false;

			int loc = i - regionsBorder*3;
			r.region.x0 = 0;
			r.region.x1 = actualBorderPx;
			r.region.y0 = actualBorderPx + loc*innerHeight/regionsBorder;
			r.region.y1 = actualBorderPx + (loc + 1)*innerHeight/regionsBorder;
		}
	}

	private void findUnoccupiedInner() {
		for (int i = 0; i < occupiedInner.size; i++) {
			if (occupiedInner.get(i))
				continue;

			int row = i/regionsInner;
			int col = i%regionsInner;

			RegionInfo r = unoccupiedRegions.grow();
			r.inner = true;
			r.region.x0 = actualBorderPx + col*innerWidth/regionsInner;
			r.region.x1 = actualBorderPx + (col + 1)*innerWidth/regionsInner;
			r.region.y0 = actualBorderPx + row*innerHeight/regionsInner;
			r.region.y1 = actualBorderPx + (row + 1)*innerHeight/regionsInner;
		}
	}

	/**
	 * True if the point is within tolerance of the border
	 */
	boolean isNearBorder( double x, double y, int width, int height ) {
		if (x <= actualBorderPx || width - x <= actualBorderPx)
			return true;
		if (y <= actualBorderPx || height - y <= actualBorderPx)
			return true;

		return false;
	}

	/** Specifies where a region is and if it's an inner region or border region. */
	public static class RegionInfo {
		/** true if it's an inner region or false if it's a border region */
		public boolean inner;
		/** Bounding box of region in pixels */
		public Rectangle2D_I32 region = new Rectangle2D_I32();

		public void reset() {
			inner = false;
			region.zero();
		}

		public RegionInfo setTo( RegionInfo src ) {
			this.inner = src.inner;
			this.region.setTo(src.region);
			return this;
		}
	}
}
