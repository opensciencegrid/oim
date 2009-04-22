package edu.iu.grid.oim.model.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.apache.log4j.Logger;

import edu.iu.grid.oim.lib.Authorization;
import edu.iu.grid.oim.model.db.record.AuthorizationTypeRecord;
import edu.iu.grid.oim.model.db.record.DNRecord;
import edu.iu.grid.oim.model.db.record.DowntimeActionRecord;
import edu.iu.grid.oim.model.db.record.RecordBase;
import edu.iu.grid.oim.model.db.record.SiteRecord;
import edu.iu.grid.oim.model.db.record.VORecord;

public class DowntimeActionModel extends SmallTableModelBase<DowntimeActionRecord> {
    static Logger log = Logger.getLogger(DowntimeActionModel.class);  
    
    public DowntimeActionModel(Authorization auth) 
    {
    	super(auth, "downtime_action");
    }
    DowntimeActionRecord createRecord() throws SQLException
	{
		return new DowntimeActionRecord();
	}
	public ArrayList<DowntimeActionRecord> getAll() throws SQLException
	{
		ArrayList<DowntimeActionRecord> list = new ArrayList<DowntimeActionRecord>();
		for(RecordBase it : getCache()) {
			list.add((DowntimeActionRecord)it);
		}
		return list;
	}
	public DowntimeActionRecord get(int id) throws SQLException {
		DowntimeActionRecord keyrec = new DowntimeActionRecord();
		keyrec.id = id;
		return get(keyrec);
	}
}