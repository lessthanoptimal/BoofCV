/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.tracker.pklt;

import gecv.alg.tracker.klt.KltFeature;

/**
 * Feature which is tracked by the {@link PyramidKltTracker}.  Each layer has its own feature description.
 *
 * @author Peter Abeles
 */
// todo comment
public class PyramidKltFeature {
	KltFeature desc[];
	float x;
	float y;
	int maxLayer;

	public PyramidKltFeature(int numLayers, int radius) {
		desc = new KltFeature[numLayers];

		for (int i = 0; i < numLayers; i++) {
			desc[i] = new KltFeature(radius);
		}
	}

	public void setPosition(float x, float y) {
		this.x = x;
		this.y = y;
	}
}
