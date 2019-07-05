package it.unibo.arces.wot.sepa.apps.chat;

class Message {
	private String from;
	private String to;
	private String time;
	private String text;
	private String msg;
	
	/**
	 * Represent a chat message
	 * 
	 * */
	public Message(String msg, String from,String to,String text,String time) {
		this.msg = msg;
		this.from = from;
		this.to= to;
		this.text = text;
		this.time = time;
	}

	public String getMessage() {
		return msg;
		
	}
	public String getTo() {
		return to;
	}

	public String getTime() {
		return time;
	}

	public String getText() {
		return text;
	}

    public String toString() {
        return "From: <"+from+"> To: <"+to+"> Message: <"+text+">";
    }
    
    public String toCSV() {
    		return msg+ " " +from + " "+ to + " " + text+ " "+ time;
    }

	public String getFrom() {
		return from;
	}
}
