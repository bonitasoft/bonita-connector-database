/*
 * Copyright (C) 2009 - 2020 Bonitasoft S.A.
 * Bonitasoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.bonitasoft.connectors.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.bonitasoft.engine.connector.ConnectorException;

/**
 * @author Matthieu Chaffotte
 * @author Baptiste Mesta
 * @author Frédéric Bouquet
 */
public class Database {

    private final Connection connection;

    private Context ctx;

    private Statement selectStatement;

    public Database(final String driver, final String url, final String username, final String password)
            throws ClassNotFoundException, SQLException {
        Class.forName(driver);
        connection = DriverManager.getConnection(url, username, password);
    }

    public Database(final String dataSource, final Properties properties) throws NamingException, SQLException {
        ctx = new InitialContext(properties);
        final DataSource ds = (DataSource) ctx.lookup(dataSource);
        connection = ds.getConnection();
    }

    public void disconnect() throws SQLException, NamingException {
        if (selectStatement != null) {
            selectStatement.close();
            selectStatement = null;
        }
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
        if (ctx != null) {
            ctx.close();
        }
    }

    public ResultSet select(final String query) throws ConnectorException, SQLException {
        if (selectStatement != null) {
            throw new ConnectorException("A Statement is already opened.");
        }
        selectStatement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY);
        return selectStatement.executeQuery(query);
    }

    public boolean executeCommand(final String command) throws SQLException, ConnectorException {
        Statement statement = null;
        boolean isExecuted = false;
        try {
            statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            isExecuted = statement.execute(command);
        } catch (SQLException e) {
            throw new ConnectorException(e);
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
        return isExecuted;
    }

    /**
     * Does not produce an output result. A Statement is created, executed and closed.
     * 
     * @param commands, the list of SQL command to execute
     * @param commit , commit after the batch execution
     * @throws SQLException
     * @throws ConnectorException
     */
    public void executeBatch(final List<String> commands, final boolean commit)
            throws SQLException, ConnectorException {
        Statement statement = null;
        try {
            connection.setAutoCommit(false);
            statement = connection.createStatement();
            for (final String command : commands) {
                statement.addBatch(command);
            }
            statement.executeBatch();

            if (commit) {
                connection.commit();
            }
        } catch (SQLException e) {
            throw new ConnectorException(e);
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

}
