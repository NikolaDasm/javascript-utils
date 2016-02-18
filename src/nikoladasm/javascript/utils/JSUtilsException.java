/*
 *  JavaScript Utils
 *  Copyright (C) 2016  Nikolay Platov
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nikoladasm.javascript.utils;

public class JSUtilsException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public JSUtilsException(Throwable t) {
		super(t);
	}
	
	public JSUtilsException(String msg) {
		super(msg);
	}
	
	public JSUtilsException(String msg, Throwable t) {
		super(msg, t);
	}
}
