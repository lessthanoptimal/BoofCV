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

import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.distort.brown.LensDistortionBrown;
import boofcv.alg.geo.bundle.BundleAdjustmentOps;
import boofcv.alg.mvs.MultiViewStereoFromKnownSceneStructure.ViewInfo;
import boofcv.core.image.LookUpColorRgb;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.image.ImageBase;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import lombok.Getter;
import org.ddogleg.struct.FastQueue;

import java.util.List;

/**
 * Helper class which handles all the data structure manipulations for extracting RGB color values from a point
 * cloud computed by {@link MultiViewStereoFromKnownSceneStructure}. Color information is extracted only
 * using the first view a point was seen inside of.
 *
 * @author Peter Abeles
 */
public class ColorizeMultiViewStereoResults<T extends ImageBase<T>> {

	/** Extracts RGB color information from an image for a given point cloud */
	@Getter final ColorizeCloudFromImage<T> colorizer;
	/** Loads an image given a string id */
	@Getter final LookUpImages lookupImages;

	// Storage for loaded image
	final T image;

	// Storage for coordinate systems transforms
	Se3_F64 world_to_view = new Se3_F64();
	Se3_F64 tmp = new Se3_F64();

	// Camera model. Don't use bundle adjustment camera model directly since some models include image center
	// and other don't, which can cause problems
	CameraPinholeBrown intrinsic = new CameraPinholeBrown();

	/**
	 * Specifies configurations
	 *
	 * @param colorLookup Convert pixels into RGB color
	 * @param lookupImages Load images given a string iD
	 */
	public ColorizeMultiViewStereoResults( LookUpColorRgb<T> colorLookup, LookUpImages lookupImages ) {
		this.colorizer = new ColorizeCloudFromImage<>(colorLookup);
		this.lookupImages = lookupImages;
		this.image = colorizer.getColorLookup().getImageType().createImage(1, 1);
	}

	/**
	 * Extracts color information for the point cloud on a view by view basis.
	 *
	 * @param scene (Input) Geometric description of the scene
	 * @param mvs (Input) Contains the 3D point cloud
	 * @param indexColor (Output) RGB values are passed through to this function.
	 */
	public void process( SceneStructureMetric scene,
						 MultiViewStereoFromKnownSceneStructure<?> mvs,
						 IndexColor indexColor ) {

		// Get a list of views that were used as "centers"
		List<ViewInfo> centers = mvs.getListCenters();

		// Get the point cloud
		FastQueue<Point3D_F64> cloud = mvs.getDisparityCloud().getCloud();

		// Step through each "center" view
		for (int centerIdx = 0; centerIdx < centers.size(); centerIdx++) {
			ViewInfo center = centers.get(centerIdx);

			if (!lookupImages.loadImage(center.relations.id, image))
				throw new RuntimeException("Couldn't find image: " + center.relations.id);

			// Which points came from this view/center
			int idx0 = mvs.getDisparityCloud().viewPointIdx.get(centerIdx);
			int idx1 = mvs.getDisparityCloud().viewPointIdx.get(centerIdx + 1);

			// Setup the camera projection model using bundle adjustment model directly
			BundleAdjustmentOps.convert(scene.getViewCamera(center.metric).model,image.width,image.height,intrinsic);
			Point2Transform2_F64 norm_to_pixel = new LensDistortionBrown(intrinsic).distort_F64(false,true);

			// Get the transform from world/cloud to this view
			scene.getWorldToView(center.metric, world_to_view, tmp);

			// Grab the colorized points from this view
			colorizer.process3(image, cloud.toList(), idx0, idx1, world_to_view, norm_to_pixel, indexColor);
		}
	}
}
