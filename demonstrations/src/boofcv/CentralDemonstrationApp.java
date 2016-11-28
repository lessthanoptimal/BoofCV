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

package boofcv;

import boofcv.demonstrations.binary.DemoBinaryImageLabelOpsApp;
import boofcv.demonstrations.binary.DemoBinaryImageOpsApp;
import boofcv.demonstrations.calibration.*;
import boofcv.demonstrations.color.ShowColorModelApp;
import boofcv.demonstrations.denoise.DenoiseVisualizeApp;
import boofcv.demonstrations.distort.*;
import boofcv.gui.image.ShowImages;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * Application which lists most of the demonstration application in a GUI and allows the user to double click
 * to launch one in a new JVM.
 *
 * @author Peter Abeles
 */
public class CentralDemonstrationApp extends JPanel implements TreeSelectionListener {

	private JTree tree;

	public CentralDemonstrationApp() {
		setLayout(new BorderLayout());
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("All Categories");
		createNodes(root,"Binary",
				DemoBinaryImageLabelOpsApp.class,
				DemoBinaryImageOpsApp.class);

		createNodes(root,"Calibration",
				CalibrateMonoPlanarGuiApp.class,
				CalibrateStereoPlanarGuiApp.class,
				DetectCalibrationChessboardApp.class,
				DetectCalibrationCircleAsymmetricApp.class,
				DetectCalibrationSquareGridApp.class);

		createNodes(root,"Color",
				ShowColorModelApp.class);

		createNodes(root,"Denoise",
				DenoiseVisualizeApp.class);

		createNodes(root,"Distort",
				EquirectangularCylinderApp.class,
				EquirectangularPinholeApp.class,
				EquirectangularRotatingApp.class,
				FisheyePinholeApp.class,
				RemoveLensDistortionApp.class,
				ShowLensDistortion.class);

		tree = new JTree(root);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
//		tree.addTreeSelectionListener(this);

		MouseListener ml = new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				int selRow = tree.getRowForLocation(e.getX(), e.getY());
				TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
				if(e.getClickCount() == 2) {
					handleClick((DefaultMutableTreeNode)tree.getLastSelectedPathComponent());
				}
			}
		};
		tree.addMouseListener(ml);

		JScrollPane treeView = new JScrollPane(tree);

		add( treeView, BorderLayout.CENTER );

		setPreferredSize(new Dimension(400,600));
	}

	private void createNodes( DefaultMutableTreeNode root, String subjectName, Class ...apps) {
		DefaultMutableTreeNode top = new DefaultMutableTreeNode(subjectName);
		for (int i = 0; i < apps.length; i++) {
			DefaultMutableTreeNode node = new DefaultMutableTreeNode(new AppInfo(apps[i]));
			top.add(node);
		}
		root.add(top);
	}

	@Override
	public void valueChanged(TreeSelectionEvent e) {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
		if (node == null)
			return;
		if (!node.isLeaf())
			return;
		AppInfo info = (AppInfo)node.getUserObject();
		System.out.println("selected "+info);
	}

	public void handleClick( DefaultMutableTreeNode node ) {
		if (node == null)
			return;
		if (!node.isLeaf())
			return;
		AppInfo info = (AppInfo)node.getUserObject();
		System.out.println("clicked "+info);
	}

	public static class AppInfo {
		Class app;

		public AppInfo(Class app) {
			this.app = app;
		}

		@Override
		public String toString() {
			return app.getSimpleName();
		}
	}

	public static void main(String[] args) {
		CentralDemonstrationApp app = new CentralDemonstrationApp();
		ShowImages.showWindow(app,"Demonstration Launcher",true);
	}




}
