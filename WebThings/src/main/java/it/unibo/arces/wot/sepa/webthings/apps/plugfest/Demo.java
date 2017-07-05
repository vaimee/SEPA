package it.unibo.arces.wot.sepa.webthings.apps.plugfest;

import java.awt.EventQueue;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import java.awt.GridBagLayout;
import java.awt.GridLayout;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.Font;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.webthings.apps.plugfest.Context.COLOR;
import it.unibo.arces.wot.sepa.webthings.apps.plugfest.Context.CONTEXT_TYPE;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JTable;
import javax.swing.JScrollPane;

public class Demo {
	protected static final Logger logger = LogManager.getLogger("WoTDemo");

	private JFrame frmWebOfThings;

	private ImageIcon backgroundIcon;
	private ImageIcon yesIcon;
	private ImageIcon noIcon;

	// Cards
	private JLabel backgroundLabel;
	private JTable usersTable;
	private JLabel chooseCardLabel;

	// Colors
	private JPanel panelColorGreen;
	private JPanel panelColorBlue;
	private JPanel panelColorRed;

	// Users
	private JLabel rfidLabel;
	private UsersTableModel usersDM = new UsersTableModel();

	// Event manager
	private DemoEventManager eventManager;

	// Action manager
	private ActionManager actionManager = new ActionManager();

	// Context
	Context context;

	private class DemoEventManager extends EventManager {

		public DemoEventManager(Context context) throws InvalidKeyException, FileNotFoundException,
				NoSuchElementException, IllegalArgumentException, NullPointerException, ClassCastException,
				NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException,
				IOException, UnrecoverableKeyException, KeyManagementException, KeyStoreException, CertificateException,
				URISyntaxException, InterruptedException {
			super(context);
		}

		@Override
		public void onPlayCards(boolean win) {
			if (win) {
				backgroundLabel.setIcon(yesIcon);
				chooseCardLabel.setText("$$$ You won $$$");
				actionManager.setText("$$$$ YOU $$$$$$$$$$$ WON $$$$$");

			} else {
				backgroundLabel.setIcon(noIcon);
				chooseCardLabel.setText("You lost");
				actionManager.setText(":-( :-( :-( :-(     YOU LOST   ");
			}
		}

		@Override
		public void onEmpty() {
			Set<COLOR> colors = new HashSet<COLOR>();

			switch (context.getActiveContextType()) {

			case CARDS:
				backgroundLabel.setIcon(backgroundIcon);
				chooseCardLabel.setText("Choose your card");

				colors.add(COLOR.BLUE);

				actionManager.setColors(colors);
				actionManager.setBlinking(true);
				actionManager.setText("Choose your card");
				break;
			case COLORS:
				panelColorRed.setVisible(false);
				panelColorGreen.setVisible(false);
				panelColorBlue.setVisible(false);

				colors.add(COLOR.GREEN);

				actionManager.setColors(colors);
				actionManager.setBlinking(true);
				actionManager.setText("Play with colors");
				break;
			case USERS:
				rfidLabel.setText("---");

				actionManager.setText("Pass a user TAG");
				actionManager.clearColors();
				actionManager.setBlinking(false);
				break;
			}
		}

		@Override
		public void onColors(Set<COLOR> colors) {
			if (colors.contains(COLOR.RED))
				panelColorRed.setVisible(true);
			else
				panelColorRed.setVisible(false);

			if (colors.contains(COLOR.GREEN))
				panelColorGreen.setVisible(true);
			else
				panelColorGreen.setVisible(false);

			if (colors.contains(COLOR.BLUE))
				panelColorBlue.setVisible(true);
			else
				panelColorBlue.setVisible(false);

			actionManager.setColors(colors);
		}

		@Override
		public void onReedEvent(boolean on) {
			actionManager.setBlinking(on);
		}

		@Override
		public void onRFIDTags(String[] tags) {
			Set<COLOR> color = new HashSet<COLOR>();
			color.add(COLOR.RED);

			actionManager.setColors(color);
			actionManager.setText("Too many TAGS");
		}

		@Override
		public void onRFIDTag(String tag) {
			if (context.addUserID(tag)) {
				// New user
				usersDM.addUserID(tag);
				rfidLabel.setText(tag);
				return;
			}

			Set<COLOR> color = new HashSet<COLOR>();
			String text = "";
			if (context.isAuthorized(tag)) {
				color.add(COLOR.GREEN);
				text = context.getUserName(tag);
			} else {
				color.add(COLOR.RED);
				text = tag;
			}
			rfidLabel.setText(text);
			actionManager.setColors(color);
			actionManager.setText(text);
		}

	}

