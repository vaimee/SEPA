/* Processing exception 
 * 
 * Author: Luca Roffia (luca.roffia@unibo.it)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.vaimee.sepa.commons.exceptions;

public class SEPAProcessingException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5490593006732026048L;

	private String message = null;
	
	public SEPAProcessingException(Throwable e){
		super.initCause(e);
	}

	public SEPAProcessingException(String string) {
		message = string;
	}
	
	public String getMessage() {
		if (message != null) return message;
		return super.getMessage();
	}

}