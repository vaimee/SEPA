package it.unibo.arces.wot.sepa.webthings.rfidreader;

import java.util.HashSet;

public interface ITagsChangesChecker {
	public HashSet<String> checkChanges(HashSet<String> tags);
	public boolean isChanged();
}
