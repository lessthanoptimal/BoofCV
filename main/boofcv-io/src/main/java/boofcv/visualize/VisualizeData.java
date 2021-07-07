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

package boofcv.visualize;

import boofcv.io.video.VideoInterface;

import java.lang.reflect.InvocationTargetException;

public class VisualizeData {

	public static PointCloudViewer createPointCloudViewer() {

		PointCloudViewer viewer = null;
		try {
			viewer = loadGenerator("boofcv.javafx.PointCloudViewerFX");
		} catch (RuntimeException ignore) {}

		if (viewer == null) {
			try {
				viewer = loadGenerator("boofcv.gui.d3.PointCloudViewerSwing");
			} catch (RuntimeException ignore) {}
		}

		if (viewer == null)
			throw new RuntimeException("You must either add swing or javafx integration");

		return viewer;
	}

	/**
	 * Loads the specified default {@link VideoInterface}.
	 *
	 * @return Video interface
	 */
	public static <T> T loadGenerator(String pathToGenerator) {
		try {
			Class c = Class.forName(pathToGenerator);
			return (T) c.getConstructor().newInstance();
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Class not found. Is it included in the class path?");
		} catch (InstantiationException | IllegalAccessException |
				NoSuchMethodException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
}
