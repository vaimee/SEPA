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

package it.unibo.arces.wot.sepa.tools;

import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ToolTipManager;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.xml.datatype.DatatypeFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JScrollPane;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.JTabbedPane;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JSplitPane;

import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;
import it.unibo.arces.wot.sepa.pattern.GenericClient;
import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.api.SPARQL11SEProperties.SubscriptionProtocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties.HTTPMethod;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;
import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;

import javax.swing.border.TitledBorder;
import java.awt.event.KeyEvent;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.border.LineBorder;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import java.awt.Panel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.JTextArea;

public class Dashboard {
	private static final Logger logger = LogManager.getLogger();

	private static final String versionLabel = "SEPA Dashboard Ver 0.9.2";

	private Properties appProperties = new Properties();

	private DefaultTableModel namespacesDM;
	private String namespacesHeader[] = new String[] { "Prefix", "URI" };

	private BindingsTableModel bindingsDM = new BindingsTableModel();
	private BindingsRender bindingsRender = new BindingsRender();

	private ForcedBindingsTableModel updateForcedBindingsDM = new ForcedBindingsTableModel();
	private ForcedBindingsTableModel subscribeForcedBindingsDM = new ForcedBindingsTableModel();

	private SortedListModel updateListDM = new SortedListModel();
	private SortedListModel subscribeListDM = new SortedListModel();

	private GenericClient sepaClient;
	private DashboardHandler handler = new DashboardHandler();

	private JTabbedPane subscriptions;
	private HashMap<String, BindingsTableModel> subscriptionResultsDM = new HashMap<String, BindingsTableModel>();
	private HashMap<String, JLabel> subscriptionResultsLabels = new HashMap<String, JLabel>();
	private HashMap<String, JTable> subscriptionResultsTables = new HashMap<String, JTable>();

	private JCheckBox chckbxClearonnotify;

	private JTextArea updateSPARQL;
	private JTextArea querySPARQL;

	class DashboardHandler implements ISubscriptionHandler {
		@Override
		public void onSemanticEvent(Notification n) {
			ARBindingsResults notify = n.getARBindingsResults();
			String spuid = n.getSpuid();

			int added = 0;
			int removed = 0;

			if (notify != null) {

				if (notify.getAddedBindings() != null)
					added = notify.getAddedBindings().size();
				if (notify.getRemovedBindings() != null)
					removed = notify.getRemovedBindings().size();

				if (chckbxClearonnotify.isSelected())
					subscriptionResultsDM.get(spuid).clear();

				subscriptionResultsDM.get(spuid).setResults(notify, spuid);

				subscriptionResultsLabels.get(spuid)
						.setText("Bindings results (" + subscriptionResultsDM.get(spuid).getRowCount() + ") Added("
								+ added + ") + Removed (" + removed + ")");
			}

		}

		@Override
		public void onBrokenConnection() {

		}

		@Override
		public void onError(ErrorResponse errorResponse) {
			lblInfo.setText(errorResponse.getErrorMessage());
			lblInfo.setToolTipText(errorResponse.getErrorMessage());
		}
	}

	private class SortedListModel extends AbstractListModel<String> {

		/**
		* 
		*/
		private static final long serialVersionUID = -4860350252985388420L;

		SortedSet<String> model;

		public SortedListModel() {
			model = new TreeSet<String>();
		}

		public int getSize() {
			return model.size();
		}

		public String getElementAt(int index) {
			return (String) model.toArray()[index];
		}

		public void add(String element) {
			if (model.add(element)) {
				fireContentsChanged(this, 0, getSize());
			}
		}

		public void clear() {
			model.clear();
			fireContentsChanged(this, 0, getSize());
		}
	}

	private DefaultTableModel propertiesDM;
	private String propertiesHeader[] = new String[] { "Property", "Domain", "Range", "Comment" };

	private Response response;
	private JFrame frmSepaDashboard;

	static Dashboard window;
	private JTable namespacesTable;

	private JLabel lblInfo;
	private JCheckBox chckbxAutoscroll;

	ApplicationProfile appProfile;
	private JTextField updateTimeout;
	private JTextField queryTimeout;
	private JTable bindingsResultsTable;
	private JTable updateForcedBindings;
	private JTable queryForcedBindings;

	private JLabel updateURL;

	private JLabel usingGraphURI;

	private JLabel usingNamedGraphURI;

	private JLabel defaultGraphURI;

	private JLabel namedGraphURI;

	private JLabel subscribeURL;

	private JLabel queryURL;

	private JButton updateButton;

	private JButton queryButton;

	private JButton subscribeButton;

	private String updateID;

	private String queryID;

	private Panel sparqlTab;

	private JList<String> queryList;

	private class CopyAction extends AbstractAction {

		/**
		 * 
		 */
		private static final long serialVersionUID = 5927169526678239559L;

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void actionPerformed(ActionEvent e) {
			final JTable tbl = (JTable) e.getSource();
			final int row = tbl.getSelectedRow();
			final int col = tbl.getSelectedColumn();
			if (row >= 0 && col >= 0) {
				final TableCellRenderer renderer = tbl.getCellRenderer(row, col);
				final Component comp = tbl.prepareRenderer(renderer, row, col);
				if (comp instanceof JLabel) {
					final String toCopy = ((JLabel) comp).getText();
					final StringSelection selection = new StringSelection(toCopy);
					final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
					clipboard.setContents(selection, selection);
				}
			}
		}

	}

	private class ForcedBindingsTableModel extends AbstractTableModel {
		/**
		 * 
		 */
		private static final long serialVersionUID = -8524602022439421892L;

		ArrayList<String[]> rowValues = new ArrayList<String[]>();
		ArrayList<String> rowTypes = new ArrayList<String>();
		ArrayList<String> columns = new ArrayList<String>();

		public void clearBindings() {
			rowValues.clear();
			rowTypes.clear();

			super.fireTableDataChanged();
		}

		public void addBindings(String variable, String literal) {
			rowValues.add(new String[] { variable, "" });
			rowTypes.add(literal);

			super.fireTableDataChanged();
		}

		public ForcedBindingsTableModel() {
			columns.add("Variable");
			columns.add("Value");
			columns.add("Datatype");
		}

		@Override
		public int getRowCount() {
			return rowValues.size();
		}

		@Override
		public int getColumnCount() {
			return 3;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if (columnIndex == 0 || columnIndex == 1)
				return rowValues.get(rowIndex)[columnIndex];
			return rowTypes.get(rowIndex);
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			/*
			 * if (columnIndex == 0 || columnIndex == 1) return String.class; return
			 * Boolean.class;
			 */
			return String.class;
		}

		@Override
		public String getColumnName(int columnIndex) {
			if (columnIndex < getColumnCount())
				return columns.get(columnIndex);
			return null;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			if (columnIndex == 1)
				return true;
			return false;
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			super.setValueAt(aValue, rowIndex, columnIndex);

			if (columnIndex == 1) {
				String[] currentValue = rowValues.get(rowIndex);
				currentValue[1] = (String) aValue;
				rowValues.set(rowIndex, currentValue);
			}
			if (columnIndex == 2)
				rowTypes.set(rowIndex, (String) aValue);

			super.fireTableCellUpdated(rowIndex, columnIndex);
		}

		@Override
		public void addTableModelListener(TableModelListener l) {
			super.addTableModelListener(l);
		}

		@Override
		public void removeTableModelListener(TableModelListener l) {
			super.removeTableModelListener(l);
		}

	}

	private class BindingValue {
		private boolean added = true;
		private String value;
		private boolean literal = true;

		public BindingValue(String value, boolean literal, boolean added) {
			this.value = value;
			this.added = added;
			this.literal = literal;
		}

		public boolean isAdded() {
			return added;

		}

		public String get() {
			return value;
		}

		public boolean isLiteral() {
			return literal;
		}
	}

	private class BindingsTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 2698789913874225961L;

