package edu.iu.grid.oim.model.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.iu.grid.oim.lib.Action;
import edu.iu.grid.oim.lib.Authorization;
import edu.iu.grid.oim.lib.Authorization.AuthorizationException;
import edu.iu.grid.oim.model.db.record.SCContactRecord;
import edu.iu.grid.oim.model.db.record.VOContactRecord;

public class SCContactModel extends DBModel {
    static Logger log = Logger.getLogger(SCContactModel.class); 
	public static HashMap<Integer/*vo_id*/, ArrayList<SCContactRecord>> cache = null;
	
	public SCContactModel(Connection _con, Authorization _auth) {
		super(_con, _auth);
		// TODO Auto-generated constructor stub
	}
	
	public HashMap<Integer/*type_id*/, ArrayList<Integer/*person_id*/>> get(int sc_id) throws AuthorizationException, SQLException
	{	
		fillCache();
		
		HashMap<Integer, ArrayList<Integer>> list = new HashMap();
		Integer key = new Integer(sc_id);
		if(cache.containsKey(key)) {
			ArrayList<SCContactRecord> recs = cache.get(key);
			for(SCContactRecord rec : recs) {
				//group records by type_id and create lists of person_id
				ArrayList<Integer> a = null;
				if(!list.containsKey(rec.type_id)) {
					//never had this type
					a = new ArrayList<Integer>();
					list.put(rec.type_id, a);
				} else {
					a = list.get(rec.type_id);
				}	
				a.add(rec.person_id);
			}
			return list;
		}
		
		log.warn("Couldn't find any record where sc_id = " + sc_id);
		return list;
	}
	
	synchronized private void fillCache() throws SQLException
	{
		if(cache != null) {
			return;
		}
		
		cache = new HashMap();
		
		String sql = "SELECT * FROM sc_contact";
		PreparedStatement stmt = null;
		stmt = con.prepareStatement(sql); 
		ResultSet rs = stmt.executeQuery();

		while(rs.next()) {
			SCContactRecord rec = new SCContactRecord(rs);
			
			//group records by vo_id and put it in the cache
			ArrayList<SCContactRecord> a = null;
			if(!cache.containsKey(rec.sc_id)) {
				//never had this type
				a = new ArrayList<SCContactRecord>();
				cache.put(rec.sc_id, a);
			} else {
				a = cache.get(rec.sc_id);
			}
			a.add(rec);
		}
	}
}
