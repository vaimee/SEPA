/* This GUI can be used for debugging SEPA applications
 * 
 * Author: Luca Roffia (luca.roffia@unibo.it)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.vaimee.sepa.tools.dashboard;

import com.vaimee.sepa.api.commons.exceptions.SEPABindingsException;
import com.vaimee.sepa.api.commons.exceptions.SEPAPropertiesException;
import com.vaimee.sepa.api.commons.exceptions.SEPAProtocolException;
import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.api.commons.properties.QueryProperties;
import com.vaimee.sepa.api.commons.properties.UpdateProperties;
import com.vaimee.sepa.api.commons.response.ErrorResponse;
import com.vaimee.sepa.api.commons.response.QueryResponse;
import com.vaimee.sepa.api.commons.response.Response;
import com.vaimee.sepa.api.commons.sparql.*;
import com.vaimee.sepa.api.pattern.JSAP;
import com.vaimee.sepa.tools.dashboard.bindings.BindingValue;
import com.vaimee.sepa.tools.dashboard.bindings.BindingsRender;
import com.vaimee.sepa.tools.dashboard.bindings.ForcedBindingsRenderer;
import com.vaimee.sepa.tools.dashboard.explorer.Explorer;
import com.vaimee.sepa.tools.dashboard.explorer.ExplorerTreeModel;
import com.vaimee.sepa.tools.dashboard.explorer.ExplorerTreeRenderer;
import com.vaimee.sepa.tools.dashboard.tableModels.*;
import com.vaimee.sepa.tools.dashboard.utils.*;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.*;

public class Dashboard implements LoginListener {
	private final Logging logger;

	static final String title = "SEPA Dashboard Ver 0.9.100";

	static Dashboard window;

	private DashboadApp sepaClient;
	private DashboardHandler handler;
	private JSAP appProfile = null;
	private Properties appProperties = new Properties();
	private DefaultTableModel namespacesDM;
	private final String[] namespacesHeader = new String[] { "Prefix", "URI" };
	private final BindingsTableModel bindingsDM = new BindingsTableModel();
	private final BindingsRender bindingsRender = new BindingsRender();
	private InstanceTableModel tableInstancePropertiesDataModel;
	private final GraphTableModel graphs = new GraphTableModel();
	private final ForcedBindingsTableModel updateForcedBindingsDM = new ForcedBindingsTableModel();
	private final ForcedBindingsTableModel subscribeForcedBindingsDM = new ForcedBindingsTableModel();
	private final SortedListModel updateListDM = new SortedListModel();
	private final SortedListModel queryListDM = new SortedListModel();
	private final SortedListModel jsapListDM = new SortedListModel();
	private final HashMap<String, BindingsTableModel> subscriptionResultsDM = new HashMap<>();
	private final HashMap<String, JLabel> subscriptionResultsLabels = new HashMap<>();
	private final HashMap<String, JTable> subscriptionResultsTables = new HashMap<>();
	private final String[] propertiesHeader = new String[] { "Property", "Domain", "Range", "Comment" };
	private JFrame frmSepaDashboard;
	private JTable bindingsResultsTable;
	private JTable updateForcedBindings;
	private JTable queryForcedBindings;
	private JLabel usingGraphURI;
	private TableRowSorter<BindingsTableModel> sorter;

	private String updateID;
	private String queryID;

	private JList<String> queryList;
	private JList<String> updateList;

	private final JTabbedPane subscriptionsPanel = new JTabbedPane(JTabbedPane.TOP);

	private Login login = null;

	private final ArrayList<String> jsapFiles = new ArrayList<>();

	private JTree explorerTree;
	private final ArrayList<RDFTerm> navStack = new ArrayList<>();

	private JTable graphsTable;
	private JCheckBox chckbxDatatype;
	private JCheckBox chckbxQname;

	private Explorer explorer;

	JButton updateButton = new JButton("UPDATE");
	JTextArea querySPARQL = new JTextArea();
	JTextArea updateSPARQL = new JTextArea();
	JLabel updateInfo = new JLabel("---");
	JTextField timeout = new JTextField();
	JTextField nRetry = new JTextField();
	JButton btnQuery = new JButton("QUERY");
	JLabel queryInfo = new JLabel("---");
	JButton subscribeButton = new JButton("SUBSCRIBE");

	JCheckBox chckbxMerge = new JCheckBox("merge");
	JTabbedPane mainTabs = new JTabbedPane(JTabbedPane.TOP);
	Panel sparqlTab = new Panel();
	JLabel updateURL = new JLabel("-");
	JLabel queryURL = new JLabel("-");
	JLabel usingNamedGraphURI = new JLabel("-");
	JLabel subscribeURL = new JLabel("-");
	JLabel defaultGraphURI = new JLabel("-");
	JLabel namedGraphURI = new JLabel("-");
	JScrollPane scrollPane_8;
	JScrollPane scrollPane = new JScrollPane();
	JScrollPane results = new JScrollPane();
	JScrollPane scrollPane_2 = new JScrollPane();
	JScrollPane scrollPane_1 = new JScrollPane();
	JScrollPane scrollPane_3 = new JScrollPane();
	JScrollPane update = new JScrollPane();
	JScrollPane query = new JScrollPane();

	JPanel updateGraphs = new JPanel();
	JPanel queryGraphs = new JPanel();
	JPanel panel_3 = new JPanel();
	JPanel panel_7 = new JPanel();
	JPanel panel_2 = new JPanel();
	JPanel panel_4 = new JPanel();
	JPanel panel_5 = new JPanel();
	JPanel panel_6 = new JPanel();

	JSplitPane updates = new JSplitPane();
	JSplitPane queries = new JSplitPane();

	JButton btnLoadXmlProfile = new JButton("Load JSAP");

	JLabel label_2 = new JLabel("using-graph-uri:");
	JLabel label_4 = new JLabel("using-named-graph-uri:");
	JLabel label_8 = new JLabel("default-graph-uri:");
	JLabel label_14 = new JLabel("QUERIES");
	JLabel label_10 = new JLabel("named-graph-uri:");
	JLabel label_12 = new JLabel("UPDATES");
	JLabel lblForcedBindings = new JLabel("FORCED BINDINGS");
	JLabel lblNewLabel_2 = new JLabel("Retry");
	JLabel lblToms = new JLabel("Timeout (ms)");
	JLabel lblForcedBindings_1 = new JLabel("FORCED BINDINGS");
	GridBagLayout gridBagLayout = new GridBagLayout();
	GridBagLayout gbl_sparqlTab = new GridBagLayout();
	GridBagLayout gbl_updateGraphs = new GridBagLayout();
	GridBagLayout gbl_queryGraphs = new GridBagLayout();
	GridBagLayout gbl_panel_3 = new GridBagLayout();
	GridBagLayout gbl_panel_4 = new GridBagLayout();
	GridBagLayout gbl_panel_7 = new GridBagLayout();
	GridBagLayout gbl_panel_5 = new GridBagLayout();
	GridBagLayout gbl_panel_6 = new GridBagLayout();
	GridBagConstraints gbc_mainTabs = new GridBagConstraints();
	GridBagConstraints gbc_updateGraphs = new GridBagConstraints();
	GridBagConstraints gbc_updateURL = new GridBagConstraints();
	GridBagConstraints gbc_updateUsingGraphURI = new GridBagConstraints();
	GridBagConstraints gbc_label_2 = new GridBagConstraints();
	GridBagConstraints gbc_label_4 = new GridBagConstraints();
	GridBagConstraints gbc_updateUsingNamedGraphURI = new GridBagConstraints();
	GridBagConstraints gbc_queryGraphs = new GridBagConstraints();
	GridBagConstraints gbc_queryURL = new GridBagConstraints();
	GridBagConstraints gbc_subscribeURL = new GridBagConstraints();
	GridBagConstraints gbc_label_8 = new GridBagConstraints();
	GridBagConstraints gbc_defaultGraphURI = new GridBagConstraints();
	GridBagConstraints gbc_scrollPane = new GridBagConstraints();
	GridBagConstraints gbc_label_12 = new GridBagConstraints();
	GridBagConstraints gbc_lblForcedBindings = new GridBagConstraints();
	GridBagConstraints gbc_scrollPane_2 = new GridBagConstraints();
	GridBagConstraints gbc_queries = new GridBagConstraints();
	GridBagConstraints gbc_label_14 = new GridBagConstraints();
	GridBagConstraints gbc_scrollPane_1 = new GridBagConstraints();
	GridBagConstraints gbc_lblForcedBindings_1 = new GridBagConstraints();
	GridBagConstraints gbc_scrollPane_3 = new GridBagConstraints();
	GridBagConstraints gbc_update = new GridBagConstraints();
	GridBagConstraints gbc_query = new GridBagConstraints();
	GridBagConstraints gbc_panel_6 = new GridBagConstraints();
	GridBagConstraints gbc_updateButton = new GridBagConstraints();
	GridBagConstraints gbc_updateInfo = new GridBagConstraints();
	GridBagConstraints gbc_lblToms = new GridBagConstraints();
	GridBagConstraints gbc_lblNewLabel_2 = new GridBagConstraints();
	GridBagConstraints gbc_timeout = new GridBagConstraints();
	GridBagConstraints gbc_nRetry = new GridBagConstraints();
	GridBagConstraints gbc_panel_7 = new GridBagConstraints();
	GridBagConstraints gbc_queryInfo = new GridBagConstraints();
	GridBagConstraints gbc_btnQuery = new GridBagConstraints();
	GridBagConstraints gbc_results = new GridBagConstraints();
	GridBagConstraints gbc_subscribeButton = new GridBagConstraints();

	JLabel lblGraphs = new JLabel("Graphs");
	JLabel lblNewLabel = new JLabel("Class tree");
	JLabel lblNewLabel_1 = new JLabel("Properties");

	JPanel explorerPanel = new JPanel();
	JPanel namespaces = new JPanel();
	JPanel panel = new JPanel();
	JPanel panel_9 = new JPanel();
	JPanel panel_1 = new JPanel();

	GridBagLayout gbl_namespaces = new GridBagLayout();
	GridBagLayout gbl_explorerPanel = new GridBagLayout();
	GridBagLayout gbl_panel = new GridBagLayout();
	GridBagLayout gbl_panel_1 = new GridBagLayout();
	GridBagLayout gbl_panel_9 = new GridBagLayout();

	JScrollPane scrollPane_4 = new JScrollPane();
	JScrollPane scrollPane_9 = new JScrollPane();
	JScrollPane scrollPane_7 = new JScrollPane();

	GridBagConstraints gbc_scrollPane_4 = new GridBagConstraints();
	GridBagConstraints gbc_lblGraphs = new GridBagConstraints();
	GridBagConstraints gbc_graphsEndpointLabel = new GridBagConstraints();
	GridBagConstraints gbc_btnNewButton_1 = new GridBagConstraints();
	GridBagConstraints gbc_scrollPane_9 = new GridBagConstraints();
	GridBagConstraints gbc_splitPane = new GridBagConstraints();
	GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
	GridBagConstraints gbc_scrollPane_7 = new GridBagConstraints();
	GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
	GridBagConstraints gbc_btnRefresh = new GridBagConstraints();
	GridBagConstraints gbc_currentSubject = new GridBagConstraints();
	GridBagConstraints gbc_buttonStackBackward = new GridBagConstraints();
	GridBagConstraints gbc_lblProperties = new GridBagConstraints();
	GridBagConstraints gbc_panel_9 = new GridBagConstraints();

	JButton btnNewButton_1 = new JButton("DROP");

	JSplitPane splitPane = new JSplitPane();

	JButton btnClean = new JButton("Clear log");
	GridBagConstraints gbc_btnClean = new GridBagConstraints();
	GridBagConstraints gbc_chkboxDatatype = new GridBagConstraints();
	GridBagConstraints gbc_checkboxQname = new GridBagConstraints();

	JButton btnNewButton_3 = new JButton("CSV");
	GridBagConstraints gbc_btnNewButton_3 = new GridBagConstraints();
	JButton btnRefresh = new JButton("Refresh");

	private final JTextField textField = new JTextField();
	private final JLabel lblNewLabel_3 = new JLabel("Filter");
	private final JPanel panel_8 = new JPanel();
	private final JTextArea textArea = new JTextArea();
	private final JScrollPane scrollPane_5 = new JScrollPane();

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(() -> {
			try {
				window = new Dashboard();
				window.frmSepaDashboard.setVisible(true);
			} catch (Exception e) {
				System.exit(-1);
			}
		});
	}

	/**
	 * Create the application.
	 */
	public Dashboard() throws SEPAPropertiesException, SEPASecurityException {
		logger = new Logging(textArea);
		
		initialize();

		loadJSAP(null, true);
	}

	private void loadDashboardProperties() throws IOException {
		FileInputStream in = new FileInputStream("dashboard.properties");
		appProperties.load(in);
	}

	protected boolean loadJSAP(String file, boolean load) throws SEPAPropertiesException, SEPASecurityException {
		namespacesDM.getDataVector().clear();
		updateListDM.clear();
		queryListDM.clear();

		updateForcedBindingsDM.clearBindings();
		subscribeForcedBindingsDM.clearBindings();

		queryURL.setText("-");
		updateURL.setText("-");

		defaultGraphURI.setText("-");
		namedGraphURI.setText("-");
		usingGraphURI.setText("-");
		usingNamedGraphURI.setText("-");
		subscribeURL.setText("-");

		queryList.clearSelection();
		updateList.clearSelection();

		querySPARQL.setText("");
		updateSPARQL.setText("");

		bindingsDM.clear();

		updateButton.setEnabled(false);
		subscribeButton.setEnabled(false);

		if (file == null) {
			try {
				loadDashboardProperties();
			} catch (IOException e) {
				logger.warn(e.getMessage());
				try {
					return onLoadJSAPButton();
				} catch (SEPASecurityException | URISyntaxException e1) {
					logger.error(e1.getMessage());
					return false;
				}
			}

			// LOAD properties
			String path = appProperties.getProperty("appProfile");

			if (path == null) {
				logger.error("Path in dashboard.properties is null");
				return false;
			}

			String[] jsaps = path.split(",");
			jsapListDM.clear();

			for (String jsap : jsaps) {
				jsapFiles.add(jsap);
				jsapListDM.add(jsap);
			}

			try {
				appProfile = new JSAP("file:///" + jsapFiles.get(0).replaceAll("\\\\", "/"));
			} catch (SEPAPropertiesException e) {
				logger.error(e.getMessage());
				return false;
			}

			if (jsapFiles.size() > 1) {
				for (int i = 1; i < jsaps.length; i++) {
					try {
						JSAP temp = new JSAP("file://" + jsapFiles.get(i).replaceAll("\\\\", "/"));
						appProfile.merge(temp);
					} catch (SEPAPropertiesException e) {
						logger.error(e.getMessage());
					}
				}
			}
		} else {
			try {
				loadDashboardProperties();
			} catch (IOException e) {
				logger.warn(e.getMessage());
			}
			try {

				if (load) {
					appProfile = new JSAP("file://" + file.replaceAll("\\\\", "/"));
					// appProfile.read(file, true);

					jsapFiles.clear();
					jsapFiles.add(file);

					jsapListDM.clear();
					jsapListDM.add(file);

				} else if (appProfile != null) {
					JSAP temp = new JSAP(file);
					appProfile.merge(temp);
					jsapFiles.add(file);
					jsapListDM.add(file);
				}
			} catch (SEPAPropertiesException e) {
				logger.error(e.getMessage());
				return false;
			}
		}

		assert appProfile != null;
		InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("explorer.jsap")));
		appProfile.read(reader,false);

		// Loading namespaces
		for (String prefix : appProfile.getNamespaces().keySet()) {
			Vector<String> row = new Vector<>();
			row.add(prefix);
			row.addElement(appProfile.getNamespaces().get(prefix));
			namespacesDM.addRow(row);
		}

		// Loading updates
		for (String update : appProfile.getUpdateIds()) {
			if (update.startsWith("___DASHBOARD_"))
				continue;
			updateListDM.add(update);
		}

		// Loading subscribes
		for (String subscribe : appProfile.getQueryIds()) {
			if (subscribe.startsWith("___DASHBOARD_"))
				continue;
			queryListDM.add(subscribe);
		}