		ArrayList<HashMap<String, BindingValue>> rows = new ArrayList<HashMap<String, BindingValue>>();
		ArrayList<String> columns = new ArrayList<String>();

		public void clear() {
			columns.clear();
			rows.clear();

			super.fireTableStructureChanged();
			super.fireTableDataChanged();
		}

		public void setResults(ARBindingsResults res, String spuid) {
			if (res == null)
				return;

			// Date date = new Date();
			// SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
			// String timestamp = sdf.format(date);

			ArrayList<String> vars = res.getAddedBindings().getVariables();
			for (String var : res.getRemovedBindings().getVariables()) {
				if (!vars.contains(var))
					vars.add(var);
			}

			if (!columns.containsAll(vars) || columns.size() != vars.size()) {
				columns.clear();
				columns.addAll(vars);
				super.fireTableStructureChanged();
			}

			if (res.getAddedBindings() != null) {
				for (Bindings sol : res.getAddedBindings().getBindings()) {
					HashMap<String, BindingValue> row = new HashMap<String, BindingValue>();
					for (String var : sol.getVariables()) {
						row.put(var, new BindingValue(sol.getBindingValue(var), sol.isLiteral(var), true));
					}
					// row.put("", new BindingValue(timestamp, false, true));
					rows.add(row);
				}
			}

			if (res.getRemovedBindings() != null) {
				for (Bindings sol : res.getRemovedBindings().getBindings()) {
					HashMap<String, BindingValue> row = new HashMap<String, BindingValue>();
					for (String var : sol.getVariables()) {
						row.put(var, new BindingValue(sol.getBindingValue(var), sol.isLiteral(var), false));
					}
					// row.put("", new BindingValue(timestamp, false, false));
					rows.add(row);
				}
			}

			if (chckbxAutoscroll.isSelected())
				if (spuid != null)
					subscriptionResultsTables.get(spuid)
							.changeSelection(subscriptionResultsTables.get(spuid).getRowCount() - 1, 0, false, false);
				else
					bindingsResultsTable.changeSelection(bindingsResultsTable.getRowCount() - 1, 0, false, false);

			super.fireTableDataChanged();
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return BindingValue.class;
		}

		@Override
		public int getRowCount() {
			return rows.size();
		}

		@Override
		public int getColumnCount() {
			return columns.size();
		}

		@Override
		public String getColumnName(int columnIndex) {
			if (columnIndex < getColumnCount())
				return columns.get(columnIndex);
			return null;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return false;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			BindingValue ret = null;
			if (rowIndex < getRowCount() && columnIndex < getColumnCount()) {
				ret = rows.get(rowIndex).get(columns.get(columnIndex));
			}
			return ret;
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			super.setValueAt(aValue, rowIndex, columnIndex);

			super.fireTableCellUpdated(rowIndex, columnIndex);
		}

		@Override
		public void addTableModelListener(TableModelListener l) {
			super.addTableModelListener(l);
		}

		@Override
		public void removeTableModelListener(TableModelListener l) {
			super.removeTableModelListener(l);
		}

