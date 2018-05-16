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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ToolTipManager;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.JPanel;
import javax.swing.JList;
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
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.JTextArea;
import javax.swing.JSplitPane;

import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;
import it.unibo.arces.wot.sepa.pattern.GenericClient;
import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
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
import javax.swing.border.EtchedBorder;

public class Dashboard {
	private static final Logger logger = LogManager.getLogger("Dashboard");

	private static final String versionLabel = "SEPA Dashboard Ver 0.9.1";

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
		public void onBrokenSocket() {

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
	private JTable updateForcedBindings;
	private JTable subscribeForcedBindings;
	private JTable bindingsResultsTable;
	private JTable namespacesTable;

	private JLabel lblInfo;
	private JTextArea SPARQLUpdate;
	private JTextArea SPARQLSubscribe;

	private JButton btnUpdate;
	private JButton btnSubscribe;
	private JButton btnQuery;
	private JCheckBox chckbxAutoscroll;

	private JList<String> updatesList;
	private JList<String> subscribesList;

	ApplicationProfile appProfile;

	private JLabel labelHttpPort;

	private JLabel labelHttpsPort;

	private JLabel labelWsPort;

	private JLabel labelWssPort;

	private JLabel labelUpdatePath;

	private JLabel labelQueryPath;

	private JLabel labelSubscribePath;

	private JLabel labelUrl;

	private JLabel labelSecurePath;

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
		ArrayList<Boolean> rowTypes = new ArrayList<Boolean>();
		ArrayList<String> columns = new ArrayList<String>();

		public void clearBindings() {
			rowValues.clear();
			rowTypes.clear();

			super.fireTableDataChanged();
		}

		public void addBindings(String variable, boolean literal) {
			rowValues.add(new String[] { variable, "" });
			rowTypes.add(literal);

			super.fireTableDataChanged();
		}

		public ForcedBindingsTableModel() {
			columns.add("Variable");
			columns.add("Value");
			columns.add("Literal");
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
			if (columnIndex == 0 || columnIndex == 1)
				return String.class;
			return Boolean.class;
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
			if (columnIndex == 1) {
				String[] currentValue = rowValues.get(rowIndex);
				currentValue[1] = (String) aValue;
				rowValues.set(rowIndex, currentValue);
			}
			if (columnIndex == 2)
				rowTypes.set(rowIndex, (Boolean) aValue);
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

//			Date date = new Date();
//			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
//			String timestamp = sdf.format(date);

			ArrayList<String> vars = res.getAddedBindings().getVariables();
			for (String var : res.getRemovedBindings().getVariables()) {
				if (!vars.contains(var)) vars.add(var);
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
					//row.put("", new BindingValue(timestamp, false, true));
					rows.add(row);
				}
			}
			
			if (res.getRemovedBindings() != null) {
				for (Bindings sol : res.getRemovedBindings().getBindings()) {
					HashMap<String, BindingValue> row = new HashMap<String, BindingValue>();
					for (String var : sol.getVariables()) {
						row.put(var, new BindingValue(sol.getBindingValue(var), sol.isLiteral(var), false));
					}
					//row.put("", new BindingValue(timestamp, false, false));
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

			//Date date = new Date();
			//SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
			//String timestamp = sdf.format(date);

			ArrayList<String> vars = bindingsResults.getVariables();

			if (!columns.containsAll(vars) || columns.size() != vars.size()) {
				columns.clear();
				//vars.add("");
				columns.addAll(vars);
				super.fireTableStructureChanged();
			}

			for (Bindings sol : bindingsResults.getBindings()) {
				HashMap<String, BindingValue> row = new HashMap<String, BindingValue>();
				for (String var : sol.getVariables()) {
					row.put(var, new BindingValue(sol.getBindingValue(var), sol.isLiteral(var), true));
				}
				//row.put("", new BindingValue(timestamp, false, true));
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

		// Enable all the buttons
		btnUpdate.setEnabled(true);
		btnSubscribe.setEnabled(true);
		btnQuery.setEnabled(true);
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

		labelUrl.setText("---");

		labelHttpPort.setText("---");
		labelHttpsPort.setText("---");
		labelWsPort.setText("---");
		labelWssPort.setText("---");

		labelUpdatePath.setText("---");
		labelQueryPath.setText("---");
		labelSubscribePath.setText("---");
		labelSecurePath.setText("---");

		SPARQLSubscribe.setText("");
		SPARQLUpdate.setText("");
		namespacesDM.getDataVector().clear();
		updateListDM.clear();
		subscribeListDM.clear();
		updateForcedBindingsDM.clearBindings();
		subscribeForcedBindingsDM.clearBindings();

		updatesList.clearSelection();
		subscribesList.clearSelection();

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
		for (String subscribe : appProfile.getSubscribeIds()) {
			// subscribeListDM.addElement(subscribe);
			subscribeListDM.add(subscribe);
		}

		labelUrl.setText(appProfile.getHost());

		labelHttpPort.setText(String.format("%d", appProfile.getHttpPort()));
		labelHttpsPort.setText(String.format("%d", appProfile.getHttpsPort()));
		labelWsPort.setText(String.format("%d", appProfile.getWsPort()));
		labelWssPort.setText(String.format("%d", appProfile.getWssPort()));

		labelUpdatePath.setText(appProfile.getUpdatePath());
		labelQueryPath.setText(appProfile.getQueryPath());
		labelSubscribePath.setText(appProfile.getSubscribePath());
		labelSecurePath.setText(appProfile.getSecurePath());

		lblInfo.setText("JSAP loaded");
		lblInfo.setToolTipText("JSAP loaded");

		try {
			sepaClient = new GenericClient(appProfile, handler);
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
		frmSepaDashboard.setTitle(versionLabel);
		frmSepaDashboard.setBounds(100, 100, 925, 768);
		frmSepaDashboard.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 925, 0 };
		gridBagLayout.rowHeights = new int[] { 78, 651, 39, 0 };
		gridBagLayout.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 1.0, 0.0, Double.MIN_VALUE };
		frmSepaDashboard.getContentPane().setLayout(gridBagLayout);

		JPanel configuration = new JPanel();
		GridBagConstraints gbc_configuration = new GridBagConstraints();
		gbc_configuration.insets = new Insets(0, 0, 5, 0);
		gbc_configuration.fill = GridBagConstraints.BOTH;
		gbc_configuration.gridx = 0;
		gbc_configuration.gridy = 0;
		frmSepaDashboard.getContentPane().add(configuration, gbc_configuration);
		configuration
				.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "SPARQL 1.1 SE Protocol configuration", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		GridBagLayout gbl_configuration = new GridBagLayout();
		gbl_configuration.columnWidths = new int[] { 46, 45, 31, 20, 0, 24, 0, 0, 0, 0, 0, 0, 37, 0 };
		gbl_configuration.rowHeights = new int[] { 9, -25, 0 };
		gbl_configuration.columnWeights = new double[] { 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0,
				0.0, Double.MIN_VALUE };
		gbl_configuration.rowWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE };
		configuration.setLayout(gbl_configuration);

		JLabel label1 = new JLabel("URL:");
		label1.setFont(new Font("Lucida Grande", Font.BOLD, 13));
		GridBagConstraints gbc_label1 = new GridBagConstraints();
		gbc_label1.anchor = GridBagConstraints.EAST;
		gbc_label1.insets = new Insets(0, 0, 5, 5);
		gbc_label1.gridx = 0;
		gbc_label1.gridy = 0;
		configuration.add(label1, gbc_label1);

		JButton btnLoadXmlProfile = new JButton("Load JSAP");
		btnLoadXmlProfile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// String path = appProperties.getProperty("path");
				final JFileChooser fc = new JFileChooser(appProperties.getProperty("appProfile"));
				// final JFileChooser fc = new
				// JFileChooser("/Users/luca/Documents/SEPAProject/WOTDemo/tools/");
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

		labelUrl = new JLabel("---");
		GridBagConstraints gbc_labelUrl = new GridBagConstraints();
		gbc_labelUrl.anchor = GridBagConstraints.WEST;
		gbc_labelUrl.insets = new Insets(0, 0, 5, 5);
		gbc_labelUrl.gridx = 1;
		gbc_labelUrl.gridy = 0;
		configuration.add(labelUrl, gbc_labelUrl);

		JLabel label2 = new JLabel("http:");
		label2.setFont(new Font("Lucida Grande", Font.BOLD, 13));
		GridBagConstraints gbc_label2 = new GridBagConstraints();
		gbc_label2.insets = new Insets(0, 0, 5, 5);
		gbc_label2.gridx = 2;
		gbc_label2.gridy = 0;
		configuration.add(label2, gbc_label2);

		labelHttpPort = new JLabel("---");
		GridBagConstraints gbc_labelHttpPort = new GridBagConstraints();
		gbc_labelHttpPort.anchor = GridBagConstraints.WEST;
		gbc_labelHttpPort.insets = new Insets(0, 0, 5, 5);
		gbc_labelHttpPort.gridx = 3;
		gbc_labelHttpPort.gridy = 0;
		configuration.add(labelHttpPort, gbc_labelHttpPort);

		JLabel lblWs = new JLabel("ws:");
		lblWs.setFont(new Font("Lucida Grande", Font.BOLD, 13));
		GridBagConstraints gbc_lblWs = new GridBagConstraints();
		gbc_lblWs.insets = new Insets(0, 0, 5, 5);
		gbc_lblWs.gridx = 4;
		gbc_lblWs.gridy = 0;
		configuration.add(lblWs, gbc_lblWs);

		labelWsPort = new JLabel("---");
		GridBagConstraints gbc_labelWsPort = new GridBagConstraints();
		gbc_labelWsPort.anchor = GridBagConstraints.WEST;
		gbc_labelWsPort.insets = new Insets(0, 0, 5, 5);
		gbc_labelWsPort.gridx = 5;
		gbc_labelWsPort.gridy = 0;
		configuration.add(labelWsPort, gbc_labelWsPort);

		JLabel lblUpdate = new JLabel("update:");
		lblUpdate.setFont(new Font("Lucida Grande", Font.BOLD, 13));
		GridBagConstraints gbc_lblUpdate = new GridBagConstraints();
		gbc_lblUpdate.insets = new Insets(0, 0, 5, 5);
		gbc_lblUpdate.gridx = 6;
		gbc_lblUpdate.gridy = 0;
		configuration.add(lblUpdate, gbc_lblUpdate);

		labelUpdatePath = new JLabel("---");
		GridBagConstraints gbc_labelUpdatepath = new GridBagConstraints();
		gbc_labelUpdatepath.anchor = GridBagConstraints.WEST;
		gbc_labelUpdatepath.insets = new Insets(0, 0, 5, 5);
		gbc_labelUpdatepath.gridx = 7;
		gbc_labelUpdatepath.gridy = 0;
		configuration.add(labelUpdatePath, gbc_labelUpdatepath);

		JLabel lblQuery = new JLabel("query:");
		lblQuery.setFont(new Font("Lucida Grande", Font.BOLD, 13));
		GridBagConstraints gbc_lblQuery = new GridBagConstraints();
		gbc_lblQuery.insets = new Insets(0, 0, 5, 5);
		gbc_lblQuery.gridx = 8;
		gbc_lblQuery.gridy = 0;
		configuration.add(lblQuery, gbc_lblQuery);

		labelQueryPath = new JLabel("---");
		GridBagConstraints gbc_labelQueryPath = new GridBagConstraints();
		gbc_labelQueryPath.anchor = GridBagConstraints.WEST;
		gbc_labelQueryPath.insets = new Insets(0, 0, 5, 5);
		gbc_labelQueryPath.gridx = 9;
		gbc_labelQueryPath.gridy = 0;
		configuration.add(labelQueryPath, gbc_labelQueryPath);

		JLabel lblSubscribe = new JLabel("subscribe:");
		lblSubscribe.setFont(new Font("Lucida Grande", Font.BOLD, 13));
		GridBagConstraints gbc_lblSubscribe = new GridBagConstraints();
		gbc_lblSubscribe.insets = new Insets(0, 0, 5, 5);
		gbc_lblSubscribe.gridx = 10;
		gbc_lblSubscribe.gridy = 0;
		configuration.add(lblSubscribe, gbc_lblSubscribe);

		labelSubscribePath = new JLabel("---");
		GridBagConstraints gbc_labelSubscribePath = new GridBagConstraints();
		gbc_labelSubscribePath.anchor = GridBagConstraints.WEST;
		gbc_labelSubscribePath.insets = new Insets(0, 0, 5, 5);
		gbc_labelSubscribePath.gridx = 11;
		gbc_labelSubscribePath.gridy = 0;
		configuration.add(labelSubscribePath, gbc_labelSubscribePath);
		GridBagConstraints gbc_btnLoadXmlProfile = new GridBagConstraints();
		gbc_btnLoadXmlProfile.insets = new Insets(0, 0, 5, 0);
		gbc_btnLoadXmlProfile.anchor = GridBagConstraints.NORTHEAST;
		gbc_btnLoadXmlProfile.gridx = 12;
		gbc_btnLoadXmlProfile.gridy = 0;
		configuration.add(btnLoadXmlProfile, gbc_btnLoadXmlProfile);

		JLabel label3 = new JLabel("https:");
		label3.setFont(new Font("Lucida Grande", Font.BOLD, 13));
		GridBagConstraints gbc_label3 = new GridBagConstraints();
		gbc_label3.insets = new Insets(0, 0, 0, 5);
		gbc_label3.gridx = 2;
		gbc_label3.gridy = 1;
		configuration.add(label3, gbc_label3);

		labelHttpsPort = new JLabel("---");
		GridBagConstraints gbc_labelHttpsPort = new GridBagConstraints();
		gbc_labelHttpsPort.anchor = GridBagConstraints.WEST;
		gbc_labelHttpsPort.insets = new Insets(0, 0, 0, 5);
		gbc_labelHttpsPort.gridx = 3;
		gbc_labelHttpsPort.gridy = 1;
		configuration.add(labelHttpsPort, gbc_labelHttpsPort);

		JLabel lblWss = new JLabel("wss:");
		lblWss.setFont(new Font("Lucida Grande", Font.BOLD, 13));
		GridBagConstraints gbc_lblWss = new GridBagConstraints();
		gbc_lblWss.insets = new Insets(0, 0, 0, 5);
		gbc_lblWss.gridx = 4;
		gbc_lblWss.gridy = 1;
		configuration.add(lblWss, gbc_lblWss);

		labelWssPort = new JLabel("---");
		GridBagConstraints gbc_labelWssPort = new GridBagConstraints();
		gbc_labelWssPort.anchor = GridBagConstraints.WEST;
		gbc_labelWssPort.insets = new Insets(0, 0, 0, 5);
		gbc_labelWssPort.gridx = 5;
		gbc_labelWssPort.gridy = 1;
		configuration.add(labelWssPort, gbc_labelWssPort);

		JLabel lblNewLabel = new JLabel("secure:");
		lblNewLabel.setFont(new Font("Lucida Grande", Font.BOLD, 13));
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.insets = new Insets(0, 0, 0, 5);
		gbc_lblNewLabel.gridx = 6;
		gbc_lblNewLabel.gridy = 1;
		configuration.add(lblNewLabel, gbc_lblNewLabel);

		labelSecurePath = new JLabel("---");
		GridBagConstraints gbc_labelSecurePath = new GridBagConstraints();
		gbc_labelSecurePath.anchor = GridBagConstraints.WEST;
		gbc_labelSecurePath.insets = new Insets(0, 0, 0, 5);
		gbc_labelSecurePath.gridx = 7;
		gbc_labelSecurePath.gridy = 1;
		configuration.add(labelSecurePath, gbc_labelSecurePath);

		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		GridBagConstraints gbc_tabbedPane = new GridBagConstraints();
		gbc_tabbedPane.insets = new Insets(0, 0, 5, 0);
		gbc_tabbedPane.fill = GridBagConstraints.BOTH;
		gbc_tabbedPane.gridx = 0;
		gbc_tabbedPane.gridy = 1;
		frmSepaDashboard.getContentPane().add(tabbedPane, gbc_tabbedPane);

		JPanel primitives = new JPanel();
		tabbedPane.addTab("SPARQL 1.1 primitives", null, primitives, null);
		primitives.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "SPARQL 1.1 Updates and Queries", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		GridBagLayout gbl_primitives = new GridBagLayout();
		gbl_primitives.columnWidths = new int[] { 684, 0, 0 };
		gbl_primitives.rowHeights = new int[] { 114, 115, 0, 0, 0 };
		gbl_primitives.columnWeights = new double[] { 1.0, 1.0, Double.MIN_VALUE };
		gbl_primitives.rowWeights = new double[] { 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE };
		primitives.setLayout(gbl_primitives);

		JSplitPane splitPanel_Update = new JSplitPane();
		splitPanel_Update.setResizeWeight(0.5);
		GridBagConstraints gbc_splitPanel_Update = new GridBagConstraints();
		gbc_splitPanel_Update.insets = new Insets(0, 0, 5, 5);
		gbc_splitPanel_Update.fill = GridBagConstraints.BOTH;
		gbc_splitPanel_Update.gridx = 0;
		gbc_splitPanel_Update.gridy = 0;
		primitives.add(splitPanel_Update, gbc_splitPanel_Update);

		JPanel panel_4 = new JPanel();
		splitPanel_Update.setLeftComponent(panel_4);
		GridBagLayout gbl_panel_4 = new GridBagLayout();
		gbl_panel_4.columnWidths = new int[] { 66, 0 };
		gbl_panel_4.rowHeights = new int[] { 17, 75, 0 };
		gbl_panel_4.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_panel_4.rowWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		panel_4.setLayout(gbl_panel_4);

		JLabel lblUpdates = new JLabel("UPDATES");
		GridBagConstraints gbc_lblUpdates = new GridBagConstraints();
		gbc_lblUpdates.anchor = GridBagConstraints.NORTH;
		gbc_lblUpdates.insets = new Insets(0, 0, 5, 0);
		gbc_lblUpdates.gridx = 0;
		gbc_lblUpdates.gridy = 0;
		panel_4.add(lblUpdates, gbc_lblUpdates);
		lblUpdates.setFont(new Font("Lucida Grande", Font.BOLD, 14));

		JScrollPane scrollPane = new JScrollPane();
		GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.fill = GridBagConstraints.BOTH;
		gbc_scrollPane.gridx = 0;
		gbc_scrollPane.gridy = 1;
		panel_4.add(scrollPane, gbc_scrollPane);

		updatesList = new JList<String>(updateListDM);
		scrollPane.setViewportView(updatesList);
		updatesList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting() == false) {

					if (updatesList.getSelectedIndex() != -1) {
						String sparql = appProfile.update(updatesList.getSelectedValue());
						sparql = sparql.replaceFirst("\n", "");
						sparql = sparql.replaceAll("\t", "");
						sparql = sparql.trim();
						SPARQLUpdate.setText(sparql);

						Bindings bindings = appProfile.updateBindings(updatesList.getSelectedValue());
						updateForcedBindingsDM.clearBindings();
						if (bindings == null)
							return;
						for (String var : bindings.getVariables()) {
							updateForcedBindingsDM.addBindings(var, bindings.isLiteral(var));
						}

					}
				}
			}
		});
		updatesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		JPanel panel_5 = new JPanel();
		splitPanel_Update.setRightComponent(panel_5);
		GridBagLayout gbl_panel_5 = new GridBagLayout();
		gbl_panel_5.columnWidths = new int[] { 101, 0 };
		gbl_panel_5.rowHeights = new int[] { 16, 0, 0 };
		gbl_panel_5.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_panel_5.rowWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		panel_5.setLayout(gbl_panel_5);

		JLabel lblForcedBindings = new JLabel("Forced bindings");
		GridBagConstraints gbc_lblForcedBindings = new GridBagConstraints();
		gbc_lblForcedBindings.anchor = GridBagConstraints.NORTH;
		gbc_lblForcedBindings.insets = new Insets(0, 0, 5, 0);
		gbc_lblForcedBindings.gridx = 0;
		gbc_lblForcedBindings.gridy = 0;
		panel_5.add(lblForcedBindings, gbc_lblForcedBindings);

		JScrollPane scrollPane_2 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_2 = new GridBagConstraints();
		gbc_scrollPane_2.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_2.gridx = 0;
		gbc_scrollPane_2.gridy = 1;
		panel_5.add(scrollPane_2, gbc_scrollPane_2);

		updateForcedBindings = new JTable(updateForcedBindingsDM);
		scrollPane_2.setViewportView(updateForcedBindings);

		JSplitPane splitPanel_Subscribe = new JSplitPane();
		GridBagConstraints gbc_splitPanel_Subscribe = new GridBagConstraints();
		gbc_splitPanel_Subscribe.insets = new Insets(0, 0, 5, 0);
		gbc_splitPanel_Subscribe.fill = GridBagConstraints.BOTH;
		gbc_splitPanel_Subscribe.gridx = 1;
		gbc_splitPanel_Subscribe.gridy = 0;
		primitives.add(splitPanel_Subscribe, gbc_splitPanel_Subscribe);

		JPanel panel_6 = new JPanel();
		splitPanel_Subscribe.setLeftComponent(panel_6);
		GridBagLayout gbl_panel_6 = new GridBagLayout();
		gbl_panel_6.columnWidths = new int[] { 193, 0 };
		gbl_panel_6.rowHeights = new int[] { 17, 72, 0 };
		gbl_panel_6.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_panel_6.rowWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		panel_6.setLayout(gbl_panel_6);

		JLabel lblSubscribes = new JLabel("QUERIES");
		GridBagConstraints gbc_lblSubscribes = new GridBagConstraints();
		gbc_lblSubscribes.anchor = GridBagConstraints.NORTH;
		gbc_lblSubscribes.insets = new Insets(0, 0, 5, 0);
		gbc_lblSubscribes.gridx = 0;
		gbc_lblSubscribes.gridy = 0;
		panel_6.add(lblSubscribes, gbc_lblSubscribes);
		lblSubscribes.setFont(new Font("Lucida Grande", Font.BOLD, 14));

		JScrollPane scrollPane_3 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_3 = new GridBagConstraints();
		gbc_scrollPane_3.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_3.gridx = 0;
		gbc_scrollPane_3.gridy = 1;
		panel_6.add(scrollPane_3, gbc_scrollPane_3);

		subscribesList = new JList<String>(subscribeListDM);
		subscribesList.addListSelectionListener(new ListSelectionListener() {

			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting() == false) {

					if (subscribesList.getSelectedIndex() != -1) {
						String sparql = appProfile.subscribe(subscribesList.getSelectedValue());
						sparql = sparql.replaceFirst("\n", "");
						sparql = sparql.replaceAll("\t", "");
						sparql = sparql.trim();
						SPARQLSubscribe.setText(sparql);
					}

					Bindings bindings = appProfile.subscribeBindings(subscribesList.getSelectedValue());
					subscribeForcedBindingsDM.clearBindings();
					if (bindings == null)
						return;
					for (String var : bindings.getVariables()) {
						subscribeForcedBindingsDM.addBindings(var, bindings.isLiteral(var));
					}
				}
			}
		});
		scrollPane_3.setViewportView(subscribesList);
		subscribesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		JPanel panel_7 = new JPanel();
		splitPanel_Subscribe.setRightComponent(panel_7);
		GridBagLayout gbl_panel_7 = new GridBagLayout();
		gbl_panel_7.columnWidths = new int[] { 454, 0 };
		gbl_panel_7.rowHeights = new int[] { 16, 126, 0 };
		gbl_panel_7.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_panel_7.rowWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		panel_7.setLayout(gbl_panel_7);

		JLabel lblForcedBindings_1 = new JLabel("Forced bindings");
		GridBagConstraints gbc_lblForcedBindings_1 = new GridBagConstraints();
		gbc_lblForcedBindings_1.anchor = GridBagConstraints.NORTH;
		gbc_lblForcedBindings_1.insets = new Insets(0, 0, 5, 0);
		gbc_lblForcedBindings_1.gridx = 0;
		gbc_lblForcedBindings_1.gridy = 0;
		panel_7.add(lblForcedBindings_1, gbc_lblForcedBindings_1);

		JScrollPane scrollPane_1 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_1 = new GridBagConstraints();
		gbc_scrollPane_1.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_1.gridx = 0;
		gbc_scrollPane_1.gridy = 1;
		panel_7.add(scrollPane_1, gbc_scrollPane_1);

		subscribeForcedBindings = new JTable(subscribeForcedBindingsDM);
		scrollPane_1.setViewportView(subscribeForcedBindings);

		JScrollPane scrollPane_Update = new JScrollPane();
		GridBagConstraints gbc_scrollPane_Update = new GridBagConstraints();
		gbc_scrollPane_Update.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_Update.insets = new Insets(0, 0, 5, 5);
		gbc_scrollPane_Update.gridx = 0;
		gbc_scrollPane_Update.gridy = 1;
		primitives.add(scrollPane_Update, gbc_scrollPane_Update);

		SPARQLUpdate = new JTextArea();
		scrollPane_Update.setViewportView(SPARQLUpdate);
		SPARQLUpdate.setLineWrap(true);

		btnUpdate = new JButton("UPDATE");
		btnUpdate.setEnabled(false);
		btnUpdate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Bindings forced = new Bindings();
				for (int index = 0; index < updateForcedBindingsDM.getRowCount(); index++) {
					String value = (String) updateForcedBindingsDM.getValueAt(index, 1);
					String var = (String) updateForcedBindingsDM.getValueAt(index, 0);
					boolean literal = (boolean) updateForcedBindingsDM.getValueAt(index, 2);
					if (value.equals("")) {
						lblInfo.setText("Please specify binding value: " + var);
						lblInfo.setToolTipText("Please specify binding value: " + var);
						return;
					}

					if (literal)
						forced.addBinding(var, new RDFTermLiteral(value));
					else
						forced.addBinding(var, new RDFTermURI(value));
				}

				String update = SPARQLUpdate.getText().replaceAll("[\n\t]", "");

				long start = System.currentTimeMillis();
				Response result = sepaClient.update(update, forced);
				long stop = System.currentTimeMillis();

				String status = "DONE";
				if (result.isError()) {
					status = "FAILED " + ((ErrorResponse) result).getErrorMessage();
				}
				lblInfo.setText("UPDATE (" + (stop - start) + " ms): " + status);
				lblInfo.setToolTipText("UPDATE (" + (stop - start) + " ms): " + status);
			}
		});

		JScrollPane scrollPane_Subscribe = new JScrollPane();
		GridBagConstraints gbc_scrollPane_Subscribe = new GridBagConstraints();
		gbc_scrollPane_Subscribe.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_Subscribe.insets = new Insets(0, 0, 5, 0);
		gbc_scrollPane_Subscribe.gridx = 1;
		gbc_scrollPane_Subscribe.gridy = 1;
		primitives.add(scrollPane_Subscribe, gbc_scrollPane_Subscribe);

		SPARQLSubscribe = new JTextArea();
		scrollPane_Subscribe.setViewportView(SPARQLSubscribe);
		SPARQLSubscribe.setLineWrap(true);
		GridBagConstraints gbc_btnUpdate = new GridBagConstraints();
		gbc_btnUpdate.insets = new Insets(0, 0, 5, 5);
		gbc_btnUpdate.gridx = 0;
		gbc_btnUpdate.gridy = 2;
		primitives.add(btnUpdate, gbc_btnUpdate);

		JPanel panel = new JPanel();
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.insets = new Insets(0, 0, 5, 0);
		gbc_panel.fill = GridBagConstraints.BOTH;
		gbc_panel.gridx = 1;
		gbc_panel.gridy = 2;
		primitives.add(panel, gbc_panel);
		GridBagLayout gbl_panel = new GridBagLayout();
		gbl_panel.columnWidths = new int[] { 0, 0, 0 };
		gbl_panel.rowHeights = new int[] { 0, 0 };
		gbl_panel.columnWeights = new double[] { 1.0, 1.0, Double.MIN_VALUE };
		gbl_panel.rowWeights = new double[] { 1.0, Double.MIN_VALUE };
		panel.setLayout(gbl_panel);

		btnQuery = new JButton("QUERY");
		btnQuery.setEnabled(false);
		btnQuery.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Bindings forced = new Bindings();
				for (int index = 0; index < subscribeForcedBindings.getRowCount(); index++) {
					String value = (String) subscribeForcedBindings.getValueAt(index, 1);
					boolean literal = (boolean) subscribeForcedBindings.getValueAt(index, 2);
					String var = (String) subscribeForcedBindings.getValueAt(index, 0);

					if (value.equals("")) {
						lblInfo.setText("Please specify binding value: " + var);
						lblInfo.setToolTipText("Please specify binding value: " + var);
						return;
					}

					if (literal)
						forced.addBinding(var, new RDFTermLiteral(value));
					else
						forced.addBinding(var, new RDFTermURI(value));
				}

				String query = SPARQLSubscribe.getText().replaceAll("[\n\t]", "");

				lblInfo.setText("Running query...");
				long start = System.currentTimeMillis();
				response = sepaClient.query(query, forced);
				long stop = System.currentTimeMillis();

				String status = "DONE";
				if (response.isError()) {
					status = "FAILED " + ((ErrorResponse) response).getErrorMessage();
				} else {
					bindingsDM.clear();
					BindingsResults ret = ((QueryResponse) response).getBindingsResults();
					bindingsDM.setAddedResults(ret, null);
					status = " " + ret.size() + " bindings results";
				}

				lblInfo.setText("QUERY (" + (stop - start) + " ms) :" + status);
				lblInfo.setToolTipText("QUERY (" + (stop - start) + " ms) :" + status);
			}
		});
		GridBagConstraints gbc_btnQuery = new GridBagConstraints();
		gbc_btnQuery.insets = new Insets(0, 0, 0, 5);
		gbc_btnQuery.gridx = 0;
		gbc_btnQuery.gridy = 0;
		panel.add(btnQuery, gbc_btnQuery);

		btnSubscribe = new JButton("SUBSCRIBE");
		btnSubscribe.setEnabled(false);
		btnSubscribe.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (btnSubscribe.getText().equals("SUBSCRIBE")) {
					Bindings forced = new Bindings();
					for (int index = 0; index < subscribeForcedBindings.getRowCount(); index++) {
						String value = (String) subscribeForcedBindings.getValueAt(index, 1);
						boolean literal = (boolean) subscribeForcedBindings.getValueAt(index, 2);
						String var = (String) subscribeForcedBindings.getValueAt(index, 0);

						if (value.equals("")) {
							lblInfo.setText("Please specify binding value: " + var);
							lblInfo.setToolTipText("Please specify binding value: " + var);
							return;
						}
						;

						if (literal)
							forced.addBinding(var, new RDFTermLiteral(value));
						else
							forced.addBinding(var, new RDFTermURI(value));
					}

					String query = SPARQLSubscribe.getText().replaceAll("[\n\t]", "");

					response = sepaClient.subscribe(query, forced);

					if (response.getClass().equals(ErrorResponse.class)) {
						lblInfo.setText(response.toString());
						lblInfo.setToolTipText(response.toString());
						return;
					}

					// SPUID and results
					String spuid = ((SubscribeResponse) response).getSpuid();
					BindingsResults ret = ((SubscribeResponse) response).getBindingsResults();

					// Subscription panel
					JPanel sub = new JPanel();

					// Results label
					JLabel infoLabel = new JLabel();
					infoLabel.setText("Subscribed. First results: " + ret.size());
					subscriptionResultsLabels.put(spuid, infoLabel);

					// Results table
					subscriptionResultsDM.put(spuid, new BindingsTableModel());
					JTable bindingsResultsTable = new JTable(subscriptionResultsDM.get(spuid));
					bindingsResultsTable.setDefaultRenderer(Object.class, bindingsRender);
					bindingsResultsTable.setAutoCreateRowSorter(true);
					bindingsResultsTable.registerKeyboardAction(new CopyAction(),
							KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
							JComponent.WHEN_FOCUSED);
					bindingsResultsTable.setCellSelectionEnabled(true);
					subscriptionResultsTables.put(spuid, bindingsResultsTable);
					subscriptionResultsDM.get(spuid).setAddedResults(ret, spuid);
					JScrollPane bindingsResults = new JScrollPane();
					bindingsResults.setViewportView(bindingsResultsTable);

					// Unsubscribe button
					JButton unsubscribeButton = new JButton(spuid);
					unsubscribeButton.setEnabled(true);
					unsubscribeButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							response = sepaClient.unsubscribe(spuid);

							// if (response.isUnsubscribeResponse()) {
							subscriptions.remove(sub);
							subscriptionResultsDM.remove(spuid);
							subscriptionResultsLabels.remove(spuid);
							subscriptionResultsTables.remove(spuid);
							// }
						}
					});

					// Query label
					JLabel queryLabel = new JLabel(
							"<html>" + query + " forced bindings: " + forced.toString() + "</html>");
					queryLabel.setFont(new Font("Arial", Font.BOLD, 14));

					// Layout
					GridBagConstraints layoutFill = new GridBagConstraints();
					layoutFill.fill = GridBagConstraints.BOTH;
					sub.setLayout(new BoxLayout(sub, BoxLayout.Y_AXIS));
					sub.setName(subscribesList.getSelectedValue());

					// Add components
					sub.add(queryLabel);
					sub.add(unsubscribeButton);
					sub.add(bindingsResults);
					sub.add(infoLabel);

					subscriptions.add(sub, layoutFill);
				}
			}
		});
		GridBagConstraints gbc_btnSubscribe = new GridBagConstraints();
		gbc_btnSubscribe.gridx = 1;
		gbc_btnSubscribe.gridy = 0;
		panel.add(btnSubscribe, gbc_btnSubscribe);

		JScrollPane bindingsResults = new JScrollPane();
		GridBagConstraints gbc_bindingsResults = new GridBagConstraints();
		gbc_bindingsResults.fill = GridBagConstraints.BOTH;
		gbc_bindingsResults.gridwidth = 2;
		gbc_bindingsResults.gridx = 0;
		gbc_bindingsResults.gridy = 3;
		primitives.add(bindingsResults, gbc_bindingsResults);

		bindingsResultsTable = new JTable(bindingsDM);
		bindingsResults.setViewportView(bindingsResultsTable);
		bindingsResultsTable.setDefaultRenderer(Object.class, bindingsRender);
		bindingsResultsTable.setAutoCreateRowSorter(true);
		bindingsResultsTable.registerKeyboardAction(new CopyAction(),
				KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
				JComponent.WHEN_FOCUSED);
		bindingsResultsTable.setCellSelectionEnabled(true);

		subscriptions = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.addTab("Subscriptions", null, subscriptions, null);

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
		gbc_infoPanel.gridy = 2;
		frmSepaDashboard.getContentPane().add(infoPanel, gbc_infoPanel);
		GridBagLayout gbl_infoPanel = new GridBagLayout();
		gbl_infoPanel.columnWidths = new int[] { 73, 0, 0, 97, 76, 0 };
		gbl_infoPanel.rowHeights = new int[] { 29, 0 };
		gbl_infoPanel.columnWeights = new double[] { 1.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
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
		GridBagConstraints gbc_lblInfo = new GridBagConstraints();
		gbc_lblInfo.anchor = GridBagConstraints.WEST;
		gbc_lblInfo.insets = new Insets(0, 10, 0, 5);
		gbc_lblInfo.gridx = 0;
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
		gbc_chckbxClearonnotify.gridx = 1;
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
		gbc_chckbxQname.gridx = 2;
		gbc_chckbxQname.gridy = 0;
		infoPanel.add(chckbxQname, gbc_chckbxQname);

		chckbxAutoscroll = new JCheckBox("Autoscroll");
		GridBagConstraints gbc_chckbxAutoscroll = new GridBagConstraints();
		gbc_chckbxAutoscroll.anchor = GridBagConstraints.WEST;
		gbc_chckbxAutoscroll.insets = new Insets(0, 0, 0, 5);
		gbc_chckbxAutoscroll.gridx = 3;
		gbc_chckbxAutoscroll.gridy = 0;
		infoPanel.add(chckbxAutoscroll, gbc_chckbxAutoscroll);
		chckbxAutoscroll.setSelected(true);

		JButton btnClean = new JButton("Clear");
		GridBagConstraints gbc_btnClean = new GridBagConstraints();
		gbc_btnClean.anchor = GridBagConstraints.NORTHWEST;
		gbc_btnClean.gridx = 4;
		gbc_btnClean.gridy = 0;
		infoPanel.add(btnClean, gbc_btnClean);
		btnClean.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (primitives.isShowing()) {
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
		});
		bindingsRender.setNamespaces(namespacesDM);
	}
}
