package edu.iu.grid.oim.model.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.log4j.Logger;

import com.webif.divex.form.CheckBoxFormElementDE;

import edu.iu.grid.oim.lib.Authorization;
import edu.iu.grid.oim.lib.Authorization.AuthorizationException;
import edu.iu.grid.oim.model.db.record.NotificationRecord;
import edu.iu.grid.oim.model.db.record.RecordBase;
import edu.iu.grid.oim.model.db.record.SCContactRecord;
import edu.iu.grid.oim.model.db.record.SCRecord;
import edu.iu.grid.oim.model.db.record.SCContactRecord;
import edu.iu.grid.oim.model.db.record.VOFieldOfScienceRecord;
import edu.iu.grid.oim.model.db.record.SCRecord;
import edu.iu.grid.oim.model.db.record.VOVORecord;
public class SCModel extends SmallTableModelBase<SCRecord> {
    static Logger log = Logger.getLogger(SCModel.class);  
    
    public SCModel(Authorization auth) 
    {
    	super(auth, "sc");
    }    
    SCRecord createRecord(ResultSet rs) throws SQLException
	{
		return new SCRecord(rs);
	}

	public Collection<SCRecord> getAllEditable() throws SQLException
	{		
		ArrayList<SCRecord> list = new ArrayList();
	    if(auth.allows("admin")) {
	    	//admin can edit all scs
	    	for(RecordBase rec : getCache()) {
	    		list.add((SCRecord)rec);
	    	}
	    } else {
	    	//only select record that is editable
		    for(RecordBase rec : getCache()) {
		    	SCRecord screc = (SCRecord)rec;
		    	if(canEdit(screc.id)) {
		    		list.add(screc);
		    	}
		    }	    	
	    }
	    return list;
	}
	public boolean canEdit(int id)
	{
		try {
			HashSet<Integer> ints = getEditableIDs();
			if(ints.contains(id)) return true;
		} catch (SQLException e) {
			//TODO - something?
		}
		return false;
	}
	
	//returns all record id that the user has access to
	private HashSet<Integer> getEditableIDs() throws SQLException
	{
		HashSet<Integer> list = new HashSet<Integer>();
		ResultSet rs = null;

		PreparedStatement stmt = null;

		String sql = "SELECT * FROM sc_contact WHERE contact_id = ?";
		stmt = getConnection().prepareStatement(sql); 
		stmt.setInt(1, auth.getContactID());

		rs = stmt.executeQuery();
		while(rs.next()) {
			SCContactRecord rec = new SCContactRecord(rs);
			list.add(rec.sc_id);
		}
		
		return list;
	}
	
	public SCRecord get(int id) throws SQLException {
		SCRecord keyrec = new SCRecord();
		keyrec.id = id;
		return get(keyrec);
	}
	
	public ArrayList<SCRecord> getAll() throws SQLException
	{
		ArrayList<SCRecord> list = new ArrayList<SCRecord>();
		for(RecordBase it : getCache()) {
			list.add((SCRecord)it);
		}
		return list;
	}
	
	public void insertDetail(SCRecord rec, 
			ArrayList<SCContactRecord> contacts) throws Exception
	{
		try {
			auth.check("edit_my_sc");
			
			//process detail information
			getConnection().setAutoCommit(false);
			
			//insert SC itself and get the new ID
			insert(rec);
			
			//process contact information
			SCContactModel cmodel = new SCContactModel(auth);
			//reset sc_id on all contact records
			for(SCContactRecord sccrec : contacts) {
				sccrec.sc_id = rec.id;
			}
			cmodel.update(cmodel.getBySCID(rec.id), contacts);
			getConnection().commit();
			getConnection().setAutoCommit(true);
		} catch (AuthorizationException e) {
			log.error(e);
			log.info("Rolling back SC insert transaction.");
			getConnection().rollback();
			getConnection().setAutoCommit(true);
			
			//re-throw original exception
			throw new Exception(e);
		} catch (SQLException e) {
			log.error(e);
			log.info("Rolling back SC insert transaction.");
			getConnection().rollback();
			getConnection().setAutoCommit(true);
			
			//re-throw original exception
			throw new Exception(e);
		}	
	}
	
	public void updateDetail(SCRecord rec,
			ArrayList<SCContactRecord> contacts) throws Exception
	{
		// update to DB
		try {
			auth.check("edit_my_sc");
			
			//process detail information
			getConnection().setAutoCommit(false);
			
			update(get(rec), rec);
			
			//process contact information
			SCContactModel cmodel = new SCContactModel(auth);
			//reset vo_id on all contact records
			for(SCContactRecord sccrec : contacts) {
				sccrec.sc_id = rec.id;
			}
			cmodel.update(cmodel.getBySCID(rec.id), contacts);
			
			getConnection().commit();
			getConnection().setAutoCommit(true);
		} catch (AuthorizationException e) {
			log.error(e);
			log.info("Rolling back SC update-insert transaction.");
			getConnection().rollback();
			getConnection().setAutoCommit(true);
			
			//re-throw original exception
			throw new Exception(e);
		} catch (SQLException e) {
			log.error(e);
			log.info("Rolling back SC update-insert transaction.");
			getConnection().rollback();
			getConnection().setAutoCommit(true);
			
			//re-throw original exception
			throw new Exception(e);
		}			
	}
		
}