		public void setAddedResults(BindingsResults bindingsResults, String spuid) {
			if (bindingsResults == null)
				return;

			// Date date = new Date();
			// SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
			// String timestamp = sdf.format(date);

			ArrayList<String> vars = bindingsResults.getVariables();

			if (!columns.containsAll(vars) || columns.size() != vars.size()) {
				columns.clear();
				// vars.add("");
				columns.addAll(vars);
				super.fireTableStructureChanged();
			}

			for (Bindings sol : bindingsResults.getBindings()) {
				HashMap<String, BindingValue> row = new HashMap<String, BindingValue>();
				for (String var : sol.getVariables()) {
					row.put(var, new BindingValue(sol.getBindingValue(var), sol.isLiteral(var), true));
				}
				// row.put("", new BindingValue(timestamp, false, true));
				rows.add(row);
			}

			if (chckbxAutoscroll.isSelected())
				if (spuid != null)
					subscriptionResultsTables.get(spuid)
							.changeSelection(subscriptionResultsTables.get(spuid).getRowCount() - 1, 0, false, false);
				else
					bindingsResultsTable.changeSelection(bindingsResultsTable.getRowCount() - 1, 0, false, false);

			super.fireTableDataChanged();
		}
	}

	private class ForcedBindingsRenderer extends DefaultTableCellRenderer {
		/**
		 * 
		 */
		private static final long serialVersionUID = -1541296097107576037L;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int col) {

			// Cells are by default rendered as a JLabel.
			JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

			if (col == 2) {
				String v = (String) table.getValueAt(row, 1);
				String type = (String) table.getValueAt(row, 2);
				logger.debug("Row: " + row + " Col: " + col + " Value: " + v + " Type: " + type);
				if (checkType(v, type)) {
					if (v.equals(""))
						l.setBackground(Color.ORANGE);
					else
						l.setBackground(Color.GREEN);
				} else
					l.setBackground(Color.RED);
			} else
				l.setBackground(Color.WHITE);

			l.setForeground(Color.BLACK);

			return l;
		}
	}

	private class BindingsRender extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 3932800852596396532L;

		DefaultTableModel namespaces;
		private boolean showAsQname = true;

		public BindingsRender() {
			super();
		}

		public void setNamespaces(DefaultTableModel namespaces) {
			this.namespaces = namespaces;
		}

		public void showAsQName(boolean set) {
			showAsQname = set;
		}

		private String qName(String uri) {
			if (namespaces == null)
				return uri;
			if (uri == null)
				return null;
			for (int row = 0; row < namespaces.getRowCount(); row++) {
				String prefix = namespaces.getValueAt(row, 0).toString();
				String ns = namespaces.getValueAt(row, 1).toString();
				if (uri.startsWith(ns))
					return uri.replace(ns, prefix + ":");
			}
			return uri;
		}

		@Override
		public void setValue(Object value) {
			if (value == null)
				return;

			BindingValue binding = (BindingValue) value;

			if (binding.isLiteral()) {
				setFont(new Font(null, Font.BOLD, 12));
				setForeground(Color.BLACK);
			} else {
				setFont(new Font(null, Font.PLAIN, 12));
				setForeground(Color.BLACK);
			}
			if (binding.isAdded()) {
				setBackground(Color.WHITE);
			} else
				setBackground(Color.LIGHT_GRAY);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			BindingValue binding = (BindingValue) value;

			if (binding == null) {
				setText("");
				return this;
			}
			if (binding.get() == null) {
				setText("");
				return this;
			}

			// Render as qname or URI
			if (showAsQname)
				setText(qName(binding.get()));
			else
				setText(binding.get());

			return this;
		}
	}

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					window = new Dashboard();
					window.frmSepaDashboard.setVisible(true);
				} catch (Exception e) {
					logger.fatal(e.getMessage());
				}
			}
		});
	}

	/**
	 * Create the application.
	 * 
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws NoSuchPaddingException
	 * @throws ClassCastException
	 * @throws NullPointerException
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 * @throws IllegalArgumentException
	 */
	public Dashboard() {
		initialize();

		loadSAP(null);
	}

	private boolean loadSAP(String file) {
		if (file == null) {
			FileInputStream in = null;
			try {
				in = new FileInputStream("dashboard.properties");
			} catch (FileNotFoundException e) {
				logger.warn(e.getMessage());
				lblInfo.setText("Error: " + e.getMessage());
				lblInfo.setToolTipText("Error: " + e.getMessage());
				frmSepaDashboard.setTitle(versionLabel + " - " + e.getMessage());
				return false;
			}

			// Properties sapFile = new Properties();
			try {
				appProperties.load(in);
			} catch (IOException e) {
				logger.error(e.getMessage());
				lblInfo.setText("Error: " + e.getMessage());
				lblInfo.setToolTipText("Error: " + e.getMessage());
				frmSepaDashboard.setTitle(versionLabel + " - " + e.getMessage());
				return false;
			}
			String path = appProperties.getProperty("appProfile");
			if (path == null) {
				lblInfo.setText("Error: path in dashboard.properties is null");
				lblInfo.setToolTipText("Error: path in dashboard.properties is null");
				frmSepaDashboard.setTitle(versionLabel + " - " + "path in dashboard.properties is null");
				return false;
			}
			file = path;
		}
		namespacesDM.getDataVector().clear();
		updateListDM.clear();
		subscribeListDM.clear();
		updateForcedBindingsDM.clearBindings();
		subscribeForcedBindingsDM.clearBindings();

		try {
			appProfile = new ApplicationProfile(file);
		} catch (SEPAPropertiesException e) {
			logger.error(e.getMessage());
			lblInfo.setText("Error: " + e.getMessage());
			lblInfo.setToolTipText("Error: " + e.getMessage());
			frmSepaDashboard.setTitle(versionLabel + " - " + e.getMessage());
			return false;
		}

		frmSepaDashboard.setTitle(versionLabel + " - " + file);

		// Loading namespaces
		for (String prefix : appProfile.getPrefixes()) {
			Vector<String> row = new Vector<String>();
			row.add(prefix);
			row.addElement(appProfile.getNamespaceURI(prefix));
			namespacesDM.addRow(row);
		}

		// Loading updates
		for (String update : appProfile.getUpdateIds()) {
			// updateListDM.addElement(update);
			updateListDM.add(update);
		}

		// Loading subscribes
		for (String subscribe : appProfile.getQueryIds()) {
			// subscribeListDM.addElement(subscribe);
			subscribeListDM.add(subscribe);
		}

		lblInfo.setText("JSAP loaded");
		lblInfo.setToolTipText("JSAP loaded");

		try {
			sepaClient = new GenericClient(appProfile);
		} catch (SEPAProtocolException | SEPASecurityException e) {
			logger.error(e.getMessage());
			System.exit(-1);
		}

		return true;
	}

	private class DashboardFileFilter extends FileFilter {
		private ArrayList<String> extensions = new ArrayList<String>();
		private String title = "Title";

		public DashboardFileFilter(String title, String ext) {
			super();
			extensions.add(ext);
			this.title = title;
		}

		@Override
		public boolean accept(File f) {
			for (String ext : extensions)
				if (f.getName().contains(ext))
					return true;
			return false;
		}

		@Override
		public String getDescription() {
			return title;
		}

	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		namespacesDM = new DefaultTableModel(0, 0) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 6788045463932990156L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		namespacesDM.setColumnIdentifiers(namespacesHeader);

		propertiesDM = new DefaultTableModel(0, 0) {

			/**
			 * 
			 */
			private static final long serialVersionUID = -5161490469556412655L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		propertiesDM.setColumnIdentifiers(propertiesHeader);

		frmSepaDashboard = new JFrame();
		frmSepaDashboard.setTitle("SEPA Dashboard Ver 0.9.1");
		frmSepaDashboard.setBounds(100, 100, 925, 768);
		frmSepaDashboard.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 925, 0 };
		gridBagLayout.rowHeights = new int[] { 651, 39, 0 };
		gridBagLayout.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 1.0, 0.0, Double.MIN_VALUE };
		frmSepaDashboard.getContentPane().setLayout(gridBagLayout);

		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		GridBagConstraints gbc_tabbedPane = new GridBagConstraints();
		gbc_tabbedPane.insets = new Insets(0, 0, 5, 0);
		gbc_tabbedPane.fill = GridBagConstraints.BOTH;
		gbc_tabbedPane.gridx = 0;
		gbc_tabbedPane.gridy = 0;
		frmSepaDashboard.getContentPane().add(tabbedPane, gbc_tabbedPane);

		sparqlTab = new Panel();
		tabbedPane.addTab("SPARQL", null, sparqlTab, null);
		tabbedPane.setEnabledAt(0, true);
		GridBagLayout gbl_sparqlTab = new GridBagLayout();
		gbl_sparqlTab.columnWidths = new int[] { 338, 0, 0 };
		gbl_sparqlTab.rowHeights = new int[] { 0, 155, -19, 39, 0, 0 };
		gbl_sparqlTab.columnWeights = new double[] { 1.0, 1.0, Double.MIN_VALUE };
		gbl_sparqlTab.rowWeights = new double[] { 0.0, 0.0, 1.0, 0.0, 1.0, Double.MIN_VALUE };
		sparqlTab.setLayout(gbl_sparqlTab);

		JPanel panel = new JPanel();
		panel.setBorder(new LineBorder(new Color(0, 0, 0), 1, true));
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.insets = new Insets(0, 0, 5, 5);
		gbc_panel.fill = GridBagConstraints.BOTH;
		gbc_panel.gridx = 0;
		gbc_panel.gridy = 0;
		sparqlTab.add(panel, gbc_panel);
		GridBagLayout gbl_panel = new GridBagLayout();
		gbl_panel.columnWidths = new int[] { 0, 0, 0 };
		gbl_panel.rowHeights = new int[] { 0, 22, 0, 0, 0 };
		gbl_panel.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		gbl_panel.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		panel.setLayout(gbl_panel);

		updateURL = new JLabel("-");
		updateURL.setForeground(UIManager.getColor("ComboBox.buttonDarkShadow"));
		updateURL.setFont(new Font("Lucida Grande", Font.PLAIN, 13));
		GridBagConstraints gbc_updateURL = new GridBagConstraints();
		gbc_updateURL.anchor = GridBagConstraints.WEST;
		gbc_updateURL.gridwidth = 2;
		gbc_updateURL.insets = new Insets(0, 0, 5, 0);
		gbc_updateURL.gridx = 0;
		gbc_updateURL.gridy = 0;
		panel.add(updateURL, gbc_updateURL);

		JLabel label_1 = new JLabel("");
		GridBagConstraints gbc_label_1 = new GridBagConstraints();
		gbc_label_1.insets = new Insets(0, 0, 5, 5);
		gbc_label_1.gridx = 0;
		gbc_label_1.gridy = 1;
		panel.add(label_1, gbc_label_1);

		JLabel label_2 = new JLabel("using-graph-uri:");
		label_2.setFont(new Font("Lucida Grande", Font.BOLD, 13));
		label_2.setForeground(UIManager.getColor("ComboBox.buttonDarkShadow"));
		GridBagConstraints gbc_label_2 = new GridBagConstraints();
		gbc_label_2.anchor = GridBagConstraints.EAST;
		gbc_label_2.insets = new Insets(0, 0, 5, 5);
		gbc_label_2.gridx = 0;
		gbc_label_2.gridy = 2;
		panel.add(label_2, gbc_label_2);

		usingGraphURI = new JLabel("-");
		usingGraphURI.setForeground(UIManager.getColor("ComboBox.buttonDarkShadow"));
		usingGraphURI.setFont(new Font("Lucida Grande", Font.PLAIN, 13));
		GridBagConstraints gbc_updateUsingGraphURI = new GridBagConstraints();
		gbc_updateUsingGraphURI.anchor = GridBagConstraints.WEST;
		gbc_updateUsingGraphURI.insets = new Insets(0, 0, 5, 0);
		gbc_updateUsingGraphURI.gridx = 1;
		gbc_updateUsingGraphURI.gridy = 2;
		panel.add(usingGraphURI, gbc_updateUsingGraphURI);

		JLabel label_4 = new JLabel("using-named-graph-uri:");
		label_4.setFont(new Font("Lucida Grande", Font.BOLD, 13));
		label_4.setForeground(UIManager.getColor("ComboBox.buttonDarkShadow"));
		GridBagConstraints gbc_label_4 = new GridBagConstraints();
		gbc_label_4.anchor = GridBagConstraints.EAST;
		gbc_label_4.insets = new Insets(0, 0, 0, 5);
		gbc_label_4.gridx = 0;
		gbc_label_4.gridy = 3;
		panel.add(label_4, gbc_label_4);

		usingNamedGraphURI = new JLabel("-");
		usingNamedGraphURI.setForeground(UIManager.getColor("ComboBox.buttonDarkShadow"));
		usingNamedGraphURI.setFont(new Font("Lucida Grande", Font.PLAIN, 13));
		GridBagConstraints gbc_updateUsingNamedGraphURI = new GridBagConstraints();
		gbc_updateUsingNamedGraphURI.anchor = GridBagConstraints.WEST;
		gbc_updateUsingNamedGraphURI.gridx = 1;
		gbc_updateUsingNamedGraphURI.gridy = 3;
		panel.add(usingNamedGraphURI, gbc_updateUsingNamedGraphURI);

		JPanel panel_1 = new JPanel();
		panel_1.setBorder(new LineBorder(new Color(0, 0, 0), 1, true));
		GridBagConstraints gbc_panel_1 = new GridBagConstraints();
		gbc_panel_1.insets = new Insets(0, 0, 5, 0);
		gbc_panel_1.fill = GridBagConstraints.BOTH;
		gbc_panel_1.gridx = 1;
		gbc_panel_1.gridy = 0;
		sparqlTab.add(panel_1, gbc_panel_1);
		GridBagLayout gbl_panel_1 = new GridBagLayout();
		gbl_panel_1.columnWidths = new int[] { 0, 0, 0 };
		gbl_panel_1.rowHeights = new int[] { 0, 0, 0, 0, 0 };
		gbl_panel_1.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		gbl_panel_1.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		panel_1.setLayout(gbl_panel_1);

		queryURL = new JLabel("-");
		queryURL.setForeground(UIManager.getColor("ComboBox.buttonDarkShadow"));
		queryURL.setFont(new Font("Lucida Grande", Font.PLAIN, 13));
		GridBagConstraints gbc_queryURL = new GridBagConstraints();
		gbc_queryURL.anchor = GridBagConstraints.WEST;
		gbc_queryURL.gridwidth = 2;
		gbc_queryURL.insets = new Insets(0, 0, 5, 0);
		gbc_queryURL.gridx = 0;
		gbc_queryURL.gridy = 0;
		panel_1.add(queryURL, gbc_queryURL);

		subscribeURL = new JLabel("-");
		subscribeURL.setForeground(UIManager.getColor("ComboBox.buttonDarkShadow"));
		subscribeURL.setFont(new Font("Lucida Grande", Font.PLAIN, 13));
		GridBagConstraints gbc_subscribeURL = new GridBagConstraints();
		gbc_subscribeURL.anchor = GridBagConstraints.WEST;
		gbc_subscribeURL.gridwidth = 2;
		gbc_subscribeURL.insets = new Insets(0, 0, 5, 0);
		gbc_subscribeURL.gridx = 0;
		gbc_subscribeURL.gridy = 1;
		panel_1.add(subscribeURL, gbc_subscribeURL);

		JLabel label_8 = new JLabel("default-graph-uri:");
		label_8.setFont(new Font("Lucida Grande", Font.BOLD, 13));
		label_8.setForeground(UIManager.getColor("ComboBox.buttonDarkShadow"));
		GridBagConstraints gbc_label_8 = new GridBagConstraints();
		gbc_label_8.anchor = GridBagConstraints.EAST;
		gbc_label_8.insets = new Insets(0, 0, 5, 5);
		gbc_label_8.gridx = 0;
		gbc_label_8.gridy = 2;
		panel_1.add(label_8, gbc_label_8);

		defaultGraphURI = new JLabel("-");
		defaultGraphURI.setForeground(UIManager.getColor("ComboBox.buttonDarkShadow"));
		defaultGraphURI.setFont(new Font("Lucida Grande", Font.PLAIN, 13));
		GridBagConstraints gbc_defaultGraphURI = new GridBagConstraints();
		gbc_defaultGraphURI.anchor = GridBagConstraints.WEST;
		gbc_defaultGraphURI.insets = new Insets(0, 0, 5, 0);
		gbc_defaultGraphURI.gridx = 1;
		gbc_defaultGraphURI.gridy = 2;
		panel_1.add(defaultGraphURI, gbc_defaultGraphURI);

		JLabel label_10 = new JLabel("named-graph-uri:");
		label_10.setFont(new Font("Lucida Grande", Font.BOLD, 13));
		label_10.setForeground(UIManager.getColor("ComboBox.buttonDarkShadow"));
		GridBagConstraints gbc_label_10 = new GridBagConstraints();
		gbc_label_10.anchor = GridBagConstraints.EAST;
		gbc_label_10.insets = new Insets(0, 0, 0, 5);
		gbc_label_10.gridx = 0;
		gbc_label_10.gridy = 3;
		panel_1.add(label_10, gbc_label_10);

		namedGraphURI = new JLabel("-");
		namedGraphURI.setForeground(UIManager.getColor("ComboBox.buttonDarkShadow"));
		namedGraphURI.setFont(new Font("Lucida Grande", Font.PLAIN, 13));
		GridBagConstraints gbc_namedGraphURI = new GridBagConstraints();
		gbc_namedGraphURI.anchor = GridBagConstraints.WEST;
		gbc_namedGraphURI.gridx = 1;
		gbc_namedGraphURI.gridy = 3;
		panel_1.add(namedGraphURI, gbc_namedGraphURI);

		JSplitPane splitPane_2 = new JSplitPane();
		splitPane_2.setResizeWeight(0.5);
		GridBagConstraints gbc_splitPane_2 = new GridBagConstraints();
		gbc_splitPane_2.insets = new Insets(0, 0, 5, 5);
		gbc_splitPane_2.fill = GridBagConstraints.BOTH;
		gbc_splitPane_2.gridx = 0;
		gbc_splitPane_2.gridy = 1;
		sparqlTab.add(splitPane_2, gbc_splitPane_2);

		JPanel panel_2 = new JPanel();
		splitPane_2.setLeftComponent(panel_2);
		GridBagLayout gbl_panel_2 = new GridBagLayout();
		gbl_panel_2.columnWidths = new int[] { 66, 0 };
		gbl_panel_2.rowHeights = new int[] { 17, 75, 0 };
		gbl_panel_2.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_panel_2.rowWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		panel_2.setLayout(gbl_panel_2);

		JLabel label_12 = new JLabel("UPDATES");
		label_12.setForeground(UIManager.getColor("Desktop.background"));
		label_12.setFont(new Font("Lucida Grande", Font.BOLD, 14));
		GridBagConstraints gbc_label_12 = new GridBagConstraints();
		gbc_label_12.anchor = GridBagConstraints.NORTH;
		gbc_label_12.insets = new Insets(0, 0, 5, 0);
		gbc_label_12.gridx = 0;
		gbc_label_12.gridy = 0;
		panel_2.add(label_12, gbc_label_12);

		JScrollPane scrollPane = new JScrollPane();
		GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.fill = GridBagConstraints.BOTH;
		gbc_scrollPane.gridx = 0;
		gbc_scrollPane.gridy = 1;
		panel_2.add(scrollPane, gbc_scrollPane);

		JList<String> updateList = new JList<String>();
		updateList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				selectUpdateID(updateList.getSelectedValue());
			}
		});
		updateList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		updateList.setModel(updateListDM);
		scrollPane.setViewportView(updateList);

		JPanel panel_3 = new JPanel();
		splitPane_2.setRightComponent(panel_3);
		GridBagLayout gbl_panel_3 = new GridBagLayout();
		gbl_panel_3.columnWidths = new int[] { 101, 0 };
		gbl_panel_3.rowHeights = new int[] { 16, 0, 0 };
		gbl_panel_3.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_panel_3.rowWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		panel_3.setLayout(gbl_panel_3);

		JLabel lblForcedBindings = new JLabel("FORCED BINDINGS");
		lblForcedBindings.setForeground(UIManager.getColor("Desktop.background"));
		GridBagConstraints gbc_lblForcedBindings = new GridBagConstraints();
		gbc_lblForcedBindings.anchor = GridBagConstraints.NORTH;
		gbc_lblForcedBindings.insets = new Insets(0, 0, 5, 0);
		gbc_lblForcedBindings.gridx = 0;
		gbc_lblForcedBindings.gridy = 0;
		panel_3.add(lblForcedBindings, gbc_lblForcedBindings);

		JScrollPane scrollPane_2 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_2 = new GridBagConstraints();
		gbc_scrollPane_2.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_2.gridx = 0;
		gbc_scrollPane_2.gridy = 1;
		panel_3.add(scrollPane_2, gbc_scrollPane_2);

		updateForcedBindings = new JTable(updateForcedBindingsDM);
		updateForcedBindings.setCellSelectionEnabled(true);
		updateForcedBindings.setRowSelectionAllowed(false);
		updateForcedBindings.setFillsViewportHeight(true);
		scrollPane_2.setViewportView(updateForcedBindings);
		updateForcedBindings.getModel().addTableModelListener(new TableModelListener() {
			@Override
			public void tableChanged(TableModelEvent e) {
				enableUpdateButton();
			}

		});
		updateForcedBindings.setDefaultRenderer(String.class, new ForcedBindingsRenderer());
		updateForcedBindings.registerKeyboardAction(new CopyAction(),
				KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
				JComponent.WHEN_FOCUSED);
		updateForcedBindings.setCellSelectionEnabled(true);

		
		JSplitPane splitPane_3 = new JSplitPane();
		GridBagConstraints gbc_splitPane_3 = new GridBagConstraints();
		gbc_splitPane_3.insets = new Insets(0, 0, 5, 0);
		gbc_splitPane_3.fill = GridBagConstraints.BOTH;
		gbc_splitPane_3.gridx = 1;
		gbc_splitPane_3.gridy = 1;
		sparqlTab.add(splitPane_3, gbc_splitPane_3);

		JPanel panel_4 = new JPanel();
		splitPane_3.setLeftComponent(panel_4);
		GridBagLayout gbl_panel_4 = new GridBagLayout();
		gbl_panel_4.columnWidths = new int[] { 193, 0 };
		gbl_panel_4.rowHeights = new int[] { 17, 72, 0 };
		gbl_panel_4.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_panel_4.rowWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		panel_4.setLayout(gbl_panel_4);

		JLabel label_14 = new JLabel("QUERIES");
		label_14.setForeground(UIManager.getColor("Desktop.background"));
		label_14.setFont(new Font("Lucida Grande", Font.BOLD, 14));
		GridBagConstraints gbc_label_14 = new GridBagConstraints();
		gbc_label_14.anchor = GridBagConstraints.NORTH;
		gbc_label_14.insets = new Insets(0, 0, 5, 0);
		gbc_label_14.gridx = 0;
		gbc_label_14.gridy = 0;
		panel_4.add(label_14, gbc_label_14);

		JScrollPane scrollPane_1 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_1 = new GridBagConstraints();
		gbc_scrollPane_1.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_1.gridx = 0;
		gbc_scrollPane_1.gridy = 1;
		panel_4.add(scrollPane_1, gbc_scrollPane_1);

		queryList = new JList<String>();
		queryList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				selectQueryID(queryList.getSelectedValue());
			}
		});
		queryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		queryList.setModel(subscribeListDM);
		scrollPane_1.setViewportView(queryList);

		JPanel panel_5 = new JPanel();
		splitPane_3.setRightComponent(panel_5);
		GridBagLayout gbl_panel_5 = new GridBagLayout();
		gbl_panel_5.columnWidths = new int[] { 123, 0 };
		gbl_panel_5.rowHeights = new int[] { 16, 126, 0 };
		gbl_panel_5.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_panel_5.rowWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		panel_5.setLayout(gbl_panel_5);

		JLabel lblForcedBindings_1 = new JLabel("FORCED BINDINGS");
		lblForcedBindings_1.setForeground(UIManager.getColor("Desktop.background"));
		GridBagConstraints gbc_lblForcedBindings_1 = new GridBagConstraints();
		gbc_lblForcedBindings_1.anchor = GridBagConstraints.NORTH;
		gbc_lblForcedBindings_1.insets = new Insets(0, 0, 5, 0);
		gbc_lblForcedBindings_1.gridx = 0;
		gbc_lblForcedBindings_1.gridy = 0;
		panel_5.add(lblForcedBindings_1, gbc_lblForcedBindings_1);

		JScrollPane scrollPane_3 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_3 = new GridBagConstraints();
		gbc_scrollPane_3.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_3.gridx = 0;
		gbc_scrollPane_3.gridy = 1;
		panel_5.add(scrollPane_3, gbc_scrollPane_3);

		queryForcedBindings = new JTable(subscribeForcedBindingsDM);
		queryForcedBindings.setFillsViewportHeight(true);
		scrollPane_3.setViewportView(queryForcedBindings);
		queryForcedBindings.getModel().addTableModelListener(new TableModelListener() {
			@Override
			public void tableChanged(TableModelEvent e) {
				enableQueryButton();
			}
		});
		queryForcedBindings.setDefaultRenderer(String.class, new ForcedBindingsRenderer());
		queryForcedBindings.registerKeyboardAction(new CopyAction(),
				KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
				JComponent.WHEN_FOCUSED);
		queryForcedBindings.setCellSelectionEnabled(true);

		JScrollPane scrollPane_8 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_8 = new GridBagConstraints();
		gbc_scrollPane_8.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_8.insets = new Insets(0, 0, 5, 5);
		gbc_scrollPane_8.gridx = 0;
		gbc_scrollPane_8.gridy = 2;
		sparqlTab.add(scrollPane_8, gbc_scrollPane_8);

		updateSPARQL = new JTextArea();
		scrollPane_8.setViewportView(updateSPARQL);
		updateSPARQL.setLineWrap(true);

		JScrollPane scrollPane_9 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_9 = new GridBagConstraints();
		gbc_scrollPane_9.insets = new Insets(0, 0, 5, 0);
		gbc_scrollPane_9.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_9.gridx = 1;
		gbc_scrollPane_9.gridy = 2;
		sparqlTab.add(scrollPane_9, gbc_scrollPane_9);

		querySPARQL = new JTextArea();
		querySPARQL.setLineWrap(true);
		scrollPane_9.setViewportView(querySPARQL);

		JPanel panel_11 = new JPanel();
		GridBagConstraints gbc_panel_11 = new GridBagConstraints();
		gbc_panel_11.fill = GridBagConstraints.HORIZONTAL;
		gbc_panel_11.insets = new Insets(0, 0, 5, 5);
		gbc_panel_11.gridx = 0;
		gbc_panel_11.gridy = 3;
		sparqlTab.add(panel_11, gbc_panel_11);
		GridBagLayout gbl_panel_11 = new GridBagLayout();
		gbl_panel_11.columnWidths = new int[] { 0, 0, 0, 0 };
		gbl_panel_11.rowHeights = new int[] { 0, 0 };
		gbl_panel_11.columnWeights = new double[] { 0.0, 0.0, 0.0, Double.MIN_VALUE };
		gbl_panel_11.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
		panel_11.setLayout(gbl_panel_11);

		updateButton = new JButton("UPDATE");
		GridBagConstraints gbc_updateButton = new GridBagConstraints();
		gbc_updateButton.insets = new Insets(0, 0, 0, 5);
		gbc_updateButton.gridx = 0;
		gbc_updateButton.gridy = 0;
		panel_11.add(updateButton, gbc_updateButton);
		updateButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				update();
			}
		});
		updateButton.setForeground(UIManager.getColor("Desktop.background"));
		updateButton.setEnabled(false);

		updateTimeout = new JTextField();
		GridBagConstraints gbc_updateTimeout = new GridBagConstraints();
		gbc_updateTimeout.insets = new Insets(0, 0, 0, 5);
		gbc_updateTimeout.gridx = 1;
		gbc_updateTimeout.gridy = 0;
		panel_11.add(updateTimeout, gbc_updateTimeout);
		updateTimeout.setText("5000");
		updateTimeout.setColumns(10);

		JLabel label_16 = new JLabel("timeout (ms)");
		GridBagConstraints gbc_label_16 = new GridBagConstraints();
		gbc_label_16.gridx = 2;
		gbc_label_16.gridy = 0;
		panel_11.add(label_16, gbc_label_16);
		label_16.setForeground(Color.GRAY);

		JPanel panel_12 = new JPanel();
		GridBagConstraints gbc_panel_12 = new GridBagConstraints();
		gbc_panel_12.insets = new Insets(0, 0, 5, 0);
		gbc_panel_12.fill = GridBagConstraints.HORIZONTAL;
		gbc_panel_12.gridx = 1;
		gbc_panel_12.gridy = 3;
		sparqlTab.add(panel_12, gbc_panel_12);
		GridBagLayout gbl_panel_12 = new GridBagLayout();
		gbl_panel_12.columnWidths = new int[] { 0, 0, 0, 0, 0 };
		gbl_panel_12.rowHeights = new int[] { 0, 0 };
		gbl_panel_12.columnWeights = new double[] { 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		gbl_panel_12.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
		panel_12.setLayout(gbl_panel_12);

		queryButton = new JButton("QUERY");
		GridBagConstraints gbc_queryButton = new GridBagConstraints();
		gbc_queryButton.insets = new Insets(0, 0, 0, 5);
		gbc_queryButton.gridx = 0;
		gbc_queryButton.gridy = 0;
		panel_12.add(queryButton, gbc_queryButton);
		queryButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				query();
			}
		});
		queryButton.setForeground(UIManager.getColor("Desktop.background"));
		queryButton.setEnabled(false);

		subscribeButton = new JButton("SUBSCRIBE");
		GridBagConstraints gbc_subscribeButton = new GridBagConstraints();
		gbc_subscribeButton.insets = new Insets(0, 0, 0, 5);
		gbc_subscribeButton.gridx = 1;
		gbc_subscribeButton.gridy = 0;
		panel_12.add(subscribeButton, gbc_subscribeButton);
		subscribeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				subscribe();
			}
		});
		subscribeButton.setForeground(UIManager.getColor("Button.select"));
		subscribeButton.setEnabled(false);

		queryTimeout = new JTextField();
		GridBagConstraints gbc_queryTimeout = new GridBagConstraints();
		gbc_queryTimeout.insets = new Insets(0, 0, 0, 5);
		gbc_queryTimeout.gridx = 2;
		gbc_queryTimeout.gridy = 0;
		panel_12.add(queryTimeout, gbc_queryTimeout);
		queryTimeout.setText("5000");
		queryTimeout.setColumns(10);

		JLabel label_17 = new JLabel("timeout (ms)");
		GridBagConstraints gbc_label_17 = new GridBagConstraints();
		gbc_label_17.gridx = 3;
		gbc_label_17.gridy = 0;
		panel_12.add(label_17, gbc_label_17);
		label_17.setForeground(Color.GRAY);

		JScrollPane scrollPane_5 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_5 = new GridBagConstraints();
		gbc_scrollPane_5.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_5.gridwidth = 2;
		gbc_scrollPane_5.insets = new Insets(0, 0, 0, 5);
		gbc_scrollPane_5.gridx = 0;
		gbc_scrollPane_5.gridy = 4;
		sparqlTab.add(scrollPane_5, gbc_scrollPane_5);

		bindingsResultsTable = new JTable(bindingsDM);
		scrollPane_5.setViewportView(bindingsResultsTable);
		bindingsResultsTable.setBorder(UIManager.getBorder("Button.border"));
		bindingsResultsTable.setFillsViewportHeight(true);
		bindingsResultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		bindingsResultsTable.setDefaultRenderer(BindingValue.class, bindingsRender);
		bindingsResultsTable.registerKeyboardAction(new CopyAction(),
				KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
				JComponent.WHEN_FOCUSED);
		bindingsResultsTable.setCellSelectionEnabled(true);
		
		subscriptions = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.addTab("Active subscriptions", null, subscriptions, null);

		JPanel namespaces = new JPanel();
		tabbedPane.addTab("Namespaces", null, namespaces, null);
		namespaces.setBorder(new TitledBorder(null, "Namespaces", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		GridBagLayout gbl_namespaces = new GridBagLayout();
		gbl_namespaces.columnWidths = new int[] { 0, 0, 0 };
		gbl_namespaces.rowHeights = new int[] { 43, 0 };
		gbl_namespaces.columnWeights = new double[] { 1.0, 1.0, Double.MIN_VALUE };
		gbl_namespaces.rowWeights = new double[] { 1.0, Double.MIN_VALUE };
		namespaces.setLayout(gbl_namespaces);

		JScrollPane scrollPane_4 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_4 = new GridBagConstraints();
		gbc_scrollPane_4.gridwidth = 2;
		gbc_scrollPane_4.insets = new Insets(0, 0, 0, 5);
		gbc_scrollPane_4.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_4.gridx = 0;
		gbc_scrollPane_4.gridy = 0;
		namespaces.add(scrollPane_4, gbc_scrollPane_4);

		namespacesTable = new JTable(namespacesDM);
		scrollPane_4.setViewportView(namespacesTable);

		JPanel infoPanel = new JPanel();
		infoPanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {

			}
		});
		GridBagConstraints gbc_infoPanel = new GridBagConstraints();
		gbc_infoPanel.anchor = GridBagConstraints.SOUTH;
		gbc_infoPanel.fill = GridBagConstraints.HORIZONTAL;
		gbc_infoPanel.gridx = 0;
		gbc_infoPanel.gridy = 1;
		frmSepaDashboard.getContentPane().add(infoPanel, gbc_infoPanel);
		GridBagLayout gbl_infoPanel = new GridBagLayout();
		gbl_infoPanel.columnWidths = new int[] { 129, 73, 0, 0, 97, 76, 0 };
		gbl_infoPanel.rowHeights = new int[] { 29, 0 };
		gbl_infoPanel.columnWeights = new double[] { 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		gbl_infoPanel.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
		infoPanel.setLayout(gbl_infoPanel);

		lblInfo = new JLabel("Info");
		lblInfo.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				JOptionPane optionPane = new JOptionPane() {
					/**
					 * 
					 */
					private static final long serialVersionUID = -5251384434573221593L;

					public int getMaxCharactersPerLineCount() {
						return 100;
					}
				};
				optionPane.setMessage(lblInfo.getText());
				optionPane.setMessageType(JOptionPane.INFORMATION_MESSAGE);
				JDialog dialog = optionPane.createDialog(null, "Info");
				dialog.setVisible(true);
			}
		});

		JButton btnLoadXmlProfile = new JButton("Load JSAP");
		btnLoadXmlProfile.setForeground(UIManager.getColor("Button.light"));
		btnLoadXmlProfile.setBackground(UIManager.getColor("Button.background"));
		GridBagConstraints gbc_btnLoadXmlProfile = new GridBagConstraints();
		gbc_btnLoadXmlProfile.insets = new Insets(0, 0, 0, 5);
		gbc_btnLoadXmlProfile.gridx = 0;
		gbc_btnLoadXmlProfile.gridy = 0;
		infoPanel.add(btnLoadXmlProfile, gbc_btnLoadXmlProfile);
		btnLoadXmlProfile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				final JFileChooser fc = new JFileChooser(appProperties.getProperty("appProfile"));
				DashboardFileFilter filter = new DashboardFileFilter("JSON SAP Profile (.jsap)", ".jsap");
				fc.setFileFilter(filter);
				int returnVal = fc.showOpenDialog(frmSepaDashboard);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					String fileName = fc.getSelectedFile().getPath();

					if (loadSAP(fileName)) {
						FileOutputStream out = null;
						try {
							out = new FileOutputStream("dashboard.properties");
						} catch (FileNotFoundException e3) {
							logger.error(e3.getMessage());
							return;
						}

						appProperties = new Properties();
						appProperties.put("appProfile", fileName);

						try {
							appProperties.store(out, "Dashboard properties");
						} catch (IOException e1) {
							logger.error(e1.getMessage());
						}
						try {
							out.close();
						} catch (IOException e2) {
							logger.error(e2.getMessage());
						}

					}
				}
			}
		});
		GridBagConstraints gbc_lblInfo = new GridBagConstraints();
		gbc_lblInfo.anchor = GridBagConstraints.WEST;
		gbc_lblInfo.insets = new Insets(0, 10, 0, 5);
		gbc_lblInfo.gridx = 1;
		gbc_lblInfo.gridy = 0;
		infoPanel.add(lblInfo, gbc_lblInfo);
		ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);

		chckbxClearonnotify = new JCheckBox("ClearOnNotify");
		chckbxClearonnotify.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
			}
		});
		GridBagConstraints gbc_chckbxClearonnotify = new GridBagConstraints();
		gbc_chckbxClearonnotify.insets = new Insets(0, 0, 0, 5);
		gbc_chckbxClearonnotify.gridx = 2;
		gbc_chckbxClearonnotify.gridy = 0;
		infoPanel.add(chckbxClearonnotify, gbc_chckbxClearonnotify);

		JCheckBox chckbxQname = new JCheckBox("Qname");
		chckbxQname.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				bindingsRender.showAsQName(chckbxQname.isSelected());

				bindingsDM.fireTableDataChanged();
				for (BindingsTableModel table : subscriptionResultsDM.values()) {
					table.fireTableDataChanged();
				}
			}
		});
		chckbxQname.setSelected(true);
		GridBagConstraints gbc_chckbxQname = new GridBagConstraints();
		gbc_chckbxQname.insets = new Insets(0, 0, 0, 5);
		gbc_chckbxQname.gridx = 3;
		gbc_chckbxQname.gridy = 0;
		infoPanel.add(chckbxQname, gbc_chckbxQname);

		chckbxAutoscroll = new JCheckBox("Autoscroll");
		GridBagConstraints gbc_chckbxAutoscroll = new GridBagConstraints();
		gbc_chckbxAutoscroll.anchor = GridBagConstraints.WEST;
		gbc_chckbxAutoscroll.insets = new Insets(0, 0, 0, 5);
		gbc_chckbxAutoscroll.gridx = 4;
		gbc_chckbxAutoscroll.gridy = 0;
		infoPanel.add(chckbxAutoscroll, gbc_chckbxAutoscroll);
		chckbxAutoscroll.setSelected(true);

		JButton btnClean = new JButton("Clear");
		btnClean.setForeground(UIManager.getColor("Button.light"));
		btnClean.setBackground(UIManager.getColor("Separator.shadow"));
		GridBagConstraints gbc_btnClean = new GridBagConstraints();
		gbc_btnClean.anchor = GridBagConstraints.NORTHWEST;
		gbc_btnClean.gridx = 5;
		gbc_btnClean.gridy = 0;
		infoPanel.add(btnClean, gbc_btnClean);
		btnClean.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clear();
			}
		});
		bindingsRender.setNamespaces(namespacesDM);
	}

	protected void clear() {
		if (sparqlTab.isShowing()) {
			bindingsDM.clear();
			lblInfo.setText("Results cleaned");
			lblInfo.setToolTipText("Results cleaned");
		} else {
			for (String spuid : subscriptionResultsTables.keySet()) {
				if (subscriptionResultsTables.get(spuid).isShowing()) {
					subscriptionResultsDM.get(spuid).clear();
					subscriptionResultsLabels.get(spuid).setText("Results cleaned");
				}
			}
		}
	}

	protected void subscribe() {
		Bindings bindings = new Bindings();
		for (int row = 0; row < queryForcedBindings.getRowCount(); row++) {
			String type = queryForcedBindings.getValueAt(row, 2).toString();
			String value = queryForcedBindings.getValueAt(row, 1).toString();
			String variable = queryForcedBindings.getValueAt(row, 0).toString();
			if (type.equals("xsd:anyURI"))
				bindings.addBinding(variable, new RDFTermURI(value));
			else
				bindings.addBinding(variable, new RDFTermLiteral(value, type));
		}
		try {
			Instant start = Instant.now();
			Response ret = sepaClient.subscribe(queryID, querySPARQL.getText(), bindings, defaultGraphURI.getText(),
					namedGraphURI.getText(), handler);
			Instant stop = Instant.now();
			if (ret.isError())
				lblInfo.setText(
						ret.toString() + String.format(" (%d ms)", (stop.toEpochMilli() - start.toEpochMilli())));
			else {
				SubscribeResponse results = (SubscribeResponse) ret;
				lblInfo.setText(
						String.format("Results: %d (%d ms). Notifications available in the <Active subscriptions> tab",
								results.getBindingsResults().size(), (stop.toEpochMilli() - start.toEpochMilli())));
				// Results table
				subscriptionResultsDM.put(results.getSpuid(), new BindingsTableModel());
				JTable bindingsResultsTable = new JTable(subscriptionResultsDM.get(results.getSpuid()));
				bindingsResultsTable.setDefaultRenderer(Object.class, bindingsRender);
				bindingsResultsTable.setAutoCreateRowSorter(true);
				bindingsResultsTable.registerKeyboardAction(new CopyAction(),
						KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
						JComponent.WHEN_FOCUSED);
				bindingsResultsTable.setCellSelectionEnabled(true);
				subscriptionResultsTables.put(results.getSpuid(), bindingsResultsTable);
				subscriptionResultsDM.get(results.getSpuid()).setAddedResults(results.getBindingsResults(),
						results.getSpuid());
				JScrollPane bindingsResults = new JScrollPane();
				bindingsResults.setViewportView(bindingsResultsTable);

				// Subscription panel
				JPanel sub = new JPanel();

				// Unsubscribe button
				JButton unsubscribeButton = new JButton(results.getSpuid());
				unsubscribeButton.setEnabled(true);
				unsubscribeButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						response = sepaClient.unsubscribe(results.getSpuid());

						subscriptions.remove(sub);
						subscriptionResultsDM.remove(results.getSpuid());
						subscriptionResultsLabels.remove(results.getSpuid());
						subscriptionResultsTables.remove(results.getSpuid());

					}
				});

				// Query label
				JLabel queryLabel = new JLabel(
						"<html>" + querySPARQL.getText() + " forced bindings: " + bindings.toString() + "</html>");
				queryLabel.setFont(new Font("Arial", Font.BOLD, 14));

				// Layout
				GridBagConstraints layoutFill = new GridBagConstraints();
				layoutFill.fill = GridBagConstraints.BOTH;
				sub.setLayout(new BoxLayout(sub, BoxLayout.Y_AXIS));
				sub.setName(queryList.getSelectedValue());

				JLabel info = new JLabel("Info");
				info.setText(
						String.format("Results: %d (%d ms)",
								results.getBindingsResults().size(), (stop.toEpochMilli() - start.toEpochMilli())));
				
				// Add components
				sub.add(queryLabel);
				sub.add(unsubscribeButton);
				sub.add(bindingsResults);
				sub.add(info);

				subscriptions.add(sub, layoutFill);
			}
		} catch (SEPAProtocolException | SEPASecurityException e) {
			lblInfo.setText(e.getMessage());
		}

	}

	protected void query() {
		Bindings bindings = new Bindings();
		for (int row = 0; row < queryForcedBindings.getRowCount(); row++) {
			String type = queryForcedBindings.getValueAt(row, 2).toString();
			String value = queryForcedBindings.getValueAt(row, 1).toString();
			String variable = queryForcedBindings.getValueAt(row, 0).toString();
			if (type.equals("xsd:anyURI"))
				bindings.addBinding(variable, new RDFTermURI(value));
			else
				bindings.addBinding(variable, new RDFTermLiteral(value, type));
		}

		try {
			Instant start = Instant.now();
			Response ret = sepaClient.query(queryID, querySPARQL.getText(), bindings, defaultGraphURI.getText(),
					namedGraphURI.getText(), sepaClient.getApplicationProfile().getQueryMethod(queryID),
					Integer.parseInt(queryTimeout.getText()));
			Instant stop = Instant.now();
			if (ret.isError())
				lblInfo.setText(
						ret.toString() + String.format(" (%d ms)", (stop.toEpochMilli() - start.toEpochMilli())));
			else {
				QueryResponse results = (QueryResponse) ret;
				lblInfo.setText(String.format("Results: %d (%d ms)", results.getBindingsResults().size(),
						(stop.toEpochMilli() - start.toEpochMilli())));
				bindingsDM.clear();
				bindingsDM.setAddedResults(results.getBindingsResults(), null);
			}
		} catch (NumberFormatException | SEPAProtocolException | SEPASecurityException | IOException e) {
			lblInfo.setText(e.getMessage());
		}
	}

	protected void update() {
		Bindings bindings = new Bindings();
		for (int row = 0; row < updateForcedBindings.getRowCount(); row++) {
			String type = updateForcedBindings.getValueAt(row, 2).toString();
			String value = updateForcedBindings.getValueAt(row, 1).toString();
			String variable = updateForcedBindings.getValueAt(row, 0).toString();
			if (type.equals("xsd:anyURI"))
				bindings.addBinding(variable, new RDFTermURI(value));
			else
				bindings.addBinding(variable, new RDFTermLiteral(value, type));
		}

		try {
			Instant start = Instant.now();
			Response ret = sepaClient.update(updateID, updateSPARQL.getText(), bindings, usingGraphURI.getText(),
					usingNamedGraphURI.getText(), sepaClient.getApplicationProfile().getUpdateMethod(updateID),
					Integer.parseInt(updateTimeout.getText()));
			Instant stop = Instant.now();
			if (ret.isError())
				lblInfo.setText(
						ret.toString() + String.format(" (%d ms)", (stop.toEpochMilli() - start.toEpochMilli())));
			else {
				lblInfo.setText(String.format("Update OK (%d ms)",
						(stop.toEpochMilli() - start.toEpochMilli())));
			}
		} catch (NumberFormatException | SEPAProtocolException | SEPASecurityException | IOException e) {
			lblInfo.setText(e.getMessage());
		}
	}

	protected void selectUpdateID(String id) {
		updateID = id;
		ApplicationProfile app = sepaClient.getApplicationProfile();
		updateSPARQL.setText(app.getSPARQLUpdate(id));

		Bindings bindings = app.getUpdateBindings(id);
		updateForcedBindingsDM.clearBindings();
		for (String variable : bindings.getVariables()) {
			if (bindings.isURI(variable))
				updateForcedBindingsDM.addBindings(variable, "xsd:anyURI");
			else
				updateForcedBindingsDM.addBindings(variable, bindings.getDatatype(variable));
		}

		String port = "";
		if (app.getUpdatePort(id) != -1)
			port = ":" + app.getUpdatePort(id);
		String url = app.getUpdateProtocolScheme(id) + "://" + app.getUpdateHost(id) + port + app.getUpdatePath(id);
		if (app.getUpdateMethod(id).equals(HTTPMethod.GET))
			updateURL.setText("GET " + url);
		else if (app.getUpdateMethod(id).equals(HTTPMethod.POST))
			updateURL.setText("POST " + url);
		else if (app.getUpdateMethod(id).equals(HTTPMethod.URL_ENCODED_POST))
			updateURL.setText("URL ENCODED POST " + url);

		usingGraphURI.setText(app.getUsingGraphURI(id));
		usingNamedGraphURI.setText(app.getUsingNamedGraphURI(id));

		enableUpdateButton();
	}

	private void selectQueryID(String id) {
		queryID = id;
		ApplicationProfile app = sepaClient.getApplicationProfile();
		querySPARQL.setText(app.getSPARQLQuery(id));

		Bindings bindings = app.getQueryBindings(id);
		subscribeForcedBindingsDM.clearBindings();
		for (String variable : bindings.getVariables()) {
			if (bindings.isURI(variable))
				subscribeForcedBindingsDM.addBindings(variable, "xsd:anyURI");
			else
				subscribeForcedBindingsDM.addBindings(variable, bindings.getDatatype(variable));
		}

		String port = "";
		if (app.getQueryPort(id) != -1)
			port = ":" + app.getQueryPort(id);
		String url = app.getQueryProtocolScheme(id) + "://" + app.getQueryHost(id) + port + app.getQueryPath(id);

		if (app.getQueryMethod(id).equals(HTTPMethod.GET))
			queryURL.setText("GET " + url);
		else if (app.getQueryMethod(id).equals(HTTPMethod.POST))
			queryURL.setText("POST " + url);
		else if (app.getQueryMethod(id).equals(HTTPMethod.URL_ENCODED_POST))
			queryURL.setText("URL ENCODED POST " + url);

		if (app.getSubscribeProtocol(id).equals(SubscriptionProtocol.WS))
			url = "ws://";
		else if (app.getSubscribeProtocol(id).equals(SubscriptionProtocol.WSS))
			url = "wss://";
		url += app.getSubscribeHost(id);
		if (app.getSubscribePort(id) != -1)
			url += ":" + app.getSubscribePort(id);
		url += app.getSubscribePath(id);
		subscribeURL.setText(url);

		defaultGraphURI.setText(app.getDefaultGraphURI(id));
		namedGraphURI.setText(app.getNamedGraphURI(id));

		enableQueryButton();
	}

	private boolean checkType(String value, String type) {
		try {
			switch (type) {
			case "xsd:anyURI":
				if (value.equals(""))
					return false;
				URI check = new URI(value);
				if (check.getScheme() == null)
					return false;
				break;
			case "xsd:base64Binary":
				Integer.parseInt(value, 16);
				break;
			case "xsd:boolean":
				if (!(value.equals("true") || value.equals("false") || value.equals("0") || value.equals("1")))
					return false;
				break;
			case "xsd:byte":
				Byte.parseByte(value);
				break;
			case "xsd:date":
			case "xsd:dateTime":
			case "xsd:time":
				DatatypeFactory.newInstance().newXMLGregorianCalendar(value);
				break;
			case "xsd:decimal":
				new java.math.BigDecimal(value);
				break;
			case "xsd:double":
				Double.parseDouble(value);
				break;
			case "xsd:float":
				Float.parseFloat(value);
				break;
			case "xsd:int":
				Integer.parseInt(value);
				break;
			case "xsd:integer":
				new java.math.BigInteger(value);
				break;
			case "xsd:long":
				Long.parseLong(value);
				break;
			case "xsd:short":
				Short.parseShort(value);
				break;
			case "xsd:QName":
				new javax.xml.namespace.QName(value);
				break;
			}
		} catch (Exception e) {
			return false;
		}

		return true;
	}

	private void enableUpdateButton() {
		updateButton.setEnabled(false);
		if (updateSPARQL.getText().equals(""))
			return;
		else {
			for (int row = 0; row < updateForcedBindings.getRowCount(); row++) {
				String type = updateForcedBindings.getValueAt(row, 2).toString();
				String value = updateForcedBindings.getValueAt(row, 1).toString();
				if (!checkType(value, type))
					return;
			}
		}
		updateButton.setEnabled(true);
	}

	private void enableQueryButton() {
		queryButton.setEnabled(false);
		subscribeButton.setEnabled(false);
		if (querySPARQL.getText().equals(""))
			return;
		else {
			for (int row = 0; row < queryForcedBindings.getRowCount(); row++) {
				String type = queryForcedBindings.getValueAt(row, 2).toString();
				String value = queryForcedBindings.getValueAt(row, 1).toString();
				if (!checkType(value, type))
					return;
			}
		}
		queryButton.setEnabled(true);
		subscribeButton.setEnabled(true);
	}
}
