package it.unibo.arces.wot.sepa.webthings.rfidreader;

import java.util.HashMap;
import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CheckerWithHysteresis implements ITagsChangesChecker {
	private static final Logger logger = LogManager.getLogger("CheckerWithHysteresis");
	
	private HashSet<String> newTags = new HashSet<String>();
	private HashSet<String> oldTags = new HashSet<String>();
	private HashSet<String> activeTags = null;

	private int HIGH_TH = 6;
	private int LOW_TH = 2;
	private int MIN = 0;
	private int MAX = 7;
	private boolean enabled = false;
	
	private HashMap<String, Integer> tagReadingsCount = new HashMap<String, Integer>();

	public CheckerWithHysteresis(boolean enabled) {
		if (!enabled) {
			HIGH_TH = 1;
			LOW_TH = 1;
			MIN = 0;
			MAX = 1;
		}
		this.enabled = enabled;
	}
	
	@Override
	public HashSet<String> checkChanges(HashSet<String> tags) {
		// Reset old and new tags
		oldTags.clear();
		newTags.clear();
		
		logger.debug("Input tags: "+tags);
		
		// Tags to be removed from counting tags set
		HashSet<String> toBeRemoved = new HashSet<String>();
		
		// Update tag counters
		for (String tag : tagReadingsCount.keySet()) {
			logger.debug(tag+" ? "+tagReadingsCount);
			int count = tagReadingsCount.get(tag);
			if (tags.contains(tag)) {
				count++;
				if (count > MAX) count = MAX;
				tagReadingsCount.put(tag, count);
				
				// Overtake threshold?
				if (count < HIGH_TH)
					continue;

				// Is a new active tag?
				if (!activeTags.contains(tag)) newTags.add(tag);
				activeTags.add(tag);
			}
			else {
				count--;
				if (count < MIN) count = MIN;
				tagReadingsCount.put(tag, count);
				
				// Overtake threshold?
				if (count >= LOW_TH)
					continue;
				
				// Is an old active tag?
				if (!activeTags.contains(tag))
					continue;
				
				activeTags.remove(tag);
				oldTags.add(tag);
				
				if (count == MIN) toBeRemoved.add(tag);	
			}
		}
		
		// Remove tags with count == 0
		for (String tag: toBeRemoved){
			tagReadingsCount.remove(tag);
		}
		
		// Insert new tags (count = 1)
		for (String tag : tags) {
			if (!tagReadingsCount.containsKey(tag)) tagReadingsCount.put(tag,1);
		}
		
		if (activeTags == null) {
			activeTags = new HashSet<String>();
			newTags.add("");
		}
		
		logger.debug("Output tags: "+activeTags);
		
		return activeTags;
	}

	public void setHighThreshold(int th) {
		if (!enabled) return;
		if (th < 0 ) HIGH_TH = 0;
		else HIGH_TH = th;
	}

	public void setLowThreshold(int th) {
		if (!enabled) return;
		if (th < 0 ) LOW_TH = 0;
		else LOW_TH = th;
	}
	
	public void setMin(int th){
		if (!enabled) return;
		MIN = th;
	}

	public void setMax(int th){
		if (!enabled) return;
		MAX = th;
	}
	
	@Override
	public boolean isChanged() {
		return !newTags.isEmpty() || !oldTags.isEmpty();
	}

}
