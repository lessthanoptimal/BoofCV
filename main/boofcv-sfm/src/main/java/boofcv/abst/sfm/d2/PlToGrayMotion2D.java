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

package boofcv.abst.sfm.d2;

import boofcv.abst.sfm.AccessPointTracks;
import boofcv.core.image.GConvertImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.Planar;
import georegression.struct.InvertibleTransform;
import georegression.struct.point.Point2D_F64;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Wrapper which converts a planar image into a gray scale image before computing its image motion.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class PlToGrayMotion2D<T extends ImageGray<T>, IT extends InvertibleTransform<IT>>
		implements ImageMotion2D<Planar<T>, IT>, AccessPointTracks {
	// motion estimation algorithm for a single band image
	ImageMotion2D<T, IT> motion;
	// if supposed, provides access to track points
	AccessPointTracks access;
	// storage for gray scale image
	T gray;

	public PlToGrayMotion2D( ImageMotion2D<T, IT> motion, Class<T> imageType ) {
		this.motion = motion;
		gray = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
		if (motion instanceof AccessPointTracks) {
			access = (AccessPointTracks)motion;
		}
	}

	@Override
	public boolean process( Planar<T> input ) {
		gray.reshape(input.width, input.height);
		GConvertImage.average(input, gray);
		return motion.process(gray);
	}

	@Override
	public void reset() {
		motion.reset();
	}

	@Override
	public void setToFirst() {
		motion.setToFirst();
	}

	@Override
	public long getFrameID() {
		return motion.getFrameID();
	}

	@Override
	public IT getFirstToCurrent() {
		return motion.getFirstToCurrent();
	}

	@Override
	public Class<IT> getTransformType() {
		return motion.getTransformType();
	}

	@Override
	public int getTotalTracks() {
		return access.getTotalTracks();
	}

	@Override
	public long getTrackId( int index ) {
		return access.getTrackId(index);
	}

	@Override
	public void getTrackPixel( int index, Point2D_F64 pixel ) {
		access.getTrackPixel(index, pixel);
	}

	@Override
	public List<Point2D_F64> getAllTracks( @Nullable List<Point2D_F64> storage ) {
		return access.getAllTracks(storage);
	}

	@Override
	public boolean isTrackInlier( int index ) {
		return access.isTrackInlier(index);
	}

	@Override
	public boolean isTrackNew( int index ) {
		return access.isTrackNew(index);
	}
}
