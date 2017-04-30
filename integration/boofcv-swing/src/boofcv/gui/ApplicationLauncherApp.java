/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

import boofcv.io.UtilIO;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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
public abstract class ApplicationLauncherApp extends JPanel implements ActionListener, ListDataListener {

	private JTree tree;
	private final int OUTPUT = 0;
	private final int SOURCE = 1;
	private OutputStreamCapturer outputStreamCapturer;

	JButton bKill = new JButton("Kill");
	JButton bKillAll = new JButton("Kill All");

	JList processList;
	DefaultListModel<ActiveProcess> listModel = new DefaultListModel<>();
	JTabbedPane outputPanel = new JTabbedPane();

	int memoryMB = 1024;
	final List<ActiveProcess> processes = new ArrayList<>();
	String path = "";

	public ApplicationLauncherApp() {
		setLayout(new BorderLayout());
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("All Categories");
		createTree(root);

		tree = new JTree(root);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
//		tree.addTreeSelectionListener(this);

		final JTextArea sourceTextArea = new JTextArea();
		JTextArea outputTextArea = new JTextArea();

		outputTextArea.setEditable(false);
		outputTextArea.setLineWrap(true);
		outputTextArea.setColumns(180);

		sourceTextArea.setEditable(false);
		sourceTextArea.setLineWrap(true);
		sourceTextArea.setColumns(180);
		sourceTextArea.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				super.mousePressed(e);
				if(SwingUtilities.isRightMouseButton(e) && !sourceTextArea.getText().isEmpty()) {
					JPopupMenu menu = new JPopupMenu();
					JMenuItem copy = new JMenuItem("Copy");
					copy.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
							clipboard.setContents(new StringSelection(sourceTextArea.getText()), null);
						}
					});
					menu.add(copy);
					menu.show(sourceTextArea, e.getX(), e.getY());
				}
			}
		});

		MouseListener ml = new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				int selRow = tree.getRowForLocation(e.getX(), e.getY());
				TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
				if(e.getClickCount() == 2) {
					handleClick((DefaultMutableTreeNode)tree.getLastSelectedPathComponent());
				}
				else if(SwingUtilities.isRightMouseButton(e)) {
					handleContextMenu(tree, e.getX(), e.getY());
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
		processList.getModel().addListDataListener(this);

		JPanel processPanel = new JPanel();
		processPanel.setLayout(new BoxLayout(processPanel, BoxLayout.Y_AXIS));
		processPanel.add( actionPanel );
		processPanel.add( processList );

		JPanel intermediateOutputContainer = new JPanel();
		intermediateOutputContainer.setLayout(new BorderLayout());
		JCheckBox outputCheckbox = new JCheckBox("Display output");
		intermediateOutputContainer.add(outputCheckbox, BorderLayout.NORTH);
		intermediateOutputContainer.add(outputTextArea, BorderLayout.CENTER);

		outputCheckbox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange() == ItemEvent.DESELECTED)
					outputStreamCapturer.toStdOutput();
				else
					outputStreamCapturer.toDisplayOutput();
			}
		});

		JScrollPane outputContainer = new JScrollPane(intermediateOutputContainer);
		outputContainer.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

		JScrollPane sourceContainer = new JScrollPane(sourceTextArea);
		sourceContainer.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);


		outputPanel.add(outputContainer, OUTPUT);
		outputPanel.add(sourceContainer, SOURCE);
		outputPanel.setTitleAt(OUTPUT, "Output");
		outputPanel.setTitleAt(SOURCE, "Source");

		JSplitPane verticalSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		verticalSplitPane.setDividerLocation(0.5);
		verticalSplitPane.setResizeWeight(0.5);
		verticalSplitPane.add(processPanel);
		verticalSplitPane.add(outputPanel);

		//needed to initialize vertical divider to 0.5 weight
		verticalSplitPane.setPreferredSize(new Dimension(500,600));

		//horizontal divider won't drag to the right without a minimum size
		verticalSplitPane.setMinimumSize(new Dimension(1,1));

		JSplitPane horizontalSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		horizontalSplitPane.setResizeWeight(0.5);
		horizontalSplitPane.add(treeView);
		horizontalSplitPane.add(verticalSplitPane);

		add( horizontalSplitPane, BorderLayout.CENTER );

		outputStreamCapturer = new OutputStreamCapturer(outputTextArea);
		outputCheckbox.setSelected(true);

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

	/**
	 * Displays a context menu for a class leaf node
	 * Allows copying of the name and the path to the source
	 * @param tree
	 * @param x
	 * @param y
	 */
	private void handleContextMenu(JTree tree, int x, int y) {
		TreePath path = tree.getPathForLocation(x, y);
		tree.setSelectionPath(path);
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

		if (node == null)
			return;
		if (!node.isLeaf()) {
			tree.setSelectionPath(null);
			return;
		}
		final AppInfo info = (AppInfo)node.getUserObject();

		JMenuItem copyname = new JMenuItem("Copy Name");
		copyname.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(new StringSelection(info.app.getSimpleName()), null);
			}
		});

		JMenuItem copypath = new JMenuItem("Copy Path");
		copypath.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String path = UtilIO.getSourcePath(info.app.getPackage().getName(), info.app.getSimpleName());
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(new StringSelection(path), null);
			}
		});

		JMenuItem github = new JMenuItem("Go to Github");
		github.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(Desktop.isDesktopSupported()) {
					try {

						URI uri = new URI(UtilIO.getGithubURL(info.app.getPackage().getName(), info.app.getSimpleName()));
						if(!uri.getPath().isEmpty())
							Desktop.getDesktop().browse(uri);
						else
							System.err.println("Bad URL received");
					} catch (Exception e1) {
						System.err.println("Something went wrong connecting to github");
						System.err.println(e1.getMessage());
					}
				}
			}
		});

		JPopupMenu submenu = new JPopupMenu();
		submenu.add(copyname);
		submenu.add(copypath);
		submenu.add(github);
		submenu.show(tree, x, y);
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

	@Override
	public void intervalAdded(ListDataEvent e) {
		//retrieve the most recently added process and display it
		DefaultListModel listModel = (DefaultListModel) e.getSource();
		ActiveProcess process = (ActiveProcess) listModel.get(listModel.getSize()-1);

		JTextArea source = (JTextArea) ((JScrollPane) outputPanel.getComponentAt(SOURCE)).getViewport().getView();
		displaySource(source, process);
	}

	@Override
	public void intervalRemoved(ListDataEvent e) {
		//retrieve the most recently added process and display it
		DefaultListModel listModel = (DefaultListModel) e.getSource();

		JTextArea source = (JTextArea) ((JScrollPane) outputPanel.getComponentAt(SOURCE)).getViewport().getView();

		//if the last process was just removed, return
		if(listModel.isEmpty()) {
			source.setText("");
			return;
		}

		ActiveProcess process = (ActiveProcess) listModel.get(listModel.getSize()-1);
		displaySource(source, process);
	}

	@Override
	public void contentsChanged(ListDataEvent e) {
	}

	private void displaySource(JTextArea sourceTextArea, ActiveProcess process) {
		String path = UtilIO.getSourcePath(process.info.app.getPackage().getName(), process.info.app.getSimpleName());
		File source = new File(path);
		if( source.exists() && source.canRead() ) {
			StringBuilder code = new StringBuilder();
			try {
				BufferedReader reader = new BufferedReader(new FileReader(source));
				String line;
				while((line = reader.readLine()) != null)
					code.append(line + System.lineSeparator());
			} catch (IOException e) {
				e.printStackTrace();
			}
			sourceTextArea.setText(code.toString());

			int scrollTo = code.toString().indexOf("class");
			scrollTo = scrollTo == -1 ? 0 : scrollTo;

			sourceTextArea.setCaretPosition(scrollTo);
		}
		else {
			sourceTextArea.setText("Source not found!");
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
