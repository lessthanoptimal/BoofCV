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
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
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

	JButton bKill = new JButton("Kill");
	JButton bKillAll = new JButton("Kill All");

	JList processList;
	DefaultListModel<ActiveProcess> listModel = new DefaultListModel<>();
	JTabbedPane outputPanel = new JTabbedPane();

	int memoryMB = 1024;
	final List<ActiveProcess> processes = new ArrayList<>();

	public ApplicationLauncherApp() {
		setLayout(new BorderLayout());
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("All Categories");
		createTree(root);

		tree = new JTree(root);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

		MouseListener ml = new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.getClickCount() == 2) {
					handleClick((DefaultMutableTreeNode) tree.getLastSelectedPathComponent());
				} else if (SwingUtilities.isRightMouseButton(e)) {
					handleContextMenu(tree, e.getX(), e.getY());
				}
			}
		};
		tree.addMouseListener(ml);

		JScrollPane treeView = new JScrollPane(tree);
		treeView.setPreferredSize(new Dimension(300, 600));

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
		processList.getModel().addListDataListener(this);

		JPanel processPanel = new JPanel();
		processPanel.setLayout(new BorderLayout());
		processPanel.add(actionPanel, BorderLayout.NORTH);
		processPanel.add(processList, BorderLayout.CENTER);

		JSplitPane verticalSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,processPanel,outputPanel);
		verticalSplitPane.setDividerLocation(150);
		// divider location won't change when window is resized
		// most of the time you want to increase the view of the text
		verticalSplitPane.setResizeWeight(0.0);

		JSplitPane horizontalSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,treeView,verticalSplitPane);
		horizontalSplitPane.setDividerLocation(250);
		horizontalSplitPane.setResizeWeight(0.0);

		add(horizontalSplitPane, BorderLayout.CENTER);
		new ProcessStatusThread().start();

		setPreferredSize(new Dimension(800,600));
	}


	protected abstract void createTree(DefaultMutableTreeNode root);

	protected void createNodes(DefaultMutableTreeNode root, String subjectName, Class... apps) {
		DefaultMutableTreeNode top = new DefaultMutableTreeNode(subjectName);
		for (int i = 0; i < apps.length; i++) {
			DefaultMutableTreeNode node = new DefaultMutableTreeNode(new AppInfo(apps[i]));
			top.add(node);
		}
		root.add(top);
	}

	private void launch(AppInfo info) {
		List<String> classPath = new ArrayList<>();
		ClassLoader cl = ClassLoader.getSystemClassLoader();

		URL[] urls = ((URLClassLoader) cl).getURLs();

		for (URL url : urls) {
			classPath.add(url.getFile());
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

	public void handleClick(DefaultMutableTreeNode node) {
		if (node == null)
			return;
		if (!node.isLeaf())
			return;
		AppInfo info = (AppInfo) node.getUserObject();
		launch(info);
		System.out.println("clicked " + info);
	}

	/**
	 * Displays a context menu for a class leaf node
	 * Allows copying of the name and the path to the source
	 *
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
		final AppInfo info = (AppInfo) node.getUserObject();

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
				openInGitHub(info);
			}
		});

		JPopupMenu submenu = new JPopupMenu();
		submenu.add(copyname);
		submenu.add(copypath);
		submenu.add(github);
		submenu.show(tree, x, y);
	}

	/**
	 * Opens github page of source code up in a browser window
	 */
	private void openInGitHub( AppInfo info ) {
		if (Desktop.isDesktopSupported()) {
			try {

				URI uri = new URI(UtilIO.getGithubURL(info.app.getPackage().getName(), info.app.getSimpleName()));
				if (!uri.getPath().isEmpty())
					Desktop.getDesktop().browse(uri);
				else
					System.err.println("Bad URL received");
			} catch (Exception e1) {
				JOptionPane.showMessageDialog(this,"Open GitHub Error",
						"Error connecting: "+e1.getMessage(),JOptionPane.ERROR_MESSAGE);
				System.err.println(e1.getMessage());
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == bKill) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					ActiveProcess selected = (ActiveProcess) processList.getSelectedValue();
					if (selected == null)
						return;

					selected.kill();

				}
			});
		} else if (e.getSource() == bKillAll) {
			synchronized (processes) {
				for (int i = 0; i < processes.size(); i++) {
					processes.get(i).kill();
				}
			}
		}
	}

	/**
	 * Called after a new process is added to the process list
	 *
	 * @param e
	 */
	@Override
	public void intervalAdded(ListDataEvent e) {
		//retrieve the most recently added process and display it
		DefaultListModel listModel = (DefaultListModel) e.getSource();
		ActiveProcess process = (ActiveProcess) listModel.get(listModel.getSize() - 1);
		addProcessTab(process, outputPanel);
	}

	@Override
	public void intervalRemoved(ListDataEvent e) {
	}

	@Override
	public void contentsChanged(ListDataEvent e) {
	}

	private void displaySource(final JTextArea sourceTextArea, ActiveProcess process) {
		if (sourceTextArea == null)
			return;

		String path = UtilIO.getSourcePath(process.info.app.getPackage().getName(), process.info.app.getSimpleName());
		File source = new File(path);
		if (source.exists() && source.canRead()) {
			String code = UtilIO.readAsString(path);
			sourceTextArea.setText(code);

			sourceTextArea.addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					super.mousePressed(e);
					if (SwingUtilities.isRightMouseButton(e) && !sourceTextArea.getText().isEmpty()) {
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
		} else {
			sourceTextArea.setText("Source not found!");
		}
	}

	private void addProcessTab(final ActiveProcess process, JTabbedPane pane) {
		String title = process.info.app.getSimpleName();

		final ProcessTabPanel component = new ProcessTabPanel(process.getId());
		component.setLayout(new BorderLayout());

		String[] optionsList = new String[]{"Source", "Output"};
		JComboBox<String> options = new JComboBox<>(optionsList);
		options.setBorder(new EmptyBorder(5,5,5,5));
		options.setMaximumSize(options.getPreferredSize());
		JButton bOpenInGitHub = new JButton("GitHub");
		bOpenInGitHub.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				openInGitHub(process.info);
			}
		});

		final JTextArea sourceTextArea = new JTextArea();
		sourceTextArea.setFont(new Font("monospaced", Font.PLAIN, 12));
		sourceTextArea.setEditable(false);
		sourceTextArea.setLineWrap(true);
		sourceTextArea.setWrapStyleWord(true);

		final JTextArea outputTextArea = new JTextArea();
		outputTextArea.setFont(new Font("monospaced", Font.PLAIN, 12));
		outputTextArea.setEditable(false);
		outputTextArea.setLineWrap(true);
		outputTextArea.setWrapStyleWord(true);

		final JScrollPane container = new JScrollPane();
		container.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

		options.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JComboBox source = (JComboBox) e.getSource();
				if (source.getSelectedIndex() == 0) {

					container.getViewport().removeAll();
					container.getViewport().add(sourceTextArea);
					displaySource(sourceTextArea, process);

					// after the GUI has figured out the shape of everthing move the view to a location of interest
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							String code = sourceTextArea.getText();
							int scrollTo = UtilIO.indexOfSourceStart(code);
							sourceTextArea.setCaretPosition(scrollTo);
						}});

				} else if (source.getSelectedIndex() == 1) {
					container.getViewport().removeAll();
					container.getViewport().add(outputTextArea);
				}
			}
		});

		// redirect console output to the GUI
		process.launcher.setPrintOut(new PrintStream(new TextOutputStream(outputTextArea)));
		process.launcher.setPrintErr(new PrintStream(new TextOutputStream(outputTextArea)));

		JPanel intermediate = new JPanel();
		intermediate.setLayout(new BoxLayout(intermediate,BoxLayout.X_AXIS));
		intermediate.add(options);
		intermediate.add(Box.createHorizontalGlue());
		if( Desktop.isDesktopSupported() ) {
			intermediate.add(bOpenInGitHub);
		}

		component.add(intermediate, BorderLayout.NORTH);
		component.add(container, BorderLayout.CENTER);

		pane.add(title, component);
		options.setSelectedIndex(1);
		pane.setSelectedIndex(pane.getComponentCount()-1);
	}

	private void removeProcessTab(final ActiveProcess process, JTabbedPane pane) {
		int index = -1;
		//lookup tab by process, assume one tab per process
		for (int i = 0; i < pane.getComponents().length; i++) {
			ProcessTabPanel component = (ProcessTabPanel) pane.getComponent(i);
			if (component.getProcessId() == process.getId())
				index = i;
		}

		if (index == -1)
			return;
		pane.remove(index);
	}

	class TextOutputStream extends OutputStream {
		private JTextArea textArea;
		public TextOutputStream(JTextArea textArea) {
			this.textArea = textArea;
		}
		@Override
		public void write(final int b) throws IOException {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					textArea.append(String.valueOf((char) b));
				}
			});
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
			System.out.println("------------------- Exit condition " + exit);
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
			if (launcher.isKillRequested() && active) {
				return "Killing " + info;
			} else {
				return info.toString();
			}
		}
	}

	class ProcessStatusThread extends Thread {
		@Override
		public void run() {
			while (true) {
				synchronized (processes) {
					for (int i = processes.size() - 1; i >= 0; i--) {
						final ActiveProcess p = processes.get(i);

						if (!p.isActive()) {
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									//intervalremoved listener is called after element is removed, so the reference
									//can't be retrieved. we call removeProcessTab() here for that reason.
									removeProcessTab(p, outputPanel);
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
