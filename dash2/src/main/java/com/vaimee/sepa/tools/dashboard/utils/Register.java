package com.vaimee.sepa.tools.dashboard.utils;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaimee.sepa.api.commons.exceptions.SEPAPropertiesException;
import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.api.commons.response.RegistrationResponse;
import com.vaimee.sepa.api.commons.response.Response;
import com.vaimee.sepa.api.commons.security.OAuthProperties;
import com.vaimee.sepa.api.commons.security.ClientSecurityManager;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Register extends JDialog {
	private static final Logger logger = LogManager.getLogger();

	/**
	 * 
	 */
	private static final long serialVersionUID = 544263217213326603L;
	private final JPanel contentPanel = new JPanel();
	private JTextField ID;
	private JTextField USERNAME;
	private JTextField TOKEN;
	
	private JLabel lblPassword;
	private JButton btnLogin;
	private JLabel lblNewLabel;
	
	private ClientSecurityManager sm;
	private OAuthProperties oauth;
	
	/**
	 * Create the dialog.
	 */
	public Register(OAuthProperties prop,JFrame parent,String clientid,String username,String initialAccessToken) {
		oauth = prop;
		
		setType(Type.POPUP);
		setModal(true);
		if (oauth == null)
			throw new IllegalArgumentException("OAuth is null");

		setResizable(false);
		setLocationRelativeTo(parent);

		setTitle("SEPA Register");
		setBounds(100, 100, 347, 152);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		GridBagLayout gbl_contentPanel = new GridBagLayout();
		gbl_contentPanel.columnWidths = new int[] { 97, 75, 175, 0 };
		gbl_contentPanel.rowHeights = new int[] { 0, 0, 0, 0, 0 };
		gbl_contentPanel.columnWeights = new double[] { 0.0, 1.0, 0.0, Double.MIN_VALUE };
		gbl_contentPanel.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };

		btnLogin = new JButton("Register");
		
		contentPanel.setLayout(gbl_contentPanel);
		{
			{
				JLabel lblUsername = new JLabel("Client ID");
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
			}
			{
				lblPassword = new JLabel("Username");
				GridBagConstraints gbc_lblPassword = new GridBagConstraints();
				gbc_lblPassword.anchor = GridBagConstraints.EAST;
				gbc_lblPassword.insets = new Insets(0, 0, 5, 5);
				gbc_lblPassword.gridx = 0;
				gbc_lblPassword.gridy = 1;
				contentPanel.add(lblPassword, gbc_lblPassword);
			}
			{
				USERNAME = new JTextField();
				USERNAME.setColumns(10);
				GridBagConstraints gbc_USERNAME = new GridBagConstraints();
				gbc_USERNAME.gridwidth = 2;
				gbc_USERNAME.insets = new Insets(0, 0, 5, 5);
				gbc_USERNAME.fill = GridBagConstraints.HORIZONTAL;
				gbc_USERNAME.gridx = 1;
				gbc_USERNAME.gridy = 1;
				contentPanel.add(USERNAME, gbc_USERNAME);
			}
			{
				lblNewLabel = new JLabel("Initial access token");
				GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
				gbc_lblNewLabel.anchor = GridBagConstraints.EAST;
				gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
				gbc_lblNewLabel.gridx = 0;
				gbc_lblNewLabel.gridy = 2;
				contentPanel.add(lblNewLabel, gbc_lblNewLabel);
			}
			{
				TOKEN = new JTextField();
				TOKEN.setColumns(10);
				GridBagConstraints gbc_TOKEN = new GridBagConstraints();
				gbc_TOKEN.gridwidth = 2;
				gbc_TOKEN.insets = new Insets(0, 0, 5, 5);
				gbc_TOKEN.fill = GridBagConstraints.HORIZONTAL;
				gbc_TOKEN.gridx = 1;
				gbc_TOKEN.gridy = 2;
				contentPanel.add(TOKEN, gbc_TOKEN);
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
			Response ret = sm.registerClient(ID.getText(), USERNAME.getText(), TOKEN.getText());
			if (ret.isRegistrationResponse()) {
				RegistrationResponse cred = (RegistrationResponse) ret;
				oauth.setCredentials(cred.getClientId(), cred.getClientSecret());
				oauth.storeProperties();
			}
		} catch (SEPASecurityException | SEPAPropertiesException e1) {
			logger.error(e1.getMessage());
	        JOptionPane.showMessageDialog(null, e1.getMessage(), "Failed to register", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
	}
}
