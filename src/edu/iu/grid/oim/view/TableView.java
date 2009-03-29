package edu.iu.grid.oim.view;

import java.util.ArrayList;

public class TableView extends View {
	static public enum CellStyle { NORMAL, HEADER };
	String cls = "";

	public class Row
	{
		public class Cell
		{
			CellStyle style = CellStyle.NORMAL;
			String html;
			Cell(String _html, CellStyle _style) {
				html = _html;
				style = _style;
			}
			public String toHTML()
			{
				switch(style) {
				case NORMAL:
					return "<td class=\"record_data\">"+html+"</td>";
				case HEADER:
					return "<th>"+html+"</th>";
				}
				return null;
			}
		}
		
		private ArrayList<Cell> cells = new ArrayList();
		
		public void addCell(String value) {
			cells.add(new Cell(Utils.strFilter(value), TableView.CellStyle.NORMAL));
		}
		public void addCell(Boolean value) {
			cells.add(new Cell(Utils.boolFilter(value), TableView.CellStyle.NORMAL));
		}
		public void addHeaderCell(String value) {
			cells.add(new Cell(Utils.strFilter(value), TableView.CellStyle.HEADER));
		}
		public void addHtmlCell(String html) {
			cells.add(new Cell(Utils.nullStrFilter(html), TableView.CellStyle.NORMAL));
		}
		public String toHTML()
		{
			String out = "";
			out += "<tr>";
			for(Cell cell : cells) {
				out += cell.toHTML();
			}
			out += "</tr>";
			return out;
		}
	}
	
	private ArrayList<Row> rows = new ArrayList();
	public void setClass(String _cls) {
		cls = _cls;
	}

	public void addRow(Row row)
	{
		rows.add(row);
	}
	
	public void addRow(String header, View view)
	{
		Row row = new Row();
		addRow(row);
		row.addHeaderCell(header);
		row.addHtmlCell(view.toHTML());
	}
	
	public String toHTML() {
		String out = "";
		
		out += "<table class='"+cls+"'>";
		for(Row row : rows) {
			out += row.toHTML();
		}
		
		//display toolbar
		out += "<tr><td></td><td>"+super.toHTML()+"</td></tr>";
		
		out += "</table>";
		return out;
		
	}

}
