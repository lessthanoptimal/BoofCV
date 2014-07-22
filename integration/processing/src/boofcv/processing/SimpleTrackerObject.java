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

import boofcv.abst.tracker.TrackerObjectQuad;
import boofcv.struct.image.ImageBase;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.struct.shapes.Quadrilateral_F64;
import georegression.struct.shapes.Rectangle2D_F64;
import processing.core.PImage;

/**
 * Simplified interface for {@link TrackerObjectQuad}.
 *
 * @author Peter Abeles
 */
public class SimpleTrackerObject {
	TrackerObjectQuad tracker;
	ImageBase boofImage;

	Quadrilateral_F64 location = new Quadrilateral_F64();
	Rectangle2D_F64 locationR = new Rectangle2D_F64();

	public SimpleTrackerObject( TrackerObjectQuad tracker ) {
		this.tracker = tracker;
		boofImage = tracker.getImageType().createImage(1,1);
	}

	public boolean initialize( PImage image , Quadrilateral_F64 initialLocation )
	{
		boofImage.reshape(image.width,image.height);
		ConvertProcessing.convertFromRGB(image,boofImage);
		return tracker.initialize(boofImage,initialLocation);
	}

	/**
	 * Initializes the track by specifying the target using a rectangle
	 */
	public boolean initialize( PImage image , double x0 , double y0 , double x1 , double y1 )
	{
		Quadrilateral_F64 q = new Quadrilateral_F64();
		q.a.set(x0, y0);
		q.b.set(x1, y0);
		q.c.set(x1, y1);
		q.d.set(x0, y1);

		return initialize(image,q);
	}

	/**
	 * Processes the next image in the sequence
	 * @return true if the target could be tracked and false if it failed
	 */
	public boolean process( PImage image ) {
		ConvertProcessing.convertFromRGB(image,boofImage);
		return tracker.process(boofImage, location);
	}

	/**
	 * Target's location as a quadrilateral.
	 */
	public Quadrilateral_F64 getLocation() {
		return location;
	}

	/**
	 * Location of the target which has been approximated by a rectangle
	 */
	public Rectangle2D_F64 getLocationR() {
		UtilPolygons2D_F64.bounding(location, locationR);
		return locationR;
	}

}