//		explorer.setSepaClient(sepaClient);
//		handler.setSepaClient(sepaClient);

		// Security
		if (appProfile.isSecure()) {
			login = new Login(appProfile.getAuthenticationProperties(), this, frmSepaDashboard);// ,clientIDString,clientSecretString);
			login.setVisible(true);
		} else {
			onLogin("ვაიმეე");
		}

		return true;
	}

	/**
	 * Initialize the contents of the frame.
	 */
	protected void initialize() {
		namespacesDM = new DefaultTableModel(0, 0) {
			private static final long serialVersionUID = 6788045463932990156L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		namespacesDM.setColumnIdentifiers(namespacesHeader);

		DefaultTableModel propertiesDM = new DefaultTableModel(0, 0) {
			private static final long serialVersionUID = -5161490469556412655L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		propertiesDM.setColumnIdentifiers(propertiesHeader);

		updateList = new JList<>();
		queryList = new JList<>();

		frmSepaDashboard = new JFrame();
		frmSepaDashboard.setFont(new Font("Montserrat", Font.BOLD, 10));
		frmSepaDashboard.setTitle(title);
		frmSepaDashboard.setBounds(100, 100, 1361, 842);
		frmSepaDashboard.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmSepaDashboard.getContentPane().setLayout(gridBagLayout);

		updateForcedBindings = new JTable(updateForcedBindingsDM);

		gridBagLayout.columnWidths = new int[] { 465, 0 };
		gridBagLayout.rowHeights = new int[] { 44, 391, 100, 0 };
		gridBagLayout.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 1.0, 1.0, Double.MIN_VALUE };
		
		GridBagConstraints gbc_panel_8 = new GridBagConstraints();
		gbc_panel_8.anchor = GridBagConstraints.NORTH;
		gbc_panel_8.insets = new Insets(0, 0, 5, 0);
		gbc_panel_8.fill = GridBagConstraints.HORIZONTAL;
		gbc_panel_8.gridx = 0;
		gbc_panel_8.gridy = 0;
		frmSepaDashboard.getContentPane().add(panel_8, gbc_panel_8);
		panel_8.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
				panel_8.add(btnLoadXmlProfile);
		
				btnLoadXmlProfile.setFont(new Font("Montserrat", Font.BOLD, 13));
				btnLoadXmlProfile.setForeground(Color.BLACK);
				btnLoadXmlProfile.setBackground(Color.WHITE);
						panel_8.add(chckbxMerge);
				
						chckbxMerge.setFont(new Font("Montserrat", Font.PLAIN, 11));
				
						btnLoadXmlProfile.addActionListener(e -> {
							try {
								onLoadJSAPButton();
							} catch (SEPASecurityException | URISyntaxException | SEPAPropertiesException e1) {
								logger.error(e1.getMessage());
							}
						});

		mainTabs.setFont(new Font("Montserrat", Font.PLAIN, 13));

		gbc_mainTabs.insets = new Insets(0, 0, 5, 0);
		gbc_mainTabs.fill = GridBagConstraints.BOTH;
		gbc_mainTabs.gridx = 0;
		gbc_mainTabs.gridy = 1;
		frmSepaDashboard.getContentPane().add(mainTabs, gbc_mainTabs);

		mainTabs.addTab("SPARQL", null, sparqlTab, null);
		mainTabs.setEnabledAt(0, true);

		gbl_sparqlTab.columnWidths = new int[] { 420, 0, 0 };
		gbl_sparqlTab.rowHeights = new int[] { 0, 87, 91, 29, 188, 0 };
		gbl_sparqlTab.columnWeights = new double[] { 1.0, 1.0, Double.MIN_VALUE };
		gbl_sparqlTab.rowWeights = new double[] { 0.0, 0.0, 1.0, 0.0, 1.0, Double.MIN_VALUE };
		sparqlTab.setLayout(gbl_sparqlTab);

		updateGraphs.setBorder(new LineBorder(new Color(0, 0, 0), 1, true));

		gbc_updateGraphs.anchor = GridBagConstraints.NORTH;
		gbc_updateGraphs.insets = new Insets(0, 0, 5, 5);
		gbc_updateGraphs.fill = GridBagConstraints.HORIZONTAL;
		gbc_updateGraphs.gridx = 0;
		gbc_updateGraphs.gridy = 0;
		sparqlTab.add(updateGraphs, gbc_updateGraphs);

		gbl_updateGraphs.columnWidths = new int[] { 0, 0, 0 };
		gbl_updateGraphs.rowHeights = new int[] { 0, 0, 0, 0 };
		gbl_updateGraphs.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		gbl_updateGraphs.rowWeights = new double[] { 0.0, 0.0, 0.0, Double.MIN_VALUE };
		updateGraphs.setLayout(gbl_updateGraphs);

		updateURL.setForeground(UIManager.getColor("ComboBox.buttonDarkShadow"));
		updateURL.setFont(new Font("Montserrat", Font.BOLD, 12));

		gbc_updateURL.gridwidth = 2;
		gbc_updateURL.insets = new Insets(0, 0, 5, 0);
		gbc_updateURL.gridx = 0;
		gbc_updateURL.gridy = 0;
		updateGraphs.add(updateURL, gbc_updateURL);

		label_2.setFont(new Font("Montserrat", Font.BOLD, 12));
		label_2.setForeground(UIManager.getColor("ComboBox.buttonDarkShadow"));

		gbc_label_2.anchor = GridBagConstraints.EAST;
		gbc_label_2.insets = new Insets(0, 0, 5, 5);
		gbc_label_2.gridx = 0;
		gbc_label_2.gridy = 1;
		updateGraphs.add(label_2, gbc_label_2);

		usingGraphURI = new JLabel("-");
		usingGraphURI.setForeground(UIManager.getColor("ComboBox.buttonDarkShadow"));
		usingGraphURI.setFont(new Font("Montserrat", Font.BOLD, 12));

		gbc_updateUsingGraphURI.anchor = GridBagConstraints.WEST;
		gbc_updateUsingGraphURI.insets = new Insets(0, 0, 5, 0);
		gbc_updateUsingGraphURI.gridx = 1;
		gbc_updateUsingGraphURI.gridy = 1;
		updateGraphs.add(usingGraphURI, gbc_updateUsingGraphURI);

		label_4.setFont(new Font("Montserrat", Font.BOLD, 12));
		label_4.setForeground(UIManager.getColor("ComboBox.buttonDarkShadow"));

		gbc_label_4.anchor = GridBagConstraints.EAST;
		gbc_label_4.insets = new Insets(0, 0, 0, 5);
		gbc_label_4.gridx = 0;
		gbc_label_4.gridy = 2;
		updateGraphs.add(label_4, gbc_label_4);

		usingNamedGraphURI.setForeground(UIManager.getColor("ComboBox.buttonDarkShadow"));
		usingNamedGraphURI.setFont(new Font("Montserrat", Font.BOLD, 12));

		gbc_updateUsingNamedGraphURI.anchor = GridBagConstraints.WEST;
		gbc_updateUsingNamedGraphURI.gridx = 1;
		gbc_updateUsingNamedGraphURI.gridy = 2;
		updateGraphs.add(usingNamedGraphURI, gbc_updateUsingNamedGraphURI);

		queryGraphs.setBorder(new LineBorder(new Color(0, 0, 0), 1, true));

		gbc_queryGraphs.anchor = GridBagConstraints.NORTH;
		gbc_queryGraphs.insets = new Insets(0, 0, 5, 0);
		gbc_queryGraphs.fill = GridBagConstraints.HORIZONTAL;
		gbc_queryGraphs.gridx = 1;
		gbc_queryGraphs.gridy = 0;
		sparqlTab.add(queryGraphs, gbc_queryGraphs);

		gbl_queryGraphs.columnWidths = new int[] { 228, 207, 0 };
		gbl_queryGraphs.rowHeights = new int[] { 0, 0, 0, 0 };
		gbl_queryGraphs.columnWeights = new double[] { 1.0, 1.0, Double.MIN_VALUE };
		gbl_queryGraphs.rowWeights = new double[] { 0.0, 0.0, 0.0, Double.MIN_VALUE };
		queryGraphs.setLayout(gbl_queryGraphs);

		queryURL.setForeground(UIManager.getColor("ComboBox.buttonDarkShadow"));
		queryURL.setFont(new Font("Montserrat", Font.BOLD, 12));

		gbc_queryURL.insets = new Insets(0, 0, 5, 5);
		gbc_queryURL.gridx = 0;
		gbc_queryURL.gridy = 0;
		queryGraphs.add(queryURL, gbc_queryURL);

		subscribeURL.setForeground(UIManager.getColor("ComboBox.buttonDarkShadow"));
		subscribeURL.setFont(new Font("Montserrat", Font.BOLD, 12));

		gbc_subscribeURL.insets = new Insets(0, 0, 5, 0);
		gbc_subscribeURL.gridx = 1;
		gbc_subscribeURL.gridy = 0;
		queryGraphs.add(subscribeURL, gbc_subscribeURL);

		label_8.setFont(new Font("Montserrat", Font.BOLD, 12));
		label_8.setForeground(UIManager.getColor("ComboBox.buttonDarkShadow"));

		gbc_label_8.anchor = GridBagConstraints.EAST;
		gbc_label_8.insets = new Insets(0, 0, 5, 5);
		gbc_label_8.gridx = 0;
		gbc_label_8.gridy = 1;
		queryGraphs.add(label_8, gbc_label_8);

		defaultGraphURI.setForeground(UIManager.getColor("ComboBox.buttonDarkShadow"));
		defaultGraphURI.setFont(new Font("Montserrat", Font.BOLD, 12));

		gbc_defaultGraphURI.anchor = GridBagConstraints.WEST;
		gbc_defaultGraphURI.insets = new Insets(0, 0, 5, 0);
		gbc_defaultGraphURI.gridx = 1;
		gbc_defaultGraphURI.gridy = 1;
		queryGraphs.add(defaultGraphURI, gbc_defaultGraphURI);

		label_10.setFont(new Font("Montserrat", Font.BOLD, 12));
		label_10.setForeground(UIManager.getColor("ComboBox.buttonDarkShadow"));
		GridBagConstraints gbc_label_10 = new GridBagConstraints();
		gbc_label_10.anchor = GridBagConstraints.EAST;
		gbc_label_10.insets = new Insets(0, 0, 0, 5);
		gbc_label_10.gridx = 0;
		gbc_label_10.gridy = 2;
		queryGraphs.add(label_10, gbc_label_10);

		namedGraphURI.setForeground(UIManager.getColor("ComboBox.buttonDarkShadow"));
		namedGraphURI.setFont(new Font("Montserrat", Font.BOLD, 12));
		GridBagConstraints gbc_namedGraphURI = new GridBagConstraints();
		gbc_namedGraphURI.anchor = GridBagConstraints.WEST;
		gbc_namedGraphURI.gridx = 1;
		gbc_namedGraphURI.gridy = 2;
		queryGraphs.add(namedGraphURI, gbc_namedGraphURI);

		updates.setOrientation(JSplitPane.VERTICAL_SPLIT);
		updates.setResizeWeight(0.5);
		GridBagConstraints gbc_updates = new GridBagConstraints();
		gbc_updates.anchor = GridBagConstraints.NORTH;
		gbc_updates.insets = new Insets(0, 0, 5, 5);
		gbc_updates.fill = GridBagConstraints.HORIZONTAL;
		gbc_updates.gridx = 0;
		gbc_updates.gridy = 1;
		sparqlTab.add(updates, gbc_updates);

		updates.setLeftComponent(panel_2);
		GridBagLayout gbl_panel_2 = new GridBagLayout();
		gbl_panel_2.columnWidths = new int[] { 66, 0 };
		gbl_panel_2.rowHeights = new int[] { 17, 75, 0 };
		gbl_panel_2.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_panel_2.rowWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		panel_2.setLayout(gbl_panel_2);

		label_12.setForeground(Color.BLACK);
		label_12.setFont(new Font("Montserrat", Font.BOLD, 14));

		gbc_label_12.anchor = GridBagConstraints.NORTH;
		gbc_label_12.insets = new Insets(0, 0, 5, 0);
		gbc_label_12.gridx = 0;
		gbc_label_12.gridy = 0;
		panel_2.add(label_12, gbc_label_12);

		gbc_scrollPane.fill = GridBagConstraints.BOTH;
		gbc_scrollPane.gridx = 0;
		gbc_scrollPane.gridy = 1;
		panel_2.add(scrollPane, gbc_scrollPane);

		updateList.setFont(new Font("Montserrat", Font.PLAIN, 11));
		updateList.addListSelectionListener(e -> {
			try {
				selectUpdateID(updateList.getSelectedValue());
			} catch (SEPABindingsException e1) {
				logger.error(e1.getMessage());
			}
		});
		updateList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		updateList.setModel(updateListDM);
		scrollPane.setViewportView(updateList);

		updates.setRightComponent(panel_3);

		gbl_panel_3.columnWidths = new int[] { 101, 0 };
		gbl_panel_3.rowHeights = new int[] { 16, 80, 0 };
		gbl_panel_3.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_panel_3.rowWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE };
		panel_3.setLayout(gbl_panel_3);

		lblForcedBindings.setFont(new Font("Montserrat", Font.PLAIN, 12));
		lblForcedBindings.setForeground(Color.BLACK);

		gbc_lblForcedBindings.anchor = GridBagConstraints.NORTH;
		gbc_lblForcedBindings.insets = new Insets(0, 0, 5, 0);
		gbc_lblForcedBindings.gridx = 0;
		gbc_lblForcedBindings.gridy = 0;
		panel_3.add(lblForcedBindings, gbc_lblForcedBindings);

		gbc_scrollPane_2.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_2.gridx = 0;
		gbc_scrollPane_2.gridy = 1;
		panel_3.add(scrollPane_2, gbc_scrollPane_2);

		updateForcedBindings.setFont(new Font("Montserrat", Font.PLAIN, 11));
		updateForcedBindings.setCellSelectionEnabled(true);
		updateForcedBindings.setRowSelectionAllowed(false);
		updateForcedBindings.setFillsViewportHeight(true);
		scrollPane_2.setViewportView(updateForcedBindings);
		updateForcedBindings.getModel().addTableModelListener(e -> enableUpdateButton());
		updateForcedBindings.setDefaultRenderer(String.class, new ForcedBindingsRenderer());
		updateForcedBindings.registerKeyboardAction(new CopyAction(),
				KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),
				JComponent.WHEN_FOCUSED);
		updateForcedBindings.setCellSelectionEnabled(true);
		updateForcedBindings.getTableHeader().setBackground(Color.WHITE);

		queries.setBackground(Color.LIGHT_GRAY);
		queries.setOrientation(JSplitPane.VERTICAL_SPLIT);

		gbc_queries.anchor = GridBagConstraints.NORTH;
		gbc_queries.insets = new Insets(0, 0, 5, 0);
		gbc_queries.fill = GridBagConstraints.HORIZONTAL;
		gbc_queries.gridx = 1;
		gbc_queries.gridy = 1;
		sparqlTab.add(queries, gbc_queries);

		queries.setLeftComponent(panel_4);

		gbl_panel_4.columnWidths = new int[] { 193, 0 };
		gbl_panel_4.rowHeights = new int[] { 17, 72, 0 };
		gbl_panel_4.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_panel_4.rowWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE };
		panel_4.setLayout(gbl_panel_4);

		label_14.setForeground(Color.BLACK);
		label_14.setFont(new Font("Montserrat", Font.BOLD, 14));

		gbc_label_14.anchor = GridBagConstraints.NORTH;
		gbc_label_14.insets = new Insets(0, 0, 5, 0);
		gbc_label_14.gridx = 0;
		gbc_label_14.gridy = 0;
		panel_4.add(label_14, gbc_label_14);

		gbc_scrollPane_1.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_1.gridx = 0;
		gbc_scrollPane_1.gridy = 1;
		panel_4.add(scrollPane_1, gbc_scrollPane_1);

		queryList.setFont(new Font("Montserrat", Font.PLAIN, 11));
		queryList.addListSelectionListener(e -> {
			try {
				selectQueryID(queryList.getSelectedValue());
			} catch (SEPABindingsException e1) {
				logger.error(e1.getMessage());
			}
		});
		queryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		queryList.setModel(queryListDM);
		scrollPane_1.setViewportView(queryList);

		queries.setRightComponent(panel_5);

		gbl_panel_5.columnWidths = new int[] { 123, 0 };
		gbl_panel_5.rowHeights = new int[] { 16, 83, 0 };
		gbl_panel_5.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_panel_5.rowWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		panel_5.setLayout(gbl_panel_5);

		lblForcedBindings_1.setFont(new Font("Montserrat", Font.PLAIN, 12));
		lblForcedBindings_1.setForeground(Color.BLACK);

		gbc_lblForcedBindings_1.anchor = GridBagConstraints.NORTH;
		gbc_lblForcedBindings_1.insets = new Insets(0, 0, 5, 0);
		gbc_lblForcedBindings_1.gridx = 0;
		gbc_lblForcedBindings_1.gridy = 0;
		panel_5.add(lblForcedBindings_1, gbc_lblForcedBindings_1);

		gbc_scrollPane_3.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_3.gridx = 0;
		gbc_scrollPane_3.gridy = 1;
		panel_5.add(scrollPane_3, gbc_scrollPane_3);

		queryForcedBindings = new JTable(subscribeForcedBindingsDM);
		queryForcedBindings.setFont(new Font("Montserrat", Font.PLAIN, 11));
		queryForcedBindings.getTableHeader().setBackground(Color.WHITE);
		queryForcedBindings.setFillsViewportHeight(true);
		scrollPane_3.setViewportView(queryForcedBindings);
		queryForcedBindings.getModel().addTableModelListener(e -> enableQueryButton());
		queryForcedBindings.setDefaultRenderer(String.class, new ForcedBindingsRenderer());
		queryForcedBindings.registerKeyboardAction(new CopyAction(),
				KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),
				JComponent.WHEN_FOCUSED);
		queryForcedBindings.setCellSelectionEnabled(true);

		gbc_update.fill = GridBagConstraints.BOTH;
		gbc_update.insets = new Insets(0, 0, 5, 5);
		gbc_update.gridx = 0;
		gbc_update.gridy = 2;
		sparqlTab.add(update, gbc_update);

		updateSPARQL.setFont(new Font("Montserrat", Font.PLAIN, 13));
		update.setViewportView(updateSPARQL);
		updateSPARQL.setLineWrap(true);

		gbc_query.insets = new Insets(0, 0, 5, 0);
		gbc_query.fill = GridBagConstraints.BOTH;
		gbc_query.gridx = 1;
		gbc_query.gridy = 2;
		sparqlTab.add(query, gbc_query);

		querySPARQL.setFont(new Font("Montserrat", Font.PLAIN, 13));
		querySPARQL.setLineWrap(true);
		query.setViewportView(querySPARQL);

		gbc_panel_6.insets = new Insets(0, 0, 5, 5);
		gbc_panel_6.fill = GridBagConstraints.BOTH;
		gbc_panel_6.gridx = 0;
		gbc_panel_6.gridy = 3;
		sparqlTab.add(panel_6, gbc_panel_6);

		gbl_panel_6.columnWidths = new int[] { 0, 137, 0, 55, 45, 34, 0 };
		gbl_panel_6.rowHeights = new int[] { 28, 0 };
		gbl_panel_6.columnWeights = new double[] { 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, Double.MIN_VALUE };
		gbl_panel_6.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
		panel_6.setLayout(gbl_panel_6);

		updateButton.setFont(new Font("Montserrat", Font.BOLD, 13));

		gbc_updateButton.anchor = GridBagConstraints.WEST;
		gbc_updateButton.insets = new Insets(0, 0, 0, 5);
		gbc_updateButton.gridx = 0;
		gbc_updateButton.gridy = 0;
		panel_6.add(updateButton, gbc_updateButton);
		updateButton.addActionListener(e -> onUpdateButton());
		updateButton.setForeground(Color.BLACK);
		updateButton.setEnabled(false);

		updateInfo.setFont(new Font("Montserrat", Font.BOLD, 11));

		gbc_updateInfo.insets = new Insets(0, 0, 0, 5);
		gbc_updateInfo.fill = GridBagConstraints.VERTICAL;
		gbc_updateInfo.anchor = GridBagConstraints.WEST;
		gbc_updateInfo.gridx = 1;
		gbc_updateInfo.gridy = 0;
		panel_6.add(updateInfo, gbc_updateInfo);

		gbc_lblToms.insets = new Insets(0, 0, 0, 5);
		gbc_lblToms.gridx = 2;
		gbc_lblToms.gridy = 0;
		panel_6.add(lblToms, gbc_lblToms);
		lblToms.setFont(new Font("Montserrat", Font.PLAIN, 11));
		lblToms.setForeground(Color.BLACK);

		gbc_timeout.fill = GridBagConstraints.HORIZONTAL;
		gbc_timeout.insets = new Insets(0, 0, 0, 5);
		gbc_timeout.gridx = 3;
		gbc_timeout.gridy = 0;
		panel_6.add(timeout, gbc_timeout);
		timeout.setFont(new Font("Montserrat", Font.PLAIN, 11));
		timeout.setText("5000");
		timeout.setColumns(10);

		gbc_lblNewLabel_2.insets = new Insets(0, 0, 0, 5);
		gbc_lblNewLabel_2.gridx = 4;
		gbc_lblNewLabel_2.gridy = 0;
		panel_6.add(lblNewLabel_2, gbc_lblNewLabel_2);
		lblNewLabel_2.setFont(new Font("Montserrat", Font.PLAIN, 11));

		gbc_nRetry.fill = GridBagConstraints.HORIZONTAL;
		gbc_nRetry.gridx = 5;
		gbc_nRetry.gridy = 0;
		panel_6.add(nRetry, gbc_nRetry);
		nRetry.setFont(new Font("Montserrat", Font.PLAIN, 11));
		nRetry.setText("0");
		nRetry.setColumns(10);

		gbc_panel_7.insets = new Insets(0, 0, 5, 0);
		gbc_panel_7.fill = GridBagConstraints.BOTH;
		gbc_panel_7.gridx = 1;
		gbc_panel_7.gridy = 3;
		sparqlTab.add(panel_7, gbc_panel_7);

		gbl_panel_7.columnWidths = new int[] { 0, 0, 67, 0 };
		gbl_panel_7.rowHeights = new int[] { 0, 0 };
		gbl_panel_7.columnWeights = new double[] { 0.0, 1.0, 0.0, Double.MIN_VALUE };
		gbl_panel_7.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
		panel_7.setLayout(gbl_panel_7);

		btnQuery.setFont(new Font("Montserrat", Font.BOLD, 13));
		btnQuery.setEnabled(false);
		btnQuery.addActionListener(e -> onQueryButton());
		gbc_btnQuery.anchor = GridBagConstraints.WEST;
		gbc_btnQuery.insets = new Insets(0, 0, 0, 5);
		gbc_btnQuery.gridx = 0;
		gbc_btnQuery.gridy = 0;
		panel_7.add(btnQuery, gbc_btnQuery);

		queryInfo.setFont(new Font("Montserrat", Font.BOLD, 11));
		gbc_queryInfo.fill = GridBagConstraints.VERTICAL;
		gbc_queryInfo.insets = new Insets(0, 0, 0, 5);
		gbc_queryInfo.anchor = GridBagConstraints.WEST;
		gbc_queryInfo.gridx = 1;
		gbc_queryInfo.gridy = 0;
		panel_7.add(queryInfo, gbc_queryInfo);

		subscribeButton.setFont(new Font("Montserrat", Font.BOLD, 13));
		gbc_subscribeButton.anchor = GridBagConstraints.WEST;
		gbc_subscribeButton.gridx = 2;
		gbc_subscribeButton.gridy = 0;
		panel_7.add(subscribeButton, gbc_subscribeButton);
		subscribeButton.addActionListener(e -> onSubscribeButton());
		subscribeButton.setForeground(Color.BLACK);
		subscribeButton.setEnabled(false);

		gbc_results.fill = GridBagConstraints.BOTH;
		gbc_results.gridwidth = 2;
		gbc_results.gridx = 0;
		gbc_results.gridy = 4;
		sparqlTab.add(results, gbc_results);

		bindingsResultsTable = new JTable(bindingsDM);
		bindingsResultsTable.setFont(new Font("Montserrat", Font.PLAIN, 12));
		bindingsResultsTable.setBorder(UIManager.getBorder("Button.border"));
		bindingsResultsTable.setFillsViewportHeight(true);
		bindingsResultsTable.setDefaultRenderer(BindingValue.class, bindingsRender);

		bindingsResultsTable.setCellSelectionEnabled(true);
		bindingsResultsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		bindingsResultsTable.registerKeyboardAction(new CopyAction(),
				KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),
				JComponent.WHEN_FOCUSED);
		bindingsResultsTable.getTableHeader().setBackground(Color.WHITE);

		// SORTING AND FILTERING RESULTS
		sorter = new TableRowSorter<BindingsTableModel>(bindingsDM);
		bindingsResultsTable.setRowSorter(sorter);
		
		
		results.setViewportView(bindingsResultsTable);
		bindingsRender.setNamespaces(namespacesDM);

		mainTabs.addTab("Active subscriptions", null, subscriptionsPanel, null);

		mainTabs.addTab("Namespaces", null, namespaces, null);
		namespaces.setBorder(null);

		gbl_namespaces.columnWidths = new int[] { 0, 0, 0 };
		gbl_namespaces.rowHeights = new int[] { 43, 0 };
		gbl_namespaces.columnWeights = new double[] { 1.0, 1.0, Double.MIN_VALUE };
		gbl_namespaces.rowWeights = new double[] { 1.0, Double.MIN_VALUE };
		namespaces.setLayout(gbl_namespaces);

		gbc_scrollPane_4.gridwidth = 2;
		gbc_scrollPane_4.insets = new Insets(0, 0, 0, 5);
		gbc_scrollPane_4.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_4.gridx = 0;
		gbc_scrollPane_4.gridy = 0;
		namespaces.add(scrollPane_4, gbc_scrollPane_4);

		JTable namespacesTable = new JTable(namespacesDM);
		namespacesTable.setFont(new Font("Montserrat", Font.PLAIN, 13));
		namespacesTable.getTableHeader().setBackground(Color.WHITE);
		scrollPane_4.setViewportView(namespacesTable);

		mainTabs.addTab("Explorer", null, explorerPanel, null);

		gbl_explorerPanel.columnWidths = new int[] { 0, 0, 0 };
		gbl_explorerPanel.rowHeights = new int[] { 0, 0, 0, 0, 0 };
		gbl_explorerPanel.columnWeights = new double[] { 1.0, 0.0, Double.MIN_VALUE };
		gbl_explorerPanel.rowWeights = new double[] { 0.0, 0.0, 1.0, 1.0, Double.MIN_VALUE };
		explorerPanel.setLayout(gbl_explorerPanel);

		JLabel graphsEndpointLabel = new JLabel("Endpoint");
		graphsEndpointLabel.setFont(new Font("Montserrat", Font.PLAIN, 13));

		gbc_graphsEndpointLabel.insets = new Insets(5, 0, 5, 5);
		gbc_graphsEndpointLabel.gridx = 0;
		gbc_graphsEndpointLabel.gridy = 0;
		explorerPanel.add(graphsEndpointLabel, gbc_graphsEndpointLabel);

		lblGraphs.setFont(new Font("Montserrat", Font.BOLD, 13));

		gbc_lblGraphs.insets = new Insets(0, 0, 5, 5);
		gbc_lblGraphs.gridx = 0;
		gbc_lblGraphs.gridy = 1;
		explorerPanel.add(lblGraphs, gbc_lblGraphs);

		btnNewButton_1.setFont(new Font("Montserrat", Font.BOLD, 10));
		btnNewButton_1.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int[] rows = graphsTable.getSelectedRows();
				if (rows.length > 0) {
					int index = graphsTable.convertRowIndexToModel(rows[0]);

					int dialogResult = JOptionPane.showConfirmDialog(null,
							"Are you sure? " + graphs.getValueAt(index, 0).toString() + " will be deleted!", "Warning",
							JOptionPane.YES_NO_OPTION);
					if (dialogResult == JOptionPane.YES_OPTION) {
						Bindings forced = new Bindings();
						forced.addBinding("graph", new RDFTermURI(graphs.getValueAt(index, 0).toString()));

						Response ret = sepaClient.dropGraph(forced);
						if (ret.isUpdateResponse()) {
							graphs.removeRow(graphs.getValueAt(index, 0).toString());
						}
					}
				}
			}
		});
		btnNewButton_1.setForeground(Color.RED);
		btnNewButton_1.setBackground(Color.RED);

		gbc_btnNewButton_1.insets = new Insets(5, 0, 5, 0);
		gbc_btnNewButton_1.gridx = 1;
		gbc_btnNewButton_1.gridy = 1;
		explorerPanel.add(btnNewButton_1, gbc_btnNewButton_1);

		gbc_scrollPane_9.gridwidth = 2;
		gbc_scrollPane_9.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_9.insets = new Insets(0, 0, 5, 0);
		gbc_scrollPane_9.gridx = 0;
		gbc_scrollPane_9.gridy = 2;
		explorerPanel.add(scrollPane_9, gbc_scrollPane_9);

		graphsTable = new JTable(graphs);
		graphsTable.setRowSelectionAllowed(false);
		graphsTable.setFont(new Font("Montserrat", Font.PLAIN, 12));
		TableRowSorter<TableModel> sorter = new TableRowSorter<>(graphsTable.getModel());

		graphsTable.setRowSorter(sorter);
		graphsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		graphsTable.setCellSelectionEnabled(true);
		graphsTable.getTableHeader().setBackground(Color.WHITE);

		scrollPane_9.setViewportView(graphsTable);

		gbc_splitPane.gridwidth = 2;
		gbc_splitPane.fill = GridBagConstraints.BOTH;
		gbc_splitPane.gridx = 0;
		gbc_splitPane.gridy = 3;
		explorerPanel.add(splitPane, gbc_splitPane);

		splitPane.setLeftComponent(panel);

		gbl_panel.columnWidths = new int[] { 264, 0 };
		gbl_panel.rowHeights = new int[] { 0, 0, 0 };
		gbl_panel.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_panel.rowWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		panel.setLayout(gbl_panel);

		lblNewLabel.setFont(new Font("Montserrat", Font.BOLD, 13));

		gbc_lblNewLabel.insets = new Insets(0, 0, 5, 0);
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 0;
		panel.add(lblNewLabel, gbc_lblNewLabel);

		gbc_scrollPane_7.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_7.gridx = 0;
		gbc_scrollPane_7.gridy = 1;
		panel.add(scrollPane_7, gbc_scrollPane_7);

		splitPane.setRightComponent(panel_1);

		gbl_panel_1.columnWidths = new int[] { 0, 0, 0, 0 };
		gbl_panel_1.rowHeights = new int[] { 0, 0, 0, 0, 0, 0 };
		gbl_panel_1.columnWeights = new double[] { 0.0, 1.0, 0.0, Double.MIN_VALUE };
		gbl_panel_1.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE };
		panel_1.setLayout(gbl_panel_1);

		btnRefresh.setFont(new Font("Montserrat", Font.BOLD, 13));

		lblNewLabel_1.setFont(new Font("Montserrat", Font.BOLD, 13));

		gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_1.gridx = 1;
		gbc_lblNewLabel_1.gridy = 0;
		panel_1.add(lblNewLabel_1, gbc_lblNewLabel_1);

		gbc_btnRefresh.insets = new Insets(5, 0, 5, 0);
		gbc_btnRefresh.gridx = 2;
		gbc_btnRefresh.gridy = 0;
		panel_1.add(btnRefresh, gbc_btnRefresh);

		JLabel currentSubject = new JLabel("uri");

		gbc_currentSubject.gridwidth = 3;
		gbc_currentSubject.insets = new Insets(0, 0, 5, 5);
		gbc_currentSubject.gridx = 0;
		gbc_currentSubject.gridy = 1;
		panel_1.add(currentSubject, gbc_currentSubject);

		JButton buttonStackBackward = new JButton("Back");
		buttonStackBackward.setFont(new Font("Montserrat Alternates", Font.PLAIN, 11));

		gbc_buttonStackBackward.anchor = GridBagConstraints.WEST;
		gbc_buttonStackBackward.insets = new Insets(0, 0, 5, 5);
		gbc_buttonStackBackward.gridx = 0;
		gbc_buttonStackBackward.gridy = 2;
		panel_1.add(buttonStackBackward, gbc_buttonStackBackward);

		buttonStackBackward.setVisible(false);

		JLabel lblProperties = new JLabel(
				"Double click on the predicate to follow the URI link or double click on the object to edit it");

		gbc_lblProperties.gridwidth = 4;
		gbc_lblProperties.insets = new Insets(0, 0, 5, 0);
		gbc_lblProperties.gridx = 0;
		gbc_lblProperties.gridy = 3;
		panel_1.add(lblProperties, gbc_lblProperties);
		lblProperties.setFont(new Font("Montserrat", Font.ITALIC, 10));

		scrollPane_8 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_8 = new GridBagConstraints();
		gbc_scrollPane_8.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_8.gridwidth = 3;
		gbc_scrollPane_8.gridx = 0;
		gbc_scrollPane_8.gridy = 4;
		panel_1.add(scrollPane_8, gbc_scrollPane_8);
		gbc_panel_9.fill = GridBagConstraints.BOTH;
		gbc_panel_9.gridx = 0;
		gbc_panel_9.gridy = 2;
		frmSepaDashboard.getContentPane().add(panel_9, gbc_panel_9);

		gbl_panel_9.columnWidths = new int[] { 59, 0, 0, 0, 0, 0, 0 };
		gbl_panel_9.rowHeights = new int[] { 0, 118, 0, 0 };
		gbl_panel_9.columnWeights = new double[] { 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		gbl_panel_9.rowWeights = new double[] { 0.0, 1.0, 0.0, Double.MIN_VALUE };
		panel_9.setLayout(gbl_panel_9);
		GridBagConstraints gbc_lblNewLabel_3 = new GridBagConstraints();
		gbc_lblNewLabel_3.anchor = GridBagConstraints.EAST;
		gbc_lblNewLabel_3.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_3.gridx = 0;
		gbc_lblNewLabel_3.gridy = 0;
		panel_9.add(lblNewLabel_3, gbc_lblNewLabel_3);
		textField.setColumns(10);

		GridBagConstraints gbc_textField = new GridBagConstraints();
		gbc_textField.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField.insets = new Insets(0, 0, 5, 5);
		gbc_textField.gridx = 1;
		gbc_textField.gridy = 0;
		panel_9.add(textField, gbc_textField);

		gbc_btnNewButton_3.anchor = GridBagConstraints.SOUTH;
		gbc_btnNewButton_3.insets = new Insets(0, 0, 5, 5);
		gbc_btnNewButton_3.gridx = 2;
		gbc_btnNewButton_3.gridy = 0;
		panel_9.add(btnNewButton_3, gbc_btnNewButton_3);
		btnNewButton_3.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				onExportCSV();
			}
		});

		chckbxDatatype = new JCheckBox("Datatype");
		gbc_chkboxDatatype.insets = new Insets(0, 0, 5, 5);

		gbc_chkboxDatatype.anchor = GridBagConstraints.EAST;
		gbc_chkboxDatatype.gridx = 3;
		gbc_chkboxDatatype.gridy = 0;
		panel_9.add(chckbxDatatype, gbc_chkboxDatatype);
		chckbxDatatype.setFont(new Font("Montserrat", Font.PLAIN, 11));
		chckbxDatatype.addChangeListener(this::onDatatypeCheckbox);
		chckbxDatatype.setSelected(true);
		chckbxQname = new JCheckBox("Qname");
		gbc_checkboxQname.insets = new Insets(0, 0, 5, 5);

		gbc_checkboxQname.anchor = GridBagConstraints.WEST;
		gbc_checkboxQname.gridx = 4;
		gbc_checkboxQname.gridy = 0;
		panel_9.add(chckbxQname, gbc_checkboxQname);
		chckbxQname.setFont(new Font("Montserrat", Font.PLAIN, 11));
		chckbxQname.addChangeListener(this::onQnameCheckbox);
		chckbxQname.setSelected(true);

		ExplorerTreeRenderer explorerTreeRenderer = new ExplorerTreeRenderer(chckbxQname);

		JButton btnNewButton = new JButton("Clear results");
		GridBagConstraints gbc_btnNewButton = new GridBagConstraints();
		gbc_btnNewButton.insets = new Insets(0, 0, 5, 0);
		gbc_btnNewButton.anchor = GridBagConstraints.SOUTHEAST;
		gbc_btnNewButton.gridx = 5;
		gbc_btnNewButton.gridy = 0;
		panel_9.add(btnNewButton, gbc_btnNewButton);
		btnNewButton.setFont(new Font("Montserrat", Font.BOLD, 13));
		btnNewButton.addActionListener(e -> clear());
		ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
		
		GridBagConstraints gbc_scrollPane_5 = new GridBagConstraints();
		gbc_scrollPane_5.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_5.gridwidth = 6;
		gbc_scrollPane_5.insets = new Insets(0, 0, 5, 5);
		gbc_scrollPane_5.gridx = 0;
		gbc_scrollPane_5.gridy = 1;
		panel_9.add(scrollPane_5, gbc_scrollPane_5);
		scrollPane_5.setViewportView(textArea);
		//TextAreaAppender.addAppender(appender, "TextArea");

		gbc_btnClean.anchor = GridBagConstraints.SOUTHEAST;
		gbc_btnClean.gridx = 5;
		gbc_btnClean.gridy = 2;
		panel_9.add(btnClean, gbc_btnClean);
		btnClean.setFont(new Font("Montserrat", Font.BOLD, 13));
		btnClean.setForeground(Color.BLACK);
		btnClean.setBackground(UIManager.getColor("Separator.shadow"));
		btnClean.addActionListener(e -> {
			textArea.setText("");
		});
		explorerTreeRenderer.setNamespaces(namespacesDM);

		explorerTree = new JTree();
		explorerTree.setFont(new Font("Montserrat", Font.PLAIN, 12));
		scrollPane_7.setViewportView(explorerTree);

		explorerTree.setModel(new ExplorerTreeModel());
		explorerTree.setCellRenderer(explorerTreeRenderer);
		ToolTipManager.sharedInstance().registerComponent(explorerTree);

		explorerPanel.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				explorer.onExplorerOpenTab(false);
			}
		});

		graphsTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				explorer.onExloperSelectGraph();
			}
		});

		buttonStackBackward.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				explorer.onExplorerBackButton(e);
			}
		});

		btnRefresh.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				explorer.onExplorerOpenTab(true);
			}
		});

		explorerTree.addTreeSelectionListener(e -> explorer.onExplorerSelectTreeElement(e));

		tableInstancePropertiesDataModel = new InstanceTableModel(graphs, lblProperties, graphsTable);
		JTable tableInstanceProperties = new JTable(tableInstancePropertiesDataModel);
		tableInstanceProperties.getTableHeader().setBackground(Color.WHITE);
		tableInstanceProperties.setFont(new Font("Montserrat", Font.PLAIN, 12));
		tableInstanceProperties.getModel().addTableModelListener(e -> {
			if (e.getFirstRow() == 0)
				return;
			logger.debug("Value: " + tableInstancePropertiesDataModel.getValueAt(e.getFirstRow(), e.getColumn()));

			DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) explorerTree.getLastSelectedPathComponent();
			Bindings nodeInfo = (Bindings) selectedNode.getUserObject();

			String type = (String) tableInstancePropertiesDataModel.getValueAt(e.getFirstRow(), 2);

			RDFTerm graph = new RDFTermURI(
					(String) graphs.getValueAt(graphsTable.convertRowIndexToModel(graphsTable.getSelectedRow()), 0));

			RDFTerm subject;
			try {
				if (nodeInfo.isURI("instance")) {
					subject = new RDFTermURI(nodeInfo.getValue("instance"));
				} else {
					subject = new RDFTermBNode(nodeInfo.getValue("instance"));
				}
			} catch (SEPABindingsException e2) {
				logger.error(e2.getMessage());
				return;
			}

			RDFTerm predicate = new RDFTermURI(
					(String) tableInstancePropertiesDataModel.getValueAt(e.getFirstRow(), 0));
			RDFTerm object;

			Bindings forcedBindings = new Bindings();
			forcedBindings.addBinding("graph", graph);
			forcedBindings.addBinding("subject", subject);
			forcedBindings.addBinding("predicate", predicate);

			Response ret;

			if (type == null) {
				object = new RDFTermLiteral((String) tableInstancePropertiesDataModel.getValueAt(e.getFirstRow(), 1));
				forcedBindings.addBinding("object", object);
				if (subject.isURI())
					ret = sepaClient.updateLiteral(forcedBindings);
				else {
					Bindings parentInfo = (Bindings) ((DefaultMutableTreeNode) selectedNode.getParent())
							.getUserObject();
					String parentClass = parentInfo.getValue("class");
					forcedBindings.addBinding("class", new RDFTermURI(parentClass));
					ret = sepaClient.updateLiteralBnode(forcedBindings);
				}
			} else if (type.equals("URI") || type.equals("BNODE")) {
				object = new RDFTermURI((String) tableInstancePropertiesDataModel.getValueAt(e.getFirstRow(), 1));
				forcedBindings.addBinding("object", object);
				ret = sepaClient.updateUri(forcedBindings);
			} else {
				object = new RDFTermLiteral((String) tableInstancePropertiesDataModel.getValueAt(e.getFirstRow(), 1),
						type);
				forcedBindings.addBinding("object", object);
				ret = sepaClient.updateLiteral(forcedBindings);
			}

			if (ret.isError())
				logger.error(ret.toString());

		});
		tableInstanceProperties.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				explorer.onExplorerPropertiesNavigation(e);
			}
		});

		scrollPane_8.setViewportView(tableInstanceProperties);

		textField.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
				filterResults(textField.getText());
			}

			public void insertUpdate(DocumentEvent e) {
				filterResults(textField.getText());
			}

			public void removeUpdate(DocumentEvent e) {
				filterResults(textField.getText());
			}
		});

		explorer = new Explorer(explorerTree, graphsTable, graphs, nRetry, timeout, navStack, currentSubject,
				tableInstancePropertiesDataModel, tableInstanceProperties, buttonStackBackward, graphsEndpointLabel);

		handler = new DashboardHandler(subscriptionResultsDM, subscriptionResultsLabels, subscriptionResultsTables,
				queryList, querySPARQL, timeout, nRetry, bindingsRender, subscriptionsPanel, mainTabs);
	}

	private void filterResults(String text) {
		RowRegexFilter rf = new RowRegexFilter(text);
		sorter.setRowFilter(rf);

	}

	protected void onQnameCheckbox(ChangeEvent ignoredE) {
		bindingsRender.showAsQName(chckbxQname.isSelected());
		bindingsDM.fireTableDataChanged();
		for (BindingsTableModel table : subscriptionResultsDM.values()) {
			table.fireTableDataChanged();
		}
	}

	protected void onDatatypeCheckbox(ChangeEvent e) {
		bindingsRender.showDataType(chckbxDatatype.isSelected());
		bindingsDM.fireTableDataChanged();
		for (BindingsTableModel table : subscriptionResultsDM.values()) {
			table.fireTableDataChanged();
		}
	}

	protected void onExportCSV() {
		String openIn = null;
		final JFileChooser fc = new JFileChooser(openIn);
		fc.setDialogTitle("Save CSV");
		fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

		int returnVal = fc.showSaveDialog(frmSepaDashboard);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			String fileName = fc.getSelectedFile().getPath();

			try {
				File fout = new File(fileName);
				FileOutputStream fos = new FileOutputStream(fout);
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

				// Header
				StringBuilder lineString = null;
				for (int col = 0; col < bindingsResultsTable.getColumnCount(); col++) {
					String value = bindingsResultsTable.getColumnName(col);
					if (lineString == null)
						lineString = new StringBuilder(value);
					else
						lineString.append("|").append(value);
				}
				assert lineString != null;
				bw.write(lineString.toString());
				bw.newLine();

				for (int row = 0; row < bindingsResultsTable.getRowCount(); row++) {
					lineString = null;
					for (int col = 0; col < bindingsResultsTable.getColumnCount(); col++) {
						String value = ((bindingsResultsTable.getValueAt(row, col) == null ? ""
								: ((BindingValue) bindingsResultsTable.getValueAt(row, col)).get()));
						if (lineString == null)
							lineString = new StringBuilder(value);
						else
							lineString.append("|").append(value);
					}
					assert lineString != null;
					bw.write(lineString.toString());
					bw.newLine();

				}
				bw.close();
			} catch (Exception e3) {
				logger.error(e3.getMessage());
			}
		}
	}

	protected boolean onLoadJSAPButton() throws SEPASecurityException, URISyntaxException, SEPAPropertiesException {
		String openIn = null;
		if (appProperties.getProperty("appProfile") != null) {
			String[] profilePath = appProperties.getProperty("appProfile").split(",");
			openIn = profilePath[profilePath.length - 1];
		}

		final JFileChooser fc = new JFileChooser(openIn);
		fc.setDialogTitle("Open JSAP file");
		DashboardFileFilter filter = new DashboardFileFilter("JSON SAP Profile (.jsap)", ".jsap");
		fc.setFileFilter(filter);
		int returnVal = fc.showOpenDialog(frmSepaDashboard);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			String fileName = fc.getSelectedFile().getPath();

			jsapFiles.remove(fileName);

			if (loadJSAP(fileName, !chckbxMerge.isSelected())) {
				StringBuilder path = new StringBuilder();
				for (int i = 0; i < jsapFiles.size(); i++) {
					if (i == 0)
						path = new StringBuilder(jsapFiles.get(i));
					else
						path.append(",").append(jsapFiles.get(i));
				}

				appProperties = new Properties();
				appProperties.put("appProfile", path.toString());

				logger.info("JSAP files: " + path);

				return storeDashboardProperties();
			}
		}

		return false;
	}

	private boolean storeDashboardProperties() {
		try {
			FileOutputStream out = new FileOutputStream("dashboard.properties");
			appProperties.store(out, "Dashboard properties");
			out.close();
		} catch (IOException e3) {
			logger.error(e3.getMessage());
			return false;
		}

		return true;
	}

	protected void onSubscribeButton() {
		try {
			subscribe();
		} catch (IOException | SEPAPropertiesException | NumberFormatException | SEPAProtocolException
				 | SEPASecurityException | SEPABindingsException | InterruptedException e1) {
			logger.error(e1.getMessage());
		}
	}

	protected void onQueryButton() {
		try {
			query();
		} catch (SEPAPropertiesException | SEPABindingsException e1) {
			logger.error(e1.getMessage());
		}
	}

	protected void onUpdateButton() {
		try {
			update();
		} catch (SEPAPropertiesException | SEPABindingsException e1) {
			logger.error(e1.getMessage());
		}
	}

	protected void clear() {
		if (sparqlTab.isShowing()) {
			bindingsDM.clear();
		} else {
			for (String spuid : subscriptionResultsTables.keySet()) {
				if (subscriptionResultsTables.get(spuid).isShowing()) {
					subscriptionResultsDM.get(spuid).clear();
					subscriptionResultsLabels.get(spuid).setText("Results cleaned");
				}
			}
		}
	}

	protected void subscribe() throws IOException, SEPAPropertiesException, NumberFormatException,
			SEPAProtocolException, SEPASecurityException, SEPABindingsException, InterruptedException {

		Bindings bindings = new Bindings();
		for (int row = 0; row < queryForcedBindings.getRowCount(); row++) {
			String type;
			if (queryForcedBindings.getValueAt(row, 2) == null)
				type = "xsd:string";
			else
				type = queryForcedBindings.getValueAt(row, 2).toString();
			String value = queryForcedBindings.getValueAt(row, 1).toString();
			String variable = queryForcedBindings.getValueAt(row, 0).toString();
			if (type.equalsIgnoreCase("URI"))
				bindings.addBinding(variable, new RDFTermURI(value));
			else if (type.equalsIgnoreCase("BNODE"))
				bindings.addBinding(variable, new RDFTermBNode(value));
			else
				bindings.addBinding(variable, new RDFTermLiteral(value, type));
		}

		if (sepaClient == null) {
			JOptionPane.showMessageDialog(null, "You need to sign in first", "Warning: not authorized",
					JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		sepaClient.subscribe(queryID, querySPARQL.getText(), bindings, Integer.parseInt(timeout.getText()),
				Integer.parseInt(nRetry.getText()));
	}

	protected void query() throws SEPAPropertiesException, SEPABindingsException {
		Bindings bindings = new Bindings();
		for (int row = 0; row < queryForcedBindings.getRowCount(); row++) {
			String type = null;
			if (queryForcedBindings.getValueAt(row, 2) != null)
				type = queryForcedBindings.getValueAt(row, 2).toString();
			String value = queryForcedBindings.getValueAt(row, 1).toString();
			String variable = queryForcedBindings.getValueAt(row, 0).toString();

			if (type == null)
				bindings.addBinding(variable, new RDFTermLiteral(value));
			else if (type.equalsIgnoreCase("URI"))
				bindings.addBinding(variable, new RDFTermURI(value));
			else if (type.equalsIgnoreCase("BNODE"))
				bindings.addBinding(variable, new RDFTermBNode(value));
			else
				bindings.addBinding(variable, new RDFTermLiteral(value, type));
		}

//		try {
		Instant start = Instant.now();

		Response ret = sepaClient.query(queryID, querySPARQL.getText(), bindings, Integer.parseInt(timeout.getText()),
				Integer.parseInt(nRetry.getText()));
		Instant stop = Instant.now();
		if (ret.isError()) {

			logger.error(ret + String.format(" (%d ms)", (stop.toEpochMilli() - start.toEpochMilli())));
			queryInfo.setText("Error: " + ((ErrorResponse) ret).getStatusCode());

		} else {
			QueryResponse results = (QueryResponse) ret;
			logger.info(String.format("Results: %d (%d ms)", results.getBindingsResults().size(),
					(stop.toEpochMilli() - start.toEpochMilli())));
			queryInfo.setText(String.format("Results: %d (%d ms)", results.getBindingsResults().size(),
					(stop.toEpochMilli() - start.toEpochMilli())));
			bindingsDM.clear();
			bindingsDM.setAddedResults(subscriptionResultsTables, results.getBindingsResults(), null);
		}
	}

	protected void update() throws SEPAPropertiesException, SEPABindingsException {
		Bindings bindings = new Bindings();
		for (int row = 0; row < updateForcedBindings.getRowCount(); row++) {
			String type;
			if (updateForcedBindings.getValueAt(row, 2) == null)
				type = null;
			else
				type = updateForcedBindings.getValueAt(row, 2).toString();
			String value = updateForcedBindings.getValueAt(row, 1).toString();
			String variable = updateForcedBindings.getValueAt(row, 0).toString();

			if (type == null)
				bindings.addBinding(variable, new RDFTermLiteral(value));
			else if (type.equalsIgnoreCase("URI"))
				bindings.addBinding(variable, new RDFTermURI(value));
			else if (type.equalsIgnoreCase("BNODE"))
				bindings.addBinding(variable, new RDFTermBNode(value));
			else
				bindings.addBinding(variable, new RDFTermLiteral(value, type));
		}

		Instant start = Instant.now();

		Response ret = sepaClient.update(updateID, updateSPARQL.getText(), bindings,
				Integer.parseInt(timeout.getText()), Integer.parseInt(nRetry.getText()));
		Instant stop = Instant.now();
		if (ret.isError()) {
			logger.error(ret + String.format(" (%d ms)", (stop.toEpochMilli() - start.toEpochMilli())));
			updateInfo.setText("Error: " + ((ErrorResponse) ret).getStatusCode());

		} else {
			logger.info(String.format("Update OK (%d ms)", (stop.toEpochMilli() - start.toEpochMilli())));
			updateInfo.setText(String.format("Update OK (%d ms)", (stop.toEpochMilli() - start.toEpochMilli())));
		}
	}

	protected void selectUpdateID(String id) throws SEPABindingsException {
		if (id == null)
			return;
		updateID = id;
		updateSPARQL.setText(appProfile.getSPARQLUpdate(id));

		Bindings bindings = appProfile.getUpdateBindings(id);
		updateForcedBindingsDM.clearBindings();
		for (String variable : bindings.getVariables()) {
			if (bindings.isURI(variable))
				updateForcedBindingsDM.addBindings(variable, "URI", bindings.getValue(variable));
			else if (bindings.isLiteral(variable))
				updateForcedBindingsDM.addBindings(variable, bindings.getDatatype(variable),
						bindings.getValue(variable));
			else
				updateForcedBindingsDM.addBindings(variable, "BNODE", bindings.getValue(variable));
		}

		String port = "";
		if (appProfile.getUpdatePort(id) != -1)
			port = ":" + appProfile.getUpdatePort(id);
		String url = appProfile.getUpdateProtocolScheme(id) + "://" + appProfile.getUpdateHost(id) + port
				+ appProfile.getUpdatePath(id);
		if (appProfile.getUpdateMethod(id).equals(UpdateProperties.UpdateHTTPMethod.POST))
			updateURL.setText("POST " + url);
		else if (appProfile.getUpdateMethod(id).equals(UpdateProperties.UpdateHTTPMethod.URL_ENCODED_POST))
			updateURL.setText("URL ENCODED POST " + url);

		if (appProfile.getUsingGraphURI(id) != null)
			usingGraphURI.setText(appProfile.getUsingGraphURI(id).toString());
		if (appProfile.getUsingNamedGraphURI(id) != null)
			usingNamedGraphURI.setText(appProfile.getUsingNamedGraphURI(id).toString());

		enableUpdateButton();
	}

	protected void selectQueryID(String id) throws SEPABindingsException {
		if (id == null)
			return;

		queryID = id;
		// JSAP app = sepaClient.getApplicationProfile();
		querySPARQL.setText(appProfile.getSPARQLQuery(id));

		Bindings bindings = appProfile.getQueryBindings(id);
		subscribeForcedBindingsDM.clearBindings();
		for (String variable : bindings.getVariables()) {
			if (bindings.isURI(variable))
				subscribeForcedBindingsDM.addBindings(variable, "URI", bindings.getValue(variable));
			else if (bindings.isLiteral(variable))
				subscribeForcedBindingsDM.addBindings(variable, bindings.getDatatype(variable),
						bindings.getValue(variable));
			else
				subscribeForcedBindingsDM.addBindings(variable, "BNODE", bindings.getValue(variable));
		}

		String port = "";
		if (appProfile.getQueryPort(id) != -1)
			port = ":" + appProfile.getQueryPort(id);
		String url = appProfile.getQueryProtocolScheme(id) + "://" + appProfile.getQueryHost(id) + port
				+ appProfile.getQueryPath(id);

		if (appProfile.getQueryMethod(id).equals(QueryProperties.QueryHTTPMethod.GET))
			queryURL.setText("GET " + url);
		else if (appProfile.getQueryMethod(id).equals(QueryProperties.QueryHTTPMethod.POST))
			queryURL.setText("POST " + url);
		else if (appProfile.getQueryMethod(id).equals(QueryProperties.QueryHTTPMethod.URL_ENCODED_POST))
			queryURL.setText("URL ENCODED POST " + url);

		url = appProfile.getSubscribeProtocol(id).getScheme() + "://";

		url += appProfile.getSubscribeHost(id);
		if (appProfile.getSubscribePort(id) != -1)
			url += ":" + appProfile.getSubscribePort(id);
		url += appProfile.getSubscribePath(id);
		subscribeURL.setText(url);

		if (appProfile.getDefaultGraphURI(id) != null)
			defaultGraphURI.setText(appProfile.getDefaultGraphURI(id).toString());
		if (appProfile.getNamedGraphURI(id) != null)
			namedGraphURI.setText(appProfile.getNamedGraphURI(id).toString());

		enableQueryButton();
	}

	protected void enableUpdateButton() {
		updateButton.setEnabled(false);
		if (updateSPARQL.getText().isEmpty())
			return;
		else {
			for (int row = 0; row < updateForcedBindings.getRowCount(); row++) {
				String type = null;
				if (updateForcedBindings.getValueAt(row, 2) != null)
					type = updateForcedBindings.getValueAt(row, 2).toString();
				String value = updateForcedBindings.getValueAt(row, 1).toString();
				if (!Utilities.checkType(value, type))
					return;
			}
		}
		updateButton.setEnabled(true);
	}

	protected void enableQueryButton() {
		btnQuery.setEnabled(false);
		subscribeButton.setEnabled(false);
		if (querySPARQL.getText().isEmpty())
			return;
		else {
			for (int row = 0; row < queryForcedBindings.getRowCount(); row++) {
				String type = null;

				if (queryForcedBindings.getValueAt(row, 2) != null)
					type = queryForcedBindings.getValueAt(row, 2).toString();
				String value = queryForcedBindings.getValueAt(row, 1).toString();
				if (!Utilities.checkType(value, type))
					return;
			}
		}
		btnQuery.setEnabled(true);
		subscribeButton.setEnabled(true);
	}

	// LOGIN
	@Override
	public void onLogin(String id) {

		try {
			sepaClient = new DashboadApp(appProfile, handler);
			explorer.setSepaClient(sepaClient);
			handler.setSepaClient(sepaClient);
			tableInstancePropertiesDataModel.setSepaClient(sepaClient);

//			if (login != null) {
//				login.setVisible(false);
//				btnLogin.setEnabled(true);
//				btnLogin.setText("Sign out");
//				signedIn = true;
//			} else {
//				btnLogin.setVisible(false);
//				btnRegister.setVisible(false);
//			}

			frmSepaDashboard.setTitle(title + " - Client ID: " + id);

		} catch (SEPAProtocolException | SEPASecurityException | SEPAPropertiesException e) {
			logger.error(e.getMessage());
		}
	}

	@Override
	public void onLoginClose() {

	}

	@Override
	public void onLoginError(ErrorResponse err) {
		logger.error(err.toString());
	}
}
