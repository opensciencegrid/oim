package edu.iu.grid.oim.model.db;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;

import edu.iu.grid.oim.lib.Authorization;
import edu.iu.grid.oim.lib.Authorization.AuthorizationException;
import edu.iu.grid.oim.model.db.record.VOContactRecord;

public class VOContactModel extends DBModel {
    static Logger log = Logger.getLogger(VOContactModel.class); 
	public static HashMap<Integer/*vo_id*/, ArrayList<VOContactRecord>> cache = null;
	
	public VOContactModel(Connection _con, Authorization _auth) {
		super(_con, _auth);
		// TODO Auto-generated constructor stub
	}
	
	public HashMap<Integer/*type_id*/, ArrayList<VOContactRecord>> get(Integer vo_id) throws AuthorizationException, SQLException
	{	
		fillCache();

		HashMap<Integer, ArrayList<VOContactRecord>> list = new HashMap();
		if(cache.containsKey(vo_id)) {
			ArrayList<VOContactRecord> recs = cache.get(vo_id);
			for(VOContactRecord rec : recs) {
				//group records by type_id and create lists of contact_id
				ArrayList<VOContactRecord> array = null;
				if(!list.containsKey(rec.contact_type_id)) {
					//never had this type
					array = new ArrayList<VOContactRecord>();
					list.put(rec.contact_type_id, array);
				} else {
					array = list.get(rec.contact_type_id);
				}	
				array.add(rec);
			}
			return list;
		}
		
		log.warn("Couldn't find any record where vo_id = " + vo_id);
		return list;
	}
	
	private void fillCache() throws SQLException
	{
		if(cache == null) {
			cache = new HashMap();
			
			String sql = "SELECT * FROM vo_contact order by contact_rank_id";
			PreparedStatement stmt = con.prepareStatement(sql); 
			ResultSet rs = stmt.executeQuery();
	
			while(rs.next()) {
				VOContactRecord rec = new VOContactRecord(rs);
				
				//group records by vo_id and put it in the cache
				ArrayList<VOContactRecord> a = null;
				if(!cache.containsKey(rec.vo_id)) {
					//never had this type
					a = new ArrayList<VOContactRecord>();
					cache.put(rec.vo_id, a);
				} else {
					a = cache.get(rec.vo_id);
				}
				a.add(rec);
			}
		}
	}
    public void emptyCache() //used after we do insert/update
    {
   		cache = null;
    }

	public void update(Integer vo_id, ArrayList<VOContactRecord> contactRecords) throws AuthorizationException, SQLException 
	{
		auth.check("write_vocontact");
	
		String logstr = "";
		con.setAutoCommit(false);

		//remove all current contacts
		try {
			String sql = "DELETE FROM vo_contact where vo_id = ?";
			PreparedStatement stmt = con.prepareStatement(sql);
			stmt.setInt(1, vo_id);
			stmt.executeUpdate();
			logstr += stmt.toString()+"\n";
		} catch (SQLException e) {
			con.rollback();
			log.error("Failed to remove previous records for vo_id: " + vo_id);
			throw new SQLException(e);
		}
		
		//insert new contact records in batch
		try {
			String sql = "INSERT INTO vo_contact (contact_id, vo_id, contact_type_id, contact_rank_id)"+
			" VALUES (?, ?, ?, ?)";
			PreparedStatement stmt = con.prepareStatement(sql); 
			
			for(VOContactRecord rec : contactRecords) {
				stmt.setInt(1, rec.contact_id);
				stmt.setInt(2, vo_id);
				stmt.setInt(3, rec.contact_type_id);
				stmt.setInt(4, rec.contact_rank_id);
				stmt.addBatch();
				logstr += stmt.toString()+"\n";
			}
			
			stmt.executeBatch();
			
		} catch (BatchUpdateException e) {
			con.rollback();
			log.error("Failed to insert new records for vo_id: " + vo_id);
			throw new SQLException(e);
		} 
		
		con.commit();
		con.setAutoCommit(true);
		
		LogModel lmodel = new LogModel(con, auth);
		lmodel.insert("update_vocontact", vo_id, logstr);
				
		emptyCache();
	}
}