	private class UsersTableModel extends AbstractTableModel {//implements TableModelListener {
		/**
		 * 
		 */
		private static final long serialVersionUID = 2300692096939701619L;

		private HashMap<Integer,String> rows = new HashMap<Integer,String>();
		private HashMap<String,Integer> records = new  HashMap<String,Integer>();
		private HashMap<Integer,String> users = new HashMap<Integer,String>();
		private HashMap<Integer,Boolean> authorized = new HashMap<Integer,Boolean>();

		private HashMap<Integer, String> columns = new HashMap<Integer, String>();

		public UsersTableModel() {
			columns.put(0, "Authorized");
			columns.put(1, "RFID");
			columns.put(2, "User name");
		}

		public void addUserID(String id) {
			if(records.containsKey(id)) return;
			
			rows.put(getRowCount(), id);
			records.put(id, getRowCount());
			
			super.fireTableDataChanged();
		}

		public void setUserName(String id, String name) {
			if(!records.containsKey(id)) return;
			
			users.put(records.get(id), name);
			
			super.fireTableDataChanged();
		}

		public void setUserAuthorization(String id, Boolean auth) {
			if(!records.containsKey(id)) return;
			
			authorized.put(records.get(id), auth);
			
			super.fireTableDataChanged();
		}

		@Override
		public int getRowCount() {
			return records.size();
		}

		// Authorized RDFID Username
		@Override
		public int getColumnCount() {
			return 3;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			switch (columnIndex) {
			case 0:
				return authorized.get(rowIndex);
			case 1:
				return rows.get(rowIndex);
			case 2:
				return users.get(rowIndex);
			}
			return null;
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			if (columnIndex == 1 || columnIndex == 2)
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
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			switch (columnIndex) {
			case 0:
				authorized.put(rowIndex, (Boolean) aValue);
				break;
			case 2:
				users.put(rowIndex, (String) aValue);
				break;
			}
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			if (columnIndex == 1)
				return false;
			return true;
		}

		@Override
		public void addTableModelListener(TableModelListener l) {
			super.addTableModelListener(l);
		}

