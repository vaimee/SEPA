package it.unibo.arces.wot.sepa.apps.chat;

/**
 * Created by luca on 25/10/17.
 */
public interface ChatListener {
    void onMessageReceived(Message message);
    void onMessageSent(Message message);
    void onMessageRead(Message message);
    void onBrokenConnection();
}
