/*
 * Copyright (c) 2024, Peter Abeles. All Rights Reserved.
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

package boofcv.app;

import boofcv.gui.BoofSwingUtil;
import boofcv.gui.image.ShowImages;
import boofcv.gui.mesh.MeshViewerPanel;
import boofcv.io.points.PointCloudIO;
import boofcv.struct.mesh.VertexMesh;
import org.apache.commons.io.FilenameUtils;
import org.ddogleg.struct.DogArray_I32;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * Very simple app for opening and viewing a 3D mesh
 *
 * @author Peter Abeles
 */
public class MeshViewerApp {
	public MeshViewerApp() {
		main(new String[]{});
	}

	private static void loadFile( File file ) {
		// Load the mesh
		var mesh = new VertexMesh();
		var colors = new DogArray_I32();
		String extension = FilenameUtils.getExtension(file.getName()).toLowerCase(Locale.ENGLISH);
		var type = switch (extension) {
			case "ply" -> PointCloudIO.Format.PLY;
			case "stl" -> PointCloudIO.Format.STL;
			case "obj" -> PointCloudIO.Format.OBJ;
			default -> {
				throw new RuntimeException("Unknown file type");
			}
		};

		try (var input = new FileInputStream(file)) {
			PointCloudIO.load(type, input, mesh, colors);
		} catch (IOException e) {
			e.printStackTrace(System.err);
			System.exit(1);
		}

		if (mesh.size() == 0) {
			System.err.println("No shapes to render! Is this a point cloud?");
		}

		SwingUtilities.invokeLater(() -> {
			var panel = new MeshViewerPanel();
			panel.setMesh(mesh, false);
			if (colors.size > 0)
				panel.setVertexColors("RGB", colors.data);
			panel.setPreferredSize(new Dimension(500, 500));
			ShowImages.showWindow(panel, "Mesh Viewer", true);
		});
	}

	public static void main( String[] args ) {
		SwingUtilities.invokeLater(() -> {
			File file = BoofSwingUtil.openFileChooser("MeshViewer", BoofSwingUtil.FileTypes.MESH);
			if (file == null)
				return;

			new Thread(() -> loadFile(file)).start();
		});
	}
}
