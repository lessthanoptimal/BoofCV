/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Storage for the raw results of finding similar images. Everything is stored in memory in an uncompressed format.
 *
 * @author Peter Abeles
 */
public class SimilarImagesData implements LookUpSimilarImages {
	public final List<String> listImages = new ArrayList<>();
	public final Map<String, Info> imageMap = new HashMap<>();

	// Which view was referenced when findSImilar() was last called.
	@Nullable Info targetInfo;

	/**
	 * Clears all references to other objects
	 */
	public void reset() {
		listImages.clear();
		imageMap.clear();
		targetInfo = null;
	}

	/**
	 * Adds a new view.
	 *
	 * @param id The unique ID for the view
	 * @param features List of image features. Pixel coordinates.
	 */
	public void add( String id, List<Point2D_F64> features ) {
		var info = new Info();
		info.index = listImages.size();
		info.features.copyAll(features, ( src, dst ) -> dst.setTo(src));
		listImages.add(id);
		imageMap.put(id, info);
	}

	/**
	 * Used to specify the relationship between two similar views by providing which features match up
	 *
	 * @param viewA The 'src' view of the matches
	 * @param viewB The 'dst' view of the matches
	 * @param matches List of matching image features
	 */
	public void setRelationship( String viewA, String viewB, List<AssociatedIndex> matches ) {
		Info infoA = Objects.requireNonNull(imageMap.get(viewA));
		Info infoB = Objects.requireNonNull(imageMap.get(viewB));

		infoA.similarViews.add(viewB);
		infoB.similarViews.add(viewA);

		// Similar data is only stored in the low index view because it's symmetric
		boolean swapped = false;
		if (infoA.index > infoB.index) {
			Info tmp = infoA;
			infoA = infoB;
			infoB = tmp;
			swapped = true;
		}

		// Copy over the matches, but make sure infoA is the src
		Relationship related = new Relationship(listImages.get(infoB.index));
		infoA.relationships.add(related);
		related.pairs.resize(matches.size());
		for (int i = 0; i < matches.size(); i++) {
			AssociatedIndex a = related.pairs.get(i);
			AssociatedIndex b = matches.get(i);

			if (swapped) {
				a.setTo(/* src= */b.dst, /* dst= */b.src);
			} else {
				a.setTo(b);
			}
		}
	}

	@Override public List<String> getImageIDs() {
		return listImages;
	}

	@Override
	public void findSimilar( String target, @Nullable BoofLambdas.Filter<String> filter, List<String> similarImages ) {
		similarImages.clear();
		Info info = Objects.requireNonNull(imageMap.get(target));
		for (int i = 0; i < info.similarViews.size(); i++) {
			if (filter == null || filter.keep(info.similarViews.get(i))) {
				similarImages.add(info.similarViews.get(i));
			}
		}

		this.targetInfo = info;
	}

	@Override public void lookupPixelFeats( String target, DogArray<Point2D_F64> features ) {
		Info similarInfo = Objects.requireNonNull(imageMap.get(target));
		features.reset();
		features.copyAll(similarInfo.features.toList(), ( src, dst ) -> dst.setTo(src));
	}

	@Override public boolean lookupAssociated( String similarID, DogArray<AssociatedIndex> pairs ) {
		Objects.requireNonNull(targetInfo, "Must call findSimilar first");

		Info similarInfo = Objects.requireNonNull(imageMap.get(similarID));
		boolean swapped = targetInfo.index > similarInfo.index;

		if (swapped) {
			String targetID = listImages.get(targetInfo.index);
			Relationship related = Objects.requireNonNull(similarInfo.findRelated(targetID));
			pairs.resetResize(related.pairs.size);
			for (int i = 0; i < pairs.size; i++) {
				AssociatedIndex b = related.pairs.get(i);
				pairs.get(i).setTo(/* src= */b.dst, /* dst= */b.src);
			}
		} else {
			Relationship related = Objects.requireNonNull(targetInfo.findRelated(similarID));
			pairs.resetResize(related.pairs.size);
			for (int i = 0; i < pairs.size; i++) {
				pairs.get(i).setTo(related.pairs.get(i));
			}
		}

		return true;
	}

	/**
	 * All the information for a single view. Images being similar is symmetric, however matches are only
	 * saved in the lower indexed view to reduce memory.
	 */
	public static class Info {
		public int index;
		public final List<String> similarViews = new ArrayList<>();
		public final List<Relationship> relationships = new ArrayList<>();
		public final DogArray<Point2D_F64> features = new DogArray<>(Point2D_F64::new);

		public @Nullable SimilarImagesData.Relationship findRelated( String id ) {
			for (int i = 0; i < relationships.size(); i++) {
				if (relationships.get(i).id.equals(id))
					return relationships.get(i);
			}
			return null;
		}
	}

	/**
	 * Specifies how two views are related by saying which image features are matched with which other image features.
	 */
	public static class Relationship {
		public String id;
		DogArray<AssociatedIndex> pairs = new DogArray<>(AssociatedIndex::new);

		public Relationship( String id ) {
			this.id = id;
		}
	}
}
