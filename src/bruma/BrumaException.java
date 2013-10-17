/*=========================================================================

    Copyright © 2011 BIREME/PAHO/WHO

    This file is part of Bruma.

    Bruma is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as 
    published by the Free Software Foundation, either version 3 of 
    the License, or (at your option) any later version.

    Bruma is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public 
    License along with Bruma. If not, see <http://www.gnu.org/licenses/>.

=========================================================================*/

package bruma;

/**
 * The only exception thrown by all Bruma member functions.
 * @author Heitor Barbieri
 */
public class BrumaException extends java.lang.Exception {
    /**
     * Creates a new instance of <code>BrumaException</code> without detail
     * message.
     */
    public BrumaException() {
        super();
    }

    /**
     * Constructs an instance of <code>BrumaException</code> with the specified
     * detail message.
     * @param message the detail message.
     */
    public BrumaException(final String message) {
        super(message);
    }

    /**
     * Constructs an instance of <code>BrumaException</code> with the specified
     * cause.
     * @param cause the responsable for the exception.
     */
    public BrumaException(final Throwable cause) {
        super(cause);
    }
}