		@Override
		public void removeTableModelListener(TableModelListener l) {
			super.removeTableModelListener(l);
		}

//		@Override
//		public void tableChanged(TableModelEvent e) {
//			super.fireTableChanged(e);
////			int row = e.getFirstRow();
////			int column = e.getColumn();
////			TableModel model = (TableModel) e.getSource();
////			String columnName = model.getColumnName(column);
////			Object data = model.getValueAt(row, column);
//
//		}
	}

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Demo window = new Demo();
					window.frmWebOfThings.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 * 
	 * @throws URISyntaxException
	 * @throws IOException
	 * @throws BadPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws NoSuchPaddingException
	 * @throws ClassCastException
	 * @throws NullPointerException
	 * @throws NoSuchElementException
	 * @throws FileNotFoundException
	 * @throws CertificateException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws IllegalArgumentException
	 * @throws InvalidKeyException
	 * @throws KeyManagementException
	 * @throws UnrecoverableKeyException
	 * @throws InterruptedException
	 */
	public Demo()
			throws UnrecoverableKeyException, KeyManagementException, InvalidKeyException, IllegalArgumentException,
			KeyStoreException, NoSuchAlgorithmException, CertificateException, FileNotFoundException,
			NoSuchElementException, NullPointerException, ClassCastException, NoSuchPaddingException,
			IllegalBlockSizeException, BadPaddingException, IOException, URISyntaxException, InterruptedException {
		initialize();

		context = new Context();
		context.setActiveContextType(CONTEXT_TYPE.COLORS);

		eventManager = new DemoEventManager(context);
		eventManager.startListeningForEvents();

		for (String id : context.getAllUserIds()) {
			usersDM.addUserID(id);
			usersDM.setUserName(id, context.getUserName(id));
			usersDM.setUserAuthorization(id, context.isAuthorized(id));
		}
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmWebOfThings = new JFrame();
		frmWebOfThings.setResizable(false);
		frmWebOfThings.setTitle("Web of Things Demo - Dusseldorf PlugFest");
		frmWebOfThings.setBounds(0, 0, 640, 640);
		frmWebOfThings.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		frmWebOfThings.getContentPane().add(tabbedPane, BorderLayout.CENTER);

		JPanel panelRGBGame = new JPanel();
		tabbedPane.addTab("RGB Game", null, panelRGBGame, null);
		panelRGBGame.setLayout(new GridLayout(0, 1, 0, 0));

		panelColorRed = new JPanel();
		panelColorRed.setBackground(Color.RED);
		panelRGBGame.add(panelColorRed);

		panelColorGreen = new JPanel();
		panelColorGreen.setBackground(Color.GREEN);
		panelRGBGame.add(panelColorGreen);

		panelColorBlue = new JPanel();
		panelColorBlue.setBackground(Color.BLUE);
		panelRGBGame.add(panelColorBlue);

		JPanel panelUsersID = new JPanel();
		tabbedPane.addTab("Users identification", null, panelUsersID, null);
		GridBagLayout gbl_panelUsersID = new GridBagLayout();
		gbl_panelUsersID.columnWidths = new int[] { 0, 0 };
		gbl_panelUsersID.rowHeights = new int[] { 0, 0, 0 };
		gbl_panelUsersID.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_panelUsersID.rowWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		panelUsersID.setLayout(gbl_panelUsersID);

		rfidLabel = new JLabel("---");
		rfidLabel.setFont(new Font("Lucida Grande", Font.PLAIN, 26));
		GridBagConstraints gbc_rfidLabel = new GridBagConstraints();
		gbc_rfidLabel.insets = new Insets(0, 0, 5, 0);
		gbc_rfidLabel.gridx = 0;
		gbc_rfidLabel.gridy = 0;
		panelUsersID.add(rfidLabel, gbc_rfidLabel);

		JScrollPane scrollPane = new JScrollPane();
		GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.fill = GridBagConstraints.BOTH;
		gbc_scrollPane.gridx = 0;
		gbc_scrollPane.gridy = 1;
		panelUsersID.add(scrollPane, gbc_scrollPane);

		usersTable = new JTable(usersDM);
		scrollPane.setViewportView(usersTable);
		//usersTable.getModel().addTableModelListener(usersDM);

		JPanel panelCards = new JPanel();
		panelCards.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.isAltDown())
					backgroundLabel.setIcon(backgroundIcon);
				else if (e.getClickCount() == 1)
					backgroundLabel.setIcon(noIcon);
				else
					backgroundLabel.setIcon(yesIcon);
			}
		});
		panelCards.setBorder(new EmptyBorder(5, 5, 5, 5));
		tabbedPane.addTab("Three cards game", null, panelCards, null);
		GridBagLayout gbl_panelCards = new GridBagLayout();
		gbl_panelCards.columnWidths = new int[] { 61, 0 };
		gbl_panelCards.rowHeights = new int[] { 16, 0, 0 };
		gbl_panelCards.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_panelCards.rowWeights = new double[] { 1.0, 0.0, Double.MIN_VALUE };
		panelCards.setLayout(gbl_panelCards);

		backgroundLabel = new JLabel("");
		GridBagConstraints gbc_backgroundLabel = new GridBagConstraints();
		gbc_backgroundLabel.insets = new Insets(0, 0, 5, 0);
		gbc_backgroundLabel.anchor = GridBagConstraints.NORTHWEST;
		gbc_backgroundLabel.gridx = 0;
		gbc_backgroundLabel.gridy = 0;
		panelCards.add(backgroundLabel, gbc_backgroundLabel);

		chooseCardLabel = new JLabel("Choose your card");
		chooseCardLabel.setVerticalAlignment(SwingConstants.TOP);
		chooseCardLabel.setFont(new Font("Lucida Grande", Font.PLAIN, 60));
		GridBagConstraints gbc_chooseCardLabel = new GridBagConstraints();
		gbc_chooseCardLabel.gridx = 0;
		gbc_chooseCardLabel.gridy = 1;
		panelCards.add(chooseCardLabel, gbc_chooseCardLabel);

		try {
			backgroundIcon = new ImageIcon(ImageIO.read(new File("background.jpg")));
		} catch (IOException e) {
			backgroundLabel.setText("background.jpg not found");
		}

		try {
			yesIcon = new ImageIcon(ImageIO.read(new File("yes.png")));
		} catch (IOException e) {
			backgroundLabel.setText("yes.png not found");
		}

		try {
			noIcon = new ImageIcon(ImageIO.read(new File("no.png")));
		} catch (IOException e) {
			backgroundLabel.setText("no.png not found");
		}

		backgroundLabel.setIcon(backgroundIcon);

		tabbedPane.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				System.out.println("Tab: " + tabbedPane.getSelectedIndex());
				switch (tabbedPane.getSelectedIndex()) {
				case 0:
					context.setActiveContextType(CONTEXT_TYPE.COLORS);
					actionManager.setText("Play with colors");
					break;
				case 1:
					context.setActiveContextType(CONTEXT_TYPE.USERS);
					actionManager.setText("Place your ID tag");
					break;
				case 2:
					context.setActiveContextType(CONTEXT_TYPE.CARDS);
					actionManager.setText("Choose your card");
					break;
				}
			}
		});
	}
}
