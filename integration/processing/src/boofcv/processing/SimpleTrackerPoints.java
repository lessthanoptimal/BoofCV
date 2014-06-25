/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.processing;

import boofcv.abst.feature.tracker.PointTrack;
import boofcv.abst.feature.tracker.PointTracker;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Point2D_F64;
import processing.core.PImage;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around {@link PointTracker} with a simplified interface.
 *
 * @author Peter Abeles
 */
public class SimpleTrackerPoints {
	PointTracker tracker;
	List<PointTrack> list = new ArrayList<PointTrack>();
	Class imageType;
	ImageSingleBand gray;

	public SimpleTrackerPoints(PointTracker tracker, Class imageType) {

		this.tracker = tracker;
		this.imageType = imageType;
		gray = GeneralizedImageOps.createSingleBand(imageType,1,1);
	}

	public void process( PImage image) {
		gray.reshape(image.width,image.height);

		if( imageType == ImageFloat32.class ) {
			ConvertProcessing.convert_RGB_F32(image,(ImageFloat32)gray);
		} else if( imageType == ImageUInt8.class ) {
			ConvertProcessing.convert_RGB_U8(image, (ImageUInt8) gray);
		}

		tracker.process(gray);
		list.clear();
		tracker.getActiveTracks(list);
	}

	public void spawnTracks() {
		tracker.spawnTracks();
		list.clear();
		tracker.getActiveTracks(list);
	}

	void reset() {
		tracker.reset();
	}

	public int totalTracks() {
		return list.size();
	}

	public Point2D_F64 getLocation( int index ) {
		return list.get(index);
	}

	public long getTrackID( int index ) {
		return list.get(index).featureId;
	}

	public boolean dropTrack( int index ) {
		return tracker.dropTrack(list.get(index));
	}
}
