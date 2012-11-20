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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.Semaphore;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
/**
 * The DataAccessObject class is a wrapper created to ease the use of SQLite databases available in Android Applications.  
 * It supports the use of multiple databases per application, database transactions, and all transactions, queries, insertions 
 * into the database, and modifications of existing data are thread safe.<br/>
 * 
 * To use this wrapper include an SQLite file in the assets directory of your Android application.  To access this database call 
 * the DataAccessObject getData static method.  To make database modifications use the DataAccesObject setData method.  Both of these 
 * methods will do all of the preparatory setup required to use the database if it has not already been done.
 * <br/>
 * Transactions are started and stopped using the DataAccessObject startTransaction and endTransaction methods.  If the endTransaction 
 * method is passed <b>false</b> as the third parameter then a roll back of the changes done as part of the transaction will be executed.  
 * <br/>
 * If your database is not going to be used any longer you can use the DataAccessObject close method to free the resources.  Only close 
 * databases if you will no longer be using them during the run of your application or you need to free resources for memory reasons.
 * 
 * 
 * 
 * @author Lee S. Barney
 *
 */
public class DataAccessObject {
	private static String databaseDirectory = null;
	private static HashMap<String,SQLiteDatabase> databases;
	private static boolean inTransaction = false;
	private static Semaphore writeBlockingSemaphore;
	static{
		if(databases == null){
			databases = new HashMap<String,SQLiteDatabase>();
			/*
			 * create a semaphore that uses 'fair' access and allows the maximum number of claims.  This means that 
			 * there will be no claim hopping.  Claims will be queued in the order they were requested if 
			 * there aren't open claims.  A transaction or write will claim all of the available claims.  A read will
			 * claim only one.
			 */
			writeBlockingSemaphore = new Semaphore(Integer.MAX_VALUE, true);
		}
	}
	/*
	 * A private constructor so no one accidentally instantiates an object of this type.
	 */
	private DataAccessObject(){}

