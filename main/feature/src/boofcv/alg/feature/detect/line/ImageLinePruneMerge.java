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

package boofcv.alg.feature.detect.line;

import georegression.metric.Distance2D_F32;
import georegression.metric.Intersection2D_F32;
import georegression.metric.UtilAngle;
import georegression.struct.line.LineParametric2D_F32;
import georegression.struct.line.LineSegment2D_F32;
import georegression.struct.point.Point2D_F32;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class ImageLinePruneMerge {

	List<Data> lines = new ArrayList<>();

	public void reset() {
		lines.clear();
	}

	public void add( LineParametric2D_F32 line , float intensity ) {
		lines.add( new Data(line,intensity));
	}

	public void pruneRelative( float fraction ) {
		float max = 0;
		for( Data d : lines ) {
			if( d.intensity > max )
				max = d.intensity;
		}

		float threshold = max*fraction;

		List<Data> filtered = new ArrayList<>();
		for( Data d : lines ) {
			if( d.intensity >= threshold ) {
				filtered.add(d);
			}
		}
		lines = filtered;
	}

	public void pruneNBest( int N ) {
		if( lines.size() <= N )
			return;

		sortByIntensity();

		List<Data> filtered = new ArrayList<>();
		for( int i = 0; i < N; i++ ) {
			filtered.add(lines.get(i));
		}
		lines = filtered;
	}

	private void sortByIntensity() {
		Collections.sort(lines, new Comparator<Data>() {
			@Override
			public int compare(Data o1, Data o2) {
				if (o1.intensity < o2.intensity)
					return 1;
				else if (o1.intensity > o2.intensity)
					return -1;
				else
					return 0;
			}
		});
	}

	public void pruneSimilar(float toleranceAngle, float toleranceDist, int imgWidth, int imgHeight) {
		sortByIntensity();

		float theta[] = new float[ lines.size() ];
		List<LineSegment2D_F32> segments = new ArrayList<>(lines.size());

		for( int i = 0; i < lines.size(); i++ ) {
			Data d = lines.get(i);
			LineParametric2D_F32 l = d.line;
			theta[i] = UtilAngle.atanSafe(l.getSlopeY(), l.getSlopeX());
			segments.add( LineImageOps.convert(l, imgWidth, imgHeight));
		}

		for( int i = 0; i < segments.size(); i++ ) {
			LineSegment2D_F32 a = segments.get(i);
			if( a == null ) continue;

			for( int j = i+1; j < segments.size(); j++) {
				LineSegment2D_F32 b = segments.get(j);

				if( b == null )
					continue;

				// see if they are nearly parallel
				if( UtilAngle.distHalf(theta[i],theta[j]) > toleranceAngle )
					continue;

				Point2D_F32 p = Intersection2D_F32.intersection(a, b, null);

				// see if it is nearly parallel and intersects inside the image
				if( p != null && p.x >= 0 && p.y >= 0 && p.x < imgWidth && p.y < imgHeight ) {
					segments.set(j,null);
				} else {
					// now just see if they are very close
					float distA = Distance2D_F32.distance(a, b.a);
					float distB = Distance2D_F32.distance(a, b.b);

					if( distA <= toleranceDist || distB < toleranceDist ) {
						segments.set(j,null);
					}
				}
			}
		}

		List<Data>  filtered = new ArrayList<>();

		for( int i = 0; i < segments.size(); i++ ) {
			if( segments.get(i) != null ) {
				filtered.add( lines.get(i));
			}
		}

		lines = filtered;
	}

	public List<LineParametric2D_F32> createList() {
		List<LineParametric2D_F32> ret = new ArrayList<>();
		for( Data d : lines ) {
			ret.add(d.line);
		}
		return ret;
	}

	private static class Data
	{
		LineParametric2D_F32 line;
		float intensity;

		private Data(LineParametric2D_F32 line, float intensity) {
			this.line = line;
			this.intensity = intensity;
		}
	}
}


