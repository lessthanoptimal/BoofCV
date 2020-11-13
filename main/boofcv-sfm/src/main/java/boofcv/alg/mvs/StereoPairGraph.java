/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.mvs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Specifies which views can be used as stereo pairs and the quality of the 3D information between the views
 *
 * @author Peter Abeles
 */
public class StereoPairGraph {
	/** List of all the views */
	public final Map<String,View> views = new HashMap<>();

	public void reset() {
		views.clear();
	}

	public static class View {
		/** The view this is in reference to */
		public String id = "";
		/** List of all views it can form a 3D stereo pair with */
		public final List<Pair> pairs = new ArrayList<>();
	}

	public static class Pair {
		/** Which view it's connected to */
		public String id = "";
		/** How good the 3D information is between these two views. 0.0 = no 3D. 1.0 = best possible. */
		public double quality3D = 0.0;
	}
}