	private synchronized static SQLiteDatabase generateDataAccessObject(WeakReference<Context> aContextRef, String databaseName) throws DataAccessException {
		SQLiteDatabase retVal = null;
		if(aContextRef == null || databaseName == null){
			throw new DataAccessException("Error: unable to access the database "+databaseName+" for the activity "+aContextRef);
		}
		
		
		if(databaseDirectory == null){
			Context aContext = aContextRef.get();
			if(aContext == null){
				return null;
			}
			databaseDirectory = "/data/data/"+aContext.getPackageName()+"/databases/";
		}
		File dbDir = new File(databaseDirectory);
		if (!dbDir.exists()){
			dbDir.mkdir();
		}
		
		
		try {
			File dbFile = new File(databaseDirectory+databaseName);
			if(!dbFile.exists()){
				Context aContext = aContextRef.get();
				if(aContext == null){
					return null;
				}
				InputStream in = aContext.getAssets().open(databaseName); 
				// Open the output file 
				OutputStream out = new FileOutputStream(databaseDirectory+databaseName); 
				// Transfer bytes from the input file to the output file 
				byte[] buf = new byte[1024]; 
				int len; 
				while ((len = in.read(buf)) > 0) { 
					out.write(buf, 0, len); 
				} 
				// Close the streams 
				out.close(); 
				in.close(); 

			}
			retVal = databases.get(databaseName);
			//Log.d(QuickConnectActivity.LOG_TAG, "found database: "+databaseName+" retVal: "+retVal);
			if (retVal == null) {
				//Log.d(QuickConnectActivity.LOG_TAG, "opening database");
				
				retVal = SQLiteDatabase.openDatabase(databaseDirectory+databaseName, 
						null, SQLiteDatabase.CREATE_IF_NECESSARY);
				
				databases.put(databaseName,retVal);
			}

		} catch (IOException e) { 
			e.printStackTrace();
		}
		return retVal;
	}
	/**
	 * This method is used to execute standard SQL query statements and matching prepared statements against any SQLite database file included in 
	 * the assets directory of an Android application.
	 * @deprecated  Replaced by              
	 * 	{@link #transact(Activity aContext, String databaseName, String SQL, Object[] parameters)}
	 * 
	 * @param aContext - The Activity object with which the database is associated.  This is usually the main, or first, Activity of 
	 * the application.
	 * @param databaseName - A string that matches the name of the SQLite file included in the assets directory of the Android application.
	 * @param SQL - An SQL string for a standard SQL statement or a prepared statement used to query the database
	 * @param parameters - An array of Objects to be bound to the ? characters in the SQL string if it is a prepared statement.  If the 
	 * SQL string is not to be used for a prepared statement this parameter should be null.
	 * @return - A DataAccessResult object that contains information regarding any database errors generated during execution, the 
	 * data found as a result of the query, and other helpful pieces of information.
	 * @throws DataAccessException
	 */
	public static DataAccessResult getData(WeakReference<Context> aContextRef, String databaseName, String SQL, Object[] parameters) throws DataAccessException{
		return dbAccess(aContextRef, databaseName, SQL, parameters, false);
	}
	/**
	 * This method is used to execute standard insert, update, etc. SQL statements and matching prepared statements against any SQLite database file included in 
	 * the assets directory of an Android application.
	 * 
	 * @deprecated  Replaced by              
	 * 	{@link #transact(Activity aContext, String databaseName, String SQL, Object[] parameters)}
	 *
	 * @param aContext - The Activity object with which the database is associated.  This is usually the main, or first, Activity of 
	 * the application.
	 * @param databaseName - A string that matches the name of the SQLite file included in the assets directory of the Android application.
	 * @param SQL - An SQL string for a standard SQL statement or a prepared statement used to query the database
	 * @param parameters - An array of Objects to be bound to the ? characters in the SQL string if it is a prepared statement.  If the 
	 * SQL string is not to be used for a prepared statement this parameter should be null.
	 * @return - A DataAccessResult object that contains information regarding any database errors generated during execution
	 *  and other helpful pieces of information.
	 * @throws DataAccessException
	 */
	public static DataAccessResult setData(WeakReference<Context> aContextRef, String databaseName, String SQL, Object[] parameters) throws DataAccessException{
		return dbAccess(aContextRef, databaseName, SQL, parameters, true);
	}
	/**
	 * This method is used to execute standard select, insert, update, etc. SQL statements and matching prepared statements against any SQLite database file included in 
	 * the assets directory of an Android application.
	 * 
	 * @param aContext - The Activity object with which the database is associated.  This is usually the main, or first, Activity of 
	 * the application.
	 * @param databaseName - A string that matches the name of the SQLite file included in the assets directory of the Android application.
	 * @param SQL - An SQL string for a standard SQL statement or a prepared statement used to query the database
	 * @param parameters - An array of Objects to be bound to the ? characters in the SQL string if it is a prepared statement.  If the 
	 * SQL string is not to be used for a prepared statement this parameter should be null.
	 * @return - A DataAccessResult object that contains information regarding any database errors generated during execution
	 *  and other helpful pieces of information.
	 * @throws DataAccessException
	 */
	public static DataAccessResult transact(WeakReference<Context> aContextRef, String databaseName, String SQL, Object[] parameters) throws DataAccessException{
		boolean isChangeData = true;
		if(SQL.toLowerCase().startsWith("select")){
			System.out.println("starts with select");
			isChangeData = false;
		}
		System.out.println("SQL: "+SQL+" isChanging: "+isChangeData);
		return dbAccess(aContextRef, databaseName, SQL, parameters, isChangeData);
	}

