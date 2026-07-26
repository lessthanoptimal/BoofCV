/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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
import org.ddogleg.struct.DogArray_I32;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/// Computes a score for amount of coverage across the image, with independent scores for the border region and inner
/// image. All the regions which are not filled can be computed also. Score is simply fraction of specified region filled.
/// A region is filled if a single point occupies it.
///
/// Size of regions along the border and inner image can be specified different. Regions will have a rectangular shape
/// as the scale factor is determined by the image's aspect ratio.
public class ScoreCalibrationFill {
	/// How close to the edge a point needs is to be considered along the image border. If relative, then it's
	/// relative to the average side length
	@Getter public final ConfigLength borderExtent = ConfigLength.relative(0.04, 5);

	/// Number of regions the border along each side will be broken up into
	@Getter @Setter public int regionsBorder = 15;

	/// Number of regions along one axis the inner image is broken up into
	@Getter @Setter public int regionsInner = 10;

	/// Minimum number of observations needed for a bin to be considered "filled" for the score
	@Getter @Setter public int minCounts = 1;

	/// Fraction of bins filled along the border. 0 = not filled. 1.0 = perfect
	@Getter protected double scoreBorder;

	/// Fraction of bins filled in the inner region. 0 = not filled. 1.0 = perfect
	@Getter protected double scoreInner;

	/// Scores how uniform the distribution of samples in inside the inner region. 0 = chaos, 1 = perfect.
	@Getter protected double scoreUniform;

	/// Occupied counts inner region then outer regions
	@Getter protected final DogArray_I32 occupiedCounts = new DogArray_I32();

	/// actual distance from border of control points
	@Getter @Setter public int actualBorderPx;

	/// Shape of expected image
	@Getter protected int imageWidth, imageHeight;

	/// image shape minus the border
	@Getter protected int innerWidth, innerHeight;

	/// Resets and initializes for an image of the specified shape
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
		occupiedCounts.reset().resize(regionsInner*regionsInner + regionsBorder*4, 0);

