/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import boofcv.BoofVersion;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.util.*;
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
	JCheckBox checkRemoveOnDeath = new JCheckBox("Auto Remove");
	JButton bKillAll = new JButton("Kill All");

	JList processList;
	DefaultListModel<ActiveProcess> listModel = new DefaultListModel<>();
	JTabbedPane outputPanel = new JTabbedPane();

	int memoryMB = 1024;
	final List<ActiveProcess> processes = new ArrayList<>();

	boolean defaultRemoveTabOnDeath;

	public ApplicationLauncherApp( boolean defaultRemoveTabOnDeath ) {
		setLayout(new BorderLayout());

		checkRemoveOnDeath.setSelected(defaultRemoveTabOnDeath);

		DefaultMutableTreeNode root = new DefaultMutableTreeNode("All Categories");
		createTree(root);

		tree = new JTree(root);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

		final JTextField searchBox = new JTextField();
		searchBox.setToolTipText("Search");
		searchBox.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				String text = searchBox.getText();
				DefaultMutableTreeNode selection = (DefaultMutableTreeNode) tree.getModel().getRoot();

				TreePath path = searchTree(text, selection, true);
				if (path != null) {
					tree.setSelectionPath(path);
					tree.scrollPathToVisible(path);
				} else {
					tree.setSelectionPath(null);
				}
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				String text = searchBox.getText();
				DefaultMutableTreeNode selection =  (DefaultMutableTreeNode) tree.getModel().getRoot();
				TreePath path = searchTree(text, selection, true);
				if (path != null) {
					tree.setSelectionPath(path);
					tree.scrollPathToVisible(path);
				} else {
					tree.setSelectionPath(null);
				}
			}

			@Override
			public void changedUpdate(DocumentEvent e) {

			}
		});
		KeyStroke down = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, true);
		KeyStroke up = KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, true);
			Action nextSearch = new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					DefaultMutableTreeNode currentSelection = tree.getLastSelectedPathComponent() != null
							? (DefaultMutableTreeNode) tree.getLastSelectedPathComponent()
							: (DefaultMutableTreeNode) tree.getModel().getRoot();
					if (currentSelection != null && searchBox.getText() != null) {
						TreePath path = searchTree(searchBox.getText(), currentSelection, true);
						if (path != null) {
							tree.setSelectionPath(path);
							tree.scrollPathToVisible(path);
						} else {
							tree.setSelectionPath(null);
						}
					}
				}
		};
		Action prevSearch = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				DefaultMutableTreeNode currentSelection = tree.getLastSelectedPathComponent() != null
						? (DefaultMutableTreeNode) tree.getLastSelectedPathComponent()
						: (DefaultMutableTreeNode) tree.getModel().getRoot();
				if (currentSelection != null && searchBox.getText() != null) {
					TreePath path = searchTree(searchBox.getText(), currentSelection, false);
					if (path != null) {
						tree.setSelectionPath(path);
						tree.scrollPathToVisible(path);
					} else {
						tree.setSelectionPath(null);
					}
				}
			}
		};

		searchBox.getInputMap().put(down, "nextSearch");
		searchBox.getActionMap().put("nextSearch", nextSearch);
		searchBox.getInputMap().put(up, "prevSearch");
		searchBox.getActionMap().put("prevSearch", prevSearch);

		searchBox.addActionListener(e -> {
			//enter key goes to next match
			DefaultMutableTreeNode selection = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
			if(selection != null) {
				handleClick(selection);
			}
			System.out.println("action");
		});

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

		JTextArea infoPanel = new JTextArea();
		infoPanel.setLineWrap(true);
		infoPanel.setEditable(false);
		infoPanel.setPreferredSize(new Dimension(100,80));
		infoPanel.setText("BoofCV "+BoofVersion.VERSION+"\nGIT SHA\n"+BoofVersion.GIT_SHA);

		JPanel searchPanel = new JPanel();
		searchPanel.setLayout(new BoxLayout(searchPanel,BoxLayout.X_AXIS));
		searchPanel.add( new JLabel("Search"));
		searchPanel.add(Box.createRigidArea(new Dimension(5,5)));
		searchPanel.add(searchBox);

		JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
		leftPanel.add(searchPanel);
		JScrollPane treeView = new JScrollPane(tree);
		treeView.setPreferredSize(new Dimension(300, 600));
		leftPanel.add(treeView);
		leftPanel.add(infoPanel);

		JPanel actionPanel = new JPanel();
		actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.X_AXIS));
		actionPanel.add(bKill);
		actionPanel.add(checkRemoveOnDeath);
		actionPanel.add(Box.createHorizontalGlue());
		actionPanel.add(bKillAll);
		bKill.addActionListener(this);
		bKillAll.addActionListener(this);

		processList = new JList(listModel);
		processList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		processList.setLayoutOrientation(JList.VERTICAL);
		processList.setVisibleRowCount(-1);
		processList.getModel().addListDataListener(this);
		processList.setCellRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				ActiveProcess process = (ActiveProcess) value;
				if(!process.isAlive()) {
					c.setForeground(Color.RED);
				}
				return c;
			}
		});

		JPanel processPanel = new JPanel();
		processPanel.setLayout(new BorderLayout());
		processPanel.add(actionPanel, BorderLayout.NORTH);
		processPanel.add(processList, BorderLayout.CENTER);

		JSplitPane verticalSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,processPanel,outputPanel);
		verticalSplitPane.setDividerLocation(150);
		// divider location won't change when window is resized
		// most of the time you want to increase the view of the text
		verticalSplitPane.setResizeWeight(0.0);

		JSplitPane horizontalSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,leftPanel,verticalSplitPane);
		horizontalSplitPane.setDividerLocation(250);
		horizontalSplitPane.setResizeWeight(0.0);

		add(horizontalSplitPane, BorderLayout.CENTER);
		new ProcessStatusThread().start();

		// get the width of the monitor.  This should work in multi-monitor systems
		GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		// have it be a reasonable size of fill the display
		int width = Math.min(1200,gd.getDisplayMode().getWidth());

		setPreferredSize(new Dimension(width,600));
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
		String path = System.getProperty("java.class.path");
		String []arrayPath = path.split(":");

		List<String> classPath = Arrays.asList(arrayPath);

		final ActiveProcess process = new ActiveProcess();
		process.info = info;
		process.launcher = new JavaRuntimeLauncher(classPath);
		process.launcher.setFrozenTime(-1);
		process.launcher.setMemoryInMB(memoryMB);

		synchronized (processes) {
			processes.add(process);
		}

		SwingUtilities.invokeLater(() -> {
			listModel.addElement(process);
			processList.invalidate();
		});

		process.start();
	}

	private TreePath searchTree(String text, DefaultMutableTreeNode node, boolean forward) {
		Enumeration e = ((DefaultMutableTreeNode) this.tree.getModel().getRoot()).breadthFirstEnumeration();
		if(!forward) {
			final Stack tmp = new Stack();
			while(e.hasMoreElements())
				tmp.push(e.nextElement());

			e = new Enumeration() {
				@Override
				public boolean hasMoreElements() {
					return !tmp.isEmpty();
				}

				@Override
				public Object nextElement() {
					return tmp.pop();
				}
			};
		}

		while (e.hasMoreElements()) {
			DefaultMutableTreeNode n = (DefaultMutableTreeNode) e.nextElement();
			if(n.equals(node)) {
				while(e.hasMoreElements()) {
					DefaultMutableTreeNode candidate = (DefaultMutableTreeNode) e.nextElement();
					if (candidate.getUserObject() instanceof AppInfo) {
						AppInfo candidateInfo = (AppInfo) candidate.getUserObject();
						if (candidateInfo.app.getSimpleName().toLowerCase().contains(text)) {
							return new TreePath(candidate.getPath());
						}
					}
				}
			}
		}

		return null;
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
		copyname.addActionListener(e -> {
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(new StringSelection(info.app.getSimpleName()), null);
		});

		JMenuItem copypath = new JMenuItem("Copy Path");
		copypath.addActionListener(e -> {
			String path1 = UtilIO.getSourcePath(info.app.getPackage().getName(), info.app.getSimpleName());
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(new StringSelection(path1), null);
		});

		JMenuItem github = new JMenuItem("Go to Github");
		github.addActionListener(e -> openInGitHub(info));

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
			ActiveProcess selected = (ActiveProcess) processList.getSelectedValue();
			if (selected == null)
				return;

			if( selected.isAlive() )
				selected.requestKill();
			else {
				removeProcessTab(selected,false);
			}
		} else if (e.getSource() == bKillAll) {
			killAllProcesses(0);
		}
	}

	/**
	 * Requests that all active processes be killed.
	 * @param blockTimeMS If &gt; 0 then it will block until all processes are killed for the specified number
	 *                    of milliseconds
	 */
	public void killAllProcesses( long blockTimeMS ) {
		// remove already dead processes from the GUI
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				DefaultListModel model = (DefaultListModel)processList.getModel();
				for (int i = model.size()-1; i >= 0; i--) {
					ActiveProcess p = (ActiveProcess)model.get(i);
					removeProcessTab(p,false);
				}
			}
		});

		// kill processes that are already running
		synchronized (processes) {
			for (int i = 0; i < processes.size(); i++) {
				processes.get(i).requestKill();
			}
		}

		// block until everything is dead
		if( blockTimeMS > 0 ) {
			long abortTime = System.currentTimeMillis()+blockTimeMS;
			while( abortTime > System.currentTimeMillis() ) {
				int total = 0;
				synchronized (processes) {
					for (int i = 0; i < processes.size(); i++) {
						if (!processes.get(i).isActive()) {
							total++;
						}
					}
					if( processes.size() == total ) {
						break;
					}
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

		// first try loading it as a regular file
		String path = UtilIO.getSourcePath(process.info.app.getPackage().getName(), process.info.app.getSimpleName());
		String code = UtilIO.readAsString(path);
		if( code == null ) {
			code = "Failed to load "+path;
		}

		sourceTextArea.setText(code);

		sourceTextArea.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				super.mousePressed(e);
				if (SwingUtilities.isRightMouseButton(e) && !sourceTextArea.getText().isEmpty()) {
					JPopupMenu menu = new JPopupMenu();
					JMenuItem copy = new JMenuItem("Copy");
					copy.addActionListener(e1 -> {
						Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
						clipboard.setContents(new StringSelection(sourceTextArea.getText()), null);
					});
					menu.add(copy);
					menu.show(sourceTextArea, e.getX(), e.getY());
				}
			}
		});
	}

	private void addProcessTab(final ActiveProcess process, JTabbedPane pane) {
		String title = process.info.app.getSimpleName();

		final ProcessTabPanel component = new ProcessTabPanel(process.getId());

		final RSyntaxTextArea sourceTextArea = new RSyntaxTextArea();
		sourceTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
		sourceTextArea.setFont(new Font("monospaced", Font.PLAIN, 12));
		sourceTextArea.setCodeFoldingEnabled(true);
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

		component.bOpenInGitHub.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				openInGitHub(process.info);
			}
		});
		component.options.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JComboBox source = (JComboBox) e.getSource();
				if (source.getSelectedIndex() == 0) {

					container.getViewport().removeAll();
					container.getViewport().add(sourceTextArea);
					displaySource(sourceTextArea, process);

					// after the GUI has figured out the shape of everything move scroll to the start of the
					// source code and skip over the preamble stuff
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
		process.launcher.setPrintOut(new PrintStream(new TextOutputStream(outputTextArea,false)));
		process.launcher.setPrintErr(new PrintStream(new TextOutputStream(outputTextArea, false)));

		// this triggers an event and causes it to be displayed
		component.options.setSelectedIndex(1);

		component.add(container, BorderLayout.CENTER);

		pane.add(title, component);

		pane.setSelectedIndex(pane.getComponentCount()-1);
	}

	class TextOutputStream extends OutputStream {
		private JTextArea textArea;
		private boolean mirror;
		public TextOutputStream(JTextArea textArea, boolean mirror ) {
			this.textArea = textArea;
			this.mirror = mirror;
		}

		@Override
		public void write(final int b) throws IOException {
			if( mirror )
				System.out.write(b);
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

	public class ActiveProcess extends Thread {
		AppInfo info;
		JavaRuntimeLauncher launcher;

		volatile boolean active = false;
		JavaRuntimeLauncher.Exit exit;

		@Override
		public void run() {
			active = true;
			exit = launcher.launch(info.app);
			launcher.getPrintOut().println("\n\n~~~~~~~~~ Finished ~~~~~~~~~");
			System.out.println();
			System.out.println("------------------- Exit condition " + exit);
			active = false;
		}

		public void requestKill() {
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
									removeProcessTab(p,true);
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

	private void removeProcessTab(ActiveProcess process, boolean autoClose ) {
		//interval removed listener is called after element is removed, so the reference
		//can't be retrieved. we call removeProcessTab() here for that reason.
		int index = -1;
		//lookup tab by process, assume one tab per process
		for (int i = 0; i < outputPanel.getComponents().length; i++) {
			ProcessTabPanel component = (ProcessTabPanel) outputPanel.getComponent(i);
			if (component.getProcessId() == process.getId()) {
				if( autoClose && !checkRemoveOnDeath.isSelected() ) {
					processList.repaint();
					return;
				}
				index = i;
			}
		}

		if (index == -1)
			return;
		outputPanel.remove(index);

		listModel.removeElement(process);
		processList.invalidate();
	}

	public void showWindow( String title ) {
		JFrame frame = ShowImages.showWindow(this,title,true);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				ApplicationLauncherApp.this.killAllProcesses(2000);
			}
		});
	}
}