	private static DataAccessResult dbAccess(WeakReference<Context> aContextRef, String databaseName, String SQL, Object[] parameters, boolean treatAsChangeData) throws DataAccessException{
		if(databaseName == null){
			return null;
		}
		if(parameters == null){
			parameters = new Object[0];
		}
		boolean startedLocalTransaction = false;
		//System.out.println("getting to database "+databaseName+" with "+SQL);
		SQLiteDatabase aDatabase = generateDataAccessObject(aContextRef, databaseName);

		DataAccessResult aRetResult = new DataAccessResult();
		try {
			System.out.println("after getting to database. Is changing: "+treatAsChangeData);

			if(treatAsChangeData){
				if(DataAccessObject.inTransaction == false){
					DataAccessObject.startTransaction(aContextRef, databaseName);
					startedLocalTransaction = true;
				}
				//System.out.println("changing data with SQL: "+SQL);
				if(parameters.length > 0){
					
					SQLiteStatement aPreparedStatement = aDatabase.compileStatement(SQL);
					int numParams = parameters.length;
					for (int i = 0; i < numParams; i++){
						if(parameters[i] == null){
							aPreparedStatement.bindNull(i);
							continue;
						}
						Class<?> type = parameters[i].getClass();
						if(String.class.isAssignableFrom(type)){
							aPreparedStatement.bindString(i+1, (String)parameters[i]);
						}
						else if(Double.class.isAssignableFrom(type)){
							aPreparedStatement.bindDouble(i+1, (Double)parameters[i]);
						}
						else if(Integer.class.isAssignableFrom(type) || Long.class.isAssignableFrom(type)){
							aPreparedStatement.bindLong(i+1, (Integer)parameters[i]);
						}

						else if(QCBlob.class.isAssignableFrom(type)){
							aPreparedStatement.bindBlob(i+1, ((QCBlob)parameters[i]).bytes());
						}
						else{
							Exception unknownParamType = new Exception("Parameter "+i+ "is anknown Parameter type: "
									+type.getCanonicalName()
									+".  Use only Strings, Doubles, Integers, or QCBlobs as parameters.");
							throw unknownParamType;
						}
					}
					aPreparedStatement.execute();
					aPreparedStatement.close();
					
				}
				else{
					aDatabase.execSQL(SQL);
				}
				//System.out.println("local trans? "+startedLocalTransaction);
				if (startedLocalTransaction) {
					//System.out.println("ending local transaction");
					DataAccessObject.endTransaction(aContextRef, databaseName, true);
				}
				////Log.d(QuickConnectActivity.LOG_TAG, "statement executed");
			}
			else{
				System.out.println("not changing data with this call: "+SQL);
				try {
					String[] parametersAsStrings = new String[parameters.length];
					for(int i = 0; i < parametersAsStrings.length; i++){
						parametersAsStrings[i] = parameters[i].toString();
					}
					System.out.println("Aquiring semaphore");
					DataAccessObject.writeBlockingSemaphore.acquire();
					System.out.println("Got semaphore");
					Cursor aCursor = aDatabase.rawQuery(SQL, (String[])parametersAsStrings);
					//System.out.println("got cursor");
					String[] columnNames = aCursor.getColumnNames();
					aRetResult.setColumnNames(columnNames);
					////Log.d(QuickConnectActivity.LOG_TAG, "column names set");
					ArrayList<ArrayList<String>> results = new ArrayList<ArrayList<String>>();
					//System.out.println("Query results: "+results);
					aRetResult.setResults(results);
					int numColumns = columnNames.length;
					while(aCursor.moveToNext()){
						ArrayList<String> row = new ArrayList<String>();
						results.add(row);

						for(int i = 0; i < numColumns; i++){
							row.add(aCursor.getString(i));
						}
					}
					aCursor.close();
					DataAccessObject.writeBlockingSemaphore.release();
				} catch (Exception e) {
					//System.out.println("error: "+e.getLocalizedMessage());
					aRetResult.setErrorDescription(e.getLocalizedMessage()+" cause: "+e.getCause());
					if (startedLocalTransaction) {
						DataAccessObject.endTransaction(aContextRef, databaseName, false);
					}
				}
			}
		}
		catch(Exception ex){
			ex.printStackTrace();
			aRetResult.setErrorDescription(ex.toString());
			endTransaction(aContextRef, databaseName, false);
		}
		//System.out.println("about to return.");
		return aRetResult;
	}
	/**
	 * This method is used as the beginning boundary of a database transaction.  After this call then any calls to setData or getData will be executed 
	 * as part of a transaction
	 * @param aContext - The Activity object with which the database is associated.  This is usually the main, or first, Activity of 
	 * the application.
	 * @param databaseName - A string that matches the name of the SQLite file included in the assets directory of the Android application.
	 * @throws DataAccessException
	 */
	public static void startTransaction(WeakReference<Context> aContextRef, String databaseName) throws DataAccessException{
		try{
			generateDataAccessObject(aContextRef,databaseName);
			System.out.println("claiming entire semaphore");
			DataAccessObject.writeBlockingSemaphore.acquire(Integer.MAX_VALUE);
			System.out.println("claimed");
		}
		catch(Exception e){
			throw new DataAccessException("Error: unable to start transaction.  "+e.getLocalizedMessage());
		}
		databases.get(databaseName).beginTransaction();
		DataAccessObject.inTransaction = true;

	}

