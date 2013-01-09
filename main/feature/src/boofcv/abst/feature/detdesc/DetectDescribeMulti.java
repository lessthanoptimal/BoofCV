/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.detdesc;

import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageSingleBand;

/**
 * Detects and describes different types of features inside an image.  All detected features are described using the
 * same type of descriptor.  Each type of detected feature is a member of a set and the found features
 * for a set are returned inside of a {@link DetectionSet}.  The order and number of sets remains constant.
 *
 * <TD> Type of feature descriptor
 * @author Peter Abeles
 */
public interface DetectDescribeMulti<T extends ImageSingleBand, TD extends TupleDesc> {

	/**
	 * Detects features inside the image.
	 *
	 * @param image Image being processed.
	 */
	public void process( T image );

	/**
	 * The number of families.
	 *
	 * @return number of families
	 */
	public int getNumberOfSets();

	/**
	 * Returns the most recently detected features for a specific set.  Each time
	 * {@link #process(boofcv.struct.image.ImageSingleBand)} is called the results are modified.
	 *
	 * @param set Which set of detected features.
	 * @param
	 * @return Results for a set.
	 */
	public DetectionSet<TD> getDetectedSet( int set );

	/**
	 * Creates new description instance
	 *
	 * @return New descriptor
	 */
	public TD createDescription();

	/**
	 * The type of region descriptor generated
	 *
	 * @return Returns the descriptor type.
	 */
	public Class<TD> getDescriptionType();

	/**
	 * Number of elements in the descriptor tuple.
	 *
	 * @return Length of descriptor.
	 */
	public int getDescriptionLength();
}
