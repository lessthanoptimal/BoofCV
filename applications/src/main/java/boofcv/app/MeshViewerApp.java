/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

import javax.swing.*;
import java.awt.*;
import java.io.File;

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
		SwingUtilities.invokeLater(() -> {
			var panel = new MeshViewerPanel();
			// Load the mesh outside the UI thread since it can be slow
			new Thread(() -> {
				if (!panel.loadMesh(file)) {
					System.err.println("Failed to load mesh " + file.getPath());
					System.exit(1);
				}
				// Finish setting up the panel
				SwingUtilities.invokeLater(() -> {
					if (panel.getMesh().rgb.size > 0)
						panel.setVertexColors("RGB", panel.getMesh().rgb.data);
					panel.setPreferredSize(new Dimension(500, 500));
					ShowImages.showWindow(panel, "Mesh Viewer", true);
				});
			}, "MeshLoad").start();
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
