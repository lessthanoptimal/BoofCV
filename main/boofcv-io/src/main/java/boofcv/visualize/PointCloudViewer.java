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

import boofcv.alg.cloud.AccessColorIndex;
import boofcv.alg.cloud.AccessPointIndex;
import boofcv.struct.Point3dRgbI_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_F32;
import org.ddogleg.struct.DogArray_I32;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

/**
 * High level interface for displaying point clouds
 *
 * @author Peter Abeles
 */
public interface PointCloudViewer {

	/**
	 * Render the XYZ axis on the screen
	 * @param show
	 */
	void setShowAxis( boolean show );

	void setTranslationStep( double step );

	/**
	 * Dot size when rendered. This is only valid if sprites are being used
	 * @param pixels apparent size of a point
	 */
	void setDotSize( int pixels );

	/**
	 * Specifies the clipping distance. The default value will be infinity or some other very large value
	 * @param distance maximum distance an object away from the camera can be seen
	 */
	void setClipDistance( double distance );

	/**
	 * If true then objects farther away will fade into the background color. Providing some sense of depth.
	 * this is by default off.
	 *
	 * @param active true to turn on
	 */
	void setFog( boolean active );

	void setBackgroundColor( int rgb );

	/**
	 * Adds a point cloud to the viewer
	 * @param iterator Iterator with 3D point and optionally color information
	 * @param hasColor true if the iterator has valid color
	 */
	void addCloud( IteratePoint iterator, boolean hasColor );

	/**
	 * Adds the point cloud using a data structure that can be access by index and has a known size. Knowing
	 * the size can allow the internal implementation to preallocate memory.
	 *
	 * @param accessPoint Accessor to point information
	 * @param accessColor Accessor to RGB color information. If null then 0xFF0000 is assumed.
	 * @param size Number of elements
	 */
	default void addCloud( AccessPointIndex<Point3D_F64> accessPoint,
						   @Nullable AccessColorIndex accessColor, int size ) {
		if (accessColor==null) {
			addCloud(new IteratePoint() {
				int index = 0;
				@Override public int next( Point3D_F64 point ) {
					accessPoint.getPoint(index++, point);
					return -1;
				}

				@Override public boolean hasNext() {return index < size;}
			}, false);
		} else {
			addCloud(new IteratePoint() {
				int index = 0;
				@Override public int next( Point3D_F64 point ) {
					accessPoint.getPoint(index, point);
					return accessColor.getRGB(index++);
				}

				@Override public boolean hasNext() {return index < size;}
			}, true);
		}
	}

	@Deprecated
	default void addCloud( List<Point3D_F64> cloudXyz , int []colorsRgb ) {
		addCloud((index, p)->p.setTo(cloudXyz.get(index)), (index)->colorsRgb[index], cloudXyz.size());
	}

	@Deprecated
	default void addCloud( List<Point3D_F64> cloud ) {
		addCloud((index, p)->p.setTo(cloud.get(index)), null, cloud.size());
	}

	@Deprecated
	default void addCloud(DogArray_F32 cloudXYZ , DogArray_I32 colorRGB ) {
		int size = cloudXYZ.size/3;
		addCloud((index, p)->{
			int i = index*3;
			p.setTo(cloudXYZ.data[i],cloudXYZ.data[i+1],cloudXYZ.data[i+2]);
		}, colorRGB::get, size);
	}

	/**
	 * adds a single point to the point cloud. This method can be very slow compared to doing it in a batch
	 */
	void addPoint( double x , double y , double z , int rgb );

	/**
	 * Adds a spite wireframe to the view. Since it's a sprite the thickness is independent of distance.
	 */
	void addWireFrame( List<Point3D_F64> vertexes , boolean closed , int rgb , int radiusPixels );

	/**
	 * Removes all points from the point cloud
	 */
	void clearPoints();

	/**
	 * Used to assign colors to points using a custom function based on their position and/or index. If
	 * a color is specified it will override it
	 */
	void setColorizer( Colorizer colorizer );

	/**
	 * If a colorizer has been specified this will remove it
	 */
	void removeColorizer();

	/**
	 * Specifies the camera's FOV in radians
	 * @param radians FOV size
	 */
	void setCameraHFov(double radians );

	/**
	 * Changes the camera location
	 * @param cameraToWorld transform from camera to world coordinates
	 */
	void setCameraToWorld(Se3_F64 cameraToWorld );

	/**
	 * Returns a copy of the camera to world transform currently being used
	 * @param storage (Optional) storage for the transform
	 * @return The transform
	 */
	Se3_F64 getCameraToWorld( @Nullable Se3_F64 storage );

	/**
	 * Copies the point cloud into the passed in list.
	 *
	 * @param copy Where the cloud should be copied into. if null a new instance is created
	 * @return The copy
	 */
	DogArray<Point3dRgbI_F64> copyCloud(@Nullable DogArray<Point3dRgbI_F64> copy);

	/**
	 * Returns a swing component for adding to a GUI
	 */
	JComponent getComponent();

	/**
	 * Computes the color for a point
	 */
	interface Colorizer {
		int color( int index , double x , double y , double z );
	}

	/** Iterator like interface for accessing point information */
	interface IteratePoint {
		/** Loads the next point and returns its color. If no color is available it should return 0 */
		int next( Point3D_F64 point );

		/** True if there ar remaining points */
		boolean hasNext();
	}
}
