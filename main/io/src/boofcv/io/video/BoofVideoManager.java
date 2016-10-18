/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.io.video;

/**
 * Allows a {@link VideoInterface} to be created abstractly without directly referencing
 * the codec class.
 *
 * @author Peter Abeles
 */
public class BoofVideoManager {

	/**
	 * Loads the default {@link VideoInterface}.
	 *
	 * @return Video interface
	 */
	public static VideoInterface loadManagerDefault() {
		try {
			return loadManager("boofcv.io.wrapper.xuggler.XugglerVideoInterface");
		} catch( RuntimeException e ) {
			return new BoofMjpegVideo();
		}
	}

	/**
	 * Loads the specified default {@link VideoInterface}.
	 *
	 * @return Video interface
	 */
	public static VideoInterface loadManager( String pathToManager ) {
		try {
			Class c = Class.forName(pathToManager);
			return (VideoInterface) c.newInstance();
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Class not found.  Is it included in the class path?");
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

}
