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

package boofcv.gui.d3;

import boofcv.alg.cloud.AccessColorIndex;
import boofcv.alg.cloud.AccessPointIndex;
import boofcv.gui.BoofSwingUtil;
import boofcv.struct.PackedArray;
import boofcv.struct.Point3dRgbI_F64;
import boofcv.visualize.PointCloudViewer;
import georegression.struct.ConvertFloatType;
import georegression.struct.point.Point3D_F32;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F32;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.BigDogArray_I32;
import org.ddogleg.struct.DogArray;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

/**
 * Wrapper around {@link PointCloudViewerPanelSwing} for {@link PointCloudViewer}.
 *
 * @author Peter Abeles
 */
public class PointCloudViewerSwing implements PointCloudViewer {
	PointCloudViewerPanelSwing panel;

	public PointCloudViewerSwing() {
		panel = new PointCloudViewerPanelSwing((float)60,(float)1.0 );
	}

	@Override
	public void setShowAxis(boolean show) {
		// Not implemented yet. Doing correctly is a bit of work. Need to compute the 3D location of each rendered
		// point on a line
	}

	@Override
	public void setTranslationStep(double step) {
		BoofSwingUtil.invokeNowOrLater(()->panel.setStepSize((float)step));
	}

	@Override
	public void setDotSize(int pixels) {
		BoofSwingUtil.invokeNowOrLater(()->panel.setDotRadius(pixels));
	}

	@Override
	public void setClipDistance(double distance) {
		BoofSwingUtil.invokeNowOrLater(()->panel.maxRenderDistance = (float)distance);
	}

	@Override
	public void setFog(boolean active) {
		BoofSwingUtil.invokeNowOrLater(()->panel.fog = active);
	}

	@Override
	public void setBackgroundColor(int rgb) {
		BoofSwingUtil.invokeNowOrLater(()->panel.backgroundColor=rgb);
	}

	@Override public void addCloud( IteratePoint iterator, boolean hasColor ) {
		Point3D_F64 p = new Point3D_F64();
		if( SwingUtilities.isEventDispatchThread() ) {
			while (iterator.hasNext()) {
				int rgb = iterator.next(p);
				panel.addPoint((float) p.x, (float) p.y, (float) p.z, hasColor ? rgb : 0xFF000000);
			}
		} else {
			SwingUtilities.invokeLater(() -> {
				while (iterator.hasNext()) {
					int rgb = iterator.next(p);
					panel.addPoint((float) p.x, (float) p.y, (float) p.z, hasColor ? rgb : 0xFF000000);
				}
			});
		}
	}

	@Override public void addCloud( AccessPointIndex<Point3D_F64> accessPoint, @Nullable AccessColorIndex accessColor, int size ) {
		Point3D_F64 p = new Point3D_F64();
		if( SwingUtilities.isEventDispatchThread() ) {
			for (int i = 0; i < size; i++) {
				accessPoint.getPoint(i, p);
				int rgb = accessColor == null ? 0xFF000000 : accessColor.getRGB(i);
				panel.addPoint((float) p.x, (float) p.y, (float) p.z, rgb);
			}
		} else {
			SwingUtilities.invokeLater(() -> {
				for (int i = 0; i < size; i++) {
					accessPoint.getPoint(i, p);
					int rgb = accessColor == null ? 0xFF000000 : accessColor.getRGB(i);
					panel.addPoint((float) p.x, (float) p.y, (float) p.z, rgb);
				}
			});
		}
	}

	@Override
	public void addPoint(double x, double y, double z, int rgb) {
		if( SwingUtilities.isEventDispatchThread() ) {
			panel.addPoint((float)x,(float)y,(float)z,rgb);
		} else {
			SwingUtilities.invokeLater(() -> {
				panel.addPoint((float) x, (float) y, (float) z, rgb);
			});
		}
	}

	@Override
	public void addWireFrame(List<Point3D_F64> vertexes, boolean closed, int rgb, int widthPixels) {
		BoofSwingUtil.invokeNowOrLater(()-> panel.addWireFrame(vertexes,closed,rgb,widthPixels));
	}

	@Override
	public void clearPoints() {
		if( SwingUtilities.isEventDispatchThread() ) {
			panel.clearCloud();
		} else {
			SwingUtilities.invokeLater(() -> panel.clearCloud());
		}
	}

	@Override
	public void setColorizer(Colorizer colorizer) {
		panel.colorizer = colorizer;
	}

	@Override
	public void removeColorizer() {
		panel.colorizer = null;
	}

	@Override
	public void setCameraHFov(double radians) {
		BoofSwingUtil.invokeNowOrLater(()->panel.setHorizontalFieldOfView((float)radians));
	}

	@Override
	public void setCameraToWorld(Se3_F64 cameraToWorld) {
		Se3_F64 worldToCamera = cameraToWorld.invert(null);
		Se3_F32 worldToCameraF32 = new Se3_F32();
		ConvertFloatType.convert(worldToCamera,worldToCameraF32);
		BoofSwingUtil.invokeNowOrLater(()->panel.setWorldToCamera(worldToCameraF32));
	}

	@Override
	public Se3_F64 getCameraToWorld(@Nullable Se3_F64 cameraToWorld) {
		if( cameraToWorld == null )
			cameraToWorld = new Se3_F64();
		Se3_F32 worldToCamera = panel.getWorldToCamera(null);
		ConvertFloatType.convert(worldToCamera.invert(null),cameraToWorld);
		return cameraToWorld;
	}

	@Override
	public DogArray<Point3dRgbI_F64> copyCloud(@Nullable DogArray<Point3dRgbI_F64> copy) {
		if( copy == null )
			copy = new DogArray<>(Point3dRgbI_F64::new);
		else
			copy.reset();

		DogArray<Point3dRgbI_F64> _copy = copy;

		// See if it has color information on the points or not
		final PackedArray<Point3D_F32> cloudXyz = panel.getCloudXyz();
		final BigDogArray_I32 cloudColor = panel.getCloudColor();
		synchronized (cloudXyz) {
			if (cloudXyz.size()==cloudColor.size) {
				cloudXyz.forIdx(0, cloudXyz.size(), ( idx, point ) ->
						_copy.grow().setTo(point.x, point.y, point.z, cloudColor.get(idx)));
			} else {
				cloudXyz.forIdx(0, cloudXyz.size(), ( idx, point ) ->
						_copy.grow().setTo(point.x, point.y, point.z));
			}
		}

		return copy;
	}

	@Override
	public JComponent getComponent() {
		return panel;
	}
}