	//if false is passed as the value of the successful boolean
	//the transaction is rolled back.
	/**
	 * This method is used as the ending boundary of a database transaction.  Any setData or getData calls made after the startTransaction method and 
	 * before a call to this method are treated in such a way that they can be rolled back.
	 * @param aContext - The Activity object with which the database is associated.  This is usually the main, or first, Activity of 
	 * the application.
	 * @param databaseName - A string that matches the name of the SQLite file included in the assets directory of the Android application.
	 * @param successful - a boolean value indicating if a roll back of all statements in the transaction should be done.  A value of 
	 * <b>true</b> causes a roll back, <b>false</b> does not.
	 * @throws DataAccessException
	 */
	public static  void endTransaction(WeakReference<Context> aContextRef, String databaseName, boolean successful) throws DataAccessException{
		//System.out.println("ending transaction");
		DataAccessObject.inTransaction = false;
		if(databaseName != null){
			try {
				generateDataAccessObject(aContextRef, databaseName);
			} catch (DataAccessException e) {
				throw new DataAccessException("Error: unable to complete the transaction. "+e.getLocalizedMessage());
			}
			if(successful){
				databases.get(databaseName).setTransactionSuccessful();
			}
			databases.get(databaseName).endTransaction();
			System.out.println("releasing entire semaphore");
			DataAccessObject.writeBlockingSemaphore.release(Integer.MAX_VALUE);
			System.out.println("released");
		}
	}
	/**
	 * This method is used to free up resources required to access a specific SQLite database file.  This method should be used sparingly.  
	 * Do NOT close a database after each use and then reopen it again using a getData or setData call unless you must for memory reasons 
	 * since this will slow down the execution of your application.
	 * @param aContext - The Activity object with which the database is associated.  This is usually the main, or first, Activity of 
	 * the application.
	 * @param databaseName - A string that matches the name of the SQLite file included in the assets directory of the Android application.
	 * @throws DataAccessException
	 */
	public static  void close(WeakReference<Context> aContextRef, String databaseName) throws DataAccessException{
		try {
			generateDataAccessObject(aContextRef, databaseName);
		} catch (DataAccessException e) {
			throw new DataAccessException("Error: unable to close database "+databaseName+". "+e.getLocalizedMessage());
		}
		databases.get(databaseName).close();
	}
	/**
	 * This method is used to free up resources required to access all open SQLite database files.  This method should be used sparingly.  
	 * Do NOT close a databases after each use and then reopen them again using a getData or setData call unless you must for memory reasons 
	 * since this will slow down the execution of your application.
	 */
	public static void closeAll(){
		Collection<SQLiteDatabase> allDatabases = databases.values();
		Iterator<SQLiteDatabase> dbIt = allDatabases.iterator();
		while (dbIt.hasNext()) {
			SQLiteDatabase aDB = dbIt.next();
			aDB.close();
		}
	}

}
