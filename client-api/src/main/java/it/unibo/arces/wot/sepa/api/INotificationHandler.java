/* This interface includes all the methods that need to be implemented by a SEPA listener
 * 
 * Author: Luca Roffia (luca.roffia@unibo.it)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package it.unibo.arces.wot.sepa.api;

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;

/**
 * The Interface NotificationHandler.
 */
public interface INotificationHandler {

	/**
	 * Semantic event.
	 *
	 * @param notify
	 *            the SEPA notification
	 * 
	 * @see Notification
	 */
	public void onSemanticEvent(Notification notify);

//	/**
//	 * Subscribe confirmed.
//	 *
//	 * @param response
//	 *            the subscribe response
//	 * 
//	 * @see SubscribeResponse
//	 */
//	public void onSubscribeConfirm(SubscribeResponse response);
//
//	/**
//	 * Unsubscribe confirmed.
//	 *
//	 * @param response
//	 *            the unsubscribe response
//	 * 
//	 * @see UnsubscribeResponse
//	 */
//	public void onUnsubscribeConfirm(UnsubscribeResponse response);

	/**
	 * Ping.
	 * 
	 * @see it.unibo.arces.wot.sepa.commons.response.Ping
	 */
	public void onPing();

	/**
	 * Broken subscription. This method is called if the Websocket connection
	 * has been lost
	 */
	public void onBrokenSocket();

	/**
	 * On error.
	 *
	 * @param errorResponse
	 *            the error response
	 * 
	 * @see ErrorResponse
	 */
	public void onError(ErrorResponse errorResponse);
}
