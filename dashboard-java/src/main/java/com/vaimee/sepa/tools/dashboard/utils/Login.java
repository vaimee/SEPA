package com.vaimee.sepa.tools.dashboard.utils;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.vaimee.sepa.api.commons.exceptions.SEPAPropertiesException;
import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.api.commons.response.ErrorResponse;
import com.vaimee.sepa.api.commons.response.Response;
import com.vaimee.sepa.api.commons.security.ClientSecurityManager;
import com.vaimee.sepa.api.commons.security.OAuthProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.SwingConstants;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JCheckBox;

public class Login extends JDialog {
	private static final Logger logger = LogManager.getLogger();

	/**
	 * 
	 */
	private static final long serialVersionUID = 544263217213326603L;
	private final JPanel contentPanel = new JPanel();
	private JTextField ID;
	private JPasswordField PWD;
	private JLabel lblPassword;

	private JButton btnLogin;

	private OAuthProperties oauth;
	private ClientSecurityManager sm;
	private LoginListener m_listener;
	private JCheckBox chckRemeberMe;
	/**
	 * Create the dialog.
	 */
	public Login(OAuthProperties oauth, LoginListener listener, JFrame parent) {//,String clientid,String clientsecret) {
		this.oauth = oauth;
		m_listener = listener;
		
		setType(Type.POPUP);
		setModal(true);
		if (oauth == null)
			throw new IllegalArgumentException("OAuthProperties is null");
		if (m_listener == null)
			throw new IllegalArgumentException("LoginListener is null");

//		ButtonGroup group = new ButtonGroup();

		setResizable(false);
		setLocationRelativeTo(parent);

		// add a window listener
		addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent e) {
				logger.info("jdialog window closed");
			}

			public void windowClosing(WindowEvent e) {
				logger.info("jdialog window closing");
				m_listener.onLoginClose();
			}
		});

		setTitle("SEPA Login");
		setBounds(100, 100, 347, 152);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		GridBagLayout gbl_contentPanel = new GridBagLayout();
		gbl_contentPanel.columnWidths = new int[] { 97, 75, 175, 0 };
		gbl_contentPanel.rowHeights = new int[] { 0, 0, 0, 0, 0 };
		gbl_contentPanel.columnWeights = new double[] { 0.0, 1.0, 0.0, Double.MIN_VALUE };
		gbl_contentPanel.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };

		btnLogin = new JButton("Login");
		
		contentPanel.setLayout(gbl_contentPanel);
		{
			{
				JLabel lblUsername = new JLabel("ID");
				GridBagConstraints gbc_lblUsername = new GridBagConstraints();
				gbc_lblUsername.anchor = GridBagConstraints.EAST;
				gbc_lblUsername.insets = new Insets(0, 0, 5, 5);
				gbc_lblUsername.gridx = 0;
				gbc_lblUsername.gridy = 0;
				contentPanel.add(lblUsername, gbc_lblUsername);
			}
			{
				ID = new JTextField();
				GridBagConstraints gbc_ID = new GridBagConstraints();
				gbc_ID.gridwidth = 2;
				gbc_ID.insets = new Insets(0, 0, 5, 0);
				gbc_ID.fill = GridBagConstraints.HORIZONTAL;
				gbc_ID.gridx = 1;
				gbc_ID.gridy = 0;
				contentPanel.add(ID, gbc_ID);
				ID.setColumns(10);
				
				if (oauth.getClientId()!= null) ID.setText(oauth.getClientId());
			}
			{
				lblPassword = new JLabel("Password");
				GridBagConstraints gbc_lblPassword = new GridBagConstraints();
				gbc_lblPassword.anchor = GridBagConstraints.EAST;
				gbc_lblPassword.insets = new Insets(0, 0, 5, 5);
				gbc_lblPassword.gridx = 0;
				gbc_lblPassword.gridy = 1;
				contentPanel.add(lblPassword, gbc_lblPassword);
			}
			{
				PWD = new JPasswordField();
				PWD.addKeyListener(new KeyAdapter() {
					@Override
					public void keyPressed(KeyEvent e) {
						if (e.getKeyCode() == KeyEvent.VK_ENTER) submit();
					}
				});
				GridBagConstraints gbc_PWD = new GridBagConstraints();
				gbc_PWD.gridwidth = 2;
				gbc_PWD.insets = new Insets(0, 0, 5, 0);
				gbc_PWD.fill = GridBagConstraints.HORIZONTAL;
				gbc_PWD.gridx = 1;
				gbc_PWD.gridy = 1;
				contentPanel.add(PWD, gbc_PWD);
				
				if (oauth.getClientSecret() != null) PWD.setText(oauth.getClientSecret());
			}
			{
				chckRemeberMe = new JCheckBox("Remember me");
				chckRemeberMe.setSelected(true);
				chckRemeberMe.setHorizontalAlignment(SwingConstants.RIGHT);
				GridBagConstraints gbc_chckRemeberMe = new GridBagConstraints();
				gbc_chckRemeberMe.insets = new Insets(0, 0, 5, 0);
				gbc_chckRemeberMe.gridx = 2;
				gbc_chckRemeberMe.gridy = 2;
				contentPanel.add(chckRemeberMe, gbc_chckRemeberMe);
			}
			
			{
				GridBagConstraints gbc_btnLogin = new GridBagConstraints();
				gbc_btnLogin.anchor = GridBagConstraints.EAST;
				gbc_btnLogin.gridx = 2;
				gbc_btnLogin.gridy = 3;
				contentPanel.add(btnLogin, gbc_btnLogin);
			}
		}

		btnLogin.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				submit();
			}
		});
	}
	
	private void submit() {
		try {
			sm = new ClientSecurityManager(oauth);
			oauth.setCredentials(ID.getText(), new String(PWD.getPassword()));

			Response ret = sm.refreshToken();
			if (ret.isError()) {
				logger.error(ret);
				m_listener.onLoginError((ErrorResponse) ret);
				setTitle("Wrong credentials");
				return;
			}

			if (chckRemeberMe.isSelected()) oauth.storeProperties();
			
			m_listener.onLogin(ID.getText());//, new String(PWD.getPassword()),chckRemeberMe.isSelected());
		} catch (SEPASecurityException | SEPAPropertiesException e1) {
			logger.error(e1.getMessage());
			m_listener.onLoginError(new ErrorResponse(401, "not_authorized", e1.getMessage()));
			return;
		}
    }
}
