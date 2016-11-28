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

package boofcv.gui;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Application which lists most of the demonstration application in a GUI and allows the user to double click
 * to launch one in a new JVM.
 *
 * @author Peter Abeles
 */
public abstract class ApplicationLauncherApp extends JPanel implements ActionListener {

	private JTree tree;

	JButton bKill = new JButton("Kill");
	JButton bKillAll = new JButton("Kill All");

	JList processList;
	DefaultListModel listModel = new DefaultListModel();

	int memoryMB = 1024;
	final List<ActiveProcess> processes = new ArrayList<>();

	public ApplicationLauncherApp() {
		setLayout(new BorderLayout());
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("All Categories");
		createTree(root);

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
		treeView.setPreferredSize(new Dimension(300,600));

		JPanel actionPanel = new JPanel();
		actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.X_AXIS));
		actionPanel.add(bKill);
		actionPanel.add(Box.createHorizontalGlue());
		actionPanel.add(bKillAll);
		bKill.addActionListener(this);
		bKillAll.addActionListener(this);

		processList = new JList(listModel);
		processList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		processList.setLayoutOrientation(JList.VERTICAL);
		processList.setVisibleRowCount(-1);
		processList.setPreferredSize(new Dimension(500,600));

		JPanel processPanel = new JPanel();
		processPanel.setLayout(new BoxLayout(processPanel, BoxLayout.Y_AXIS));
		processPanel.add( actionPanel );
		processPanel.add( processList );

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitPane.add(treeView);
		splitPane.add(processPanel);

		add( splitPane, BorderLayout.CENTER );

//		setPreferredSize(new Dimension(400,600));

		new ProcessStatusThread().start();
	}

	protected abstract void createTree( DefaultMutableTreeNode root );

	protected void createNodes( DefaultMutableTreeNode root, String subjectName, Class ...apps) {
		DefaultMutableTreeNode top = new DefaultMutableTreeNode(subjectName);
		for (int i = 0; i < apps.length; i++) {
			DefaultMutableTreeNode node = new DefaultMutableTreeNode(new AppInfo(apps[i]));
			top.add(node);
		}
		root.add(top);
	}

	private void launch( AppInfo info ) {
		List<String> classPath = new ArrayList<>();
		ClassLoader cl = ClassLoader.getSystemClassLoader();

		URL[] urls = ((URLClassLoader)cl).getURLs();

		for(URL url: urls){
			classPath.add( url.getFile());
		}

		final ActiveProcess process = new ActiveProcess();
		process.info = info;
		process.launcher = new JavaRuntimeLauncher(classPath);
		process.launcher.setFrozenTime(-1);
		process.launcher.setMemoryInMB(memoryMB);

		synchronized (processes) {
			processes.add(process);
		}

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				listModel.addElement(process);
				processList.invalidate();
			}
		});

		process.start();
	}

	public void handleClick( DefaultMutableTreeNode node ) {
		if (node == null)
			return;
		if (!node.isLeaf())
			return;
		AppInfo info = (AppInfo)node.getUserObject();
		System.out.println("clicked "+info);
		launch(info);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == bKill ) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					ActiveProcess selected = (ActiveProcess)processList.getSelectedValue();
					if( selected == null )
						return;

					selected.kill();
				}
			});
		} else if( e.getSource() == bKillAll ) {
			synchronized (processes ) {
				for (int i = 0; i < processes.size(); i++) {
					processes.get(i).kill();
				}
			}
		}
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

	public static class ActiveProcess extends Thread {
		AppInfo info;
		JavaRuntimeLauncher launcher;

		volatile boolean active = false;
		JavaRuntimeLauncher.Exit exit;

		@Override
		public void run() {
			active = true;
			exit = launcher.launch(info.app);
			System.out.println();
			System.out.println("------------------- Exit condition "+exit);
			active = false;
		}

		public void kill() {
			launcher.requestKill();
		}

		public boolean isActive() {
			return active;
		}

		@Override
		public String toString() {
			if( launcher.isKillRequested() && active ) {
				return "Killing "+info;
			} else {
				return info.toString();
			}
		}
	}

	class ProcessStatusThread extends Thread {
		@Override
		public void run() {
			while( true ) {
				synchronized (processes) {
					for (int i = processes.size()-1; i >= 0; i--) {
						final ActiveProcess p = processes.get(i);

						if( !p.isActive() ) {
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									listModel.removeElement(p);
									processList.invalidate();
								}
							});
							processes.remove(i);
						}
					}
				}

				try {
					sleep(250);
				} catch (InterruptedException e) {
					break;
				}
			}
		}
	}
}
