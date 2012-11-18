/*
 Copyright (c) 2008, 2009, 2011 Lee Barney
 Permission is hereby granted, free of charge, to any person obtaining a 
 copy of this software and associated documentation files (the "Software"), 
 to deal in the Software without restriction, including without limitation the 
 rights to use, copy, modify, merge, publish, distribute, sublicense, 
 and/or sell copies of the Software, and to permit persons to whom the Software 
 is furnished to do so, subject to the following conditions:
 
 The above copyright notice and this permission notice shall be 
 included in all copies or substantial portions of the Software.
 
 
 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
 INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A 
 PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT 
 HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE 
 OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 
 
 */
package org.quickconnectfamily.dbaccess;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * This class is a container, JavaBean, for information regarding the execution of a DataAccessObject getData or setData method call.
 * @author Lee S. Barney
 *
 */

public class DataAccessResult implements Serializable {
	/**
	 * An Array of Strings that includes all of the field names of the resultant table of a query.
	 */
	private String[] columnNames = {};

	/**
	 * An ArrayList of ArrayLists of Strings containing the resultant data of a query.  All values in this 'table' are Strings regardless 
	 * of their underlying type.
	 */
	private ArrayList<ArrayList<String>> results = new ArrayList<ArrayList<String>>();
	/**
	 * A String describing any database error that occurred as part of a DataAccessObject getData or setData call.  If no error occured 
	 * then this attribute is null. 
	 */
	private String errorDescription = "not an error";
	
	public DataAccessResult() {
		// TODO Auto-generated constructor stub
	}
	/**
	 * Accessor for the field names of the resultant table of a query.
	 * @return - An array of Strings containing all the field names.
	 */
	public String[] getColumnNames() {
		return columnNames;
	}

	public void setColumnNames(String[] columnNames) {
		this.columnNames = columnNames;
	}
	/**
	 * Accessor for the data 'table' that is the result of executing a query against a database. 
	 * @return - An ArrayList of ArrayLists of Strings that is the resultant 'table'
	 */
	public ArrayList<ArrayList<String>> getResults() {
		return results;
	}

	public void setResults(ArrayList<ArrayList<String>> results) {
		this.results = results;
	}
	/**
	 * Accessor for any error that may have occurred as a result of a call to the DataAccessObject getData or setData methods.
	 * @return - a String describing the error or null if there was no error.
	 */
	public String getErrorDescription() {
		return errorDescription;
	}

	public void setErrorDescription(String errorDescription) {
		//error descriptions have double quotes in them that can cause problems with JSON.  Change them to single quotes.
		errorDescription = errorDescription.replaceAll("\"", "'");
		this.errorDescription = errorDescription;
	}

}
