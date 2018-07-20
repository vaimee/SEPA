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

import it.unibo.arces.wot.sepa.pattern.JSAP;
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
import it.unibo.arces.wot.sepa.commons.security.AuthenticationProperties;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermBNode;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;

import javax.swing.border.TitledBorder;
import java.awt.event.KeyEvent;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.border.LineBorder;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import java.awt.Panel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class Dashboard {
	private static final Logger logger = LogManager.getLogger();

	static Dashboard window;

	private static final String versionLabel = "SEPA Dashboard Ver 0.9.5";

	private GenericClient sepaClient;
	private DashboardHandler handler = new DashboardHandler();
	private JSAP appProfile;
	private Properties appProperties = new Properties();
	private AuthenticationProperties oauth = null;
	
	private DefaultTableModel namespacesDM;
	private String namespacesHeader[] = new String[] { "Prefix", "URI" };

	private BindingsTableModel bindingsDM = new BindingsTableModel();
	private BindingsRender bindingsRender = new BindingsRender();

	private ForcedBindingsTableModel updateForcedBindingsDM = new ForcedBindingsTableModel();
	private ForcedBindingsTableModel subscribeForcedBindingsDM = new ForcedBindingsTableModel();

	private SortedListModel updateListDM = new SortedListModel();
	private SortedListModel queryListDM = new SortedListModel();

	private HashMap<String, JTabbedPane> subscriptions = new HashMap<String, JTabbedPane>();
	private HashMap<String, BindingsTableModel> subscriptionResultsDM = new HashMap<String, BindingsTableModel>();
	private HashMap<String, JLabel> subscriptionResultsLabels = new HashMap<String, JLabel>();
	private HashMap<String, JTable> subscriptionResultsTables = new HashMap<String, JTable>();

	private JTextArea updateSPARQL;
	private JTextArea querySPARQL;

	private DefaultTableModel propertiesDM;
	private String propertiesHeader[] = new String[] { "Property", "Domain", "Range", "Comment" };

	private JFrame frmSepaDashboard;

	private Panel sparqlTab;

	private JTable namespacesTable;
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
	private JButton subscribeButton;

	private String updateID;
	private String queryID;

	private JList<String> queryList;
	private JList<String> updateList;
	private JTabbedPane mainTabs;
	private JTextField timeout;

	private JTextArea textArea;

	private String jksName = "sepa.jks";
	private String jksPass = "sepa2017";
	private String keyPass = "sepa2017";

	private JTextField userID;
	private JButton btnQuery;
	private JLabel updateInfo;
	private JLabel queryInfo;

	private JButton btnRegister;

	private SEPASecurityManager sm;

	private JTabbedPane subscriptionsPanel = new JTabbedPane(JTabbedPane.TOP);

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
			logger.error(errorResponse.getErrorMessage());
		}

		@Override
		public void onSubscribe(String spuid, String alias) {
			// Subscription panel
			JPanel sub = new JPanel();

			// Layout
			GridBagConstraints layoutFill = new GridBagConstraints();
			layoutFill.fill = GridBagConstraints.BOTH;
			sub.setLayout(new BoxLayout(sub, BoxLayout.Y_AXIS));
			sub.setName(queryList.getSelectedValue());

			// Query label
			JLabel queryLabel = new JLabel(
					"<html>" + querySPARQL.getText() + "</html>");
			queryLabel.setFont(new Font("Arial", Font.BOLD, 14));

			// Info label
			JLabel info = new JLabel("Info");
			info.setText(spuid);
			subscriptionResultsLabels.put(spuid, info);

			// Unsubscribe button
			JButton unsubscribeButton = new JButton(spuid);
			unsubscribeButton.setEnabled(true);
			unsubscribeButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						sepaClient.unsubscribe(spuid,Integer.parseInt(timeout.getText()));
					} catch (NumberFormatException | SEPASecurityException | SEPAPropertiesException
							| SEPAProtocolException e1) {
						logger.error(e1.getMessage());
					}
				}
			});

			// Results table
			subscriptionResultsDM.put(spuid, new BindingsTableModel());

			JTable bindingsResultsTable = new JTable(subscriptionResultsDM.get(spuid));
			JScrollPane bindingsResults = new JScrollPane();
			bindingsResults.setViewportView(bindingsResultsTable);

			bindingsResultsTable.setDefaultRenderer(Object.class, bindingsRender);
			bindingsResultsTable.setAutoCreateRowSorter(true);
			bindingsResultsTable.registerKeyboardAction(new CopyAction(),
					KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
					JComponent.WHEN_FOCUSED);
			bindingsResultsTable.setCellSelectionEnabled(true);

			subscriptionResultsTables.put(spuid, bindingsResultsTable);

			// Add all elements
			sub.add(queryLabel);
			sub.add(unsubscribeButton);
			sub.add(bindingsResults);
			sub.add(info);

			// Add tab
			subscriptionsPanel.add(sub, layoutFill);

			subscriptionsPanel.setSelectedIndex(subscriptionsPanel.getTabCount() - 1);
			mainTabs.setSelectedIndex(1);
			
		}

		@Override
		public void onUnsubscribe(String spuid) {
			subscriptionsPanel.remove(subscriptions.get(spuid));
			subscriptions.remove(spuid);
			subscriptionResultsDM.remove(spuid);
			subscriptionResultsLabels.remove(spuid);
			subscriptionResultsTables.remove(spuid);	
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

		public void addBindings(String variable, String literal, String value) {
			if (value != null)
				rowValues.add(new String[] { variable, value });
			else
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

			if (res.getRemovedBindings() != null) {
				for (Bindings sol : res.getRemovedBindings().getBindings()) {
					HashMap<String, BindingValue> row = new HashMap<String, BindingValue>();
					for (String var : sol.getVariables()) {
						row.put(var, new BindingValue(sol.getValue(var), sol.isLiteral(var), false));
					}
					rows.add(row);
				}
			}

			if (res.getAddedBindings() != null) {
				for (Bindings sol : res.getAddedBindings().getBindings()) {
					HashMap<String, BindingValue> row = new HashMap<String, BindingValue>();
					for (String var : sol.getVariables()) {
						row.put(var, new BindingValue(sol.getValue(var), sol.isLiteral(var), true));
					}
					rows.add(row);
				}
			}

			subscriptionResultsTables.get(spuid).changeSelection(subscriptionResultsTables.get(spuid).getRowCount() - 1,
					0, false, false);

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

			ArrayList<String> vars = bindingsResults.getVariables();

			if (!columns.containsAll(vars) || columns.size() != vars.size()) {
				columns.clear();
				columns.addAll(vars);
				super.fireTableStructureChanged();
			}

			for (Bindings sol : bindingsResults.getBindings()) {
				HashMap<String, BindingValue> row = new HashMap<String, BindingValue>();
				for (String var : sol.getVariables()) {
					row.put(var, new BindingValue(sol.getValue(var), sol.isLiteral(var), true));
				}
				rows.add(row);
			}

			subscriptionResultsTables.get(spuid).changeSelection(subscriptionResultsTables.get(spuid).getRowCount() - 1,
					0, false, false);

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
				logger.trace("Row: " + row + " Col: " + col + " Value: " + v + " Type: " + type);
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
					System.exit(-1);
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
			FileInputStream in = null;
			try {
				in = new FileInputStream("dashboard.properties");
			} catch (FileNotFoundException e) {
				logger.warn(e.getMessage());
				return false;
			}

			try {
				appProperties.load(in);
			} catch (IOException e) {
				logger.error(e.getMessage());
				return false;
			}

			// LOAD properties
			String path = appProperties.getProperty("appProfile");

			if (path == null) {
				logger.error("Path in dashboard.properties is null");
				return false;
			}
			file = path;
		}

		try {
			appProfile = new JSAP(file);
		} catch (SEPAPropertiesException | SEPASecurityException e) {
			logger.error(e.getMessage());
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
			updateListDM.add(update);
		}

		// Loading subscribes
		for (String subscribe : appProfile.getQueryIds()) {
			queryListDM.add(subscribe);
		}

		// Security
		if (appProfile.isSecure()) {
			try {
				oauth = new AuthenticationProperties(appProfile.getFileName());
			} catch (SEPAPropertiesException | SEPASecurityException e1) {
				logger.error(e1.getMessage());
				return false;
			}
			try {
				sm = new SEPASecurityManager(jksName, jksPass, keyPass,oauth);
				sepaClient = new GenericClient(appProfile, sm);
			} catch (SEPAProtocolException | SEPASecurityException e) {
				logger.error(e.getMessage());
				return false;
			}
			btnRegister.setEnabled(true);
			userID.setEnabled(true);
		}
		else {
			btnRegister.setEnabled(false);
			userID.setEnabled(false);
			
			try {
				sepaClient = new GenericClient(appProfile);
			} catch (SEPAProtocolException e) {
				logger.error(e.getMessage());
				return false;
			}
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
			if (f.isDirectory()) return true;
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
			private static final long serialVersionUID = 6788045463932990156L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		namespacesDM.setColumnIdentifiers(namespacesHeader);

		propertiesDM = new DefaultTableModel(0, 0) {
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
		gridBagLayout.rowHeights = new int[] { 500, 129, 39, 0 };
		gridBagLayout.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 1.0, 0.0, 0.0, Double.MIN_VALUE };
		frmSepaDashboard.getContentPane().setLayout(gridBagLayout);

		mainTabs = new JTabbedPane(JTabbedPane.TOP);
		GridBagConstraints gbc_mainTabs = new GridBagConstraints();
		gbc_mainTabs.insets = new Insets(0, 0, 5, 0);
		gbc_mainTabs.fill = GridBagConstraints.BOTH;
		gbc_mainTabs.gridx = 0;
		gbc_mainTabs.gridy = 0;
		frmSepaDashboard.getContentPane().add(mainTabs, gbc_mainTabs);

		sparqlTab = new Panel();
		mainTabs.addTab("SPARQL", null, sparqlTab, null);
		mainTabs.setEnabledAt(0, true);
		GridBagLayout gbl_sparqlTab = new GridBagLayout();
		gbl_sparqlTab.columnWidths = new int[] { 233, 0, 0 };
		gbl_sparqlTab.rowHeights = new int[] { 0, 155, 82, 47, 172, 0 };
		gbl_sparqlTab.columnWeights = new double[] { 1.0, 1.0, Double.MIN_VALUE };
		gbl_sparqlTab.rowWeights = new double[] { 0.0, 0.0, 1.0, 0.0, 1.0, Double.MIN_VALUE };
		sparqlTab.setLayout(gbl_sparqlTab);

		JPanel updateGraphs = new JPanel();
		updateGraphs.setBorder(new LineBorder(new Color(0, 0, 0), 1, true));
		GridBagConstraints gbc_updateGraphs = new GridBagConstraints();
		gbc_updateGraphs.insets = new Insets(0, 0, 5, 5);
		gbc_updateGraphs.fill = GridBagConstraints.BOTH;
		gbc_updateGraphs.gridx = 0;
		gbc_updateGraphs.gridy = 0;
		sparqlTab.add(updateGraphs, gbc_updateGraphs);
		GridBagLayout gbl_updateGraphs = new GridBagLayout();
		gbl_updateGraphs.columnWidths = new int[] { 0, 0, 0 };
		gbl_updateGraphs.rowHeights = new int[] { 0, 22, 0, 0, 0 };
		gbl_updateGraphs.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		gbl_updateGraphs.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		updateGraphs.setLayout(gbl_updateGraphs);

		updateURL = new JLabel("-");
		updateURL.setForeground(UIManager.getColor("ComboBox.buttonDarkShadow"));
		updateURL.setFont(new Font("Lucida Grande", Font.PLAIN, 13));
		GridBagConstraints gbc_updateURL = new GridBagConstraints();
		gbc_updateURL.anchor = GridBagConstraints.WEST;
		gbc_updateURL.gridwidth = 2;
		gbc_updateURL.insets = new Insets(0, 0, 5, 0);
		gbc_updateURL.gridx = 0;
		gbc_updateURL.gridy = 0;
		updateGraphs.add(updateURL, gbc_updateURL);

		JLabel label_1 = new JLabel("");
		GridBagConstraints gbc_label_1 = new GridBagConstraints();
		gbc_label_1.insets = new Insets(0, 0, 5, 5);
		gbc_label_1.gridx = 0;
		gbc_label_1.gridy = 1;
		updateGraphs.add(label_1, gbc_label_1);

		JLabel label_2 = new JLabel("using-graph-uri:");
		label_2.setFont(new Font("Lucida Grande", Font.BOLD, 13));
		label_2.setForeground(UIManager.getColor("ComboBox.buttonDarkShadow"));
		GridBagConstraints gbc_label_2 = new GridBagConstraints();
		gbc_label_2.anchor = GridBagConstraints.EAST;
		gbc_label_2.insets = new Insets(0, 0, 5, 5);
		gbc_label_2.gridx = 0;
		gbc_label_2.gridy = 2;
		updateGraphs.add(label_2, gbc_label_2);

		usingGraphURI = new JLabel("-");
		usingGraphURI.setForeground(UIManager.getColor("ComboBox.buttonDarkShadow"));
		usingGraphURI.setFont(new Font("Lucida Grande", Font.PLAIN, 13));
		GridBagConstraints gbc_updateUsingGraphURI = new GridBagConstraints();
		gbc_updateUsingGraphURI.anchor = GridBagConstraints.WEST;
		gbc_updateUsingGraphURI.insets = new Insets(0, 0, 5, 0);
		gbc_updateUsingGraphURI.gridx = 1;
		gbc_updateUsingGraphURI.gridy = 2;
		updateGraphs.add(usingGraphURI, gbc_updateUsingGraphURI);

		JLabel label_4 = new JLabel("using-named-graph-uri:");
		label_4.setFont(new Font("Lucida Grande", Font.BOLD, 13));
		label_4.setForeground(UIManager.getColor("ComboBox.buttonDarkShadow"));
		GridBagConstraints gbc_label_4 = new GridBagConstraints();
		gbc_label_4.anchor = GridBagConstraints.EAST;
		gbc_label_4.insets = new Insets(0, 0, 0, 5);
		gbc_label_4.gridx = 0;
		gbc_label_4.gridy = 3;
		updateGraphs.add(label_4, gbc_label_4);

		usingNamedGraphURI = new JLabel("-");
		usingNamedGraphURI.setForeground(UIManager.getColor("ComboBox.buttonDarkShadow"));
		usingNamedGraphURI.setFont(new Font("Lucida Grande", Font.PLAIN, 13));
		GridBagConstraints gbc_updateUsingNamedGraphURI = new GridBagConstraints();
		gbc_updateUsingNamedGraphURI.anchor = GridBagConstraints.WEST;
		gbc_updateUsingNamedGraphURI.gridx = 1;
		gbc_updateUsingNamedGraphURI.gridy = 3;
		updateGraphs.add(usingNamedGraphURI, gbc_updateUsingNamedGraphURI);

		JPanel queryGraphs = new JPanel();
		queryGraphs.setBorder(new LineBorder(new Color(0, 0, 0), 1, true));
		GridBagConstraints gbc_queryGraphs = new GridBagConstraints();
		gbc_queryGraphs.insets = new Insets(0, 0, 5, 0);
		gbc_queryGraphs.fill = GridBagConstraints.BOTH;
		gbc_queryGraphs.gridx = 1;
		gbc_queryGraphs.gridy = 0;
		sparqlTab.add(queryGraphs, gbc_queryGraphs);
		GridBagLayout gbl_queryGraphs = new GridBagLayout();
		gbl_queryGraphs.columnWidths = new int[] { 0, 0, 0 };
		gbl_queryGraphs.rowHeights = new int[] { 0, 0, 0, 0, 0 };
		gbl_queryGraphs.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		gbl_queryGraphs.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		queryGraphs.setLayout(gbl_queryGraphs);

		queryURL = new JLabel("-");
		queryURL.setForeground(UIManager.getColor("ComboBox.buttonDarkShadow"));
		queryURL.setFont(new Font("Lucida Grande", Font.PLAIN, 13));
		GridBagConstraints gbc_queryURL = new GridBagConstraints();
		gbc_queryURL.anchor = GridBagConstraints.WEST;
		gbc_queryURL.gridwidth = 2;
		gbc_queryURL.insets = new Insets(0, 0, 5, 0);
		gbc_queryURL.gridx = 0;
		gbc_queryURL.gridy = 0;
		queryGraphs.add(queryURL, gbc_queryURL);

		subscribeURL = new JLabel("-");
		subscribeURL.setForeground(UIManager.getColor("ComboBox.buttonDarkShadow"));
		subscribeURL.setFont(new Font("Lucida Grande", Font.PLAIN, 13));
		GridBagConstraints gbc_subscribeURL = new GridBagConstraints();
		gbc_subscribeURL.anchor = GridBagConstraints.WEST;
		gbc_subscribeURL.gridwidth = 2;
		gbc_subscribeURL.insets = new Insets(0, 0, 5, 0);
		gbc_subscribeURL.gridx = 0;
		gbc_subscribeURL.gridy = 1;
		queryGraphs.add(subscribeURL, gbc_subscribeURL);

		JLabel label_8 = new JLabel("default-graph-uri:");
		label_8.setFont(new Font("Lucida Grande", Font.BOLD, 13));
		label_8.setForeground(UIManager.getColor("ComboBox.buttonDarkShadow"));
		GridBagConstraints gbc_label_8 = new GridBagConstraints();
		gbc_label_8.anchor = GridBagConstraints.EAST;
		gbc_label_8.insets = new Insets(0, 0, 5, 5);
		gbc_label_8.gridx = 0;
		gbc_label_8.gridy = 2;
		queryGraphs.add(label_8, gbc_label_8);

		defaultGraphURI = new JLabel("-");
		defaultGraphURI.setForeground(UIManager.getColor("ComboBox.buttonDarkShadow"));
		defaultGraphURI.setFont(new Font("Lucida Grande", Font.PLAIN, 13));
		GridBagConstraints gbc_defaultGraphURI = new GridBagConstraints();
		gbc_defaultGraphURI.anchor = GridBagConstraints.WEST;
		gbc_defaultGraphURI.insets = new Insets(0, 0, 5, 0);
		gbc_defaultGraphURI.gridx = 1;
		gbc_defaultGraphURI.gridy = 2;
		queryGraphs.add(defaultGraphURI, gbc_defaultGraphURI);

		JLabel label_10 = new JLabel("named-graph-uri:");
		label_10.setFont(new Font("Lucida Grande", Font.BOLD, 13));
		label_10.setForeground(UIManager.getColor("ComboBox.buttonDarkShadow"));
		GridBagConstraints gbc_label_10 = new GridBagConstraints();
		gbc_label_10.anchor = GridBagConstraints.EAST;
		gbc_label_10.insets = new Insets(0, 0, 0, 5);
		gbc_label_10.gridx = 0;
		gbc_label_10.gridy = 3;
		queryGraphs.add(label_10, gbc_label_10);

		namedGraphURI = new JLabel("-");
		namedGraphURI.setForeground(UIManager.getColor("ComboBox.buttonDarkShadow"));
		namedGraphURI.setFont(new Font("Lucida Grande", Font.PLAIN, 13));
		GridBagConstraints gbc_namedGraphURI = new GridBagConstraints();
		gbc_namedGraphURI.anchor = GridBagConstraints.WEST;
		gbc_namedGraphURI.gridx = 1;
		gbc_namedGraphURI.gridy = 3;
		queryGraphs.add(namedGraphURI, gbc_namedGraphURI);

		JSplitPane updates = new JSplitPane();
		updates.setResizeWeight(0.5);
		GridBagConstraints gbc_updates = new GridBagConstraints();
		gbc_updates.insets = new Insets(0, 0, 5, 5);
		gbc_updates.fill = GridBagConstraints.BOTH;
		gbc_updates.gridx = 0;
		gbc_updates.gridy = 1;
		sparqlTab.add(updates, gbc_updates);

		JPanel panel_2 = new JPanel();
		updates.setLeftComponent(panel_2);
		GridBagLayout gbl_panel_2 = new GridBagLayout();
		gbl_panel_2.columnWidths = new int[] { 66, 0 };
		gbl_panel_2.rowHeights = new int[] { 17, 75, 0 };
		gbl_panel_2.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_panel_2.rowWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		panel_2.setLayout(gbl_panel_2);

		JLabel label_12 = new JLabel("UPDATES");
		label_12.setForeground(Color.BLACK);
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

		updateList = new JList<String>();
		updateList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				selectUpdateID(updateList.getSelectedValue());
			}
		});
		updateList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		updateList.setModel(updateListDM);
		scrollPane.setViewportView(updateList);

		JPanel panel_3 = new JPanel();
		updates.setRightComponent(panel_3);
		GridBagLayout gbl_panel_3 = new GridBagLayout();
		gbl_panel_3.columnWidths = new int[] { 101, 0 };
		gbl_panel_3.rowHeights = new int[] { 16, 0, 0 };
		gbl_panel_3.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_panel_3.rowWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		panel_3.setLayout(gbl_panel_3);

		JLabel lblForcedBindings = new JLabel("FORCED BINDINGS");
		lblForcedBindings.setForeground(Color.BLACK);
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

		JSplitPane queries = new JSplitPane();
		GridBagConstraints gbc_queries = new GridBagConstraints();
		gbc_queries.insets = new Insets(0, 0, 5, 0);
		gbc_queries.fill = GridBagConstraints.BOTH;
		gbc_queries.gridx = 1;
		gbc_queries.gridy = 1;
		sparqlTab.add(queries, gbc_queries);

		JPanel panel_4 = new JPanel();
		queries.setLeftComponent(panel_4);
		GridBagLayout gbl_panel_4 = new GridBagLayout();
		gbl_panel_4.columnWidths = new int[] { 193, 0 };
		gbl_panel_4.rowHeights = new int[] { 17, 72, 0 };
		gbl_panel_4.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_panel_4.rowWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		panel_4.setLayout(gbl_panel_4);

		JLabel label_14 = new JLabel("QUERIES");
		label_14.setForeground(Color.BLACK);
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
		queryList.setModel(queryListDM);
		scrollPane_1.setViewportView(queryList);

		JPanel panel_5 = new JPanel();
		queries.setRightComponent(panel_5);
		GridBagLayout gbl_panel_5 = new GridBagLayout();
		gbl_panel_5.columnWidths = new int[] { 123, 0 };
		gbl_panel_5.rowHeights = new int[] { 16, 126, 0 };
		gbl_panel_5.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_panel_5.rowWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		panel_5.setLayout(gbl_panel_5);

		JLabel lblForcedBindings_1 = new JLabel("FORCED BINDINGS");
		lblForcedBindings_1.setForeground(Color.BLACK);
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

		JScrollPane update = new JScrollPane();
		GridBagConstraints gbc_update = new GridBagConstraints();
		gbc_update.fill = GridBagConstraints.BOTH;
		gbc_update.insets = new Insets(0, 0, 5, 5);
		gbc_update.gridx = 0;
		gbc_update.gridy = 2;
		sparqlTab.add(update, gbc_update);

		updateSPARQL = new JTextArea();
		update.setViewportView(updateSPARQL);
		updateSPARQL.setLineWrap(true);

		JScrollPane query = new JScrollPane();
		GridBagConstraints gbc_query = new GridBagConstraints();
		gbc_query.insets = new Insets(0, 0, 5, 0);
		gbc_query.fill = GridBagConstraints.BOTH;
		gbc_query.gridx = 1;
		gbc_query.gridy = 2;
		sparqlTab.add(query, gbc_query);

		querySPARQL = new JTextArea();
		querySPARQL.setLineWrap(true);
		query.setViewportView(querySPARQL);

		JPanel panel_6 = new JPanel();
		GridBagConstraints gbc_panel_6 = new GridBagConstraints();
		gbc_panel_6.insets = new Insets(0, 0, 5, 5);
		gbc_panel_6.fill = GridBagConstraints.BOTH;
		gbc_panel_6.gridx = 0;
		gbc_panel_6.gridy = 3;
		sparqlTab.add(panel_6, gbc_panel_6);
		GridBagLayout gbl_panel_6 = new GridBagLayout();
		gbl_panel_6.columnWidths = new int[] { 0, 120, 57, 0, 0 };
		gbl_panel_6.rowHeights = new int[] { 28, 0 };
		gbl_panel_6.columnWeights = new double[] { 0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE };
		gbl_panel_6.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
		panel_6.setLayout(gbl_panel_6);

		updateButton = new JButton("UPDATE");
		GridBagConstraints gbc_updateButton = new GridBagConstraints();
		gbc_updateButton.anchor = GridBagConstraints.WEST;
		gbc_updateButton.insets = new Insets(0, 0, 0, 5);
		gbc_updateButton.gridx = 0;
		gbc_updateButton.gridy = 0;
		panel_6.add(updateButton, gbc_updateButton);
		updateButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					update();
				} catch (SEPAPropertiesException e1) {
					logger.error(e1.getMessage());
				}
			}
		});
		updateButton.setForeground(Color.BLACK);
		updateButton.setEnabled(false);

		updateInfo = new JLabel("---");
		GridBagConstraints gbc_udpdateInfo = new GridBagConstraints();
		gbc_udpdateInfo.fill = GridBagConstraints.VERTICAL;
		gbc_udpdateInfo.insets = new Insets(0, 0, 0, 5);
		gbc_udpdateInfo.anchor = GridBagConstraints.WEST;
		gbc_udpdateInfo.gridx = 1;
		gbc_udpdateInfo.gridy = 0;
		panel_6.add(updateInfo, gbc_udpdateInfo);

		timeout = new JTextField();
		GridBagConstraints gbc_updateTimeout = new GridBagConstraints();
		gbc_updateTimeout.fill = GridBagConstraints.HORIZONTAL;
		gbc_updateTimeout.insets = new Insets(0, 0, 0, 5);
		gbc_updateTimeout.gridx = 2;
		gbc_updateTimeout.gridy = 0;
		panel_6.add(timeout, gbc_updateTimeout);
		timeout.setText("5000");
		timeout.setColumns(10);

		JLabel lblToms = new JLabel("Timeout (ms)");
		GridBagConstraints gbc_lblToms = new GridBagConstraints();
		gbc_lblToms.anchor = GridBagConstraints.EAST;
		gbc_lblToms.gridx = 3;
		gbc_lblToms.gridy = 0;
		panel_6.add(lblToms, gbc_lblToms);
		lblToms.setForeground(Color.BLACK);

		JPanel panel_7 = new JPanel();
		GridBagConstraints gbc_panel_7 = new GridBagConstraints();
		gbc_panel_7.insets = new Insets(0, 0, 5, 0);
		gbc_panel_7.fill = GridBagConstraints.BOTH;
		gbc_panel_7.gridx = 1;
		gbc_panel_7.gridy = 3;
		sparqlTab.add(panel_7, gbc_panel_7);
		GridBagLayout gbl_panel_7 = new GridBagLayout();
		gbl_panel_7.columnWidths = new int[] { 0, 0, 67, 0 };
		gbl_panel_7.rowHeights = new int[] { 0, 0 };
		gbl_panel_7.columnWeights = new double[] { 0.0, 1.0, 0.0, Double.MIN_VALUE };
		gbl_panel_7.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
		panel_7.setLayout(gbl_panel_7);

		btnQuery = new JButton("QUERY");
		btnQuery.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					query();
				} catch (SEPAPropertiesException e1) {
					logger.error(e1.getMessage());
				}
			}
		});
		GridBagConstraints gbc_btnQuery = new GridBagConstraints();
		gbc_btnQuery.anchor = GridBagConstraints.WEST;
		gbc_btnQuery.insets = new Insets(0, 0, 0, 5);
		gbc_btnQuery.gridx = 0;
		gbc_btnQuery.gridy = 0;
		panel_7.add(btnQuery, gbc_btnQuery);

		queryInfo = new JLabel("---");
		GridBagConstraints gbc_queryInfo = new GridBagConstraints();
		gbc_queryInfo.fill = GridBagConstraints.VERTICAL;
		gbc_queryInfo.insets = new Insets(0, 0, 0, 5);
		gbc_queryInfo.anchor = GridBagConstraints.WEST;
		gbc_queryInfo.gridx = 1;
		gbc_queryInfo.gridy = 0;
		panel_7.add(queryInfo, gbc_queryInfo);

		subscribeButton = new JButton("SUBSCRIBE");
		GridBagConstraints gbc_subscribeButton = new GridBagConstraints();
		gbc_subscribeButton.anchor = GridBagConstraints.WEST;
		gbc_subscribeButton.gridx = 2;
		gbc_subscribeButton.gridy = 0;
		panel_7.add(subscribeButton, gbc_subscribeButton);
		subscribeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					subscribe();
				} catch (IOException | SEPAPropertiesException | NumberFormatException | SEPAProtocolException | SEPASecurityException e1) {
					logger.error(e1.getMessage());
				}
			}
		});
		subscribeButton.setForeground(Color.BLACK);
		subscribeButton.setEnabled(false);

		JScrollPane results = new JScrollPane();
		GridBagConstraints gbc_results = new GridBagConstraints();
		gbc_results.fill = GridBagConstraints.BOTH;
		gbc_results.gridwidth = 2;
		gbc_results.gridx = 0;
		gbc_results.gridy = 4;
		sparqlTab.add(results, gbc_results);

		bindingsResultsTable = new JTable(bindingsDM);
		results.setViewportView(bindingsResultsTable);
		bindingsResultsTable.setBorder(UIManager.getBorder("Button.border"));
		bindingsResultsTable.setFillsViewportHeight(true);
		bindingsResultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		bindingsResultsTable.setDefaultRenderer(BindingValue.class, bindingsRender);
		bindingsResultsTable.registerKeyboardAction(new CopyAction(),
				KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
				JComponent.WHEN_FOCUSED);
		bindingsResultsTable.setCellSelectionEnabled(true);
		bindingsRender.setNamespaces(namespacesDM);

		mainTabs.addTab("Active subscriptions", null, subscriptionsPanel, null);

		JPanel namespaces = new JPanel();
		mainTabs.addTab("Namespaces", null, namespaces, null);
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

		JScrollPane scrollPane_5 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_5 = new GridBagConstraints();
		gbc_scrollPane_5.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_5.insets = new Insets(0, 0, 5, 0);
		gbc_scrollPane_5.gridx = 0;
		gbc_scrollPane_5.gridy = 1;
		frmSepaDashboard.getContentPane().add(scrollPane_5, gbc_scrollPane_5);

		textArea = new JTextArea();
		textArea.setLineWrap(true);
		scrollPane_5.setViewportView(textArea);
		TextAreaAppender appender = new TextAreaAppender(textArea);
		TextAreaAppender.addAppender(appender, "TextArea");

		JPanel infoPanel = new JPanel();
		GridBagConstraints gbc_infoPanel = new GridBagConstraints();
		gbc_infoPanel.anchor = GridBagConstraints.SOUTH;
		gbc_infoPanel.fill = GridBagConstraints.HORIZONTAL;
		gbc_infoPanel.gridx = 0;
		gbc_infoPanel.gridy = 2;
		frmSepaDashboard.getContentPane().add(infoPanel, gbc_infoPanel);
		GridBagLayout gbl_infoPanel = new GridBagLayout();
		gbl_infoPanel.columnWidths = new int[] { 104, 88, 0, 0, 0, 97, 76, 0 };
		gbl_infoPanel.rowHeights = new int[] { 29, 0 };
		gbl_infoPanel.columnWeights = new double[] { 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		gbl_infoPanel.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
		infoPanel.setLayout(gbl_infoPanel);

		JButton btnLoadXmlProfile = new JButton("Load JSAP");
		btnLoadXmlProfile.setForeground(Color.BLACK);
		btnLoadXmlProfile.setBackground(Color.WHITE);
		GridBagConstraints gbc_btnLoadXmlProfile = new GridBagConstraints();
		gbc_btnLoadXmlProfile.anchor = GridBagConstraints.WEST;
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
						appProperties.put("jksName", jksName);
						appProperties.put("jksPass", jksPass);
						appProperties.put("keyPass", keyPass);

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
		ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);

		btnRegister = new JButton("REGISTER");
		btnRegister.setEnabled(false);
		btnRegister.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					sm.register(userID.getText());
				} catch (SEPASecurityException | SEPAPropertiesException e1) {
					logger.error(e1.getMessage());
				}
			}
		});
		GridBagConstraints gbc_btnRegister = new GridBagConstraints();
		gbc_btnRegister.insets = new Insets(0, 0, 0, 5);
		gbc_btnRegister.gridx = 1;
		gbc_btnRegister.gridy = 0;
		infoPanel.add(btnRegister, gbc_btnRegister);

		userID = new JTextField();
		userID.setEnabled(false);
		userID.setText("SEPATest");
		GridBagConstraints gbc_userID = new GridBagConstraints();
		gbc_userID.insets = new Insets(0, 0, 0, 5);
		gbc_userID.fill = GridBagConstraints.HORIZONTAL;
		gbc_userID.gridx = 2;
		gbc_userID.gridy = 0;
		infoPanel.add(userID, gbc_userID);
		userID.setColumns(10);

		JCheckBox chckbxQname = new JCheckBox("Qname");
		GridBagConstraints gbc_chckbxQname = new GridBagConstraints();
		gbc_chckbxQname.insets = new Insets(0, 0, 0, 5);
		gbc_chckbxQname.gridx = 4;
		gbc_chckbxQname.gridy = 0;
		infoPanel.add(chckbxQname, gbc_chckbxQname);
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

		JButton btnNewButton = new JButton("Clear results");
		GridBagConstraints gbc_btnNewButton = new GridBagConstraints();
		gbc_btnNewButton.insets = new Insets(0, 0, 0, 5);
		gbc_btnNewButton.gridx = 5;
		gbc_btnNewButton.gridy = 0;
		infoPanel.add(btnNewButton, gbc_btnNewButton);
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clear();
			}
		});

		JButton btnClean = new JButton("Clear log");
		btnClean.setForeground(Color.BLACK);
		btnClean.setBackground(UIManager.getColor("Separator.shadow"));
		GridBagConstraints gbc_btnClean = new GridBagConstraints();
		gbc_btnClean.anchor = GridBagConstraints.NORTHWEST;
		gbc_btnClean.gridx = 6;
		gbc_btnClean.gridy = 0;
		infoPanel.add(btnClean, gbc_btnClean);
		btnClean.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textArea.setText("");
				// clear();
			}
		});
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

	protected void subscribe() throws IOException, SEPAPropertiesException, NumberFormatException, SEPAProtocolException, SEPASecurityException {
		Bindings bindings = new Bindings();
		for (int row = 0; row < queryForcedBindings.getRowCount(); row++) {
			String type = queryForcedBindings.getValueAt(row, 2).toString();
			String value = queryForcedBindings.getValueAt(row, 1).toString();
			String variable = queryForcedBindings.getValueAt(row, 0).toString();
			if (type.toUpperCase().equals("URI"))
				bindings.addBinding(variable, new RDFTermURI(value));
			else if (type.toUpperCase().equals("BNODE"))
				bindings.addBinding(variable, new RDFTermBNode(value));
			else
				bindings.addBinding(variable, new RDFTermLiteral(value, type));
		}

		sepaClient.subscribe(queryID, querySPARQL.getText(), bindings, handler,Integer.parseInt(timeout.getText()));
	}

	protected void query() throws SEPAPropertiesException {
		Bindings bindings = new Bindings();
		for (int row = 0; row < queryForcedBindings.getRowCount(); row++) {
			String type = queryForcedBindings.getValueAt(row, 2).toString();
			String value = queryForcedBindings.getValueAt(row, 1).toString();
			String variable = queryForcedBindings.getValueAt(row, 0).toString();
			if (type.toUpperCase().equals("URI"))
				bindings.addBinding(variable, new RDFTermURI(value));
			else if (type.toUpperCase().equals("BNODE"))
				bindings.addBinding(variable, new RDFTermBNode(value));
			else
				bindings.addBinding(variable, new RDFTermLiteral(value, type));
		}

		try {
			Instant start = Instant.now();
			Response ret = sepaClient.query(queryID, querySPARQL.getText(), bindings,
					Integer.parseInt(timeout.getText()));
			Instant stop = Instant.now();
			if (ret.isError()) {
				logger.error(ret.toString() + String.format(" (%d ms)", (stop.toEpochMilli() - start.toEpochMilli())));
				queryInfo.setText("Error: " + ((ErrorResponse) ret).getErrorCode());
			} else {
				QueryResponse results = (QueryResponse) ret;
				logger.info(String.format("Results: %d (%d ms)", results.getBindingsResults().size(),
						(stop.toEpochMilli() - start.toEpochMilli())));
				queryInfo.setText(String.format("Results: %d (%d ms)", results.getBindingsResults().size(),
						(stop.toEpochMilli() - start.toEpochMilli())));
				bindingsDM.clear();
				bindingsDM.setAddedResults(results.getBindingsResults(), null);
			}
		} catch (NumberFormatException | SEPAProtocolException | SEPASecurityException | IOException e) {
			logger.error(e.getMessage());
		}
	}

	protected void update() throws SEPAPropertiesException {
		Bindings bindings = new Bindings();
		for (int row = 0; row < updateForcedBindings.getRowCount(); row++) {
			String type = updateForcedBindings.getValueAt(row, 2).toString();
			String value = updateForcedBindings.getValueAt(row, 1).toString();
			String variable = updateForcedBindings.getValueAt(row, 0).toString();
			if (type.equals("URI"))
				bindings.addBinding(variable, new RDFTermURI(value));
			else if (type.equals("BNODE"))
				bindings.addBinding(variable, new RDFTermBNode(value));
			else
				bindings.addBinding(variable, new RDFTermLiteral(value, type));
		}

		try {
			Instant start = Instant.now();
			Response ret = sepaClient.update(updateID, updateSPARQL.getText(), bindings,
					Integer.parseInt(timeout.getText()));
			Instant stop = Instant.now();
			if (ret.isError()) {
				logger.error(ret.toString() + String.format(" (%d ms)", (stop.toEpochMilli() - start.toEpochMilli())));
				updateInfo.setText("Error: " + ((ErrorResponse) ret).getErrorCode());
			} else {
				logger.info(String.format("Update OK (%d ms)", (stop.toEpochMilli() - start.toEpochMilli())));
				updateInfo.setText(String.format("Update OK (%d ms)", (stop.toEpochMilli() - start.toEpochMilli())));
			}
		} catch (NumberFormatException | SEPAProtocolException | SEPASecurityException | IOException e) {
			logger.error(e.getMessage());
		}
	}

	protected void selectUpdateID(String id) {
		if (id == null)
			return;
		updateID = id;
		JSAP app = sepaClient.getApplicationProfile();
		updateSPARQL.setText(app.getSPARQLUpdate(id));

		Bindings bindings = app.getUpdateBindings(id);
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
		if (id == null)
			return;

		queryID = id;
		JSAP app = sepaClient.getApplicationProfile();
		querySPARQL.setText(app.getSPARQLQuery(id));

		Bindings bindings = app.getQueryBindings(id);
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
			case "URI":
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
		btnQuery.setEnabled(false);
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
		btnQuery.setEnabled(true);
		subscribeButton.setEnabled(true);
	}
}
