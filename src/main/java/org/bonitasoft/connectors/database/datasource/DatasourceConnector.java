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
package org.bonitasoft.connectors.database.datasource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
 */
public class DatasourceConnector implements Connector {

    private static final Logger LOGGER = Logger.getLogger(DatasourceConnector.class.getName());

    public static final String DATASOURCE_INPUT = "dataSourceName";
	public static final String SCRIPT_INPUT = "script";
	public static final String SEPARATOR_INPUT = "separator";
	public static final String PROPERTIES_INPUT = "properties";

	private String datasource;
	private String script;
	private String separator;
	private Properties properties;
	private Database database;
	private ResultSet resultSet;


	@Override
	public Map<String, Object> execute() throws ConnectorException {
		if (hasMultipleQuery()) {
			return executeBatch();
		} else {
			return executeSingleQuery();
		}
	}

    private boolean hasMultipleQuery() {
        return separator != null;
    }

	@Override
	public void setInputParameters(final Map<String, Object> parameters) {
		datasource = (String) parameters.get(DATASOURCE_INPUT);
		LOGGER.info(DATASOURCE_INPUT + " " + datasource);
		script = (String) parameters.get(SCRIPT_INPUT);
		LOGGER.info(SCRIPT_INPUT + " " + script);
		separator = (String) parameters.get(SEPARATOR_INPUT);
		LOGGER.info(SEPARATOR_INPUT + " " + separator);

        @SuppressWarnings("unchecked")
        List<List<Object>> propertiesList = (List<List<Object>>) parameters.get(PROPERTIES_INPUT);
		if (propertiesList != null) {
			properties = new Properties();
			for (List<Object> line : propertiesList) {
				if(line.size() == 1){
					LOGGER.info("Property " + line.get(0) + " null");
					properties.put(line.get(0), null);
				}else if(line.size() == 2){
					LOGGER.info("Property " + line.get(0) + " " + line.get(1));
					properties.put(line.get(0), line.get(1));
				}//else ignore line
			}
		}
	}

	@Override
	public void validateInputParameters() throws ConnectorValidationException {
        final List<String> messages = new ArrayList<>(0);
		if (datasource == null || datasource.isEmpty()) {
			messages.add("Datasource can't be empty");
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
			database = new Database(datasource, properties);
		} catch (final Exception e) {
			throw new ConnectorException(e);
		}
	}

	@Override
	public void disconnect() throws ConnectorException {
		if (resultSet != null) {
			try {
				resultSet.close();
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
            final Map<String, Object> result = new HashMap<>(2);
			if (command.startsWith("SELECT")) {
				resultSet = database.select(script);
				result.put("resultset", resultSet);
			} else {
				database.executeCommand(script);
			}
			return result;
		} catch (final SQLException sqle) {
			throw new ConnectorException(sqle);
		}
	}

	private Map<String, Object> executeBatch() throws ConnectorException {
		final List<String> commands = getScriptCommands();
		try {
			database.executeBatch(commands, true);
			return null;
		} catch (final Exception e) {
			throw new ConnectorException(e);
		}
	}

	private List<String> getScriptCommands() {
        final List<String> commands = new ArrayList<>();
		final StringTokenizer tokenizer = new StringTokenizer(script, separator);
		while (tokenizer.hasMoreTokens()) {
			final String command = tokenizer.nextToken();
			commands.add(command.trim());
		}
		return commands;
	}
}
