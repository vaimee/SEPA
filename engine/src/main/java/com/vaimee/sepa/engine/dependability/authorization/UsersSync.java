package com.vaimee.sepa.engine.dependability.authorization;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.logging.Logging;

public class UsersSync {
	private final IUsersSync ldap;
	private final IUsersAcl isql;

	private JsonObject users = new JsonObject();

	public UsersSync(IUsersSync l, IUsersAcl i) {
		ldap = l;
		isql = i;

		Thread th = new Thread() {
			public void run() {
				while (true) {
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						Logging.logger.debug(e.getMessage());
						return;
					}

					try {
						updateUsers(ldap.sync(), users);
					} catch (SEPASecurityException e) {
						Logging.logger.error(e.getMessage());
					}
				}
			}
		};
		th.setName("SEPA-LDAP-SYNC");
		th.start();
	}

	private void updateUsers(JsonObject ldapUsers, JsonObject users) throws SEPASecurityException {
		for (Entry<String, JsonElement> user : ldapUsers.entrySet()) {
			String uid = user.getKey();

			if (users.has(uid)) {
				JsonObject currentGraphs = users.getAsJsonObject(uid);
				JsonObject newGraphs = user.getValue().getAsJsonObject();
				JsonObject addGraphs = new JsonObject();
				JsonArray removeGraphs = new JsonArray();

				// Removed or updated
				for (Entry<String, JsonElement> graph : currentGraphs.entrySet()) {
					String graphUri = graph.getKey();
					if (newGraphs.has(graphUri)) {
						if (newGraphs.get(graphUri).getAsInt() != currentGraphs.get(graphUri).getAsInt()) {
							addGraphs.add(graphUri, newGraphs.get(graphUri));
						}
					} else
						removeGraphs.add(graphUri);
				}

				// Added
				for (Entry<String, JsonElement> graph : newGraphs.entrySet()) {
					if (!users.getAsJsonObject(uid).has(graph.getKey())) {
						addGraphs.add(graph.getKey(), graph.getValue());
					}
				}

				// UPDATE USER
				if (addGraphs.size() != 0 || removeGraphs.size() != 0) isql.updateUser(uid, addGraphs, removeGraphs);
				
				// Update users
				for (Entry<String, JsonElement> graph : addGraphs.entrySet()) {
					users.getAsJsonObject(uid).add(graph.getKey(), graph.getValue());
				}
				for (JsonElement graph : removeGraphs) {
					users.getAsJsonObject(uid).remove(graph.getAsString());
				}				
			} else {
				// CREATE USER
				isql.createUser(uid, user.getValue());
				
				// Add new entry
				users.add(uid, user.getValue());
			}
		}

		Set<Entry<String, JsonElement>> userSet = new HashSet<Entry<String, JsonElement>>();
		userSet.addAll(users.entrySet());
		
		for (Entry<String, JsonElement> user : userSet) {
			if (!ldapUsers.has(user.getKey())) {
				// REMOVE USER
				isql.removeUser(user.getKey());
				
				// Remove entry
				users.remove(user.getKey());	
			}
		}
	}
}
