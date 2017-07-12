package it.unibo.arces.wot.sepa.webthings.apps.plugfest;

import java.awt.EventQueue;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;

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

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.webthings.apps.plugfest.Context.COLOR;
import it.unibo.arces.wot.sepa.webthings.apps.plugfest.Context.CONTEXT_TYPE;

import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.border.LineBorder;

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
	private JPanel infoPanel;
	private JLabel infoLabel;
	private JLabel onOffLabel;
	private JPanel panelOnOff;

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
			Set<COLOR> colors = new HashSet<COLOR>();
			
			if (win) {
				backgroundLabel.setIcon(yesIcon);
				chooseCardLabel.setText("$$$ You won $$$");
				
				actionManager.setText("$$$$$ YOU $$$$$$$$$$$ WON $$$$$$");
				colors.add(COLOR.GREEN);
				actionManager.setColors(colors);

			} else {
				backgroundLabel.setIcon(noIcon);
				chooseCardLabel.setText("You lost");
				
				actionManager.setText(":-( :-( :-( :-(     YOU LOST   ");
				colors.add(COLOR.RED);
				actionManager.setColors(colors);
				
			}
		}

		@Override
		public void onEmpty() {
			actionManager.clearColors();
			
			switch (context.getActiveContextType()) {

			case CARDS:
				backgroundLabel.setIcon(backgroundIcon);
				chooseCardLabel.setText("Choose your card");
				
				actionManager.setText("Choose your card");
				break;
			
			case COLORS:
				panelColorRed.setVisible(false);
				panelColorGreen.setVisible(false);
				panelColorBlue.setVisible(false);
				
				actionManager.setText("Play with colors");
				break;
			
			case USERS:
				rfidLabel.setText("---");
				actionManager.setText("Pass a user TAG");
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
			//actionManager.setBlinking(false);
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
			if (context.isColor(tag) || context.isCard(tag)) {
				//Not allowed
				Set<COLOR> color = new HashSet<COLOR>();
				color.add(COLOR.RED);
				actionManager.setColors(color);
				actionManager.setText("Wanna play with colors or cards?");
				return;
			}
			
			if (context.isNewUser(tag)) {
				// New user
				context.addUserID(tag);
				
				usersDM.addUserID(tag);
				rfidLabel.setText(tag);
				actionManager.clearColors();
				
				actionManager.setText(tag);
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

		@Override
		public void onConnectionStatus(Boolean on) {
			if (on) {
				onOffLabel.setText("ONLINE");
				panelOnOff.setBackground(Color.GREEN);
			}
			else {
				onOffLabel.setText("OFFLINE");
				panelOnOff.setBackground(Color.RED);
			}
			
		}

		@Override
		public void onConnectionError(ErrorResponse error) {
			// TODO Auto-generated method stub
			
		}

	}

	class UsersTableModel extends AbstractTableModel {// implements
																// TableModelListener
																// {
		/**
		 * 
		 */
		private static final long serialVersionUID = 2300692096939701619L;

		private HashMap<Integer, String> rows = new HashMap<Integer, String>();
		private HashMap<String, Integer> records = new HashMap<String, Integer>();
		private HashMap<Integer, String> users = new HashMap<Integer, String>();
		private HashMap<Integer, Boolean> authorized = new HashMap<Integer, Boolean>();

		private HashMap<Integer, String> columns = new HashMap<Integer, String>();

		public UsersTableModel() {
			columns.put(0, "Authorized");
			columns.put(1, "RFID");
			columns.put(2, "User name");
		}

		public void addUserID(String id) {
			if (records.containsKey(id))
				return;

			rows.put(getRowCount(), id);
			records.put(id, getRowCount());

			super.fireTableDataChanged();
		}

		public void setUserName(String id, String name) {
			if (!records.containsKey(id))
				return;

			users.put(records.get(id), name);

			super.fireTableDataChanged();
		}

		public void setUserAuthorization(String id, Boolean auth) {
			if (!records.containsKey(id))
				return;

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
				context.setUserAuthorization((String) getValueAt(rowIndex,1), (Boolean) aValue);
				break;
			case 2:
				users.put(rowIndex, (String) aValue);
				context.setUserName((String) getValueAt(rowIndex,1), (String) aValue);
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
		
		actionManager.clearColors();
		actionManager.setText("Play with colors");
		infoLabel.setText("WoT PlugFest - Dusseldorf - 9 July 2017 - Let Things Talk");
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmWebOfThings = new JFrame();
		frmWebOfThings.setResizable(false);
		frmWebOfThings.setTitle("Web of Things Demo - Dusseldorf PlugFest");
		frmWebOfThings.setBounds(0, 0, 1366, 700);
		frmWebOfThings.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 640, 0 };
		gridBagLayout.rowHeights = new int[] { 720, 34, 0 };
		gridBagLayout.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 1.0, 1.0, Double.MIN_VALUE };
		frmWebOfThings.getContentPane().setLayout(gridBagLayout);

		try {
			backgroundIcon = new ImageIcon(ImageIO.read(new File("resources/background.jpg")));
		} catch (IOException e) {
			backgroundLabel.setText("background.jpg not found");
		}

		try {
			yesIcon = new ImageIcon(ImageIO.read(new File("resources/yes.png")));
		} catch (IOException e) {
			backgroundLabel.setText("yes.png not found");
		}

		try {
			noIcon = new ImageIcon(ImageIO.read(new File("no.png")));
		} catch (IOException e) {
			backgroundLabel.setText("no.png not found");
		}

		JTabbedPane contextSelectPanel = new JTabbedPane(JTabbedPane.TOP);
		GridBagConstraints gbc_contextSelectPanel = new GridBagConstraints();
		gbc_contextSelectPanel.insets = new Insets(0, 0, 5, 0);
		gbc_contextSelectPanel.fill = GridBagConstraints.BOTH;
		gbc_contextSelectPanel.gridx = 0;
		gbc_contextSelectPanel.gridy = 0;
		frmWebOfThings.getContentPane().add(contextSelectPanel, gbc_contextSelectPanel);

		JPanel panelRGBGame = new JPanel();
		contextSelectPanel.addTab("RGB Game", null, panelRGBGame, null);
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
		contextSelectPanel.addTab("Users identification", null, panelUsersID, null);
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

		JPanel panelCards = new JPanel();

		panelCards.setBorder(new EmptyBorder(5, 5, 5, 5));
		contextSelectPanel.addTab("Three cards game", null, panelCards, null);
		GridBagLayout gbl_panelCards = new GridBagLayout();
		gbl_panelCards.columnWidths = new int[] { 61, 0 };
		gbl_panelCards.rowHeights = new int[] { 16, 0, 0 };
		gbl_panelCards.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_panelCards.rowWeights = new double[] { 1.0, 0.0, Double.MIN_VALUE };
		panelCards.setLayout(gbl_panelCards);

		backgroundLabel = new JLabel("");
		GridBagConstraints gbc_backgroundLabel = new GridBagConstraints();
		gbc_backgroundLabel.insets = new Insets(0, 0, 5, 0);
		gbc_backgroundLabel.anchor = GridBagConstraints.NORTH;
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

		backgroundLabel.setIcon(backgroundIcon);

		infoPanel = new JPanel();
		GridBagConstraints gbc_infoPanel = new GridBagConstraints();
		gbc_infoPanel.fill = GridBagConstraints.BOTH;
		gbc_infoPanel.gridx = 0;
		gbc_infoPanel.gridy = 1;
		frmWebOfThings.getContentPane().add(infoPanel, gbc_infoPanel);
		GridBagLayout gbl_infoPanel = new GridBagLayout();
		gbl_infoPanel.columnWidths = new int[] { 409, 723, 0, 0 };
		gbl_infoPanel.rowHeights = new int[] { 43, 0 };
		gbl_infoPanel.columnWeights = new double[] { 1.0, 1.0, 1.0, Double.MIN_VALUE };
		gbl_infoPanel.rowWeights = new double[] { 1.0, Double.MIN_VALUE };
		infoPanel.setLayout(gbl_infoPanel);

		infoLabel = new JLabel("Info");
		infoLabel.setFont(new Font("Lucida Grande", Font.BOLD, 15));
		GridBagConstraints gbc_infoLabel = new GridBagConstraints();
		gbc_infoLabel.insets = new Insets(0, 0, 0, 5);
		gbc_infoLabel.gridx = 0;
		gbc_infoLabel.gridy = 0;
		infoPanel.add(infoLabel, gbc_infoLabel);
		
		panelOnOff = new JPanel();
		panelOnOff.setBorder(new LineBorder(new Color(0, 0, 0), 3));
		panelOnOff.setBackground(Color.RED);
		GridBagConstraints gbc_panelOnOff = new GridBagConstraints();
		gbc_panelOnOff.fill = GridBagConstraints.BOTH;
		gbc_panelOnOff.gridx = 2;
		gbc_panelOnOff.gridy = 0;
		infoPanel.add(panelOnOff, gbc_panelOnOff);

		onOffLabel = new JLabel("OFFLINE");
		onOffLabel.setHorizontalAlignment(SwingConstants.CENTER);
		panelOnOff.add(onOffLabel);
		onOffLabel.setForeground(Color.BLACK);
		onOffLabel.setFont(new Font("Lucida Grande", Font.BOLD, 13));
		onOffLabel.setBackground(Color.BLACK);

		contextSelectPanel.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				System.out.println("Tab: " + contextSelectPanel.getSelectedIndex());
				actionManager.clearColors();
				switch (contextSelectPanel.getSelectedIndex()) {
				case 0:
					context.setActiveContextType(CONTEXT_TYPE.COLORS);
					
					actionManager.setText("Play with colors");
					break;
				case 1:
					context.setActiveContextType(CONTEXT_TYPE.USERS);
					
					actionManager.setText("Place a user TAG");
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
