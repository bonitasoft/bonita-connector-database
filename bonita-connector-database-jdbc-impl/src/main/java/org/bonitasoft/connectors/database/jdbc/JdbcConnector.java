/**
 * Copyright (C) 2012 BonitaSoft S.A.
 * BonitaSoft, 31 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.connectors.database.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.bonitasoft.connectors.database.Database;
import org.bonitasoft.engine.connector.Connector;
import org.bonitasoft.engine.connector.ConnectorException;
import org.bonitasoft.engine.connector.ConnectorValidationException;

/**
 * @author Matthieu Chaffotte
 * @author Baptiste Mesta
 * @author Frédéric Bouquet
 * @author Romain Bioteau
 */
public class JdbcConnector implements Connector {

	public static final String RESULTSET_OUTPUT = "resultset";

	public static final String SINGLE_RESULT_OUTPUT = "singleResult";

	public static final String ONEROW_NCOL_RESULT_OUTPUT = "oneRowNColResult";

	public static final String NROW_ONECOL_RESULT_OUTPUT = "nRowOneColResult";

	public static final String TABLE_RESULT_OUTPUT = "tableResult";

	public static final String USERNAME = "username";

	public static final String PASSWORD = "password";

	public static final String SCRIPT = "script";

	public static final String SEPARATOR = "separator";

	public static final String DRIVER = "driver";

	public static final String URL = "url";

	public static final String OUTPUT_TYPE = "outputType";

	//Output types
	public static final String SINGLE = "single";
	public static final String N_ROW = "n_row";
	public static final String ONE_ROW = "one_row";
	public static final String TABLE = "table";

	private String url;

	private String userName;

	private String password;

	private String driver;

	private String separator;

	private String script;

	private String outputType;

	private Database database;

	private ResultSet data;

	private Logger LOGGER = Logger.getLogger(this.getClass().getName());

	@Override
	public Map<String, Object> execute() throws ConnectorException {
		if (separator != null) {
			return executeBatch();
		} else {
			return executeSingleQuery();
		}
	}

	@Override
	public void setInputParameters(final Map<String, Object> parameters) {
		userName = (String) parameters.get(USERNAME);
		LOGGER.info(USERNAME + " " + userName);
		final String paswordString = (String) parameters.get(PASSWORD);
		LOGGER.info(PASSWORD + " ******");

		if (paswordString != null && !paswordString.isEmpty()) {
			password = paswordString;
		} else {
			password = null;
		}
		script = (String) parameters.get(SCRIPT);
		LOGGER.info(SCRIPT + " " + script);
		separator = (String) parameters.get(SEPARATOR);
		LOGGER.info(SEPARATOR + " " + separator);
		driver = (String) parameters.get(DRIVER);
		LOGGER.info(DRIVER + " " + driver);
		url = (String) parameters.get(URL);
		LOGGER.info(URL + " " + url);
		outputType = (String) parameters.get(OUTPUT_TYPE);
		LOGGER.info(OUTPUT_TYPE + " " + outputType);
	}

	@Override
	public void validateInputParameters() throws ConnectorValidationException {
		final List<String> messages = new ArrayList<String>(0);
		if (url == null || url.isEmpty()) {
			messages.add("Url can't be empty");
		}
		if (driver == null || driver.isEmpty()) {
			messages.add("Driver is not set");
		}
		if (script == null || script.isEmpty()) {
			messages.add("Script is not set");
		}

		if (!messages.isEmpty()) {
			throw new ConnectorValidationException(this, messages);
		}
	}

	@Override
	public void connect() throws ConnectorException {
		try {
			database = new Database(driver, url, userName, password);
		} catch (final Exception e) {
			throw new ConnectorException(e);
		}
	}

	@Override
	public void disconnect() throws ConnectorException {
		if (script.toUpperCase().trim().startsWith("SELECT")) {
			try {
				if(data != null){
					data.close();
				}
			} catch (Exception e) {
				throw new ConnectorException(e);
			}
		}

		if (database != null) {
			try {
				database.disconnect();
			} catch (final Exception e) {
				throw new ConnectorException(e);
			}
		}
	}

