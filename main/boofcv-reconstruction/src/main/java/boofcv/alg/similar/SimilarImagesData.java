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

package boofcv.alg.similar;

import boofcv.alg.structure.LookUpSimilarImages;
import boofcv.misc.BoofLambdas;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.image.ImageDimension;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Storage for the raw results of finding similar images
 *
 * @author Peter Abeles
 */
public class SimilarImagesData implements LookUpSimilarImages {
	public final List<String> listImages = new ArrayList<>();

	public final Map<String, Info> imageMap = new HashMap<>();

	public void reset() {
		listImages.clear();
		imageMap.clear();
	}

	public void add( String id, int width, int height, List<Point2D_F64> features, List<String> similar ) {
		Info info = new Info();
		info.shape.setTo(width, height);
		info.similar.addAll(similar);
		info.features.copyAll(features, ( src, dst ) -> dst.setTo(src));

		listImages.add(id);
		imageMap.put(id, info);
	}

	@Override public List<String> getImageIDs() {
		return listImages;
	}

	@Override
	public void findSimilar( String target, @Nullable BoofLambdas.Filter<String> filter, List<String> similarImages ) {
		similarImages.clear();
		Info info = imageMap.get(target);
		for (int i = 0; i < info.similar.size(); i++) {
			if (filter == null || filter.keep(info.similar.get(i))) {
				similarImages.add(info.similar.get(i));
			}
		}
	}

	@Override public void lookupPixelFeats( String target, DogArray<Point2D_F64> features ) {
		features.reset();
		features.copyAll(imageMap.get(target).features.toList(), ( src, dst ) -> dst.setTo(src));
	}

	@Override public boolean lookupAssociated( String similarD, DogArray<AssociatedIndex> pairs ) {
		return false;
	}

	public static class Info {
		public final List<String> similar = new ArrayList<>();
		public final ImageDimension shape = new ImageDimension();
		public final DogArray<Point2D_F64> features = new DogArray<>(Point2D_F64::new);
	}
}
