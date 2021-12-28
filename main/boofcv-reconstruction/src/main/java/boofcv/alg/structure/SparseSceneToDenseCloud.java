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

package boofcv.alg.structure;

import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.mvs.ColorizeMultiViewStereoResults;
import boofcv.alg.mvs.MultiViewStereoFromKnownSceneStructure;
import boofcv.core.image.LookUpColorRgbFormats;
import boofcv.misc.LookUpImages;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point3D_F64;
import gnu.trove.map.TIntObjectMap;
import lombok.Getter;
import org.ddogleg.struct.DogArray_I32;

import java.util.List;

/**
 * Takes in a known sparse scene that's in SBA format and converts it into a dense point cloud.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class SparseSceneToDenseCloud<T extends ImageGray<T>> {

	/** MVS algorithm */
	private final MultiViewStereoFromKnownSceneStructure<T> mvs;
	/** Finds and scores stereo pairs from sparse scene graph */
	private final @Getter GenerateStereoPairGraphFromScene generateGraph = new GenerateStereoPairGraphFromScene();
	/** RGB colors in found dense point cloud */
	private final @Getter DogArray_I32 colorRgb = new DogArray_I32();

	// Profiling times in milliseconds
	@Getter double timeCreateGraphMS;
	@Getter double timeMultiViewStereoMS;
	@Getter double timeColorizeMS;

	/** Format of gray scale image it processes */
	@Getter Class<T> grayType;

	public SparseSceneToDenseCloud( Class<T> imageType ) {
		mvs = new MultiViewStereoFromKnownSceneStructure<>(ImageType.single(imageType));
	}

	/**
	 * Uses the given sparse scene to compute a dense 3D cloud
	 *
	 * @param scene (Input) sparse scene that is assumed to be known perfectly
	 * @param viewIdx_to_ImageID (Input) Lookup table to go from an images index to its name
	 * @param lookUpImages Used to lookup images by name
	 * @return true if successful or false if it failed
	 */
	public boolean process( SceneStructureMetric scene, TIntObjectMap<String> viewIdx_to_ImageID,
							LookUpImages lookUpImages ) {
		// reset variables
		timeCreateGraphMS = 0;
		timeMultiViewStereoMS = 0;
		timeColorizeMS = 0;

		// Select stereo pairs to use and score them
		long time0 = System.nanoTime();
		generateGraph.process(viewIdx_to_ImageID, scene);
		long time1 = System.nanoTime();
		timeCreateGraphMS = (time1 - time0)*1e-6;

		// Compute the dense cloud
		mvs.setImageLookUp(lookUpImages);
		mvs.process(scene, generateGraph.getStereoGraph());
		long time2 = System.nanoTime();
		timeMultiViewStereoMS = (time2 - time1)*1e-6;

		// Extract colors from cloud
		colorRgb.resize(mvs.getCloud().size());
		var colorizeMvs = new ColorizeMultiViewStereoResults<>(new LookUpColorRgbFormats.PL_U8(), lookUpImages);
		colorizeMvs.processMvsCloud(scene, mvs, ( idx, r, g, b ) -> colorRgb.set(idx, (r << 16) | (g << 8) | b));
		long time3 = System.nanoTime();
		timeColorizeMS = (time3 - time2)*1e-6;

		return true;
	}

	/**
	 * Returns the generated dense 3D point cloud
	 */
	public List<Point3D_F64> getCloud() {
		return mvs.getCloud();
	}

	/** Returns the MVS used internally */
	public MultiViewStereoFromKnownSceneStructure<T> getMultiViewStereo() {
		return mvs;
	}
}
