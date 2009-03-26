package edu.iu.grid.oim.model.db;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import edu.iu.grid.oim.lib.Authorization;
import edu.iu.grid.oim.model.db.record.IRecord;

public class DBModel {
    static Logger log = Logger.getLogger(DBModel.class);  
    protected Connection con;
    protected Authorization auth;
    
    public DBModel(Connection _con, Authorization _auth) {
    	con = _con;
    	auth = _auth;
    }
    public static String toXML(ResultSet rs)
    {
    	try {
	        ResultSetMetaData rsmd = rs.getMetaData();
	        int colCount = rsmd.getColumnCount();
	        StringBuffer xml = new StringBuffer();
	        xml.append("<Results>");
	
	        while (rs.next())
	        {
	            xml.append("<Row>");
	
	            for (int i = 1; i <= colCount; i++)
	            {
	                String columnName = rsmd.getColumnName(i);
	                Object value = rs.getObject(i);
	                xml.append("<" + columnName + ">");
	
	                if (value != null)
	                {
	                    xml.append(value.toString().trim());
	                }
	                xml.append("</" + columnName + ">");
	            }
	            xml.append("</Row>");
	        }
	
	        xml.append("</Results>");
	
	        return xml.toString();
    	} catch (SQLException e) {
    		log.error(e.getMessage());
    		return null;
    	}
    }
    
    protected Boolean isAccessibleType(Integer type_id)
    {
    	//In the future, we can restrict access to
    	//only certain type_id
    	return true;
    }
}