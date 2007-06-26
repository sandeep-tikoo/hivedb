/**
 * HiveDB is an Open Source (LGPL) system for creating large, high-transaction-volume
 * data storage systems.
 */
package org.hivedb.meta.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.hivedb.HiveRuntimeException;
import org.hivedb.meta.HiveSemaphore;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

/**
 * @author Justin McCarthy (jmccarthy@cafepress.com)
 * @author Britt Crawford (bcrawford@cafepress.com)
 */
public class HiveSemaphoreDao extends JdbcDaoSupport {
	public HiveSemaphoreDao(DataSource ds) {
		this.setDataSource(ds);
	}

	//Changed to delegate but kept the method to perserve the interface
	public HiveSemaphore get() {
		return getSemaphore();
	}

	private HiveSemaphore getSemaphore() {
		JdbcTemplate t = getJdbcTemplate();
		HiveSemaphore result;
		try {
			result = (HiveSemaphore) t.queryForObject("SELECT * FROM semaphore_metadata", new HiveSemaphoreRowMapper());
		} catch (BadSqlGrammarException ex) {
			throw new HiveSemaphoreNotFound(
					"Exception loading HiveSemaphore -- verify that semaphore_metadata has one and only one row: "
							+ ex.getMessage());
		} catch (EmptyResultDataAccessException ex) {
			throw new HiveSemaphoreNotFound(
					"Exception loading HiveSemaphore -- verify that semaphore_metadata has one and only one row: "
							+ ex.getMessage());
		}
		return result;
	}

	public HiveSemaphore create() {
		HiveSemaphore hs;
		if(doesHiveSemaphoreExist())
			hs = get();
		else {
			JdbcTemplate j = getJdbcTemplate();
			hs = new HiveSemaphore(false,1);
			Object[] parameters = new Object[] { hs.isReadOnly() ? 1 : 0, hs.getRevision() };
			try {
				j.update("INSERT INTO semaphore_metadata (read_only,revision) VALUES (?,?)",parameters);
			} catch(BadSqlGrammarException e) {
				throw new HiveSemaphoreNotFound(e.getMessage());
			}
		}
		return hs;
	}

	/***************************************************************************
	 * Update HiveSemaphore. Will perform a single attempt to create the
	 * semaphore if any Exception is encountered.
	 * 
	 * @param hs
	 */
	public HiveSemaphore update(HiveSemaphore hs) {
		//Unilateral decision to abandon implicit creation
		Object[] parameters = new Object[] { hs.isReadOnly() ? 1 : 0,
				hs.getRevision() };
		JdbcTemplate j = getJdbcTemplate();
		try {
			int rows = j.update("UPDATE semaphore_metadata SET read_only = ?, revision = ?",parameters);
			if(rows != 1)
				throw new IllegalStateException("Hive semaphore contians more than one row and has been corrupted.");
		} catch (BadSqlGrammarException e) {
			throw new HiveSemaphoreNotFound(e.getMessage());
		}
		return hs;
	}

	public boolean doesHiveSemaphoreExist() {
		boolean exists;
		try {
			get();
			exists = true;
		} catch(HiveSemaphoreNotFound e) {
			exists = false;
		}
		return exists;
	}
	
	protected class HiveSemaphoreRowMapper implements RowMapper {
		public Object mapRow(ResultSet rs, int rowNumber) throws SQLException {
			return new HiveSemaphore(
					rs.getInt("read_only") == 0 ? false : true, rs
							.getInt("revision"));
		}
	}

	public void incrementAndPersist() {
		HiveSemaphore hs = get();
		hs.incrementRevision();
		update(hs);
	}
	
	public class HiveSemaphoreNotFound extends HiveRuntimeException{
		private static final long serialVersionUID = 7237048097222555154L;

		public HiveSemaphoreNotFound(String msg) {
			super(msg);
		}
	}
}
