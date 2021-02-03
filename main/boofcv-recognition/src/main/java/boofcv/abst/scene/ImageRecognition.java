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

package boofcv.abst.scene;

import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.VerbosePrint;

import java.io.Reader;
import java.io.Writer;
import java.util.Iterator;

/**
 * High level interface for image recognition. The goal here is to identify and recall images of scenes that have
 * been seen previously from approximately the sam perspective.
 *
 * TODO describe usage of functions
 *
 * @author Peter Abeles
 */
public interface ImageRecognition<T extends ImageBase<T>> extends VerbosePrint {

	void learnDescription( Iterator<T> images );

	void saveDescription( Writer writer );

	void loadDescription( Reader reader );

	void addDataBase( String id, T image );

	void saveDataBase( Writer writer );

	void loadDataBase( Reader reader );

	/**
	 * Finds the best matches in the database to the query image.
	 *
	 * @param queryImage (Input) image being processed
	 * @param matches (Output) Set of matches. Can be empty.
	 * @return true if at least one valid match was found or false if no valid matches could be found
	 */
	boolean findBestMatch( T queryImage, DogArray<Match> matches );

	ImageType<T> getImageType();

	/** References a match in the database to the query image */
	class Match {
		/** ID of matching image */
		public String id;
		/** Error. Larger the value less similar it is to the original. Meaning is implementation dependent. */
		public double error;
	}
}