		innerWidth = imageWidth - actualBorderPx*2;
		innerHeight = imageHeight - actualBorderPx*2;
	}

	public ScoreCalibrationFill setTo( ScoreCalibrationFill src ) {
		this.borderExtent.setTo(src.borderExtent);
		this.regionsBorder = src.regionsBorder;
		this.regionsInner = src.regionsInner;
		this.minCounts = src.minCounts;
		this.scoreBorder = src.scoreBorder;
		this.scoreInner = src.scoreInner;
		this.occupiedCounts.setTo(src.occupiedCounts);
		this.actualBorderPx = src.actualBorderPx;
		this.imageWidth = src.imageWidth;
		this.imageHeight = src.imageHeight;
		this.innerWidth = src.innerWidth;
		this.innerHeight = src.innerHeight;
		return this;
	}

	/// Adds the bin counts in `src` to the counts in `this.
	public void addToBins( DogArray_I32 src ) {
		if (src.size != occupiedCounts.size)
			throw new IllegalArgumentException("Array length must match bin count");

		for (int i = 0; i < occupiedCounts.size; i++) {
			occupiedCounts.data[i] += src.data[i];
		}
	}

	/// For each list in the list, it increments the bin it's contained in
	public void addObservations( int size, ConvertElement<Point2D_F64> list ) {
		if (this.imageWidth <= 0)
			throw new IllegalArgumentException("You must call initialize first.");

		var o = new Point2D_F64();

		for (int obsIdx = 0; obsIdx < size; obsIdx++) {
			list.access(obsIdx, o);
			int index = pixelToBin((int)(o.x + 0.5), (int)(o.y + 0.5));
			occupiedCounts.data[index]++;
		}

		recomputeScore();
	}

	/// Score based on fraction of regions filled
	public void recomputeScore() {
		int totalInner = regionsInner*regionsInner;
		scoreBorder = fractionFilled(totalInner, occupiedCounts.size);
		scoreInner = fractionFilled(0, totalInner);

		// See how close each bin is to having an even fraction contained inside
		long sumInner = 0;
		for (int i = 0; i < totalInner; i++) {
			sumInner += occupiedCounts.get(i);
		}
		double targetInner = sumInner/(double)totalInner;
		scoreUniform = 0;
		for (int i = 0; i < totalInner; i++) {
			int count = occupiedCounts.get(i);
			if (count > targetInner) {
				scoreUniform += targetInner / count;
			} else {
				scoreUniform += count / targetInner;
			}
		}
		scoreUniform /= totalInner;
	}

	/// For each list in the list, it increments the bin it's contained in
	public void addObservations( List<PointIndex2D_F64> obs ) {
		if (this.imageWidth <= 0)
			throw new IllegalArgumentException("You must call initialize first.");

		for (int obsIdx = 0; obsIdx < obs.size(); obsIdx++) {
			Point2D_F64 o = obs.get(obsIdx).p;
			int index = pixelToBin((int)(o.x + 0.5), (int)(o.y + 0.5));
			occupiedCounts.data[index]++;
		}

		// Score based on fraction of regions contained
		recomputeScore();
	}

	/// Given a pixel coordinate, it returns the bin the pixel is in.
	public int pixelToBin( int px, int py ) {
		if (isNearBorder(px, py, imageWidth, imageHeight)) {
			int index = regionsInner*regionsInner;
			if (py <= actualBorderPx) {
				index += regionsBorder*px/imageWidth;
			} else if (py >= imageHeight - actualBorderPx) {
				index += (regionsBorder*px/imageWidth) + regionsBorder*2;
			} else {
				// Have the regions start below the top border so that they don't overlap
				double adjY = py - actualBorderPx;
				index += (int)(regionsBorder*adjY/innerHeight);
				if (px >= imageWidth - actualBorderPx) {
					index += regionsBorder;
				} else {
					index += regionsBorder*3;
				}
			}
			return index;
		} else {
			// Compute grid coordinate inside inner region
			int row = regionsInner*(py - actualBorderPx)/innerHeight;
			int col = regionsInner*(px - actualBorderPx)/innerWidth;
			return row*regionsInner + col;
		}
	}

	/// Convert from a bin index to [RegionInfo]
	public RegionInfo binToRegion( int bin, @Nullable RegionInfo r ) {
		if (r == null)
			r = new RegionInfo();

		int totalInner = regionsInner*regionsInner;
		if (bin < totalInner) {
			int row = bin/regionsInner;
			int col = bin%regionsInner;

			r.counts = occupiedCounts.get(bin);
			r.inner = true;
			r.region.x0 = actualBorderPx + col*innerWidth/regionsInner;
			r.region.x1 = actualBorderPx + (col + 1)*innerWidth/regionsInner;
			r.region.y0 = actualBorderPx + row*innerHeight/regionsInner;
			r.region.y1 = actualBorderPx + (row + 1)*innerHeight/regionsInner;
			return r;
		}
		r.inner = false;
		r.counts = occupiedCounts.get(bin);
		int i = bin - totalInner;
		if (i < regionsBorder) { // TOP
			r.region.x0 = i*imageWidth/regionsBorder;
			r.region.x1 = (i + 1)*imageWidth/regionsBorder;
			r.region.y0 = 0;
			r.region.y1 = actualBorderPx;
		} else if (i < 2*regionsBorder) { // RIGHT
			int loc = i - regionsBorder;
			r.region.x0 = imageWidth - actualBorderPx;
			r.region.x1 = imageWidth;
			r.region.y0 = actualBorderPx + loc*innerHeight/regionsBorder;
			r.region.y1 = actualBorderPx + (loc + 1)*innerHeight/regionsBorder;
		} else if (i < 3*regionsBorder) { // BOTTOM
			int loc = i - 2*regionsBorder;
			r.region.x0 = loc*imageWidth/regionsBorder;
			r.region.x1 = (loc + 1)*imageWidth/regionsBorder;
			r.region.y0 = imageHeight - actualBorderPx;
			r.region.y1 = imageHeight;
		} else { // LEFT
			int loc = i - regionsBorder*3;
			r.region.x0 = 0;
			r.region.x1 = actualBorderPx;
			r.region.y0 = actualBorderPx + loc*innerHeight/regionsBorder;
			r.region.y1 = actualBorderPx + (loc + 1)*innerHeight/regionsBorder;
		}
		return r;
	}

	private double fractionFilled( int idx0, int idx1 ) {
		int total = 0;
		for (int i = idx0; i < idx1; i++) {
			if (occupiedCounts.get(i) >= minCounts)
				total++;
		}
		return total/(double)(idx1 - idx0);
	}

	/// Passes in information for every region. The passed in region is recycled so if you need to save it make
	/// a copy
	public void forEach( Consumer<RegionInfo> op ) {
		var work = new RegionInfo();
		forTop(work, op);
		forRight(work, op);
		forLeft(work, op);
		forBottom(work, op);
		forInner(work, op);
	}

	/// Retrieve a list of unoccupied regions
	public void getUnoccupied( DogArray<RegionInfo> unoccupiedRegions ) {
		unoccupiedRegions.reset();
		forEach(( region ) -> {
			if (region.counts < minCounts)
				unoccupiedRegions.grow().setTo(region);
		});
	}

	private void forTop( RegionInfo r, Consumer<RegionInfo> op ) {
		int totalInner = regionsInner*regionsInner;
		for (int i = 0; i < regionsBorder; i++) {
			r.inner = false;
			r.counts = occupiedCounts.get(i + totalInner);
			r.region.x0 = i*imageWidth/regionsBorder;
			r.region.x1 = (i + 1)*imageWidth/regionsBorder;
			r.region.y0 = 0;
			r.region.y1 = actualBorderPx;
			op.accept(r);
		}
	}

	private void forRight( RegionInfo r, Consumer<RegionInfo> op ) {
		int totalInner = regionsInner*regionsInner;
		for (int i = regionsBorder; i < 2*regionsBorder; i++) {
			r.inner = false;
			r.counts = occupiedCounts.get(i + totalInner);
			int loc = i - regionsBorder;
			r.region.x0 = imageWidth - actualBorderPx;
			r.region.x1 = imageWidth;
			r.region.y0 = actualBorderPx + loc*innerHeight/regionsBorder;
			r.region.y1 = actualBorderPx + (loc + 1)*innerHeight/regionsBorder;
			op.accept(r);
		}
	}

	private void forBottom( RegionInfo r, Consumer<RegionInfo> op ) {
		int totalInner = regionsInner*regionsInner;
		for (int i = 2*regionsBorder; i < 3*regionsBorder; i++) {
			r.inner = false;
			r.counts = occupiedCounts.get(i + totalInner);
			int loc = i - 2*regionsBorder;
			r.region.x0 = loc*imageWidth/regionsBorder;
			r.region.x1 = (loc + 1)*imageWidth/regionsBorder;
			r.region.y0 = imageHeight - actualBorderPx;
			r.region.y1 = imageHeight;
			op.accept(r);
		}
	}

	private void forLeft( RegionInfo r, Consumer<RegionInfo> op ) {
		int totalInner = regionsInner*regionsInner;
		for (int i = 3*regionsBorder; i < 4*regionsBorder; i++) {
			r.inner = false;
			r.counts = occupiedCounts.get(i + totalInner);
			int loc = i - regionsBorder*3;
			r.region.x0 = 0;
			r.region.x1 = actualBorderPx;
			r.region.y0 = actualBorderPx + loc*innerHeight/regionsBorder;
			r.region.y1 = actualBorderPx + (loc + 1)*innerHeight/regionsBorder;
			op.accept(r);
		}
	}

	private void forInner( RegionInfo r, Consumer<RegionInfo> op ) {
		int totalInner = regionsInner*regionsInner;
		for (int i = 0; i < totalInner; i++) {
			int row = i/regionsInner;
			int col = i%regionsInner;

			r.counts = occupiedCounts.get(i);
			r.inner = true;
			r.region.x0 = actualBorderPx + col*innerWidth/regionsInner;
			r.region.x1 = actualBorderPx + (col + 1)*innerWidth/regionsInner;
			r.region.y0 = actualBorderPx + row*innerHeight/regionsInner;
			r.region.y1 = actualBorderPx + (row + 1)*innerHeight/regionsInner;
			op.accept(r);
		}
	}

	/// True if the point is within tolerance of the border
	boolean isNearBorder( double x, double y, int width, int height ) {
		if (x <= actualBorderPx || width - x <= actualBorderPx)
			return true;
		if (y <= actualBorderPx || height - y <= actualBorderPx)
			return true;

		return false;
	}

	/// Keeps the structure but zeros the counts for all bins
	public void clearCounts() {
		occupiedCounts.fill(0);
		scoreInner = 0;
		scoreBorder = 0;
	}

	/// Specifies where a region is and if it's an inner region or border region.
	public static class RegionInfo {
		/// true if it's an inner region or false if it's a border region
		public boolean inner;
		/// Bounding box of region in pixels
		public Rectangle2D_I32 region = new Rectangle2D_I32();
		/// How many counts are in this region
		public int counts;

		public void reset() {
			inner = false;
			region.zero();
			counts = 0;
		}

		public RegionInfo setTo( RegionInfo src ) {
			this.inner = src.inner;
			this.region.setTo(src.region);
			this.counts = src.counts;
			return this;
		}
	}

	public interface ConvertElement<T> {
		void access( int index, T values );
	}
}
