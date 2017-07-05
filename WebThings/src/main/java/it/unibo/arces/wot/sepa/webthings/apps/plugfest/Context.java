package it.unibo.arces.wot.sepa.webthings.apps.plugfest;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Context {
	private CONTEXT_TYPE activeContextType;

	public enum CONTEXT_TYPE {
		USERS, COLORS, CARDS
	};

	public enum COLOR {
		RED, GREEN, BLUE
	};

	private HashMap<String, COLOR> colors = new HashMap<String, COLOR>();
	private HashMap<String, Boolean> cards = new HashMap<String, Boolean>();

	// Users context
	private HashMap<String, String> users = new HashMap<String, String>();
	private HashMap<String, Boolean> authorizations = new HashMap<String, Boolean>();
	private JsonObject db;

	public void setActiveContextType(CONTEXT_TYPE type) {
		activeContextType = type;
	}

	public CONTEXT_TYPE getActiveContextType() {
		return activeContextType;
	}

	public Context() throws FileNotFoundException {
		colors.put("E0:02:22:0C:47:08:C2:C6", COLOR.RED);
		colors.put("E0:02:22:0C:47:08:BA:57", COLOR.GREEN);
		colors.put("E0:02:22:0C:47:08:C2:C5", COLOR.BLUE);

		cards.put("E0:02:22:0C:47:08:BA:47", false); // Q
		cards.put("E0:02:22:0C:47:08:BA:58", false); // J
		cards.put("E0:02:22:0C:47:08:BA:48", true); // Jolly

		loadUsers();
	}

	public COLOR getColor(String tag) {
		return colors.get(tag);
	}

	public boolean isJolly(String tag) {
		return cards.get(tag);
	}

	private void loadUsers() {
		FileReader in = null;

		try {
			in = new FileReader("usersDB.json");
		} catch (FileNotFoundException e) {
			return;
		}
		if (in != null) {
			db = new JsonParser().parse(in).getAsJsonObject();

			for (Entry<String, JsonElement> record : db.entrySet()) {
				String id = record.getKey();
				String user = record.getValue().getAsJsonObject().get("user").getAsString();
				Boolean authorized = record.getValue().getAsJsonObject().get("authorized").getAsBoolean();
				users.put(id, user);
				authorizations.put(id, authorized);
			}
		}
	}

	private void storeUsers() throws IOException {
		FileWriter out = new FileWriter("usersDB.json");
		out.write(db.toString());
		out.close();
	}

	public boolean addUserID(String id){
		if (users.containsKey(id)) return false;
		
		users.put(id, "");
		authorizations.put(id, false);

		try {
			storeUsers();
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	public boolean setUserAuthorization(String id, Boolean authorized) {
		if (users.get(id).equals("")) return false;
		
		authorizations.put(id, authorized);
		try {
			storeUsers();
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	public boolean setUserName(String id, String name) {
		users.put(id, name);
		try {
			storeUsers();
		} catch (IOException e) {
			return false;
		}
		return true;
	}
	
	public Set<String> getAllUserIds() {
		return users.keySet();
	}
	
	public String getUserName(String id) {
		if (users.containsKey(id)) return users.get(id);
		return "";
	}
	
	public boolean isAuthorized(String id) {
		if (authorizations.containsKey(id)) return authorizations.get(id);
		return false;
	}
}