	private Map<String, Object> executeSingleQuery() throws ConnectorException {
		try {
			final String command = script.toUpperCase().trim();
			final Map<String, Object> result = new HashMap<String, Object>(2);
			if (command.startsWith("SELECT")) {
				data = database.select(script);
				if(SINGLE.equals(outputType)){
					handleSingleResult(data,result);
				}else if(N_ROW.equals(outputType)){
					handleNRowResult(data,result);
				}else if(ONE_ROW.equals(outputType)){
					handleOneRowResult(data,result);
				}else if(TABLE.equals(outputType)){
					handleTableResult(data,result);
				}else{
					result.put(RESULTSET_OUTPUT, data);
				}
			} else {
				database.executeCommand(script);
				result.put(RESULTSET_OUTPUT, null);
			}
			return result;
		} catch (final SQLException sqle) {
			throw new ConnectorException(sqle);
		}
	}

	protected void handleTableResult(ResultSet rSet, Map<String, Object> result) throws SQLException {
		final List<List<Object>> resultTable = new ArrayList<List<Object>>();
		int maxColumn = rSet.getMetaData().getColumnCount()+1;
		while(rSet.next()){
			final List<Object> row = new ArrayList<Object>();
			for(int colIndex = 1;  colIndex < maxColumn ; colIndex++){
				row.add(rSet.getObject(colIndex));
			}
			resultTable.add(row);
		}
		result.put(TABLE_RESULT_OUTPUT,resultTable);
		rSet.close();
	}

	protected void handleOneRowResult(ResultSet rSet, Map<String, Object> result) throws SQLException, ConnectorException {
		final List<Object> resultList = new ArrayList<Object>();
		if(rSet.first()){
			if(rSet.isLast()){
				int maxColumn = rSet.getMetaData().getColumnCount()+1;
				for(int colIndex = 1;  colIndex < maxColumn ; colIndex++){
					resultList.add(rSet.getObject(colIndex));
				}
				result.put(ONEROW_NCOL_RESULT_OUTPUT,resultList);
			}else{
				rSet.close();
				throw new ConnectorException("One row N columns result output mode is not compatible with execucted query (invalid number of rows in resultset):\n"+script);
			}
		}else{
			result.put(ONEROW_NCOL_RESULT_OUTPUT, resultList);
		}
		rSet.close();
	}

	protected void handleNRowResult(ResultSet rSet, Map<String, Object> result) throws SQLException, ConnectorException {
		int colCount = rSet.getMetaData().getColumnCount();
		if(colCount != 1){
			rSet.close();
			throw new ConnectorException("N rows one column result output mode is not compatible with execucted query (invalid number of columns in resultset):\n"+script);
		}
		final List<Object> resultList = new ArrayList<Object>();
		while (rSet.next()) {
			resultList.add(rSet.getObject(1));
		}
		result.put(NROW_ONECOL_RESULT_OUTPUT,resultList);
		rSet.close();
	}

	protected void handleSingleResult(ResultSet rSet, Map<String, Object> result) throws SQLException, ConnectorException {
		int colCount = rSet.getMetaData().getColumnCount();
		if(colCount != 1){
			rSet.close();
			throw new ConnectorException("Single result output mode is not compatible with execucted query (invalid number of columns in resultset):\n"+script);
		}
		if(rSet.first()){
			result.put(SINGLE_RESULT_OUTPUT, rSet.getObject(1));
		}else{
			result.put(SINGLE_RESULT_OUTPUT, null);
		}
		rSet.close();
	}

	private Map<String, Object> executeBatch() throws ConnectorException {
		final List<String> commands = getScriptCommands();
		try {
			database.executeBatch(commands, true);
			Map<String, Object> result = new HashMap<String, Object>();
			result.put(RESULTSET_OUTPUT, null);
			return result;
		} catch (final Exception e) {
			throw new ConnectorException(e);
		}
	}

	private List<String> getScriptCommands() {
		final List<String> commands = new ArrayList<String>();
		final StringTokenizer tokenizer = new StringTokenizer(script, separator);
		while (tokenizer.hasMoreTokens()) {
			final String command = tokenizer.nextToken();
			commands.add(command.trim());
		}
		return commands;
	}
}
