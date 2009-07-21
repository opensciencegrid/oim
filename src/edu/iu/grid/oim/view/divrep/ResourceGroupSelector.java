package edu.iu.grid.oim.view.divrep;

import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import com.webif.divrep.DivRep;
import com.webif.divrep.DivRepEvent;
import com.webif.divrep.DivRepEventListener;
import com.webif.divrep.common.DivRepFormElement;
import com.webif.divrep.common.DivRepSelectBox;
import com.webif.divrep.common.DivRepToggler;

import edu.iu.grid.oim.lib.Authorization;
import edu.iu.grid.oim.lib.StaticConfig;
import edu.iu.grid.oim.model.Context;
import edu.iu.grid.oim.model.db.FacilityModel;
import edu.iu.grid.oim.model.db.ResourceGroupModel;
import edu.iu.grid.oim.model.db.ResourceModel;
import edu.iu.grid.oim.model.db.SiteModel;
import edu.iu.grid.oim.model.db.record.FacilityRecord;
import edu.iu.grid.oim.model.db.record.ResourceGroupRecord;
import edu.iu.grid.oim.model.db.record.ResourceRecord;
import edu.iu.grid.oim.model.db.record.SiteRecord;

public class ResourceGroupSelector extends DivRepSelectBox {

	static Logger log = Logger.getLogger(ResourceGroupSelector.class);  
    private Context context;

    public ResourceGroupSelector(DivRep parent, Context _context) {
		super(parent);
		context = _context;
		
		FacilityModel fmodel = new FacilityModel(context);
		SiteModel smodel = new SiteModel(context);
		try {
			ArrayList<FacilityRecord> frecs = fmodel.getAll();
			for(FacilityRecord frec : frecs) {
				ArrayList<SiteRecord> srecs = smodel.getByFacilityID(frec.id);
				TreeMap<Integer, String> sites = new TreeMap<Integer, String>();
				for(SiteRecord srec : srecs) {
					sites.put(srec.id, srec.name);
				}
				addGroup(frec.name, sites);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}