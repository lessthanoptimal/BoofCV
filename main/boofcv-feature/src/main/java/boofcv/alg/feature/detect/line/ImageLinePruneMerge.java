/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.line;

import georegression.metric.Distance2D_F32;
import georegression.metric.Intersection2D_F32;
import georegression.metric.UtilAngle;
import georegression.struct.line.LineParametric2D_F32;
import georegression.struct.line.LineSegment2D_F32;
import georegression.struct.point.Point2D_F32;
import org.ddogleg.struct.DogArray;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Functions for pruning and merging lines.
 *
 * @author Peter Abeles
 */
public class ImageLinePruneMerge {

	List<Data> lines = new ArrayList<>();

	DogArray<LineSegment2D_F32> segments = new DogArray<>(LineSegment2D_F32::new, LineSegment2D_F32::zero);

	public void reset() {
		lines.clear();
	}

	public void add( LineParametric2D_F32 line, float intensity ) {
		lines.add(new Data(line, intensity));
	}

	public void pruneRelative( float fraction ) {
		float max = 0;
		for (int lineIdx = 0; lineIdx < lines.size(); lineIdx++) {
			Data d = lines.get(lineIdx);
			if (d.intensity > max)
				max = d.intensity;
		}

		float threshold = max*fraction;

		List<Data> filtered = new ArrayList<>();
		for (int lineIdx = 0; lineIdx < lines.size(); lineIdx++) {
			Data d = lines.get(lineIdx);
			if (d.intensity >= threshold) {
				filtered.add(d);
			}
		}
		lines = filtered;
	}

	public void pruneNBest( int N ) {
		if (lines.size() <= N)
			return;

		sortByIntensity();

		List<Data> filtered = new ArrayList<>();
		for (int i = 0; i < N; i++) {
			filtered.add(lines.get(i));
		}
		lines = filtered;
	}

	private void sortByIntensity() {
		Collections.sort(lines, ( o1, o2 ) -> {
			// need to sort by location to make results repeatable even if input order has been shuffled
			// that happens if concurrency is turned on
			if (o1.intensity < o2.intensity)
				return 1;
			else if (o1.intensity > o2.intensity)
				return -1;
			else if (o1.line.p.x < o2.line.p.x) {
				return -1;
			} else if (o1.line.p.x > o2.line.p.x) {
				return 1;
			} else {
				return Float.compare(o1.line.p.y, o2.line.p.y);
			}
		});
	}

	public void pruneSimilar( float toleranceAngle, float toleranceDist, int imgWidth, int imgHeight ) {
		sortByIntensity();

		float[] theta = new float[lines.size()];
		segments.reset().resize(lines.size());

		for (int i = 0; i < lines.size(); i++) {
			Data d = lines.get(i);
			LineParametric2D_F32 l = d.line;
			theta[i] = UtilAngle.atanSafe(l.getSlopeY(), l.getSlopeX());
			segments.get(i).setTo(LineImageOps.convert(l, imgWidth, imgHeight));
		}

		for (int i = 0; i < segments.size(); i++) {
			LineSegment2D_F32 a = segments.get(i);
			if (a == null) continue;

			for (int j = i + 1; j < segments.size(); j++) {
				LineSegment2D_F32 b = segments.get(j);

				if (b == null)
					continue;

				// see if they are nearly parallel
				if (UtilAngle.distHalf(theta[i], theta[j]) > toleranceAngle)
					continue;

				// NOTE: I don't like the way this distance metric looks. Seems arbitrary and will vary depending on
				//       the image size.

				Point2D_F32 p = Intersection2D_F32.intersection(a, b, null);

				// If they intersect inside the image they are much more likely to be the same line
				boolean close = false;
				if (p != null) {
					if (p.x >= 0 && p.y >= 0 && p.x < imgWidth && p.y < imgHeight) {
						close = true;
					}
				}

				// While a bit arbitrary look at the distance at the image border as a measure of how visually
				// similar two lines are
				if (!close) {
					// now just see if they are very close
					float distA = Distance2D_F32.distance(a, b.a);
					float distB = Distance2D_F32.distance(a, b.b);

					if (distA > toleranceDist && distB > toleranceDist) {
						continue;
					}

					// These distances will probably be the same as the previously computed distA and distB
					// someone should try to prove/disprove this... Maybe if the intersection doesn't lie on
					// the same image border line?

					distA = Distance2D_F32.distance(b, a.a);
					distB = Distance2D_F32.distance(b, a.b);

					if (distA > toleranceDist && distB > toleranceDist) {
						continue;
					}

					close = true;
				}

				if (close) {
					if (lines.get(j).intensity > lines.get(i).intensity) {
						lines.get(i).intensity = lines.get(j).intensity;
					}

					// Mark it so that it will be filtered
					segments.get(j).a.x = Float.NaN;
				}
			}
		}

		var filtered = new ArrayList<Data>();

		for (int i = 0; i < segments.size(); i++) {
			if (!Float.isNaN(segments.get(i).a.x)) {
				filtered.add(lines.get(i));
			}
		}

		lines = filtered;
	}

	public List<LineParametric2D_F32> createList( @Nullable List<LineParametric2D_F32> ret ) {
		if (ret == null)
			ret = new ArrayList<>();
		else
			ret.clear();

		for (int lineIdx = 0; lineIdx < lines.size(); lineIdx++) {
			Data d = lines.get(lineIdx);
			ret.add(d.line);
		}
		return ret;
	}

	private static class Data {
		LineParametric2D_F32 line;
		float intensity;

		private Data( LineParametric2D_F32 line, float intensity ) {
			this.line = line;
			this.intensity = intensity;
		}
	}
}
