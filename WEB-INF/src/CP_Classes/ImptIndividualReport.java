package CP_Classes;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import CP_Classes.common.ConnectionBean;
import CP_Classes.vo.voCluster;
import CP_Classes.vo.voCompetency;
import CP_Classes.vo.voKeyBehaviour;
import CP_Classes.vo.voRatingResult;
import CP_Classes.vo.votblAssignment;
import CP_Classes.vo.votblRelationHigh;
import CP_Classes.vo.votblScaleValue;
import CP_Classes.vo.votblSurveyRating;
import CP_Classes.Translate;
import CP_Classes.AdditionalQuestionController;
import CP_Classes.AdditionalQuestion;

import com.sun.star.beans.XPropertySet;
import com.sun.star.chart.XChartDocument;
import com.sun.star.container.XIndexAccess;
import com.sun.star.document.XEmbeddedObjectSupplier;
import com.sun.star.drawing.XShape;
import com.sun.star.frame.XController;
import com.sun.star.frame.XModel;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.table.XTableChart;
import com.sun.star.table.XTableCharts;
import com.sun.star.table.XTableChartsSupplier;

import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XInterface;


/**
 * This class implements all the operations for Individual Report in Excel.
 * It implements OpenOffice API.
 */

/*
 *CHANGE LOG
 *====================================================================================================================
 *	Date		Modified by			Method(s)							Reason
 *====================================================================================================================
 *	04/07/2012	Albert				All									Created this java file for MOE Importance Report
 *
 *	05/07/2012 	Albert				Report()							Modify templateName part so its properly set to the correct template chosen
 *
 *	17/07/2012	Albert				insertRatingScale()					Enable generating the rating scale in the report in a table
 *
 *	17/07/2012	Albert				insertRatingScaleList()				Enable generating the rating scale in the report in list type
 *	
 *  25/07/2012  Liu Taichen         InsertAdditionalQuestions()			Added pagebreakers		
 */	

/*****
 * 
 * Edited By Roger 13 June 2008
 * Add additional orgId when calling sendMail
 *
 */
public class ImptIndividualReport
{ 	
	private NumberFormat formatter = new DecimalFormat("#0.00");
	private Calculation C;
	private Questionnaire Q;
	private OpenOffice OO;
	private GlobalFunc G;
	private SurveyResult SR;
	private RaterRelation RR;
	private ExcelQuestionnaire EQ;
	private MailHTMLStd EMAIL;
	private Setting ST;
	private RatingScale rscale;
	private Translate trans = new Translate();

	private Vector vGapSorted;	// this is to store the gap of each competency so does not need to reopen another resultset
	private Vector vGapUnsorted;
	private Vector vCompID;
	private Vector vCompName;
	private Vector vCPValues;	//add to store CP values of each competency for sorting , Mark Oei 16 April 2010

	// These 4 vectors below are for Development Map
	private Vector Q1 = new Vector();
	private Vector Q2 = new Vector();
	private Vector Q3 = new Vector();
	private Vector Q4 = new Vector();

	private int surveyID;
	private int targetID;
	private int iPastSurveyID;		// For Toyota combined report
	private	int iPastTargetLogin;	// For Toyota combined report
	private int iCancel = 0;		// If user cancelled the printing process. 0=Not cancelled, 1=Cancelled
	private String surveyInfo [];
	private int arrN []; //To print N (No of Raters) for Simplified report

	private final int BGCOLOR = 12632256;
	private final int BGCOLORCLUSTER = 16774400;
	private final int BGCOLORIMPORTANCE = 16774400;
	private final int ROWHEIGHT = 560;

	private XMultiComponentFactory xRemoteServiceManager = null;
	private XComponent xDoc = null;
	private XSpreadsheet xSpreadsheet0 = null;
	private XSpreadsheet xSpreadsheet = null;
	private XSpreadsheet xSpreadsheet2 = null;
	private String storeURL;

	private int row;
	private int column;
	private int startColumn;
	private int endColumn;
	private int iReportType; //1=Simplified Report "No Competencies charts", 2=Standard Report
	private int iNoCPR = 1;	 //0=CPR is chosen for survey, 1=No CPR chosen for survey
	private int totalColumn = 12;

	private int splitOthers = 0;//0="Others" 1="Subordinates" and "Peers"
	private int CPRorFPR = 1; //1=CPR, 2=FPR
    
	private int lastPageRowCount = 0; //For keeping a record of the last page row count
	private boolean isGroupCPLine = false; //For displaying/not displaying Group CP Line

	private String language = ""; //To track the current language
	private int templateLanguage = 0;
    private int format = 0;
    private int pageCount = 10;
	/**
	 * Creates a new instance of IndividualReport object.
	 */
	public ImptIndividualReport()
	{
		ST 	= new Setting();
		C 	= new Calculation();
		Q 	= new Questionnaire();
		OO	= new OpenOffice();
		G	= new GlobalFunc();
		SR	= new SurveyResult();
		RR  = new RaterRelation();
		EQ  = new ExcelQuestionnaire();
		ST	= new Setting();
		EMAIL = new MailHTMLStd();
		rscale = new RatingScale();

		vGapSorted = new Vector();
		vGapUnsorted = new Vector();
		vCompID = new Vector();
		vCompName = new Vector();
		vCPValues = new Vector(); // instantiate vCPValues object, Mark Oei 16 April 2010

		startColumn = 0;
		endColumn = 12;
	}

	/**
	 * Retrieves the survey details and stores in an array.
	 * Modified by Albert (27 June 2012) to include useCluster
	 */
	public String [] SurveyInfo() throws SQLException
	{
		String [] info = new String[10];

		String query = "SELECT tblSurvey.LevelOfSurvey, tblJobPosition.JobPosition, tblSurvey.AnalysisDate, ";
		query = query + "[User].FamilyName, [User].GivenName, tblOrganization.NameSequence, tblSurvey.SurveyName, ";
		query = query + "tblOrganization.OrganizationName, tblOrganization.OrganizationLogo , tblSurvey.useCluster FROM ";
		query = query + "tblSurvey INNER JOIN tblJobPosition ON ";
		query = query + "tblSurvey.JobPositionID = tblJobPosition.JobPositionID INNER JOIN ";
		query = query + "tblAssignment ON tblSurvey.SurveyID = tblAssignment.SurveyID INNER JOIN ";
		query = query + "[User] ON tblAssignment.TargetLoginID = [User].PKUser INNER JOIN ";
		query = query + "tblOrganization ON tblSurvey.FKOrganization = tblOrganization.PKOrganization ";
		query = query + "WHERE tblSurvey.SurveyID = " + surveyID;
		query = query + " AND tblAssignment.TargetLoginID = " + targetID;

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			if(rs.next()) {
				for(int i=0; i<10; i++) {
					info[i] = rs.getString(i+1);		

				}
			}

		}catch(Exception ex){
			System.out.println("IndividualReport.java - SurveyInfo - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return info;
	}	

	/**
	 * 	Retrieves the survey details and stores in an array (For Toyota combined report)
	 *
	 *	@return Array String	Survey Details (Group, Dept, Job Pos., Rater Name etc...)
	 */
	public String [] SurveyInfoToyota() throws SQLException
	{
		String [] info = new String[11];

		String query = "SELECT Surv.LevelOfSurvey, JobPos.JobPosition, [User].IDNumber, [User].FamilyName, [User].GivenName, ";
		query = query + "Org.NameSequence, Surv.SurveyName, Org.OrganizationName, Dept.DepartmentName, Grp.GroupName, Surv.JobPositionID ";
		query = query + "FROM tblSurvey Surv INNER JOIN ";
		query = query + "tblJobPosition JobPos ON Surv.JobPositionID = JobPos.JobPositionID INNER JOIN ";
		query = query + "tblAssignment Assign ON Surv.SurveyID = Assign.SurveyID INNER JOIN ";
		query = query + "[User] ON Assign.TargetLoginID = [User].PKUser INNER JOIN ";
		query = query + "tblOrganization Org ON Surv.FKOrganization = Org.PKOrganization INNER JOIN ";
		query = query + "tblConsultingCompany Comp ON Surv.FKCompanyID = Comp.CompanyID AND [User].FKCompanyID = Comp.CompanyID AND ";
		query = query + "Org.FKCompanyID = Comp.CompanyID INNER JOIN [Group] Grp ON [User].Group_Section = Grp.PKGroup INNER JOIN ";
		query = query + "Department Dept ON [User].FKDepartment = Dept.PKDepartment AND Org.PKOrganization = Dept.FKOrganization ";
		query = query + "WHERE Surv.SurveyID = " + surveyID;
		query = query + " AND Assign.TargetLoginID = " + targetID;

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			if(rs.next()) {


				for(int i=0; i<11; i++)
					info[i] = rs.getString(i+1);										

			}

		}catch(Exception ex){
			System.out.println("IndividualReport.java - SurveyInfoToyota - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return info;
	}

	/**
	 * Initializes all processes dealing with Survey.
	 */
	public void InitializeSurvey(int surveyID, int targetID, String fileName) throws SQLException, IOException
	{
		//System.out.println("Initialize Survey");

		column = 0;
		this.surveyID = surveyID;
		this.targetID = targetID;

		surveyInfo = new String [10];
		surveyInfo = SurveyInfo();

		//System.out.println("Initialize Survey Completed");
	}	

	/**
	 * 	Initializes all processes dealing with Survey. (For Toyota combined report)
	 */
	public void InitializeSurveyToyota(int surveyID, int targetID, String fileName) throws SQLException, IOException
	{
		column = 0;
		this.surveyID = surveyID;
		this.targetID = targetID;

		surveyInfo = new String [10];
		surveyInfo = SurveyInfoToyota();		
	}

	/**
	 * Get the username based on the name sequence.
	 */
	public String UserName() 
	{
		String name = "";

		int nameSeq = Integer.parseInt(surveyInfo[5]);	//0=familyname first

		String familyName = surveyInfo[3];
		String GivenName = surveyInfo[4];

		if(nameSeq == 0)
			name = familyName + " " + GivenName;
		else
			name = GivenName + " " + familyName;

		return name;		
	}
	
	/*
	 * This method insert the rating scale into the individual report
	 * @author: Liu Taichen
	 * created on: 17/July/2012
	 * modified by: Albert (18 July 2012)
	 * changes: add new paragraph and dynamic placement of rating scale following the template
	 */
	public void InsertRatingScaleList() throws Exception{
		int address[] = OO.findString(xSpreadsheet, "<Rating Scale>");
		OO.findAndReplace(xSpreadsheet, "<Rating Scale>", "");
		int iColumn = address[0];
		int iRow = address[1];
		System.out.println("Printing Rating Scale.");
		//row and colume specify where the rating scale is to be inserted/
		try{ 

			//row = 88;
			//column = 0;
			ExcelQuestionnaire eq = new ExcelQuestionnaire();

			int totalColumn = 12;
			int maxScale = eq.maxScale(surveyID) + 1;

			Vector v = eq.SurveyRating(surveyID);
			int count = 0;

			try{OO.insertRows(xSpreadsheet, 0, totalColumn, iRow, iRow+1, 1, 1);
			}
			catch(Exception e){
				System.out.println("it's here");
			}

			for(int i=0; i<1; i++) {
				votblSurveyRating vo = (votblSurveyRating)v.elementAt(i);
				count++;

				boolean hideNA = Q.getHideNAOption(surveyID);	
				String code = vo.getRatingCode();
				String ratingTask = vo.getRatingTaskName();
				int scaleID = vo.getScaleID();

				Vector RS = Q.getRatingScale(scaleID);
				String paragraph = "All the graphs in this report with a "+(RS.size()-1)+" point scale use the following rating scale description: ";
				OO.insertString(xSpreadsheet, paragraph, iRow, startColumn);
				//OO.mergeCells(xSpreadsheet, startColumn, totalColumn, iRow, iRow );
				
				for(int j=0; j<RS.size(); j++) {
					String [] sRS = new String[3];

					sRS = (String[])RS.elementAt(j);

					int low = Integer.parseInt(sRS[0]);
					int high = Integer.parseInt(sRS[1]);
					String desc = sRS[2];
					//Denise 29/12/2009 to hide NA if required
					if (!(hideNA && (desc.equalsIgnoreCase("NA") || desc.equalsIgnoreCase("N/A") || desc.equals("Not applicable")
							|| desc.contains("NA") || desc.contains("N/A")|| desc.contains("Not applicable") ||desc.contains("Not Applicable"))))
					{


						iRow += 1;
						column = 0;
						OO.insertRows(xSpreadsheet, 0, totalColumn, iRow, iRow+1, 1, 1);
						String temp = "";

						while(low <= high) {						
							if(low > 1)
								temp += "    ";
							temp = temp + Integer.toString(low);

							low++;						
						}
						OO.insertString(xSpreadsheet, (temp + "  -  " + desc).trim(), iRow, iColumn);	// add in scale description
						OO.setCellAllignment(xSpreadsheet, iColumn, totalColumn, iRow, iRow, 2, 2);
						//	OO.setCellAllignment(xSpreadsheet, column, totalColumn, row, row, 2, 2);
						OO.mergeCells(xSpreadsheet, 0, totalColumn, iRow, iRow);
					}//end if to insert Rating scale
				}			
			}	
			//insertPageBreak(xSpreadsheet, 1, 12, row + 2);		
		}
		catch(Exception e){
			System.out.println(e);
		}
		//row = iRow;
		//column = iColumn;		
	}
	
	public void InsertRatingScale(){
		int iRow = row;
		int iColumn =column;

		try{ 


			row = 60;
			column = 1;
			ExcelQuestionnaire eq = new ExcelQuestionnaire();

			int totalColumn = 12;
			int maxScale = eq.maxScale(surveyID) + 1;


			//int totalCells = totalColumn / maxScale;
			int totalCells = 2;
			int totalMerge = 0;		// total cells to be merged after rounding
			double merge = 0;		// total cells to be merged before rounding
			Vector v = eq.SurveyRating(surveyID);
			int count = 0;

			int[] scale = new int[2];
			scale[0] = 0;
			scale[1] = 0;

			try{OO.insertRows(xSpreadsheet, 0, totalColumn, row, row+1, 1, 1);
			}
			catch(Exception e){
				System.out.println("it's here");
			}
			OO.insertString(xSpreadsheet, "Rating Scales used in this survey:", row, column);
			OO.setFontBold(xSpreadsheet, column, column, row, row);
			OO.mergeCells(xSpreadsheet, column, column + 4, row, row + 1 );

			row ++;
			OO.insertRows(xSpreadsheet, 0, totalColumn, row, row+1, 1, 1);
			OO.insertRows(xSpreadsheet, 0, totalColumn, row, row+1, 1, 1);

			for(int i=0; i<1; i++) {
				votblSurveyRating vo = (votblSurveyRating)v.elementAt(i);
				count++;

				boolean hideNA = Q.getHideNAOption(surveyID);	
				String code = vo.getRatingCode();
				String ratingTask = vo.getRatingTaskName();
				int scaleID = vo.getScaleID();

				row++;
				OO.insertRows(xSpreadsheet, 0, totalColumn, row, row+1, 1, 1);
				scale[1] += 1;

				OO.mergeCells(xSpreadsheet, column+1, totalColumn, row, row);

				// add rating scale	
				row = row + 2;
				OO.insertRows(xSpreadsheet, 1, 1, row, row+2, 2, 1);
				scale[1] += 2;

				int c = 1;
				int r = row;
				int to = c;

				Vector RS = Q.getRatingScale(scaleID);


				for(int j=0; j<RS.size(); j++) {
					String [] sRS = new String[3];

					sRS = (String[])RS.elementAt(j);

					int low = Integer.parseInt(sRS[0]);
					int high = Integer.parseInt(sRS[1]);
					String desc = sRS[2];
					//Denise 29/12/2009 to hide NA if required
					if (!(hideNA && (desc.equalsIgnoreCase("NA") || desc.equalsIgnoreCase("N/A") || desc.equals("Not applicable")
							|| desc.contains("NA") || desc.contains("N/A")|| desc.contains("Not applicable") ||desc.contains("Not Applicable"))))
					{

						if (column + totalCells > totalColumn)
						{
							row += 2;
							column = 1;
							OO.insertRows(xSpreadsheet, 0, totalColumn, row, row+3, 3, 1);

							row += 1;
							System.out.println("runnnn");
						}
						OO.insertString(xSpreadsheet, desc, row, column);	// add in scale description
						OO.setCellAllignment(xSpreadsheet, column, column, row, row, 1, 2);
						OO.setCellAllignment(xSpreadsheet, column, column, row, row, 2, 2);

						r = row + 1;
						c = column;


						int start = c; // start merge cell
						String temp = "";

						while(low <= high) {						
							if(low > 1)
								temp += "    ";
							temp = temp + Integer.toString(low);

							low++;						
						}

						OO.insertString(xSpreadsheet, temp, r, c);	// add in rating scale value
						OO.setCellAllignment(xSpreadsheet, c, c, r, r, 1, 2);

						to = start+totalCells-1;	// merge cell for rating scale value

						OO.mergeCells(xSpreadsheet, start, to, r, r);
						OO.setTableBorder(xSpreadsheet, start, to, r, r, true, true, true, true, true, true);

						OO.mergeCells(xSpreadsheet, start, to, row, row);	// merge cell for rating scale description
						OO.setTableBorder(xSpreadsheet, start, to, row, row, true, true, true, true, true, true);
						OO.setBGColor(xSpreadsheet, start, to, row, row, BGCOLOR);

						merge = (double)desc.trim().length() / (double)(totalCells);				

						BigDecimal BD = new BigDecimal(merge);
						BD.setScale(0, BD.ROUND_UP);
						BigInteger BI = BD.toBigInteger();
						totalMerge = BI.intValue() + 1;

						OO.setRowHeight(xSpreadsheet, row, start, (150 * totalMerge));

						column = to + 1;
					}//end if to insert Rating scale
				}
				row = r + 2;
				scale[1] += 2;							
				column = 0;					
			}	
			insertPageBreak(xSpreadsheet, 1, 12, row);

		}
		catch(Exception e){
			System.out.println(e);
		}
		row = iRow;
		column = iColumn;
	}

	/**
	 * Retrieves clusters under the surveyID.
	 */
	public Vector ClusterByName() throws SQLException 
	{
		String query = "";
		Vector v = new Vector();

		query = query + "SELECT tblSurveyCluster.ClusterID, Cluster.ClusterName ";
		query = query + "FROM tblSurveyCluster INNER JOIN Cluster ON ";
		query = query + "tblSurveyCluster.ClusterID = Cluster.PKCluster ";
		query = query + "WHERE tblSurveyCluster.SurveyID = " + surveyID;
		query = query + " ORDER BY Cluster.ClusterName";

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			while(rs.next()) {
				voCluster vo = new voCluster();
				vo.setClusterID(rs.getInt("ClusterID"));
				vo.setClusterName(rs.getString("ClusterName"));
				v.add(vo);
			}

		}catch(Exception ex){
			System.out.println("IndividualReport.java - ClusterByName - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return v;
	}

	/**
	 * 	Retrieves clusters under the surveyID.
	 *	@param int iOrder	0 = Ascending, 1 = Descending
	 *
	 *	@return Vector Cluster
	 */
	public Vector Cluster(int iOrder) throws SQLException
	{
		String query = "";

		Vector v = new Vector();

		query = query + "SELECT tblSurveyCluster.ClusterID, Cluster.ClusterName ";
		query = query + "FROM tblSurveyCluster INNER JOIN Cluster ON ";
		query = query + "tblSurveyCluster.ClusterID = Cluster.PKCluster ";
		query = query + "WHERE tblSurveyCluster.SurveyID = " + surveyID;
		query = query + " ORDER BY Cluster.ClusterName";

		if (iOrder == 0)
			query = query + " ORDER BY Cluster.ClusterName";
		else
			query = query + " ORDER BY Cluster.ClusterName DESC";


		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			while(rs.next()) {
				voCluster vo = new voCluster();
				vo.setClusterID(rs.getInt("ClusterID"));
				vo.setClusterName(rs.getString("ClusterName"));
				v.add(vo);
			}

		}catch(Exception ex){
			System.out.println("IndividualReport.java - Cluster - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return v;
	}

	/**
	 * Retrieves competencies under the surveyID and clusterID.
	 */
	public Vector ClusterCompetencyByName(int clusterID) throws SQLException 
	{
		String query = "";
		int surveyLevel = Integer.parseInt(surveyInfo[0]);
		Vector v = new Vector();

		if(surveyLevel == 0) {
			query = query + "SELECT Cluster.ClusterName, tblSurveyCompetency.CompetencyID, Competency.CompetencyName, ";
			query = query + "CompetencyDefinition FROM tblSurveyCompetency INNER JOIN Competency ON ";
			query = query + "tblSurveyCompetency.CompetencyID = Competency.PKCompetency INNER JOIN Cluster ON Cluster.PKCluster = tblSurveyCompetency.ClusterID";
			query = query + "WHERE tblSurveyCompetency.SurveyID = " + surveyID + " AND tblSurveyCompetency.ClusterID = "+clusterID;
			query = query + " ORDER BY Cluster.ClusterName, Competency.CompetencyName";

		} else {

			query = query + "SELECT DISTINCT Cluster.ClusterName, tblSurveyBehaviour.CompetencyID, Competency.CompetencyName, ";
			query = query + "Competency.CompetencyDefinition FROM Competency INNER JOIN ";
			query = query + "tblSurveyBehaviour ON Competency.PKCompetency = tblSurveyBehaviour.CompetencyID ";
			query = query + "INNER JOIN Cluster ON Cluster.PKCluster = tblSurveyBehaviour.ClusterID ";
			query = query + "WHERE tblSurveyBehaviour.SurveyID = " + surveyID + "AND tblSurveyBehaviour.ClusterID = "+clusterID;
			query = query + " ORDER BY Cluster.ClusterName, Competency.CompetencyName";
		}


		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			while(rs.next()) {
				voCompetency vo = new voCompetency();
				vo.setCompetencyID(rs.getInt("CompetencyID"));
				vo.setCompetencyName(rs.getString("CompetencyName"));
				vo.setCompetencyDefinition(rs.getString("CompetencyDefinition"));
				v.add(vo);

			}

		}catch(Exception ex){
			System.out.println("IndividualReport.java - ClusterCompetencyByName - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return v;
	}

	/**
	 * 	Retrieves competencies under the surveyID.
	 *	@param int iOrder	0 = Ascending, 1 = Descending
	 *
	 *	@return ResultSet Competency
	 */
	public Vector ClusterCompetency(int iOrder, int clusterID) throws SQLException
	{
		String query = "";
		int surveyLevel = Integer.parseInt(surveyInfo[0]);

		Vector v = new Vector();

		if(surveyLevel == 0) {
			query = query + "SELECT Cluster.ClusterName, tblSurveyCompetency.CompetencyID, Competency.CompetencyName, ";
			query = query + "CompetencyDefinition FROM tblSurveyCompetency INNER JOIN Competency ON ";
			query = query + "tblSurveyCompetency.CompetencyID = Competency.PKCompetency INNER JOIN Cluster ON Cluster.PKCluster = tblSurveyCompetency.ClusterID";
			query = query + "WHERE tblSurveyCompetency.SurveyID = " + surveyID + " AND tblSurveyCompetency.ClusterID = "+clusterID;

			//Changed by HA  07/07/08 Order should be by Competency Name instead of CompetencyID
			if (iOrder == 0)
				query = query + " ORDER BY Cluster.ClusterName, Competency.CompetencyName";
			else
				query = query + " ORDER BY Cluster.ClusterName, Competency.CompetencyName DESC";

		} else {

			query = query + "SELECT DISTINCT Cluster.ClusterName, tblSurveyBehaviour.CompetencyID, Competency.CompetencyName, ";
			query = query + "Competency.CompetencyDefinition FROM Competency INNER JOIN ";
			query = query + "tblSurveyBehaviour ON Competency.PKCompetency = tblSurveyBehaviour.CompetencyID ";
			query = query + "INNER JOIN Cluster ON Cluster.PKCluster = tblSurveyBehaviour.ClusterID " ;
			query = query + "WHERE tblSurveyBehaviour.SurveyID = " + surveyID + " AND tblSurveyBehaviour.ClusterID = "+clusterID;

			//Changed by Ha 02/07/08 Order by CompetencyName instead of CompetencyID
			//Problem with old query: It is ordered by competency ID while the respective
			//value is ordered by Competency name. Therefore, name and value do not match
			if (iOrder == 0)
				query = query + " ORDER BY Cluster.ClusterName, Competency.CompetencyName";
			else
				query = query + " ORDER BY Cluster.ClusterName, Competency.CompetencyName DESC";

		}

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			while(rs.next()) {
				voCompetency vo = new voCompetency();
				vo.setCompetencyID(rs.getInt("CompetencyID"));
				vo.setCompetencyName(rs.getString("CompetencyName"));
				vo.setCompetencyDefinition(rs.getString("CompetencyDefinition"));
				v.add(vo);

			}

		}catch(Exception ex){
			System.out.println("IndividualReport.java - Competency - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return v;
	}

	/**
	 * Retrieves Key Behaviour lists based on CompetencyID and clusterID.
	 */
	public Vector ClusterKBList(int compID, int clusterID) throws SQLException 
	{
		String query = "SELECT DISTINCT tblSurveyBehaviour.KeyBehaviourID, KeyBehaviour.KeyBehaviour ";
		query = query + "FROM tblSurveyBehaviour INNER JOIN KeyBehaviour ON ";
		query = query + "tblSurveyBehaviour.KeyBehaviourID = KeyBehaviour.PKKeyBehaviour ";
		query = query + "WHERE tblSurveyBehaviour.SurveyID = " + surveyID + " AND ";
		query = query + "tblSurveyBehaviour.CompetencyID = " + compID + " AND tblSurveyBehaviour.ClusterID = "+clusterID;
		query = query + " ORDER BY tblSurveyBehaviour.KeyBehaviourID";

		Vector v = new Vector();

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			while(rs.next()) {
				voKeyBehaviour vo = new voKeyBehaviour();
				vo.setKeyBehaviourID(rs.getInt("KeyBehaviourID"));
				vo.setKeyBehaviour(rs.getString("KeyBehaviour"));
				v.add(vo);

			}

		}catch(Exception ex){
			System.out.println("IndividualReport.java - ClusterKBList - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return v;
	}

	/**
	 * Retrieves competencies under the surveyID.
	 */
	public Vector CompetencyByName() throws SQLException 
	{
		String query = "";
		int surveyLevel = Integer.parseInt(surveyInfo[0]);
		Vector v = new Vector();

		if(surveyLevel == 0) {
			query = query + "SELECT tblSurveyCompetency.CompetencyID, Competency.CompetencyName, ";
			query = query + "CompetencyDefinition FROM tblSurveyCompetency INNER JOIN Competency ON ";
			query = query + "tblSurveyCompetency.CompetencyID = Competency.PKCompetency ";
			query = query + "WHERE tblSurveyCompetency.SurveyID = " + surveyID;
			query = query + " ORDER BY Competency.CompetencyName";

		} else {

			query = query + "SELECT DISTINCT tblSurveyBehaviour.CompetencyID, Competency.CompetencyName, ";
			query = query + "Competency.CompetencyDefinition FROM Competency INNER JOIN ";
			query = query + "tblSurveyBehaviour ON Competency.PKCompetency = tblSurveyBehaviour.CompetencyID ";
			query = query + "AND Competency.PKCompetency = tblSurveyBehaviour.CompetencyID ";
			query = query + "WHERE tblSurveyBehaviour.SurveyID = " + surveyID;
			query = query + " ORDER BY Competency.CompetencyName";
		}


		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			while(rs.next()) {
				voCompetency vo = new voCompetency();
				vo.setCompetencyID(rs.getInt("CompetencyID"));
				vo.setCompetencyName(rs.getString("CompetencyName"));
				vo.setCompetencyDefinition(rs.getString("CompetencyDefinition"));
				v.add(vo);

			}

		}catch(Exception ex){
			System.out.println("IndividualReport.java - CompetencyByName - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return v;
	}

	/**
	 * 	Retrieves competencies under the surveyID.
	 *	@param int iOrder	0 = Ascending, 1 = Descending
	 *
	 *	@return ResultSet Competency
	 */
	public Vector Competency(int iOrder) throws SQLException
	{
		String query = "";
		int surveyLevel = Integer.parseInt(surveyInfo[0]);

		Vector v = new Vector();

		if(surveyLevel == 0) {
			query = query + "SELECT tblSurveyCompetency.CompetencyID, Competency.CompetencyName, ";
			query = query + "CompetencyDefinition FROM tblSurveyCompetency INNER JOIN Competency ON ";
			query = query + "tblSurveyCompetency.CompetencyID = Competency.PKCompetency ";
			query = query + "WHERE tblSurveyCompetency.SurveyID = " + surveyID;

			//Changed by HA  07/07/08 Order should be by Competency Name instead of CompetencyID
			if (iOrder == 0)
				query = query + " ORDER BY Competency.CompetencyName";
			else
				query = query + " ORDER BY Competency.CompetencyName DESC";

		} else {

			query = query + "SELECT DISTINCT tblSurveyBehaviour.CompetencyID, Competency.CompetencyName, ";
			query = query + "Competency.CompetencyDefinition FROM Competency INNER JOIN ";
			query = query + "tblSurveyBehaviour ON Competency.PKCompetency = tblSurveyBehaviour.CompetencyID ";
			query = query + "AND Competency.PKCompetency = tblSurveyBehaviour.CompetencyID ";
			query = query + "WHERE tblSurveyBehaviour.SurveyID = " + surveyID;

			//Changed by Ha 02/07/08 Order by CompetencyName instead of CompetencyID
			//Problem with old query: It is ordered by competency ID while the respective
			//value is ordered by Competency name. Therefore, name and value do not match
			if (iOrder == 0)
				query = query + " ORDER BY Competency.CompetencyName";
			else
				query = query + " ORDER BY Competency.CompetencyName DESC";

		}

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			while(rs.next()) {
				voCompetency vo = new voCompetency();
				vo.setCompetencyID(rs.getInt("CompetencyID"));
				vo.setCompetencyName(rs.getString("CompetencyName"));
				vo.setCompetencyDefinition(rs.getString("CompetencyDefinition"));
				v.add(vo);

			}

		}catch(Exception ex){
			System.out.println("IndividualReport.java - Competency - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return v;
	}

	/**
	 * Retrieves Key Behaviour lists based on CompetencyID.
	 */
	public Vector KBList(int compID) throws SQLException 
	{
		String query = "SELECT DISTINCT tblSurveyBehaviour.KeyBehaviourID, KeyBehaviour.KeyBehaviour ";
		query = query + "FROM tblSurveyBehaviour INNER JOIN KeyBehaviour ON ";
		query = query + "tblSurveyBehaviour.KeyBehaviourID = KeyBehaviour.PKKeyBehaviour ";
		query = query + "WHERE tblSurveyBehaviour.SurveyID = " + surveyID + " AND ";
		query = query + "tblSurveyBehaviour.CompetencyID = " + compID;
		query = query + " ORDER BY tblSurveyBehaviour.KeyBehaviourID";

		Vector v = new Vector();

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			while(rs.next()) {
				voKeyBehaviour vo = new voKeyBehaviour();
				vo.setKeyBehaviourID(rs.getInt("KeyBehaviourID"));
				vo.setKeyBehaviour(rs.getString("KeyBehaviour"));
				v.add(vo);

			}

		}catch(Exception ex){
			System.out.println("IndividualReport.java - KBLIst - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return v;
	}

	/**
	 * To retrieve a list of competency in the survey which contains comments by raters
	 * @author Sebastian
	 * @since v.1.3.12.78 (19 July 2010)
	 **/
	public Vector CompListHaveComments() throws SQLException 
	{
		String query = "SELECT DISTINCT(Competency.PKCompetency), Competency.CompetencyName, Competency.CompetencyDefinition FROM tblAssignment "; 
		query = query + "INNER JOIN tblComment ON tblAssignment.AssignmentID = tblComment.AssignmentID ";
		query = query + "INNER JOIN Competency ON tblComment.CompetencyID = Competency.PKCompetency ";
		query = query + "WHERE tblAssignment.SurveyID = " + surveyID + " ";
		query = query + "AND tblAssignment.TargetLoginID = " + targetID + " ";
		query = query + "AND tblComment.Comment != '' ";
		query = query + "ORDER BY Competency.CompetencyName";

		Vector v = new Vector();

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			while(rs.next()) {
				voCompetency vo = new voCompetency();
				vo.setCompetencyID(rs.getInt("PKCompetency"));
				vo.setCompetencyName(rs.getString("CompetencyName"));
				vo.setCompetencyDefinition(rs.getString("CompetencyDefinition"));
				v.add(vo);
			}

		}catch(Exception ex){
			System.out.println("IndividualReport.java - CompListHaveComments - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return v;

	}

	/**
	 * To retreive the Key Behaviours in the competency that contains comments by raters
	 * @param compID - Specifiy the competency to reference
	 * @author Sebastian
	 * @since v.1.3.12.78 (19 July 2010)
	 **/
	public Vector KBListHaveComments(int compID) throws SQLException 
	{
		String query = "SELECT DISTINCT(KeyBehaviour.PKKeyBehaviour), KeyBehaviour.KeyBehaviour FROM tblAssignment "; 
		query = query + "INNER JOIN tblComment ON tblAssignment.AssignmentID = tblComment.AssignmentID "; 
		query = query + "INNER JOIN Competency ON tblComment.CompetencyID = Competency.PKCompetency ";
		query = query + "INNER JOIN KeyBehaviour ON tblComment.KeyBehaviourID = KeyBehaviour.PKKeyBehaviour "; 
		query = query + "WHERE tblAssignment.SurveyID = " + surveyID + " ";
		query = query + "AND tblAssignment.TargetLoginID = " + targetID + " ";
		query = query + "AND Competency.PKCompetency = " + compID + " ";
		query = query + "AND tblComment.Comment != '' ";
		query = query + "ORDER BY KeyBehaviour.PKKeyBehaviour";

		Vector v = new Vector();

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			while(rs.next()) {
				voKeyBehaviour vo = new voKeyBehaviour();
				vo.setKeyBehaviourID(rs.getInt("PKKeyBehaviour"));
				vo.setKeyBehaviour(rs.getString("KeyBehaviour"));
				v.add(vo);

			}

		}catch(Exception ex){
			System.out.println("IndividualReport.java - KBListHaveComments - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return v;
	}
	/**
	 * Retrieve all the rating task assigned to the specific survey.
	 */
	public Vector RatingTask()  throws SQLException 
	{
		// Changed by Ha 27/05/08: add keyword DISTINCT into the query
		String query = "SELECT DISTINCT tblSurveyRating.RatingTaskID, tblRatingTask.RatingCode, ";
		query = query + "tblSurveyRating.RatingTaskName FROM tblSurveyRating INNER JOIN ";
		query = query + "tblRatingTask ON tblSurveyRating.RatingTaskID = tblRatingTask.RatingTaskID ";			
		query = query + "WHERE tblSurveyRating.SurveyID = " + surveyID;
		query = query + " and tblRatingTask.RatingCode in('CP', 'CPR', 'FPR')";
		query = query + " ORDER BY tblSurveyRating.RatingTaskID";

		Vector v = new Vector();

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			while(rs.next()) {
				votblSurveyRating vo = new votblSurveyRating();
				vo.setRatingTaskID(rs.getInt("RatingTaskID"));
				vo.setRatingCode(rs.getString("RatingCode"));
				vo.setRatingTaskName(rs.getString("RatingTaskName"));
				v.add(vo);
			}

		}catch(Exception ex){
			System.out.println("IndividualReport.java - RatingTask - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return v;
	}

	/**
	 * Get the maximum scale, which is to be used in the alignment process.
	 */
	public int MaxScale() throws SQLException 
	{	
		int total = 0;

		String query = "SELECT MAX(tblScale.ScaleRange) AS Result FROM ";
		query = query + "tblScale INNER JOIN tblSurveyRating ON ";
		query = query + "tblScale.ScaleID = tblSurveyRating.ScaleID WHERE ";
		query = query + "tblSurveyRating.SurveyID = " + surveyID;

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			if(rs.next())
				total = rs.getInt(1);

		}catch(Exception ex){
			System.out.println("IndividualReport.java - MaxScale - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return total;
	}

	/**
	 * Count the total clusters in the particular survey.
	 */
	public int totalCluster() throws SQLException {
		String query = "";

		int total = 0;

		query += "SELECT  COUNT(ClusterID) AS Total FROM tblSurveyCluster ";
		query += "WHERE SurveyID = " + surveyID;

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			if(rs.next())
				total = rs.getInt(1);


		}catch(Exception ex){
			System.out.println("IndividualReport.java - totalCluster - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return total;
	}

	/**
	 * Count the total competencies in the particular survey.
	 */
	public int totalCompetency() throws SQLException {
		String query = "";
		int surveyLevel = Integer.parseInt(surveyInfo[0]);

		int total = 0;

		if(surveyLevel == 0) {
			query = query + "SELECT  COUNT(CompetencyID) AS Total FROM tblSurveyCompetency ";
			query = query + "WHERE SurveyID = " + surveyID;
		}else {
			query = query + "SELECT COUNT(DISTINCT CompetencyID) AS Total FROM ";
			query = query + "tblSurveyBehaviour WHERE SurveyID = " + surveyID;
		}

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			if(rs.next())
				total = rs.getInt(1);


		}catch(Exception ex){
			System.out.println("IndividualReport.java - totalCompetency - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return total;
	}

	/**by Hemilda 23/09/2008
	 * Count total Others for the particular survey and target.
	 * for KB level
	 * To calculate number of others rater  for each rating task of each KB
	 */	
	public int totalOth1(int iRatingTaskID, int iCompetencyID) throws SQLException 
	{	
		int total = 0;
		SurveyResult SR = new SurveyResult();
		Calculation cal = new Calculation();
		String query = "select max(table1.Cnt)AS Total ";
		query = query + " From( ";
		query = query + " SELECT     COUNT(tblAssignment.RaterCode) AS Cnt,tblResultBehaviour.KeyBehaviourID ";
		query = query + " FROM         tblAssignment INNER JOIN ";
		query = query + " tblResultBehaviour ON tblAssignment.AssignmentID = tblResultBehaviour.AssignmentID INNER JOIN ";
		query = query + " KeyBehaviour ON tblResultBehaviour.KeyBehaviourID = KeyBehaviour.PKKeyBehaviour ";
		query = query + " WHERE     (tblAssignment.SurveyID =  " + surveyID + ") AND (tblAssignment.TargetLoginID = "+targetID+") " ;
		if (cal.NAIncluded(surveyID)==0)
			query = query + " AND RaterCode LIKE 'OTH%' and RaterStatus in(1,2,4)";
		else
			query = query + " AND RaterCode LIKE 'OTH%' and RaterStatus in(1,2,4,5)";
		query = query + "  AND (tblResultBehaviour.RatingTaskID = "+iRatingTaskID+")and (KeyBehaviour.FKCompetency = "+iCompetencyID +") ";
		if (cal.NAIncluded(surveyID)==0)
			query = query + " AND (tblResultBehaviour.Result <> 0)";   
		query = query + "  group by tblResultBehaviour.KeyBehaviourID ";
		query = query + "  ) table1 ";


		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			if(rs.next())
				total = rs.getInt(1);


		}catch(Exception ex){
			System.out.println("IndividualReport.java - totalOth - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}
		//System.out.println(">>>>>>>>> "+query);	
		return total;

	}

	/**
	 * Count total Others for the particular survey and target.
	 * Code edited by Ha adding RatingTask and Competency to method signature
	 * To calculate number of others rater  for each rating task of each competency
	 */
	public int totalOth(int iRatingTaskID, int iCompetencyID) throws SQLException 
	{	
		int total = 0;
		SurveyResult SR = new SurveyResult();
		Calculation cal = new Calculation();
		//Query changed by Ha 07/07/08 to calculate number of others rated for this target
		//exlcluded who put NA in their questionnaire if the survey is NA_Excluded
		String query = "SELECT COUNT(RaterCode) AS Total FROM tblAssignment ";
		query = query + " INNER JOIN  tblResultCompetency ON tblAssignment.AssignmentID = tblResultCompetency.AssignmentID ";
		query = query + "WHERE SurveyID = " + surveyID + " AND TargetLoginID = " + targetID;
		if (cal.NAIncluded(surveyID)==0)
			query = query + " AND RaterCode LIKE 'OTH%' and RaterStatus in(1,2,4)";
		else
			query = query + " AND RaterCode LIKE 'OTH%' and RaterStatus in(1,2,4,5)";
		query = query + " AND tblResultCompetency.RatingTaskID = "+iRatingTaskID;
		query = query + " AND tblResultCompetency.CompetencyID = "+iCompetencyID;
		if (cal.NAIncluded(surveyID)==0)
			query = query + "AND tblResultCompetency.Result <> 0";


		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			if(rs.next())
				total = rs.getInt(1);


		}catch(Exception ex){
			System.out.println("IndividualReport.java - totalOth - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}
		//System.out.println(">>>>>>>>> "+query);	
		return total;

	}

	/**by Hemilda date 23/09/2008
	 * Count total Others for the particular survey and target.
	 * for KB level
	 * To calculate number of others rater  for each rating task of each competency
	 */	
	public int totalOth(int iRatingTaskID, int iCompetencyID,int iKBId) throws SQLException 
	{	
		int total = 0;
		SurveyResult SR = new SurveyResult();
		Calculation cal = new Calculation();

		String query = "SELECT     COUNT(tblAssignment.RaterCode) AS Total ";
		query = query + " FROM         tblAssignment INNER JOIN ";
		query = query + "  tblResultBehaviour ON tblAssignment.AssignmentID = tblResultBehaviour.AssignmentID ";
		query = query + "  WHERE     (tblAssignment.SurveyID =  "+surveyID+") AND (tblAssignment.TargetLoginID ="+targetID+")";
		if (cal.NAIncluded(surveyID)==0)
			query = query + " AND RaterCode LIKE 'OTH%' and RaterStatus in(1,2,4)";
		else
			query = query + " AND RaterCode LIKE 'OTH%' and RaterStatus in(1,2,4,5)";
		query = query + " AND (tblResultBehaviour.RatingTaskID ="+iRatingTaskID+") AND (tblResultBehaviour.KeyBehaviourID = "+iKBId+") ";
		if (cal.NAIncluded(surveyID)==0)
			query = query + " AND (tblResultBehaviour.Result <> 0)";                      



		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			if(rs.next())
				total = rs.getInt(1);


		}catch(Exception ex){
			System.out.println("IndividualReport.java - totalOth - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}
		//System.out.println(">>>>>>>>> "+query);	
		return total;

	}
	/** by Hemilda 23/09/2008
	 * Count total Supervisors for the particular survey and target.
	 * for KB level
	 * To calculate number of supervisor rater  for each rating task of each KB
	 */
	public int totalSup1(int iRatingTaskID, int iCompetencyID) throws SQLException 
	{	
		int total = 0;
		SurveyResult SR = new SurveyResult();
		Calculation cal = new Calculation();
		String query = "select max(table1.Cnt)AS Total ";
		query = query + " From( ";
		query = query + " SELECT     COUNT(tblAssignment.RaterCode) AS Cnt,tblResultBehaviour.KeyBehaviourID ";
		query = query + " FROM         tblAssignment INNER JOIN ";
		query = query + " tblResultBehaviour ON tblAssignment.AssignmentID = tblResultBehaviour.AssignmentID INNER JOIN ";
		query = query + " KeyBehaviour ON tblResultBehaviour.KeyBehaviourID = KeyBehaviour.PKKeyBehaviour ";
		query = query + " WHERE     (tblAssignment.SurveyID =  " + surveyID + ") AND (tblAssignment.TargetLoginID = "+targetID+") " ;
		if (cal.NAIncluded(surveyID)==0)
			query = query + " AND RaterCode LIKE 'SUP%' and RaterStatus in(1,2,4)";
		else
			query = query + " AND RaterCode LIKE 'SUP%' and RaterStatus in(1,2,4,5)";
		query = query + "  AND (tblResultBehaviour.RatingTaskID = "+iRatingTaskID+")and (KeyBehaviour.FKCompetency = "+iCompetencyID +") ";
		if (cal.NAIncluded(surveyID)==0)
			query = query + " AND (tblResultBehaviour.Result <> 0)";   
		query = query + "  group by tblResultBehaviour.KeyBehaviourID ";
		query = query + "  ) table1 ";


		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			if(rs.next())
				total = rs.getInt(1);


		}catch(Exception ex){
			System.out.println("IndividualReport.java - totalSup - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}
		//System.out.println(">>>>>>>>> "+query);	
		return total;

	}
	/**by Hemilda 23/09/2008
	 * Count total Supervisors for the particular survey and target.
	 *KB level
	 * To calculate number of supervisor rater  for each rating task of each competency
	 */
	public int totalSup(int iRatingTaskID, int iCompetencyID,int iKBId) throws SQLException 
	{
		int total = 0;
		Calculation cal  = new Calculation();

		String query = "SELECT COUNT(RaterCode) AS Total FROM tblAssignment ";
		query = query + " INNER JOIN tblResultBehaviour ON tblAssignment.AssignmentID = tblResultBehaviour.AssignmentID ";
		query = query + "WHERE SurveyID = " + surveyID + " AND TargetLoginID = " + targetID;
		if (cal.NAIncluded(surveyID)==0)
			query = query + " AND RaterCode LIKE 'SUP%' and RaterStatus in(1,2,4)";
		else
			query = query + "AND RaterCode LIKE 'SUP%' and RaterStatus in(1,2,4,5)";
		query = query + " AND tblResultBehaviour.RatingTaskID = "+iRatingTaskID;
		query = query + " AND tblResultBehaviour.KeyBehaviourID = "+iKBId;
		if (cal.NAIncluded(surveyID)==0)
			query = query + " AND tblResultBehaviour.Result <> 0";
		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			if(rs.next())
				total = rs.getInt(1);


		}catch(Exception ex){
			System.out.println("IndividualReport.java - totalSup - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return total;
	}

	/**
	 * Count total Supervisors for the particular survey and target.
	 * Code edited by Ha adding RatingTask and Competency to method signature
	 * To calculate number of supervisor rater  for each rating task of each competency
	 */
	public int totalSup(int iRatingTaskID, int iCompetencyID) throws SQLException 
	{
		int total = 0;
		Calculation cal  = new Calculation();
		//Query changed by Ha 07/07/08 to calculate number of supervisor rated for this target
		//exlcluded who put NA in their questionnaire if survey is NA_Excluded
		String query = "SELECT COUNT(RaterCode) AS Total FROM tblAssignment ";
		query = query + " INNER JOIN tblResultCompetency ON tblAssignment.AssignmentID = tblResultCompetency.AssignmentID ";
		query = query + "WHERE SurveyID = " + surveyID + " AND TargetLoginID = " + targetID;
		if (cal.NAIncluded(surveyID)==0)
			query = query + " AND RaterCode LIKE 'SUP%' and RaterStatus in(1,2,4)";
		else
			query = query + "AND RaterCode LIKE 'SUP%' and RaterStatus in(1,2,4,5)";
		query = query + " AND tblResultCompetency.RatingTaskID = "+iRatingTaskID;
		query = query + " AND tblResultCompetency.CompetencyID = "+iCompetencyID;
		if (cal.NAIncluded(surveyID)==0)
			query = query + " AND tblResultCompetency.Result <> 0";
		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			if(rs.next())
				total = rs.getInt(1);


		}catch(Exception ex){
			System.out.println("IndividualReport.java - totalSup - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return total;
	}
	/** by Hemilda 23/09/2008
	 * Count total Self for the particular survey and target.
	 * KB level
	 * To calculate number of SELF rater  for each rating task of each KB
	 */
	public int totalSelf1(int iRatingTaskID, int iCompetencyID) throws SQLException 
	{	
		int total = 0;
		SurveyResult SR = new SurveyResult();
		Calculation cal = new Calculation();
		String query = "select max(table1.Cnt)AS Total ";
		query = query + " From( ";
		query = query + " SELECT     COUNT(tblAssignment.RaterCode) AS Cnt,tblResultBehaviour.KeyBehaviourID ";
		query = query + " FROM         tblAssignment INNER JOIN ";
		query = query + " tblResultBehaviour ON tblAssignment.AssignmentID = tblResultBehaviour.AssignmentID INNER JOIN ";
		query = query + " KeyBehaviour ON tblResultBehaviour.KeyBehaviourID = KeyBehaviour.PKKeyBehaviour ";
		query = query + " WHERE     (tblAssignment.SurveyID =  " + surveyID + ") AND (tblAssignment.TargetLoginID = "+targetID+") " ;
		if (cal.NAIncluded(surveyID)==0)
			query = query + " AND RaterCode LIKE 'SELF' and RaterStatus in(1,2,4)";
		else
			query = query + " AND RaterCode LIKE 'SELF' and RaterStatus in(1,2,4,5)";
		query = query + "  AND (tblResultBehaviour.RatingTaskID = "+iRatingTaskID+")and (KeyBehaviour.FKCompetency = "+iCompetencyID +") ";
		if (cal.NAIncluded(surveyID)==0)
			query = query + " AND (tblResultBehaviour.Result <> 0)";   
		query = query + "  group by tblResultBehaviour.KeyBehaviourID ";
		query = query + "  ) table1 ";


		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			if(rs.next())
				total = rs.getInt(1);


		}catch(Exception ex){
			System.out.println("IndividualReport.java - totalSelf - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}
		//System.out.println(">>>>>>>>> "+query);	
		return total;

	}

	/**
	 * Count total Self for the particular survey and target.
	 * for KB level
	 * To calculate number of SELF rater  for each rating task of each competency
	 */
	public int totalSelf(int iRatingTaskID, int iCompetencyID,int iKBId) throws SQLException 
	{	
		int total = 0;
		Calculation cal = new Calculation();

		String query = "SELECT COUNT(RaterCode) AS Total FROM tblAssignment INNER JOIN tblResultBehaviour";
		query = query + " ON tblAssignment.AssignmentID = tblResultBehaviour.AssignmentID ";
		query = query + "WHERE SurveyID = " + surveyID + " AND TargetLoginID = " + targetID;
		if (cal.NAIncluded(surveyID) ==0)
			query = query + " AND RaterCode = 'SELF' and RaterStatus in(1,2,4)";
		else
			query = query + " AND RaterCode = 'SELF' and RaterStatus in(1,2,4,5)";
		query = query + " AND tblResultBehaviour.RatingTaskID  =  "+iRatingTaskID;
		query = query + " AND tblResultBehaviour.KeyBehaviourID = "+iKBId;

		if (cal.NAIncluded(surveyID)==0)
			query = query + " AND tblResultBehaviour.Result <>0";

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			if(rs.next())
				total = rs.getInt(1);


		}catch(Exception ex){
			System.out.println("IndividualReport.java - totalSelf - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return total;
	}

	/**
	 * Count total Self for the particular survey and target.
	 * Code edited by Ha 07/07/08 add Rating task ID and Competency ID to method signature
	 * To calculate number of SELF rater  for each rating task of each competency
	 */
	public int totalSelf(int iRatingTaskID, int iCompetencyID) throws SQLException 
	{	
		int total = 0;
		Calculation cal = new Calculation();
		//Query changed by Ha 07/07/08 to calculate number of SELF rated for this target
		//exlcluded who put NA in their questionnaire if survey is NA_Excluded
		String query = "SELECT COUNT(RaterCode) AS Total FROM tblAssignment INNER JOIN tblResultCompetency";
		query = query + " ON tblAssignment.AssignmentID = tblResultCompetency.AssignmentID ";
		query = query + "WHERE SurveyID = " + surveyID + " AND TargetLoginID = " + targetID;
		if (cal.NAIncluded(surveyID) ==0)
			query = query + " AND RaterCode = 'SELF' and RaterStatus in(1,2,4)";
		else
			query = query + " AND RaterCode = 'SELF' and RaterStatus in(1,2,4,5)";
		query = query + " AND tblResultCompetency.RatingTaskID  =  "+iRatingTaskID;
		query = query + " AND tblResultCompetency.CompetencyID  = "+iCompetencyID;

		if (cal.NAIncluded(surveyID)==0)
			query = query + " AND tblResultCompetency.Result <>0";

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			if(rs.next())
				total = rs.getInt(1);


		}catch(Exception ex){
			System.out.println("IndividualReport.java - totalSelf - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return total;
	}

	/**
	 * Count total groups for the particular survey and target.
	 */	
	public int totalGroup() throws SQLException 
	{	
		int total = 0;

		String query = "SELECT COUNT(DISTINCT tblAssignment.RTRelation) AS TotalGroup ";
		query = query + "FROM tblAssignment INNER JOIN tblSurveyRating ON ";
		query = query + "tblAssignment.SurveyID = tblSurveyRating.SurveyID INNER JOIN ";
		query = query + "tblRatingTask ON tblSurveyRating.RatingTaskID = tblRatingTask.RatingTaskID ";
		query = query + "WHERE tblAssignment.SurveyID = " + surveyID + " AND ";
		query = query + "tblAssignment.TargetLoginID = " + targetID + " AND tblRatingTask.RatingCode = 'CP'";
		query = query + " and tblAssignment.RaterStatus in(1,2,4)";

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			if(rs.next())
				total = rs.getInt(1);


		}catch(Exception ex){
			System.out.println("IndividualReport.java - totalGroup - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return total;
	}

	/**
	 * Count total other rating tasks besides CP for the particular survey.
	 */
	public int totalOtherRT() throws SQLException 
	{
		int total = 0;

		String query = "SELECT COUNT(tblRatingTask.RatingCode) AS TotalRT ";
		query = query + "FROM tblSurveyRating INNER JOIN tblRatingTask ON ";
		query = query + "tblSurveyRating.RatingTaskID = tblRatingTask.RatingTaskID ";
		query = query + "WHERE tblSurveyRating.SurveyID = " + surveyID;
		query = query + " AND (tblRatingTask.RatingCode = 'CPR' or tblRatingTask.RatingCode = 'FPR')";

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			if(rs.next())
				total = rs.getInt(1);


		}catch(Exception ex){
			System.out.println("IndividualReport.java - totalOtherRT - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}


		return total;
	}

	/**
	 * Retrieves the competency score for all.
	 * This is only applied in KB Level Analysis.
	 * The score is stored in table tblTrimmedMean
	 */
	public double CompTrimmedMeanforAll(int RTID, int compID) throws SQLException 
	{
		double Result = 0;
		String query = "";
		int reliabilityIndex = C. ReliabilityCheck(surveyID);

		if(reliabilityIndex == 0) {
			query = query + "SELECT CompetencyID, Type, round(TrimmedMean, 2) AS Result FROM tblTrimmedMean ";
			query += "WHERE SurveyID = " + surveyID;
			query += " AND TargetLoginID = " + targetID + " AND RatingTaskID = " + RTID + " and CompetencyID = " + compID;
			query += " ORDER BY CompetencyID";
		} else {
			query = "select RatingTaskID, CompetencyID, cast(AVG(AvgMean) as numeric(38,2)) as Result from tblAvgMean ";
			query = query + "where SurveyID = " + surveyID;
			query = query + " AND TargetLoginID = " + targetID;
			query = query + " and Type = 1";
			query += " AND RatingTaskID = "+ RTID + " AND CompetencyID = " + compID;
			query = query + " group by CompetencyID, RatingTaskID";
		}

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			if(rs.next())
				Result = rs.getDouble("Result");

		}catch(Exception ex){
			System.out.println("IndividualReport.java - CompTrimmedMeanForAll - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}


		return Result;
	}	

	/**
	 * Retrieves the average mean of KB for a specific competency.
	 * This is only applied in KB Level Analysis.
	 */
	public Vector KBMean(int RTID, int compID) throws SQLException 
	{
		String query = "SELECT CompetencyID, Type, CAST(AVG(AvgMean) AS numeric(38, 2)) AS Result ";
		query = query + "FROM tblAvgMean WHERE SurveyID = " + surveyID + " AND TargetLoginID = " + targetID;
		query = query + " AND CompetencyID = " + compID + " and RatingTaskID = " + RTID;
		query = query + " GROUP BY CompetencyID, Type ORDER BY Type";

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		Vector v = new Vector();

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			while(rs.next()) {
				String [] arr = new String[3];
				arr[0] = rs.getString(1);
				arr[1] = rs.getString(2);
				arr[2] = rs.getString(3);
				v.add(arr);
			}
		}catch(Exception ex){
			System.out.println("IndividualReport.java - KBMean - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return v;
	}	

	/**
	 * Retrieve the average or trimmed mean result based on competency and key behaviour for each group.
	 */
	public Vector MeanResult(int RTID, int compID, int KBID) throws SQLException 
	{
		String query = "";
		int surveyLevel = Integer.parseInt(surveyInfo[0]);
		int reliabilityCheck = C. ReliabilityCheck(surveyID);

		String tblName = "tblAvgMean";
		String result = "AvgMean";

		if(reliabilityCheck == 0) {
			tblName = "tblTrimmedMean";
			result = "TrimmedMean";
		}
		// Changed by Ha 27/05/08: add keyword DISTINCT
		if(surveyLevel == 0) {
			query = query + "SELECT DISTINCT " + tblName + ".CompetencyID, ";
			query = query + tblName + ".Type, " + tblName + "." + result;
			query = query + " as Result FROM " + tblName;
			query = query + " WHERE " + tblName + ".SurveyID = " + surveyID + " AND ";
			query = query + tblName + ".TargetLoginID = " + targetID;
			query = query + " AND " + tblName + ".RatingTaskID = " + RTID;
			query = query + " AND " + tblName + ".CompetencyID = " + compID;
			query = query + " ORDER BY " + tblName + ".Type";
		} else {
			query = query + "SELECT DISTINCT CompetencyID, Type, AvgMean as Result, KeyBehaviourID ";
			query = query + "FROM tblAvgMean WHERE SurveyID = " + surveyID + " AND ";
			query = query + "TargetLoginID = " + targetID + " AND RatingTaskID = " + RTID;
			query = query + " AND CompetencyID = " + compID + " AND KeyBehaviourID = " + KBID;
			query = query + " ORDER BY Type";
		}

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		Vector v = new Vector();

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			while(rs.next()) {  			    
				String [] arr = new String[3];
				arr[0] = rs.getString(1);	 			
				arr[1] = rs.getString(2);	 			
				arr[2] = rs.getString(3);	 			
				v.add(arr);
			}
		}catch(Exception ex){
			System.out.println("IndividualReport.java - MEanResult - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return v;
	}

	/**
	 * Retrieves the individual level of agreement from tblLevelOfAgreement based on Competency and Key Behaviour.
	 * KBID = 0 if it is Competency Level Analysis.
	 */
	public double LevelOfAgreement(int compID, int KBID) throws SQLException 
	{
		String query = "";
		int surveyLevel = Integer.parseInt(surveyInfo[0]);
		double LOA = -1;

		if(surveyLevel == 0) {

			query = query + "select * from tblLevelOfAgreement where SurveyID = " + surveyID;
			query = query + " and TargetLoginID = " + targetID + " and CompetencyID = " + compID;

		}else {
			query = query + "select * from tblLevelOfAgreement where SurveyID = " + surveyID;
			query = query + " and TargetLoginID = " + targetID + " and CompetencyID = " + compID;
			query = query + " and KeyBehaviourID = " + KBID;
		}

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			if(rs.next())
				LOA = rs.getDouble("LevelOfAgreement");


		}catch(Exception ex){
			System.out.println("IndividualReport.java - LevelOfAgreement - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}


		return LOA;
	}

	/**
	 * Calculate the average of individual level of agreement for each competency.
	 * This is only apply for KB Level Analysis.
	 */
	public double AvgLevelOfAgreement(int compID, int noOfRaters) throws SQLException 
	{	
		String query = "";
		int iBase = C.getLOABase(noOfRaters);
		double LOA = -1;
		int iMaxScale = rscale.getMaxScale(surveyID); //Get Maximum Scale of this survey

		/*query = query + "SELECT tblResultBehaviour.RatingTaskID, KeyBehaviour.FKCompetency, ";
		query = query + "cast((100-(stDev(tblResultBehaviour.Result * 10 / " + iMaxScale + ") * " + iBase + ")) AS numeric(38, 2)) AS LOA ";
		query = query + "FROM tblAssignment INNER JOIN tblResultBehaviour ON ";
		query = query + "tblAssignment.AssignmentID = tblResultBehaviour.AssignmentID INNER JOIN ";
		query = query + "tblRatingTask ON tblResultBehaviour.RatingTaskID = tblRatingTask.RatingTaskID ";
		query = query + "INNER JOIN KeyBehaviour ON ";
		query = query + "tblResultBehaviour.KeyBehaviourrID = KeyBehaviour.PKKeyBehaviour ";
		query = query + "WHERE tblAssignment.SurveyID = " + surveyID + " AND ";
		query = query + "tblAssignment.TargetLoginID = " + targetID + " AND ";
		query = query + "tblAssignment.RaterStatus IN (1, 2, 4) AND KeyBehaviour.FKCompetency = " + compID;
		query = query + " AND tblAssignment.RaterCode <> 'SELF' AND tblRatingTask.RatingCode = 'CP' ";
		query = query + "GROUP BY tblResultBehaviour.RatingTaskID, KeyBehaviour.FKCompetency";		
		 */
		//Edit by Roger 24 July 2008.
		//The base use in this calculation is wrong. Different competencies have different number of raters used to make
		// the calculation, depending on whether the survey is exclude NA and if the rater entered NA
		query = query + "SELECT tblAvgMeanByRater.RatingTaskID, tblAvgMeanByRater.CompetencyID, count(*) as numOfRaters, ";
		query = query + "stDev(tblAvgMeanByRater.AvgMean * 10 / " + iMaxScale + ") AS LOA ";
		query = query + "FROM tblAssignment INNER JOIN " ;
		query = query + "tblAvgMeanByRater ON tblAssignment.AssignmentID = tblAvgMeanByRater.AssignmentID INNER JOIN ";
		query = query + "tblRatingTask ON tblAvgMeanByRater.RatingTaskID = tblRatingTask.RatingTaskID ";
		query = query + "WHERE tblAssignment.SurveyID = " + surveyID + " AND ";
		query = query + "tblAssignment.TargetLoginID = " + targetID + " AND ";
		query = query + "tblAssignment.RaterStatus IN (1, 2, 4) AND tblAvgMeanByRater.CompetencyID = " + compID;
		query = query + " AND tblAssignment.RaterCode <> 'SELF' AND tblRatingTask.RatingCode = 'CP' ";
		query = query + "GROUP BY tblAvgMeanByRater.RatingTaskID, tblAvgMeanByRater.CompetencyID";		


		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			if(rs.next()) {
				//Edit by Roger 24 July 2008
				//Shift the calculation of LOA from Query to here. It is because the number of raters is get from the query
				//and we need to use the getLOABase formula seperately
				LOA = 100-rs.getDouble("LOA")*C.getLOABase(rs.getInt("numOfRaters"));
				BigDecimal bd = new BigDecimal(LOA);
				bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP); //round to 2 decimal place
				LOA = bd.doubleValue();
			}


		}catch(Exception ex){
			System.out.println("IndividualReport.java - AvgLevelOfAgreement - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return LOA;
	}	

	/**
	 * Retrieves the gap results from tblGap based on competency and key behaviour.
	 * KBID = 0 if it is Competency Level Analysis.
	 */
	public double Gap(int compID, int KBID) throws SQLException 
	{
		String query = "";
		double gap = -1;

		query = query + "SELECT Gap FROM tblGap WHERE SurveyID = " + surveyID;
		query = query + " AND TargetLoginID = " + targetID + " AND CompetencyID = " + compID;
		query = query + " and KeyBehaviourID = " + KBID;

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			if(rs.next())
				gap = rs.getDouble(1);


		}catch(Exception ex){
			System.out.println("IndividualReport.java - Gap - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return gap;
	}

	/**
	 * Retrieves the gap results from tblGap based on competency and key behaviour.
	 * KBID = 0 if it is Competency Level Analysis.
	 */
	public double GapToyota(int compID) throws SQLException 
	{
		String query = "";
		double gap = -1;

		query = query + "SELECT Gap FROM tblGap WHERE SurveyID = " + surveyID;
		query = query + " AND TargetLoginID = " + targetID + " AND CompetencyID = " + compID;

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			if(rs.next())
				gap = rs.getDouble(1);


		}catch(Exception ex){
			System.out.println("IndividualReport.java - GapToyota - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return gap;
	}

	/**
	 * Retrieves the importance result based on competency and key behaviour id.
	 * KBID = 0 if it is Competency Level Analysis.
	 */
	public Vector Importance(int compID, int KBID) throws SQLException 
	{
		String query = "";
		int surveyLevel = Integer.parseInt(surveyInfo[0]);
		int reliabilityCheck = C. ReliabilityCheck(surveyID);

		String tblName = "tblAvgMean";
		String result = "AvgMean";

		if(reliabilityCheck == 0) {
			tblName = "tblTrimmedMean";
			result = "TrimmedMean";
		}


		//try {			
		if(surveyLevel == 0) {
			query = query + "SELECT tblRatingTask.RatingCode, ";
			query = query + "tblSurveyRating.RatingTaskName, " + tblName + "." + result + " as Result ";
			query = query + "FROM " + tblName + " INNER JOIN tblRatingTask ON ";
			query = query + tblName + ".RatingTaskID = tblRatingTask.RatingTaskID ";
			query = query + "INNER JOIN tblSurveyRating ON ";
			query = query + "tblRatingTask.RatingTaskID = tblSurveyRating.RatingTaskID AND ";
			query = query + tblName + ".SurveyID = tblSurveyRating.SurveyID ";                      
			query = query + "WHERE " + tblName + ".SurveyID = " + surveyID + " AND ";
			query = query + tblName + ".TargetLoginID = " + targetID + " AND " + tblName + ".Type = 1 AND ";
			query = query + tblName + ".CompetencyID = " + compID + " AND ";
			query = query + "(tblRatingTask.RatingCode = 'IN' OR tblRatingTask.RatingCode = 'IF')";
		} else {
			query = query + "SELECT tblRatingTask.RatingCode, tblSurveyRating.RatingTaskName, ";
			query = query + "tblAvgMean.AvgMean AS Result FROM tblAvgMean INNER JOIN ";
			query = query + "tblRatingTask ON tblAvgMean.RatingTaskID = tblRatingTask.RatingTaskID ";
			query = query + "INNER JOIN tblSurveyRating ON ";
			query = query + "tblRatingTask.RatingTaskID = tblSurveyRating.RatingTaskID AND ";
			query = query + "tblAvgMean.SurveyID = tblSurveyRating.SurveyID ";                      				
			query = query + "WHERE tblAvgMean.SurveyID = " + surveyID + " AND ";
			query = query + "tblAvgMean.TargetLoginID = " + targetID + " AND ";
			query = query + "tblAvgMean.CompetencyID = " + compID + " AND ";
			query = query + "tblAvgMean.KeyBehaviourID = " + KBID + " AND tblAvgMean.Type = 1 ";
			query = query + "AND (tblRatingTask.RatingCode = 'IN' OR tblRatingTask.RatingCode = 'IF')";
		}				

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		Vector v = new Vector();

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			while(rs.next()) {
				String [] arr = new String[3];
				arr[0] = rs.getString(1);
				arr[1] = rs.getString(2);
				arr[2] = rs.getString(3);
				v.add(arr);
			}
		}catch(Exception ex){
			System.out.println("IndividualReport.java - Importance - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return v;
	}

	public Vector ImportanceMOE(int compID, int surveyIDImpt) throws SQLException 
	{
		String query = "";
		int reliabilityCheck = C. ReliabilityCheck(surveyID);

		String tblName = "tblAvgMean";
		String result = "AvgMean";

		if(reliabilityCheck == 0) {
			tblName = "tblTrimmedMean";
			result = "TrimmedMean";
		}

		query = query + "SELECT tblRatingTask.RatingCode, ";
		query = query + "tblSurveyRating.RatingTaskName, " + tblName + "." + result + " as Result, "+tblName+".Type ";
		query = query + "FROM " + tblName + " INNER JOIN tblRatingTask ON ";
		query = query + tblName + ".RatingTaskID = tblRatingTask.RatingTaskID ";
		query = query + "INNER JOIN tblSurveyRating ON ";
		query = query + "tblRatingTask.RatingTaskID = tblSurveyRating.RatingTaskID AND ";
		query = query + tblName + ".SurveyID = tblSurveyRating.SurveyID ";                      
		query = query + "WHERE " + tblName + ".SurveyID = " + surveyIDImpt + " AND ";
		query = query + tblName + ".TargetLoginID = " + targetID + " AND ";
		query = query + tblName + ".CompetencyID = " + compID + " AND ";
		query = query + "(tblRatingTask.RatingCode = 'IN' OR tblRatingTask.RatingCode = 'IF')";

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		Vector v = new Vector();

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			while(rs.next()) {
				String [] arr = new String[4];
				arr[0] = rs.getString(1);
				arr[1] = rs.getString(2);
				arr[2] = rs.getString(3);
				arr[3] = rs.getString(4);
				v.add(arr);
			}
		}catch(Exception ex){
			System.out.println("IndividualReport.java - Importance - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return v;
	}

	/**
	 * Calculate the average of importance for each competency.
	 * This is only apply for KB Level Analysis.
	 */
	public Vector AvgImportance(int compID) throws SQLException 
	{
		String query = "";

		//try {			
		query = query + "SELECT tblRatingTask.RatingCode, tblSurveyRating.RatingTaskName, ";
		query = query + "cast(avg(tblAvgMean.AvgMean) as numeric(38,2)) AS Result FROM tblAvgMean ";
		query = query + "INNER JOIN tblRatingTask ON tblAvgMean.RatingTaskID = tblRatingTask.RatingTaskID ";
		query = query + "INNER JOIN tblSurveyRating ON ";
		query = query + "tblRatingTask.RatingTaskID = tblSurveyRating.RatingTaskID AND ";
		query = query + "tblAvgMean.SurveyID = tblSurveyRating.SurveyID ";
		query = query + "WHERE tblAvgMean.SurveyID = " + surveyID;
		query = query + " AND tblAvgMean.TargetLoginID = " + targetID;
		query = query + " AND tblAvgMean.CompetencyID = " + compID;
		query = query + " AND tblAvgMean.Type = 1 AND ";
		query = query + "(tblRatingTask.RatingCode = 'IN' OR tblRatingTask.RatingCode = 'IF') ";
		query = query + "group by tblRatingTask.RatingTaskID,tblRatingTask.RatingCode, ";
		query = query + "tblSurveyRating.RatingTaskName";


		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		Vector v = new Vector();

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			while(rs.next()) {
				String [] arr = new String[3];
				arr[0] = rs.getString(1);
				arr[1] = rs.getString(2);
				arr[2] = rs.getString(3);
				v.add(arr);
			}
		}catch(Exception ex){
			System.out.println("IndividualReport.java - AvgImportance - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return v;
	}	

	/**
	 * Calculate the average of gap for each competency.
	 * This is only apply for KB Level Analysis.
	 */
	public double getAvgGap(int compID) throws SQLException 
	{
		double gap = 0;


		String query = "Select cast(AVG(Gap) as numeric(38,2)) from tblGap where SurveyID = " + surveyID;
		query = query + " AND TargetLoginID = " + targetID + " and CompetencyID = " + compID;

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			if(rs.next())
				gap = rs.getDouble(1);


		}catch(Exception ex){
			System.out.println("IndividualReport.java - getAvgGap - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return gap;
	}

	/**
	 * Retrieves the minimum and maximum gap, which was set when create/edit survey.
	 */
	public double [] getMinMaxGap() throws SQLException 
	{
		double gap [] = new double [2];

		String query = "Select MIN_gap, MAX_Gap from tblSurvey where SurveyID = " + surveyID;

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			if(rs.next()) {
				gap[0] = rs.getDouble(1);
				gap[1] = rs.getDouble(2);
			}


		}catch(Exception ex){
			System.out.println("IndividualReport.java - getMinMaxGap - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return gap;
	}	

	/**
	 * Retrieves all target results based on the reliability check.
	 */
	public Vector getAllTargetsResults() throws SQLException 
	{
		int surveyLevel = Integer.parseInt(surveyInfo[0]);

		String query = "";

		int reliabilityCheck = C. ReliabilityCheck(surveyID);

		//try {		
		if(reliabilityCheck == 0) {			
			query = query + "SELECT tblTrimmedMean.CompetencyID, Competency.CompetencyName, ";
			query = query + "cast(AVG(tblTrimmedMean.TrimmedMean) as numeric(38,2)) AS Result ";
			query = query + "FROM tblRatingTask INNER JOIN tblTrimmedMean ON ";
			query = query + "tblRatingTask.RatingTaskID = tblTrimmedMean.RatingTaskID INNER JOIN ";
			query = query + "Competency ON tblTrimmedMean.CompetencyID = Competency.PKCompetency ";
			query = query + "WHERE tblTrimmedMean.SurveyID = " + surveyID;
			query = query + " AND tblTrimmedMean.TargetLoginID <> " + targetID;
			query = query + " AND tblTrimmedMean.Type = 1 AND tblRatingTask.RatingCode = 'CP' ";
			query = query + "GROUP BY tblTrimmedMean.CompetencyID, Competency.CompetencyName ";
			query = query + "order by Competency.CompetencyName";
		} else {
			if(surveyLevel == 0) {
				query = query + "SELECT tblAvgMean.CompetencyID, Competency.CompetencyName, ";
				query = query + "cast(AVG(tblAvgMean.AvgMean) as numeric(38,2)) AS Result ";
				query = query + "FROM tblAvgMean INNER JOIN tblRatingTask ON ";
				query = query + "tblAvgMean.RatingTaskID = tblRatingTask.RatingTaskID ";
				query = query + "INNER JOIN Competency ON ";
				query = query + "tblAvgMean.CompetencyID = Competency.PKCompetency ";
				query = query + "WHERE tblAvgMean.SurveyID = " + surveyID + " AND ";
				query = query + "tblAvgMean.TargetLoginID <> " + targetID + " AND tblAvgMean.Type = 1 AND ";
				query = query + "tblRatingTask.RatingCode = 'CP' ";
				query = query + "GROUP BY tblAvgMean.CompetencyID, Competency.CompetencyName ";
				query = query + "order by Competency.CompetencyName";

			} else {

				query = query + "SELECT tblAvgMean.CompetencyID, Competency.CompetencyName, ";
				query = query + "CAST(AVG(tblAvgMean.AvgMean) AS numeric(38, 2)) AS Result ";
				query = query + "FROM tblRatingTask INNER JOIN tblAvgMean ON ";
				query = query + "tblRatingTask.RatingTaskID = tblAvgMean.RatingTaskID ";
				query = query + "INNER JOIN Competency ON ";
				query = query + "tblAvgMean.CompetencyID = Competency.PKCompetency ";
				query = query + "WHERE tblAvgMean.SurveyID = " + surveyID + " AND ";
				query = query + "tblAvgMean.TargetLoginID <> " + targetID + " AND tblAvgMean.Type = 1 AND ";
				query = query + "tblRatingTask.RatingCode = 'CP' ";
				query = query + "GROUP BY tblAvgMean.CompetencyID, Competency.CompetencyName ";
				query = query + "order by Competency.CompetencyName";
			}

		}
		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		Vector v = new Vector();

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			while(rs.next()) {
				String [] arr = new String[3];
				arr[0] = rs.getString(1);
				arr[1] = rs.getString(2);
				arr[2] = rs.getString(3);
				v.add(arr);
			}
		}catch(Exception ex){
			System.out.println("IndividualReport.java - KBMean - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return v;
	}		

	/**
	 * Retrieves the results under that particular rating code.
	 */
	public Vector CPCPR(String RTCode) throws SQLException 
	{
		String query = "";
		int surveyLevel = Integer.parseInt(surveyInfo[0]);
		int reliabilityCheck = C. ReliabilityCheck(surveyID);

		if(reliabilityCheck == 0) 
		{			
			query = "SELECT tblTrimmedMean.CompetencyID, Competency.CompetencyName, tblTrimmedMean.TrimmedMean as Result ";
			query = query + "FROM tblTrimmedMean INNER JOIN tblRatingTask ON ";
			query = query + "tblTrimmedMean.RatingTaskID = tblRatingTask.RatingTaskID ";
			query = query + "INNER JOIN Competency ON ";
			query = query + "tblTrimmedMean.CompetencyID = Competency.PKCompetency ";
			query = query + "WHERE tblTrimmedMean.SurveyID = " + surveyID + " AND ";
			query = query + "tblTrimmedMean.TargetLoginID = " + targetID + " AND tblTrimmedMean.Type = 1 AND ";
			query = query + "tblRatingTask.RatingCode = '" + RTCode + "' ";
			query = query + "ORDER BY Competency.CompetencyName";
		} 
		else 
		{
			if(surveyLevel == 0) 
			{
				query = "SELECT tblAvgMean.CompetencyID, Competency.CompetencyName, tblAvgMean.AvgMean as Result ";
				query = query + "FROM tblAvgMean INNER JOIN tblRatingTask ON ";
				query = query + "tblAvgMean.RatingTaskID = tblRatingTask.RatingTaskID ";
				query = query + "INNER JOIN Competency ON ";
				query = query + "tblAvgMean.CompetencyID = Competency.PKCompetency ";
				query = query + "WHERE tblAvgMean.SurveyID = " + surveyID + " AND ";
				query = query + "tblAvgMean.TargetLoginID = " + targetID + " AND tblAvgMean.Type = 1 AND ";
				query = query + "tblRatingTask.RatingCode = '" + RTCode + "' ORDER BY Competency.CompetencyName";
			} 
			else 
			{
				query = "SELECT tblAvgMean.CompetencyID, Competency.CompetencyName, ";
				query = query + "CAST(AVG(tblAvgMean.AvgMean) AS numeric(38, 2)) AS Result ";
				query = query + "FROM tblRatingTask INNER JOIN tblAvgMean ON ";
				query = query + "tblRatingTask.RatingTaskID = tblAvgMean.RatingTaskID ";
				query = query + "INNER JOIN Competency ON ";
				query = query + "tblAvgMean.CompetencyID = Competency.PKCompetency ";
				query = query + "WHERE tblAvgMean.SurveyID = " + surveyID + " AND ";
				query = query + "tblAvgMean.TargetLoginID = " + targetID + " AND tblAvgMean.Type = 1 AND ";
				query = query + "tblRatingTask.RatingCode = '" + RTCode + "' GROUP BY tblAvgMean.CompetencyID, ";
				query = query + "Competency.CompetencyName order by Competency.CompetencyName";
			}
		}
		System.out.println("cpcpr HIIIIIIIIIIIIIIIIIIII\n"+query);
		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		Vector v = new Vector();

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			while(rs.next()) {
				String [] arr = new String[3];
				arr[0] = rs.getString(1);
				arr[1] = rs.getString(2);
				arr[2] = rs.getString(3);
				v.add(arr);
			}
		}catch(Exception ex){
			System.out.println("IndividualReport.java - CPCPR - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}
		//System.out.println(query);
		return v;
	}	

	/**
	 * Calculate CP Score, break down to Specific Relation of OTH
	 * @param RTCode
	 * @return CP score
	 * @throws SQLException
	 * @author Maruli
	 */
	public double CPCPRAllianz(int iRTCode, int iCompID, String sRelHigh, String sRelSpec) throws SQLException 
	{
		String query = "";
		double dCP = 0;
		int surveyLevel = Integer.parseInt(surveyInfo[0]);
		int reliabilityCheck = C. ReliabilityCheck(surveyID);

		if(reliabilityCheck == 0) 
		{			
			/*query = query + "SELECT tblTrimmedMean.CompetencyID, Competency.CompetencyName, tblTrimmedMean.TrimmedMean as Result ";
			query = query + "FROM tblTrimmedMean INNER JOIN tblRatingTask ON ";
			query = query + "tblTrimmedMean.RatingTaskID = tblRatingTask.RatingTaskID ";
			query = query + "INNER JOIN Competency ON ";
			query = query + "tblTrimmedMean.CompetencyID = Competency.PKCompetency ";
			query = query + "WHERE tblTrimmedMean.SurveyID = " + surveyID + " AND ";
			query = query + "tblTrimmedMean.TargetLoginID = " + targetID + " AND tblTrimmedMean.Type = 1 AND ";
			query = query + "tblRatingTask.RatingCode = '" + RTCode + "' ";
			query = query + "ORDER BY Competency.CompetencyName";*/
		} 
		else 
		{
			if(surveyLevel == 0) 
			{

			} 
			else 
			{			
				query = "SELECT FKCompetency, CAST(CAST(SUM(AvgMean) AS float) / COUNT(FKCompetency) AS numeric(38, 2)) AS Result ";
				query = query + "FROM (SELECT RB.RatingTaskID, KB.FKCompetency, RB.KeyBehaviourID, "; 
				query = query + "CAST(CAST(SUM(RB.Result) AS float) / COUNT(RB.Result) AS numeric(38, 2)) AS AvgMean, RS.RelationSpecific ";
				query = query + "FROM tblAssignment ASG INNER JOIN ";
				query = query + "tblResultBehaviour RB ON ASG.AssignmentID = RB.AssignmentID INNER JOIN ";
				query = query + "KeyBehaviour KB ON RB.KeyBehaviourID = KB.PKKeyBehaviour LEFT OUTER JOIN ";
				query = query + "tblRelationSpecific RS ON ASG.RTSpecific = RS.SpecificID ";
				query = query + "WHERE (ASG.SurveyID = "+surveyID+") AND (ASG.TargetLoginID = "+targetID+") AND (KB.FKCompetency = "+iCompID+") ";
				query = query + "AND (ASG.RaterStatus IN (1, 2, 4)) AND (ASG.RaterCode LIKE '"+sRelHigh+"') AND (RB.Result <> 0) ";
				query = query + "GROUP BY RB.RatingTaskID, KB.FKCompetency, RB.KeyBehaviourID, RS.RelationSpecific ";
				query = query + "HAVING (RB.RatingTaskID = "+iRTCode+") ";

				if(sRelSpec != "")
					query = query + "AND (RS.RelationSpecific = '"+sRelSpec+"') ";

				query = query + ") DERIVEDTBL GROUP BY FKCompetency";
			}
		}

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			if(rs.next())
				dCP = rs.getDouble("Result");

		}catch(Exception ex){
			System.out.println("IndividualReport.java - CPCPRAllianz - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return dCP;
	}


	/**
	 * Retrieves the results under that particular rating code sorted by CompetencyID
	 */
	public Vector CPCPRSortedID(String RTCode) throws SQLException 
	{
		int surveyLevel = Integer.parseInt(surveyInfo[0]);

		String query = "";

		int reliabilityCheck = C. ReliabilityCheck(surveyID);

		if(reliabilityCheck == 0) {
			query = "SELECT tblTrimmedMean.CompetencyID, Competency.CompetencyName, tblTrimmedMean.TrimmedMean as Result ";
			query = query + "FROM tblTrimmedMean INNER JOIN tblRatingTask ON ";
			query = query + "tblTrimmedMean.RatingTaskID = tblRatingTask.RatingTaskID ";
			query = query + "INNER JOIN Competency ON ";
			query = query + "tblTrimmedMean.CompetencyID = Competency.PKCompetency ";
			query = query + "WHERE tblTrimmedMean.SurveyID = " + surveyID + " AND ";
			query = query + "tblTrimmedMean.TargetLoginID = " + targetID + " AND tblTrimmedMean.Type = 1 AND ";
			query = query + "tblRatingTask.RatingCode = '" + RTCode + "' ";
			query = query + "ORDER BY tblTrimmedMean.CompetencyID";
		} else {
			if(surveyLevel == 0) {
				query = "SELECT tblAvgMean.CompetencyID, Competency.CompetencyName, tblAvgMean.AvgMean as Result ";
				query = query + "FROM tblAvgMean INNER JOIN tblRatingTask ON ";
				query = query + "tblAvgMean.RatingTaskID = tblRatingTask.RatingTaskID ";
				query = query + "INNER JOIN Competency ON ";
				query = query + "tblAvgMean.CompetencyID = Competency.PKCompetency ";
				query = query + "WHERE tblAvgMean.SurveyID = " + surveyID + " AND ";
				query = query + "tblAvgMean.TargetLoginID = " + targetID + " AND tblAvgMean.Type = 1 AND ";
				query = query + "tblRatingTask.RatingCode = '" + RTCode + "' ORDER BY tblAvgMean.CompetencyID";

			} else {
				query = "SELECT tblAvgMean.CompetencyID, Competency.CompetencyName, ";
				query = query + "CAST(AVG(tblAvgMean.AvgMean) AS numeric(38, 2)) AS Result ";
				query = query + "FROM tblRatingTask INNER JOIN tblAvgMean ON ";
				query = query + "tblRatingTask.RatingTaskID = tblAvgMean.RatingTaskID ";
				query = query + "INNER JOIN Competency ON ";
				query = query + "tblAvgMean.CompetencyID = Competency.PKCompetency ";
				query = query + "WHERE tblAvgMean.SurveyID = " + surveyID + " AND ";
				query = query + "tblAvgMean.TargetLoginID = " + targetID + " AND tblAvgMean.Type = 1 AND ";
				query = query + "tblRatingTask.RatingCode = '" + RTCode + "' GROUP BY tblAvgMean.CompetencyID, ";
				query = query + "Competency.CompetencyName ORDER BY tblAvgMean.CompetencyID";
			}
		}


		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		Vector v = new Vector();

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			while(rs.next()) {
				String [] arr = new String[3];
				arr[0] = rs.getString(1);
				arr[1] = rs.getString(2);
				arr[2] = rs.getString(3);
				v.add(arr);
			}
		}catch(Exception ex){
			System.out.println("IndividualReport.java - CPCPRSorted - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return v;	
	}	


	/**
	 * 	Retrieves past survey results under that particular rating code
	 *	This function is used only for Toyota combined report
	 */
	public Vector PastCP(String RTCode) throws SQLException 
	{
		Vector v = new Vector();

		String query = "";
		boolean bPastSurveyExist = false;

		int surveyLevel = Integer.parseInt(surveyInfo[0]);
		int reliabilityCheck = C. ReliabilityCheck(surveyID);

		/*
		 *	Check whether there are any existing survey in the same Job Position as chosen survey
		 */
		query = "SELECT MAX(tblSurvey.SurveyID) AS SurveyID ";
		query = query + "FROM tblSurvey INNER JOIN tblAssignment ON tblSurvey.SurveyID = tblAssignment.SurveyID ";
		query = query + "WHERE (JobPositionID = " + surveyInfo[10] +") ";
		query = query + "AND tblSurvey.SurveyID < " + surveyID;

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		int ID = 0;
		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			if(rs.next()) {
				ID = rs.getInt(1);
			}
		}catch(Exception ex){
			System.out.println("IndividualReport.java - PastCP - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}


		if (ID !=0)
		{
			//rsPastSurvey.next();
			iPastSurveyID = ID;

			if (iPastSurveyID > 0)
			{
				query = "SELECT DISTINCT TargetLoginID FROM tblAssignment WHERE SurveyID = " + iPastSurveyID +
				//Added by Jenty on 26-Sept-06
				//To solve the mising previous CP
				" and TargetLoginID = " + targetID; 

				try{
					con=ConnectionBean.getConnection();
					st=con.createStatement();
					rs=st.executeQuery(query);
					if(rs.next()) {
						iPastTargetLogin = targetID;
						bPastSurveyExist = true;
					}
				}catch(Exception ex){
					System.out.println("IndividualReport.java - PastCP - "+ex.getMessage());
				}
				finally{
					ConnectionBean.closeRset(rs); //Close ResultSet
					ConnectionBean.closeStmt(st); //Close statement
					ConnectionBean.close(con); //Close connection
				}

			}

		}
		// ~~~ END Past Survey Exist ~~~

		if (bPastSurveyExist == true)
		{
			if(reliabilityCheck == 0)
			{
				query = "SELECT tblTrimmedMean.CompetencyID, Competency.CompetencyName, tblTrimmedMean.TrimmedMean as Result ";
				query = query + "FROM tblTrimmedMean INNER JOIN tblRatingTask ON ";
				query = query + "tblTrimmedMean.RatingTaskID = tblRatingTask.RatingTaskID ";
				query = query + "INNER JOIN Competency ON ";
				query = query + "tblTrimmedMean.CompetencyID = Competency.PKCompetency ";
				query = query + "WHERE tblTrimmedMean.SurveyID = " + iPastSurveyID + " AND ";
				query = query + "tblTrimmedMean.TargetLoginID = " + iPastTargetLogin + " AND tblTrimmedMean.Type = 1 AND ";
				query = query + "tblRatingTask.RatingCode = '" + RTCode + "' ";
				query = query + "ORDER BY Competency.PKCompetency";
			}
			else
			{
				if(surveyLevel == 0)
				{
					query = "SELECT tblAvgMean.CompetencyID, Competency.CompetencyName, tblAvgMean.AvgMean as Result ";
					query = query + "FROM tblAvgMean INNER JOIN tblRatingTask ON ";
					query = query + "tblAvgMean.RatingTaskID = tblRatingTask.RatingTaskID ";
					query = query + "INNER JOIN Competency ON ";
					query = query + "tblAvgMean.CompetencyID = Competency.PKCompetency ";
					query = query + "WHERE tblAvgMean.SurveyID = " + iPastSurveyID + " AND ";
					query = query + "tblAvgMean.TargetLoginID = " + iPastTargetLogin + " AND tblAvgMean.Type = 1 AND ";
					query = query + "tblRatingTask.RatingCode = '" + RTCode + "' ORDER BY Competency.PKCompetency";
				}
				else
				{
					query = "SELECT tblAvgMean.CompetencyID, Competency.CompetencyName, ";
					query = query + "CAST(AVG(tblAvgMean.AvgMean) AS numeric(38, 2)) AS Result ";
					query = query + "FROM tblRatingTask INNER JOIN tblAvgMean ON ";
					query = query + "tblRatingTask.RatingTaskID = tblAvgMean.RatingTaskID ";
					query = query + "INNER JOIN Competency ON ";
					query = query + "tblAvgMean.CompetencyID = Competency.PKCompetency ";
					query = query + "WHERE tblAvgMean.SurveyID = " + iPastSurveyID + " AND ";
					query = query + "tblAvgMean.TargetLoginID = " + iPastTargetLogin + " AND tblAvgMean.Type = 1 AND ";
					query = query + "tblRatingTask.RatingCode = '" + RTCode + "' GROUP BY tblAvgMean.CompetencyID, ";
					query = query + "Competency.CompetencyName ORDER BY Competency.PKCompetency";
				}
			}

			try{
				con=ConnectionBean.getConnection();
				st=con.createStatement();
				rs=st.executeQuery(query);

				while(rs.next()) {
					String [] arr = new String[3];
					arr[0] = rs.getString(1);
					arr[1] = rs.getString(2);
					arr[2] = rs.getString(3);
					v.add(arr);
				}
			}catch(Exception ex){
				System.out.println("IndividualReport.java - PastCP - "+ex.getMessage());
			}
			finally{
				ConnectionBean.closeRset(rs); //Close ResultSet
				ConnectionBean.closeStmt(st); //Close statement
				ConnectionBean.close(con); //Close connection
			}

		}	//	bPastSurveyExist

		return v;
	}	


	/**
	 * Retrieve the average or trimmed mean result based on competency and key behaviour for each group.
	 */
	public Vector MeanResultToyota(int RTID, int compID, int KBID) throws SQLException 
	{
		String query = "";
		int surveyLevel = Integer.parseInt(surveyInfo[0]);
		int reliabilityCheck = C. ReliabilityCheck(surveyID);

		String tblName = "tblAvgMean";
		String result = "AvgMean";

		if(reliabilityCheck == 0) {
			tblName = "tblTrimmedMean";
			result = "TrimmedMean";
		}

		if(surveyLevel == 0) {
			query = query + "SELECT " + tblName + ".CompetencyID, ";
			query = query + tblName + ".Type, " + tblName + "." + result;
			query = query + " as Result FROM " + tblName;
			query = query + " WHERE " + tblName + ".SurveyID = " + iPastSurveyID + " AND ";
			query = query + tblName + ".TargetLoginID = " + iPastTargetLogin;
			query = query + " AND " + tblName + ".RatingTaskID = " + RTID;
			query = query + " AND " + tblName + ".CompetencyID = " + compID;
			query = query + " AND " + tblName + ".Type = 1";

		} else {
			//Maruli
			query = query + "SELECT CompetencyID, Type, AVG(" + result + ") as Result ";
			query = query + "FROM " + tblName + " WHERE SurveyID = " + iPastSurveyID + " AND ";
			query = query + "TargetLoginID = " + iPastTargetLogin + " AND RatingTaskID = " + RTID;
			query = query + " AND CompetencyID = " + compID;
			query = query + " AND Type = 1";
			query = query + " GROUP BY CompetencyID, " + result;
		}

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;
		Vector v = new Vector();

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			while(rs.next()) {
				String [] arr = new String[3];
				arr[0] = rs.getString(1);
				arr[1] = rs.getString(2);
				arr[2] = rs.getString(3);
				v.add(arr);
			}
		}catch(Exception ex){
			System.out.println("IndividualReport.java - MeanResultToyota - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return v;
	}


	/**
	 * Retrieves target gaps based on the low, high value, and the type.
	 * Type: 1 = Strength
	 *		 2 = Meet Expectation
	 *		 3 = Developmental Area
	 */
	public Vector getTargetGap(double low, double high, int type) throws SQLException 
	{
		int surveyLevel = Integer.parseInt(surveyInfo[0]);
		String query = "";
		String filter = "";

		if(surveyLevel == 0) {
			switch (type) {
			case 1 : filter = " and tblGap.Gap >= " + low;
			break;
			case 2 : filter = " and tblGap.Gap > " + low + " and tblGap.Gap < " + high;
			break;
			case 3 : filter = " and tblGap.Gap <= " + low;
			break;		 
			}

			query = query + "SELECT tblGap.CompetencyID, Competency.CompetencyName, cast(tblGap.Gap as numeric(38,2)) as Result ";
			query = query + "FROM tblGap INNER JOIN Competency ON tblGap.CompetencyID = Competency.PKCompetency ";
			query = query + "WHERE tblGap.SurveyID = " + surveyID + " AND tblGap.TargetLoginID = " + targetID;
			query = query + filter;
			query = query + " ORDER BY Competency.CompetencyName";
		} else {
			switch (type) {
			case 1 : filter = " having CAST(AVG(tblGap.Gap) AS numeric(38, 2))  >= " + low;
			break;
			case 2 : filter = " having CAST(AVG(tblGap.Gap) AS numeric(38, 2))  > " + low + " and CAST(AVG(tblGap.Gap) AS numeric(38, 2)) < " + high;
			break;
			case 3 : filter = " having CAST(AVG(tblGap.Gap) AS numeric(38, 2))  <= " + low;
			break;		 
			}

			query = query + "SELECT tblGap.CompetencyID, Competency.CompetencyName, ";
			query = query + "CAST(AVG(tblGap.Gap) AS numeric(38, 2)) AS Result ";
			query = query + "FROM tblGap INNER JOIN Competency ON ";
			query = query + "tblGap.CompetencyID = Competency.PKCompetency ";
			query = query + "WHERE tblGap.SurveyID = " + surveyID;
			query = query + " AND tblGap.TargetLoginID = " + targetID;
			query = query + " GROUP BY tblGap.CompetencyID, Competency.CompetencyName ";
			query = query + filter;
			query = query + " ORDER BY Competency.CompetencyName";
		}

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;
		Vector v = new Vector();

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			while(rs.next()) {
				String [] arr = new String[3];
				arr[0] = rs.getString(1);
				arr[1] = rs.getString(2);
				arr[2] = rs.getString(3);
				v.add(arr);
			}
		}catch(Exception ex){
			System.out.println("IndividualReport.java - getTargetGap - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return v;
	}	


	/**
	 * Retrieves rater results for each competency to calculate percentile in Normative Report.
	 */
	public Vector getOtherTargetResults(int ID, int compID) throws SQLException 
	{
		String query = "";
		int surveyLevel = Integer.parseInt(surveyInfo[0]);
		int reliabilityCheck = C. ReliabilityCheck(surveyID);

		if(reliabilityCheck == 0) 
		{			
			query = query + "SELECT tblTrimmedMean.CompetencyID, Competency.CompetencyName, tblTrimmedMean.TrimmedMean as Result ";
			query = query + "FROM tblTrimmedMean INNER JOIN tblRatingTask ON ";
			query = query + "tblTrimmedMean.RatingTaskID = tblRatingTask.RatingTaskID ";
			query = query + "INNER JOIN Competency ON ";
			query = query + "tblTrimmedMean.CompetencyID = Competency.PKCompetency ";
			query = query + "WHERE tblTrimmedMean.SurveyID = " + surveyID + " AND ";
			query = query + "tblTrimmedMean.TargetLoginID = " + ID + " AND tblTrimmedMean.Type = 1 AND ";
			query = query + "tblRatingTask.RatingCode = 'CP' and tblTrimmedMean.CompetencyID = " + compID;				
		} else {
			if(surveyLevel == 0) 
			{
				query = query + "SELECT tblAvgMean.CompetencyID, Competency.CompetencyName, tblAvgMean.AvgMean as Result ";
				query = query + "FROM tblAvgMean INNER JOIN tblRatingTask ON ";
				query = query + "tblAvgMean.RatingTaskID = tblRatingTask.RatingTaskID ";
				query = query + "INNER JOIN Competency ON ";
				query = query + "tblAvgMean.CompetencyID = Competency.PKCompetency ";
				query = query + "WHERE tblAvgMean.SurveyID = " + surveyID + " AND ";
				query = query + "tblAvgMean.TargetLoginID = " + ID + " AND tblAvgMean.Type = 1 AND ";
				query = query + "tblRatingTask.RatingCode = 'CP' and tblAvgMean.CompetencyID = " + compID;

			} else 
			{	
				query = query + "SELECT tblAvgMean.CompetencyID, Competency.CompetencyName, ";
				query = query + "CAST(AVG(tblAvgMean.AvgMean) AS numeric(38, 2)) AS Result ";
				query = query + "FROM tblRatingTask INNER JOIN tblAvgMean ON ";
				query = query + "tblRatingTask.RatingTaskID = tblAvgMean.RatingTaskID ";
				query = query + "INNER JOIN Competency ON ";
				query = query + "tblAvgMean.CompetencyID = Competency.PKCompetency ";
				query = query + "WHERE tblAvgMean.SurveyID = " + surveyID + " AND ";
				query = query + "tblAvgMean.TargetLoginID = " + ID + " AND tblAvgMean.Type = 1 AND ";
				query = query + "tblRatingTask.RatingCode = 'CP' and tblAvgMean.CompetencyID = " + compID;
				query = query + " GROUP BY tblAvgMean.CompetencyID, Competency.CompetencyName";
			}
		}
		//System.out.println("query1 "+query);
		Connection con = null;
		Statement st = null;
		ResultSet rs = null;
		Vector v = new Vector();

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			while(rs.next()) {
				String [] arr = new String[3];
				arr[0] = rs.getString(1);
				arr[1] = rs.getString(2);
				arr[2] = rs.getString(3);
				v.add(arr);
			}
		}catch(Exception ex){
			System.out.println("IndividualReport.java - getOtherTargetResults - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return v;
	}	


	/**
	 * Get total targets which status is completed included hinself
	 */
	public int TotalTargetsIncluded() throws SQLException 
	{
		int total = 0;

		String query = "SELECT count(DISTINCT tblAssignment.TargetLoginID) as total FROM tblAssignment INNER JOIN ";
		query = query + "[User] ON tblAssignment.TargetLoginID = [User].PKUser INNER JOIN ";
		query = query + "[User] User_1 ON [User].Group_Section = User_1.Group_Section AND ";
		query = query + "tblAssignment.TargetLoginID = User_1.PKUser WHERE ";
		query = query + "tblAssignment.SurveyID = " + surveyID;
		query = query + " AND tblAssignment.RaterStatus <> 0";
		query += " AND tblAssignment.TargetLoginID IN (SELECT TargetLoginID FROM tblAssignment ";
		query += "WHERE SurveyID = " + surveyID + " AND RaterStatus <> 0 AND RaterCode <> 'SELF')";

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			if(rs.next())
				total = rs.getInt(1);

		}catch(Exception ex){
			System.out.println("IndividualReport.java - totalTargetsIncluded - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return total;
	}


	/**
	 * Get total targets which status is completed
	 */
	public int TotalTargets() throws SQLException 
	{
		int total = 0;
		String query = "SELECT count(DISTINCT tblAssignment.TargetLoginID) as total FROM tblAssignment INNER JOIN ";
		query = query + "[User] ON tblAssignment.TargetLoginID = [User].PKUser INNER JOIN ";
		query = query + "[User] User_1 ON [User].Group_Section = User_1.Group_Section AND ";
		query = query + "tblAssignment.TargetLoginID = User_1.PKUser WHERE ";
		query = query + "tblAssignment.SurveyID = " + surveyID;
		query = query + " AND tblAssignment.RaterStatus <> 0 AND tblAssignment.TargetLoginID <> " + targetID;

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			if(rs.next())
				total = rs.getInt(1);

		}catch(Exception ex){
			System.out.println("IndividualReport.java - totalTargets - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return total;
	}					

	/**
	 * Retrieves all the comments input upon fill in the questionnaire.
	 * @param raterCode
	 * @param compID
	 * @param KBID
	 * @return
	 * @throws SQLException
	 */
	public Vector getComments(String raterCode, int compID, int KBID) throws SQLException 
	{
		String query = "";
		int surveyLevel = Integer.parseInt(surveyInfo[0]);

		if(surveyLevel == 0) {
			query = query + "SELECT Competency.CompetencyName, tblComment.Comment, PKCompetency ";
			query = query + "FROM tblAssignment INNER JOIN tblComment ON ";
			query = query + "tblAssignment.AssignmentID = tblComment.AssignmentID INNER JOIN ";
			query = query + "[User] ON tblAssignment.TargetLoginID = [User].PKUser INNER JOIN ";
			query = query + "Competency ON tblComment.CompetencyID = Competency.PKCompetency ";
			query = query + "WHERE tblAssignment.SurveyID = " + surveyID;
			query = query + " AND tblAssignment.TargetLoginID = " + targetID;
			query = query + " AND tblAssignment.RaterCode LIKE '" + raterCode + "'";
			query = query + " AND Competency.PKCompetency = " + compID;
			query = query + " AND tblComment.Comment != ''"; //Added addition condition to filter out competencies that does not have any comments, Sebastian 19 July 2010
			query = query + " ORDER BY tblComment.Comment";
		} else {
			query = query + "SELECT Competency.CompetencyName, tblComment.Comment, KeyBehaviour.KeyBehaviour ";
			query = query + "FROM tblAssignment INNER JOIN tblComment ON ";
			query = query + "tblAssignment.AssignmentID = tblComment.AssignmentID INNER JOIN ";
			query = query + "[User] ON tblAssignment.TargetLoginID = [User].PKUser INNER JOIN ";
			query = query + "Competency ON tblComment.CompetencyID = Competency.PKCompetency ";
			query = query + "INNER JOIN KeyBehaviour ON ";
			query = query + "tblComment.KeyBehaviourID = KeyBehaviour.PKKeyBehaviour ";
			query = query + "WHERE tblAssignment.SurveyID = " + surveyID;
			query = query + " AND tblAssignment.TargetLoginID = " + targetID;
			query = query + " AND tblAssignment.RaterCode LIKE '" + raterCode + "'";
			query = query + " AND Competency.PKCompetency = " + compID;
			query = query + " AND KeyBehaviour.PKKeyBehaviour = " + KBID;
			query = query + " AND tblComment.Comment != ''"; //Added addition condition to filter out key behaviours that does not have any comments, Sebastian 19 July 2010
			query = query + " ORDER BY tblComment.Comment";												
		}

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;
		Vector v = new Vector();

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			while(rs.next()) {
				String [] arr = new String[3];
				arr[0] = rs.getString(1);
				arr[1] = rs.getString(2);
				arr[2] = rs.getString(3);
				v.add(arr);
			}
		}catch(Exception ex){
			System.out.println("IndividualReport.java - getComments - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return v;
	}	

	/**
	 * Get comments. Allianz does not want to break down into KB.
	 * @param raterCode
	 * @param compID
	 * @return
	 * @throws SQLException
	 */
	public Vector getCommentsAllianz(String raterCode, int compID) throws SQLException 
	{
		String query = "SELECT Competency.CompetencyName, tblComment.Comment ";
		query = query + "FROM tblAssignment INNER JOIN tblComment ON ";
		query = query + "tblAssignment.AssignmentID = tblComment.AssignmentID INNER JOIN ";
		query = query + "[User] ON tblAssignment.TargetLoginID = [User].PKUser INNER JOIN ";
		query = query + "Competency ON tblComment.CompetencyID = Competency.PKCompetency ";
		query = query + "WHERE tblAssignment.SurveyID = " + surveyID;
		query = query + " AND tblAssignment.TargetLoginID = " + targetID;
		query = query + " AND tblAssignment.RaterCode LIKE '" + raterCode + "'";
		query = query + " AND Competency.PKCompetency = " + compID;
		query = query + " ORDER BY tblComment.Comment";

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;
		Vector v = new Vector();

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			while(rs.next()) {
				String [] arr = new String[3];
				arr[0] = rs.getString(1);
				arr[1] = rs.getString(2);
				arr[2] = rs.getString(3);
				v.add(arr);
			}
		}catch(Exception ex){
			System.out.println("IndividualReport.java - getCommentsAllianz - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return v;
	}

	/**
	 * Get target gap.
	 */
	public Vector getTargetGap() throws SQLException 
	{
		String query = "";
		int surveyLevel = Integer.parseInt(surveyInfo[0]);

		if(surveyLevel == 0) {
			query = query + "SELECT CompetencyID, Competency.CompetencyName, Gap FROM tblGap ";
			query += "INNER JOIN Competency ON tblGap.CompetencyID = Competency.PKCompetency WHERE ";
			query += "SurveyID = " + surveyID + " AND TargetLoginID = " + targetID;
			query += " ORDER BY CompetencyID";
		} else {
			query = query + "SELECT CompetencyID, Competency.CompetencyName, CAST(AVG(Gap) AS numeric(38, 2)) AS Gap ";
			query += "FROM tblGap INNER JOIN Competency ON tblGap.CompetencyID = Competency.PKCompetency WHERE SurveyID = " + surveyID;
			query += " AND TargetLoginID = " + targetID;
			query += " GROUP BY CompetencyID, Competency.CompetencyName ORDER BY Competency.CompetencyName, tblGap.CompetencyID";										
		}

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;
		Vector v = new Vector();

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			while(rs.next()) {
				String [] arr = new String[3];
				arr[0] = rs.getString(1);
				arr[1] = rs.getString(2);
				arr[2] = rs.getString(3);
				v.add(arr);
			}
		}catch(Exception ex){
			System.out.println("IndividualReport.java - getTargetGap - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return v;
	}


	/**
	 * Get target gap.
	 */
	public Vector getTargetGapToyota(String sGapQuery) throws SQLException 
	{
		String query = "";
		int surveyLevel = Integer.parseInt(surveyInfo[0]);

		if(surveyLevel == 0) {
			query = query + "SELECT CompetencyID, Competency.CompetencyName, Gap FROM tblGap ";
			query += "INNER JOIN Competency ON tblGap.CompetencyID = Competency.PKCompetency WHERE ";
			query += "SurveyID = " + surveyID + " AND TargetLoginID = " + targetID + " ";
			query += sGapQuery;
		} else {
			query = query + "SELECT CompetencyID, Competency.CompetencyName, CAST(AVG(Gap) AS numeric(38, 2)) AS Gap ";
			query += "FROM tblGap INNER JOIN Competency ON tblGap.CompetencyID = Competency.PKCompetency WHERE SurveyID = " + surveyID;
			query += " AND TargetLoginID = " + targetID;
			query += sGapQuery;
			query += " GROUP BY CompetencyID, Competency.CompetencyName ORDER BY Competency.CompetencyName, tblGap.CompetencyID";
		}

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;
		Vector v = new Vector();

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			while(rs.next()) {
				String [] arr = new String[3];
				arr[0] = rs.getString(1);
				arr[1] = rs.getString(2);
				arr[2] = rs.getString(3);
				v.add(arr);
			}
		}catch(Exception ex){
			System.out.println("IndividualReport.java - getTargetGapToyota - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return v;
	}


	public Vector getOtherTargetGap(int ID, int compID) throws SQLException 
	{
		String query = "";
		int surveyLevel = Integer.parseInt(surveyInfo[0]);

		if(surveyLevel == 0) {
			query = query + "SELECT CompetencyID, Competency.CompetencyName, Gap FROM tblGap ";
			query += "INNER JOIN Competency ON tblGap.CompetencyID = Competency.PKCompetency WHERE ";
			query += "SurveyID = " + surveyID + " AND TargetLoginID = " + ID + " and CompetencyID = " + compID;
			query += " ORDER BY CompetencyID";
		} else {
			query = query + "SELECT CompetencyID, Competency.CompetencyName, CAST(AVG(Gap) AS numeric(38, 2)) AS Gap ";
			query += "FROM tblGap INNER JOIN Competency ON tblGap.CompetencyID = Competency.PKCompetency WHERE SurveyID = " + surveyID;
			query += " AND TargetLoginID = " + ID + " and CompetencyID = " + compID;;
			query += " GROUP BY CompetencyID, Competency.CompetencyName ORDER BY Competency.CompetencyName, tblGap.CompetencyID";										
		}

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;
		Vector v = new Vector();

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			while(rs.next()) {
				String [] arr = new String[3];
				arr[0] = rs.getString(1);
				arr[1] = rs.getString(2);
				arr[2] = rs.getString(3);
				v.add(arr);
			}
		}catch(Exception ex){
			System.out.println("IndividualReport.java - getOtherTargetGap - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return v;
	}	


	/**
	 * Get all Targets' ID which status is completed
	 */
	public Vector TargetsID() {

		String query = "SELECT DISTINCT tblAssignment.TargetLoginID FROM tblAssignment INNER JOIN ";
		query = query + "[User] ON tblAssignment.TargetLoginID = [User].PKUser INNER JOIN ";
		query = query + "[User] User_1 ON [User].Group_Section = User_1.Group_Section AND ";
		query = query + "tblAssignment.TargetLoginID = User_1.PKUser WHERE ";
		query = query + "tblAssignment.SurveyID = " + surveyID;
		query = query + " AND tblAssignment.RaterStatus <> 0 AND tblAssignment.TargetLoginID <> " + targetID;

		// added in so that target who only completed by SELF does not counted as complete.
		query += " AND RaterCode <> 'SELF' ";

		query = query + " ORDER BY tblAssignment.TargetLoginID";

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;
		Vector v = new Vector();

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			while(rs.next()) {
				v.add(new Integer(rs.getInt(1)));
			}
		}catch(Exception ex){
			System.out.println("IndividualReport.java - getOtherTargetGap - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return v;
	}

	/***************************************** SPREADSHEET ********************************************************/

	/**
	 * Initialize all the processes dealing with Excel Application.
	 */
	public void InitializeExcel(String savedFileName) throws IOException, Exception 
	{	
		//System.out.println("2. Excel Initialisation Starts");

		storeURL 	= "file:///" + ST.getOOReportPath() + savedFileName;
		String templateURL 	= "file:///" + ST.getOOReportTemplatePath() + "Individual Report Template.xls";

		xRemoteServiceManager = OO.getRemoteServiceManager("uno:socket,host=localhost,port=2002;urp;StarOffice.ServiceManager");
		xDoc = OO.openDoc(xRemoteServiceManager, templateURL);

		//save as the template into a new file first. This is to avoid the template being used.		
		OO.storeDocComponent(xRemoteServiceManager, xDoc, storeURL);
		OO.closeDoc(xDoc);

		//open up the saved file and modify from there
		xDoc = OO.openDoc(xRemoteServiceManager, storeURL);
		xSpreadsheet = OO.getSheet(xDoc, "Sheet1");
		xSpreadsheet2 = OO.getSheet(xDoc, "Sheet2");

		//System.out.println("2. Excel Initialisation Completed");
	}


	/**
	 * 	Initialize all the processes dealing with Excel Application. (For Toyota combined report)
	 */
	public void InitializeExcelToyota(String savedFileName) throws IOException, Exception 
	{	
		//System.out.println("2. Excel Initialisation Starts");

		storeURL 	= "file:///" + ST.getOOReportPath() + savedFileName;
		String templateURL 	= "file:///" + ST.getOOReportTemplatePath() + "Individual Report Template Combined.xls";

		xRemoteServiceManager = OO.getRemoteServiceManager("uno:socket,host=localhost,port=8100;urp;StarOffice.ServiceManager");
		xDoc = OO.openDoc(xRemoteServiceManager, templateURL);

		//save as the template into a new file first. This is to avoid the template being used.		
		OO.storeDocComponent(xRemoteServiceManager, xDoc, storeURL);		
		OO.closeDoc(xDoc);

		//open up the saved file and modify from there
		xDoc = OO.openDoc(xRemoteServiceManager, storeURL);
		xSpreadsheet = OO.getSheet(xDoc, "Individual Report");
		xSpreadsheet2 = OO.getSheet(xDoc, "Sheet2");

		//	System.out.println("2. Excel Initialisation Completed");
	}

	/**
	 * Initialize excel. For both customised and standard report
	 * @param sSavedFileName	Name of the file when prompted to save
	 * @param sTemplateName		Name of the template of the excel file
	 * @throws IOException
	 * @throws Exception
	 */
	public void InitializeExcel(String sSavedFileName, String sTemplateName) throws IOException, Exception
	{
		//System.out.println("2. Excel Initialisation Starts");

		storeURL 	= "file:///" + ST.getOOReportPath() + sSavedFileName;
		String templateURL 	= "file:///" + ST.getOOReportTemplatePath() + sTemplateName;

		System.out.println(storeURL);
		System.out.println(templateURL);

		xRemoteServiceManager = OO.getRemoteServiceManager("uno:socket,host=localhost,port=2002;urp;StarOffice.ServiceManager");

		xDoc = OO.openDoc(xRemoteServiceManager, templateURL);


		//save as the template into a new file first. This is to avoid the template being used.		
		OO.storeDocComponent(xRemoteServiceManager, xDoc, storeURL);		
		OO.closeDoc(xDoc);

		//open up the saved file and modify from there
		xDoc = OO.openDoc(xRemoteServiceManager, storeURL);
		xSpreadsheet = OO.getSheet(xDoc, "Individual Report");
		xSpreadsheet2 = OO.getSheet(xDoc, "Sheet2");

		//	System.out.println("2. Excel Initialisation Completed");
	}

	public void InitializeExcelDevMap(String sSavedFileName, String sTemplateName) throws IOException, Exception
	{
		//System.out.println("2. Excel Initialisation Starts");

		storeURL 	= "file:///" + ST.getOOReportPath() + sSavedFileName;
		String templateURL 	= "file:///" + ST.getOOReportTemplatePath() + sTemplateName;

		xRemoteServiceManager = OO.getRemoteServiceManager("uno:socket,host=localhost,port=8100;urp;StarOffice.ServiceManager");
		xDoc = OO.openDoc(xRemoteServiceManager, templateURL);

		//save as the template into a new file first. This is to avoid the template being used.		
		OO.storeDocComponent(xRemoteServiceManager, xDoc, storeURL);		
		OO.closeDoc(xDoc);

		//open up the saved file and modify from there
		xDoc = OO.openDoc(xRemoteServiceManager, storeURL);

		xSpreadsheet0 = OO.getSheet(xDoc, "Cover");
		xSpreadsheet = OO.getSheet(xDoc, "Dev Map");
		xSpreadsheet2 = OO.getSheet(xDoc, "Quadrant Details");
	}

	/**
	 * Replace words with <> tags with another word
	 * @throws Exception
	 * @throws IOException
	 */
	public void Replacement() throws Exception, IOException
	{
		//System.out.println("3. Replacement Starts");
		//System.out.println("1OK");
		int surveyLevel = Integer.parseInt(surveyInfo[0]);
		int cluster = Integer.parseInt(surveyInfo[9]);
		String after;
		String clusterOpt;
		//System.out.println("2OK");	

		if(surveyLevel == 0) {		
			after = "(Competency Level)";

			//if (ST.LangVer == 2)	//Commented away to allow translation below, Chun Yeong 1 Aug 2011
			//after = "(Tingkat Kompetensi)";

		} else {		
			after = "(Key Behaviour Level)";
			//if (ST.LangVer == 2)	//Commented away to allow translation below, Chun Yeong 1 Aug 2011
			//after = "(Tingkat Perilaku Kunci)";
		}


		//Allow dynamic translation, Chun Yeong 1 Aug 2011
		OO.findAndReplace(xSpreadsheet, "<Comp/KB Level>", trans.tslt(templateLanguage, after));
		OO.findAndReplace(xSpreadsheet, "<Job Position>", surveyInfo[1]);

		//Changed the default language to English by Chun Yeong 9 Jun 2011
		//Commented away to allow translation below, Chun Yeong 1 Aug 2011
		//if (ST.LangVer == 2) //Indonesian
		//	OO.findAndReplace(xSpreadsheet, "<Target Name:>", "Nama Target: " + UserName());
		//else // if (ST.LangVer == 1) English
		//	OO.findAndReplace(xSpreadsheet, "<Target Name:>", "Target Name: " + UserName());

		//Allow dynamic translation, Chun Yeong 1 Aug 2011
		OO.findAndReplace(xSpreadsheet, "<Target Name:>", trans.tslt(templateLanguage, "Target Name") + ": " + UserName());
		System.out.println("3. Replacement Completed");
	}

	public void replacementDevelopmentMap() throws Exception, IOException
	{
		//System.out.println("3. Replacement Starts");

		OO.findAndReplace(xSpreadsheet0, "<target>", UserName());

		//Changed the default language to English by Chun Yeong 9 Jun 2011
		if (ST.LangVer == 2) { //Indonesian
			OO.findAndReplace(xSpreadsheet, "<competency>", "Kompetensi");
			OO.findAndReplace(xSpreadsheet, "<behaviour>", "Perilaku Kunci");
			OO.findAndReplace(xSpreadsheet, "<positive>", "Selisih Positif");
			OO.findAndReplace(xSpreadsheet, "<negative>", "Selisih Negatif");
		}
		else { //English
			OO.findAndReplace(xSpreadsheet, "<competency>", "Competency");
			OO.findAndReplace(xSpreadsheet, "<behaviour>", "Key Behaviour");
			OO.findAndReplace(xSpreadsheet, "<positive>", "Positive Gap");
			OO.findAndReplace(xSpreadsheet, "<negative>", "Negative Gap");
		}
	}

	/**
	 * Replace words with <> tags with another word (Toyota)
	 * @throws Exception
	 * @throws IOException
	 */
	public void ReplacementToyota() throws Exception, IOException{

		//System.out.println("3. Replacement Starts");

		OO.findAndReplace(xSpreadsheet, "<Name>", UserName());
		OO.findAndReplace(xSpreadsheet, "<Job Title>", surveyInfo[1].toUpperCase());
		OO.findAndReplace(xSpreadsheet, "<EmpID>", surveyInfo[2]);
		OO.findAndReplace(xSpreadsheet, "<Company Name>", surveyInfo[7].toUpperCase());
		OO.findAndReplace(xSpreadsheet, "<Department>", surveyInfo[8].toUpperCase());
		OO.findAndReplace(xSpreadsheet, "<Group>", surveyInfo[9].toUpperCase());

		//System.out.println("3. Replacement Completed");
	}

	/**
	 * Replace words with <> tags with another word (Allianz)
	 * @throws Exception
	 * @throws IOException
	 */
	public void ReplacementAllianz() throws Exception, IOException
	{
		//System.out.println("3. Replacement Starts");
		OO.findAndReplace(xSpreadsheet, "<Target Name:>", "Assessee: " + UserName());
		//System.out.println("3. Replacement Completed");
	}

	/**
	 * Insert in the one legend in the indicated row and column
	 * @author Qiao Li 22 Dec 2009
	 * @throws Exception
	 * 
	 */
	public void InsertIndividualProfileLegend(char num, String title, String desc, int row, int col) throws Exception{
		OO.insertString(xSpreadsheet, num+".", row, column);
		OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, title), row, column+1);
		OO.mergeCells(xSpreadsheet, column+1,endColumn, row, row);
		OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, desc), row+1, column+1);
		OO.mergeCells(xSpreadsheet, column+1, endColumn, row+1, row+1);
	}

	/**
	 * Insert in the appropriate legends depending on whether there is CPR and whether split "Others" to "Subordinates" and "Peers"
	 * @author Qiao Li 22 Dec 2009
	 * precondition: the xSpreadsheet of the template should contain string "<Profile Legend>"
	 */
	public void InsertProfileLegend(){
		int[] address;
		try {
			address = OO.findString(xSpreadsheet, "<Profile Legend>");
			column = address[0];
			row = address[1];
			char i = 'a';
			int j = 0;


			OO.findAndReplace(xSpreadsheet, "<Profile Legend>", "");

			if (iNoCPR == 0){//there is CPR
				InsertIndividualProfileLegend(i, "Current Proficiency Required - All (CPR - ALL)", 
						"This refers to the proficiency level that is required for the current job as perceived by all the other respondents.",
						row+j, column);
				i++;
				j+=3;	
			}
			InsertIndividualProfileLegend(i, "Current Proficiency - All (CP - ALL)", 
					"This refers to the target's proficiency at the current moment, as perceived by all the respondents excluding self.",
					row+j, column);
			i++;
			j+=3;
			InsertIndividualProfileLegend(i, "Current Proficiency - Superior (CP - Superior)", 
					"This refers to the target's proficiency at the current moment, as perceived by superiors only.",
					row+j, column);
			i++;
			j+=3;

			if (splitOthers == 0){//No split of others
				InsertIndividualProfileLegend(i, "Current Proficiency - Others (CP - Others)", 
						"This refers to the Target's proficiency as perceived by all the other respondents at the current moment, excluding the input(s) of the Superior.",
						row+j, column);
				i++;
				j+=3;
			}
			else{//split Others to Subordinates and Peers
				InsertIndividualProfileLegend(i, "Current Proficiency - Subordinates (CP - Subordinates)", 
						"This refers to the Target's proficiency at the current moment, as perceived by all the subordinates.",
						row+j, column);
				i++;
				j+=3;
				InsertIndividualProfileLegend(i, "Current Proficiency - Peers (CP - Peers)", 
						"This refers to the Target's proficiency at the current moment, as perceived by all the peers.",
						row+j, column);
				i++;
				j+=3;

			}
			InsertIndividualProfileLegend(i, "Current Proficiency - Self (CP - Self)", 
					"This refers to the target's proficiency as perceived by himself.",
					row+j, column);
			i++;
			j+=3;
			if (iNoCPR == 0){//There is CPR -> there is Gap
				//Change from CPR (All) - CP (All) to CP(All) to CPR(All), Mark Oei 22 Mark 2010
				InsertIndividualProfileLegend(i, "Gap", 
						"Gap = Current Proficiency (All) minus Current Proficiency Required (All)",
						row+j, column);
				i++;
				j+=3;

			}
			else if (CPRorFPR == 2){
				InsertIndividualProfileLegend(i, "Gap", 
						"Gap =  FPR (All) minus Current Proficiency (All)",
						row+j, column);
				i++;
				j+=3;
			}
			//Remove the following "between all the raters' score given for that particular rating task." after 'extent of agreement'.
			//Change wordings for clarify, Mark Oei 22 Mark 2010
			InsertIndividualProfileLegend(i, "Level Of Agreement", 
					"This refers to the extent of agreement amongst all the Current Proficiency scores for all the raters. The greater the agreement, the higher the percentage.",
					row+j, column);
			i++;
			j+=3;
		} catch (Exception e) {
			System.out.println("IndividualReport - InsertProfileLegend()");
			e.printStackTrace();
		}



	}

	/**
	 * Get Group Current Competency value of a specific competency from database base on surveyID and targetID
	 * @param compName the competency name
	 * @return Vector
	 * @throws SQLException
	 * @author Chun Yeong
	 * @since v1.3.12.108 //29 Jun 2011
	 */
	public double getGroupCPForOneCompetency(String compName) throws SQLException 
	{
		String query = "";	
		Double result = 0.0;

		query = "SELECT Competency.PKCompetency AS CompetencyID, Competency.CompetencyName, " 
			+ "ROUND(AVG(tblAvgMean.AvgMean),2) AS Result FROM tblAvgMean INNER JOIN Competency ON " 
			+ "tblAvgMean.CompetencyID = Competency.PKCompetency INNER JOIN tblRatingTask ON " 
			+ "tblAvgMean.RatingTaskID = tblRatingTask.RatingTaskID INNER JOIN [User] ON " 
			+ "[User].PKUser = tblAvgMean.TargetLoginID "
			+ "WHERE Competency.CompetencyName = '" + compName + "' AND tblAvgMean.SurveyID = " + surveyID + " AND tblAvgMean.Type = 1 AND tblRatingTask.RatingCode = 'CP' AND " 
			+ "tblAvgMean.TargetLoginID IN "
			+ "(SELECT TargetLoginID FROM tblAssignment INNER JOIN [USER] ON [USER].PKUser = tblAssignment.TargetLoginID " 
			+ "WHERE SurveyID = " + surveyID + " AND RaterCode <> 'SELF' AND RaterStatus IN (1, 2, 4) AND "
			+ "[USER].FKOrganization = (SELECT FKOrganization FROM [USER] WHERE PKUser = " + targetID + ")) "
			+ "GROUP BY tblAvgMean.SurveyID, Competency.PKCompetency, Competency.CompetencyName " 
			+ "ORDER BY Competency.CompetencyName";

		Vector v = new Vector();
		Connection con = null;
		Statement st = null;
		ResultSet rs = null;
		System.out.println("QUERY: \n"+query);
		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);
			while(rs.next()) {
				result = rs.getDouble("Result");
			}
		}catch(Exception ex){
			System.out.println("IndividualReport.java - getGroupCPForOneCompetency - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return result;
	}

	/**
	 * Get Group Current Competencies from database base on surveyID and targetID
	 * @return Vector
	 * @throws SQLException
	 * @author Chun Yeong
	 * @since v1.3.12.96 //2 Jun 2011
	 */
	public Vector getGroupCP() throws SQLException 
	{
		String query = "";	

		query = "SELECT Competency.PKCompetency AS CompetencyID, Competency.CompetencyName, " 
			+ "ROUND(AVG(tblAvgMean.AvgMean),2) AS Result FROM tblAvgMean INNER JOIN Competency ON " 
			+ "tblAvgMean.CompetencyID = Competency.PKCompetency INNER JOIN tblRatingTask ON " 
			+ "tblAvgMean.RatingTaskID = tblRatingTask.RatingTaskID INNER JOIN [User] ON " 
			+ "[User].PKUser = tblAvgMean.TargetLoginID "
			+ "WHERE tblAvgMean.SurveyID = " + surveyID + " AND tblAvgMean.Type = 1 AND tblRatingTask.RatingCode = 'CP' AND " 
			+ "tblAvgMean.TargetLoginID IN "
			+ "(SELECT TargetLoginID FROM tblAssignment INNER JOIN [USER] ON [USER].PKUser = tblAssignment.TargetLoginID " 
			+ "WHERE SurveyID = " + surveyID + " AND RaterCode <> 'SELF' AND RaterStatus IN (1, 2, 4) AND "
			+ "[USER].FKOrganization = (SELECT FKOrganization FROM [USER] WHERE PKUser = " + targetID + ")) "
			+ "GROUP BY tblAvgMean.SurveyID, Competency.PKCompetency, Competency.CompetencyName " 
			+ "ORDER BY Competency.CompetencyName";

		Vector v = new Vector();
		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);
			while(rs.next()) {
				voRatingResult vo = new voRatingResult();
				vo.setCompetencyID(rs.getInt("CompetencyID"));
				vo.setCompetencyName(rs.getString("CompetencyName"));
				vo.setResult(rs.getDouble("Result"));

				v.add(vo);
			}
		}catch(Exception ex){
			System.out.println("IndividualReport.java - getGroupCP - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return v;
	}

	/**
	 * Write CP versus CPR results to excel.
	 */
	public void InsertCPvsCPR() throws SQLException, IOException, Exception {

		//System.out.println("4. CP Versus CPR Starts");

		int iNoOfRT 	= 0;
		String RTCode 	= "";
		Vector vComp = Competency(0);

		Vector RT 	= RatingTask();
		Vector CP 	= new Vector();
		Vector CPR 	= new Vector();
		Vector vGCP = new Vector();	//Added for Group Current Proficiency (GCP) by Chun Yeong 2 Jun 2011
		CPRorFPR 		= 1;		// 1=cpr, 2=fpr

		int [] address = OO.findString(xSpreadsheet, "<CP versus CPR Graph>");

		column = address[0];
		row = address[1];

		OO.findAndReplace(xSpreadsheet, "<CP versus CPR Graph>", "");		
		//OO.insertRows(xSpreadsheet, startColumn, endColumn, row, row+18, 18, 1);
		//set as no CPR first (Qiao Li 21 Dec 2009)
		iNoCPR = 1;
		for(int i=0; i<RT.size(); i++) {
			votblSurveyRating vo = (votblSurveyRating)RT.elementAt(i);

			RTCode 	=  vo.getRatingCode();

			//System.out.println(RTCode);
			if(RTCode.equals("CP") || RTCode.equals("CPR") || RTCode.equals("FPR")) {
				if(RTCode.equals("CP"))
					CP = CPCPR(RTCode);
				else {
					CPR = CPCPR(RTCode);
					if(RTCode.equals("CPR")) {
						CPRorFPR = 1;
						iNoCPR = 0;	// CPR is chosen in this survey
					} else {
						CPRorFPR = 2;
					}
				}
			}
			iNoOfRT++;
		}

		//Chart Source Data
		//Vector vComp = CompetencyByName();
		//		int total = totalCompetency();		// 1 for all		
		//		String title = "Current Proficiency Vs Required Proficiency";

		int r = row;
		int c = 0;

		OO.insertString(xSpreadsheet2, "CP", r, c+1);
		if(CPRorFPR == 1){
			//only insert CPR values when there is CPR.i.e do not insert when all are 0s
			//Qiao Li 21 Dec 2009
			if (iNoCPR == 0){
				OO.insertString(xSpreadsheet2, "CPR", r, c+2);

				//Added, if there are CPR values, display/not display GCP on 3 column, by Chun Yeong 2 Jun 2011 
				if(isGroupCPLine) {
					OO.insertString(xSpreadsheet2, "GCP", r, c+3);
				}
			}
			else {
				//Added, if there are no CPR values, display/not display GCP on 2 column, by Chun Yeong 2 Jun 2011
				if(isGroupCPLine){
					OO.insertString(xSpreadsheet2, "GCP", r, c+2);
				}
			}
		}
		else {
			OO.insertString(xSpreadsheet2, "FPR", r, c+2);
			//			title = "Current Proficiency Vs Future Required Proficiency";
		}

		r++;

		vGapSorted.clear();
		vGapUnsorted.clear();
		vCompID.clear();
		vCPValues.clear(); // clear the object, Mark Oei 16 April 2010

		double dCP = 0;
		double dCPR = 0;

		//Added for Group Current Proficiency (GCP) by Chun Yeong 2 Jun 2011
		double dGCP = 0;
		vGCP = getGroupCP();

		for(int i=0; i<vComp.size(); i++) {

			voCompetency voComp = (voCompetency)vComp.elementAt(i);
			int compID 		= voComp.getCompetencyID();
			String statement = voComp.getCompetencyName();		
			String compName = UnicodeHelper.getUnicodeStringAmp(statement);

			dCP 	= 0;
			dCPR 	= 0;
			dGCP 	= 0; //Added for Group Current Proficiency (GCP) by Chun Yeong 2 Jun 2011

			if(CP.size() != 0 && i<CP.size()) {
				String arr [] = (String[])CP.elementAt(i);
				dCP = Double.parseDouble(arr[2]);
			}

			if(CPR.size() != 0 && i<CPR.size()) {
				String arr [] = (String[])CPR.elementAt(i);
				dCPR = Double.parseDouble(arr[2]);
			}

			//Added to insert values into Group Current Proficiency (GCP) by Chun Yeong 2 Jun 2011
			if(vGCP.size() != 0 && i<vGCP.size()){
				voRatingResult voGCP = (voRatingResult) vGCP.elementAt(i);
				dGCP = voGCP.getResult();
			}

			double gap = Math.round((dCP - dCPR) * 100.0) / 100.0;

			vCompID.add(new Integer(compID));
			vGapSorted.add(new String[]{compName, Double.toString(gap)});
			vGapUnsorted.add(new String[]{compName, Double.toString(gap)});
			vCPValues.add(new String[] {compName, Double.toString(dCP)}); //add competency name and CP values to object, Mark Oei 16 April 2010

			//Added translation for the competency name, Chun Yeong 1 Aug 2011
			OO.insertString(xSpreadsheet2, getTranslatedCompetency(compName).elementAt(0).toString(), r, c);
			OO.insertNumeric(xSpreadsheet2, dCP, r, c+1);

			//only insert CPR values when there is CPR.i.e do not insert when all are 0s
			//Qiao Li 21 Dec 2009
			if((CPRorFPR == 1 && iNoCPR == 0)||CPRorFPR ==2){
				OO.insertNumeric(xSpreadsheet2, dCPR, r, c+2);

				//Added, if there are CPR values, display/not display GCP on 3 column, by Chun Yeong 2 Jun 2011 
				if(isGroupCPLine){
					OO.insertNumeric(xSpreadsheet2, dGCP, r, c+3);
				}
			} else {
				//Added, if there are no CPR values, display/not display GCP on 2 column, by Chun Yeong 2 Jun 2011
				if(isGroupCPLine){
					OO.insertNumeric(xSpreadsheet2, dGCP, r, c+2);
				}
			}

			r++;
		}


		//		String xAxis = "Competencies";
		//		String yAxis = "Rating";
		//		if (ST.LangVer == 2) {		
		//			xAxis = "Kompetensi";
		//			yAxis = "Penilaian";
		//		}

		/* rianto
		//draw chart
		OO.setFontSize(8);
		//XTableChart xtablechart = OO.getChart(xSpreadsheet, xSpreadsheet2, 0, c+2, row, r-1, "Executive", 13000, 9000, row, 1);
		XTableChart xtablechart = OO.getChart(xSpreadsheet, xSpreadsheet2, 0, c+2, row, r-1, "Executive", 15500, 11500, row, 1);
		xtablechart = OO.setChartTitle(xtablechart, title);	
		OO.setChartProperties(xtablechart, false, true, true, true, true);
		xtablechart = OO.setAxes(xtablechart, xAxis, yAxis, 10, 1, 4500, 0);

		//need to change to LineDiagram and set the scale of xAxis, and also the width of the line
		OO.changeChartType("com.sun.star.chart.LineDiagram", xtablechart);
		 */
		//set the Y Axis to the maximum scale of the survey rating Qiao Li 23 Dec 2009
		double maxScale = this.MaxScale();
		XTableChart chart = OO.changeAxesMax(xSpreadsheet, 0, -1, maxScale);

		//only draw chart for CPR values when there is CPR.(iNoCPR == 0)
		//Qiao Li 21 Dec 2009
		if ((CPRorFPR == 1 && iNoCPR == 0)||CPRorFPR ==2){
			//dynamically change the description about "CP versus CPR" graph Qiao Li 23 Dec 2009
			//Added, if Group CP is checked/not check, display respective columns, by Chun Yeong 2 Jun 2011
			if(!isGroupCPLine) {
				OO.findAndReplace(xSpreadsheet,"<Graph Description>",trans.tslt(templateLanguage, "Current Proficiency versus the Required Proficiency"));
				OO.setSourceData(xSpreadsheet, xSpreadsheet2, 0, 0, c+2, row, r-1);
			} else {
				OO.findAndReplace(xSpreadsheet,"<Graph Description>", trans.tslt(templateLanguage, "Current Proficiency versus the Required Proficiency")
						+ ". "
						+ trans.tslt(templateLanguage, "The mean CP score for the entire group of targets for each competency is also presented in the graph (Group Current Proficiency)"));
				OO.setSourceData(xSpreadsheet, xSpreadsheet2, 0, 0, c+3, row, r-1);
			}

			insertPageBreak(xSpreadsheet, startColumn, endColumn, row+1);
		}
		else{//CPR are all 0s
			//dynamically change the description about "CP versus CPR" graph Qiao Li 23 Dec 2009
			//Added, if Group CP is checked/not check, display respective columns, by Chun Yeong 2 Jun 2011
			if(!isGroupCPLine) {
				OO.findAndReplace(xSpreadsheet,"<Graph Description>",trans.tslt(templateLanguage, "Current Proficiency"));
				OO.setSourceData(xSpreadsheet, xSpreadsheet2, 0, 0, c+1, row, r-1);
			} else {
				OO.findAndReplace(xSpreadsheet,"<Graph Description>",trans.tslt(templateLanguage, "Current Proficiency")
						+ ". "
						+ trans.tslt(templateLanguage, "The mean CP score for the entire group of targets for each competency is also presented in the graph (Group Current Proficiency)"));
				OO.setSourceData(xSpreadsheet, xSpreadsheet2, 0, 0, c+2, row, r-1);
			}

			//Change the title of the xtablechart (Qiao Li 22 Dec 2009)
			OO.changeChartTitle(chart, trans.tslt(templateLanguage, "Current Proficiency"));

		}

		System.out.println("4. CP Versus CPR Completed");
	}


	/**
	 * Write CP versus CPR results to excel.
	 */
	public void InsertCPvsCPRToyota() throws SQLException, IOException, Exception {

		//System.out.println("4. CP Versus CPR Starts");	

		//int RTID 		= 0;
		//String RTName 	= "";
		String RTCode 	= "";

		Vector RT 	= RatingTask();
		Vector CP 	= null;
		Vector CPR 	= null;
		Vector PastCP = null;
		int type 		= 1;		// 1=cpr, 2=fpr

		for(int i=0; i<RT.size(); i++) {
			votblSurveyRating vo = (votblSurveyRating)RT.elementAt(i);

			RTCode 	=  vo.getRatingCode();

			if(RTCode.equals("CP") || RTCode.equals("CPR") || RTCode.equals("FPR")) {
				if(RTCode.equals("CP")) {
					CP = CPCPRSortedID(RTCode);
					PastCP = PastCP(RTCode);
				}
				else {
					CPR = CPCPRSortedID(RTCode);
					if(RTCode.equals("CPR"))
						type = 1;
					else
						type = 2;
				}
			}
		}

		int [] address = OO.findString(xSpreadsheet, "<Chart Title>");

		column = address[0];
		row = address[1];
		row++;

		//Chart Source Data
		Vector vComp = Competency(0);
		//		int total = totalCompetency();		// 1 for all		
		String title = "Current Proficiency Vs Required Proficiency";

		int r = row+2;
		int c = 0;

		OO.insertString(xSpreadsheet2, "CP", r, c+1);
		if(type == 1)
			OO.insertString(xSpreadsheet2, "CPR", r, c+2);
		else {
			OO.insertString(xSpreadsheet2, "FPR", r, c+2);
			title = "Current Proficiency Vs Future Required Proficiency";
		}
		OO.insertString(xSpreadsheet2, "Prev. CP", r, c+3);
		r++;

		OO.findAndReplace(xSpreadsheet, "<Chart Title>", title);

		/**
		 * Added by Jenty 6 Oct 06
		 * 
		 * Previously it didn't clear the vector, hence it will return the same gap if we generate multiple reports
		 */		
		vGapUnsorted.clear();

		vGapSorted.clear();
		vCompID.clear();

		double dCP = 0;
		double dCPR = 0;
		double dPastCP = 0;

		for(int i=0; i<vComp.size(); i++) {
			voCompetency voComp = (voCompetency)vComp.elementAt(i);

			int compID 		= voComp.getCompetencyID();
			String statement = voComp.getCompetencyName();

			String compName = UnicodeHelper.getUnicodeStringAmp(statement);

			dCP 	= 0;
			dCPR 	= 0;
			dPastCP = 0;

			if(CP.size() != 0 && i<CP.size()) {
				String arr [] = (String[])CP.elementAt(i);
				dCP = Double.parseDouble(arr[2]);
			}

			if(CPR.size() != 0 && i<CPR.size()) {
				String arr [] = (String[])CPR.elementAt(i);
				dCPR = Double.parseDouble(arr[2]);
			}

			if(PastCP.size() != 0 && i<PastCP.size()) {
				String arr [] = (String[])PastCP.elementAt(i);
				dPastCP = Double.parseDouble(arr[2]);
			}
			double gap = Math.round((dCP - dCPR) * 100.0) / 100.0;

			vCompID.add(new Integer(compID));
			vGapSorted.add(new String[]{compName, Double.toString(gap)});
			vGapUnsorted.add(new String[]{compName, Double.toString(gap)});

			//OO.insertString(xSpreadsheet2, compName, r, c);
			OO.insertNumeric(xSpreadsheet2, dCP, r, c+1);
			OO.insertNumeric(xSpreadsheet2, dCPR, r, c+2);
			OO.insertNumeric(xSpreadsheet2, dPastCP, r, c+3);

			r++;
		}

		//System.out.println("4. CP Versus CPR Completed");	
	}


	/**
	 * Write target gap results to excel worksheet.
	 */

	//****Added by Tracy 26 aug 08************************
	//Add dynamic title to Individual report

	public void InsertGapTitleCluster(int surveyID) throws SQLException, IOException, Exception
	{
		//System.out.println("5.1 Gap Title Insertion Starts");

		int [] address = OO.findString(xSpreadsheet, "<Gap Title Cluster>");

		OO.findAndReplace(xSpreadsheet, "<Gap Title Cluster>", "");		

		column = address[0];
		row = address[1];
		int i=0;
		Vector RTaskID= new Vector();
		Vector RTaskName=new Vector();

		//add to get the upper and lower cp limit for display
		//Mark Oei 16 April 2010
		double MinMaxGap [] = getMinMaxGap();
		double low = MinMaxGap[0];
		double high = MinMaxGap[1];

		//Get Rating from database according to s urvey ID
		String query = "SELECT a.RatingTaskID as RTaskID, b.RatingTask as RTaskName FROM tblSurveyRating a ";
		query += "INNER JOIN tblRatingTask b ON a.RatingTaskID=b.RatingTaskID  WHERE a.SurveyID = "+ surveyID;

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try {
			con = ConnectionBean.getConnection();
			st = con.createStatement();
			rs = st.executeQuery(query);

			while (rs.next()) {
				RTaskID.add(i,new Integer(rs.getInt("RTaskID")));
				RTaskName.add(i,new String(rs.getString("RTaskName")));
				i++;
			}

			//Check CPR or FPR
			String pType="";
			String CPR="";
			String CP="";
			String FPR="";

			for (int n=0; RTaskID.size()-1>=n; n++ ) {
				if (((Integer)RTaskID.elementAt(n)).intValue()==1) {
					CP=RTaskName.elementAt(n).toString();
				}else if (((Integer)RTaskID.elementAt(n)).intValue()==2){
					CPR=RTaskName.elementAt(n).toString();
					pType="C";
				}else if (((Integer)RTaskID.elementAt(n)).intValue()==3){
					FPR=RTaskName.elementAt(n).toString();
					pType="F";
				}
			}
			//changed by Hemilda 15/09/2009 change word add (All) and make it fit the width of column
			String title = "";
			String info = "";
			info = "The table below shows the competency scores of the Target in accordance to the clusters. The competencies within each cluster is sorted by descending order of positive gaps";
			if (pType.equals("C")){
				//added to display the information
				//Mark Oei 16 April 2010
				title= "Gap = " + CP + " (All) minus " + CPR + " (All)"; // : Strengths and Development Areas Report";
			}
			else if (pType.equals("F")){   												
				title= "Gap = " + FPR + " (All) minus " + CP + " (All)"; // : Strengths and Development Areas Report";
			}
			else {   												
				title = "CP = " + CP + " (All)";
			}
			System.out.println("title = " + title);

			String gapTitle = "Strengths and Development Areas Report";
			//Insert title to excel file
			OO.insertString(xSpreadsheet, info, row-3, 0);//added to display info programmatically, Mark Oei 16 April 2010
			OO.insertString(xSpreadsheet, gapTitle, row-1,0);
			OO.setFontBold(xSpreadsheet, startColumn, endColumn, row-1, row-1);
			OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, title), row, 0);	
			OO.mergeCells(xSpreadsheet, startColumn, endColumn, row, row);				
			OO.setRowHeight(xSpreadsheet, row-3, 1, ROWHEIGHT*OO.countTotalRow(info, 90)); //added to allow auto-increment of row height, Mark Oei 16 April 2010
			OO.setRowHeight(xSpreadsheet, row, 1, ROWHEIGHT*OO.countTotalRow(title, 90));

		} catch (Exception E) {
			System.err.println("IndividualReport.java - InsertGapTitleCluster - " + E);
		} finally {
			ConnectionBean.closeRset(rs); // Close ResultSet
			ConnectionBean.closeStmt(st); // Close statement
			ConnectionBean.close(con); // Close connection

		}
	}

	public void InsertGapTitle(int surveyID) throws SQLException, IOException, Exception
	{
		//System.out.println("5.1 Gap Title Insertion Starts");

		int [] address = OO.findString(xSpreadsheet, "<Gap Title>");

		OO.findAndReplace(xSpreadsheet, "<Gap Title>", "");		

		column = address[0];
		row = address[1];
		int i=0;
		Vector RTaskID= new Vector();
		Vector RTaskName=new Vector();

		//add to get the upper and lower cp limit for display
		//Mark Oei 16 April 2010
		double MinMaxGap [] = getMinMaxGap();
		double low = MinMaxGap[0];
		double high = MinMaxGap[1];
        /*
         * change the value of low to 4.0
         * 
         */
		
		low = 4.0;
		//Get Rating from database according to s urvey ID
		String query = "SELECT a.RatingTaskID as RTaskID, b.RatingTask as RTaskName FROM tblSurveyRating a ";
		query += "INNER JOIN tblRatingTask b ON a.RatingTaskID=b.RatingTaskID  WHERE a.SurveyID = "+ surveyID;

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try {
			con = ConnectionBean.getConnection();
			st = con.createStatement();
			rs = st.executeQuery(query);

			while (rs.next()) {
				RTaskID.add(i,new Integer(rs.getInt("RTaskID")));
				RTaskName.add(i,new String(rs.getString("RTaskName")));
				i++;
			}

			//Check CPR or FPR
			String pType="";
			String CPR="";
			String CP="";
			String FPR="";

			for (int n=0; RTaskID.size()-1>=n; n++ ) {
				if (((Integer)RTaskID.elementAt(n)).intValue()==1) {
					CP=RTaskName.elementAt(n).toString();
				}else if (((Integer)RTaskID.elementAt(n)).intValue()==2){
					CPR=RTaskName.elementAt(n).toString();
					pType="C";
				}else if (((Integer)RTaskID.elementAt(n)).intValue()==3){
					FPR=RTaskName.elementAt(n).toString();
					pType="F";
				}
			}
			//changed by Hemilda 15/09/2009 change word add (All) and make it fit the width of column
			String title = "";
			String info = "";
			if (pType.equals("C")){
				//added to display the information
				//Mark Oei 16 April 2010
				info = trans.tslt(templateLanguage, "The table below indicates the Target's strengths and areas of development.") + " ";
				info += trans.tslt(templateLanguage, "For competencies where the gap is positive, these are the Target's strengths.") + " ";
				info += trans.tslt(templateLanguage, "For competencies where the gap is negative, these are areas where the Target requires development.");
				title= "Gap = " + CP + " (All) minus " + CPR + " (All)"; // : Strengths and Development Areas Report";
			}
			else if (pType.equals("F")){
				info = trans.tslt(templateLanguage, "The table below indicates the Target's strengths and areas of development.") + " ";
				info += trans.tslt(templateLanguage, "For competencies where the gap is positive, these are the Target's strengths.") + "";
				info += trans.tslt(templateLanguage, "For competencies where the gap is negative, these are areas where the Target requires development.");   												
				title= "Gap = " + FPR + " (All) minus " + CP + " (All)"; // : Strengths and Development Areas Report";
			}
			else {
				info = trans.tslt(templateLanguage, "The table below indicates the Target's strengths and areas of development.") + " ";
				info += trans.tslt(templateLanguage, "For competencies where the CP is higher than") + " " + high + trans.tslt(templateLanguage, ", these are the Target's strengths.") + " ";
				info += trans.tslt(templateLanguage, "For competencies where the CP is lower than") + " " + low + trans.tslt(templateLanguage, ", these are areas where the Target requires development.");   												
				title = "CP = " + CP + " (All)";
			}
			System.out.println("title = " + title);

			//Insert title to excel file
			OO.insertString(xSpreadsheet, info, row-3, 0);//added to display info programmatically, Mark Oei 16 April 2010
			OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, title), row, 0);	
			OO.mergeCells(xSpreadsheet, startColumn, endColumn, row, row);				
			OO.setRowHeight(xSpreadsheet, row-3, 1, ROWHEIGHT*OO.countTotalRow(info, 90)); //added to allow auto-increment of row height, Mark Oei 16 April 2010
			OO.setRowHeight(xSpreadsheet, row, 1, ROWHEIGHT*OO.countTotalRow(title, 90));

		} catch (Exception E) {
			System.err.println("IndividualReport.java - InsertGapTitle - " + E);
		} finally {
			ConnectionBean.closeRset(rs); // Close ResultSet
			ConnectionBean.closeStmt(st); // Close statement
			ConnectionBean.close(con); // Close connection

		}
	}
	//***End Tracy edit 26 aug 08****************************

	public void InsertGapCluster() throws SQLException, IOException, Exception{
		//System.out.println("5.2 Gap Insertion Starts");

		//Added to instantiate a local variable called xSpreadsheet2 to make it common with Group Report.java
		//Mark Oei 25 Mar 2010
		XSpreadsheet xSpreadsheet2 = OO.getSheet(xDoc, "Sheet2");

		int [] address = OO.findString(xSpreadsheet, "<Gap Cluster>");

		column = address[0];
		row = address[1];
		int c = 0;
		//Added to define columns where CP, CPR, Gap are inserted in spreadsheet
		//Mark Oei 25 Mar 2010
		int gapCol = 11;
		int cprCol = 10;
		int cpCol = 9;

		//To include gcp column, Chun Yeong 29 Jun 2011
		int gcpCol = 11;
		if(isGroupCPLine){
			gapCol = 10;
			cprCol = 9;
			cpCol = 8;
		}

		int[] cpAddress;
		double cpValue = 0.0;
		double cprValue = 0.0; 

		vGapSorted = G.sorting(vGapSorted, 1);

		OO.findAndReplace(xSpreadsheet, "<Gap Cluster>", "");		

		double MinMaxGap [] = getMinMaxGap();

		double low = MinMaxGap[0];
		double high = MinMaxGap[1];
		//int type = 2;	// 1 is >=, 2 is >x>, 3 is <


		//Added for Group Current Proficiency (GCP) by Chun Yeong 29 Jun 2011
		double gcpValue = 0.0;
		Vector vGCP = new Vector();

		if (iNoCPR == 0)	// If no CPR is chosen in this survey
		{
			String title = "COMPETENCY SCORES BY CLUSTER";


			OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, title), row, c);		
			OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);

			row++;
			OO.insertRows(xSpreadsheet, startColumn, endColumn, row, row+2, 2, 1);

			int startBorder = row;
			int endBorder;
			Vector v = ClusterByName();
			for(int i=0; i<v.size(); i++){
				voCluster vCluster = (voCluster) v.elementAt(i);
				String clusterName = vCluster.getClusterName();
				int clusterID = vCluster.getClusterID();
				//Allow dynamic translation, Chun Yeong 1 Aug 2011
				OO.insertString(xSpreadsheet, clusterName , row, c);
				OO.insertString(xSpreadsheet, "CP", row, cpCol);
				OO.insertString(xSpreadsheet, "CPR", row, cprCol);
				OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Gap"), row, gapCol);
				if(isGroupCPLine)
					OO.insertString(xSpreadsheet, "GCP", row, gcpCol);		//Added a new column for GCP by Chun Yeong 29 Jun 2011

				OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
				OO.setFontSize(xSpreadsheet, cpCol, gcpCol, row, row, 12);
				OO.setCellAllignment(xSpreadsheet, cpCol, gcpCol, row, row, 1, 2);
				OO.setBGColor(xSpreadsheet, startColumn, endColumn-1, row, row, BGCOLORCLUSTER);					
				row++;

				Vector<String> compList = new Vector<String>();
				Vector vComp = ClusterCompetencyByName(clusterID);
				for(int k=0; k<vComp.size(); k++){
					voCompetency vCompetency = (voCompetency) vComp.elementAt(k);
					compList.add(vCompetency.getCompetencyName());
				}

				for(int j=0; j<vGapSorted.size(); j++) {
					double gap = Double.valueOf(((String [])vGapSorted.elementAt(j))[1]).doubleValue();
					String compName = ((String [])vGapSorted.elementAt(j))[0];

					if(compList.contains(compName)) {
						OO.insertRows(xSpreadsheet, startColumn, endColumn, row, row+1, 1, 1);			

						//Added translation for the competency name, Chun Yeong 1 Aug 2011
						OO.insertString(xSpreadsheet, getTranslatedCompetency(compName).elementAt(0).toString(), row, c);
						OO.insertNumeric(xSpreadsheet, gap, row, gapCol);

						//Insert CP and CPR values next to Gap, Mark Oei 25 Mar 2010
						//Added translation for the competency name, Chun Yeong 1 Aug 2011
						cpAddress = OO.findString(xSpreadsheet2, getTranslatedCompetency(compName).elementAt(0).toString());
						cpValue = OO.getCellValue(xSpreadsheet2, cpAddress[1], cpAddress[0]+1);
						cprValue = OO.getCellValue(xSpreadsheet2, cpAddress[1], cpAddress[0]+2);
						OO.insertNumeric(xSpreadsheet, cpValue, row, cpCol);
						OO.insertNumeric(xSpreadsheet, cprValue, row, cprCol);

						//Insert GCP values next to Gap, Chun Yeong 29 Jun 2011 
						if(isGroupCPLine) {
							gcpValue = getGroupCPForOneCompetency(compName.trim());
							OO.insertNumeric(xSpreadsheet, gcpValue, row, gcpCol);
						}

						OO.setFontSize(xSpreadsheet, cpCol, gcpCol, row, row, 12);
						row++;	
					}
				}
				row++;
				endBorder = row;
				OO.setTableBorder(xSpreadsheet, startColumn, endColumn-1, startBorder, endBorder, false, false, true, true, true, true);
				//Add border lines from cpCol to gcpCol
				//Mark Oei 25 Mar 2010
				OO.setTableBorder(xSpreadsheet, cpCol, endColumn-1, startBorder, endBorder, true, false, true, true, false, false);

				startBorder = endBorder;		
			}
			row++;
			row++;
			insertPageBreak(xSpreadsheet, startColumn, endColumn, row);

		} else { //if CPR is chosen

			vCPValues = G.sorting(vCPValues, 1); //added to sort CPValues, Mark Oei 16 April 2010
			String title = "CLUSTER";

			//Allow dynamic translation, Chun Yeong 1 Aug 2011
			OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, title), row, c);		
			OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);

			row++;
			OO.insertRows(xSpreadsheet, startColumn, endColumn, row, row+2, 2, 1);

			int startBorder = row;
			int endBorder;
			cpCol = 11;
			//shift the cp column to the left 1 to allow space for GCP column.
			if(isGroupCPLine){
				cpCol = 10;
			}	    		

			Vector v = ClusterByName();
			for(int i=0; i<v.size(); i++){
				voCluster vCluster = (voCluster) v.elementAt(i);
				String clusterName = vCluster.getClusterName();
				int clusterID = vCluster.getClusterID();

				//Allow dynamic translation, Chun Yeong 1 Aug 2011
				OO.insertString(xSpreadsheet, clusterName, row, c);
				OO.insertString(xSpreadsheet, "CP", row, cpCol);
				if(isGroupCPLine)
					OO.insertString(xSpreadsheet, "GCP", row, gcpCol);		//Added a new column for GCP by Chun Yeong 29 Jun 2011

				OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
				OO.setFontSize(xSpreadsheet, cpCol-1, gcpCol, row, row, 12);
				OO.setCellAllignment(xSpreadsheet, cpCol-1, gcpCol, row, row, 1, 2);
				OO.setBGColor(xSpreadsheet, startColumn, endColumn-1, row, row, BGCOLORCLUSTER);					
				row++;

				Vector<String> compList = new Vector<String>();
				Vector vComp = ClusterCompetencyByName(clusterID);
				for(int k=0; k<vComp.size(); k++){
					voCompetency vCompetency = (voCompetency) vComp.elementAt(k);
					compList.add(vCompetency.getCompetencyName());
				}

				for(int j=0; j<vCPValues.size(); j++) {
					double cpValues = Double.valueOf(((String [])vCPValues.elementAt(j))[1]).doubleValue();
					String compName = ((String [])vCPValues.elementAt(j))[0];

					if(compList.contains(compName)) {							
						OO.insertRows(xSpreadsheet, startColumn, endColumn, row, row+1, 1, 1);			

						//Added translation for competency name, Chun Yeong 1 Aug 2011
						OO.insertString(xSpreadsheet, getTranslatedCompetency(compName).elementAt(0).toString(), row, c);
						OO.insertNumeric(xSpreadsheet, cpValues, row, cpCol);

						//Insert GCP values next to Gap, Chun Yeong 29 Jun 2011
						if(isGroupCPLine) {
							gcpValue = getGroupCPForOneCompetency(compName.trim());	
							OO.insertNumeric(xSpreadsheet, gcpValue, row, gcpCol);
						}

						OO.setFontSize(xSpreadsheet, cpCol-1, gcpCol, row, row, 12);
						row++;	
					}
				}

				row++;
				endBorder = row;
				OO.setTableBorder(xSpreadsheet, startColumn, endColumn-1, startBorder, endBorder, false, false, true, true, true, true);
				//Add border lines from cpCol to gcpCol
				//Mark Oei 25 Mar 2010
				OO.setTableBorder(xSpreadsheet, cpCol, endColumn-1, startBorder, endBorder, true, false, true, true, false, false);

				startBorder = endBorder;		
			}
			row++;
			row++;
			insertPageBreak(xSpreadsheet, startColumn, endColumn, row);			
		}//end else

		//System.out.println("5. Gap Completed");
	}

	public void InsertGap() throws SQLException, IOException, Exception 
	{
		//System.out.println("5.2 Gap Insertion Starts");

		//Added to instantiate a local variable called xSpreadsheet2 to make it common with Group Report.java
		//Mark Oei 25 Mar 2010
		XSpreadsheet xSpreadsheet2 = OO.getSheet(xDoc, "Sheet2");

		int [] address = OO.findString(xSpreadsheet, "<Gap>");

		column = address[0];
		row = address[1];
		int c = 0;
		//Added to define columns where CP, CPR, Gap are inserted in spreadsheet
		//Mark Oei 25 Mar 2010
		int gapCol = 11;
		int cprCol = 10;
		int cpCol = 9;

		//To include gcp column, Chun Yeong 29 Jun 2011
		int gcpCol = 11;
		if(isGroupCPLine){
			gapCol = 10;
			cprCol = 9;
			cpCol = 8;
		}

		int[] cpAddress;
		double cpValue = 0.0;
		double cprValue = 0.0; 

		vGapSorted = G.sorting(vGapSorted, 1);

		OO.findAndReplace(xSpreadsheet, "<Gap>", "");		

		double MinMaxGap [] = getMinMaxGap();

		double low = MinMaxGap[0];
		double high = MinMaxGap[1];
		low = 4.0;
		//int type = 2;	// 1 is >=, 2 is >x>, 3 is <


		//Added for Group Current Proficiency (GCP) by Chun Yeong 29 Jun 2011
		double gcpValue = 0.0;
		Vector vGCP = new Vector();

		if (iNoCPR == 0)	// If no CPR is chosen in this survey
		{
			//ResultSet Gap = null;
			String title = "COMPETENCY";

			//Commented away to allow translation below, Chun Yeong 1 Aug 2011
			//if (ST.LangVer == 2)
			//	title = "KOMPETENSI";

			OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, title), row, c);		
			OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);

			row++;
			OO.insertRows(xSpreadsheet, startColumn, endColumn, row, row+2, 2, 1);

			int startBorder = row;

			//Insert 2 new labels CP and CPR before Gap and set font size to 12
			//Mark Oei 25 Mar 2010
			//Changed the default language to English by Chun Yeong 9 Jun 2011
			//Commented away to allow translation below, Chun Yeong 1 Aug 2011
			/*if (ST.LangVer == 2){ //Indonesian
				OO.insertString(xSpreadsheet, "KEKUATAN ( Gap >= " + high + " )", row, c);
				OO.insertString(xSpreadsheet, "CP", row, cpCol);
				OO.insertString(xSpreadsheet, "CPR", row, cprCol);
				OO.insertString(xSpreadsheet, "Selisih", row, gapCol);
				if(isGroupCPLine)
					OO.insertString(xSpreadsheet, "GCP", row, gcpCol);		//Added a new column for GCP by Chun Yeong 29 Jun 2011
			}
			else { //English
				OO.insertString(xSpreadsheet, "STRENGTH ( Gap >= " + high +" )", row, c);
				OO.insertString(xSpreadsheet, "CP", row, cpCol);
				OO.insertString(xSpreadsheet, "CPR", row, cprCol);
				OO.insertString(xSpreadsheet, "Gap", row, gapCol);
				if(isGroupCPLine)
					OO.insertString(xSpreadsheet, "GCP", row, gcpCol);		//Added a new column for GCP by Chun Yeong 29 Jun 2011
			}*/

			//Allow dynamic translation, Chun Yeong 1 Aug 2011
			OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "STRENGTH") + " ( "+ trans.tslt(templateLanguage, "Gap") + " >= " + high +" )", row, c);
			OO.insertString(xSpreadsheet, "CP", row, cpCol);
			OO.insertString(xSpreadsheet, "CPR", row, cprCol);
			OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Gap"), row, gapCol);
			if(isGroupCPLine)
				OO.insertString(xSpreadsheet, "GCP", row, gcpCol);		//Added a new column for GCP by Chun Yeong 29 Jun 2011

			OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
			OO.setFontSize(xSpreadsheet, cpCol, gcpCol, row, row, 12);
			OO.setCellAllignment(xSpreadsheet, cpCol, gcpCol, row, row, 1, 2);
			OO.setBGColor(xSpreadsheet, startColumn, endColumn-1, row, row, BGCOLOR);					
			row++;


			for(int i=0; i<vGapSorted.size(); i++) {
				double gap = Double.valueOf(((String [])vGapSorted.elementAt(i))[1]).doubleValue();

				if(gap >= high) {
					String compName = ((String [])vGapSorted.elementAt(i))[0];

					OO.insertRows(xSpreadsheet, startColumn, endColumn, row, row+1, 1, 1);			

					//Added translation for the competency name, Chun Yeong 1 Aug 2011
					OO.insertString(xSpreadsheet, getTranslatedCompetency(compName).elementAt(0).toString(), row, c);
					OO.insertNumeric(xSpreadsheet, gap, row, gapCol);

					//Insert CP and CPR values next to Gap, Mark Oei 25 Mar 2010
					//Added translation for the competency name, Chun Yeong 1 Aug 2011
					cpAddress = OO.findString(xSpreadsheet2, getTranslatedCompetency(compName).elementAt(0).toString());
					cpValue = OO.getCellValue(xSpreadsheet2, cpAddress[1], cpAddress[0]+1);
					cprValue = OO.getCellValue(xSpreadsheet2, cpAddress[1], cpAddress[0]+2);
					OO.insertNumeric(xSpreadsheet, cpValue, row, cpCol);
					OO.insertNumeric(xSpreadsheet, cprValue, row, cprCol);

					//Insert GCP values next to Gap, Chun Yeong 29 Jun 2011 
					if(isGroupCPLine) {
						gcpValue = getGroupCPForOneCompetency(compName.trim());
						OO.insertNumeric(xSpreadsheet, gcpValue, row, gcpCol);
					}

					OO.setFontSize(xSpreadsheet, cpCol, gcpCol, row, row, 12);
					row++;	
				}
			}

			row++;
			int endBorder = row;
			OO.setTableBorder(xSpreadsheet, startColumn, endColumn-1, startBorder, endBorder, false, false, true, true, true, true);
			//Add border lines from cpCol to gcpCol
			//Mark Oei 25 Mar 2010
			OO.setTableBorder(xSpreadsheet, cpCol, endColumn-1, startBorder, endBorder, true, false, true, true, false, false);

			startBorder = endBorder + 1;		
			row++;	
			OO.insertRows(xSpreadsheet, startColumn, endColumn, row, row+2, 2, 1);

			//Insert 2 new labels CP and CPR before Gap and set font size to 12
			//Mark Oei 25 Mar 2010
			//Changed the default language to English by Chun Yeong 9 Jun 2011
			//Commented away to allow translation below, Chun Yeong 1 Aug 2011
			/*if (ST.LangVer == 2){ //Indonesian			
				OO.insertString(xSpreadsheet, "MEMENUHI PENGHARAPAN ( " + low + " < Gap < " + high + " )", row, c);
				OO.insertString(xSpreadsheet, "CP", row, cpCol);
				OO.insertString(xSpreadsheet, "CPR", row, cprCol);
				OO.insertString(xSpreadsheet, "Selisih" , row, gapCol);
				if(isGroupCPLine)
					OO.insertString(xSpreadsheet, "GCP", row, gcpCol);		//Added a new column for GCP by Chun Yeong 29 Jun 2011
			} 
			else { //English
				OO.insertString(xSpreadsheet, "MEET EXPECTATIONS ( " + low + " < Gap < " + high + " )" , row, c);
				OO.insertString(xSpreadsheet, "CP", row, cpCol);
				OO.insertString(xSpreadsheet, "CPR", row, cprCol);
				OO.insertString(xSpreadsheet, "Gap", row, gapCol);
				if(isGroupCPLine)
					OO.insertString(xSpreadsheet, "GCP", row, gcpCol);		//Added a new column for GCP by Chun Yeong 29 Jun 2011
			}*/

			//Allow dynamic translation, Chun Yeong 1 Aug 2011
			OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "MEET EXPECTATIONS") + " ( " + low + " <= " + trans.tslt(templateLanguage, "Gap") + " < " + high + " )" , row, c);
			OO.insertString(xSpreadsheet, "CP", row, cpCol);
			OO.insertString(xSpreadsheet, "CPR", row, cprCol);
			OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Gap"), row, gapCol);
			if(isGroupCPLine)
				OO.insertString(xSpreadsheet, "GCP", row, gcpCol);		//Added a new column for GCP by Chun Yeong 29 Jun 2011

			OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
			OO.setFontSize(xSpreadsheet, cpCol, gcpCol, row, row, 12);
			OO.setCellAllignment(xSpreadsheet, cpCol, gcpCol, row, row, 1, 2);
			OO.setBGColor(xSpreadsheet, startColumn, endColumn-1, row, row, BGCOLOR);
			row++;

			for(int i=0; i<vGapSorted.size(); i++) {
				double gap = Double.valueOf(((String [])vGapSorted.elementAt(i))[1]).doubleValue();

				if(gap < high && gap > low) {
					String compName = ((String [])vGapSorted.elementAt(i))[0];				

					OO.insertRows(xSpreadsheet, startColumn, endColumn, row, row+1, 1, 1);

					//Added translation for the competency name, Chun Yeong 1 Aug 2011
					OO.insertString(xSpreadsheet, getTranslatedCompetency(compName).elementAt(0).toString(), row, c);
					OO.insertNumeric(xSpreadsheet, gap, row, gapCol);
					//Insert CP and CPR values next to Gap, Mark Oei 25 Mar 2010
					//Added translation for the competency name, Chun Yeong 1 Aug 2011
					cpAddress = OO.findString(xSpreadsheet2, getTranslatedCompetency(compName).elementAt(0).toString());
					cpValue = OO.getCellValue(xSpreadsheet2, cpAddress[1], cpAddress[0]+1);
					cprValue = OO.getCellValue(xSpreadsheet2, cpAddress[1], cpAddress[0]+2);
					OO.insertNumeric(xSpreadsheet, cpValue, row, cpCol);
					OO.insertNumeric(xSpreadsheet, cprValue, row, cprCol);

					//Insert GCP values next to Gap, Chun Yeong 29 Jun 2011
					if(isGroupCPLine) {
						gcpValue = getGroupCPForOneCompetency(compName.trim());
						OO.insertNumeric(xSpreadsheet, gcpValue, row, gcpCol);
					}

					OO.setFontSize(xSpreadsheet, cpCol, gcpCol, row, row, 12);
					row++;
				}
			}

			row++;
			endBorder = row;
			OO.setTableBorder(xSpreadsheet, startColumn, endColumn-1, startBorder, endBorder, false, false, true, true, true, true);
			//Add border lines from cpCol to gapCol
			//Mark Oei 25 Mar 2010
			OO.setTableBorder(xSpreadsheet, cpCol, endColumn-1, startBorder, endBorder, true, false, true, true, false, false);

			startBorder = endBorder + 1;		
			row++;

			OO.insertRows(xSpreadsheet, startColumn, endColumn, row, row+2, 2, 1);					

			//Insert 2 new labels CP and CPR before Gap and set font size to 12
			//Mark Oei 25 Mar 2010
			//Changed the default language to English by Chun Yeong 9 Jun 2011
			//Commented away to allow translation below, Chun Yeong 1 Aug 2011
			/*if (ST.LangVer == 2){ //Indonesian
				OO.insertString(xSpreadsheet, "AREA PERKEMBANGAN ( Gap <= " + low + " )", row, c);
				OO.insertString(xSpreadsheet, "CP", row, cpCol);
				OO.insertString(xSpreadsheet, "CPR", row, cprCol);
				OO.insertString(xSpreadsheet, "Selisih", row, gapCol);
				if(isGroupCPLine)
					OO.insertString(xSpreadsheet, "GCP", row, gcpCol);		//Added a new column for GCP by Chun Yeong 29 Jun 2011
			} 
			else { //English
				OO.insertString(xSpreadsheet, "DEVELOPMENTAL AREA ( Gap <= " + low + " )", row, c);
				OO.insertString(xSpreadsheet, "CP", row, cpCol);
				OO.insertString(xSpreadsheet, "CPR", row, cprCol);
				OO.insertString(xSpreadsheet, "Gap", row, gapCol);
				if(isGroupCPLine)
					OO.insertString(xSpreadsheet, "GCP", row, gcpCol);		//Added a new column for GCP by Chun Yeong 29 Jun 2011
			}*/

			//Allow dynamic translation, Chun Yeong 1 Aug 2011
			OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "DEVELOPMENTAL AREA") + " ( " + trans.tslt(templateLanguage, "Gap") + " < " + low + " )", row, c);
			OO.insertString(xSpreadsheet, "CP", row, cpCol);
			OO.insertString(xSpreadsheet, "CPR", row, cprCol);
			OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Gap"), row, gapCol);
			if(isGroupCPLine)
				OO.insertString(xSpreadsheet, "GCP", row, gcpCol);		//Added a new column for GCP by Chun Yeong 29 Jun 2011

			OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
			OO.setFontSize(xSpreadsheet, cpCol, gcpCol, row, row, 12);
			OO.setCellAllignment(xSpreadsheet, cpCol, gcpCol, row, row, 1, 2);
			OO.setBGColor(xSpreadsheet, startColumn, endColumn-1, row, row, BGCOLOR);

			row++;

			for(int i=0; i<vGapSorted.size(); i++) {
				double gap = Double.valueOf(((String [])vGapSorted.elementAt(i))[1]).doubleValue();

				if(gap <= low) {
					String compName = ((String [])vGapSorted.elementAt(i))[0];

					OO.insertRows(xSpreadsheet, startColumn, endColumn, row, row+1, 1, 1);			

					//Added translation for the competency name, Chun Yeong 1 Aug 2011
					OO.insertString(xSpreadsheet, getTranslatedCompetency(compName).elementAt(0).toString(), row, c);
					OO.insertNumeric(xSpreadsheet, gap, row, gapCol);
					//Insert CP and CPR values next to Gap, Mark Oei 25 Mar 2010
					//Added translation for the competency name, Chun Yeong 1 Aug 2011
					cpAddress = OO.findString(xSpreadsheet2, getTranslatedCompetency(compName).elementAt(0).toString());
					cpValue = OO.getCellValue(xSpreadsheet2, cpAddress[1], cpAddress[0]+1);
					cprValue = OO.getCellValue(xSpreadsheet2, cpAddress[1], cpAddress[0]+2);
					OO.insertNumeric(xSpreadsheet, cpValue, row, cpCol);
					OO.insertNumeric(xSpreadsheet, cprValue, row, cprCol);

					//Insert GCP values next to Gap, Chun Yeong 29 Jun 2011
					if(isGroupCPLine) {
						gcpValue = getGroupCPForOneCompetency(compName.trim());
						OO.insertNumeric(xSpreadsheet, gcpValue, row, gcpCol);
					}

					OO.setFontSize(xSpreadsheet, cpCol, gapCol, row, row, 12);
					row++;	
				}
			}

			endBorder = row;
			OO.setTableBorder(xSpreadsheet, startColumn, endColumn-1, startBorder, endBorder, false, false, true, true, true, true);
			//Add border lines from cpCol to gapCol
			//Mark Oei 25 Mar 2010
			OO.setTableBorder(xSpreadsheet, cpCol, endColumn-1, startBorder, endBorder, true, false, true, true, false, false);
		} else {
			// **************************************************************************
			// Delete the rows with Gap Table description from the report
			//adjusted to relative numbers to completely delete the page with gap comments (Qiao Li 23 Dec 2009)

			//Commented Off deleteRows method and added codes to insert CP values into column
			//Mark Oei 16 April 2010
			//OO.deleteRows(xSpreadsheet, startColumn, endColumn, row-8, row+6, 14, 0);

			//ResultSet Gap = null;

			vCPValues = G.sorting(vCPValues, 1); //added to sort CPValues, Mark Oei 16 April 2010
			String title = "COMPETENCY";

			//Commented away to allow translation below, Chun Yeong 1 Aug 2011
			//if (ST.LangVer == 2)
			//	title = "KOMPETENSI";

			//Allow dynamic translation, Chun Yeong 1 Aug 2011
			OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, title), row, c);		
			OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);

			row++;
			OO.insertRows(xSpreadsheet, startColumn, endColumn, row, row+2, 2, 1);

			int startBorder = row;

			cpCol = 11;
			//shift the cp column to the left 1 to allow space for GCP column.
			if(isGroupCPLine){
				cpCol = 10;
			}	    		

			//Insert 2 new labels CP and CPR before Gap and set font size to 12
			//Mark Oei 25 Mar 2010
			//Changed the default language to English by Chun Yeong 9 Jun 2011
			//Commented away to allow translation below, Chun Yeong 1 Aug 2011
			/*if (ST.LangVer == 2){ //Indonesian
				OO.insertString(xSpreadsheet, "KEKUATAN ( CP >= " + high + " )", row, c);
				OO.insertString(xSpreadsheet, "CP", row, cpCol);
				if(isGroupCPLine)
					OO.insertString(xSpreadsheet, "GCP", row, gcpCol);		//Added a new column for GCP by Chun Yeong 29 Jun 2011
			}
			else { //English
				OO.insertString(xSpreadsheet, "STRENGTH ( CP >= " + high +" )", row, c);
				OO.insertString(xSpreadsheet, "CP", row, cpCol);
				if(isGroupCPLine)
					OO.insertString(xSpreadsheet, "GCP", row, gcpCol);		//Added a new column for GCP by Chun Yeong 29 Jun 2011
			}*/

			//Allow dynamic translation, Chun Yeong 1 Aug 2011
			OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "STRENGTH") + " ( CP >= " + high +" )", row, c);
			OO.insertString(xSpreadsheet, "CP", row, cpCol);
			if(isGroupCPLine)
				OO.insertString(xSpreadsheet, "GCP", row, gcpCol);		//Added a new column for GCP by Chun Yeong 29 Jun 2011

			OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
			OO.setFontSize(xSpreadsheet, cpCol-1, gcpCol, row, row, 12);
			OO.setCellAllignment(xSpreadsheet, cpCol-1, gcpCol, row, row, 1, 2);
			OO.setBGColor(xSpreadsheet, startColumn, endColumn-1, row, row, BGCOLOR);					
			row++;

			for(int i=0; i<vCPValues.size(); i++) {
				double cpValues = Double.valueOf(((String [])vCPValues.elementAt(i))[1]).doubleValue();

				if(cpValues >= high) {
					String compName = ((String [])vCPValues.elementAt(i))[0];

					OO.insertRows(xSpreadsheet, startColumn, endColumn, row, row+1, 1, 1);			

					//Added translation for competency name, Chun Yeong 1 Aug 2011
					OO.insertString(xSpreadsheet, getTranslatedCompetency(compName).elementAt(0).toString(), row, c);
					OO.insertNumeric(xSpreadsheet, cpValues, row, cpCol);

					//Insert GCP values next to Gap, Chun Yeong 29 Jun 2011
					if(isGroupCPLine) {
						gcpValue = getGroupCPForOneCompetency(compName.trim());	
						OO.insertNumeric(xSpreadsheet, gcpValue, row, gcpCol);
					}

					OO.setFontSize(xSpreadsheet, cpCol-1, gcpCol, row, row, 12);
					row++;	
				}
			}

			row++;
			int endBorder = row;
			OO.setTableBorder(xSpreadsheet, startColumn, endColumn-1, startBorder, endBorder, false, false, true, true, true, true);
			OO.setTableBorder(xSpreadsheet, cpCol, endColumn-1, startBorder, endBorder, true, false, true, true, false, false);
			
			startBorder = endBorder + 1;		
			row++;	
			OO.insertRows(xSpreadsheet, startColumn, endColumn, row, row+2, 2, 1);

			//Insert 2 new labels CP and CPR before Gap and set font size to 12
			//Mark Oei 25 Mar 2010
			//Changed the default language to English by Chun Yeong 9 Jun 2011
			//Commented away to allow translation below, Chun Yeong 1 Aug 2011
			/*if (ST.LangVer == 2){ //Indonesian			
				OO.insertString(xSpreadsheet, "MEMENUHI PENGHARAPAN ( " + low + " < CP < " + high + " )", row, c);
				OO.insertString(xSpreadsheet, "CP", row, cpCol);
				if(isGroupCPLine)
					OO.insertString(xSpreadsheet, "GCP", row, gcpCol);		//Added a new column for GCP by Chun Yeong 29 Jun 2011
			} 
			else { //English
				OO.insertString(xSpreadsheet, "MEET EXPECTATIONS ( " + low + " < CP < " + high + " )" , row, c);
				OO.insertString(xSpreadsheet, "CP", row, cpCol);
				if(isGroupCPLine)
					OO.insertString(xSpreadsheet, "GCP", row, gcpCol);		//Added a new column for GCP by Chun Yeong 29 Jun 2011
			}*/

			//Allow dynamic translation, Chun Yeong 1 Aug 2011
			OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "MEET EXPECTATIONS") + " ( " + low + " <= CP < " + high + " )" , row, c);
			OO.insertString(xSpreadsheet, "CP", row, cpCol);
			if(isGroupCPLine)
				OO.insertString(xSpreadsheet, "GCP", row, gcpCol);		//Added a new column for GCP by Chun Yeong 29 Jun 2011

			OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
			OO.setFontSize(xSpreadsheet, cpCol-1, gcpCol, row, row, 12);
			OO.setCellAllignment(xSpreadsheet, cpCol-1, gcpCol, row, row, 1, 2);
			OO.setBGColor(xSpreadsheet, startColumn, endColumn-1, row, row, BGCOLOR);
			row++;

			for(int i=0; i<vCPValues.size(); i++) {
				double cpValues = Double.valueOf(((String [])vCPValues.elementAt(i))[1]).doubleValue();

				if(cpValues < high && cpValues > low) {
					String compName = ((String [])vCPValues.elementAt(i))[0];

					OO.insertRows(xSpreadsheet, startColumn, endColumn, row, row+1, 1, 1);			

					//Added translation for competency name, Chun Yeong 1 Aug 2011
					OO.insertString(xSpreadsheet, getTranslatedCompetency(compName).elementAt(0).toString(), row, c);
					OO.insertNumeric(xSpreadsheet, cpValues, row, cpCol);

					//Insert GCP values next to Gap, Chun Yeong 29 Jun 2011
					if(isGroupCPLine) {
						gcpValue = getGroupCPForOneCompetency(compName.trim());
						OO.insertNumeric(xSpreadsheet, gcpValue, row, gcpCol);
					}

					OO.setFontSize(xSpreadsheet, cpCol-1, gcpCol, row, row, 12);
					row++;
				}
			}

			row++;
			endBorder = row;
			OO.setTableBorder(xSpreadsheet, startColumn, endColumn-1, startBorder, endBorder, false, false, true, true, true, true);
			OO.setTableBorder(xSpreadsheet, cpCol, endColumn-1, startBorder, endBorder, true, false, true, true, false, false);
			
			startBorder = endBorder + 1;		
			row++;

			OO.insertRows(xSpreadsheet, startColumn, endColumn, row, row+2, 2, 1);					

			//Changed the default language to English by Chun Yeong 9 Jun 2011
			//Commented away to allow translation below, Chun Yeong 1 Aug 2011
			/*if (ST.LangVer == 2){ //Indonesian
				OO.insertString(xSpreadsheet, "AREA PERKEMBANGAN ( CP <= " + low + " )", row, c); //Change wording Gap to CP for CP Only, Mark 20 May 2010
				OO.insertString(xSpreadsheet, "CP", row, cpCol);
				if(isGroupCPLine)
					OO.insertString(xSpreadsheet, "GCP", row, gcpCol);		//Added a new column for GCP by Chun Yeong 29 Jun 2011
			}
			else { //English
				OO.insertString(xSpreadsheet, "DEVELOPMENTAL AREA ( CP <= " + low + " )", row, c); //Change wording Gap to CP for CP Only, Mark 20 May 2010
				OO.insertString(xSpreadsheet, "CP", row, cpCol);
				if(isGroupCPLine)
					OO.insertString(xSpreadsheet, "GCP", row, gcpCol);		//Added a new column for GCP by Chun Yeong 29 Jun 2011
			}*/

			//Allow dynamic translation, Chun Yeong 1 Aug 2011
			OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "DEVELOPMENTAL AREA") + " ( CP < " + low + " )", row, c); //Change wording Gap to CP for CP Only, Mark 20 May 2010
			OO.insertString(xSpreadsheet, "CP", row, cpCol);
			if(isGroupCPLine)
				OO.insertString(xSpreadsheet, "GCP", row, gcpCol);		//Added a new column for GCP by Chun Yeong 29 Jun 2011

			OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
			OO.setFontSize(xSpreadsheet, cpCol-1, gcpCol, row, row, 12);
			OO.setCellAllignment(xSpreadsheet, cpCol-1, gcpCol, row, row, 1, 2);
			OO.setBGColor(xSpreadsheet, startColumn, endColumn-1, row, row, BGCOLOR);

			row++;

			for(int i=0; i<vCPValues.size(); i++) {
				double cpValues = Double.valueOf(((String [])vCPValues.elementAt(i))[1]).doubleValue();

				if(cpValues <= low) {
					String compName = ((String [])vCPValues.elementAt(i))[0];

					OO.insertRows(xSpreadsheet, startColumn, endColumn, row, row+1, 1, 1);			

					//Added translation for competency name, Chun Yeong 1 Aug 2011
					OO.insertString(xSpreadsheet, getTranslatedCompetency(compName).elementAt(0).toString(), row, c);
					OO.insertNumeric(xSpreadsheet, cpValues, row, cpCol);

					//Insert GCP values next to Gap, Chun Yeong 29 Jun 2011
					if(isGroupCPLine) {
						gcpValue = getGroupCPForOneCompetency(compName.trim());
						OO.insertNumeric(xSpreadsheet, gcpValue, row, gcpCol);
					}

					OO.setFontSize(xSpreadsheet, cpCol-1, gcpCol, row, row, 12);
					row++;	
				}
			}

			endBorder = row;
			OO.setTableBorder(xSpreadsheet, startColumn, endColumn-1, startBorder, endBorder, false, false, true, true, true, true);
			OO.setTableBorder(xSpreadsheet, cpCol, endColumn-1, startBorder, endBorder, true, false, true, true, false, false);
		}

		//System.out.println("5. Gap Completed");
	}


	/**
	 * Write target gap results to excel worksheet. (For Toyota combined report)
	 */
	public void InsertGapToyota() throws SQLException, IOException, Exception 
	{
		//System.out.println("5. Gap Insertion Starts");

		int surveyLevel = Integer.parseInt(surveyInfo[0]);

		row = 8;
		column = 23;
		startColumn = 23;
		endColumn = 34;

		Vector tempGapVector = new Vector();
		double MinMaxGap [] = getMinMaxGap();

		double low = MinMaxGap[0];
		double high = MinMaxGap[1];
		//int type = 2;	// 1 is >=, 2 is >x>, 3 is <

		//ResultSet Gap = null;
		String title = "COMPETENCY";

		OO.insertString(xSpreadsheet, title, row, column);		
		OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
		OO.setTableBorder(xSpreadsheet, startColumn, endColumn, row, row, false, false, true, true, true, true);
		row++;

		int startBorder = row;

		if (surveyLevel == 0)
		{
			OO.insertString(xSpreadsheet, "STRENGTH", row, column);
			OO.insertString(xSpreadsheet, "Gap >= " + high, row, column+10);

			OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
			OO.setBGColor(xSpreadsheet, startColumn, endColumn, row, row, BGCOLOR);					
			row++;

			Vector rsGapValue = null;
			rsGapValue = getTargetGapToyota("AND tblGap.Gap >= " + high + " ORDER BY tblGap.Gap DESC");

			for(int i=0; i<rsGapValue.size(); i++)
			{
				String [] arr = (String[])rsGapValue.elementAt(i);
				OO.insertString(xSpreadsheet, UnicodeHelper.getUnicodeStringAmp(arr[1]), row, column);
				OO.insertNumeric(xSpreadsheet, Double.parseDouble(arr[2]), row, column+10);
				row++;	
			}

			row++;
			int endBorder = row;
			OO.setTableBorder(xSpreadsheet, startColumn, endColumn, startBorder, endBorder, false, false, true, true, true, true);

			startBorder = endBorder + 1;
			row++;

			OO.insertString(xSpreadsheet, "MEET EXPECTATIONS", row, column);
			OO.insertString(xSpreadsheet, low + " < Gap < " + high, row, column+10);

			OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
			OO.setBGColor(xSpreadsheet, startColumn, endColumn, row, row, BGCOLOR);						
			row++;

			rsGapValue = getTargetGapToyota("AND tblGap.Gap > " + low + " AND tblGap.Gap < " + high + " ORDER BY tblGap.Gap DESC");

			for(int i=0; i<rsGapValue.size(); i++)
			{
				String [] arr = (String[])rsGapValue.elementAt(i);
				OO.insertString(xSpreadsheet, UnicodeHelper.getUnicodeStringAmp(arr[1]), row, column);
				OO.insertNumeric(xSpreadsheet, Double.parseDouble(arr[2]), row, column+10);
				row++;	
			}

			row++;
			endBorder = row;
			OO.setTableBorder(xSpreadsheet, startColumn, endColumn, startBorder, endBorder, false, false, true, true, true, true);

			startBorder = endBorder + 1;		
			row++;

			OO.insertString(xSpreadsheet, "DEVELOPMENTAL AREA", row, column);
			OO.insertString(xSpreadsheet, "Gap <= " + low, row, column+10);

			OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
			OO.setBGColor(xSpreadsheet, startColumn, endColumn, row, row, BGCOLOR);

			row++;

			rsGapValue = getTargetGapToyota("AND tblGap.Gap <= " + low + " ORDER BY tblGap.Gap");

			for(int i=0; i<rsGapValue.size(); i++)
			{
				String [] arr = (String[])rsGapValue.elementAt(i);
				OO.insertString(xSpreadsheet, UnicodeHelper.getUnicodeStringAmp(arr[1]), row, column);
				OO.insertNumeric(xSpreadsheet, Double.parseDouble(arr[2]), row, column+10);
				row++;	
			}

			endBorder = row;
			OO.setTableBorder(xSpreadsheet, startColumn, endColumn, startBorder, endBorder, false, false, true, true, true, true);
		}	// SurveyLevel == 0
		else
		{
			double dGap = 0;
			String sCompName = "";

			OO.insertString(xSpreadsheet, "STRENGTH", row, column);
			OO.insertString(xSpreadsheet, "Gap >= " + high, row, column+10);

			OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
			OO.setBGColor(xSpreadsheet, startColumn, endColumn, row, row, BGCOLOR);					
			row++;

			tempGapVector = sortGap(vGapSorted, 0);

			for(int a=0; a<tempGapVector.size(); a++)
			{
				dGap = Double.valueOf(((String [])tempGapVector.elementAt(a))[1]).doubleValue();
				sCompName = ((String [])tempGapVector.elementAt(a))[0];

				if (dGap >= high)
				{
					OO.insertString(xSpreadsheet, sCompName, row, column);
					OO.insertNumeric(xSpreadsheet, dGap, row, column+10);
					row++;	
				}
			}

			row++;
			int endBorder = row;
			OO.setTableBorder(xSpreadsheet, startColumn, endColumn, startBorder, endBorder, false, false, true, true, true, true);

			startBorder = endBorder + 1;
			row++;

			OO.insertString(xSpreadsheet, "MEET EXPECTATIONS", row, column);
			OO.insertString(xSpreadsheet, low + " < Gap < " + high, row, column+10);

			OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
			OO.setBGColor(xSpreadsheet, startColumn, endColumn, row, row, BGCOLOR);						
			row++;


			for(int b=0; b<tempGapVector.size(); b++)
			{
				dGap = Double.valueOf(((String [])tempGapVector.elementAt(b))[1]).doubleValue();
				sCompName = ((String [])tempGapVector.elementAt(b))[0];

				if (dGap > low && dGap < high)
				{
					OO.insertString(xSpreadsheet, sCompName, row, column);
					OO.insertNumeric(xSpreadsheet, dGap, row, column+10);
					row++;	
				}
			}

			row++;
			endBorder = row;
			OO.setTableBorder(xSpreadsheet, startColumn, endColumn, startBorder, endBorder, false, false, true, true, true, true);

			startBorder = endBorder + 1;
			row++;

			OO.insertString(xSpreadsheet, "DEVELOPMENTAL AREA", row, column);
			OO.insertString(xSpreadsheet, "Gap <= " + low, row, column+10);

			OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
			OO.setBGColor(xSpreadsheet, startColumn, endColumn, row, row, BGCOLOR);				
			row++;

			for(int d=0; d<tempGapVector.size(); d++)
			{
				dGap = Double.valueOf(((String [])tempGapVector.elementAt(d))[1]).doubleValue();
				sCompName = ((String [])tempGapVector.elementAt(d))[0];

				if (dGap <= low)
				{
					OO.insertString(xSpreadsheet, sCompName, row, column);
					OO.insertNumeric(xSpreadsheet, dGap, row, column+10);
					row++;	
				}
			}

			endBorder = row;
			OO.setTableBorder(xSpreadsheet, startColumn, endColumn, startBorder, endBorder, false, false, true, true, true, true);
		}	// SurveyLevel == 1

		//System.out.println("5. Gap Completed");
	}

	/*
	 * Sort the gap.
	 *	@param int type		0 = DESC, 1 = ASC
	 */
	public Vector sortGap(Vector vGapLocal, int type) throws SQLException, Exception 
	{
		//System.out.println("Sort vGapLocal.size() = " + vGapLocal.size());
		Vector vLocal = (Vector) vGapLocal.clone();
		Vector vSorted = new Vector();
		double max  = 0; //highest score
		double temp = 0; //temp score
		int curr  = 0; //curr highest element

		while(!vLocal.isEmpty()) {
			max = Double.valueOf(((String [])vLocal.elementAt(0))[1]).doubleValue();
			curr = 0;

			// do sorting here
			for(int t=1; t<vLocal.size(); t++) {
				temp = Double.valueOf(((String [])vLocal.elementAt(t))[1]).doubleValue();

				if(type == 0) {
					if(temp > max) {    
						max = temp;
						curr = t;
					}	
				} else {
					if(temp < max) {    
						max = temp;
						curr = t;
					}	
				}				
			}

			String info [] = {((String [])vLocal.elementAt(curr))[0], ((String [])vLocal.elementAt(curr))[1]};
			vSorted.add(info);

			vLocal.removeElementAt(curr);
		}

		return vSorted;
	}


	/**
	 * Write Normative results on excel worksheet.
	 */
	public void InsertNormative() throws SQLException, IOException, Exception 
	{
		// Added by Tracy 01 Sep 08**************************************
		// Insert CP Rating into "Normative"
		int [] CPAddress= OO.findString(xSpreadsheet, "<CP>");
		String RTaskName="";


		column = CPAddress[0];
		row = CPAddress[1];

		//		Get CP Rating from database according to survey ID
		String query = "SELECT b.RatingTask as RTaskName FROM tblSurveyRating a ";
		query += "INNER JOIN tblRatingTask b ON a.RatingTaskID=b.RatingTaskID  WHERE a.SurveyID = "+ surveyID + " AND a.RatingTaskID=1";

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try {
			con = ConnectionBean.getConnection();
			st = con.createStatement();
			rs = st.executeQuery(query);

			if(rs.next()) {
				RTaskName= rs.getString("RTaskName");
			}
		} catch (Exception E) {
			System.err.println("SurveyResult.java - GroupSection - " + E);
		} finally {
			ConnectionBean.closeRset(rs); // Close ResultSet
			ConnectionBean.closeStmt(st); // Close statement
			ConnectionBean.close(con); // Close connection
		}

		OO.findAndReplace(xSpreadsheet, "<CP>", trans.tslt(templateLanguage, RTaskName));
		//End Edit by Tracy 01 Sep 08******************************
		int [] address = OO.findString(xSpreadsheet, "<Normative>");

		column = address[0];
		row = address[1];

		OO.findAndReplace(xSpreadsheet, "<Normative>", "");
		//OO.insertRows(xSpreadsheet, startColumn, endColumn, row, row+26, 26, 1);


		Vector allTargets 		= null;
		Vector targetResult 	= CPCPR("CP");		
		Vector otherTarget 		= null;
		int totalTargets 		= TotalTargetsIncluded();		// include SELF		

		//Changed by Alvis on 29 Sep 09 to ensure weight retains its decimal value
		double weight = (double) 100/totalTargets;
		int total 	= totalCompetency();
		int tot 	= 0;
		int compID 	= 0;

		//Initialise array for arrN
		arrN = new int[total * 10 * 6]; //size = total competency * max 10 KBs * max 6 Rating

		//double [] normative = new double[total];
		//String comp [] 		= new String[total];
		double normative = 0;
		String comp		 = "";

		int r = row;

		//by Hemilda 15/09/2008 remove the competencies 0 at chart normative report

		for(int i=0; i<targetResult.size(); i++) {		// particular target's result by All
			String [] arr = (String[])targetResult.elementAt(i); //arr is each competency, loop through all competencies

			allTargets = null;
			allTargets = TargetsID();

			tot = 0;		// 1 for the target himself
			normative = 0;

			compID 			= Integer.parseInt(arr[0]); //get targetcompetency id
			comp	 		= arr[1]; //get target competency name
			double target 	= Double.parseDouble(arr[2]); //target competency score
			//System.out.println("Self score="+target);

			for(int j=0; j<allTargets.size(); j++) {

				int iTargetLoginID = ((Integer)allTargets.elementAt(j)).intValue();
				otherTarget = getOtherTargetResults(iTargetLoginID, compID);
				//Changed by HA 07/07/08
				//The result returned is a vector with only one element because 
				//it stores value of each Target, therefore should not have 
				// j < otherTarget.size()
				if(otherTarget.size() != 0 ) {
					//Changed by Ha 07/07/08 should only retrieve the first element
					String [] arrOther = (String[])otherTarget.elementAt(0);

					double all = Double.parseDouble(arrOther[2]); //the other targetscore
					//System.out.println("target" + j + "score= " + all);

					if(target >= all)
						tot++; //count how many with lower or equal score than target
				}				
			}

			//to round up to 2 decimal points
			//int twodec = 100 - ((int)((double)tot/(double)totalTargets * 100 * 100));
			//Changed by Alvis on 25 Sep 09 formula to calculate normative score and ensure normative score is >= 0.
			tot = tot + 1; //include target candidate himself and those with lower or equal score(same as total no. of candidates-no. of candidates with scores higher than the target candidate)
			//System.out.println("tot="+tot);
			//Add by Alvis on 29 Sep 09 to make sure the person with the highest score has a normative score of 100
			if ( (totalTargets != 0) && ((allTargets.size()+ 1 - tot) > 0) ){ 
				normative = tot * weight;
				//Added by Alvis on 29 Sep 09 to round off normative score to 2 decimal places
				normative = ((double)((int)((normative+0.005)*100.0)))/100.0;
			}
			else {
				normative = 100;
			}
			System.out.println(comp + "-----" + normative);

			//Added translation for the competency name, Chun Yeong 1 Aug 2011
			OO.insertString(xSpreadsheet2, getTranslatedCompetency(UnicodeHelper.getUnicodeStringAmp(comp)).elementAt(0).toString(), r, 0);
			OO.insertNumeric(xSpreadsheet2, normative, r, 1);

			r++;


		}

		//rianto
		//int height = 2000 * (r - row);
		//if((r - row) > 6)
		//height = 14000;

		//Allow dynamic translation, Chun Yeong 1 Aug 2011
		String title = trans.tslt(templateLanguage, "Normative Report for") + " " + UserName() + " vs " + surveyInfo[1];

		//Commented away to allow translation above, Chun Yeong 1 Aug 2011
		//if(ST.LangVer == 2)
		//	title = "Laporan Normative untuk " + UserName() + " vs " + surveyInfo[1];

		/* rianto
		//draw chart
		OO.setFontSize(8);
		//XTableChart xtablechart = OO.getChart(xSpreadsheet, xSpreadsheet2, 0, 1, row, r-1, "Normative", 13000, height, row, 1);
		XTableChart xtablechart = OO.getChart(xSpreadsheet, xSpreadsheet2, 0, 1, row, r-1, "Normative", 16000, height + 1500, row, 1);
		xtablechart = OO.setChartTitle(xtablechart, title);
		xtablechart = OO.setAxes(xtablechart, "Competencies", "Results (%)", 100, 10, 4500, 2000);
		OO.setChartProperties(xtablechart, false, true, true, true, true);
		 */

		XTableChart xtablechart = OO.getChartByIndex(xSpreadsheet, 1);
		xtablechart = OO.setChartTitle(xtablechart, title);


		//OO.setSourceData(xSpreadsheet, xSpreadsheet2, 1, 0, 1, row+1, r-1);
		// Changed by Ha 26/05/08 change parameters passing to the following method
		OO.setSourceData(xSpreadsheet, xSpreadsheet2, 1, 0, 1, row, r);

		//Set back to Bar Chart Horizontal, because system automatically set back to Bar Chart Vertical.
		//Remove the horizontal gridlines, Mark Oei 25 Mar 2010
		OO.setChartProperties(xtablechart, false, true, true, true, true); 

		//	System.out.println("6. Normative Completed");
	}


	/**
	 * Write competency report to excel.
	 */

	//Added by Tracy 01 Sep 08**********************************
	public void InsertCompGap(int surveyID) throws SQLException, Exception {
		//System.out.println("6.1 Competency Gap Insertion Starts");

		int i=0;
		Vector RTaskID= new Vector();
		Vector RTaskName=new Vector();

		//Get Rating from database according to survey ID
		String query = "SELECT a.RatingTaskID as RTaskID, b.RatingTask as RTaskName FROM tblSurveyRating a ";
		query += "INNER JOIN tblRatingTask b ON a.RatingTaskID=b.RatingTaskID  WHERE a.SurveyID = "+ surveyID;

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try {
			con = ConnectionBean.getConnection();
			st = con.createStatement();
			rs = st.executeQuery(query);

			while (rs.next()) {
				RTaskID.add(i,new Integer(rs.getInt("RTaskID")));
				RTaskName.add(i,new String(rs.getString("RTaskName")));
				i++;
			}

			//Check CPR or FPR
			String pType="";
			String CPR="";
			String FPR="";

			for (int n=0; RTaskID.size()-1>=n; n++ ) {
				if (((Integer)RTaskID.elementAt(n)).intValue()==1) {
					//CP=RTaskName.elementAt(n).toString();
				}else if (((Integer)RTaskID.elementAt(n)).intValue()==2){
					CPR=RTaskName.elementAt(n).toString();
					pType="C";
				}else if (((Integer)RTaskID.elementAt(n)).intValue()==3){
					FPR=RTaskName.elementAt(n).toString();
					pType="F";
				}
			}

			String RPTitle = "";
			if (pType.equals("C"))
				RPTitle= CPR ;
			else if (pType.equals("F"))
				RPTitle= FPR ;

			//Insert title to excel file
			OO.findAndReplace(xSpreadsheet, "<CompRP>", RPTitle);		


		} catch (Exception E) {
			System.err.println("SurveyResult.java - InsertCompGap - " + E);
		} finally {
			ConnectionBean.closeRset(rs); // Close ResultSet
			ConnectionBean.closeStmt(st); // Close statement
			ConnectionBean.close(con); // Close connection

		}
	}
	// End add by Tracy 01 Sep 08*******************************
	/**
	 * Added by Santoso : 16 Oct 2008
	 * A helper method to merge and center a cell
	 * 
	 */
	private void mergeAndCenter(XSpreadsheet xSpreadsheet, int startRow, int endRow) throws Exception {
		OO.mergeCells(xSpreadsheet, 0, 0, startRow, endRow);
		OO.setCellAllignment(xSpreadsheet, 0, 0, startRow, startRow, 2, 2);
	}

	/**
	 * Added by Santoso : 16 Oct 2008
	 * To prepare the cells to write the number of rater
	 * 
	 * @return array of row position for writing nr. of rater starting from bottom to top
	 * @throws Exception
	 * Qiao Li (21 Dec 2009)
	 * precondition: totalRater can only be 4, 5 or 6 
	 * 
	 */
	private int[] prepareCells(XSpreadsheet xSpreadsheet, int startingRow, int totalRater) throws Exception {
		int[] result = new int[6]; // Change size of result from 5 to 6 so as to cater for the splitting of peers and subordinates, Desmond 21 Oct 09
		OO.setFontSize(xSpreadsheet, 0, 0, startingRow, startingRow + 11, 12);
		/* Change (s): seperate the cases to totalRater == 5 and totalRater == 6, throw exeption if totalRoter not 4, 5 or 6
		 * Reason (s): accomodate the different alignment of n for different number of raters
		 * Updated by: Qiao Li
		 * Date: 21 Dec 2009
		 */
		if (totalRater == 6) {

			// Modified to suit new layout in order to cater for splitting of Peers and Subordinates, Desmond 22 Oct 09
			result[0] = startingRow + 2;
			result[1] = startingRow + 4;
			result[2] = startingRow + 6;
			result[3] = startingRow + 8;
			result[4] = startingRow + 10;

			// Added so that the n value is aligned properly after splitting peers and subordinates, Desmond 22 Oct 09
			result[5] = startingRow + 12;

		}else if (totalRater == 5){
			result[0] = startingRow + 2;
			result[1] = startingRow + 4;
			result[2] = startingRow + 7;
			result[3] = startingRow + 9;
			result[4] = startingRow + 12;
			mergeAndCenter(xSpreadsheet, result[1], result[1]+1);
			mergeAndCenter(xSpreadsheet, result[3], result[3]+1);

		}
		else if (totalRater == 4){
			// at least there is 4
			result[0] = startingRow + 2;
			mergeAndCenter(xSpreadsheet, result[0], result[0]+1);
			result[1] = startingRow + 5;
			mergeAndCenter(xSpreadsheet, result[1], result[1]+1);
			result[2] = startingRow + 8;
			mergeAndCenter(xSpreadsheet, result[2], result[2]+1);
			result[3] = startingRow + 11;
			mergeAndCenter(xSpreadsheet, result[3], result[3]+1);
		} else{
			result[0] = startingRow + 2;
			result[1] = startingRow + 4;
			result[2] = startingRow + 6;
			result[3] = startingRow + 8;
			result[4] = startingRow + 10;
			result[5] = startingRow + 12;
			System.out.println("Invalid parameter totalRater - IndividualReport.java");
		}
		return result;
	}

	private int findRatingIdx(String name, String[] rating, double[] result, int[] totalRater, String[] newRating, 
			double[] newResult, int[] newTotalRater, int idx, List ratingProcessed) {
		int ratingIdx = -1;
		for (int i = 0; i < rating.length; i++) {
			if (rating[i] != null && rating[i].equals(name)) {
				ratingIdx = i;
				break;
			}
		}
		if (ratingIdx != -1) {
			newRating[idx] = rating[ratingIdx];
			newResult[idx] = result[ratingIdx];
			newTotalRater[idx++] = totalRater[ratingIdx];
			ratingProcessed.add(new Integer(ratingIdx));
		}
		return idx;
	}

	/**
	 * Added by Santoso
	 * Sort Rating and Result array
	 * The order is CP(ALL), CP(SELF), CP(OTHERS), CP(SUPERVISORS), CPR(ALL)
	 * @param params array containing Rating and Result
	 */
	private Object[] sortRatingResult(Object[] params) {
		String[] rating = (String[]) params[0];
		double[] result = (double[]) params[1];
		int[] totalRater = (int[]) params[2];
		String[] newRating = new String[rating.length];
		double[] newResult = new double[result.length];
		int[] newTotalRater = new int[totalRater.length];

		int idx = 0;
		List ratingProcessed = new ArrayList();
		// we do it in a simple way, hardcoding the rating name here
		idx = findRatingIdx("CP(All)", rating, result, totalRater, newRating, newResult, newTotalRater, idx, ratingProcessed);
		idx = findRatingIdx("CP(Self)", rating, result, totalRater, newRating, newResult, newTotalRater, idx, ratingProcessed);
		idx = findRatingIdx("CP(Others)", rating, result, totalRater, newRating, newResult, newTotalRater, idx, ratingProcessed);
		idx = findRatingIdx("CP(Supervisors)", rating, result, totalRater, newRating, newResult, newTotalRater, idx, ratingProcessed);
		idx = findRatingIdx("CPR(All)", rating, result, totalRater, newRating, newResult, newTotalRater, idx, ratingProcessed);

		// do we have some rating not inserted yet?
		if (ratingProcessed.size() < rating.length) {
			for (int i = 0; i < rating.length; i++) {
				if (ratingProcessed.contains(new Integer(i))) {
					continue;
				}
				newRating[idx] = rating[i];
				newResult[idx] = result[i];
				newTotalRater[idx++] = totalRater[i];
			}
		}

		return new Object[] {newRating, newResult, newTotalRater};
	}

	/**by Santoso 2008/10/29
	 * Count total rater for the particular survey and target.
	 * for KB level
	 * To calculate number of others rater  for each rating task of each KB
	 */	
	/* Change (s): add in surveyLevel to use different tables 
	 * (tblResultCompetency<-surveyLevel == 0 and tblResultKeyBehavior<- surveyLevel ==1) 
	 * to retrieve number of raters
	 * Updated by: Qiao Li 21 Dec 2009
	 */
	public int totalRater(int iRatingTaskID, int iCompetencyID, int iKBID, String raterCode, int surveyLevel) throws SQLException 
	{	
		int total = 0;
		SurveyResult SR = new SurveyResult();
		Calculation cal = new Calculation();
		String query = "select count(*) AS Total ";
		query = query + " From( ";
		query = query + " SELECT DISTINCT tblAssignment.RaterCode";
		query = query + " FROM tblAssignment INNER JOIN ";
		if (surveyLevel==0){
			query = query+ "tblResultCompetency ON tblAssignment.AssignmentID = tblResultCompetency.AssignmentID ";
			query = query + " WHERE     (tblAssignment.SurveyID =  " + surveyID + ") AND (tblAssignment.TargetLoginID = "+targetID+") " ;
			if (cal.NAIncluded(surveyID)==0)
				query = query + " AND RaterCode LIKE '"+raterCode+"' and RaterStatus in(1,2,4)";
			else
				query = query + " AND RaterCode LIKE '"+raterCode+"' and RaterStatus in(1,2,4,5)";
			query = query + "  AND (tblResultCompetency.RatingTaskID = "+iRatingTaskID+")and (tblResultCompetency.CompetencyID = "+iCompetencyID +") ";
			if (cal.NAIncluded(surveyID)==0)
				query = query + " AND (tblResultCompetency.Result <> 0)";
			query = query + "  ) table1 ";
		}
		else{
			query = query + " tblResultBehaviour ON tblAssignment.AssignmentID = tblResultBehaviour.AssignmentID INNER JOIN ";
			query = query + " KeyBehaviour ON tblResultBehaviour.KeyBehaviourID = KeyBehaviour.PKKeyBehaviour ";
			query = query + " WHERE     (tblAssignment.SurveyID =  " + surveyID + ") AND (tblAssignment.TargetLoginID = "+targetID+") " ;
			if (cal.NAIncluded(surveyID)==0)
				query = query + " AND RaterCode LIKE '"+raterCode+"' and RaterStatus in(1,2,4)";
			else
				query = query + " AND RaterCode LIKE '"+raterCode+"' and RaterStatus in(1,2,4,5)";
			query = query + "  AND (tblResultBehaviour.RatingTaskID = "+iRatingTaskID+")and ";
			if(iKBID==-1)
				query+="(KeyBehaviour.FKCompetency = "+iCompetencyID +") ";
			else
				query+="(KeyBehaviour.PKKeyBehaviour = "+iKBID +") ";
			
			if (cal.NAIncluded(surveyID)==0)
				query = query + " AND (tblResultBehaviour.Result <> 0)";
			query = query + "  ) table1 ";
		}

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			if(rs.next())
				total = rs.getInt(1);


		}catch(Exception ex){
			System.out.println("IndividualReport.java - totalRater - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}
		return total;

	}
	/*
	 * Added to print cluster in the report
	 * by Albert
	 * 29 June 2012
	 */
	public void InsertClusterCompetency(int reportType, int SurveyID) throws SQLException, Exception {

		int iN = 0; //To be used as counter for arrN

		//  System.out.println("7. Competencies Starts");

		int [] address = OO.findString(xSpreadsheet, "<Report>");

		column = address[0];
		row = address[1];

		OO.findAndReplace(xSpreadsheet, "<Report>", "");

		int surveyLevel = Integer.parseInt(surveyInfo[0]);

		//Commented away to allow translation below, Chun Yeong 1 Aug 2011
		//String level 	= "Competency";
		//if (ST.LangVer == 2) 
		//	level = "Kompetensi";

		// Change by Santoso (22/10/08)
		// store the result of totalOtherRT to be reused below
		int totalOtherRT = totalOtherRT();
		int total = totalGroup() + totalOtherRT + 1;		// 1 for all
		//added in a variable to store number of types we have (16 Dec 2009 Qiao Li)
		int totalType = 6;
		/* Change: retrieve splitOthersOption of this survey 
		 * Reason: see whether "Others" is splitted
		 * Updated by: Qiao Li
		 * Date: 17 Dec 2009
		 */

		// int rowTotal 	= row + 1;
		// Changed by Ha 27/05/08: size of array from total --> total + 1
		// String [] Rating = new String [total+1];
		// double [] Result = new double [total+1];
		// Change by Santoso 22/10/08
		// Rating & Result now fixed to 4 + totalOtherRT
		// i.e. CP(All), CP(Sups), CP(Oth), CP(Self) and/or CPR(All)/FPR(All)
		// we can hardcode the string here anyway, since it is also hardcoded belows

		//used variable totalType to determine the initialization of array size (16 Dec 2009 Qiao Li) 
		String [] Rating = new String [totalType + totalOtherRT]; // Changed from 4 + totalOtherRT to 5 + totalOtherRT, to clean up later on, DeZ 
		double [] Result = new double [totalType + totalOtherRT]; // Changed from 4 + totalOtherRT to 5 + totalOtherRT, to clean up later on, DeZ
		int[] totalRater = new int[totalType + totalOtherRT]; // Changed from 4 + totalOtherRT to 5 + totalOtherRT, to clean up later on, DeZ

		// initialize Rating --> CP only, will be replace later on in this method if scores exists for that row, added comments here, Desmond 22 Oct 09
		Rating[0] = "CP(All)";
		Rating[1] = "CP(Superior)"; // Change from Supervisors to Superior, Desmond 22 Oct 09
		//added back the others for type customization(16 Dec 2009 Qiao Li)
		Rating[2] = "CP(Others)"; // Commented away to remove CP(Others) to cater for splitting Subordinates and Peers, Desmond 22 Oct 09
		Rating[3] = "CP(Self)";
		// Add additional categories for bar graph to cater for splitting of Subordinates and Peers, Desmond 21 Oct 09
		Rating[4] = "CP(Subordinates)";
		Rating[5] = "CP(Peers)";

		int maxScale = MaxScale();

		int count = 0; // to count total chart for each page, max = 2;
		int r1 = 1;
		int add = 13/total;
		//added back the others for type customization(16 Dec 2009 Qiao Li)
		int totalOth = 0; // Commented away to remove CP(Others) to cater for splitting Subordinates and Peers, Desmond 22 Oct 09
		int totalSup = 0;				
		int totalSelf = 0;
		int totalAll = 0;

		// Added new variables in order to cater for splitting of Subordinates and Peers, Desmond 21 Oct 2009
		int totalPeer = 0;
		int totalSub = 0;
		Vector vClust = ClusterByName();
		Vector vComp = null;
		insertPageBreak(xSpreadsheet, startColumn, endColumn, row);

		//Commented away to allow translation below, Chun Yeong 1 Aug 2011
		//if (ST.LangVer == 2) //Indonesian
		//	OO.insertString(xSpreadsheet, "Laporan Kompetensi", row, 0);
		//else //if (ST.LangVer == 1) //English
		//	OO.insertString(xSpreadsheet, "Competency Report", row, 0);

		//Allow dynamic translation, Chun Yeong 1 Aug 2011
		OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Competency Report"), row, 0);
		OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);

		row += 2;

		if(surveyLevel == 0) { 		// Competency Level Survey

			System.out.println("InsertClusterCompetency() - Survey is at Competency Level"); // To Remove, Dez
			int endRow = row; 
			//Added by Alvis 06-Aug-09 for pagebreak fix implemented below
			int currentPageHeight = 1076;//starting page height is set to 1076 to include the title of the competency report.

			for(int l=0; l<vClust.size(); l++) {
				voCluster voClust = (voCluster)vClust.elementAt(l);
				String clustName = voClust.getClusterName();
				int clusterID = voClust.getClusterID();

				int startRow = row;	// for border
				int RTID = 0;			
				int KBID = 0;

				int statementPos = row;					
				row++;

				r1 = row; 
				//Added tranlsation for the competency definition, Chun Yeong 1 Aug 2011
				OO.insertString(xSpreadsheet, clustName, row, 0);
				OO.mergeCells(xSpreadsheet, startColumn, endColumn, row, row);
				//adjust the merged cell as top alignment (Qiao Li 21 Dec 2009 )
				OO.setCellAllignment(xSpreadsheet, startColumn, endColumn, row, row, 2, 1);
				row++;

				vComp = ClusterCompetencyByName(clusterID);
				for(int i=0; i<vComp.size();i++){
					voCompetency voComp = (voCompetency)vComp.elementAt(i);

					int compID 		= voComp.getCompetencyID();
					String statement = voComp.getCompetencyName();
					String desc = voComp.getCompetencyDefinition();

					startRow = row;	// for border
					RTID = 0;			
					KBID = 0;

					statementPos = row;					
					row++;

					r1 = row; 
					//Added tranlsation for the competency definition, Chun Yeong 1 Aug 2011
					OO.insertString(xSpreadsheet, getTranslatedCompetency(statement).elementAt(1).toString(), row, 0);
					OO.mergeCells(xSpreadsheet, startColumn, endColumn, row, row);
					//adjust the merged cell as top alignment (Qiao Li 21 Dec 2009 )
					OO.setCellAllignment(xSpreadsheet, startColumn, endColumn, row, row, 2, 1);
					OO.setRowHeight(xSpreadsheet, row, 1, ROWHEIGHT*OO.countTotalRow(desc, 90));
					row++;

					String RTCode = "";

					Vector RT = RatingTask();
					int r = 0;


					boolean hasCPRFPR = false;
					for(int j=0; j<RT.size(); j++) {
						votblSurveyRating vo = (votblSurveyRating)RT.elementAt(j);

						RTID = vo.getRatingTaskID();
						RTCode 	=  vo.getRatingCode();


						Vector result = MeanResult(RTID, compID, KBID);


						if(RTCode.equals("CP")) 
						{
							//Changed by Ha 09/07/08 to calculate total rater for each competency for each rating task
							//added back the others for type customization(16 Dec 2009 Qiao Li)
							totalOth = totalOth(1,compID); // Commented away to remove CP(Others) to cater for splitting Subordinates and Peers, Desmond 22 Oct 09
							totalSup = totalSup(1, compID);						
							totalSelf = totalSelf(1, compID);

							// Added to cater to the splitting of peers and subordinates, Desmond 21 Oct 09
							/* Change (s): add in surveyLevel to use different tables 
							 * (tblResultCompetency and tblResultKeyBehavior) to retrieve number of raters
							 * Updated by: Qiao Li 21 Dec 2009
							 */
							totalPeer = totalRater(RTID, compID, -1, "PEER%", surveyLevel);
							totalSub = totalRater(RTID, compID, -1, "SUB%", surveyLevel);

							// Re-locate and modified codes to include Peers and Subordinates by Desmond 21 Oct 09
							totalAll = totalSup + totalPeer + totalSub + totalOth;

							for(int k=0; k<result.size(); k++) 
							{							
								String [] arrOther = (String[])result.elementAt(k);

								int type = Integer.parseInt(arrOther[1]);
								String t = "";
								switch(type) 
								{
								case 1 : t = "All";

								arrN[iN] = totalAll;
								iN++;
								// Change by Santoso 22/10/08
								// the total rater will be printed later below
								// set the valur of total all here
								totalRater[0] = totalAll;
								break;
								case 2 : t = "Superior"; // Change from Supervisors to Superior, Desmond 22 Oct 09
								arrN[iN] = totalSup;
								iN++;
								// Change by Santoso 22/10/08
								// the total rater will be printed later below
								// set the valur of total sup here
								totalRater[1] = totalSup;
								break;
								//added back the others for type customization(16 Dec 2009 Qiao Li)
								case 3 : t = "Others";
								arrN[iN] = totalOth;
								iN++;
								// Change by Santoso 22/10/08
								// the total rater will be printed later below
								// set the valur of total others here
								totalRater[2] = totalOth;
								break;
								case 4 : t = "Self";
								arrN[iN] = totalSelf;
								iN++;
								// Change by Santoso 22/10/08
								// the total rater will be printed later below
								// set the valur of total self here
								totalRater[3] = totalSelf;
								break;

								// Added case 5 and 6 to cater to splitting of Peers and Subordinates by Desmond 21 Oct 09
								case 5 : t = "Subordinates";
								arrN[iN] = totalSub;
								iN++;
								totalRater[4] = totalSub;
								break;
								case 6 : t = "Peers";
								arrN[iN] = totalPeer;
								iN++;
								totalRater[5] = totalPeer;
								break;
								}

								//Should not insert a "\n". Will push col into 2 rows. Printing will go haywire (Maruli)
								//Rating[r] = RTCode + "\n(" + t + ")";

								// Change by Santoso 22/10/08
								// Since we have fixed the order of rating (according to the type)
								// we can set the Rating text here using type-1 as the index
								Rating[type-1] = RTCode + "(" + t + ")";
								if(iReportType == 1)
									Rating[type-1] = Rating[r].replaceAll("\n", " ");

								Result[type-1] = Double.parseDouble(arrOther[2]);

								//If i don't split..means = others
								//Result[2] = value;
							}
						}
						else if(RTCode.equals("CPR") || RTCode.equals("FPR"))
						{	
							//Changed by Ha 26/06/08 should not have j < result.size in the condition
							//Problem with old condition: value were not displayed correctly
							if (result.size()  > 0)
							{						

								// Change by Santoso 22/10/08
								// hasCPRFPR will be needed later, we need to set the value to true here
								hasCPRFPR = true;
								String [] arrOther = (String[])result.elementAt(0);

								//Should not insert a "\n". Will push col into 2 rows. Printing will go haywire (Maruli)
								//Rating[r] = RTCode + "\n(All)";

								/*
								 * Change(s) : Change to get the correct number of raters for CPR when there is a split
								 * Updated By: Mark Oei 1 Mar 2010
								 * Previous Updates:
								 * - Change by Santoso 22/10/08 : Rating order and value already initialized above (also the Result)
								 * we need to set the RTCode and the result at the appropriate position as already defined above
								 * - Change from 5 to totalType for customization (16 Dec 2009 Qiao Li)
								 */
								totalOth = totalRater(2, compID, -1, "OTH%", surveyLevel); 
								totalSup = totalRater(2, compID, -1, "SUP%", surveyLevel);

								totalPeer = totalRater(2, compID, -1, "PEER%", surveyLevel);
								totalSub = totalRater(2, compID, -1, "SUB%", surveyLevel);

								Rating[totalType] = RTCode + "(All)"; // Changed from Rating[4] to Rating[5] to cater for new catergories Subordinates & Peers, Desmond 22 Oct 09 

								if(iReportType == 1)
									Rating[totalType] = Rating[totalType].replaceAll("\n", " ");  // Changed from Rating[4] to Rating[5] to cater for new catergories Subordinates & Peers, Desmond 22 Oct 09

								Result[totalType] = Double.parseDouble(arrOther[2]);   // Changed from Rating[4] to Rating[5] to cater for new catergories Subordinates & Peers, Desmond 22 Oct 09

								if (RTCode.equals("CPR")){
									//Change to get correct number of raters for CPR when 
									//  there is a split, Mark Oei 01 Mar 2010
									if (splitOthers == 0)
										totalAll = totalSup(2,compID) + totalOth(2,compID);
									else
										totalAll = totalSup(2,compID) + totalPeer + totalSub;
								}
								else if (RTCode.equals("FPR"))
									totalAll = totalSup(3,compID) + totalOth(3,compID);

								arrN[iN] = totalAll;
								iN++;


								// Change by Santoso 22/10/08
								// the total rater will be printed later below
								// set the valur of total all here
								totalRater[totalType] = totalAll;  // Changed from totalRater[4] to totalRater[5] to cater for new catergories Subordinates & Peers, Desmond 22 Oct 09	


								// 11/12/2009 Denise
								// Change position to display CPR/FPR on the top 
								//change from 5 to totalType for customization (16 Dec 2009 Qiao Li)
								double CPRFPR_Result = Result[totalType];
								int    CPRFPR_TotalRating = totalRater[totalType];

								//change from 5 to totalType for customization (16 Dec 2009 Qiao Li)
								for (int x=totalType; x>0; x--)
								{
									totalRater[x] = totalRater[x-1];
									Result[x] = Result[x-1];
								}

								Result[0] = CPRFPR_Result;
								totalRater[0] = CPRFPR_TotalRating;
								//change from 5 to totalType for customization (17 Dec 2009 Qiao Li)
								Rating[0] = Rating[totalType];
								Rating[1] = "CP(All)";
								Rating[2] = "CP(Superior)";
								//added back the others for type customization(16 Dec 2009 Qiao Li)
								//Change CP(Other) to CP(Others) in order for the competency bar graph 
								//  to be displayed for split with CPR, 01 Mar 2010 Mark Oei 
								Rating[3] = "CP(Others)";
								Rating[4] = "CP(Self)";
								Rating[5] = "CP(Subordinates)";
								Rating[6] = "CP(Peers)";
							}
						}
					}
					//rater type can change depends on whether "Others" is splitted
					//get the appropriate number and prepareCells(Qiao Li 17 Dec 2009)
					int totRaterType = Rating.length;
					if (splitOthers== 0){
						totRaterType -= 2;
					}
					else{
						totRaterType -=1;
					}
					//while RT
					// Change by Santoso 22/10/08
					// Alignment of n number with the bar of the graph 
					// was : rowTotal = row + 11;
					//int[] rowPos = prepareCells(xSpreadsheet, row, totalRater.length);
					int[] rowPos = prepareCells(xSpreadsheet, row, totRaterType);
					row++;		//start draw chart from here
					OO.setFontSize(12);

					// Change by Santoso 22/10/08
					// the total rater will be printed in drawChart therefore we need
					// to pass the rating point and the position
					drawChart(Rating, Result, totalRater, rowPos, maxScale, splitOthers);

					//  12/12/2009 Denise
					// reinitialize Result and totalRater
					//change from 5 to totalType for customization (16 Dec 2009 Qiao Li)
					Result = new double [totalType + totalOtherRT]; 
					totalRater = new int[totalType + totalOtherRT]; 

					column = 9;		//write the importance n gap
					int rtemp = row;

					Vector vImportance = Importance(compID, KBID);

					for(int j=0; j<vImportance.size(); j++) {
						String [] arr = (String[]) vImportance.elementAt(j);
						String task = arr[1];
						double taskResult = Double.parseDouble(arr[2]);

						OO.insertString(xSpreadsheet, task + ": " + taskResult, rtemp, column);
						OO.mergeCells(xSpreadsheet, column, endColumn, rtemp, rtemp+1);
						OO.setCellAllignment(xSpreadsheet, column, endColumn, rtemp, rtemp+1, 2, 1);

						rtemp += 3;
					}

					double gap = 0;

					// Change by Santoso 22/10/08
					// only calculate Gap if survey include CPR/FPR Rating task
					if (hasCPRFPR) {
						gap = getAvgGap(compID);
						// If CPR is chosen in this survey
						{
							//Changed the default language to English by Chun Yeong 9 Jun 2011
							//Commented away to allow translation below, Chun Yeong 1 Aug 2011
							/*if (ST.LangVer == 2) //Indonesian
								OO.insertString(xSpreadsheet, "Selisih = " + gap, rtemp, column);
							else //if (ST.LangVer == 1) English
								OO.insertString(xSpreadsheet, "Gap = " + gap, rtemp, column);*/

							//Allow dynamic translation, Chun Yeong 1 Aug 2011
							OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Gap") + " = " + gap, rtemp, column);
							OO.mergeCells(xSpreadsheet, column, endColumn, rtemp, rtemp+1);

							OO.setCellAllignment(xSpreadsheet, column, endColumn, rtemp, rtemp+1, 2, 1);	
						}				
					}
					rtemp+=3;

					double LOA = LevelOfAgreement(compID, KBID);
					//Changed the default language to English by Chun Yeong 9 Jun 2011
					//Commented away to allow translation below, Chun Yeong 1 Aug 2011
					/*if (ST.LangVer == 2) //Indonesian
						OO.insertString(xSpreadsheet, "Tingkat Persetujuan: \n" + LOA + "%", rtemp, column);
					else //if (ST.LangVer == 1) English
						OO.insertString(xSpreadsheet, "Level Of Agreement: \n" + LOA + "%", rtemp, column);*/

					//Allow dynamic translation, Chun Yeong 1 Aug 2011
					OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Level Of Agreement")+": \n" + LOA + "%", rtemp, column);
					OO.mergeCells(xSpreadsheet, column, endColumn, rtemp, rtemp+1);
					OO.setCellAllignment(xSpreadsheet, column, endColumn, rtemp, rtemp+1, 2, 1);

					column = 0;
					count++;

					//Removed by Alvis for Pagebreaks problem
					//if(count == 2) {
					//count = 0;
					//	row += 17;					
					//	insertPageBreak(xSpreadsheet, startColumn, endColumn, row);
					//} else {
					//	column = 0;					
					//	row += 16;						
					//}
					//End of removal section by Alvis for Pagebreaks problem
					/* Change: standardize the number of rows for the charts to be 15 
					 * Reason: fit 2 charts in one page
					 * Updated by: Qiao Li
					 * Date: 23 Dec 2009
					 */
					row += 15;
					endRow = row-1;

					/*
					 * Change(s) : Added codes to check height of the table to be
					 * 			  added, and insert pagebreak if necessary
					 * Reason(s) : Fix the problem of a table being spilt into between 
					 * 			  two pages. 
					 * Updated By: Alvis 
					 * Updated On: 06 Aug 2009
					 */

					//Check height and insert pagebreak where necessary
					int pageHeightLimit = 22272	;//Page limit is 22272						    
					int tableHeight = 0;

					//calculate the height of the table that is being dded.
					for(int i1=startRow+1;i1<=endRow+1;i1++){
						int rowToCalculate = i1;
						tableHeight += OO.getRowHeight(xSpreadsheet, rowToCalculate, startColumn);

					}

					currentPageHeight = currentPageHeight + tableHeight; //add new table height to current pageheight.
					int dis = 2; //Denise 08/01/2009 to move the table two lines down
					if(currentPageHeight >pageHeightLimit){//adding the table will exceed a single page
						OO.insertRows(xSpreadsheet, startColumn, endColumn, startRow, startRow+dis, dis, 1);				
						insertPageBreak(xSpreadsheet, startColumn, endColumn, startRow);
						statementPos +=dis ;
						row +=dis;
						startRow +=dis;
						endRow +=dis;
						currentPageHeight = tableHeight;
					}
					//Denise 08/01/2009 insert competency statement
					//Added translation for the competency name, Chun Yeong 1 Aug 2011
					OO.insertString(xSpreadsheet, UnicodeHelper.getUnicodeStringAmp(getTranslatedCompetency(statement).elementAt(0).toString()), statementPos, 0);
					OO.setFontBold(xSpreadsheet, startColumn, endColumn, statementPos, statementPos);
					OO.setBGColor(xSpreadsheet, startColumn, endColumn, statementPos, statementPos, BGCOLOR);		
					//comp name and definition
					OO.setTableBorder(xSpreadsheet, startColumn, endColumn, startRow, startRow+1, 
							false, false, true, true, true, true);

					//total sup n others

					OO.setTableBorder(xSpreadsheet, startColumn, startColumn, startRow+2, endRow, 
							false, false, true, true, true, true);

					//chart

					OO.setTableBorder(xSpreadsheet, startColumn+1, 8, startRow+2, endRow, 
							false, false, true, true, true, true);
					OO.setTableBorder(xSpreadsheet, 9, endColumn, startRow+2, endRow, 
							false, false, true, true, true, true);
					OO.setCellAllignment(xSpreadsheet, startColumn, startColumn, startRow+2, endRow, 1, 2);
					OO.setCellAllignment(xSpreadsheet, startColumn, startColumn, startRow+2, endRow, 1, 2);

					//added by Alvis on 07-Aug-09 to ensure next section begin on a new page.
					if(i==(vComp.size()-1)){//last table added
						//insertpagebreak
						insertPageBreak(xSpreadsheet, startColumn, endColumn, endRow+1);
					}

					//	if (breakpage)
					//	{
					//		OO.insertRows(xSpreadsheet, startColumn, endColumn, startRow, startRow+1, 1, 0);
					//		row++;
					//	}
				}// end of for vComp.size
			} //end of for vClust.size
			// End Competency Level

		} else {

			//Start KB level
			System.out.println("InsertClusterCompetency() - Survey is at KB Level"); // To Remove, Dez
			int start = 0;
			int startRow = row;	// for border
			int endRow = row;						
			for(int m=0;m<vClust.size();m++){
				voCluster voClust = (voCluster)vClust.elementAt(m);
				String clusterName = voClust.getClusterName();
				int clusterID = voClust.getClusterID();

				OO.insertString(xSpreadsheet, clusterName, row, column);
				OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
				OO.setBGColor(xSpreadsheet, startColumn, endColumn, row, row, BGCOLORCLUSTER);
				row++;

				vComp = ClusterCompetencyByName(clusterID);
				for(int i=0; i<vComp.size(); i++)
				{					

					// Add by Santoso (22/10/08) : reinitialize array per loop
					// reinitialize the array each loop (otherwise it will use the previous value)
					totalRater = new int[totalRater.length];
					Result = new double[Result.length];

					// Reset only rating[4] since Rating[0]..[3] always have the same value
					/*				if (Rating.length > 4) {
						Rating[4] = "";
					}*/

					// Change by Santoso (22/10/08)
					// No need to calculate rowTotal anymore since the raters displayed on the graph are fixed
					//rowTotal = row + 1;
					start = 0;
					int RTID = 0;

					int KBID = 0;
					String KB = "";

					voCompetency voComp = (voCompetency)vComp.elementAt(i);

					int compID = voComp.getCompetencyID();

					String statement = voComp.getCompetencyName();
					String desc = voComp.getCompetencyDefinition();

					startRow = row;

					//Added translation for competency name, Chun Yeong 1 Aug 2011
					OO.insertString(xSpreadsheet, getTranslatedCompetency(UnicodeHelper.getUnicodeStringAmp(statement)).elementAt(0).toString(), row, column);
					OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
					OO.setBGColor(xSpreadsheet, startColumn, endColumn, row, row, BGCOLOR);			
					row++;

					r1 = row;
					OO.insertString(xSpreadsheet, UnicodeHelper.getUnicodeStringAmp(desc), row, column);
					OO.mergeCells(xSpreadsheet, startColumn, endColumn, row, row);
					//adjust the merged cell as top alignment (Qiao Li 21 Dec 2009 )
					OO.setCellAllignment(xSpreadsheet, startColumn, endColumn, row, row, 2, 1);
					OO.setRowHeight(xSpreadsheet, row, 1, ROWHEIGHT*OO.countTotalRow(desc, 90));

					row++;
					start++;

					String RTCode = "";

					Vector RT = RatingTask();


					boolean hasCPRFPR = false;
					//Print out the "N" in other section for simplified report

					for(int j=0; j<RT.size(); j++) {
						votblSurveyRating vo = (votblSurveyRating)RT.elementAt(j);

						RTID = vo.getRatingTaskID();
						RTCode 	=  vo.getRatingCode();

						Vector result = null;

						if(RTCode.equals("CP")) {
							result = KBMean(RTID, compID);

							// Change by Santoso 2008/10/29
							// use a new query for retrieving total rater
							//added back the others for type customization(16 Dec 2009 Qiao Li)
							/* Change (s): add in surveyLevel to use different tables 
							 * (tblResultCompetency and tblResultKeyBehavior) to retrieve number of raters
							 * Updated by: Qiao Li 21 Dec 2009
							 */
							totalOth = totalRater(RTID, compID, -1, "OTH%", surveyLevel); // Commented away to remove CP(Others) to cater for splitting Subordinates and Peers, Desmond 22 Oct 09
							totalSup = totalRater(RTID, compID, -1, "SUP%", surveyLevel);
							totalSelf = totalRater(RTID, compID, -1, "SELF", surveyLevel);

							// Added to cater to the splitting of peers and subordinates, Desmond 21 Oct 09
							totalPeer = totalRater(RTID, compID, -1, "PEER%", surveyLevel);
							totalSub = totalRater(RTID, compID, -1, "SUB%", surveyLevel);

							// Re-locate and modified codes to include Peers and Subordinates by Desmond 22 Oct 09							
							totalAll = totalSup + totalPeer + totalSub + totalOth;

							for(int k=0; k<result.size(); k++) {

								String [] arr = (String[])result.elementAt(k);

								// Updated adjustments to type in order to cater for splitting of subordinates & peers, Desmond 28 Oct 09
								int type = Integer.parseInt(arr[1]);
								//remove the hack, use back the type such that results for others are 
								//also retrieved (16 Dec 2009 Qiao Li)
								//if(type > 3) type = type -1; // Adjust values > 3 because we removed Others from the list

								String t = "";
								switch(type) 
								{
								case 1 : t = "All";

								arrN[iN] = totalAll;
								iN++;
								// Change by Santoso 22/10/08
								// the total rater will be printed later below
								// set the valur of total all here
								totalRater[0] = totalAll;
								break;
								case 2 : t = "Superior"; // Change from Supervisors to Superior, Desmond 22 Oct 09
								arrN[iN] = totalSup;
								iN++;
								// Change by Santoso 22/10/08
								// the total rater will be printed later below
								// set the valur of total sup here
								totalRater[1] = totalSup;
								break;
								//added back the others for type customization(16 Dec 2009 Qiao Li)
								case 3 : t = "Others";
								arrN[iN] = totalOth;
								iN++;
								// Change by Santoso 22/10/08
								// the total rater will be printed later below
								// set the valur of total others here
								totalRater[2] = totalOth;
								break;		 	
								case 4 : t = "Self";
								arrN[iN] = totalSelf;
								iN++;
								// Change by Santoso 22/10/08
								// the total rater will be printed later below
								// set the valur of total self here
								totalRater[3] = totalSelf;
								break;
								// Commented away to remove CP(Others) to cater for splitting Subordinates and Peers, Desmond 22 Oct 09										 
								// Added case 5 and 6 to cater to splitting of Peers and Subordinates by Desmond 21 Oct 09
								case 5 : t = "Subordinates";
								arrN[iN] = totalSub;
								iN++;
								totalRater[4] = totalSub;
								break;
								case 6 : t = "Peers";
								arrN[iN] = totalPeer;
								iN++;
								totalRater[5] = totalPeer;
								break;
								}
								//Should not insert a "\n". Will push col into 2 rows. Printing will go haywire (Maruli)
								//Rating[r] = RTCode + "\n(" + t + ")";
								// Change by Santoso 22/10/08 : Rating order and value already initialized above (also the Result)
								// we need to set the RTCode and the result at the appropriate position as already defined above
								Rating[type-1] = RTCode + "(" + t + ")";
								if(iReportType == 1)
									Rating[type-1] = Rating[type-1].replaceAll("\n", " ");

								System.out.print("InsertClusterCompetency() - Rating" + "[" + (type-1) + "]" + " = " + Rating[type-1]); // To Remove, Desmond

								if(type == 1)
									Result[type-1] = CompTrimmedMeanforAll(RTID, compID);
								else
									Result[type-1] = Double.parseDouble(arr[2]);

								//System.out.println(", Result" + "[" + (type-1) + "]" + " = " + Result[type-1]); // To Remove, Desmond
							}
						}else if(RTCode.equals("CPR") || RTCode.equals("FPR")){
							// Change by Santoso 22/10/08
							// need to keep track whether CPR/FPR is included in the survey or not to keep Gap from printed if no CPR/FPR
							hasCPRFPR = true;
							// Change by Santoso 2008/10/29 
							// use a new query for retrieving total rater
							//added back the others for type customization(16 Dec 2009 Qiao Li)
							/* Change (s): add in surveyLevel to use different tables 
							 * (tblResultCompetency and tblResultKeyBehavior) to retrieve number of raters
							 * Updated by: Qiao Li 21 Dec 2009
							 */
							totalOth = totalRater(RTID, compID, -1, "OTH%", surveyLevel); // Commented away to remove CP(Others) to cater for splitting Subordinates and Peers, Desmond 22 Oct 09
							totalSup = totalRater(RTID, compID, -1, "SUP%", surveyLevel);

							// Added to cater to the splitting of peers and subordinates, Desmond 21 Oct 09
							totalPeer = totalRater(RTID, compID, -1, "PEER%", surveyLevel);
							totalSub = totalRater(RTID, compID, -1, "SUB%", surveyLevel);

							// Re-locate and modified codes to include Peers and Subordinates by Desmond 22 Oct 09							
							totalAll = totalSup + totalPeer + totalSub + totalOth;

							arrN[iN] = totalAll;
							iN++;
							//Should not insert a "\n". Will push col into 2 rows. Printing will go haywire (Maruli)
							//Rating[r] = RTCode + "\n(All)";
							// Change by Santoso 22/10/08 : Rating order and value already initialized above (also the Result)
							// we need to set the RTCode and the result at the appropriate position as already defined above
							//change from 5 to totalType for customization (16 Dec 2009 Qiao Li)
							Rating[totalType] = RTCode + "(All)";
							if(iReportType == 1) {
								System.out.println("insertClusterCompetency() - iReportType = 1 so replace all \n"); // To Remove, Desmond
								Rating[totalType] = Rating[totalType].replaceAll("\n", " "); // Changed from Rating[4] to Rating[5] to cater for new catergories Subordinates & Peers, Desmond 22 Oct 09
							}

							totalRater[totalType] = totalAll; // Changed from Rating[4] to Rating[6] to cater for new catergories Subordinates & Peers, Desmond 22 Oct 09
							Result[totalType] = CompTrimmedMeanforAll(RTID, compID); // Changed from Rating[4] to Rating[5] to cater for new catergories Subordinates & Peers, Desmond 22 Oct 09
							System.out.println("insertClusterCompetency() - CPR Score - Result" + "[5]" + " = " + Result[5]); // To Remove, Desmond

							// 11/12/2009 Denise
							// Change position to display CPR/FPR on the top 
							double CPRFPR_Result = Result[totalType];
							int    CPRFPR_TotalRating = totalRater[totalType];
							//change from x=5 to totalType for customization (16 Dec 2009 Qiao Li)
							for (int x=totalType; x>0; x--)
							{
								totalRater[x] = totalRater[x-1];
								Result[x] = Result[x-1];
							}

							Result[0] = CPRFPR_Result;
							totalRater[0] = CPRFPR_TotalRating;

							Rating[0] = Rating[totalType];
							Rating[1] = "CP(All)";
							Rating[2] = "CP(Superior)"; 
							//added back the others for type customization(16 Dec 2009 Qiao Li)
							Rating[3] = "CP(Others)";
							Rating[4] = "CP(Self)";
							Rating[5] = "CP(Subordinates)";
							Rating[6] = "CP(Peers)";
						}				

					}//while RT
					//rater type can change depends on whether "Others" is splitted
					//get the appropriate number and prepareCells(Qiao Li 17 Dec 2009)
					int totRaterType = Rating.length;
					if (splitOthers== 0){
						totRaterType -= 2;
					}
					else{
						totRaterType -=1;
					}
					// Change by Santoso : Alignment of n number with the bar of the graph
					// rowTotal is no longer needed, the raters displayed are fixed
					// was : rowTotal = row + 11;
					//int[] rowPos = prepareCells(xSpreadsheet, row, totalRater.length);
					int[] rowPos = prepareCells(xSpreadsheet, row, totRaterType);
					row++;
					// Change by Santoso (2008-10-08)
					// total rater is printed inside drawChart, we need to pass
					// the total rater list and the position to draw
					drawChart(Rating, Result, totalRater, rowPos, maxScale, splitOthers);						

					//  12/12/2009 Denise
					// reinitialize Result and totalRater
					//change from 5 to totalType for customization (16 Dec 2009 Qiao Li)
					Result = new double [totalType + totalOtherRT]; // Changed from 4 + totalOtherRT to 5 + totalOtherRT, to clean up later on, DeZ
					totalRater = new int[totalType + totalOtherRT]; 

					column = 9;
					r1 = row;

					Vector Importance = AvgImportance(compID);

					for(int j=0; j<Importance.size(); j++) {
						String [] arr = (String[])Importance.elementAt(j);

						String task = arr[1];
						double taskResult = Double.parseDouble(arr[2]);

						OO.insertString(xSpreadsheet, task + ": " + taskResult, r1, column);
						OO.mergeCells(xSpreadsheet, column, endColumn, r1, r1+1);														
						r1 += 3;
					}

					double gap = 0;
					// Change by Santoso 22/10/08
					// only calculate Gap if survey include CPR/FPR Rating task
					if (hasCPRFPR) {
						int element = vCompID.indexOf(new Integer(compID));
						gap = Double.valueOf(((String [])vGapUnsorted.elementAt(element))[1]).doubleValue();
						//System.out.println(gap + "----" + compID + " --- " + element);
						if (iNoCPR == 0)	// If CPR is chosen in this survey
						{
							//Changed the default language to English by Chun Yeong 9 Jun 2011
							//Commented away to allow translation below, Chun Yeong 1 Aug 2011
							/*if (ST.LangVer == 2) //Indonesian
									OO.insertString(xSpreadsheet, "Selisih = " + gap, r1, column);	
								else // if (ST.LangVer == 1) English
									OO.insertString(xSpreadsheet, "Gap = " + gap, r1, column);*/

							//Allow dynamic translation, Chun Yeong 1 Aug 2011
							OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Gap") + " = " + gap, r1, column);
							OO.mergeCells(xSpreadsheet, column, endColumn, r1, r1+1);
						}
					}
					r1 += 3;

					double LOA = AvgLevelOfAgreement(compID, totalAll);
					//System.out.println(LOA + "----" + compID );

					//Changed the default language to English by Chun Yeong 9 Jun 2011
					//Commented away to allow translation below, Chun Yeong 1 Aug 2011
					/*if (ST.LangVer == 2) //Indonesian
							OO.insertString(xSpreadsheet, "Tingkat Persetujuan: \n" + LOA + "%", r1, column);
						else //if (ST.LangVer == 1) //English
							OO.insertString(xSpreadsheet, "Level Of Agreement: \n" + LOA + "%", r1, column);*/

					//Allow dynamic translation, Chun Yeong 1 Aug 2011
					OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Level Of Agreement") + ": \n" + LOA + "%", r1, column);	
					OO.mergeCells(xSpreadsheet, column, endColumn, r1, r1+1);									
					r1 += 3;

					count++;
					column = 0;
					if(count == 2) {
						count = 0;

						row += 15;						
						insertPageBreak(xSpreadsheet, startColumn, endColumn, row);
					} else {					
						row += 15;						
					}

					endRow = row-1;

					//comp name and definition
					OO.setTableBorder(xSpreadsheet, startColumn, endColumn, startRow, startRow+1, 
							false, false, true, true, true, true);

					//total sup n others				
					OO.setTableBorder(xSpreadsheet, startColumn, startColumn, startRow+2, endRow, 
							false, false, true, true, true, true);
					//chart
					OO.setTableBorder(xSpreadsheet, startColumn+1, 8, startRow+2, endRow, 
							false, false, true, true, true, true);	
					OO.setTableBorder(xSpreadsheet, 9, endColumn, startRow+2, endRow, 
							false, false, true, true, true, true);

					OO.setCellAllignment(xSpreadsheet, startColumn, startColumn, startRow+2, endRow, 1, 2);


					// KB LEVEL //				

					//(30-Sep-05) Rianto: Replaced with Simplified report with no competencies & KB charts
					//if(reportType == 2) {			// only if standard report, simplified report no need for KB

					Vector KBList = KBList(compID);	

					for(int j=0; j<KBList.size(); j++) {
						voKeyBehaviour voKB = (voKeyBehaviour)KBList.elementAt(j);
						KBID = voKB.getKeyBehaviourID();
						KB = voKB.getKeyBehaviour();

						startRow = row;
						r1 = row;

						//Added translation for the key behaviour, Chun Yeong 1 Aug 2011
						OO.insertString(xSpreadsheet, start + ". " + getTranslatedKeyBehavior(UnicodeHelper.getUnicodeStringAmp(KB)), row, 0);
						OO.mergeCells(xSpreadsheet, startColumn, endColumn, row, row);
						//adjust the merged cell as top alignment (Qiao Li 21 Dec 2009 )
						OO.setCellAllignment(xSpreadsheet, startColumn, endColumn, row, row, 2, 1);
						OO.setRowHeight(xSpreadsheet, row, 0, ROWHEIGHT*OO.countTotalRow(KB, 90));

						row += 2;
						//row ++;
						start++;

						//change from 5 to totalType for customization (16 Dec 2009 Qiao Li)
						totalRater = new int[totalType + totalOtherRT]; // Changed from 4 + totalOtherRT to 5 + totalOtherRT, to clean up later on, DeZ
						Result = new double [totalType + totalOtherRT]; // Changed from 4 + totalOtherRT to 5 + totalOtherRT, to clean up later on, DeZ

						RT = RatingTask();
						// Change by Santoso 22/10/08
						// only calculate Gap if survey include CPR/FPR Rating task
						// initialize cpr/fpr flag
						hasCPRFPR = false;
						for(int k=0; k<RT.size(); k++) {
							votblSurveyRating vo = (votblSurveyRating)RT.elementAt(k);
							RTID = vo.getRatingTaskID();
							RTCode 	=  vo.getRatingCode();

							Vector result = MeanResult(RTID, compID, KBID);

							if(RTCode.equals("CP")) {
								//Comment off by Ha 02/07/08 should not re-set the r to 0
								//It will print out the KB CP, CPR, FPR incorrecly
								//r = 0;

								//by Hemilda 23/09/2008 to get total oth,sup,self,all for each kb
								//added back the others for type customization(16 Dec 2009 Qiao Li)
								totalOth =totalOth(RTID, compID, KBID); // Commented away to remove CP(Others) to cater for splitting Subordinates and Peers, Desmond 22 Oct 09
								totalSup = totalSup(RTID, compID, KBID);						
								totalSelf = totalSelf(RTID, compID, KBID);

								// Added to cater to the splitting of peers and subordinates, Desmond 21 Oct 09
								/* Change (s): add in surveyLevel to use different tables 
								 * (tblResultCompetency and tblResultKeyBehavior) to retrieve number of raters
								 * Updated by: Qiao Li 21 Dec 2009
								 */
								totalPeer = totalRater(RTID, compID, KBID, "PEER%", surveyLevel);
								totalSub = totalRater(RTID, compID, KBID,"SUB%", surveyLevel);

								// Re-locate and modified codes to include Peers and Subordinates by Desmond 22 Oct 09							
								totalAll = totalSup + totalPeer + totalSub+totalOth;


								for(int l=0; l<result.size(); l++) {
									String [] arr = (String[])result.elementAt(l);

									// Updated adjustments to type in order to cater for splitting of subordinates & peers, Desmond 28 Oct 09
									int type = Integer.parseInt(arr[1]);
									//remove the hack, use back the type such that results for others are also retrieved (16 Dec 2009 Qiao Li)
									//if(type > 3) type = type -1; // Adjust values > 3 because we removed Others from the list

									String t = "";

									switch(type) {
									case 1 : t = "All";	
									arrN[iN] = totalAll;
									iN++;
									// Change by Santoso 22/10/08
									// the total rater will be printed later below
									// set the valur of total all here
									totalRater[0] = totalAll;
									break;
									case 2 : t = "Superior"; // Change from Supervisors to Superior, Desmond 22 Oct 09
									arrN[iN] = totalSup;
									iN++;
									// Change by Santoso 22/10/08
									// the total rater will be printed later below
									// set the valur of total sup here
									totalRater[1] = totalSup;
									break;
									// Commented away to remove CP(Others) to cater for splitting Subordinates and Peers, Desmond 22 Oct 09
									//added back the others for type customization(16 Dec 2009 Qiao Li)
									case 3 : t = "Others";
									arrN[iN] = totalOth;
									iN++;
									// Change by Santoso 22/10/08
									// the total rater will be printed later below
									// set the valur of total others here
									totalRater[2] = totalOth;
									break;	
									case 4 : t = "Self";
									arrN[iN] = totalSelf;
									iN++;
									// Change by Santoso 22/10/08
									// the total rater will be printed later below
									// set the valur of total self here
									totalRater[3] = totalSelf;
									break;

									// Added case 5 and 6 to cater to splitting of Peers and Subordinates by Desmond 21 Oct 09
									case 5 : t = "Subordinates";
									arrN[iN] = totalSub;
									iN++;
									totalRater[4] = totalSub;
									break;
									case 6 : t = "Peers";
									arrN[iN] = totalPeer;
									iN++;
									totalRater[5] = totalPeer;
									break;
									}

									//Should not insert a "\n". Will push col into 2 rows. Printing will go haywire (Maruli)
									//Rating[r] = RTCode + "\n(" + t + ")";

									// Change by Santoso 22/10/08 : Rating order and value already initialized above (also the Result)
									// we need to set the RTCode and the result at the appropriate position as already defined above
									Rating[type-1] = RTCode + "(" + t + ")";

									if(iReportType == 1)
										Rating[type-1] = Rating[type-1].replaceAll("\n", " ");

									Result[type-1] = Double.parseDouble(arr[2]);	;

								}
							}else if(RTCode.equals("CPR") || RTCode.equals("FPR")){
								//Comment off by Ha 02/07/08 should not re-set the r to 0
								//It will print out the KB CP, CPR, FPR incorrecly

								//r = 0;
								// Change by Santoso : 2008-10-19
								// Do we need to check k< result.size? 
								// k refers to index of RatingTask and result refers to the MeanResult
								if(result.size() != 0) { // && k<result.size()) {
									// Change by Santoso : 2008-10-19
									// Wrong value seems to be retrieved (sup's value --> result[2])
									// Retrieve the correct value for All in result[0]
									// String [] arr = (String[])result.elementAt(k);
									String [] arr = (String[])result.elementAt(0);
									hasCPRFPR = true;
									//Should not insert a "\n". Will push col into 2 rows. Printing will go haywire (Maruli)
									//Rating[r] = RTCode + "\n(All)";
									// Change by Santoso 22/10/08 : Rating order and value already initialized above (also the Result)
									// we need to set the RTCode and the result at the appropriate position as already defined above
									//change from 5 to totalType for customization (16 Dec 2009 Qiao Li)
									Rating[totalType] = RTCode + "(All)";  // Changed from Rating[4] to Rating[5] to cater for new catergories Subordinates & Peers, Desmond 22 Oct 09
									if(iReportType == 1)
										Rating[totalType] = Rating[totalType].replaceAll("\n", " "); // Changed from Rating[4] to Rating[5] to cater for new catergories Subordinates & Peers, Desmond 22 Oct 09

									// Change by Santoso (2008-10-19) 
									// Fix CPR(All), we need to find it again (instead of using the current totalAll value)
									//added back the others for type customization(16 Dec 2009 Qiao Li)
									totalOth =totalOth(RTID, compID, KBID); // Commented away to remove CP(Others) to cater for splitting Subordinates and Peers, Desmond 22 Oct 09
									totalSup = totalSup(RTID, compID, KBID);

									// Added to cater to the splitting of peers and subordinates, Desmond 21 Oct 09
									/* Change (s): add in surveyLevel to use different tables 
									 * (tblResultCompetency and tblResultKeyBehavior) to retrieve number of raters
									 * Updated by: Qiao Li 21 Dec 2009
									 */
									totalPeer = totalRater(RTID, compID,KBID, "PEER%", surveyLevel);
									totalSub = totalRater(RTID, compID,KBID, "SUB%", surveyLevel);

									// Re-locate and modified codes to include Peers and Subordinates by Desmond 22 Oct 09							
									totalAll = totalSup + totalPeer + totalSub + totalOth;

									// Changed from Rating[4] to Rating[5] to cater for new catergories Subordinates & Peers, Desmond 22 Oct 09
									//change from 5 to totalType for customization (16 Dec 2009 Qiao Li)
									totalRater[totalType] = totalAll; 
									Result[totalType] = Double.parseDouble(arr[2]);

									// 11/12/2009 Denise
									// Change postion to display CPR/FPR on the top
									//change from 5 to totalType for customization (16 Dec 2009 Qiao Li)
									double CPRFPR_Result = Result[totalType];
									int    CPRFPR_TotalRating = totalRater[totalType];

									for (int x=totalType; x>0; x--)
									{
										totalRater[x] = totalRater[x-1];
										Result[x] = Result[x-1];
									}

									Result[0] = CPRFPR_Result;
									totalRater[0] = CPRFPR_TotalRating;

									Rating[0] = Rating[6];
									Rating[1] = "CP(All)";
									Rating[2] = "CP(Superior)"; 
									//added back the others for type customization(16 Dec 2009 Qiao Li)
									Rating[3] = "CP(Others)";
									Rating[4] = "CP(Self)";
									Rating[5] = "CP(Subordinates)";
									Rating[6] = "CP(Peers)";
								}					
							}

						}//while RT
						//rater type can change depends on whether "Others" is splitted
						//get the appropriate number and prepareCells(Qiao Li 17 Dec 2009)
						totRaterType = Rating.length;
						if (splitOthers== 0){
							totRaterType -= 2;
						}
						else{
							totRaterType -=1;
						}
						// Change by Santoso : Alignment of n number with the bar of the graph 
						// the raters displayed are fixed, no need to calculate rowTotal anymore
						// was : rowTotal = row + 11;

						//rowPos = prepareCells(xSpreadsheet, row, totalRater.length);
						rowPos = prepareCells(xSpreadsheet, row, totRaterType);
						row++;
						// Change by Santoso (2008-10-08)
						// total rater is printed inside drawChart, we need to pass
						// the total rater list and the position to draw
						drawChart(Rating, Result, totalRater, rowPos, maxScale, splitOthers);

						//  12/12/2009 Denise
						// reinitialize Result and totalRater
						//change from 5 to totalType for customization (16 Dec 2009 Qiao Li)
						Result = new double [totalType + totalOtherRT]; // Changed from 4 + totalOtherRT to 5 + totalOtherRT, to clean up later on, DeZ
						totalRater = new int[totalType + totalOtherRT]; 

						column = 9;							
						r1 = row;

						Vector vImportance = Importance(compID, KBID);

						for(int k=0; k<vImportance.size(); k++) {
							String [] arr = (String[]) vImportance.elementAt(k);
							String task = arr[1];
							double taskResult = Double.parseDouble(arr[2]);


							arrN[iN] = totalAll;
							iN++;
							OO.insertString(xSpreadsheet, task + ": " + taskResult, r1, column);
							OO.mergeCells(xSpreadsheet, column, endColumn, r1, r1+1);
							r1 += 3;
						}

						// Change by Santoso 22/10/08
						// only calculate Gap if survey include CPR/FPR Rating task
						if (hasCPRFPR) {
							gap = Gap(compID, KBID);
							gap = Math.round(gap * 100.0) / 100.0;

							if (iNoCPR == 0)	// If CPR is chosen in this survey
							{
								//Changed the default language to English by Chun Yeong 9 Jun 2011
								//Commented away to allow translation below, Chun Yeong 1 Aug 2011
								/*if (ST.LangVer == 2) //Indonesian
										OO.insertString(xSpreadsheet, "Selisih = " + gap, r1, column);	
									else //if (ST.LangVer == 1) English
										OO.insertString(xSpreadsheet, "Gap = " + gap, r1, column);*/

								//Allow dynamic translation, Chun Yeong 1 Aug 2011
								OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Gap") + " = " + gap, r1, column);
								OO.mergeCells(xSpreadsheet, column, endColumn, r1, r1+1);
							}
						}
						r1 += 3;

						LOA = LevelOfAgreement(compID, KBID);
						//Changed the default language to English by Chun Yeong 9 Jun 2011
						//Commented away to allow translation below, Chun Yeong 1 Aug 2011
						/*if (ST.LangVer == 2) //Indonesian
								OO.insertString(xSpreadsheet, "Tingkat Persetujuan: \n" + LOA + "%", r1, column);
							else //if (ST.LangVer == 1) English
								OO.insertString(xSpreadsheet, "Level Of Agreement: \n" + LOA + "%", r1, column);*/ 

						//Allow dynamic translation, Chun Yeong 1 Aug 2011
						OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Level Of Agreement") + ": \n" + LOA + "%", r1, column); 
						OO.mergeCells(xSpreadsheet, column, endColumn, r1, r1+1);																				
						r1 += 3;							

						count++;
						column = 0;
						//added a page break at the end of the chart section of KB Level Survey
						//to solve part of pagination problem (Qiao Li 23 Dec 2009)
						if(count == 2 ||(j == KBList.size()-1 && i == vComp.size()-1)) {
							count = 0;	
							/* Change: standardize the number of rows for the charts to be 15 
							 * Reason: fit 2 charts in one page
							 * Updated by: Qiao Li
							 * Date: 23 Dec 2009
							 */
							row += 15;							
							insertPageBreak(xSpreadsheet, startColumn, endColumn, row);

						} else								
							row += 15;

						endRow = row - 1;

						//comp name and definition						
						OO.setTableBorder(xSpreadsheet, startColumn, endColumn, startRow, startRow+1, 
								false, false, true, true, true, true);
						//total sup n others				
						OO.setTableBorder(xSpreadsheet, startColumn, startColumn, startRow+2, endRow, 
								false, false, true, true, true, true);
						//chart
						OO.setTableBorder(xSpreadsheet, startColumn+1, 8, startRow+2, endRow, 
								false, false, true, true, true, true);	
						OO.setTableBorder(xSpreadsheet, 9, endColumn, startRow+2, endRow, 
								false, false, true, true, true, true);

						OO.setCellAllignment(xSpreadsheet, startColumn, startColumn, startRow+2, endRow, 1, 2);

						///Denise 07/01/2009
						if(count == 0) { //move table two lines down
							OO.insertRows(xSpreadsheet, 0, 10, row+1, row+3, 2, 1);		    			
							row +=2;
						} 
					}//while KBList
					//}	// end if of standard version
				}// while Comp
			}//end of for vClust.size
		}
	} // End InsertClusterCompetency()

	/* Change: added in SurveyID to see whether "Others" is splitted
	 * Updated by: Qiao Li
	 * Date: 17 Dec 2009
	 */
	public void InsertCompetency(int reportType, int SurveyID, int surveyIDImpt) throws SQLException, Exception {

		int iN = 0; //To be used as counter for arrN

		//  System.out.println("7. Competencies Starts");

		int [] address = OO.findString(xSpreadsheet, "<Report>");

		column = address[0];
		row = address[1];

		OO.findAndReplace(xSpreadsheet, "<Report>", "");

		int surveyLevel = Integer.parseInt(surveyInfo[0]);

		//Commented away to allow translation below, Chun Yeong 1 Aug 2011
		//String level 	= "Competency";
		//if (ST.LangVer == 2) 
		//	level = "Kompetensi";

		// Change by Santoso (22/10/08)
		// store the result of totalOtherRT to be reused below
		int totalOtherRT = totalOtherRT();
		int total = totalGroup() + totalOtherRT + 1;		// 1 for all
		//added in a variable to store number of types we have (16 Dec 2009 Qiao Li)
		int totalType = 6;
		/* Change: retrieve splitOthersOption of this survey 
		 * Reason: see whether "Others" is splitted
		 * Updated by: Qiao Li
		 * Date: 17 Dec 2009
		 */

		// int rowTotal 	= row + 1;
		// Changed by Ha 27/05/08: size of array from total --> total + 1
		// String [] Rating = new String [total+1];
		// double [] Result = new double [total+1];
		// Change by Santoso 22/10/08
		// Rating & Result now fixed to 4 + totalOtherRT
		// i.e. CP(All), CP(Sups), CP(Oth), CP(Self) and/or CPR(All)/FPR(All)
		// we can hardcode the string here anyway, since it is also hardcoded belows

		//used variable totalType to determine the initialization of array size (16 Dec 2009 Qiao Li) 
		String [] Rating = new String [totalType + totalOtherRT]; // Changed from 4 + totalOtherRT to 5 + totalOtherRT, to clean up later on, DeZ
		double [] Result = new double [totalType + totalOtherRT]; // Changed from 4 + totalOtherRT to 5 + totalOtherRT, to clean up later on, DeZ
		int[] totalRater = new int[totalType + totalOtherRT]; // Changed from 4 + totalOtherRT to 5 + totalOtherRT, to clean up later on, DeZ

		// initialize Rating --> CP only, will be replace later on in this method if scores exists for that row, added comments here, Desmond 22 Oct 09
		Rating[0] = "CP(All)";
		Rating[1] = "CP(Superior)"; // Change from Supervisors to Superior, Desmond 22 Oct 09
		//added back the others for type customization(16 Dec 2009 Qiao Li)
		Rating[2] = "CP(Others)"; // Commented away to remove CP(Others) to cater for splitting Subordinates and Peers, Desmond 22 Oct 09
		Rating[3] = "CP(Self)";
		// Add additional categories for bar graph to cater for splitting of Subordinates and Peers, Desmond 21 Oct 09
		Rating[4] = "CP(Subordinates)";
		Rating[5] = "CP(Peers)";

		int maxScale = MaxScale();

		int count = 0; // to count total chart for each page, max = 2;
		int r1 = 1;
		int add = 13/total;
		//added back the others for type customization(16 Dec 2009 Qiao Li)
		int totalOth = 0; // Commented away to remove CP(Others) to cater for splitting Subordinates and Peers, Desmond 22 Oct 09
		int totalSup = 0;				
		int totalSelf = 0;
		int totalAll = 0;

		// Added new variables in order to cater for splitting of Subordinates and Peers, Desmond 21 Oct 2009
		int totalPeer = 0;
		int totalSub = 0;

		Vector vComp = CompetencyByName();

		insertPageBreak(xSpreadsheet, startColumn, endColumn, row);

		//Commented away to allow translation below, Chun Yeong 1 Aug 2011
		//if (ST.LangVer == 2) //Indonesian
		//	OO.insertString(xSpreadsheet, "Laporan Kompetensi", row, 0);
		//else //if (ST.LangVer == 1) //English
		//	OO.insertString(xSpreadsheet, "Competency Report", row, 0);

		//Allow dynamic translation, Chun Yeong 1 Aug 2011
		OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Competency Report"), row, 0);
		OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);

		row += 2;
		//since this is for MOE survey, it will go to KB Level. This part will be left behind.
		if(surveyLevel == 0) { 		// Competency Level Survey

			System.out.println("InsertCompetency() - Survey is at Competency Level"); // To Remove, Dez
			/* Change:adjust the row height immediately after "Competency Report" to fit two diagrams in a graph
			 * Updated by: Qiao Li 23 Dec 2009*/
			//		OO.setRowHeight(xSpreadsheet, row-1, startColumn, 200); //Denise 08/01/2009 no need to reduce rowheight since we use A4 size now

			int endRow = row; 
			//Added by Alvis 06-Aug-09 for pagebreak fix implemented below
			int currentPageHeight = 1076;//starting page height is set to 1076 to include the title of the competency report.

			for(int i=0; i<vComp.size(); i++) {

				voCompetency voComp = (voCompetency)vComp.elementAt(i);

				int compID 		= voComp.getCompetencyID();
				String statement = voComp.getCompetencyName();
				String desc = voComp.getCompetencyDefinition();

				int startRow = row;	// for border
				int RTID = 0;			
				int KBID = 0;

				int statementPos = row;
				//				OO.insertString(xSpreadsheet, UnicodeHelper.getUnicodeStringAmp(statement), row, 0);
				//				OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
				//				OO.setBGColor(xSpreadsheet, startColumn, endColumn, row, row, BGCOLOR);							
				row++;

				r1 = row; 
				//Added tranlsation for the competency definition, Chun Yeong 1 Aug 2011
				OO.insertString(xSpreadsheet, getTranslatedCompetency(statement).elementAt(1).toString(), row, 0);
				OO.mergeCells(xSpreadsheet, startColumn, endColumn, row, row);
				//OO.mergeCells(xSpreadsheet, startColumn, endColumn, row, row);
				//adjust the merged cell as top alignment (Qiao Li 21 Dec 2009 )
				OO.setCellAllignment(xSpreadsheet, startColumn, endColumn, row, row, 2, 1);
				OO.setRowHeight(xSpreadsheet, row, 1, ROWHEIGHT*OO.countTotalRow(desc, 90));
				row++;

				String RTCode = "";

				Vector RT = RatingTask();
				int r = 0;


				boolean hasCPRFPR = false;
				for(int j=0; j<RT.size(); j++) {
					votblSurveyRating vo = (votblSurveyRating)RT.elementAt(j);

					RTID = vo.getRatingTaskID();
					RTCode 	=  vo.getRatingCode();


					Vector result = MeanResult(RTID, compID, KBID);


					if(RTCode.equals("CP")) 
					{
						//Changed by Ha 09/07/08 to calculate total rater for each competency for each rating task
						//added back the others for type customization(16 Dec 2009 Qiao Li)
						totalOth = totalOth(1,compID); // Commented away to remove CP(Others) to cater for splitting Subordinates and Peers, Desmond 22 Oct 09
						totalSup = totalSup(1, compID);						
						totalSelf = totalSelf(1, compID);

						// Added to cater to the splitting of peers and subordinates, Desmond 21 Oct 09
						/* Change (s): add in surveyLevel to use different tables 
						 * (tblResultCompetency and tblResultKeyBehavior) to retrieve number of raters
						 * Updated by: Qiao Li 21 Dec 2009
						 */
						totalPeer = totalRater(RTID, compID, -1, "PEER%", surveyLevel);
						totalSub = totalRater(RTID, compID, -1, "SUB%", surveyLevel);

						// Re-locate and modified codes to include Peers and Subordinates by Desmond 21 Oct 09
						totalAll = totalSup + totalPeer + totalSub + totalOth;

						for(int k=0; k<result.size(); k++) 
						{							
							String [] arrOther = (String[])result.elementAt(k);

							int type = Integer.parseInt(arrOther[1]);
							String t = "";
							switch(type) 
							{
							case 1 : t = "All";

							arrN[iN] = totalAll;
							iN++;
							// Change by Santoso 22/10/08
							// the total rater will be printed later below
							// set the valur of total all here
							totalRater[0] = totalAll;
							break;
							case 2 : t = "Superior"; // Change from Supervisors to Superior, Desmond 22 Oct 09
							arrN[iN] = totalSup;
							iN++;
							// Change by Santoso 22/10/08
							// the total rater will be printed later below
							// set the valur of total sup here
							totalRater[1] = totalSup;
							break;
							//added back the others for type customization(16 Dec 2009 Qiao Li)
							case 3 : t = "Others";
							arrN[iN] = totalOth;
							iN++;
							// Change by Santoso 22/10/08
							// the total rater will be printed later below
							// set the valur of total others here
							totalRater[2] = totalOth;
							break;
							case 4 : t = "Self";
							arrN[iN] = totalSelf;
							iN++;
							// Change by Santoso 22/10/08
							// the total rater will be printed later below
							// set the valur of total self here
							totalRater[3] = totalSelf;
							break;

							// Added case 5 and 6 to cater to splitting of Peers and Subordinates by Desmond 21 Oct 09
							case 5 : t = "Subordinates";
							arrN[iN] = totalSub;
							iN++;
							totalRater[4] = totalSub;
							break;
							case 6 : t = "Peers";
							arrN[iN] = totalPeer;
							iN++;
							totalRater[5] = totalPeer;
							break;
							}

							//Should not insert a "\n". Will push col into 2 rows. Printing will go haywire (Maruli)
							//Rating[r] = RTCode + "\n(" + t + ")";

							// Change by Santoso 22/10/08
							// Since we have fixed the order of rating (according to the type)
							// we can set the Rating text here using type-1 as the index
							Rating[type-1] = RTCode + "(" + t + ")";
							if(iReportType == 1)
								Rating[type-1] = Rating[r].replaceAll("\n", " ");

							Result[type-1] = Double.parseDouble(arrOther[2]);

							//If i don't split..means = others
							//Result[2] = value;
						}
					}
					else if(RTCode.equals("CPR") || RTCode.equals("FPR"))
					{	
						//Changed by Ha 26/06/08 should not have j < result.size in the condition
						//Problem with old condition: value were not displayed correctly
						if (result.size()  > 0)
						{						

							// Change by Santoso 22/10/08
							// hasCPRFPR will be needed later, we need to set the value to true here
							hasCPRFPR = true;
							String [] arrOther = (String[])result.elementAt(0);

							//Should not insert a "\n". Will push col into 2 rows. Printing will go haywire (Maruli)
							//Rating[r] = RTCode + "\n(All)";

							/*
							 * Change(s) : Change to get the correct number of raters for CPR when there is a split
							 * Updated By: Mark Oei 1 Mar 2010
							 * Previous Updates:
							 * - Change by Santoso 22/10/08 : Rating order and value already initialized above (also the Result)
							 * we need to set the RTCode and the result at the appropriate position as already defined above
							 * - Change from 5 to totalType for customization (16 Dec 2009 Qiao Li)
							 */
							totalOth = totalRater(2, compID, -1, "OTH%", surveyLevel); 
							totalSup = totalRater(2, compID, -1, "SUP%", surveyLevel);

							totalPeer = totalRater(2, compID, -1, "PEER%", surveyLevel);
							totalSub = totalRater(2, compID, -1, "SUB%", surveyLevel);

							Rating[totalType] = RTCode + "(All)"; // Changed from Rating[4] to Rating[5] to cater for new catergories Subordinates & Peers, Desmond 22 Oct 09 

							if(iReportType == 1)
								Rating[totalType] = Rating[totalType].replaceAll("\n", " ");  // Changed from Rating[4] to Rating[5] to cater for new catergories Subordinates & Peers, Desmond 22 Oct 09

							Result[totalType] = Double.parseDouble(arrOther[2]);   // Changed from Rating[4] to Rating[5] to cater for new catergories Subordinates & Peers, Desmond 22 Oct 09

							if (RTCode.equals("CPR")){
								//Change to get correct number of raters for CPR when 
								//  there is a split, Mark Oei 01 Mar 2010
								if (splitOthers == 0)
									totalAll = totalSup(2,compID) + totalOth(2,compID);
								else
									totalAll = totalSup(2,compID) + totalPeer + totalSub;
							}
							else if (RTCode.equals("FPR"))
								totalAll = totalSup(3,compID) + totalOth(3,compID);

							arrN[iN] = totalAll;
							iN++;


							// Change by Santoso 22/10/08
							// the total rater will be printed later below
							// set the valur of total all here
							totalRater[totalType] = totalAll;  // Changed from totalRater[4] to totalRater[5] to cater for new catergories Subordinates & Peers, Desmond 22 Oct 09	


							// 11/12/2009 Denise
							// Change position to display CPR/FPR on the top 
							//change from 5 to totalType for customization (16 Dec 2009 Qiao Li)
							double CPRFPR_Result = Result[totalType];
							int    CPRFPR_TotalRating = totalRater[totalType];

							//change from 5 to totalType for customization (16 Dec 2009 Qiao Li)
							for (int x=totalType; x>0; x--)
							{
								totalRater[x] = totalRater[x-1];
								Result[x] = Result[x-1];
							}

							Result[0] = CPRFPR_Result;
							totalRater[0] = CPRFPR_TotalRating;
							//change from 5 to totalType for customization (17 Dec 2009 Qiao Li)
							Rating[0] = Rating[totalType];
							Rating[1] = "CP(All)";
							Rating[2] = "CP(Superior)";
							//added back the others for type customization(16 Dec 2009 Qiao Li)
							//Change CP(Other) to CP(Others) in order for the competency bar graph 
							//  to be displayed for split with CPR, 01 Mar 2010 Mark Oei 
							Rating[3] = "CP(Others)";
							Rating[4] = "CP(Self)";
							Rating[5] = "CP(Subordinates)";
							Rating[6] = "CP(Peers)";
						}
					}
				}
				//rater type can change depends on whether "Others" is splitted
				//get the appropriate number and prepareCells(Qiao Li 17 Dec 2009)
				int totRaterType = Rating.length;
				if (splitOthers== 0){
					totRaterType -= 2;
				}
				else{
					totRaterType -=1;
				}
				//while RT
				// Change by Santoso 22/10/08
				// Alignment of n number with the bar of the graph 
				// was : rowTotal = row + 11;
				//int[] rowPos = prepareCells(xSpreadsheet, row, totalRater.length);
				int[] rowPos = prepareCells(xSpreadsheet, row, totRaterType);
				row++;		//start draw chart from here
				OO.setFontSize(12);

				// Change by Santoso 22/10/08
				// the total rater will be printed in drawChart therefore we need
				// to pass the rating point and the position
				drawChart(Rating, Result, totalRater, rowPos, maxScale, splitOthers);

				//  12/12/2009 Denise
				// reinitialize Result and totalRater
				//change from 5 to totalType for customization (16 Dec 2009 Qiao Li)
				Result = new double [totalType + totalOtherRT]; 
				totalRater = new int[totalType + totalOtherRT]; 

				column = 9;		//write the importance n gap
				int rtemp = row;

				Vector vImportance = Importance(compID, KBID);

				for(int j=0; j<vImportance.size(); j++) {
					String [] arr = (String[]) vImportance.elementAt(j);
					String task = arr[1];
					double taskResult = Double.parseDouble(arr[2]);

					OO.insertString(xSpreadsheet, task + ": " + taskResult, rtemp, column);
					OO.mergeCells(xSpreadsheet, column, endColumn, rtemp, rtemp+1);
					OO.setCellAllignment(xSpreadsheet, column, endColumn, rtemp, rtemp+1, 2, 1);

					rtemp += 3;
				}

				double gap = 0;

				// Change by Santoso 22/10/08
				// only calculate Gap if survey include CPR/FPR Rating task
				if (hasCPRFPR) {
					gap = getAvgGap(compID);
					// If CPR is chosen in this survey
					{
						//Changed the default language to English by Chun Yeong 9 Jun 2011
						//Commented away to allow translation below, Chun Yeong 1 Aug 2011
						/*if (ST.LangVer == 2) //Indonesian
							OO.insertString(xSpreadsheet, "Selisih = " + gap, rtemp, column);
						else //if (ST.LangVer == 1) English
							OO.insertString(xSpreadsheet, "Gap = " + gap, rtemp, column);*/

						//Allow dynamic translation, Chun Yeong 1 Aug 2011
						OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Gap") + " = " + gap, rtemp, column);
						OO.mergeCells(xSpreadsheet, column, endColumn, rtemp, rtemp+1);

						OO.setCellAllignment(xSpreadsheet, column, endColumn, rtemp, rtemp+1, 2, 1);	
					}				
				}
				rtemp+=3;

				double LOA = LevelOfAgreement(compID, KBID);
				//Changed the default language to English by Chun Yeong 9 Jun 2011
				//Commented away to allow translation below, Chun Yeong 1 Aug 2011
				/*if (ST.LangVer == 2) //Indonesian
					OO.insertString(xSpreadsheet, "Tingkat Persetujuan: \n" + LOA + "%", rtemp, column);
				else //if (ST.LangVer == 1) English
					OO.insertString(xSpreadsheet, "Level Of Agreement: \n" + LOA + "%", rtemp, column);*/

				//Allow dynamic translation, Chun Yeong 1 Aug 2011
				OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Level Of Agreement")+": \n" + LOA + "%", rtemp, column);
				OO.mergeCells(xSpreadsheet, column, endColumn, rtemp, rtemp+1);
				OO.setCellAllignment(xSpreadsheet, column, endColumn, rtemp, rtemp+1, 2, 1);

				column = 0;
				count++;

				//Removed by Alvis for Pagebreaks problem
				//if(count == 2) {
				//count = 0;
				//	row += 17;					
				//	insertPageBreak(xSpreadsheet, startColumn, endColumn, row);
				//} else {
				//	column = 0;					
				//	row += 16;						
				//}
				//End of removal section by Alvis for Pagebreaks problem
				/* Change: standardize the number of rows for the charts to be 15 
				 * Reason: fit 2 charts in one page
				 * Updated by: Qiao Li
				 * Date: 23 Dec 2009
				 */
				row += 15;
				endRow = row-1;

				/*
				 * Change(s) : Added codes to check height of the table to be
				 * 			  added, and insert pagebreak if necessary
				 * Reason(s) : Fix the problem of a table being spilt into between 
				 * 			  two pages. 
				 * Updated By: Alvis 
				 * Updated On: 06 Aug 2009
				 */

				//Check height and insert pagebreak where necessary
				int pageHeightLimit = 22272	;//Page limit is 22272						    
				int tableHeight = 0;

				//calculate the height of the table that is being dded.
				for(int i1=startRow+1;i1<=endRow+1;i1++){
					int rowToCalculate = i1;
					tableHeight += OO.getRowHeight(xSpreadsheet, rowToCalculate, startColumn);

				}

				currentPageHeight = currentPageHeight + tableHeight; //add new table height to current pageheight.
				int dis = 2; //Denise 08/01/2009 to move the table two lines down
				if(currentPageHeight >pageHeightLimit){//adding the table will exceed a single page
					OO.insertRows(xSpreadsheet, startColumn, endColumn, startRow, startRow+dis, dis, 1);				
					insertPageBreak(xSpreadsheet, startColumn, endColumn, startRow);
					statementPos +=dis ;
					row +=dis;
					startRow +=dis;
					endRow +=dis;
					currentPageHeight = tableHeight;
				}
				//Denise 08/01/2009 insert competency statement
				//Added translation for the competency name, Chun Yeong 1 Aug 2011
				OO.insertString(xSpreadsheet, UnicodeHelper.getUnicodeStringAmp(getTranslatedCompetency(statement).elementAt(0).toString()), statementPos, 0);
				OO.setFontBold(xSpreadsheet, startColumn, endColumn, statementPos, statementPos);
				OO.setBGColor(xSpreadsheet, startColumn, endColumn, statementPos, statementPos, BGCOLOR);		
				//comp name and definition
				OO.setTableBorder(xSpreadsheet, startColumn, endColumn, startRow, startRow+1, 
						false, false, true, true, true, true);

				//total sup n others

				OO.setTableBorder(xSpreadsheet, startColumn, startColumn, startRow+2, endRow, 
						false, false, true, true, true, true);

				//chart

				OO.setTableBorder(xSpreadsheet, startColumn+1, 8, startRow+2, endRow, 
						false, false, true, true, true, true);
				OO.setTableBorder(xSpreadsheet, 9, endColumn, startRow+2, endRow, 
						false, false, true, true, true, true);
				OO.setCellAllignment(xSpreadsheet, startColumn, startColumn, startRow+2, endRow, 1, 2);
				OO.setCellAllignment(xSpreadsheet, startColumn, startColumn, startRow+2, endRow, 1, 2);

				//added by Alvis on 07-Aug-09 to ensure next section begin on a new page.
				if(i==(vComp.size()-1)){//last table added
					//insertpagebreak
					insertPageBreak(xSpreadsheet, startColumn, endColumn, endRow+1);
				}

				//	if (breakpage)
				//	{
				//		OO.insertRows(xSpreadsheet, startColumn, endColumn, startRow, startRow+1, 1, 0);
				//		row++;
				//	}
			}// while Comp

			// End Competency Level

		} else {
			//Start KB level
			System.out.println("InsertCompetency() - Survey is at KB Level"); // To Remove, Dez
			int start = 0;
			int startRow = row;	// for border
			int endRow = row;						
			for(int i=0; i<vComp.size(); i++)
			{					

				// Add by Santoso (22/10/08) : reinitialize array per loop
				// reinitialize the array each loop (otherwise it will use the previous value)
				totalRater = new int[totalRater.length];
				Result = new double[Result.length];


				// Change by Santoso (22/10/08)
				// No need to calculate rowTotal anymore since the raters displayed on the graph are fixed
				//rowTotal = row + 1;
				start = 0;
				int RTID = 0;

				int KBID = 0;
				String KB = "";

				voCompetency voComp = (voCompetency)vComp.elementAt(i);

				int compID = voComp.getCompetencyID();

				String statement = voComp.getCompetencyName();
				String desc = voComp.getCompetencyDefinition();

				startRow = row;

				//Added translation for competency name, Chun Yeong 1 Aug 2011
				OO.insertString(xSpreadsheet, getTranslatedCompetency(UnicodeHelper.getUnicodeStringAmp(statement)).elementAt(0).toString(), row, column);
				OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
				OO.setBGColor(xSpreadsheet, startColumn, endColumn, row, row, BGCOLOR);			
				row++;

				r1 = row;
				OO.insertString(xSpreadsheet, UnicodeHelper.getUnicodeStringAmp(desc), row, column);
				OO.mergeCells(xSpreadsheet, startColumn, endColumn, row, row);
				//adjust the merged cell as top alignment (Qiao Li 21 Dec 2009 )
				OO.setCellAllignment(xSpreadsheet, startColumn, endColumn, row, row, 2, 1);
				OO.setRowHeight(xSpreadsheet, row, 1, ROWHEIGHT*OO.countTotalRow(desc, 90));

				row++;
				start++;

				String RTCode = "";

				Vector RT = RatingTask();


				boolean hasCPRFPR = false;
				//Print out the "N" in other section for simplified report

				for(int j=0; j<RT.size(); j++) {
					votblSurveyRating vo = (votblSurveyRating)RT.elementAt(j);

					RTID = vo.getRatingTaskID();
					RTCode 	=  vo.getRatingCode();

					Vector result = null;

					if(RTCode.equals("CP")) {
						result = KBMean(RTID, compID);

						// Change by Santoso 2008/10/29
						// use a new query for retrieving total rater
						//added back the others for type customization(16 Dec 2009 Qiao Li)
						/* Change (s): add in surveyLevel to use different tables 
						 * (tblResultCompetency and tblResultKeyBehavior) to retrieve number of raters
						 * Updated by: Qiao Li 21 Dec 2009
						 */
						totalOth = totalRater(RTID, compID, -1, "OTH%", surveyLevel); // Commented away to remove CP(Others) to cater for splitting Subordinates and Peers, Desmond 22 Oct 09
						totalSup = totalRater(RTID, compID,-1, "SUP%", surveyLevel);
						totalSelf = totalRater(RTID, compID, -1, "SELF", surveyLevel);

						// Added to cater to the splitting of peers and subordinates, Desmond 21 Oct 09
						totalPeer = totalRater(RTID, compID, -1, "PEER%", surveyLevel);
						totalSub = totalRater(RTID, compID, -1,"SUB%", surveyLevel);

						// Re-locate and modified codes to include Peers and Subordinates by Desmond 22 Oct 09							
						totalAll = totalSup + totalPeer + totalSub + totalOth;
						System.out.println("totalSup(" + totalSup + ") + totalPeer(" + totalPeer + ") + totalSub(" + totalSub + ") + totalOth(" + totalOth + ") = totalAll(" + totalAll+")"); // To Remove, Desmond 17 Nov 09
						for(int k=0; k<result.size(); k++) {

							String [] arr = (String[])result.elementAt(k);

							// Updated adjustments to type in order to cater for splitting of subordinates & peers, Desmond 28 Oct 09
							int type = Integer.parseInt(arr[1]);
							//remove the hack, use back the type such that results for others are 
							//also retrieved (16 Dec 2009 Qiao Li)
							//if(type > 3) type = type -1; // Adjust values > 3 because we removed Others from the list

							String t = "";
							switch(type) 
							{
							case 1 : t = "All";

							arrN[iN] = totalAll;
							iN++;
							// Change by Santoso 22/10/08
							// the total rater will be printed later below
							// set the valur of total all here
							totalRater[0] = totalAll;
							break;
							case 2 : t = "Superior"; // Change from Supervisors to Superior, Desmond 22 Oct 09
							arrN[iN] = totalSup;
							iN++;
							// Change by Santoso 22/10/08
							// the total rater will be printed later below
							// set the valur of total sup here
							totalRater[1] = totalSup;
							break;
							//added back the others for type customization(16 Dec 2009 Qiao Li)
							case 3 : t = "Others";
							arrN[iN] = totalOth;
							iN++;
							// Change by Santoso 22/10/08
							// the total rater will be printed later below
							// set the valur of total others here
							totalRater[2] = totalOth;
							break;		 	
							case 4 : t = "Self";
							arrN[iN] = totalSelf;
							iN++;
							// Change by Santoso 22/10/08
							// the total rater will be printed later below
							// set the valur of total self here
							totalRater[3] = totalSelf;
							break;
							// Commented away to remove CP(Others) to cater for splitting Subordinates and Peers, Desmond 22 Oct 09										 
							// Added case 5 and 6 to cater to splitting of Peers and Subordinates by Desmond 21 Oct 09
							case 5 : t = "Subordinates";
							arrN[iN] = totalSub;
							iN++;
							totalRater[4] = totalSub;
							break;
							case 6 : t = "Peers";
							arrN[iN] = totalPeer;
							iN++;
							totalRater[5] = totalPeer;
							break;
							}
							//Should not insert a "\n". Will push col into 2 rows. Printing will go haywire (Maruli)
							//Rating[r] = RTCode + "\n(" + t + ")";
							// Change by Santoso 22/10/08 : Rating order and value already initialized above (also the Result)
							// we need to set the RTCode and the result at the appropriate position as already defined above
							Rating[type-1] = RTCode + "(" + t + ")";
							if(iReportType == 1)
								Rating[type-1] = Rating[type-1].replaceAll("\n", " ");

							System.out.print("InsertCompetency() - Rating" + "[" + (type-1) + "]" + " = " + Rating[type-1]); // To Remove, Desmond

							if(type == 1)
								Result[type-1] = CompTrimmedMeanforAll(RTID, compID);
							else
								Result[type-1] = Double.parseDouble(arr[2]);

							//System.out.println(", Result" + "[" + (type-1) + "]" + " = " + Result[type-1]); // To Remove, Desmond
						}
					}else if(RTCode.equals("CPR") || RTCode.equals("FPR")){
						// Change by Santoso 22/10/08
						// need to keep track whether CPR/FPR is included in the survey or not to keep Gap from printed if no CPR/FPR
						hasCPRFPR = true;
						// Change by Santoso 2008/10/29 
						// use a new query for retrieving total rater
						//added back the others for type customization(16 Dec 2009 Qiao Li)
						/* Change (s): add in surveyLevel to use different tables 
						 * (tblResultCompetency and tblResultKeyBehavior) to retrieve number of raters
						 * Updated by: Qiao Li 21 Dec 2009
						 */
						totalOth = totalRater(RTID, compID,-1, "OTH%", surveyLevel); // Commented away to remove CP(Others) to cater for splitting Subordinates and Peers, Desmond 22 Oct 09
						totalSup = totalRater(RTID, compID, -1, "SUP%", surveyLevel);

						// Added to cater to the splitting of peers and subordinates, Desmond 21 Oct 09
						totalPeer = totalRater(RTID, compID,-1, "PEER%", surveyLevel);
						totalSub = totalRater(RTID, compID,-1, "SUB%", surveyLevel);

						// Re-locate and modified codes to include Peers and Subordinates by Desmond 22 Oct 09							
						totalAll = totalSup + totalPeer + totalSub + totalOth;

						arrN[iN] = totalAll;
						iN++;
						//Should not insert a "\n". Will push col into 2 rows. Printing will go haywire (Maruli)
						//Rating[r] = RTCode + "\n(All)";
						// Change by Santoso 22/10/08 : Rating order and value already initialized above (also the Result)
						// we need to set the RTCode and the result at the appropriate position as already defined above
						//change from 5 to totalType for customization (16 Dec 2009 Qiao Li)
						Rating[totalType] = RTCode + "(All)";
						if(iReportType == 1) {
							System.out.println("insertCompetency() - iReportType = 1 so replace all \n"); // To Remove, Desmond
							Rating[totalType] = Rating[totalType].replaceAll("\n", " "); // Changed from Rating[4] to Rating[5] to cater for new catergories Subordinates & Peers, Desmond 22 Oct 09
						}

						totalRater[totalType] = totalAll; // Changed from Rating[4] to Rating[6] to cater for new catergories Subordinates & Peers, Desmond 22 Oct 09
						Result[totalType] = CompTrimmedMeanforAll(RTID, compID); // Changed from Rating[4] to Rating[5] to cater for new catergories Subordinates & Peers, Desmond 22 Oct 09
						System.out.println("insertCompetency() - CPR Score - Result" + "[5]" + " = " + Result[5]); // To Remove, Desmond

						// 11/12/2009 Denise
						// Change position to display CPR/FPR on the top 
						double CPRFPR_Result = Result[totalType];
						int    CPRFPR_TotalRating = totalRater[totalType];
						//change from x=5 to totalType for customization (16 Dec 2009 Qiao Li)
						for (int x=totalType; x>0; x--)
						{
							totalRater[x] = totalRater[x-1];
							Result[x] = Result[x-1];
						}

						Result[0] = CPRFPR_Result;
						totalRater[0] = CPRFPR_TotalRating;

						Rating[0] = Rating[totalType];
						Rating[1] = "CP(All)";
						Rating[2] = "CP(Superior)"; 
						//added back the others for type customization(16 Dec 2009 Qiao Li)
						Rating[3] = "CP(Others)";
						Rating[4] = "CP(Self)";
						Rating[5] = "CP(Subordinates)";
						Rating[6] = "CP(Peers)";
					}				

				}//while RT
				//rater type can change depends on whether "Others" is splitted
				//get the appropriate number and prepareCells(Qiao Li 17 Dec 2009)
				int totRaterType = Rating.length;
				if (splitOthers== 0){
					totRaterType -= 2;
				}
				else{
					totRaterType -=1;
				}
				// Change by Santoso : Alignment of n number with the bar of the graph
				// rowTotal is no longer needed, the raters displayed are fixed
				// was : rowTotal = row + 11;
				//int[] rowPos = prepareCells(xSpreadsheet, row, totalRater.length);
				int[] rowPos = prepareCells(xSpreadsheet, row, totRaterType);
				row++;
				// Change by Santoso (2008-10-08)
				// total rater is printed inside drawChart, we need to pass
				// the total rater list and the position to draw
				drawChart(Rating, Result, totalRater, rowPos, maxScale, splitOthers);						

				//  12/12/2009 Denise
				// reinitialize Result and totalRater
				//change from 5 to totalType for customization (16 Dec 2009 Qiao Li)
				Result = new double [totalType + totalOtherRT]; // Changed from 4 + totalOtherRT to 5 + totalOtherRT, to clean up later on, DeZ
				totalRater = new int[totalType + totalOtherRT]; 

				column = 9;
				r1 = row;

				Vector Importance = AvgImportance(compID);

				for(int j=0; j<Importance.size(); j++) {
					String [] arr = (String[])Importance.elementAt(j);

					String task = arr[1];
					double taskResult = Double.parseDouble(arr[2]);

					OO.insertString(xSpreadsheet, task + ": " + taskResult, r1, column);
					OO.mergeCells(xSpreadsheet, column, endColumn, r1, r1+1);														
					r1 += 3;
				}

				double gap = 0;
				// Change by Santoso 22/10/08
				// only calculate Gap if survey include CPR/FPR Rating task
				if (hasCPRFPR) {
					int element = vCompID.indexOf(new Integer(compID));
					gap = Double.valueOf(((String [])vGapUnsorted.elementAt(element))[1]).doubleValue();
					//System.out.println(gap + "----" + compID + " --- " + element);
					if (iNoCPR == 0)	// If CPR is chosen in this survey
					{
						//Changed the default language to English by Chun Yeong 9 Jun 2011
						//Commented away to allow translation below, Chun Yeong 1 Aug 2011
						/*if (ST.LangVer == 2) //Indonesian
								OO.insertString(xSpreadsheet, "Selisih = " + gap, r1, column);	
							else // if (ST.LangVer == 1) English
								OO.insertString(xSpreadsheet, "Gap = " + gap, r1, column);*/

						//Allow dynamic translation, Chun Yeong 1 Aug 2011
						OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Gap") + " = " + gap, r1, column);
						OO.mergeCells(xSpreadsheet, column, endColumn, r1, r1+1);
					}
				}
				r1 += 3;

				double LOA = AvgLevelOfAgreement(compID, totalAll);
				//System.out.println(LOA + "----" + compID );

				//Changed the default language to English by Chun Yeong 9 Jun 2011
				//Commented away to allow translation below, Chun Yeong 1 Aug 2011
				/*if (ST.LangVer == 2) //Indonesian
						OO.insertString(xSpreadsheet, "Tingkat Persetujuan: \n" + LOA + "%", r1, column);
					else //if (ST.LangVer == 1) //English
						OO.insertString(xSpreadsheet, "Level Of Agreement: \n" + LOA + "%", r1, column);*/

				//Allow dynamic translation, Chun Yeong 1 Aug 2011
				OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Level Of Agreement") + ": \n" + LOA + "%", r1, column);	
				OO.mergeCells(xSpreadsheet, column, endColumn, r1, r1+2);									
				r1 += 4;

				OO.insertString(xSpreadsheet, "IMPT:", r1, column);
				r1++;
				Vector ImportanceImpt = ImportanceMOE(compID, surveyIDImpt);
				for(int j=0; j<ImportanceImpt.size(); j++) {
					String [] arr = (String[])ImportanceImpt.elementAt(j);
					String ratingCode = "";
					double taskResult = Double.parseDouble(arr[2]);
					int type = Integer.parseInt(arr[3]);
					if (type==2){
						ratingCode = "Superior";
					} else if(type==4){
						ratingCode = "Self";
					} else if(type==1){
						ratingCode = "All";
						continue; //since not used in MOE Importance Report
					}
					OO.insertString(xSpreadsheet, ratingCode + ": " + taskResult, r1, column);
					OO.mergeCells(xSpreadsheet, column, column+2, r1, r1);
					r1 += 1;
				}

				count++;
				column = 0;
				if(count == 2) {
					count = 0;

					row += 15;						
					insertPageBreak(xSpreadsheet, startColumn, endColumn, row);
				} else {					
					row += 15;						
				}

				endRow = row-1;

				//comp name and definition
				OO.setTableBorder(xSpreadsheet, startColumn, endColumn, startRow, startRow+1, 
						false, false, true, true, true, true);

				//total sup n others				
				OO.setTableBorder(xSpreadsheet, startColumn, startColumn, startRow+2, endRow, 
						false, false, true, true, true, true);
				//chart
				OO.setTableBorder(xSpreadsheet, startColumn+1, 8, startRow+2, endRow, 
						false, false, true, true, true, true);	
				OO.setTableBorder(xSpreadsheet, 9, endColumn, startRow+2, endRow, 
						false, false, true, true, true, true);

				OO.setCellAllignment(xSpreadsheet, startColumn, startColumn, startRow+2, endRow, 1, 2);


				// KB LEVEL //				

				//(30-Sep-05) Rianto: Replaced with Simplified report with no competencies & KB charts
				//if(reportType == 2) {			// only if standard report, simplified report no need for KB

				Vector KBList = KBList(compID);	

				for(int j=0; j<KBList.size(); j++) {
					voKeyBehaviour voKB = (voKeyBehaviour)KBList.elementAt(j);
					KBID = voKB.getKeyBehaviourID();
					KB = voKB.getKeyBehaviour();

					startRow = row;
					r1 = row;

					//Added translation for the key behaviour, Chun Yeong 1 Aug 2011
					OO.insertString(xSpreadsheet, start + ". " + getTranslatedKeyBehavior(UnicodeHelper.getUnicodeStringAmp(KB)), row, 0);
					OO.mergeCells(xSpreadsheet, startColumn, endColumn, row, row);
					//adjust the merged cell as top alignment (Qiao Li 21 Dec 2009 )
					OO.setCellAllignment(xSpreadsheet, startColumn, endColumn, row, row, 2, 1);
					OO.setRowHeight(xSpreadsheet, row, 0, ROWHEIGHT*OO.countTotalRow(KB, 90));

					row += 2;
					//row ++;
					start++;

					//change from 5 to totalType for customization (16 Dec 2009 Qiao Li)
					totalRater = new int[totalType + totalOtherRT]; // Changed from 4 + totalOtherRT to 5 + totalOtherRT, to clean up later on, DeZ
					Result = new double [totalType + totalOtherRT]; // Changed from 4 + totalOtherRT to 5 + totalOtherRT, to clean up later on, DeZ

					RT = RatingTask();
					// Change by Santoso 22/10/08
					// only calculate Gap if survey include CPR/FPR Rating task
					// initialize cpr/fpr flag
					hasCPRFPR = false;
					for(int k=0; k<RT.size(); k++) {
						votblSurveyRating vo = (votblSurveyRating)RT.elementAt(k);
						RTID = vo.getRatingTaskID();
						RTCode 	=  vo.getRatingCode();

						Vector result = MeanResult(RTID, compID, KBID);

						if(RTCode.equals("CP")) {
							//Comment off by Ha 02/07/08 should not re-set the r to 0
							//It will print out the KB CP, CPR, FPR incorrecly
							//r = 0;

							//by Hemilda 23/09/2008 to get total oth,sup,self,all for each kb
							//added back the others for type customization(16 Dec 2009 Qiao Li)
							totalOth =totalOth(RTID, compID, KBID); // Commented away to remove CP(Others) to cater for splitting Subordinates and Peers, Desmond 22 Oct 09
							totalSup = totalSup(RTID, compID, KBID);						
							totalSelf = totalSelf(RTID, compID, KBID);

							// Added to cater to the splitting of peers and subordinates, Desmond 21 Oct 09
							/* Change (s): add in surveyLevel to use different tables 
							 * (tblResultCompetency and tblResultKeyBehavior) to retrieve number of raters
							 * Updated by: Qiao Li 21 Dec 2009
							 */
							totalPeer = totalRater(RTID, compID,KBID, "PEER%", surveyLevel);
							totalSub = totalRater(RTID, compID,KBID, "SUB%", surveyLevel);

							// Re-locate and modified codes to include Peers and Subordinates by Desmond 22 Oct 09							
							totalAll = totalSup + totalPeer + totalSub+totalOth;


							for(int l=0; l<result.size(); l++) {
								String [] arr = (String[])result.elementAt(l);

								// Updated adjustments to type in order to cater for splitting of subordinates & peers, Desmond 28 Oct 09
								int type = Integer.parseInt(arr[1]);
								//remove the hack, use back the type such that results for others are also retrieved (16 Dec 2009 Qiao Li)
								//if(type > 3) type = type -1; // Adjust values > 3 because we removed Others from the list

								String t = "";

								switch(type) {
								case 1 : t = "All";	
								arrN[iN] = totalAll;
								iN++;
								// Change by Santoso 22/10/08
								// the total rater will be printed later below
								// set the valur of total all here
								totalRater[0] = totalAll;
								break;
								case 2 : t = "Superior"; // Change from Supervisors to Superior, Desmond 22 Oct 09
								arrN[iN] = totalSup;
								iN++;
								// Change by Santoso 22/10/08
								// the total rater will be printed later below
								// set the valur of total sup here
								totalRater[1] = totalSup;
								break;
								// Commented away to remove CP(Others) to cater for splitting Subordinates and Peers, Desmond 22 Oct 09
								//added back the others for type customization(16 Dec 2009 Qiao Li)
								case 3 : t = "Others";
								arrN[iN] = totalOth;
								iN++;
								// Change by Santoso 22/10/08
								// the total rater will be printed later below
								// set the valur of total others here
								totalRater[2] = totalOth;
								break;	
								case 4 : t = "Self";
								arrN[iN] = totalSelf;
								iN++;
								// Change by Santoso 22/10/08
								// the total rater will be printed later below
								// set the valur of total self here
								totalRater[3] = totalSelf;
								break;

								// Added case 5 and 6 to cater to splitting of Peers and Subordinates by Desmond 21 Oct 09
								case 5 : t = "Subordinates";
								arrN[iN] = totalSub;
								iN++;
								totalRater[4] = totalSub;
								break;
								case 6 : t = "Peers";
								arrN[iN] = totalPeer;
								iN++;
								totalRater[5] = totalPeer;
								break;
								}

								//Should not insert a "\n". Will push col into 2 rows. Printing will go haywire (Maruli)
								//Rating[r] = RTCode + "\n(" + t + ")";

								// Change by Santoso 22/10/08 : Rating order and value already initialized above (also the Result)
								// we need to set the RTCode and the result at the appropriate position as already defined above
								Rating[type-1] = RTCode + "(" + t + ")";

								if(iReportType == 1)
									Rating[type-1] = Rating[type-1].replaceAll("\n", " ");

								Result[type-1] = Double.parseDouble(arr[2]);	;

							}
						}else if(RTCode.equals("CPR") || RTCode.equals("FPR")){
							//Comment off by Ha 02/07/08 should not re-set the r to 0
							//It will print out the KB CP, CPR, FPR incorrecly

							//r = 0;
							// Change by Santoso : 2008-10-19
							// Do we need to check k< result.size? 
							// k refers to index of RatingTask and result refers to the MeanResult
							if(result.size() != 0) { // && k<result.size()) {
								// Change by Santoso : 2008-10-19
								// Wrong value seems to be retrieved (sup's value --> result[2])
								// Retrieve the correct value for All in result[0]
								// String [] arr = (String[])result.elementAt(k);
								String [] arr = (String[])result.elementAt(0);
								hasCPRFPR = true;
								//Should not insert a "\n". Will push col into 2 rows. Printing will go haywire (Maruli)
								//Rating[r] = RTCode + "\n(All)";
								// Change by Santoso 22/10/08 : Rating order and value already initialized above (also the Result)
								// we need to set the RTCode and the result at the appropriate position as already defined above
								//change from 5 to totalType for customization (16 Dec 2009 Qiao Li)
								Rating[totalType] = RTCode + "(All)";  // Changed from Rating[4] to Rating[5] to cater for new catergories Subordinates & Peers, Desmond 22 Oct 09
								if(iReportType == 1)
									Rating[totalType] = Rating[totalType].replaceAll("\n", " "); // Changed from Rating[4] to Rating[5] to cater for new catergories Subordinates & Peers, Desmond 22 Oct 09

								// Change by Santoso (2008-10-19) 
								// Fix CPR(All), we need to find it again (instead of using the current totalAll value)
								//added back the others for type customization(16 Dec 2009 Qiao Li)
								totalOth =totalOth(RTID, compID, KBID); // Commented away to remove CP(Others) to cater for splitting Subordinates and Peers, Desmond 22 Oct 09
								totalSup = totalSup(RTID, compID, KBID);

								// Added to cater to the splitting of peers and subordinates, Desmond 21 Oct 09
								/* Change (s): add in surveyLevel to use different tables 
								 * (tblResultCompetency and tblResultKeyBehavior) to retrieve number of raters
								 * Updated by: Qiao Li 21 Dec 2009
								 */
								totalPeer = totalRater(RTID, compID,KBID, "PEER%", surveyLevel);
								totalSub = totalRater(RTID, compID,KBID, "SUB%", surveyLevel);

								// Re-locate and modified codes to include Peers and Subordinates by Desmond 22 Oct 09							
								totalAll = totalSup + totalPeer + totalSub + totalOth;

								// Changed from Rating[4] to Rating[5] to cater for new catergories Subordinates & Peers, Desmond 22 Oct 09
								//change from 5 to totalType for customization (16 Dec 2009 Qiao Li)
								totalRater[totalType] = totalAll; 
								Result[totalType] = Double.parseDouble(arr[2]);

								// 11/12/2009 Denise
								// Change postion to display CPR/FPR on the top
								//change from 5 to totalType for customization (16 Dec 2009 Qiao Li)
								double CPRFPR_Result = Result[totalType];
								int    CPRFPR_TotalRating = totalRater[totalType];

								for (int x=totalType; x>0; x--)
								{
									totalRater[x] = totalRater[x-1];
									Result[x] = Result[x-1];
								}

								Result[0] = CPRFPR_Result;
								totalRater[0] = CPRFPR_TotalRating;

								Rating[0] = Rating[6];
								Rating[1] = "CP(All)";
								Rating[2] = "CP(Superior)"; 
								//added back the others for type customization(16 Dec 2009 Qiao Li)
								Rating[3] = "CP(Others)";
								Rating[4] = "CP(Self)";
								Rating[5] = "CP(Subordinates)";
								Rating[6] = "CP(Peers)";
							}					
						}

					}//while RT
					//rater type can change depends on whether "Others" is splitted
					//get the appropriate number and prepareCells(Qiao Li 17 Dec 2009)
					totRaterType = Rating.length;
					if (splitOthers== 0){
						totRaterType -= 2;
					}
					else{
						totRaterType -=1;
					}
					// Change by Santoso : Alignment of n number with the bar of the graph 
					// the raters displayed are fixed, no need to calculate rowTotal anymore
					// was : rowTotal = row + 11;

					//rowPos = prepareCells(xSpreadsheet, row, totalRater.length);
					rowPos = prepareCells(xSpreadsheet, row, totRaterType);
					row++;
					// Change by Santoso (2008-10-08)
					// total rater is printed inside drawChart, we need to pass
					// the total rater list and the position to draw
					drawChart(Rating, Result, totalRater, rowPos, maxScale, splitOthers);

					//  12/12/2009 Denise
					// reinitialize Result and totalRater
					//change from 5 to totalType for customization (16 Dec 2009 Qiao Li)
					Result = new double [totalType + totalOtherRT]; // Changed from 4 + totalOtherRT to 5 + totalOtherRT, to clean up later on, DeZ
					totalRater = new int[totalType + totalOtherRT]; 

					column = 9;							
					r1 = row;

					Vector vImportance = Importance(compID, KBID);

					for(int k=0; k<vImportance.size(); k++) {
						String [] arr = (String[]) vImportance.elementAt(k);
						String task = arr[1];
						double taskResult = Double.parseDouble(arr[2]);


						arrN[iN] = totalAll;
						iN++;
						OO.insertString(xSpreadsheet, task + ": " + taskResult, r1, column);
						OO.mergeCells(xSpreadsheet, column, endColumn, r1, r1+1);
						r1 += 3;
					}

					// Change by Santoso 22/10/08
					// only calculate Gap if survey include CPR/FPR Rating task
					if (hasCPRFPR) {
						gap = Gap(compID, KBID);
						gap = Math.round(gap * 100.0) / 100.0;

						if (iNoCPR == 0)	// If CPR is chosen in this survey
						{
							//Changed the default language to English by Chun Yeong 9 Jun 2011
							//Commented away to allow translation below, Chun Yeong 1 Aug 2011
							/*if (ST.LangVer == 2) //Indonesian
									OO.insertString(xSpreadsheet, "Selisih = " + gap, r1, column);	
								else //if (ST.LangVer == 1) English
									OO.insertString(xSpreadsheet, "Gap = " + gap, r1, column);*/

							//Allow dynamic translation, Chun Yeong 1 Aug 2011
							OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Gap") + " = " + gap, r1, column);
							OO.mergeCells(xSpreadsheet, column, endColumn, r1, r1+1);
						}
					}
					r1 += 3;

					LOA = LevelOfAgreement(compID, KBID);
					//Changed the default language to English by Chun Yeong 9 Jun 2011
					//Commented away to allow translation below, Chun Yeong 1 Aug 2011
					/*if (ST.LangVer == 2) //Indonesian
							OO.insertString(xSpreadsheet, "Tingkat Persetujuan: \n" + LOA + "%", r1, column);
						else //if (ST.LangVer == 1) English
							OO.insertString(xSpreadsheet, "Level Of Agreement: \n" + LOA + "%", r1, column);*/ 

					//Allow dynamic translation, Chun Yeong 1 Aug 2011
					OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Level Of Agreement") + ": \n" + LOA + "%", r1, column); 
					OO.mergeCells(xSpreadsheet, column, endColumn, r1, r1+2);																				
					r1 += 4;							

					count++;
					column = 0;
					//added a page break at the end of the chart section of KB Level Survey
					//to solve part of pagination problem (Qiao Li 23 Dec 2009)
					if(count == 2 ||(j == KBList.size()-1 && i == vComp.size()-1)) {
						count = 0;	
						/* Change: standardize the number of rows for the charts to be 15 
						 * Reason: fit 2 charts in one page
						 * Updated by: Qiao Li
						 * Date: 23 Dec 2009
						 */
						row += 15;							
						insertPageBreak(xSpreadsheet, startColumn, endColumn, row);

					} else								
						row += 15;

					endRow = row - 1;

					//comp name and definition						
					OO.setTableBorder(xSpreadsheet, startColumn, endColumn, startRow, startRow+1, 
							false, false, true, true, true, true);
					//total sup n others				
					OO.setTableBorder(xSpreadsheet, startColumn, startColumn, startRow+2, endRow, 
							false, false, true, true, true, true);
					//chart
					OO.setTableBorder(xSpreadsheet, startColumn+1, 8, startRow+2, endRow, 
							false, false, true, true, true, true);	
					OO.setTableBorder(xSpreadsheet, 9, endColumn, startRow+2, endRow, 
							false, false, true, true, true, true);

					OO.setCellAllignment(xSpreadsheet, startColumn, startColumn, startRow+2, endRow, 1, 2);

					///Denise 07/01/2009
					if(count == 0) { //move table two lines down
						OO.insertRows(xSpreadsheet, 0, 10, row+1, row+3, 2, 1);		    			
						row +=2;
					} 
				}//while KBList
				//}	// end if of standard version
			}// while Comp
		}
	} // End InsertCompetency()


	/**
	 * Write competency report to excel.
	 */
	public void InsertCompetencyToyota() throws SQLException, Exception 
	{	
		//System.out.println("7. Competencies Starts");

		column = 0;
		row = 32;
		startColumn = 0;
		endColumn = 4;

		int surveyLevel = Integer.parseInt(surveyInfo[0]);
		int total 		= totalGroup() + totalOtherRT() + 1;		// 1 for all
		//int total 		= totalGroup() + totalOtherRT() + 2; // For TMT problematic rep, COMMENT OFF (Maruli)

		String [] Rating = new String [total];
		double [] Result = new double [total];

		int iCompCount = 1; // Counter to count the no of competencies
		int iCompRow = 32;	// Counter to store Row of <Comp Name X>
		int iCompCol = 0;	// Counter to count Column of <Comp Name X>
		//int iCompRow = 60;	// Counter to store Row of <Comp Name X>
		//int iCompCol = 18;	// Counter to count Column of <Comp Name X>

		//int totalOth = totalOth();
		//int totalSup = totalSup();
		//int totalSelf = totalSelf();
		//int totalAll = totalOth + totalSup;

		//ResultSet competency = Competency(1);
		Vector vComp = Competency(0);

		if(surveyLevel == 0) {

			for(int i=0; i<vComp.size(); i++) {
				voCompetency voComp = (voCompetency)vComp.elementAt(i);

				int compID 		= voComp.getCompetencyID();
				String statement = voComp.getCompetencyName();
				String desc = voComp.getCompetencyDefinition();

				int RTID = 0;			
				int KBID = 0;


				String RTCode = "";

				Vector RT = RatingTask();
				int r = 0;

				for(int j=0; j<RT.size(); j++) {
					votblSurveyRating vo = (votblSurveyRating)RT.elementAt(j);

					RTID = vo.getRatingTaskID();
					RTCode 	=  vo.getRatingCode();

					Vector result = MeanResult(RTID, compID, KBID);

					if(RTCode.equals("CP")) {
						for(int l=0; l<result.size(); l++) {
							String [] arr = (String[])result.elementAt(l);

							int type = Integer.parseInt(arr[1]);
							String t = "";

							switch(type) {
							case 1 : t = "All";
							break;
							case 2 : t = "Supervisors";
							break;
							case 3 : t = "Others";
							break;
							case 4 : t = "Self";
							break;
							}

							Rating[r] = RTCode + " (" + t + ")";
							Result[r++] = Double.parseDouble(arr[2]);
						}
					} else if(RTCode.equals("CPR") || RTCode.equals("FPR")) {

						if(result.size() != 0 && j < result.size()) {
							String [] arr = (String[])result.elementAt(j);

							int type = Integer.parseInt(arr[1]);
							Rating[r] = RTCode + " (All)";
							Result[r++] = Double.parseDouble(arr[2]);
						}
					}

				}//while RT

				r = row;

				// Insert Score
				for(int j=0; j<Rating.length; j++) 
				{	
					if (Rating[j].equals("CPR (All)"))
						r = row + 5;
					else if (Rating[j].equals("CP (All)"))
						r = row + 3;
					else if (Rating[j].equals("CP (Self)"))
						r = row + 2;
					else if (Rating[j].equals("CP (Supervisors)"))
						r = row + 1;
					else if (Rating[j].equals("CP (Others)"))
						r = row;

					OO.insertString(xSpreadsheet2, Rating[j], r, column);
					OO.insertNumeric(xSpreadsheet2, Result[j], r, column+1);
				}

				int element = vCompID.indexOf(new Integer(compID));
				double gap = Double.valueOf(((String [])vGapUnsorted.elementAt(element))[1]).doubleValue();					

				OO.insertString(xSpreadsheet, " Gap = " + gap, iCompRow, iCompCol);

				row = row + 7;
				iCompCount++;
				iCompCol = iCompCol + 6;
				//iCompCol = iCompCol - 6;

				/*
				 *	After 4 competencies name are printed, increase rows to print the next 4 competencies
				 *	At the same time reduce column to 0 to start printing from Left to Right again
				 */

				if (iCompCount == 5 || iCompCount == 9)
				{
					iCompCol = 0;
					iCompRow = iCompRow + 14;
				}
				/*
    			if (iCompCount == 5 || iCompCount == 9)
    			{
    				iCompCol = 18;
    				iCompRow = iCompRow - 14;
    			}
				 */
			}// while Comp


		}else {	//kb level

			for(int i=0; i<vComp.size(); i++) {
				voCompetency voComp = (voCompetency)vComp.elementAt(i);

				int r = 0;
				int RTID = 0;
				int compID = voComp.getCompetencyID();								
				String RTCode = "";

				Vector RT = RatingTask();

				for(int j=0; j<RT.size(); j++) {
					votblSurveyRating vo = (votblSurveyRating)RT.elementAt(j);

					RTID = vo.getRatingTaskID();
					RTCode 	=  vo.getRatingCode();

					Vector result = null;

					if(RTCode.equals("CP")) {
						result = KBMean(RTID, compID);

						for(int k=0; k<result.size(); k++) {
							String [] arr = (String[])result.elementAt(k);

							int type = Integer.parseInt(arr[1]);
							String t = "";

							switch(type) {
							case 1 : t = "All";
							break;
							case 2 : t = "Supervisors";
							break;
							case 3 : t = "Others";
							break;		 		 	
							case 4 : t = "Self";
							break;
							}

							Rating[r] = RTCode + " (" + t + ")";

							if(type == 1)
								Result[r++] = CompTrimmedMeanforAll(RTID, compID);
							else
								Result[r++] = Double.parseDouble(arr[2]);

						}
					}else if(RTCode.equals("CPR") || RTCode.equals("FPR")) {

						Rating[r] = RTCode + " (All)";								
						Result[r++] = CompTrimmedMeanforAll(RTID, compID);				
					}				

				}//while RT

				r = row;

				// Insert Score
				for(int j=0; j<Rating.length; j++) {

					if (Rating[j].equals("CPR (All)"))
						r = row + 5;
					else if (Rating[j].equals("CP (All)"))
						r = row + 3;
					else if (Rating[j].equals("CP (Self)"))
						r = row + 2;
					else if (Rating[j].equals("CP (Supervisors)"))
						r = row + 1;
					else if (Rating[j].equals("CP (Others)"))
						r = row;

					OO.insertString(xSpreadsheet2, Rating[j], r, column);
					OO.insertNumeric(xSpreadsheet2, Result[j], r, column+1);
					//r++;
				}

				int element = vCompID.indexOf(new Integer(compID));
				double gap = Double.valueOf(((String [])vGapUnsorted.elementAt(element))[1]).doubleValue();

				OO.insertString(xSpreadsheet, " Gap = " + gap, iCompRow, iCompCol);

				row = row + 7;
				iCompCount++;
				//Uncommented by Jenty on 27-Sept-06
				//Otherwise the gap did not show properly
				iCompCol = iCompCol + 6;
				//iCompCol = iCompCol - 6;

				/*
				 *	After 4 competencies name are printed, increase rows to print the next 4 competencies
				 *	At the same time reduce column to 0 to start printing from Left to Right again
				 */
				if (iCompCount == 5 || iCompCount == 9)
				{
					iCompCol = 0;
					iCompRow = iCompRow + 14;
					//iCompCol = 18;
					//iCompRow = iCompRow - 14;
				}
			}// while Comp
		}

		Vector vResult = getCompAvg(surveyID, vCompID, "CP");

		int r = 36;

		for(int v=0;v<vResult.size();v++)
		{
			double dAvgCPPos = Double.parseDouble((String)vResult.elementAt(v));

			OO.insertString(xSpreadsheet2, "Avg. CP of Position", r, column);
			OO.insertNumeric(xSpreadsheet2, dAvgCPPos, r, column+1);

			r = r + 7;
		}
	}

	/**
	 * Change by Santoso (2008-10-08)
	 * total rater is printed inside drawChart, we need to pass
	 * the total rater list and the position to draw
	 * 
	 * Draw bar chart for competency report.
	 */
	/**
	 * drawChart
	 * 
	 * 
	 * @param Rating
	 * @param Result
	 * @param totalRater
	 * @param rowPos
	 * @param maxScale
	 * @param splitOthers
	 * changed by Qiao Li (17 Dec 2009)
	 * added in splitOthers to indicate whether we should show "Others" or "Subordinates" and "Peers"
	 * if splitOthers ==0, show "Others", if splitOthers==1, show "Subordinates" and "Peers"
	 * precondition: Ratings should only include:"CP(All)","CP(Superior)","CP(Others)","CP(Self)","CP(Subordinates)","CP(Peers)";
	 * */
	public void drawChart(String Rating [], double Result [], int[] totalRater, int[] rowPos, int maxScale, int splitOthers) throws IOException, Exception 
	{
		//iReportType = 1 (Simplified Report "No Comp Chart"), 2 (Standard Report)
		int r = row;
		int c = 0;
		/* Change (s): added in selfIdx to get values of "Self"
		 * Reason (s): rearrange "Self" to be at the bottom of the chart
		 * Updated by: Qiao Li
		 * Date: 17 Dec 2009
		 */
		int selfIdx = -1;
		int cellIdx = 0;

		if(iReportType == 1)
		{
			//Print heading for "N" and align 
			OO.insertString(xSpreadsheet, "N", r, c+5);
			OO.setCellAllignment(xSpreadsheet, c+5, c+5, r, r, 1, 3);
		}

		/* Change (s): Calculation of the Others value when split option is not selected
		 * Reason (s): To allow specific reports to have a not split option
		 * Updated by: Chun Yeong
		 * Date: 13 Jun 2011
		 */
		if(splitOthers == 0){
			//Find the index of Others, Subordinate and Peers
			int iOth = 0, iSub = 0, iPeer = 0;
			for(int i = 0; i < Rating.length; i++) {
				if(Rating[i].contains("Oth")) {
					iOth = i;
				}
				if(Rating[i].contains("Sub")) {
					iSub = i;
				}
				if(Rating[i].contains("Peer")) {
					iPeer = i;
				}
			}

			//Do reverse engineering to calculate the new Others value
			formatter.setRoundingMode(RoundingMode.HALF_UP);
			String value = formatter.format(((totalRater[iSub]*Result[iSub]) + (totalRater[iPeer]*Result[iPeer]) 
					+ (totalRater[iOth]*Result[iOth]))/(totalRater[iSub] + totalRater[iPeer] + totalRater[iOth]));
			//Store the value into Others row in Result
			Result[iOth] = Double.parseDouble(value);
			//Store the total number of people into Others row in totalRater
			totalRater[iOth] += totalRater[iSub] + totalRater[iPeer];
		}

		for(int i=0; i<Rating.length; i++) {
			// Change by Santoso (2008-10-08)
			// add protection in case Rating[i] is null --> we fixed the rating
			if (Rating[i] != null ) {
				//identify "Self" (Qiao Li 17 Dec 2009)
				if (Rating[i].equals("CP(Self)")){
					selfIdx = i;
					continue;
				}

				/* Change (s): added in flags
				 * Reason (s): show "others" or ("subordinates" and "peers")
				 * Updated by: Qiao Li
				 * Date: 17 Dec 2009
				 */
				if ((splitOthers== 1 && !Rating[i].equals("CP(Others)"))||
						(splitOthers == 0 && !Rating[i].equals("CP(Subordinates)")&& !Rating[i].equals("CP(Peers)"))){
					r++;
					OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, Rating[i]), r, c + 2);
					OO.mergeCells(xSpreadsheet, c + 2, c + 3, r, r);

					OO.insertNumeric(xSpreadsheet, Result[i], r, c + 4);
					// change by Santoso (2008-10-08)
					// use totalRater (already sorted) instead of arrN
					// OO.insertNumeric(xSpreadsheet, arrN[i], r, c+5);
					OO.insertNumeric(xSpreadsheet, totalRater[i], r, c + 5);
				}
			}
		}
		if (selfIdx != -1) {
			// identify "Self" and put it at the bottom of the chart(Qiao Li 17 Dec 2009)
			r++;
			OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, Rating[selfIdx]), r, c + 2);
			OO.mergeCells(xSpreadsheet, c + 2, c + 3, r, r);

			OO.insertNumeric(xSpreadsheet, Result[selfIdx], r, c + 4);
			// change by Santoso (2008-10-08)
			// use totalRater (already sorted) instead of arrN
			// OO.insertNumeric(xSpreadsheet, arrN[i], r, c+5);
			OO.insertNumeric(xSpreadsheet, totalRater[selfIdx], r, c + 5);
		}
		selfIdx = -1;//reset
		r = row; //reset
		if(iReportType == 2) 
		{
			/*
			 * Change(s) : add a line to insert "n" at top of no. of raters column
			 * Reason(s) : as per requirement of software
			 * Added By: Alvis
			 * Added On: 06 Aug 2009
			 */
			//Print "n" right at the top of the no.of.raters column
			//qiaoli
			//OO.insertString(xSpreadsheet, "n", rowPos[0]-2, 0);
			OO.insertString(xSpreadsheet, "n", r-1, 0);
			// Add by Santoso (2008-10-08)
			// Print the total rater value here, instead of in many places
			/*commented out for loop
			 *do the printing of n later instead such that we can put "self" at the bottom (Qiao Li 17 Dec 2009) 
			 */
			/*for (int idx = 0; idx < totalRater.length; idx++) {
			 	// OO.insertNumeric(xSpreadsheet, totalAll, rowTotal, 0);
					OO.insertNumeric(xSpreadsheet, totalRater[idx], rowPos[idx], 0);
			}*/
			cellIdx = 0;
			for(int i=0; i<Rating.length; i++) {
				//Added by Ha 25/06/08 to get rid of "0" line in the report			
				if (Rating[i]!=null)
				{	
					//identify "self" (Qiao Li 17 Dec 2009)
					if (Rating[i].equals("CP(Self)")){
						selfIdx = i;
						continue;
					}
					/* Change (s): added in flags
					 * Reason (s): show "others" or ("subordinates" and "peers")
					 * Updated by: Qiao Li
					 * Date: 17 Dec 2009
					 */
					if ((splitOthers== 1 && !Rating[i].equals("CP(Others)"))||
							(splitOthers == 0 && !Rating[i].equals("CP(Subordinates)")&& !Rating[i].equals("CP(Peers)"))){
						OO.insertString(xSpreadsheet2, trans.tslt(templateLanguage, Rating[i]), r, c);
						OO.insertNumeric(xSpreadsheet2, Result[i], r, c+1);
						OO.insertNumeric(xSpreadsheet, totalRater[i], rowPos[cellIdx], 0);
						r++;
						cellIdx++;
					}
				}			
			}
			if (selfIdx != -1) {
				OO.insertString(xSpreadsheet2, trans.tslt(templateLanguage, Rating[selfIdx]), r, c);
				OO.insertNumeric(xSpreadsheet2, Result[selfIdx], r, c + 1);
				//	System.out.println("test " + rowPos.length + "  " + cellIdx + " " + selfIdx + "  " + totalRater.length);
				OO.insertNumeric(xSpreadsheet, totalRater[selfIdx], rowPos[cellIdx], 0);
				r++;
			}

			//draw chart
			XTableChart xtablechart = OO.getChart(xSpreadsheet, xSpreadsheet2, c, c+1, row-1, r-1, Integer.toString(row), 10000, 7800, row, 2);
			OO.setFontSize(8);
			xtablechart = OO.setChartTitle(xtablechart, "");
			OO.showLegend(xtablechart, false);
			// (22-08-06) Axes should all be 0 degree right? Otherwise, slanted score seems funny (Maruli)
			//xtablechart = OO.setAxes(xtablechart, "", "Scores (%)", maxScale, 1, 0, 4500);
			xtablechart = OO.setAxes(xtablechart, "", "Scores (%)", maxScale, 1, 0, 0);
			OO.drawGridLines(xtablechart, 0); //draw the gridlines, Mark Oei 25 Mar 2010
			OO.setChartProperties(xtablechart, false, true, false, false, true); // display only the vertical lines, Mark Oei 25 Mar 2010
		}//end iReportType
	}

	/**
	 * Write comments on excel.
	 */
	public void InsertComments() throws SQLException, IOException, Exception 
	{
		//Added by Alvis 07-Aug-09 for pagebreak fix implemented below
		int currentPageHeight = 1076;//1076 is to accomodate the first two cells, which contains the title.
		Vector vComp = CompListHaveComments();
		row++;

		int surveyLevel = Integer.parseInt(surveyInfo[0]);
		column = 0;

		//if competency having comments exist, display Narrative Comments title. If not, hide the title. To hide Narrative Comments title when there are totally no comments for the competencies in the survey Sebastian 19 July 2010
		//if (vComp.size() > 0)		//Commented away to display even title if there is no comments by Chun Yeong 29 Jun 2011
		//{

		//Changed the default language to English by Chun Yeong 9 Jun 2011
		//Commented away to allow translation below, Chun Yeong 1 Aug 2011
		/*if (ST.LangVer == 2) //Indonesian
			OO.insertString(xSpreadsheet, "Komentar Naratif", row, column);
		else //if (ST.LangVer == 1) English
			OO.insertString(xSpreadsheet, "Narrative Comments", row, column);*/

		//Allow dynamic translation, Chun Yeong 1 Aug 2011
		OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Narrative Comments"), row, column);
		OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);

		//}

		row += 2;


		if(vComp.size() == 0) {
			OO.insertString(xSpreadsheet, "Nil", row, column);
		}
		row++;

		int startBorder1 = 1;
		int startBorder = 1;
		int endBorder = 1;
		int endBorder1 = 1;
		int selfIncluded = Q.SelfCommentIncluded(surveyID);
		int column = 0;

		//added by Ping Yang on 11/08/08, check raters assigned
		boolean blnSupIncluded = Q.SupCommentIncluded(surveyID, targetID);
		boolean blnOthIncluded = Q.OthCommentIncluded(surveyID, targetID);
		System.out.println("blnSupIncluded = " + blnSupIncluded + ", blnOthIncluded = " + blnOthIncluded + ", surveyLevel = " + surveyLevel); // To remove, Desmond 16 Nov 09	
		if(surveyLevel == 0) {

			// ----------------- START COMPETENCY LEVEL SURVEY ----------------- //

			int count = 0;

			startBorder = row;
			for(int i=0; i<vComp.size(); i++) {
				voCompetency voComp = (voCompetency)vComp.elementAt(i);

				int compID 		= voComp.getCompetencyID();
				String statement = voComp.getCompetencyName();

				count++;

				int statementPos = row; //Denise 08/01/2009 store position to insert competency description
				//			OO.insertString(xSpreadsheet, Integer.toString(count) + ".", row, column);								
				//			OO.insertString(xSpreadsheet, UnicodeHelper.getUnicodeStringAmp(statement), row, column+1);
				//			OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
				//			OO.setBGColor(xSpreadsheet, startColumn, endColumn, row, row, BGCOLOR);
				//			OO.setCellAllignment(xSpreadsheet, startColumn, endColumn, row, row, 2, 1); // Set alignment of competency name to top, Desmond 16 Nov 09

				int KBID = 0;
				int start = 0;
				row++;


				//Added by Ha 23/06/08 reset the value start to print the header of comment correctly
				start = 0;					 					
				Vector supComments = getComments("SUP%", compID, KBID);

				// Added variables to store comments from peers and subordinates, Desmond 18 Nov 09
				Vector othComments = getComments("OTH%", compID, KBID);
				Vector peerComments = getComments("PEER%", compID, KBID);
				Vector subComments = getComments("SUB%", compID, KBID);

				if(blnSupIncluded){//added by Ping Yang on 11/08/08, check raters assigned
					boolean blnSupCommentExists  = false;//Added by ping yang on 31/7/08 to get rid of extra '-'s

					for(int j=0; j<supComments.size(); j++) {
						String [] arr = (String[])supComments.elementAt(j);
						if(start == 0) {

							OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Superior(s)"), row, column+1); // Change from Supervisors to Superior, Desmond 22 Oct 09
							OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
							OO.setFontItalic(xSpreadsheet, startColumn, endColumn, row, row);

							row++;
							start++;
						}

						String comment = arr[1];
						if(!comment.trim().equals("")){//Added by ping yang on 31/7/08 to get rid of extra '-'s
							OO.insertString(xSpreadsheet, "- " + UnicodeHelper.getUnicodeStringAmp(comment), row, column+1);			
							OO.mergeCells(xSpreadsheet, column+1, endColumn, row, row);
							OO.setRowHeight(xSpreadsheet, row, column+1, ROWHEIGHT*OO.countTotalRow(comment, 85));
							OO.setCellAllignment(xSpreadsheet, startColumn, startColumn, row, row, 2, 1);
							row++;
							blnSupCommentExists = true;
						}

					}

					start = 0;		

					/*
					 * Change(s) : Added codes to point to next row below if exist rater code comments. Remove codes that add default empty comment in the report if rater code have no comments
					 * Reason(s) : To remove empty narrative comments by rater category, KB then Competency. i.e If competency has no comments from raters, remove the entire competency in the narrative comments.
					 * Updated By: Sebastian
					 * Updated On: 19 July 2010
					 */
					if (supComments.size() > 0)
					{
						row++;
					}
				}// end if(blnSupIncluded)

				if(blnOthIncluded){//added by Ping Yang on 11/08/08, check raters assigned
					/* Change: determine whether to show Others'comments based on splitOthers
					   Updated by: Qiao Li 23 dec 2009*/
					if (splitOthers==1){
						// Added codes to insert peers' comments, Desmond 23 Nov 09
						// Insert Peers' comments

						boolean blnPeerCommentExist = false; //Added to get rid of extra '-'s
						for(int k=0; k<peerComments.size(); k++) {
							String [] arr = (String[])peerComments.elementAt(k);
							String comment = arr[1];

							if (start == 0) {
								//Changed the default language to English by Chun Yeong 9 Jun 2011
								//Commented away to allow translation below, Chun Yeong 1 Aug 2011
								/*if (ST.LangVer == 2) //Indonesian
									OO.insertString(xSpreadsheet, "Lainnya", row,
											column + 1);
								else // if (ST.LangVer == 1) English
									OO.insertString(xSpreadsheet, "Peer(s)", row,
											column + 1);*/

								//Allow dynamic translation, Chun Yeong 1 Aug 2011
								OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Peer(s)"), row,
										column + 1);
								OO.setFontBold(xSpreadsheet, startColumn,
										endColumn, row, row);
								OO.setFontItalic(xSpreadsheet, startColumn,
										endColumn, row, row);

								start++;
								row++;
							}	

							if(!comment.trim().equals("")){
								OO.insertString(xSpreadsheet, "- " + UnicodeHelper.getUnicodeStringAmp(comment), row, column+1);
								OO.mergeCells(xSpreadsheet, column+1, endColumn, row, row);
								OO.setRowHeight(xSpreadsheet, row, column+1, ROWHEIGHT*OO.countTotalRow(comment, 85));
								OO.setCellAllignment(xSpreadsheet, startColumn, endColumn, row, row, 2, 1);

								row++;	
								blnPeerCommentExist = true;
							}															
						}

						// Adjust counters
						start = 0;

						if (peerComments.size() > 0)
						{
							row++;
						}

						// Added codes to insert subordinates' comments, Desmond 23 Nov 09
						// Insert Subordinates' comments
						boolean blnSubCommentExist = false;	//Added to get rid of extra '-'s
						for(int k=0; k<subComments.size(); k++) {
							String [] arr = (String[])subComments.elementAt(k);
							String comment = arr[1];

							if (start == 0) {
								//Changed the default language to English by Chun Yeong 9 Jun 2011
								//Commented away to allow translation below, Chun Yeong 1 Aug 2011
								/*if (ST.LangVer == 2) //Indonesian
									OO.insertString(xSpreadsheet, "Lainnya", row,
											column + 1);
								else //if (ST.LangVer == 1) English
									OO.insertString(xSpreadsheet, "Subordinate(s)",
											row, column + 1);*/

								//Allow dynamic translation, Chun Yeong 1 Aug 2011
								OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Subordinate(s)"),
										row, column + 1);
								OO.setFontBold(xSpreadsheet, startColumn,
										endColumn, row, row);
								OO.setFontItalic(xSpreadsheet, startColumn,
										endColumn, row, row);

								start++;
								row++;
							}					

							if(!comment.trim().equals("")){
								OO.insertString(xSpreadsheet, "- " + UnicodeHelper.getUnicodeStringAmp(comment), row, column+1);
								OO.mergeCells(xSpreadsheet, column+1, endColumn, row, row);
								OO.setRowHeight(xSpreadsheet, row, column+1, ROWHEIGHT*OO.countTotalRow(comment, 85));
								OO.setCellAllignment(xSpreadsheet, startColumn, endColumn, row, row, 2, 1);

								row++;	
								blnSubCommentExist = true;
							}															
						}

						// Adjust counters
						start = 0;

						if (subComments.size() > 0)
						{
							row++;	
						}
					}

					// Added codes so that Others' comments (including the header "Others") is displayed only if at least one Others' comment exists, Desmond, 18 Nov 09
					// Execute this portion of codes only if there are Others' comments, if not don't even print out the header "Others"
					/* Change: determine whether to show Others'comments based on splitOthers
					   Updated by: Qiao Li 23 dec 2009*/
					else{
						// Insert Others' comments

						//Added both peers and subordinate comments together with other's comments to append them all by Chun Yeong 1 Jul 2011
						othComments.addAll(peerComments);
						othComments.addAll(subComments);

						boolean blnOthCommentExists  = false;//Added to get rid of extra '-'s

						for(int j=0; j<othComments.size(); j++) {
							String [] arr = (String[])othComments.elementAt(j);													
							String comment = arr[1];

							if(start == 0) {
								//Changed the default language to English by Chun Yeong 9 Jun 2011
								//Commented away to allow translation below, Chun Yeong 1 Aug 2011
								/*if (ST.LangVer == 2) //Indonesian
									OO.insertString(xSpreadsheet, "Orang lain", row, column+1);
								else //if (ST.LangVer == 1) English
									OO.insertString(xSpreadsheet, "Others", row, column+1);*/

								//Allow dynamic translation, Chun Yeong 1 Aug 2011
								OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Others"), row, column+1);
								OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
								OO.setFontItalic(xSpreadsheet, startColumn, endColumn, row, row);

								start++;
								row++;
							}

							//Added to get rid of extra '-'s
							if(!comment.trim().equals("")){
								OO.insertString(xSpreadsheet, "- " + UnicodeHelper.getUnicodeStringAmp(comment), row, column+1);			
								OO.mergeCells(xSpreadsheet, column+1, endColumn, row, row);
								OO.setRowHeight(xSpreadsheet, row, column+1, ROWHEIGHT*OO.countTotalRow(comment, 85));
								OO.setCellAllignment(xSpreadsheet, startColumn, startColumn, row, row, 2, 1);
								row++;
								blnOthCommentExists = true;
							}
						}

						start = 0;

						if (othComments.size() > 0)
						{
							row++;
						}
					} // end if(!othComments.isEmpty())
				}//end if(blnOthIncluded)

				//changed the order of comments by moving "Self" to the back
				//Qiao Li 23 Dec 2009
				if(selfIncluded == 1) {
					Vector selfComments = getComments("SELF", compID, KBID);

					if(selfComments != null) {
						boolean blnSelfCommentExists  = false;//Added by ping yang on 31/7/08 to get rid of extra '-'s
						for(int j=0; j<selfComments.size(); j++) {
							String [] arr = (String[])selfComments.elementAt(j);
							if(start == 0) {
								//Changed the default language to English by Chun Yeong 9 Jun 2011
								//Commented away to allow translation below, Chun Yeong 1 Aug 2011
								/*if (ST.LangVer == 2)
									OO.insertString(xSpreadsheet, "Diri Sendiri", row, column+1);
								else //if (ST.LangVer == 1)
									OO.insertString(xSpreadsheet, "Self", row, column+1);*/

								//Allow dynamic translation, Chun Yeong 1 Aug 2011
								OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Self"), row, column+1);
								OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
								OO.setFontItalic(xSpreadsheet, startColumn, endColumn, row, row);

								row++;
								start++;
							}

							String comment = arr[1];

							if(!comment.trim().equals("")){//Added by ping yang on 31/7/08 to get rid of extra '-'s
								OO.insertString(xSpreadsheet, "- " + UnicodeHelper.getUnicodeStringAmp(comment), row, column+1);
								OO.mergeCells(xSpreadsheet, column+1, endColumn, row, row);
								OO.setRowHeight(xSpreadsheet, row, column+1, ROWHEIGHT*OO.countTotalRow(comment, 85));
								OO.setCellAllignment(xSpreadsheet, startColumn, startColumn, row, row, 2, 1);

								row++;
								blnSelfCommentExists = true;
							}	
						}

						if (selfComments.size() > 0)
						{
							row++;
						}
					}else {										
						start = 0;					
						row++;
					}
					row++;
				}

				endBorder = row;

				/*
				 * Change(s) : Added codes to check height of the table to be
				 * 			  added, and insert pagebreak if necessary
				 * Reason(s) : Fix the problem of a table being spilt into between 
				 * 			  two pages. 
				 * Updated By: Alvis 
				 * Updated On: 07 Aug 2009
				 */

				//Check height and insert pagebreak where necessary
				int pageHeightLimit = 22272	;//Page limit is 22272						    
				int tableHeight = 0;

				//calculate the height of the table that is being added.
				for(int i1=startBorder;i1<=endBorder;i1++){
					int rowToCalculate = i1;

					tableHeight += OO.getRowHeight(xSpreadsheet, rowToCalculate, startColumn);
				}
				currentPageHeight = currentPageHeight + tableHeight; //add new table height to current pageheight.
				int dis = 2; //Denise 08/01/2009 to move the table two lines down
				if(currentPageHeight >pageHeightLimit){//adding the table will exceed a single page, insert page break
					insertPageBreak(xSpreadsheet, startColumn, endColumn, startBorder); 
					OO.insertRows(xSpreadsheet, startColumn, endColumn, startBorder,startBorder+dis, dis, 1);
					row += dis;
					startBorder +=dis;
					endBorder +=dis;
					currentPageHeight = tableHeight;
					statementPos +=dis;
				}
				//Denise 08/01/2009 insert competency description
				OO.insertString(xSpreadsheet, Integer.toString(count) + ".",statementPos, column);	
				//Added translation to the competency name, Chun Yeong 1 Aug 2011
				OO.insertString(xSpreadsheet, UnicodeHelper.getUnicodeStringAmp(getTranslatedCompetency(statement).elementAt(0).toString()), statementPos, column+1);
				OO.setFontBold(xSpreadsheet, startColumn, endColumn, statementPos, statementPos);
				OO.setBGColor(xSpreadsheet, startColumn, endColumn, statementPos, statementPos, BGCOLOR);
				OO.setCellAllignment(xSpreadsheet, startColumn, endColumn, statementPos, statementPos, 2, 1); // Set alignment of competency name to top, Desmond 16 Nov 09

				OO.setTableBorder(xSpreadsheet, startColumn, endColumn, startBorder, endBorder, 
						false, false, true, true, true, true);		

				startBorder = row;
			}//end for-loop v.comp.size()

			// ----------------- END COMPETENCY LEVEL SURVEY ----------------- //

		} else {

			// ----------------- START KB LEVEL SURVEY ----------------- //

			int count = 0;
			int check = 0;
			int checkKB = 0;
			int lastKB = 0;

			startBorder = row;
			startBorder1 = row;
			for(int i=0; i<vComp.size(); i++) {
				check++;
				checkKB = 1;
				voCompetency voComp = (voCompetency)vComp.elementAt(i);
				count++;
				int compID = voComp.getCompetencyID();
				String statement = voComp.getCompetencyName();							

				OO.insertString(xSpreadsheet, Integer.toString(count) + ".", row, column);
				//Added translation for the competency name, Chun Yeong 1 Aug 2011
				OO.insertString(xSpreadsheet, UnicodeHelper.getUnicodeStringAmp(getTranslatedCompetency(statement).elementAt(0).toString()), row, column+1);												
				OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
				OO.setBGColor(xSpreadsheet, startColumn, endColumn, row, row, BGCOLOR);
				OO.setCellAllignment(xSpreadsheet, startColumn, endColumn, row, row, 2, 1); // Set alignment of competency name to top, Desmond 16 Nov 09

				row++;

				Vector KBList = KBListHaveComments(compID);	

				for(int j=0; j<KBList.size(); j++) {
					checkKB++;
					voKeyBehaviour voKB = (voKeyBehaviour)KBList.elementAt(j);
					int KBID = voKB.getKeyBehaviourID();
					String KB = voKB.getKeyBehaviour();

					OO.insertString(xSpreadsheet, "KB:", row, column);  // Change from "-" to "KB:" to indicate each KB statement, Desmond 17 Nov 09
					//Added translation for the key behaviour name, Chun Yeong 1 Aug 2011
					OO.insertString(xSpreadsheet, getTranslatedKeyBehavior(UnicodeHelper.getUnicodeStringAmp(KB)), row, column+1);
					OO.setFontBold(xSpreadsheet, column, column+1, row, row); // Bold KB Statements, Desmond 17 Nov 09
					OO.mergeCells(xSpreadsheet, column+1, endColumn, row, row);
					OO.setRowHeight(xSpreadsheet, row, column+1, ROWHEIGHT*OO.countTotalRow(KB, 85));
					OO.setCellAllignment(xSpreadsheet, startColumn, startColumn, row, row, 2, 1);

					Vector supComments = getComments("SUP%", compID, KBID);

					// Added variables to store comments from peers and subordinates, Desmond 18 Nov 09
					Vector othComments = getComments("OTH%", compID, KBID);
					Vector peerComments = getComments("PEER%", compID, KBID);
					Vector subComments = getComments("SUB%", compID, KBID);

					int start = 0;	
					row++;

					if(blnSupIncluded){//added by Ping Yang on 11/08/08, check raters assigned
						boolean blnSupCommentExist = false;//Added by ping yang on 31/7/08 to get rid of extra '-'s

						for(int k=0; k<supComments.size(); k++) {
							String [] arr = (String[])supComments.elementAt(k);

							String comment = arr[1];	

							if(start == 0) {
								OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Superior(s)"), row, column+1); // Change from Supervisors to Superior, Desmond 22 Oct 09
								OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
								OO.setFontItalic(xSpreadsheet, startColumn, endColumn, row, row);

								row++;
								start++;
							}

							if(!comment.trim().equals("")){//Added by ping yang on 31/7/08 to get rid of extra '-'s
								OO.insertString(xSpreadsheet, "- " + UnicodeHelper.getUnicodeStringAmp(comment), row, column+1);
								OO.mergeCells(xSpreadsheet, column+1, endColumn, row, row);
								OO.setRowHeight(xSpreadsheet, row, column+1, ROWHEIGHT*OO.countTotalRow(comment, 85));
								OO.setCellAllignment(xSpreadsheet, startColumn, endColumn, row, row, 2, 1);
								row++;
								blnSupCommentExist = true;
							}


						}// end while sup comments

						start = 0;					

						if (supComments.size() > 0)
						{
							row++;
						}
					}// end if(blnSupIncluded)

					// Execute this section of codes only if there are Peers', Subordinates' or Others' comments
					if(blnOthIncluded){//added by Ping Yang on 11/08/08, check raters assigned

						/* Change: determine whether to show Others'comments based on splitOthers
						   Updated by: Qiao Li 23 dec 2009*/
						if (splitOthers==1){
							boolean blnPeerCommentExist = false; // Added by
							// ping yang
							// on
							// 31/7/08
							// to get
							// rid of
							// extra
							// '-'s
							for (int k = 0; k < peerComments.size(); k++) {
								String[] arr = (String[]) peerComments
								.elementAt(k);
								String comment = arr[1];

								// Added codes to insert peers' comments, Desmond 18 Nov 09
								// Insert Peers' comments
								if (start == 0) {
									//Changed the default language to English by Chun Yeong 9 Jun 2011
									//Commented away to allow translation below, Chun Yeong 1 Aug 2011
									/*if (ST.LangVer == 2) //Indonesian
										OO.insertString(xSpreadsheet, "Lainnya",
												row, column + 1);
									else //if (ST.LangVer == 1) English
										OO.insertString(xSpreadsheet, "Peer(s)",
												row, column + 1);*/

									//Allow dynamic translation, Chun Yeong 1 Aug 2011
									OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Peer(s)"),
											row, column + 1);
									OO.setFontBold(xSpreadsheet, startColumn,
											endColumn, row, row);
									OO.setFontItalic(xSpreadsheet, startColumn,
											endColumn, row, row);

									start++;
									row++;
								}

								if (!comment.trim().equals("")) {
									OO.insertString(xSpreadsheet, "- " + UnicodeHelper.getUnicodeStringAmp(comment),
											row, column + 1);
									OO.mergeCells(xSpreadsheet, column + 1,
											endColumn, row, row);
									OO.setRowHeight(xSpreadsheet, row, column + 1, 
											ROWHEIGHT * OO.countTotalRow(comment, 85));
									OO.setCellAllignment(xSpreadsheet,
											startColumn, endColumn, row, row,
											2, 1);

									row++;
									blnPeerCommentExist = true;
								}
							}

							// Adjust counters
							start = 0;

							if (peerComments.size() > 0)
							{
								row++;
							}

							boolean blnSubCommentExist = false; // Added by ping
							// yang on
							// 31/7/08 to
							// get rid of
							// extra '-'s
							for (int k = 0; k < subComments.size(); k++) {
								String[] arr = (String[]) subComments
								.elementAt(k);
								String comment = arr[1];

								// Added codes to insert subordinates' comments,
								// Desmond 18 Nov 09
								// Insert Subordinates' comments
								if (start == 0) {
									//Changed the default language to English by Chun Yeong 9 Jun 2011
									//Commented away to allow translation below, Chun Yeong 1 Aug 2011
									/*if (ST.LangVer == 2) //Indonesian
										OO.insertString(xSpreadsheet, "Lainnya",
												row, column + 1);
									else //if (ST.LangVer == 1) English
										OO.insertString(xSpreadsheet,
												"Subordinate(s)", row, column + 1);*/

									//Allow dynamic translation, Chun Yeong 1 Aug 2011
									OO.insertString(xSpreadsheet,
											trans.tslt(templateLanguage, "Subordinate(s)"), row, column + 1);
									OO.setFontBold(xSpreadsheet, startColumn,
											endColumn, row, row);
									OO.setFontItalic(xSpreadsheet, startColumn,
											endColumn, row, row);

									start++;
									row++;
								}

								if (!comment.trim().equals("")) {OO.insertString(
										xSpreadsheet,"- "+ UnicodeHelper.getUnicodeStringAmp(comment),
										row, column + 1);
								OO.mergeCells(xSpreadsheet, column + 1,
										endColumn, row, row);
								OO.setRowHeight(xSpreadsheet, row,
										column + 1, ROWHEIGHT
										* OO.countTotalRow(comment,
												85));
								OO.setCellAllignment(xSpreadsheet,
										startColumn, endColumn, row, row,
										2, 1);

								row++;
								blnSubCommentExist = true;
								}
							}

							if (subComments.size() > 0)
							{
								row++;
							}
							// Adjust counters
							start = 0;
						}
						// Added codes so that Others' comments (including the header "Others") is displayed only if at least Others' comment exists, Desmond, 18 Nov 09
						// Execute this portion of codes only if there are Others' comments, if not don't even print out the header "Others"
						/* Change: determine whether to show Others'comments based on splitOthers
						   Updated by: Qiao Li 23 dec 2009*/
						else{					
							boolean blnOthCommentExist = false;	//Added by ping yang on 31/7/08 to get rid of extra '-'s
							for(int k=0; k<othComments.size(); k++) {
								String [] arr = (String[])othComments.elementAt(k);
								String comment = arr[1];

								// Insert Others' comments
								if(start == 0) {
									//Changed the default language to English by Chun Yeong 9 Jun 2011
									//Commented away to allow translation below, Chun Yeong 1 Aug 2011
									/*if (ST.LangVer == 2) //Indonesian
										OO.insertString(xSpreadsheet, "Orang lain", row, column+1);
									else //if (ST.LangVer == 1) English
										OO.insertString(xSpreadsheet, "Others", row, column+1);*/	

									//Allow dynamic translation, Chun Yeong 1 Aug 2011
									OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Others"), row, column+1);
									OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
									OO.setFontItalic(xSpreadsheet, startColumn, endColumn, row, row);

									start++;
									row++;
								}

								if(!comment.trim().equals("")){
									OO.insertString(xSpreadsheet, "- " + UnicodeHelper.getUnicodeStringAmp(comment), row, column+1);
									OO.mergeCells(xSpreadsheet, column+1, endColumn, row, row);
									OO.setRowHeight(xSpreadsheet, row, column+1, ROWHEIGHT*OO.countTotalRow(comment, 85));
									OO.setCellAllignment(xSpreadsheet, startColumn, endColumn, row, row, 2, 1);

									row++;	
									blnOthCommentExist = true;
								}															
							}

							if (othComments.size() > 0)
							{
								row++;
							}
							start = 0;

						} // if(!othComments.isEmpty())
					}//end if(blnOthIncluded)

					if(selfIncluded == 1) {
						Vector selfComments = getComments("SELF", compID, KBID);

						boolean blnSelfCommentExist = false;//Added by ping yang on 31/7/08 to get rid of extra '-'s

						for(int k=0; k<selfComments.size(); k++) {
							String [] arr = (String[])selfComments.elementAt(k);				
							String comment = arr[1];

							if(start == 0) {
								//Changed the default language to English by Chun Yeong 9 Jun 2011
								//Commented away to allow translation below, Chun Yeong 1 Aug 2011
								/*if (ST.LangVer == 2) //Indonesian
									OO.insertString(xSpreadsheet, "Diri Sendiri", row, column+1);
								else // if(ST.LangVer == 1) English
									OO.insertString(xSpreadsheet, "Self", row, column+1);*/

								//Allow dynamic translation, Chun Yeong 1 Aug 2011
								OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Self"), row, column+1);
								OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
								OO.setFontItalic(xSpreadsheet, startColumn, endColumn, row, row);

								row++;
								start++;
							}

							if(!comment.trim().equals("")){//Added by ping yang on 31/7/08 to get rid of extra '-'s
								OO.insertString(xSpreadsheet, "- " + UnicodeHelper.getUnicodeStringAmp(comment), row, column+1);
								OO.mergeCells(xSpreadsheet, column+1, endColumn, row, row);
								OO.setRowHeight(xSpreadsheet, row, column+1, ROWHEIGHT*OO.countTotalRow(comment, 85));
								OO.setCellAllignment(xSpreadsheet, startColumn, endColumn, row, row, 2, 1); // Corrected the column range of cells that alignment is applied for self comments, Desmond 18 Nov 09
								row++;
								blnSelfCommentExist = true;//Added by ping yang on 31/7/08 to get rid of extra '-'s
							}
						}

						if (selfComments.size() > 0)
						{
							row++;
						}
					}
					/*
					 * Change(s) : Give border and page break for KB
					 * Reason(s) : geting row for previous KB not this KB 
					 * Updated By: Johanes 
					 * Updated On: 02 Nov 2009
					 */
					if (endBorder == 1)
						endBorder1 = row;
					else
						endBorder1 = endBorder;
					endBorder = row;
					row++;

					//Check height and insert pagebreak where necessary
					int pageHeightLimit = 22272	;//Page limit is 22272						    
					int tableHeight = 0;


					//calculate the height of the table that is being added.
					for(int i1=startBorder;i1<=endBorder;i1++){
						int rowToCalculate = i1;

						tableHeight += OO.getRowHeight(xSpreadsheet, rowToCalculate, startColumn);
					}
					currentPageHeight = currentPageHeight + tableHeight; //add new table height to current pageheight.
					//Denise 08/01/2009 to move the table two lines down
					int dis = 2; 
					if(currentPageHeight >pageHeightLimit){//adding the table will exceed a single page, insert page break		
						OO.insertRows(xSpreadsheet, startColumn, endColumn, startBorder, startBorder+dis, dis, 0);
						OO.setTableBorder(xSpreadsheet, startColumn, endColumn, startBorder1, endBorder1, 
								false, false, true, true, true, true);
						insertPageBreak(xSpreadsheet, startColumn, endColumn, startBorder);
						endBorder +=dis;
						//	OO.insertRow(xSpreadsheet, startColumn, endColumn, 2);
						row +=dis;
						startBorder +=dis;
						startBorder1 = startBorder;
						lastKB = checkKB;
						currentPageHeight = tableHeight;
					}					
					startBorder = row;

				} // kb

				/*
				 * Change(s) : Give border and page break for KB
				 * Reason(s) : give border for last page, without this code last page never have border 
				 * Updated By: Johanes 
				 * Updated On: 02 Nov 2009
				 */
				/* Change: remove lastKB != KBList.size() 
				 * Reason: Sometimes the borders are not added when lastKB == KBList.size()
				 * Updated By: Qiao Li 29 Dec 2009
				 */
				if(check == vComp.size() /*&& lastKB != KBList.size()*/){
					OO.setTableBorder(xSpreadsheet, startColumn, endColumn, startBorder1, endBorder, 
							false, false, true, true, true, true);	
				}
			}

		}

		// ----------------- END KB LEVEL SURVEY ----------------- //

	} // End of InsertComments()

	/*********************** GENERATES ALL REPORTS ************************************************************/

	public int NameSequence(int orgID) throws Exception {

		String query = "Select NameSequence from tblOrganization where PKOrganization = " + orgID;

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		int iNameSeq = 0;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			if(rs.next()) {
				iNameSeq = rs.getInt(1);
			}

		}catch(Exception ex){
			System.out.println("IndividualReport.java - NameSequence - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return iNameSeq;
	}

	/**
	 * Retrieves all targets under the particular division, department or group.
	 * Group = 0: all targets all groups
	 * @param surveyID
	 * @param divID
	 * @param deptID
	 * @param groupID
	 * @param orgID
	 * @return Vector
	 * @throws Exception
	 */
	public Vector AllTargets(int surveyID, int divID, int deptID, int groupID, int orgID) throws Exception
	{
		int nameSeq = NameSequence(orgID);

		String query = "SELECT DISTINCT Asg.SurveyID, S.SurveyName, Asg.TargetLoginID, ";

		if(nameSeq == 0)
			query += "U.FamilyName + ' ' + U.GivenName AS FullName ";
		else
			query += "U.GivenName + ' ' +  U.FamilyName as FullName ";

		query += "FROM [User] U INNER JOIN tblAssignment Asg ON U.PKUser = Asg.TargetLoginID INNER JOIN tblSurvey S ON Asg.SurveyID = S.SurveyID ";
		query += "WHERE Asg.SurveyID = " + surveyID;

		if(divID > 0)
			query += " AND Asg.FKTargetDivision = " + divID;

		if(deptID > 0)
			query += " AND Asg.FKTargetDepartment = " + deptID;

		if(groupID > 0)
			query += " AND Asg.FKTargetGroup = " + groupID;

		query = query + " ORDER BY FullName";

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		Vector vTargets = new Vector();

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			while(rs.next()) 
			{	
				String surveyName = rs.getString("SurveyName").trim();
				String targetID = rs.getString("TargetLoginID");
				String name = rs.getString("FullName").trim();

				String [] info = {surveyName, targetID, name};

				vTargets.add(info);
			}

		}catch(Exception ex){
			System.out.println("IndividualReport.java - AllTargets - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return vTargets;
	}

	/**
	 * Get the competency avg mean for CP and CPR.
	 * The table to store Competency Level and KB Level Result are not the same.
	 *
	 * For Competency Level:
	 *		ReliabilityIndex is stored in tblAvgMean
	 *		TrimmedMean is stored in tblTrimmedMean
	 *
	 * For KBLevel, both are stored in tblAvgMean
	 *
	 * This is a little bit messy, wrong design previously.
	 *
	 */
	public Vector getCompAvg(int surveyID, Vector vCompID, String rtCode) throws SQLException
	{
		int surveyLevel 		= C.LevelOfSurvey(surveyID);
		int reliabilityCheck 	= C.ReliabilityCheck(surveyID); // 0=trimmed mean

		String query 		= "";
		String tableName 	= "";
		String columnName 	= "";

		Vector vCompCP 	= new Vector();
		Vector vScore 	= new Vector();
		Vector vResult = new Vector();

		if(surveyLevel == 1) {	//KB Level

			query = "SELECT tblAvgMean.CompetencyID, ROUND(AVG(tblAvgMean.AvgMean), 2) AS AvgMean ";
			query += "FROM tblAvgMean INNER JOIN tblRatingTask ON tblAvgMean.RatingTaskID = tblRatingTask.RatingTaskID ";
			query += "WHERE tblAvgMean.SurveyID = " + surveyID;
			query += " AND (tblAvgMean.Type = 1) AND (tblRatingTask.RatingCode = '" + rtCode + "') ";
			query += " and CompetencyID IN (";

			for(int i=0; i<vCompID.size(); i++) {
				if(i != 0)
					query += ",";

				query += vCompID.elementAt(i);
			}

			query += ")";
			query += " GROUP BY tblAvgMean.CompetencyID";

		} else {	// Competency Level

			if(reliabilityCheck == 0) {
				tableName = "tblTrimmedMean";
				columnName = "TrimmedMean";
			}
			else {
				tableName = "tblAvgMean";
				columnName = "AvgMean";
			}

			query = "SELECT " + tableName + ".CompetencyID, ROUND(AVG(" + tableName + "." + columnName + "), 2) AS AvgMean ";
			query += "FROM " + tableName + " INNER JOIN tblRatingTask ON ";
			query += tableName + ".RatingTaskID = tblRatingTask.RatingTaskID WHERE ";
			query += tableName + ".Type = 1 AND " + tableName + ".SurveyID = " + surveyID;
			query += " AND tblRatingTask.RatingCode = 'CP' ";

			query += " and CompetencyID IN (";

			for(int i=0; i<vCompID.size(); i++) {
				if(i != 0)
					query += ",";

				query += vCompID.elementAt(i);
			}

			query += ") ";

			query += "GROUP BY " + tableName + ".CompetencyID";
		}

		//if(db.con == null)
		//db.openDB();

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;


		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			while(rs.next()) {			
				String fkComp 	= rs.getString("CompetencyID");
				String score	= rs.getString("AvgMean");

				vCompCP.add(fkComp);
				vScore.add(score);
			}


		}catch(Exception ex){
			System.out.println("IndividualReport.java - getCompAvg - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}


		//copy all the score into the correct order
		for(int i=0; i<vCompID.size(); i++) 
		{	
			String score = "0";

			String sCompScore = (String)vCompID.elementAt(i).toString();
			int element = vCompCP.indexOf(sCompScore);

			if(element != -1)
				score = (String)vScore.elementAt(element);

			vResult.add(score);
		}

		return vResult;
	}

	/**
	 * Overall Competency Ratings (Allianz)
	 * @param iSurvey
	 * @throws IOException
	 * @throws Exception
	 */
	public void AllianzOverall() throws IOException, Exception
	{
		//System.out.println("4. Overall Report");
		int [] address = OO.findString(xSpreadsheet, "<overall>");

		column = address[0];
		row = address[1];

		OO.findAndReplace(xSpreadsheet, "<overall>", "");

		insertRatingScale(column, row);
		overallGraph();
	}

	public Vector getRatingScaleDescending(int RatingScaleID) {

		Vector v = new Vector();

		String query = "SELECT * FROM tblScaleValue WHERE ScaleID = " + RatingScaleID;
		query = query + " ORDER BY LowValue DESC, HighValue DESC";

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try
		{          
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			while(rs.next())
			{
				votblScaleValue vo = new votblScaleValue();
				vo.setHighValue(rs.getInt("HighValue"));
				vo.setLowValue(rs.getInt("LowValue"));
				vo.setScaleDescription(rs.getString("ScaleDescription"));
				v.add(vo);
			}

		}
		catch(Exception E) 
		{
			System.err.println("IndividualReport.java - getRatingScaleDescending - " + E);
		}
		finally
		{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return v;
	}

	/**
	 * Insert Rating Scale (Allianz)
	 * @param iCol
	 * @param iRow
	 * @param iSurveyID
	 * @throws SQLException
	 * @throws IOException
	 * @throws Exception
	 * @author Maruli
	 */
	public void insertRatingScale(int iCol, int iRow) throws SQLException, IOException, Exception 
	{
		//System.out.println("	- Printing Rating Scales");

		int count = 0;
		int maxScale = EQ.maxScale(surveyID) + 1;
		int totalCells = totalColumn / maxScale;
		int totalMerge = 0;		// total cells to be merged after rounding
		double merge = 0;		// total cells to be merged before rounding

		Vector RT = EQ.SurveyRating(surveyID);		

		for(int j=0; j<RT.size(); j++) {

			votblSurveyRating vo = (votblSurveyRating)RT.elementAt(j);
			count++;

			int scaleID = vo.getScaleID();
			int c = 1;
			int r = iRow;
			int start = 0;

			Vector RS = getRatingScaleDescending(scaleID);

			for(int i=0; i<RS.size(); i++)
			{
				votblScaleValue voScale = (votblScaleValue)RS.elementAt(i);
				int low = voScale.getLowValue();
				int high = voScale.getHighValue();
				String desc = voScale.getScaleDescription();

				OO.insertString(xSpreadsheet, desc, iRow, column+1);	// add in scale description
				OO.setFontBold(xSpreadsheet, column+1, column+1, iRow, iRow);
				OO.setCellAllignment(xSpreadsheet, column+1, column+1, iRow, iRow, 1, 2);
				OO.setCellAllignment(xSpreadsheet, column+1, column+1, iRow, iRow, 2, 2);

				r = iRow + 1;
				c = column;

				start = c; // start merge cell
				String temp = "";

				while(low <= high) {						
					if(low > 1)
						temp += "    ";
					temp = temp + Integer.toString(low);

					low++;						
				}

				OO.insertString(xSpreadsheet, temp.trim(), r, c+1);	// add in rating scale value
				OO.setCellAllignment(xSpreadsheet, c+1, c+1, r, r, 1, 2);

				int to = start+totalCells-1;	// merge cell for rating scale value
				OO.mergeCells(xSpreadsheet, start+1, to+1, r, r);
				OO.setTableBorder(xSpreadsheet, start+1, to+1, r, r, true, true, true, true, true, true);
				OO.setFontSize(xSpreadsheet, start+1, to+1, r, r, 10);

				OO.mergeCells(xSpreadsheet, start+1, to+1, iRow, iRow);	// merge cell for rating scale description
				OO.setTableBorder(xSpreadsheet, start+1, to+1, iRow, iRow, true, true, true, true, true, true);
				OO.setBGColor(xSpreadsheet, start+1, to+1, iRow, iRow, BGCOLOR);

				merge = (double)desc.length() / (double)(totalCells * 2);				

				BigDecimal BD = new BigDecimal(merge);
				BD.setScale(0, BD.ROUND_UP);
				BigInteger BI = BD.toBigInteger();
				totalMerge = BI.intValue() + 1;

				column = to + 1;
			}

			OO.setRowHeight(xSpreadsheet, iRow, start+1, (300 * totalMerge));

			row = r + 3;					
			column = 0;				
		}	
	}

	/**
	 * Calculate CP score. OTHERS are further break down into relation specific (Allianz)
	 * @throws SQLException
	 * @throws IOException
	 * @throws Exception
	 * @author Maruli
	 */
	public void overallGraph() throws SQLException, IOException, Exception
	{
		//System.out.println("	- Working on overall graph");
		int iNoOfRT 	= 0;
		String RTCode 	= "";

		Vector RT 	= RatingTask();
		Vector CP 	= null;
		Vector CPR 	= null;
		int type 		= 1;

		for(int j=0; j<RT.size(); j++) {
			votblSurveyRating vo = (votblSurveyRating)RT.elementAt(j);

			RTCode 	=  vo.getRatingCode();

			if(RTCode.equals("CP") || RTCode.equals("CPR") || RTCode.equals("FPR")) {
				if(RTCode.equals("CP"))
					CP = CPCPR(RTCode);
				else {
					CPR = CPCPR(RTCode);
					if(RTCode.equals("CPR")) {
						type = 1;
						iNoCPR = 0;	// CPR is chosen in this survey
					} else {
						type = 2;
					}
				}
			}
			iNoOfRT++;
		}

		vCompID.clear();
		vCompName.clear();

		Vector vComp = CompetencyByName();

		int iLocalR = 1;
		int iLocalC = 0;

		double dCPSelf = 0;
		double dCPSup = 0;
		double dCPPeer = 0;
		double dCPDR = 0;
		double dCPBIP = 0;
		//double dCPR = 0;

		for(int i=0; i<vComp.size(); i++) {
			voCompetency voComp = (voCompetency)vComp.elementAt(i);

			int compID 		= voComp.getCompetencyID();
			String compName = UnicodeHelper.getUnicodeStringAmp(voComp.getCompetencyName());

			dCPSelf = CPCPRAllianz(1, compID, "SELF", "");
			dCPSup = CPCPRAllianz(1, compID, "SUP%", "");
			dCPPeer = CPCPRAllianz(1, compID, "OTH%", "Peer");
			dCPDR = CPCPRAllianz(1, compID, "OTH%", "Direct Report");
			dCPBIP = CPCPRAllianz(1, compID, "OTH%", "BIP Project Champion");
			//dCPR 	= 0;

			//			if(CP != null && CP.next())
			//				dCP = CP.getDouble("Result");

			//			if(CPR != null && CPR.next())
			//				dCPR = CPR.getDouble("Result");

			//System.out.println(dCP);

			vCompID.add(new Integer(compID));
			vCompName.add(new String(compName));

			OO.insertString(xSpreadsheet2, compName, iLocalR, iLocalC);
			OO.insertNumeric(xSpreadsheet2, dCPDR, iLocalR, iLocalC+1);
			OO.insertNumeric(xSpreadsheet2, dCPPeer, iLocalR, iLocalC+2);
			OO.insertNumeric(xSpreadsheet2, dCPBIP, iLocalR, iLocalC+3);
			OO.insertNumeric(xSpreadsheet2, dCPSup, iLocalR, iLocalC+4);
			OO.insertNumeric(xSpreadsheet2, dCPSelf, iLocalR, iLocalC+5);
			//OO.insertNumeric(xSpreadsheet2, dCPR, iLocalR, iLocalC+2);

			iLocalR++;
		}

		row = iLocalR + 2;
		OO.setSourceData(xSpreadsheet, xSpreadsheet2, 0, iLocalC, iLocalC+5, 0, iLocalR-1);
	}

	/**
	 * Generate competency report (Allianz)
	 * @throws IOException
	 * @throws Exception
	 */
	public void AllianzCompReport() throws IOException, Exception
	{
		//System.out.println("5. Printing Competency Report");

		int [] address = OO.findString(xSpreadsheet, "<competency>");

		column = address[0];
		row = address[1];

		OO.findAndReplace(xSpreadsheet, "<competency>", "");

		int iCompID = 0;
		String sCompName = "";

		for(int i=0; i<vCompID.size(); i++) 
		{
			iCompID = Integer.parseInt( ((String)vCompID.elementAt(i).toString()) );
			sCompName = (String)vCompName.elementAt(i).toString();

			//OO.insertRows(xSpreadsheet, 0, 1, row, row+1, 1, 1);

			OO.insertString(xSpreadsheet, sCompName.toUpperCase(), row, 0);
			OO.setFontBold(xSpreadsheet, column, column + 3, row, row);
			OO.setFontSize(xSpreadsheet, startColumn, endColumn, row, row, 12);

			row = row + 2;
			//OO.insertRows(xSpreadsheet, 0, 1, row, row+2, 2, 1);

			OO.setFontSize(xSpreadsheet, startColumn, endColumn, row, row, 10);
			insertRatingScale(column, row);

			//OO.insertRows(xSpreadsheet, 0, 1, row, row+19, 17, 1);
			drawCompetencyChartAllianz(iCompID, sCompName);
			printKBAllianz(iCompID);
		}
	}

	/**
	 * Draw competency charts (Allianz)
	 * @param iCompID
	 * @param sCompName
	 * @throws IOException
	 * @throws Exception
	 * @author Maruli
	 */
	public void drawCompetencyChartAllianz(int iCompID, String sCompName) throws IOException, Exception
	{
		int iKBID = 0;
		int iLocalC = 0;
		int startRow = row;
		int iLocalR = row;
		int iLocCounter = 1;
		int iMaxScale = MaxScale();

		double dCPSelf = 0;
		double dCPSup = 0;
		double dCPPeer = 0;
		double dCPDR = 0;
		double dCPBIP = 0;

		String sKB = "";

		Vector KBList = KBList(iCompID);

		OO.insertString(xSpreadsheet2, "Direct Report", iLocalR, iLocalC+1);
		OO.insertString(xSpreadsheet2, "Peer", iLocalR, iLocalC+2);
		OO.insertString(xSpreadsheet2, "BIP Champion", iLocalR, iLocalC+3);
		OO.insertString(xSpreadsheet2, "Supervisor", iLocalR, iLocalC+4);
		OO.insertString(xSpreadsheet2, "Self", iLocalR, iLocalC+5);

		iLocalR++;

		for(int j=0; j<KBList.size(); j++) {
			voKeyBehaviour voKB = (voKeyBehaviour)KBList.elementAt(j);
			iKBID = voKB.getKeyBehaviourID();
			sKB = voKB.getKeyBehaviour();

			dCPSelf = getAvgBehvScoreAllianz(iKBID, "SELF", "");
			dCPSup = getAvgBehvScoreAllianz(iKBID, "SUP%", "");
			dCPPeer = getAvgBehvScoreAllianz(iKBID, "OTH%", "Peer");
			dCPDR = getAvgBehvScoreAllianz(iKBID, "OTH%", "Direct Report");
			dCPBIP = getAvgBehvScoreAllianz(iKBID, "OTH%", "BIP Project Champion");

			OO.insertString(xSpreadsheet2, "Q"+iLocCounter, iLocalR, iLocalC);
			OO.insertNumeric(xSpreadsheet2, dCPDR, iLocalR, iLocalC+1);
			OO.insertNumeric(xSpreadsheet2, dCPPeer, iLocalR, iLocalC+2);
			OO.insertNumeric(xSpreadsheet2, dCPBIP, iLocalR, iLocalC+3);
			OO.insertNumeric(xSpreadsheet2, dCPSup, iLocalR, iLocalC+4);
			OO.insertNumeric(xSpreadsheet2, dCPSelf, iLocalR, iLocalC+5);

			iLocalR++;
			iLocCounter++;
		}

		// Draw chart
		XTableChart xtablechart = OO.getChart(xSpreadsheet, xSpreadsheet2, iLocalC, iLocalC+5, startRow, iLocalR-1, 
				Integer.toString(row), 15050, 9000, startRow, 1);
		xtablechart = OO.setChartTitle(xtablechart, "");
		xtablechart = OO.setAxes(xtablechart, "Competency", "", iMaxScale, 1, 0, 0);
		OO.setChartProperties(xtablechart, false, false, true, false, true);
		OO.setLegendPosition(xtablechart, 4);
		OO.changeChartType("com.sun.star.chart.LineDiagram", xtablechart);
		//OO.changeChartLineColours(1);
	}

	/**
	 * Print out KB under each competency graph (Allianz)
	 * @param iCompID
	 * @throws IOException
	 * @throws Exception
	 */
	public void printKBAllianz(int iCompID) throws IOException, Exception
	{
		int iLocalC = 1;
		row = row + 18;
		int iStartRow = row;
		int iLocCounter = 1;

		int iStartCol = 1;
		int iEndCol = 10;
		String sKB = "";

		Vector KBList = KBList(iCompID);

		for(int j=0; j<KBList.size(); j++) {
			voKeyBehaviour voKB = (voKeyBehaviour)KBList.elementAt(j);
			sKB = voKB.getKeyBehaviour();

			OO.mergeCells(xSpreadsheet, 2, iEndCol, row, row);
			OO.insertString(xSpreadsheet, "Q"+iLocCounter, row, iLocalC);
			OO.insertString(xSpreadsheet, sKB, row, 2);

			OO.setRowHeight(xSpreadsheet, row, 1, 510*OO.countTotalRow(sKB, 92));
			OO.setTableBorder(xSpreadsheet, iStartCol, iEndCol, row, row, true, true, true, true, true, true);

			row++;
			iLocCounter++;
		}

		OO.setFontSize(xSpreadsheet, startColumn, endColumn, iStartRow, row, 10);
		insertPageBreak(xSpreadsheet, startColumn, endColumn, row);
	}


	/**
	 * Get CP for competency graphs (Allianz)
	 * @param iKBID
	 * @param sRelHigh
	 * @param sRelSpec
	 * @return
	 * @throws IOException
	 * @throws Exception
	 */
	public double getAvgBehvScoreAllianz(int iKBID, String sRelHigh, String sRelSpec) throws IOException, Exception
	{
		double dAvgCP = 0; 

		String query = "SELECT RB.RatingTaskID, KB.FKCompetency, RB.KeyBehaviourID, CAST(CAST(SUM(RB.Result) AS float) / COUNT(RB.Result) AS numeric(38, 2)) ";
		query = query + "AS Result, RS.RelationSpecific ";
		query = query + "FROM tblAssignment ASG INNER JOIN ";
		query = query + "tblResultBehaviour RB ON ASG.AssignmentID = RB.AssignmentID INNER JOIN ";
		query = query + "KeyBehaviour KB ON RB.KeyBehaviourID = KB.PKKeyBehaviour LEFT OUTER JOIN ";
		query = query + "tblRelationSpecific RS ON ASG.RTSpecific = RS.SpecificID ";
		query = query + "WHERE (ASG.SurveyID = "+surveyID+") AND (ASG.TargetLoginID = "+targetID+") AND ";
		query = query + "(ASG.RaterStatus IN (1, 2, 4)) AND (ASG.RaterCode LIKE '"+sRelHigh+"') AND (RB.Result <> 0) ";
		query = query + "GROUP BY RB.RatingTaskID, KB.FKCompetency, RB.KeyBehaviourID, RS.RelationSpecific ";
		query = query + "HAVING (RB.RatingTaskID = 1) AND (RB.KeyBehaviourID = "+iKBID+") ";

		if(sRelSpec != "")
			query = query + "AND (RS.RelationSpecific = '"+sRelSpec+"') ";

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);

			if(rs.next())
				dAvgCP = rs.getDouble("Result");

		}catch(Exception ex){
			System.out.println("IndividualReport.java - getAvgBehvScoreAllianz - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}


		return dAvgCP;
	}

	/**
	 * Print out comments (Allianz)
	 * @throws SQLException
	 * @throws IOException
	 * @throws Exception
	 */
	public void InsertCommentsAllianz() throws SQLException, IOException, Exception
	{
		//System.out.println("6. Printing Comments");

		int startBorder = 1;
		int endBorder = 1;
		int selfIncluded = Q.SelfCommentIncluded(surveyID);
		int column = 0;
		int iCompID = 0;
		int count = 0;
		int iLocEndCol = 10;
		String sCompName = "";

		row++;

		OO.insertString(xSpreadsheet, "FEEDBACK COMMENTS", row, column);								
		OO.setFontBold(xSpreadsheet, column, column+1, row, row);
		OO.setBGColor(xSpreadsheet, column, column+10, row, row, BGCOLOR);
		OO.setFontSize(xSpreadsheet, column, column+10, row, row, 14);

		row = row + 2;
		startBorder = row;
		OO.setFontSize(xSpreadsheet, column, iLocEndCol, row, row, 12);

		for(int i=0; i<vCompID.size(); i++)
		{	
			count++;
			iCompID = Integer.parseInt( ((String)vCompID.elementAt(i).toString()) );
			sCompName = (String)vCompName.elementAt(i).toString();

			OO.insertString(xSpreadsheet, Integer.toString(count) + ".", row, column);								
			OO.insertString(xSpreadsheet, sCompName, row, column+1);
			OO.setFontBold(xSpreadsheet, startColumn, iLocEndCol, row, row);
			OO.setBGColor(xSpreadsheet, startColumn, iLocEndCol, row, row, BGCOLOR);

			int start = 0;
			row++;

			if(selfIncluded == 1) 
			{
				Vector selfComments = getCommentsAllianz("SELF", iCompID);

				for(int j=0; j<selfComments.size(); j++) {
					String [] arr = (String[])selfComments.elementAt(j);
					if(start == 0) 
					{
						row++;
						OO.insertString(xSpreadsheet, "Self", row, column+1);

						OO.setFontBold(xSpreadsheet, startColumn, iLocEndCol, row, row);
						OO.setFontItalic(xSpreadsheet, startColumn, iLocEndCol, row, row);

						row++;
						start++;
					}

					String comment = (arr[1]).trim();

					OO.insertString(xSpreadsheet, "-", row, column);
					OO.setCellAllignment(xSpreadsheet, column, column, row, row, 1, 3);

					OO.mergeCells(xSpreadsheet, column+1, iLocEndCol, row, row);
					OO.insertString(xSpreadsheet, UnicodeHelper.getUnicodeStringAmp(comment), row, column+1);
					OO.setRowHeight(xSpreadsheet, row, column+1, ROWHEIGHT*OO.countTotalRow(comment, 85));
					OO.setCellAllignment(xSpreadsheet, startColumn, startColumn, row, row, 2, 1);
					row++;		
				}
			}

			Vector supComments = getCommentsAllianz("SUP%", iCompID);
			Vector othComments = getCommentsAllianz("OTH%", iCompID);

			start = 0;

			for(int j=0; j<supComments.size(); j++) {
				String [] arr = (String[])supComments.elementAt(j);

				if(start == 0) 
				{
					row++;
					OO.insertString(xSpreadsheet, "Supervisor", row, column+1);
					OO.setFontBold(xSpreadsheet, startColumn, iLocEndCol, row, row);
					OO.setFontItalic(xSpreadsheet, startColumn, iLocEndCol, row, row);

					row++;
					start++;
				}

				String comment = (arr[1]).trim();
				OO.insertString(xSpreadsheet, "-", row, column);
				OO.setCellAllignment(xSpreadsheet, column, column, row, row, 1, 3);

				OO.mergeCells(xSpreadsheet, column+1, iLocEndCol, row, row);
				OO.insertString(xSpreadsheet, UnicodeHelper.getUnicodeStringAmp(comment), row, column+1);
				OO.setRowHeight(xSpreadsheet, row, column+1, ROWHEIGHT*OO.countTotalRow(comment, 85));
				OO.setCellAllignment(xSpreadsheet, startColumn, startColumn, row, row, 2, 1);
				row++;
			}

			start = 0;

			for(int j=0; j<othComments.size(); j++) {
				String [] arr = (String[])othComments.elementAt(j);					
				if(start == 0) 
				{
					row++;
					OO.insertString(xSpreadsheet, "Others (Peers, Direct Reports)", row, column+1);
					OO.setFontBold(xSpreadsheet, startColumn, iLocEndCol, row, row);
					OO.setFontItalic(xSpreadsheet, startColumn, iLocEndCol, row, row);

					start++;
					row++;
				}	

				String comment = (arr[1]).trim();
				OO.insertString(xSpreadsheet, "-", row, column);
				OO.setCellAllignment(xSpreadsheet, column, column, row, row, 1, 3);

				OO.mergeCells(xSpreadsheet, column+1, iLocEndCol, row, row);
				OO.insertString(xSpreadsheet, UnicodeHelper.getUnicodeStringAmp(comment), row, column+1);
				OO.setRowHeight(xSpreadsheet, row, column+1, ROWHEIGHT*OO.countTotalRow(comment, 85));
				OO.setCellAllignment(xSpreadsheet, startColumn, startColumn, row, row, 2, 1);
				row++;
			}

			row++;

			endBorder = row;
			OO.setTableBorder(xSpreadsheet, startColumn, iLocEndCol, startBorder, endBorder, 
					false, false, true, true, true, true);								
			row++;

			//Insert page break for each competency comments
			insertPageBreak(xSpreadsheet, startColumn, endColumn, row);
			row++;
			startBorder = row;
		}
	}

	/**
	 * Draw the development map
	 * @param iSurvey
	 * @param iTarget
	 * @throws SQLException
	 * @throws IOException
	 * @throws Exception
	 * @see IndividualReport1#reportDevelopmentMap(int, int, String)
	 * @author Maruli
	 */	
	public void drawDevelopmentMap() throws SQLException, IOException, Exception
	{
		/*	  	   	   	   +ve
		 *     	       Q3 	|   Q4
		 *    (-ve) --------|-------- (+ve)
		 *             Q1 	|   Q2
		 *				   -ve
		 *    
		 *  Quadrant classification
		 *  Q1.	Competency gap above 0 with all KB gap above 0
		 *	Q2.	Competency gap above 0 with one or more KB gap below 0
		 *	Q3.	Competency gap below 0 with one or more KB gap above 0
		 *	Q4.	Competency gap below 0 with all KB gap below 0
		 */
		//System.out.println("4. Drawing Development Map grid");

		double dCompGap = 0;
		double dBehvGap = 0;

		int iQ1 = 0;
		int iQ2 = 0;
		int iQ3 = 0;
		int iQ4 = 0;

		Q1.removeAllElements();
		Q2.removeAllElements();
		Q3.removeAllElements();
		Q4.removeAllElements();

		//double dMinGap = 0;
		//double [] arrGap = getMinMaxGap();		
		//dMinGap = arrGap[0];

		// PROCESSING SECTION
		int iCompID = 0;
		int iCompRank = 0;
		String sComp = "";

		Vector rsComp = SR.getCompList(this.surveyID);
		Vector rsBehv = null;

		for(int i=0; i<rsComp.size(); i++) 
		{
			voCompetency vo = (voCompetency)rsComp.elementAt(i);
			iQ1 = 0;
			iQ2 = 0;
			iQ3 = 0;
			iQ4 = 0;

			iCompID = vo.getCompetencyID();
			sComp = vo.getCompetencyName();
			iCompRank = vo.getRank();

			// Get Competency's Gap
			dCompGap = SR.getCompAvgGapDevMap(this.surveyID, this.targetID, iCompID);

			if(dCompGap < 0)	// Negative competency
			{
				// Q1 or Q2
				rsBehv = SR.getBehaviourGapDevMap(this.surveyID, this.targetID, iCompID);

				for(int j=0; j<rsBehv.size(); j++)
				{

					String [] arr = (String []) rsBehv.elementAt(j);
					dBehvGap = Double.parseDouble(arr[0]);

					if(dBehvGap < 0)					
						iQ1++;
					else
					{
						// Since 1 KB is already +ve (Q2), break the loop
						iQ2++;
						break;
					}
				}

				if(iQ2 > 0)
					Q2.add(new String [] { Integer.toString(iCompID), sComp, Double.toString(dCompGap), 
							Integer.toString(iCompRank) });
				else
					Q1.add(new String [] { Integer.toString(iCompID), sComp, Double.toString(dCompGap), 
							Integer.toString(iCompRank) });
			}			
			else	// Positive competency
			{
				// Q3 or Q4
				rsBehv = SR.getBehaviourGapDevMap(this.surveyID, this.targetID, iCompID);

				for(int j=0; j<rsBehv.size(); j++)
				{

					String [] arr = (String []) rsBehv.elementAt(j);

					dBehvGap = Double.parseDouble(arr[0]);

					if(dBehvGap < 0)
					{
						// Since 1 KB is already -ve (Q3), break the loop
						iQ3++;
						break;
					}
					else
						iQ4++;
				}

				if(iQ3 > 0)
					Q3.add(new String [] { Integer.toString(iCompID), sComp, Double.toString(dCompGap),
							Integer.toString(iCompRank) });
				else
					Q4.add(new String [] { Integer.toString(iCompID), sComp, Double.toString(dCompGap),
							Integer.toString(iCompRank) });
			}
		}

		// END OF PROCESSING SECTION

		// DRAWING SECTION		
		int i = 0;	// For loop
		int iBigTotRow = 0;
		int iSmallTotRow = 0;
		String sBiggerQuad = "";
		String sSmallerQuad = "";
		String sBigQuadReplacement = "";
		String sSmallQuadReplacement = "";

		// Draw the upper part of the map (Q3 & Q4)
		// We need to know how many rows to insert.
		// Find out which one has more elements in it (Q3 or Q4). Store in iTotRow		
		if(Q3.size() > Q4.size())
		{
			iBigTotRow = Q3.size();	// Q3 is bigger
			iSmallTotRow = Q4.size();
			sBiggerQuad = "<q3>";
			sSmallerQuad = "<q4>";
			sBigQuadReplacement = "Q3 - STRENGTHEN";
			sSmallQuadReplacement = "Q4 - SUSTAIN";
		}
		else if(Q4.size() > Q3.size())
		{
			iBigTotRow = Q4.size();	// Q4 is bigger
			iSmallTotRow = Q3.size();
			sBiggerQuad = "<q4>";
			sSmallerQuad = "<q3>";
			sBigQuadReplacement = "Q4 - SUSTAIN";
			sSmallQuadReplacement = "Q3 - STRENGTHEN";
		}
		else	// Same size
		{
			iBigTotRow = Q4.size();	// Equal size, whichever can be used
			iSmallTotRow = Q3.size();
			sBiggerQuad = "<q4>";
			sSmallerQuad = "<q3>";
			sBigQuadReplacement = "Q4 - SUSTAIN";
			sSmallQuadReplacement = "Q3 - STRENGTHEN";
		}

		int [] address = OO.findString(xSpreadsheet, sBiggerQuad);

		column = address[0];
		row = address[1];

		OO.findAndReplace(xSpreadsheet, sBiggerQuad, sBigQuadReplacement);
		row = row + 2;
		int iInitRow = row;

		for(i=0; i<iBigTotRow; i++)
		{
			OO.insertRows(xSpreadsheet, startColumn, endColumn, row-1, row, 1, 1);

			if(sBiggerQuad.equals("<q4>"))
				OO.insertString(xSpreadsheet, ((String [])Q4.elementAt(i))[1], row, column);
			else
				OO.insertString(xSpreadsheet, ((String [])Q3.elementAt(i))[1], row, column);

			row++;
		}

		address = OO.findString(xSpreadsheet, sSmallerQuad);

		column = address[0];
		row = address[1];

		OO.findAndReplace(xSpreadsheet, sSmallerQuad, sSmallQuadReplacement);

		for(i=0; i<iSmallTotRow; i++)
		{
			if(sSmallerQuad.equals("<q4>"))
				OO.insertString(xSpreadsheet, ((String [])Q4.elementAt(i))[1], iInitRow, column);
			else
				OO.insertString(xSpreadsheet, ((String [])Q3.elementAt(i))[1], iInitRow, column);

			iInitRow++;
		}

		// Draw the bottom part of the map (Q1 & Q2)
		if(Q1.size() > Q2.size())
		{
			iBigTotRow = Q1.size();	// Q1 is bigger
			iSmallTotRow = Q2.size();
			sBiggerQuad = "<q1>";
			sSmallerQuad = "<q2>";
			sBigQuadReplacement = "Q1 - ACQUIRE";
			sSmallQuadReplacement = "Q2 - INVEST";
		}
		else if(Q2.size() > Q1.size())
		{
			iBigTotRow = Q2.size();	// Q2 is bigger
			iSmallTotRow = Q1.size();
			sBiggerQuad = "<q2>";
			sSmallerQuad = "<q1>";
			sBigQuadReplacement = "Q2 - INVEST";
			sSmallQuadReplacement = "Q1 - ACQUIRE";
		}
		else	// Same size
		{
			iBigTotRow = Q2.size();	// Equal size, whichever can be used
			iSmallTotRow = Q1.size();
			sBiggerQuad = "<q2>";
			sSmallerQuad = "<q1>";
			sBigQuadReplacement = "Q2 - INVEST";
			sSmallQuadReplacement = "Q1 - ACQUIRE";
		}

		address = OO.findString(xSpreadsheet, sBiggerQuad);

		column = address[0];
		row = address[1];

		OO.findAndReplace(xSpreadsheet, sBiggerQuad, sBigQuadReplacement);
		row = row - 1;
		iInitRow = row;

		for(i=0; i<iBigTotRow; i++)
		{
			OO.insertRows(xSpreadsheet, column, column, row, row+1, 1, 1);

			if(sBiggerQuad.equals("<q2>"))
				OO.insertString(xSpreadsheet, ((String [])Q2.elementAt(i))[1], row, column);
			else
				OO.insertString(xSpreadsheet, ((String [])Q1.elementAt(i))[1], row, column);

			row++;
		}

		address = OO.findString(xSpreadsheet, sSmallerQuad);

		column = address[0];
		row = address[1];

		OO.findAndReplace(xSpreadsheet, sSmallerQuad, sSmallQuadReplacement);
		//row = row - 2;	// Go up 2 spaces

		for(i=0; i<iSmallTotRow; i++)
		{
			if(sSmallerQuad.equals("<q2>"))
				OO.insertString(xSpreadsheet, ((String [])Q2.elementAt(i))[1], iInitRow, column);
			else
				OO.insertString(xSpreadsheet, ((String [])Q1.elementAt(i))[1], iInitRow, column);

			iInitRow++;
		}
	}

	/**
	 * Write the Quadrant description and table heading
	 * @param iQuadrant
	 * @param bBreakDown
	 * @throws Exception
	 * @see IndividualReport1#populateQuadrantDetail(boolean)
	 */
	public void writeQuadrantData(int iQuadrant, boolean bBreakDown) throws Exception
	{
		column = 0;
		startColumn = 0;
		endColumn = 0;

		String sTitle = "";
		String sDesc = "";

		switch(iQuadrant)
		{
		case 1 :
			sTitle = "QUADRANT 1 - ACQUIRE";
			sDesc = "This quadrant is typified by competencies that have negative gaps and with all key behaviours rated with negative gaps."
				+ " " + "Raters generally think that the target does not meet expectations in these competencies."
				+ " " + "The target would need to acquire these competencies through substantive development efforts.";
			break;

		case 2 :
			sTitle = "QUADRANT 2 - INVEST";
			sDesc = "This quadrant is typified by competencies that have negative gaps but with some key behaviours rated with positive gaps."
				+ " " + "Although the target is rated as not meeting the required expectations for these competencies, raters do think that there are certain behaviours that are considered as meeting or exceeding expectations."
				+ " " + "As such, the target only need to invest developmental efforts in those behaviours that are rated with negative gaps to improve the overall proficiency of the competencies.";
			break;

		case 3 :
			sTitle = "QUADRANT 3 - STRENGTHEN";
			sDesc = "This quadrant is typified by competencies that have positive gaps but with some key behaviours rated with negative gaps."
				+ " " + "As such, raters generally think that the target has met expectations in these competencies but there are behaviours that are considered as below requirements."
				+ " " +	"The target could strengthen these competencies by working on those behaviours that are rated with negative gaps.";
			break;

		case 4 :
			sTitle = "QUADRANT 4 - SUSTAIN";
			sDesc = "This quadrant is typified by competencies that have positive gaps and with all key behaviours rated with positive gaps."
				+ " " + "The target is seen to meet or exceed expectations for all these competencies."
				+ " " + "The target only need to sustain proficiency in these competencies."
				+ " " + "No developmental intervention is necessary in this quadrant.";
			break;
		}

		OO.insertString(xSpreadsheet2, sTitle, row, column);
		OO.mergeCells(xSpreadsheet2, startColumn, startColumn+1, row, row);
		OO.setFontBold(xSpreadsheet2, startColumn, startColumn+1, row, row);
		row++;

		OO.mergeCells(xSpreadsheet2, startColumn, startColumn+6, row, row);
		OO.insertString(xSpreadsheet2, sDesc, row, column);
		OO.setRowHeight(xSpreadsheet2, row, 1, ROWHEIGHT*OO.countTotalRow(sDesc, 110));

		row += 2;
		OO.insertString(xSpreadsheet2, "Ranked Order", row, column);
		OO.mergeCells(xSpreadsheet2, column, column, row, row+1);
		OO.setFontBold(xSpreadsheet2, column, column, row, row+1);
		OO.setCellAllignment(xSpreadsheet2, column, column, row, row+1, 1, 2);
		OO.setCellAllignment(xSpreadsheet2, column, column, row, row+1, 2, 2);
		column++;

		OO.insertString(xSpreadsheet2, "Competency", row, column);
		OO.mergeCells(xSpreadsheet2, column, column, row, row+1);
		OO.setFontBold(xSpreadsheet2, column, column, row, row+1);
		OO.setCellAllignment(xSpreadsheet2, column, column, row, row+1, 1, 2);
		OO.setCellAllignment(xSpreadsheet2, column, column, row, row+1, 2, 2);
		column++;

		int iCategory = RR.getTotalRelation(this.surveyID, bBreakDown);

		OO.insertString(xSpreadsheet2, "Current Proficiency", row, column);
		// Calculate total no of categories and merge the cell
		OO.mergeCells(xSpreadsheet2, column, column + (iCategory-1), row, row);
		OO.setFontBold(xSpreadsheet2, column, column + (iCategory-1), row, row+1);
		OO.setCellAllignment(xSpreadsheet2, column, column, row, row+1, 1, 2);

		int iGapCol = column + iCategory;
		OO.insertString(xSpreadsheet2, "Gap", row, iGapCol);
		OO.mergeCells(xSpreadsheet2, iGapCol, iGapCol, row, row+1);
		OO.setFontBold(xSpreadsheet2, iGapCol, iGapCol, row, row+1);
		OO.setCellAllignment(xSpreadsheet2, iGapCol, iGapCol, row, row+1, 1, 2);
		OO.setCellAllignment(xSpreadsheet2, iGapCol, iGapCol, row, row+1, 2, 2);

		endColumn = iGapCol;
		OO.setTableBorder(xSpreadsheet2, startColumn, endColumn, row, row+1, true, true, true, true, true, true);

		row++;

		if(bBreakDown)
			writeBreakDownQuadrantScore(iQuadrant);
		else
			writeQuadrantScore(iQuadrant);

		insertPageBreak(xSpreadsheet2, 0, 1, row);
	}

	/**
	 * Main function to write the Quadrant Details (Sheet 2)
	 * @param bBreakDown
	 * @throws SQLException
	 * @throws IOException
	 * @throws Exception
	 * @author Maruli
	 * @see IndividualReport1#reportDevelopmentMap(int, int, String, boolean)
	 */
	public void populateQuadrantDetail(boolean bBreakDown) throws Exception
	{
		//	System.out.println("5. Populating Quadrant details");

		row = 0;

		writeQuadrantData(1, bBreakDown);
		writeQuadrantData(2, bBreakDown);
		writeQuadrantData(3, bBreakDown);
		writeQuadrantData(4, bBreakDown);
	}

	/**
	 * Write the Competency, KB, CP and Gap score (Category Relation High)
	 * @param iQuadrant
	 * @throws Exception
	 * @author Maruli
	 * @see IndividualReport1#writeQuadrantData(int, boolean)
	 */
	public void writeQuadrantScore(int iQuadrant) throws Exception
	{
		int iCol = 2;		// To iterate through each group of CP

		Vector vLocal = null;
		Vector rsBehv = null;
		Vector rsRelation = null;

		double dCompCP = 0;
		double dBehvCP = 0;
		double dCompGap = 0;
		double dBehvGap = 0;
		int iCompID = 0;
		int iBehvID = 0;
		int iCompRank = 0;
		String sComp = "";
		String sBehv = "";

		switch (iQuadrant)
		{
		case 1 : vLocal = (Vector) Q1.clone();
		break;
		case 2 : vLocal = (Vector) Q2.clone();
		break;
		case 3 : vLocal = (Vector) Q3.clone();
		break;
		case 4 : vLocal = (Vector) Q4.clone();
		break;
		}

		//vLocal = G.sortVector(vLocal, 1);

		for(int a=0; a<vLocal.size(); a++)
		{
			if(a == 0)
			{
				String sRelation = "";

				OO.insertString(xSpreadsheet2, "Self", row, iCol);
				OO.setFontBold(xSpreadsheet2, iCol, iCol, row, row);
				OO.setCellAllignment(xSpreadsheet2, iCol, iCol, row, row, 1, 2);
				OO.setCellAllignment(xSpreadsheet2, iCol, iCol, row, row, 2, 2);
				iCol++;

				rsRelation = RR.getRelationHigh(this.surveyID);

				for(int i=0; i<rsRelation.size(); i++)
				{
					votblRelationHigh vo = (votblRelationHigh)rsRelation.elementAt(i);
					sRelation = vo.getRelationHigh();
					OO.insertString(xSpreadsheet2, sRelation, row, iCol);
					OO.setFontBold(xSpreadsheet2, iCol, iCol, row, row);
					OO.setCellAllignment(xSpreadsheet2, iCol, iCol, row, row, 1, 2);
					OO.setCellAllignment(xSpreadsheet2, iCol, iCol, row, row, 2, 2);
					iCol++;
				}

				row++;
			}

			iCol = 0;

			iCompID = Integer.parseInt( ((String [])vLocal.elementAt(a))[0] );
			sComp = ((String [])vLocal.elementAt(a))[1];
			dCompGap = Double.parseDouble( ((String [])vLocal.elementAt(a))[2] );
			iCompRank = Integer.parseInt( ((String [])vLocal.elementAt(a))[3] ); 

			OO.insertNumeric(xSpreadsheet2, iCompRank, row, iCol);
			OO.setCellAllignment(xSpreadsheet2, iCol, iCol, row, row, 1, 2);
			OO.setBGColor(xSpreadsheet2, iCol, iCol, row, row, BGCOLOR);
			iCol++;

			OO.insertString(xSpreadsheet2, sComp, row, iCol);
			OO.setBGColor(xSpreadsheet2, iCol, iCol, row, row, BGCOLOR);
			iCol++;

			// SELF CP goes first
			dCompCP = SR.getAvgCPComp(this.surveyID, this.targetID, 4, iCompID);
			OO.insertNumeric(xSpreadsheet2, dCompCP, row, iCol);
			OO.setCellAllignment(xSpreadsheet2, iCol, iCol, row, row, 1, 2);
			OO.setBGColor(xSpreadsheet2, iCol, iCol, row, row, BGCOLOR);
			iCol++;

			int iRelID = 0;

			rsRelation = RR.getRelationHigh(this.surveyID);
			for(int i=0; i<rsRelation.size(); i++)
			{
				votblRelationHigh vo = (votblRelationHigh)rsRelation.elementAt(i);

				iRelID = vo.getRTRelation();

				// In tblAvgMean SUP = 2 but in tblRelation SUP = 1. OTH is not affected (Both 3)
				if(iRelID == 1)
					iRelID = 2;

				dCompCP = SR.getAvgCPComp(this.surveyID, this.targetID, iRelID, iCompID);
				OO.insertNumeric(xSpreadsheet2, dCompCP, row, iCol);
				OO.setCellAllignment(xSpreadsheet2, iCol, iCol, row, row, 1, 2);
				OO.setBGColor(xSpreadsheet2, iCol, iCol, row, row, BGCOLOR);
				iCol++;
			}

			OO.insertNumeric(xSpreadsheet2, dCompGap, row, iCol);
			OO.setCellAllignment(xSpreadsheet2, iCol, iCol, row, row, 1, 2);
			OO.setBGColor(xSpreadsheet2, iCol, iCol, row, row, BGCOLOR);

			OO.setTableBorder(xSpreadsheet2, startColumn, endColumn, row, row, true, true, true, true, true, true);

			row++;
			iCol = 1;

			// Get Behaviour's gap
			rsBehv = SR.getBehaviourGapDevMap(this.surveyID, this.targetID, iCompID);

			for(int j=0; j<rsBehv.size(); j++)
			{

				String [] arr = (String []) rsBehv.elementAt(j);


				iCol = 1;
				iBehvID = Integer.parseInt(arr[2]);
				sBehv = arr[1];
				dBehvGap = Double.parseDouble(arr[0]);

				OO.insertString(xSpreadsheet2, sBehv, row, iCol);
				iCol++;
				// SELF CP goes first
				dBehvCP = SR.getAvgCPKB(this.surveyID, this.targetID, 4, iBehvID);
				OO.insertNumeric(xSpreadsheet2, dBehvCP, row, iCol);
				OO.setCellAllignment(xSpreadsheet2, iCol, iCol, row, row, 1, 2);
				iCol++;

				rsRelation = RR.getRelationHigh(this.surveyID);

				for(int i=0; i<rsRelation.size(); i++)
				{
					votblRelationHigh vo = (votblRelationHigh)rsRelation.elementAt(i);
					iRelID = vo.getRTRelation();

					// In tblAvgMean SUP = 2 but in tblRelation SUP = 1. OTH is not affected (Both 3)
					if(iRelID == 1)
						iRelID = 2;

					dBehvCP = SR.getAvgCPKB(this.surveyID, this.targetID, iRelID, iBehvID);
					OO.insertNumeric(xSpreadsheet2, dBehvCP, row, iCol);
					OO.setCellAllignment(xSpreadsheet2, iCol, iCol, row, row, 1, 2);
					iCol++;
				}

				OO.insertNumeric(xSpreadsheet2, dBehvGap, row, iCol);
				OO.setCellAllignment(xSpreadsheet2, iCol, iCol, row, row, 1, 2);

				OO.setTableBorder(xSpreadsheet2, startColumn, endColumn, row, row, true, true, true, true, true, true);
				row++;
			}
		}

		// If there is no competency in that particular Quadrant, insert relation and leave 2 blank spaces
		if(vLocal.size() == 0)
		{
			String sRelation = "";

			OO.insertString(xSpreadsheet2, "Self", row, iCol);
			OO.setCellAllignment(xSpreadsheet2, iCol, iCol, row, row, 1, 2);
			iCol++;

			rsRelation = RR.getRelationHigh(this.surveyID);

			for(int i=0; i<rsRelation.size(); i++)
			{
				votblRelationHigh vo = (votblRelationHigh)rsRelation.elementAt(i);

				sRelation = vo.getRelationHigh();
				OO.insertString(xSpreadsheet2, sRelation, row, iCol);
				OO.setCellAllignment(xSpreadsheet2, iCol, iCol, row, row, 1, 2);
				iCol++;
			}

			row++;
			OO.insertString(xSpreadsheet2, "There are no competencies under this Quadrant", row, 0);
			OO.mergeCells(xSpreadsheet2, 0, endColumn, row, row);
			row += 2;
		}
	}

	/**
	 * Write the Competency, KB, CP and Gap score (Category Relation High & Specific)
	 * @param iQuadrant
	 * @throws Exception
	 * @author Maruli
	 * @see IndividualReport1#writeQuadrantData(int, boolean)
	 */
	public void writeBreakDownQuadrantScore(int iQuadrant) throws Exception
	{
		int iCol = 2;		// To iterate through each group of CP

		Vector vLocal = null;
		Vector rsBehv = null;
		Vector rsRelationHigh = null;
		Vector rsRelationSpec = null;

		double dCompCP = 0;
		double dBehvCP = 0;
		double dCompGap = 0;
		double dBehvGap = 0;
		int iCompID = 0;
		int iBehvID = 0;
		int iCompRank = 0;
		String sComp = "";
		String sBehv = "";
		String sRelation = "";
		int iRelHigh = 0;

		switch (iQuadrant)
		{
		case 1 : vLocal = (Vector) Q1.clone();
		break;
		case 2 : vLocal = (Vector) Q2.clone();
		break;
		case 3 : vLocal = (Vector) Q3.clone();
		break;
		case 4 : vLocal = (Vector) Q4.clone();
		break;
		}

		//vLocal = G.sortVector(vLocal, 1);

		for(int a=0; a<vLocal.size(); a++)
		{
			if(a == 0)
			{
				OO.insertString(xSpreadsheet2, "Self", row, iCol);
				OO.setCellAllignment(xSpreadsheet2, iCol, iCol, row, row, 1, 2);
				OO.setCellAllignment(xSpreadsheet2, iCol, iCol, row, row, 2, 2);
				iCol++;

				rsRelationHigh = RR.getRelationHigh(this.surveyID);

				for(int i=0; i<rsRelationHigh.size(); i++)
				{
					votblRelationHigh vo = (votblRelationHigh)rsRelationHigh.elementAt(i);

					iRelHigh = vo.getRTRelation();

					rsRelationSpec = RR.getRelationSpecific(this.surveyID, iRelHigh);

					for(int j=0; j<rsRelationSpec.size(); j++)
					{
						votblAssignment voTbl = (votblAssignment)rsRelationSpec.elementAt(j);
						sRelation = voTbl.getRelationSpecific();
						OO.insertString(xSpreadsheet2, sRelation, row, iCol);
						OO.setCellAllignment(xSpreadsheet2, iCol, iCol, row, row, 1, 2);
						OO.setCellAllignment(xSpreadsheet2, iCol, iCol, row, row, 2, 2);
						iCol++;
					}
				}

				row++;
			}

			iCol = 0;

			iCompID = Integer.parseInt( ((String [])vLocal.elementAt(a))[0] );
			sComp = ((String [])vLocal.elementAt(a))[1];
			dCompGap = Double.parseDouble( ((String [])vLocal.elementAt(a))[2] );
			iCompRank = Integer.parseInt( ((String [])vLocal.elementAt(a))[3] );

			OO.insertNumeric(xSpreadsheet2, iCompRank, row, iCol);
			OO.setCellAllignment(xSpreadsheet2, iCol, iCol, row, row, 1, 2);
			OO.setBGColor(xSpreadsheet2, iCol, iCol, row, row, BGCOLOR);
			iCol++;

			OO.insertString(xSpreadsheet2, sComp, row, iCol);
			OO.setBGColor(xSpreadsheet2, iCol, iCol, row, row, BGCOLOR);
			iCol++;

			// SELF CP goes first
			dCompCP = SR.getAvgCPCompBreakDown(this.surveyID, this.targetID, 2, 0, iCompID);
			OO.insertNumeric(xSpreadsheet2, dCompCP, row, iCol);
			OO.setCellAllignment(xSpreadsheet2, iCol, iCol, row, row, 1, 2);
			OO.setBGColor(xSpreadsheet2, iCol, iCol, row, row, BGCOLOR);
			iCol++;

			rsRelationHigh = RR.getRelationHigh(this.surveyID);

			for(int i=0; i<rsRelationHigh.size(); i++)
			{
				votblRelationHigh vo = (votblRelationHigh)rsRelationHigh.elementAt(i);

				iRelHigh = vo.getRTRelation();

				rsRelationSpec = RR.getRelationSpecific(this.surveyID, iRelHigh);

				for(int j=0; j<rsRelationSpec.size(); j++)
				{
					votblAssignment voTbl = (votblAssignment)rsRelationSpec.elementAt(j);

					dCompCP = SR.getAvgCPCompBreakDown(this.surveyID, this.targetID, iRelHigh, 
							voTbl.getRTSpecific(), iCompID);
					OO.insertNumeric(xSpreadsheet2, dCompCP, row, iCol);
					OO.setCellAllignment(xSpreadsheet2, iCol, iCol, row, row, 1, 2);
					OO.setBGColor(xSpreadsheet2, iCol, iCol, row, row, BGCOLOR);
					iCol++;
				}
			}

			OO.insertNumeric(xSpreadsheet2, dCompGap, row, iCol);
			OO.setCellAllignment(xSpreadsheet2, iCol, iCol, row, row, 1, 2);
			OO.setBGColor(xSpreadsheet2, iCol, iCol, row, row, BGCOLOR);

			OO.setTableBorder(xSpreadsheet2, startColumn, endColumn, row, row, true, true, true, true, true, true);

			row++;
			iCol = 1;

			// Get Behaviour's gap

			rsBehv = SR.getBehaviourGapDevMap(this.surveyID, this.targetID, iCompID);

			for(int j=0; j<rsBehv.size(); j++)
			{

				String [] arr = (String []) rsBehv.elementAt(j);


				iCol = 1;
				iBehvID = Integer.parseInt(arr[2]);
				sBehv = arr[1];
				dBehvGap = Double.parseDouble(arr[0]);;

				OO.insertString(xSpreadsheet2, sBehv, row, iCol);
				iCol++;
				// SELF CP goes first
				dBehvCP = SR.getAvgCPKBBreakDown(this.surveyID, this.targetID, 2, 0, iBehvID);
				OO.insertNumeric(xSpreadsheet2, dBehvCP, row, iCol);
				OO.setCellAllignment(xSpreadsheet2, iCol, iCol, row, row, 1, 2);
				iCol++;

				rsRelationHigh = RR.getRelationHigh(this.surveyID);

				for(int i=0; i<rsRelationHigh.size(); i++)
				{
					votblRelationHigh vo = (votblRelationHigh)rsRelationHigh.elementAt(i);

					iRelHigh = vo.getRTRelation();

					rsRelationSpec = RR.getRelationSpecific(this.surveyID, iRelHigh);

					for(int k=0; k<rsRelationSpec.size(); k++)
					{
						votblAssignment voTbl = (votblAssignment)rsRelationSpec.elementAt(k);

						dBehvCP = SR.getAvgCPKBBreakDown(this.surveyID, this.targetID, iRelHigh, 
								voTbl.getRTSpecific(), iBehvID);
						OO.insertNumeric(xSpreadsheet2, dBehvCP, row, iCol);
						OO.setCellAllignment(xSpreadsheet2, iCol, iCol, row, row, 1, 2);
						iCol++;
					}
				}

				OO.insertNumeric(xSpreadsheet2, dBehvGap, row, iCol);
				OO.setCellAllignment(xSpreadsheet2, iCol, iCol, row, row, 1, 2);

				OO.setTableBorder(xSpreadsheet2, startColumn, endColumn, row, row, true, true, true, true, true, true);
				row++;
			}
		}

		// If there is no competency in that particular Quadrant, insert relation and leave 2 blank spaces
		if(vLocal.size() == 0)
		{	
			OO.insertString(xSpreadsheet2, "Self", row, iCol);
			OO.setCellAllignment(xSpreadsheet2, iCol, iCol, row, row, 1, 2);
			iCol++;

			rsRelationHigh = RR.getRelationHigh(this.surveyID);

			for(int i=0; i<rsRelationHigh.size(); i++)
			{
				votblRelationHigh vo = (votblRelationHigh)rsRelationHigh.elementAt(i);

				iRelHigh = vo.getRTRelation();

				rsRelationSpec = RR.getRelationSpecific(this.surveyID, iRelHigh);

				for(int j=0; j<rsRelationSpec.size(); j++)
				{
					votblAssignment voTbl = (votblAssignment)rsRelationSpec.elementAt(j);
					sRelation = voTbl.getRelationSpecific();

					OO.insertString(xSpreadsheet2, sRelation, row, iCol);
					OO.setCellAllignment(xSpreadsheet2, iCol, iCol, row, row, 1, 2);
					iCol++;
				}
			}

			row++;
			OO.insertString(xSpreadsheet2, "There are no competencies under this Quadrant", row, 0);
			OO.mergeCells(xSpreadsheet2, 0, endColumn, row, row);
			row += 2;
		}
	}

	public int getCompetencyRank(int iCompetency) throws SQLException
	{
		int iRank = 0;

		String sSQL = "SELECT CompetencyRank FROM tblSurveyCompetency WHERE CompetencyID = " + iCompetency;
		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(sSQL);

			if(rs.next())
				iRank = rs.getInt("CompetencyRank");


		}catch(Exception ex){
			System.out.println("IndividualReport.java - getCompetencyRank - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}


		return iRank;
	}

	/**
	 * Development Map Report (Main function)
	 * @param surveyID
	 * @param targetID
	 * @param fileName
	 * @param bBreakDown - True=Breakdown OTH into sub category
	 * @author Maruli
	 */
	public void reportDevelopmentMap(int surveyID, int targetID, String fileName, boolean bBreakDown)
	{
		try {

			//	System.out.println("Development Map Generation Starts");

			InitializeExcelDevMap(fileName, "Development Map Template.xls");
			InitializeSurvey(surveyID, targetID, fileName);
			replacementDevelopmentMap();

			drawDevelopmentMap();
			populateQuadrantDetail(bBreakDown);

			Date timestamp = new Date();
			SimpleDateFormat dFormat = new SimpleDateFormat("dd/MM/yyyy");
			String temp = dFormat.format(timestamp);

			//changed copyright symbol to \u00a9 and registered symbol to \u00AE by Chun Yeong 8 Jun 2011
			OO.insertHeaderFooter(xDoc, "", surveyInfo[6] + "\nTarget: " + UserName() + "\n", 
					"Date of Printing: " + temp + "\n" + "Copyright \u00a9 3-Sixty Profiler\u00AE is a product of Pacific Century Consulting Pte Ltd.", 1, 3);			

			//	System.out.println("Development Map Generation Completed");

		}catch (SQLException SE) {
			System.out.println("a " + SE.getMessage());
		}catch (Exception E) {
			System.out.println("b " + E.getMessage());
		} finally {      

			try {			
				OO.storeDocComponent(xRemoteServiceManager, xDoc, storeURL);
				OO.closeDoc(xDoc);	
			}catch (SQLException SE) {
				System.out.println("a " + SE.getMessage());
			}catch (IOException IO) {
				System.err.println(IO);
			}catch (Exception E) {
				System.out.println("b " + E.getMessage());
			}
		}
	}

	/**
	 * Print Individual Report
	 * @param surveyID
	 * @param targetID
	 * @param pkUser
	 * @param fileName
	 * @param type - 1=Simplified(No charts), 2=Standard
	 * @param chkNormative
	 * @param chkGroupCPLine - Added by Chun Yeong, 2 Jun 2011 to allow option to show Group Current Proficiency
	 * @param chkSplit - Added by Chun Yeong, 13 Jun 2011 to allow split others option 
	 * @param lang - Added by Chun Yeong, 1 Aug 2011, to enable selection of Language
	 * @param template - Added by Chun Yeong, 1 Aug 2011, to enable selection of template
	 * @author Jenty - Edited by Maruli
	 */
	//Edited printing report with Normation optional by  Tracy 01 Sep 08
	public void Report(int surveyID, int surveyIDImpt, int targetID, int pkUser, String fileName, int type, String chkNormative, String chkGroupCPLine, String chkSplit, int lang, String template) 
	// End add by Tracy 01 Sep 08
	{
		try {
			/*
			 * We need to re-initialize the start and end column.
			 * If we need to send reports through email, the next time we tried to generate report,
			 * the start and end column won't be initialised by constructor anymore.
			 * 
			 * If there is a part which substract/add these 2 variables, the code is going to have problem.
			 * Possibly array out of bound will happen.
			 */
			startColumn = 0;
			endColumn = 12;

			//Added to populate the hashtable for translation, Chun Yeong 1 Aug 2011
			trans.populateHashtable();
			//Check if template String contains the specific language type and set the variable templateLanguage to the respective value
			// 1 - Indonesian
			// 2 - Thai
			// 3 - Korean
			// 4 - Chinese(Simplified)
			// 5 - Chinese(Traditional)
			if(template.toLowerCase().contains("indo")) templateLanguage = 1;
			else if(template.toLowerCase().contains("thai")) templateLanguage = 2;
			//else if(template.toLowerCase().contains("korean")) templateLanguage = 3;
			//else if(template.toLowerCase().contains("chinese(simp)")) templateLanguage = 4;
			//else if(template.toLowerCase().contains("chinese(trad)")) templateLanguage = 5;
			else templateLanguage = 0; //English

			//System.out.println("1. Individual Report Generation Starts");
			iReportType = type; //Set report type
			//added in a class varibble to indicate whether this survey split "Others" to "Subordinates" and "Peers" 
			Create_Edit_Survey ces = new Create_Edit_Survey();
			splitOthers = ces.getSplitOthersOption(surveyID);

			//Update from whether to reference the template or not to split Peers and Subordinates by Chun Yeong 13 Jun 2011
			if (!chkSplit.equals("")) {
				//1 for split, 0 for join
				if(splitOthers == 1) {
					splitOthers = 0;
				} else if (splitOthers == 0) {
					splitOthers = 1;
				}
			}

			/*Set the language type base on what the user selected.
			 * 1 - Indonesian
			 * 2 - Thai
			 * 3 - Korean
			 * 4 - Chinese (Simplified)
			 * 5 - Chinese (Traditional) 
			 *If the lang == 0, the default is English, Chun Yeong 1 Aug 2011*/
			if(lang == 1){ language = "1"; } 
			else if (lang == 2){ language = "2"; } 
			else { language = ""; }

			/*
			 * Change(s) : Added logic and relevant variables to facilitate switching between report templates
			 * Reason(s) : To facilitate switching between report templates for SPS and other future clients
			 * Updated By: Desmond
			 * Updated On: 23 Oct 2009
			 * Previous Update: Edited printing ind report with Normative option, By Tracy 01 Sep 08
			 */
			String templateName = ""; // Variable for storing filename of template to be used for generating report, Desmond 23 Oct 09
			//set back to "" such that we can have a choice of different organizations (Qiao Li 22 Dec 2009)
			String org = "";
			//String org = "SPS"; // Set Organization here manually so that the correct report template is used, Desmond 23 Oct 09

			if (chkNormative=="") {

				templateName = template+"_no_normative.xls"; // Default Template
				if(org.equalsIgnoreCase("SPS")) { templateName = template+"_no_normative_SPS.xls";} // SPS Template

				//Base on the variable templateLanguage set above, set the proper template, Chun Yeong 1 Aug 2011
				if(templateLanguage == 1) {templateName = template+"_No_Normative_Indo.xls";}
				if(templateLanguage == 2) {templateName = template+"_No_Normative_Thai.xls";}

				InitializeExcel(fileName, templateName); // Default Template
				System.out.println("Using NON-normative template: " + templateName);
			}else {

				templateName = template; // Default Template
				if(org.equalsIgnoreCase("SPS")) {templateName = template+"_SPS.xls";} // SPS Template

				if(templateLanguage == 1) {templateName = template;}
				if(templateLanguage == 2) {templateName = template;}

				InitializeExcel(fileName, templateName );
				System.out.println("Using Normative template: " + templateName);
			}

          /*try 
			
			
			{ System.out.println(xDoc);
				XSpreadsheetDocument xtd = (XSpreadsheetDocument)UnoRuntime.queryInterface(XSpreadsheetDocument.class, xDoc);
				System.out.println("xtd= " + xtd);
			XModel xmodel = (XModel)UnoRuntime.queryInterface(XModel.class, xtd); 
			System.out.println(xmodel);
			XController xcont = xmodel.getCurrentController(); 
			
			System.out.println("xcont = " + xcont);
			XPropertySet xps = (XPropertySet)UnoRuntime.queryInterface(XPropertySet.class, xcont); 
			System.out.println(xps);
			    System.out.println(xps.getPropertyValue("PageCount"));
			    Integer pageCount = Integer.parseInt(xps.getPropertyValue("PageCount").toString()); 
			    System.out.println("PageCount= " + pageCount); 
			} 
			catch (Exception e) 
			{ 
			    e.printStackTrace();
			    System.out.println("heres the exception"); 
			    return;
			} 
			
		*/	
			
			//Added, set global boolean variable to display/not display Group CP Line, by Chun Yeong 2 Jun 2011 
			isGroupCPLine = (chkGroupCPLine.equals("")) ? false : true;

			InitializeSurvey(surveyID, targetID, fileName);
			Replacement();

			InsertCPvsCPR();
			InsertRatingScaleList();
			InsertGapTitle(surveyID);
			InsertGap();

			//Edited printing ind report with Normative option
			// by Tracy 01 Sep 08***
			System.out.println("chkNormative "+chkNormative);
			if (chkNormative!=""){
				InsertNormative();
				System.out.println("normative");
			}else{
				// by Hemilda 23/08/2008 for not include normative hasn't define the array, always got error
				int total 	= totalCompetency();
				//Initialise array for arrN
				arrN = new int[total * 10 * 6]; //size = total competency * max 10 KBs * max 6 Rating

			}
			//End edit by Tracy 01 Sep 08***

			// Added by Tracy 01 Sep 08************************
			//Page 6: Ind Profile report- Insert dynamic competency in Gap
			InsertCompGap(surveyID);
			// End add by Tracy 01 Sep 08**********************

			//insert the appropriate legends (w/o CPR, split "Others") (Qiao Li 22 Dec 2009)

			InsertProfileLegend();
			if(Integer.parseInt(surveyInfo[9])==0){ //without cluster
				InsertCompetency(type,surveyID,surveyIDImpt);
			} else{ //with cluster
				InsertClusterCompetency(type,surveyID);
			}

			/*
			 * Change(s) : Added new section of blind spot analysis
			 * Reason(s) : New section required
			 * Updated By: Chun Yeong, 27 May 2011
			 */
			//Insert the header
			OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "BLIND SPOT ANALYSIS"), row, 0);
			OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
			OO.setFontSize(xSpreadsheet, startColumn, endColumn, row, row, 16);
			OO.setRowHeight(xSpreadsheet, row, startColumn, 600);
			OO.mergeCells(xSpreadsheet, startColumn, startColumn + 6, row, row);
			row++;

			if(Integer.parseInt(surveyInfo[9])==0){ //without cluster
				InsertBlindSpotAnalysis(true);		//Insert positive table
				row++;
				InsertBlindSpotAnalysis(false);		//Insert negative table
			} else{ //with cluster
				InsertClusterBlindSpotAnalysis(true);		//Insert positive table
				row++;
				InsertClusterBlindSpotAnalysis(false);		//Insert negative table
			}
			lastPageRowCount = 0;
			insertPageBreak(xSpreadsheet, startColumn, endColumn, row);
			//End add by Chun Yeong, 25 May 2011************

			int included = Q.commentIncluded(surveyID);
			//Added by Ha 23/06/09 to insert self commnent as well
			int selfIncluded = Q.SelfCommentIncluded(surveyID);
			if(included == 1||selfIncluded==1)
				InsertComments();

			/*
			 * Change(s) : Added new section of blind spot analysis
			 * Reason(s) : New section required
			 * Updated By: Chun Yeong, 27 May 2011
			 */

			/*
			 * Change(s) : Added new section for generating additional questions in the report
			 * Updated By: Wei Han 16 Apr 2012 
			 */
			AdditionalQuestionController aqc = new AdditionalQuestionController();
			if(aqc.getQuestions(surveyID).size()>0)
				InsertAdditionalQuestions();

			if(surveyInfo[8] != "") //Org Logo
			{
				System.out.println("Report() - Logo location: " + ST.getOOLogoPath()+ surveyInfo[8]); // To Remove, Desmond 19 Nov 09
				File F = new File(ST.getOOLogoPath()+ surveyInfo[8]); //directory where the file supposed to be stored
				if(F.exists())
					OO.replaceLogo(xSpreadsheet, xDoc, "<Logo>", ST.getOOLogoPath()+ surveyInfo[8]);
				else
					OO.replaceLogo(xSpreadsheet, xDoc, "<Logo>", "");
			}

			Date timestamp = new Date();
			SimpleDateFormat dFormat = new SimpleDateFormat("dd/MM/yyyy");
			String temp = dFormat.format(timestamp);
			
			//changed copyright symbol to \u00a9 and registered symbol to \u00AE by Chun Yeong 8 Jun 2011
		/*	OO.insertHeaderFooter(xDoc, surveyInfo[1], surveyInfo[6] + "\n" + UserName() + "\n", 
					"Date of printing: " + temp + "\n" + "Copyright \u00a9 3-Sixty Profiler\u00AE is a product of Pacific Century Consulting Pte Ltd.");			
*/
			OO.insertHeaderFooter(xDoc, surveyInfo[1], surveyInfo[6] + "\n" + UserName() + "\n", 
					"Date of printing: " + temp + "\n" + "Copyright \u00a9 3-Sixty Profiler\u00AE is a product of Pacific Century Consulting Pte Ltd.",1,1 );
			//System.out.println("Individual Report Generation Completed");

			
		}catch (SQLException SE) {
			System.out.println("a " + SE.getMessage());
		}catch (Exception E) {
			System.out.println("b " + E.getMessage());
			E.printStackTrace();
		} finally {      

			try {
				if(format == 0){
					OO.storeDocComponent(xRemoteServiceManager, xDoc, storeURL);
					}
					else {
						OO.storeAsPDF(xRemoteServiceManager, xDoc, storeURL , pageCount);
					}
					OO.closeDoc(xDoc);		
			}catch (SQLException SE) {
				System.out.println("a " + SE.getMessage());
			}catch (IOException IO) {
				System.err.println(IO);
			}catch (Exception E) {
				System.out.println("b " + E.getMessage());
			}
		}
	}

	public void InsertAdditionalQuestions()
	{
		try{
			//insert the additional questions header 
			insertPageBreak(xSpreadsheet, startColumn, endColumn, row);
			int startrow = row;
	
			row++;
			OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Additional Questions"), row, column);
			OO.setFontSize(xSpreadsheet, startColumn, endColumn, row, row, 16);
			OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
			row++;

			int startborder = row+1;

			//loop through all questions in this survey
			AdditionalQuestionController aqController = new AdditionalQuestionController();
			Vector<AdditionalQuestion> questions = aqController.getQuestions(surveyID);
			for(int i=0;i<questions.size();i++)
			{
				AdditionalQuestion qn = questions.get(i);
				row++;
				//insert the question into the report and format it 
				OO.insertString(xSpreadsheet, Integer.toString(i+1) + ".", row, column);	
				OO.insertString(xSpreadsheet, qn.getQuestion(), row, column+1);
				OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
				OO.setBGColor(xSpreadsheet, startColumn, endColumn, row, row, BGCOLOR);
				OO.mergeCells(xSpreadsheet, column+1, endColumn, row, row);
				OO.wrapText(xSpreadsheet, column+1, endColumn, row, row);
				OO.setCellAllignment(xSpreadsheet, startColumn, endColumn, row, row, 2, 1); // Set alignment of competency name to top, Desmond 16 Nov 09
				row++;


				//loop through the questions in the survey and categorize the answers 
				//according to the user type

				//get all the answers to the questions and segregate them according to the type
				Vector<String> supAns = aqController.getReportAnswers("SUP%", qn.getAddQnId(), targetID);
				Vector<String> subAns = aqController.getReportAnswers("SUB%", qn.getAddQnId(), targetID);
				Vector<String> peerAns = aqController.getReportAnswers("PEER%", qn.getAddQnId(), targetID);
				Vector<String> selfAns = aqController.getReportAnswers("SELF%", qn.getAddQnId(), targetID);
				Vector<String> othAns = aqController.getReportAnswers("OTH%", qn.getAddQnId(), targetID);

				//print the headings and the answers 
				if(supAns.size()>0)//if there are comments by superiors print them
				{
					OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Superior(s)"), row, column+1); // Change from Supervisors to Superior, Desmond 22 Oct 09
					OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
					OO.setFontItalic(xSpreadsheet, startColumn, endColumn, row, row);
					row++;
					insertAnswer(supAns);
				}


				if(splitOthers==1)
					//check to see if split others options is enabled
				{
					if(subAns.size()>0)//if there are comments by subordinates print them
					{
						row++;
						OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Subordinate(s)"), row, column+1); // Change from Supervisors to Superior, Desmond 22 Oct 09
						OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
						OO.setFontItalic(xSpreadsheet, startColumn, endColumn, row, row);
						row++;
						insertAnswer(subAns);
					}

					if(peerAns.size()>0)
					{
						row++;
						OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Peer(s)"), row, column+1); // Change from Supervisors to Superior, Desmond 22 Oct 09
						OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
						OO.setFontItalic(xSpreadsheet, startColumn, endColumn, row, row);
						row++;
						insertAnswer(peerAns);
					}

				}
				else//split others option disabled
				{
					if(peerAns.size()>0||subAns.size()>0||othAns.size()>0)
					{
						row++;
						OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Others"), row, column+1); // Change from Supervisors to Superior, Desmond 22 Oct 09
						OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
						OO.setFontItalic(xSpreadsheet, startColumn, endColumn, row, row);
						row++;
						if(peerAns.size()>0)
							insertAnswer(peerAns);

						if(subAns.size()>0)
							insertAnswer(subAns);	

						if(othAns.size()>0)
							insertAnswer(othAns);	
					}
				}

				if(selfAns.size()>0)//if there are comments by superiors print them
				{
					row++;
					OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Self"), row, column+1); // Change from Supervisors to Superior, Desmond 22 Oct 09
					OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
					OO.setFontItalic(xSpreadsheet, startColumn, endColumn, row, row);
					row++;
					insertAnswer(selfAns);
				}

			}
			OO.setTableBorder(xSpreadsheet, startColumn, endColumn, startborder, row+1, false, false, true, true, true, true);	
            
            int length = row + 1 - startrow;
            int pageHeight = 0;
            for(int i = 0; i< length; i++){
            	pageHeight += OO.getRowHeight(xSpreadsheet, startrow + i, 0);
            	
            	if(pageHeight > 22272){
            		insertPageBreak(xSpreadsheet, startColumn, endColumn, startrow + i -1);
            		
            		pageHeight = OO.getRowHeight(xSpreadsheet, startrow + i, 0);
            		
            	}
            }
            
         /*   while(length > 28){
            	insertPageBreak(xSpreadsheet, startColumn, endColumn, startrow +28);
            	length -=28;
            	startrow +=28;
            }
*/
		}catch(Exception e)
		{

		}
	}

	public void insertAnswer(Vector<String> vans)
	{					
		for(int q=0;q<vans.size();q++)
		{
			try{
				OO.insertString(xSpreadsheet, UnicodeHelper.getUnicodeStringAmp(vans.get(q)), row, column+1);			
				OO.mergeCells(xSpreadsheet, column+1, endColumn, row, row);
				OO.setRowHeight(xSpreadsheet, row, column+1, ROWHEIGHT*OO.countTotalRow(vans.get(q), 85));
				OO.setCellAllignment(xSpreadsheet, startColumn, startColumn, row, row, 2, 1);
				row++;
			}
			catch(Exception e)
			{

			}

		}

	}


	/**
	 * Print out the customised version report for Toyota
	 * @param surveyID
	 * @param targetID
	 * @param pkUser
	 * @param fileName
	 * @author Maruli
	 */
	public void ReportToyota(int surveyID, int targetID, int pkUser, String fileName) 
	{	
		try {

			//System.out.println("1. Individual Report Generation Starts");

			InitializeExcel(fileName, "Individual Report Template Combined.xls");
			InitializeSurveyToyota(surveyID, targetID, fileName);
			ReplacementToyota();
			InsertCPvsCPRToyota();
			InsertGapToyota();
			InsertCompetencyToyota();

			//System.out.println("Individual Report Generation Completed");

		}catch (SQLException SE) {
			System.out.println("a " + SE.getMessage());
		}catch (Exception E) {
			System.out.println("b " + E.getMessage());
		} finally {      

			try {
				OO.storeDocComponent(xRemoteServiceManager, xDoc, storeURL);
				OO.closeDoc(xDoc);	
			}catch (SQLException SE) {
				//System.out.println("a " + SE.getMessage());
				System.out.println("a " + SE);
			}catch (IOException IO) {
				System.err.println(IO);
			}catch (Exception E) {
				//System.out.println("b " + E.getMessage());
				System.out.println("b " + E);
			}
		}
	}


	/**
	 * Customised report for Allianz
	 * @param iSurveyID
	 * @param iTarget
	 * @param iPKUser
	 * @param sFilename
	 * @author Maruli
	 */
	public void ReportAllianz(int iSurveyID, int iTarget, int iPKUser, String sFilename)
	{
		try {

			//System.out.println("1. Individual Report Generation Starts");

			InitializeExcel(sFilename, "Individual Report Template Allianz.xls");
			InitializeSurvey(iSurveyID, iTarget, sFilename);
			ReplacementAllianz();
			AllianzOverall();
			AllianzCompReport();
			InsertCommentsAllianz();

			OO.insertHeaderFooter(xDoc, "", "Assessee: "+UserName(), "");
			//System.out.println("Individual Report Generation Completed");

		}catch (SQLException SE) {
			System.out.println("a " + SE.getMessage());

		}catch (Exception E) {
			System.out.println("b " + E.getMessage());

		} finally {

			try {
				OO.storeDocComponent(xRemoteServiceManager, xDoc, storeURL);
				OO.closeDoc(xDoc);
			} catch (SQLException SE) {
				System.out.println("a " + SE.getMessage());
			} catch (IOException IO) {
				System.err.println(IO);
			} catch (Exception E) {
				System.out.println("b " + E.getMessage());
			}
		}
	}

	/**
	 * Send generated individual report through email
	 * @param sTargetName
	 * @param sSurveyName
	 * @param sFilename
	 * @author Maruli
	 */
	public void sendIndividualReport(String sTargetName, String sSurveyName, String sEmail, String sFilename, int surveyId)
	{
		String sHeader = "INDIVIDUAL REPORT OF " + sTargetName + " FOR " + sSurveyName;

		try {
			//Edited By Roger 13 June 2008
			EMAIL.sendMail_with_Attachment(ST.getAdminEmail(), sEmail, sHeader, "", sFilename, getOrgId(surveyId));
		}
		catch(Exception E)
		{
			System.out.println("a " + E.getMessage());
		}
	}

	/**
	 * Send generated development map through email
	 * @param sTargetName
	 * @param sSurveyName
	 * @param sFilename
	 * @author Maruli
	 */
	public void sendDevelopmentMap(String sTargetName, String sSurveyName, String sEmail, String sFilename, int surveyId)
	{
		String sHeader = "DEVELOPMENT MAP OF " + sTargetName + " FOR " + sSurveyName;

		try {
			//Edited By Roger 13 June 2008
			EMAIL.sendMail_with_Attachment(ST.getAdminEmail(), sEmail, sHeader, "", sFilename, getOrgId(surveyId));
		}
		catch(Exception E)
		{
			System.out.println("a " + E.getMessage());
		}
	}

	public void setCancelPrint(int iVar) {
		iCancel = iVar;
	}

	public int getCancelPrint() {
		return iCancel;
	}

	//Edited By Roger 13 June 2008
	//Get Org ID From SurveyID
	public int getOrgId(int surveyId) {

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;	
		int orgId = 0;
		try {
			String sql = "SELECT FKOrganization FROM tblSurvey WHERE SurveyID=" + surveyId;

			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(sql);

			if (rs.next()) {
				orgId = rs.getInt("FKOrganization");
			}
		} catch (Exception e) {

		} finally {	
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}		
		return orgId;
	}

	/**
	 * This method retrieves the translated competency name of the selected language
	 * @param compName
	 * @return a vector: element(0) for translated competency name, element(1) for translated competency definition
	 * @author Chun Yeong
	 * @since v1.3.12.113 //1 Aug 2011
	 */
	public Vector getTranslatedCompetency(String compName){
		String competencyN = "", competencyD = "";
		String query = "";	
		Vector translatedComp = new Vector();
		query = "SELECT CompetencyName, CompetencyDefinition, CompetencyName" + language + ", CompetencyDefinition" + language + " FROM Competency " 
		+ "WHERE CompetencyName = '" + compName.replaceAll("'", "''").trim() + "'";

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);
			while(rs.next()) {
				//Retrieve the translated competency name.
				//When the competency name is null, set to the default english language
				competencyN = rs.getString("CompetencyName" + language);
				if(rs.wasNull()) competencyN = rs.getString("CompetencyName");

				//Retrieve the translated competency definition
				//When the competency definition is null, set to the default english language
				competencyD = rs.getString("CompetencyDefinition" + language);
				if(rs.wasNull()) competencyD = rs.getString("CompetencyDefinition");

				translatedComp.add(competencyN);
				translatedComp.add(competencyD);
			}
		}catch(Exception ex){
			System.out.println("IndividualReport.java - getTranslatedCompetency - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}
		return translatedComp;
	}

	/**
	 * This method retrieves the translated key behaviour of the selected language
	 * @param KB
	 * @return translated Key Behavior String
	 * @author Chun Yeong
	 * @since v1.3.12.113 //1 Aug 2011
	 */
	public String getTranslatedKeyBehavior(String KB){
		String translatedKB = "";
		String query = "";	
		query = "SELECT KeyBehaviour, KeyBehaviour" + language + " FROM KeyBehaviour " 
		+ "WHERE KeyBehaviour = '" + KB.replaceAll("'", "''").trim() + "'";

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		try{
			con=ConnectionBean.getConnection();
			st=con.createStatement();
			rs=st.executeQuery(query);
			while(rs.next()) {
				//Retrieve the translated key behaviour
				//When the key behaviour is null, set to the default english language
				translatedKB = rs.getString("KeyBehaviour" + language);
				if(rs.wasNull()) translatedKB = rs.getString("KeyBehaviour");
			}
		}catch(Exception ex){
			System.out.println("IndividualReport.java - getTranslatedKeyBehavior - "+ex.getMessage());
		}
		finally{
			ConnectionBean.closeRset(rs); //Close ResultSet
			ConnectionBean.closeStmt(st); //Close statement
			ConnectionBean.close(con); //Close connection
		}

		return translatedKB;
	}

	public void InsertClusterBlindSpotAnalysis(Boolean positive) throws Exception{
		int surveyLevel = Integer.parseInt(surveyInfo[0]);

		int compCount = 1;
		int compID = 0;
		String compName = "";
		String clusterName = "";
		int clusterID = 0;
		int maxScale = MaxScale();

		column = 0;

		int firstColumn = 0;
		//Added to define columns where positive, negative, others and self are inserted in spreadsheet
		int posCol = 6;
		int negCol = 8;
		int othersCol = 10;
		int selfCol = 11;
		double othersValue = 0.0;
		double selfValue = 0.0;

		Vector vClust = ClusterByName();
		Vector vComp = new Vector();
		Vector<Vector> vCompDisplay = new Vector<Vector>();
		String[] Result = new String[4];

		int RTID = 0;
		String RTCode = "";
		for (int m = 0; m< vClust.size(); m++){
			voCluster voClust = (voCluster) vClust.elementAt(m);
			clusterID = voClust.getClusterID();
			vCompDisplay.add(new Vector());
			vComp = ClusterCompetencyByName(clusterID);
			for (int i = 0; i < vComp.size(); i++) {  
				Result = new String[4];

				voCompetency voComp = (voCompetency) vComp.elementAt(i);
				compID = voComp.getCompetencyID();
				compName = voComp.getCompetencyName();

				Vector RT = RatingTask();
				for (int j = 0; j < RT.size(); j++) {
					votblSurveyRating vo = (votblSurveyRating) RT.elementAt(j);
					RTID = vo.getRatingTaskID();
					RTCode = vo.getRatingCode();

					//RTID = 1, RTCode = CP
					//RTID = 2, RTCode = CPR

					Vector result = null;
					if(surveyLevel == 0) { //Competency level
						result = MeanResult(RTID, compID, 0);
					} else { //KB level
						result = KBMean(RTID, compID);
					}


					if (RTCode.equals("CP")) { 
						for (int k = 0; k < result.size(); k++) {                        
							String[] arr = (String[]) result.elementAt(k);

							//arr[0]: CompetencyID
							//arr[1]: Type
							//arr[2]: CAST(AVG(AvgMean) AS numeric(38, 2)) AS Result
							int type = Integer.parseInt(arr[1]);
							if(type == 1 || type == 4){
								Result[0] = compID + "";
								Result[1] = compName;
								if (type == 1) {
									Result[2] = CompTrimmedMeanforAll(RTID, compID) + "";
								} else {
									Result[3] = Double.parseDouble(arr[2]) + "";
								}
							}
						}
						try{othersValue = Double.parseDouble(Result[2]);}catch(Exception ex){othersValue = 0.0;}
						try{selfValue = Double.parseDouble(Result[3]);}catch(Exception ex){selfValue = 0.0;}

						if(positive){
							//3 conditions: 1) others must be more than (maxScale/2)
							//				2) both others and self results are not the same
							//				3) others must be more than self
							if(othersValue >= (maxScale/(2*1.0)) && (othersValue > selfValue) && (othersValue != selfValue)) {
								vCompDisplay.elementAt(m).add(Result);
							}
						} else if (!positive) {
							//3 conditions: 1) self must be more than (maxScale/2)
							//				2) both others and self results are not the same
							//				3) self must be more than others
							if(selfValue >= (maxScale/(2*1.0)) && (selfValue > othersValue) && (othersValue != selfValue)) {
								vCompDisplay.elementAt(m).add(Result);
							}
						}
					} //else if RTCode is CPR/FPR
				}
			}//End of for loop, Competency list
		}//end of for loop, cluster list
		/***********************
		 * Construct the table *
		 ***********************/
		boolean construct = false;
		for(int i=0; i<vClust.size(); i++){
			if(vCompDisplay.elementAt(i).size() != 0){
				construct = true;
				break;
			}
		}
		if(construct) {
			int startBorder = 0;
			//First time entering here, if last page row count is = 0, or >= row
			//Set the startBorder to the row

			if(lastPageRowCount == 0) {
				startBorder = row;
				lastPageRowCount = row;
			} else {
				startBorder = lastPageRowCount;
			}

			//Set up headers, Chun Yeong 1 Aug 2011
			//\u2264 writes 'Smaller than or Equals to' or <=
			//\u003E writes 'Greater than' or >
			String title = "";
			String titleDesc = "";
			if(positive){
				title = trans.tslt(templateLanguage, "Positive Blind Spots");
				titleDesc = trans.tslt(templateLanguage, "Competencies/KB where others rated you better than what you rated yourself " 
						+ "and where the rating from others is above the effective point " 
						+ "of the scale used (i.e.")+ " " + maxScale/(2*1.0) + " \u2264 " + trans.tslt(templateLanguage, "Other's Rating")
						+ " \u003E " + trans.tslt(templateLanguage, "Self Rating") + ")";
			}
			else {
				title = trans.tslt(templateLanguage, "Negative Blind Spots");
				titleDesc = trans.tslt(templateLanguage, "Competencies/KB where you rated yourself better than others rated you " 
						+ "and where your self rating is above the effective point " 
						+ "of the scale used (i.e. ")+ " " + maxScale/(2*1.0) + " \u2264 " + trans.tslt(templateLanguage, "Self Rating") 
						+ " \u003E " + trans.tslt(templateLanguage, "Other's Rating") + ")";
			}

			//To cater to if the negative table is starting on a new page
			//And reset the variables: startBorder, lastPageRowCount and currentPageHeight 
			//Check height and insert pagebreak where necessary
			int pageHeightLimit = 22272	;//Page limit is 22272
			int currentPageHeight = 1076;
			//calculate the height of the table that is being added.
			for(int i=startBorder; i <= row; i++){
				currentPageHeight += OO.getRowHeight(xSpreadsheet, i, startColumn);
			}

			currentPageHeight += 1614;

			if(currentPageHeight > pageHeightLimit || ((pageHeightLimit - currentPageHeight) <= 3000)){//adding the table will exceed a single page, insert page break
				//Draw the border
				OO.setTableBorder(xSpreadsheet, startColumn, endColumn - 1, row - 1, row - 1, false, false, false, false, false, true);
				//Insert page break
				insertPageBreak(xSpreadsheet, startColumn, endColumn, row );	
				//to move the table two lines down
				row += 2;
				//reset values
				startBorder = row;
				lastPageRowCount = row;
				currentPageHeight = 0;

			} //End of checking for page limit



			//Insert the title
			OO.insertString(xSpreadsheet, UnicodeHelper.getUnicodeStringAmp(title), row, firstColumn);
			OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
			OO.mergeCells(xSpreadsheet, startColumn, posCol - 1, row, row);
			row+=2;

			//Insert the description
			OO.insertString(xSpreadsheet, UnicodeHelper.getUnicodeStringAmp(titleDesc), row, firstColumn);
			OO.mergeCells(xSpreadsheet, startColumn, endColumn - 1, row, row);
			row+=2;

			//Insert 2 new labels CP and CPR before Gap and set font size to 12
			OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Positive"), row, posCol);
			OO.mergeCells(xSpreadsheet, posCol, negCol-1, row, row);
			OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Negative"), row, negCol);
			OO.mergeCells(xSpreadsheet, negCol, othersCol-1, row, row);
			OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Others"), row, othersCol);
			OO.mergeCells(xSpreadsheet, othersCol, selfCol-1, row, row);
			OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Self"), row, selfCol);
			OO.mergeCells(xSpreadsheet, selfCol, endColumn-1, row, row);

			OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
			OO.setFontSize(xSpreadsheet, posCol, selfCol, row, row, 12);
			OO.setCellAllignment(xSpreadsheet, posCol, selfCol, row, row, 1, 2);
			//OO.setBGColor(xSpreadsheet, startColumn, endColumn - 1, row, row, BGCOLOR);
			OO.setTableBorder(xSpreadsheet, startColumn, posCol - 1, row, row, false, false, false, false, false, true);
			OO.setTableBorder(xSpreadsheet, posCol, negCol - 1, row, row, false, false, true, true, true, true);
			OO.setTableBorder(xSpreadsheet, negCol, othersCol - 1, row, row, false, false, true, true, true, true);
			OO.setTableBorder(xSpreadsheet, othersCol, endColumn - 1, row, row, true, false, true, true, true, true);

			row++;

			for(int m = 0; m<vClust.size(); m++){
				voCluster voClust = (voCluster) vClust.elementAt(m);
				clusterName = voClust.getClusterName();

				//skip the cluster since it has no competency inside
				if(vCompDisplay.elementAt(m).size()==0)
					continue;

				OO.insertString(xSpreadsheet, clusterName.toUpperCase(), row, firstColumn);
				OO.mergeCells(xSpreadsheet, startColumn, posCol - 1, row, row);
				OO.setBGColor(xSpreadsheet, startColumn, endColumn - 1, row, row, BGCOLORCLUSTER);
				row++;
				for(Object obj: vCompDisplay.elementAt(m)) {

					/****************************************************************************
					 * Check if line exceeds new page. If yes, insert page break 			    * 
					 * This section is similar to BOTTOM checking before printing key behavior. *
					 ****************************************************************************/
					//To cater to if the competency name is starting on a new page
					//And reset the variables: startBorder, lastPageRowCount and currentPageHeight 
					//Check height and insert pagebreak where necessary
					pageHeightLimit = 22272	;//Page limit is 22272
					currentPageHeight = 1076;

					//calculate the height of the table that is being added.
					for(int i=startBorder; i <= row; i++){
						currentPageHeight += OO.getRowHeight(xSpreadsheet, i, startColumn);
					}

					currentPageHeight += 1076;

					if(currentPageHeight > pageHeightLimit || ((pageHeightLimit - currentPageHeight) <= 3000)){//adding the table will exceed a single page, insert page break
						//Draw the border
						OO.setTableBorder(xSpreadsheet, startColumn, endColumn - 1, row - 1, row - 1, false, false, false, false, false, true);
						//Insert page break
						insertPageBreak(xSpreadsheet, startColumn, endColumn, row );	
						//to move the table two lines down
						row += 2;
						//reset values
						startBorder = row;
						lastPageRowCount = row;
						currentPageHeight = 0;

						//Insert 2 new labels CP and CPR before Gap and set font size to 12
						OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Positive"), row, posCol);
						OO.mergeCells(xSpreadsheet, posCol, negCol-1, row, row);
						OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Negative"), row, negCol);
						OO.mergeCells(xSpreadsheet, negCol, othersCol-1, row, row);
						OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Others"), row, othersCol);
						OO.mergeCells(xSpreadsheet, othersCol, selfCol-1, row, row);
						OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Self"), row, selfCol);
						OO.mergeCells(xSpreadsheet, selfCol, endColumn-1, row, row);

						OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
						OO.setFontSize(xSpreadsheet, posCol, selfCol, row, row, 12);
						OO.setCellAllignment(xSpreadsheet, posCol, selfCol, row, row, 1, 2);
						OO.setTableBorder(xSpreadsheet, startColumn, posCol - 1, row, row, false, false, false, false, false, true);
						OO.setTableBorder(xSpreadsheet, posCol, negCol - 1, row, row, false, false, true, true, true, true);
						OO.setTableBorder(xSpreadsheet, negCol, othersCol - 1, row, row, false, false, true, true, true, true);
						OO.setTableBorder(xSpreadsheet, othersCol, endColumn - 1, row, row, true, false, true, true, true, true);
						row++;
					} //End of checking for page limit


					//Print competency
					//temp[0] = competency id; temp[1] = competency name; temp[2] = other's rating; temp[3] = self rating; 
					String[] temp = (String[]) obj;
					compID = Integer.parseInt(temp[0]);
					compName = temp[1];
					try{othersValue = Double.parseDouble(temp[2]);}catch(Exception ex){othersValue = 0.0;}
					try{selfValue = Double.parseDouble(temp[3]);}catch(Exception ex){selfValue = 0.0;}
					if(positive){ //Positive, insert into positive column
						OO.insertString(xSpreadsheet, "X", row, posCol);
					} else if (!positive){ //Negative, insert into negative column
						OO.insertString(xSpreadsheet, "X", row, negCol);
					}

					//Insert Competency Name
					//Added translation to competency name, Chun Yeong 1 Aug 2011
					OO.insertString(xSpreadsheet, compCount + ". " + UnicodeHelper.getUnicodeStringAmp(getTranslatedCompetency(temp[1]).elementAt(0).toString()), row, firstColumn);
					OO.mergeCells(xSpreadsheet, startColumn, posCol - 1, row, row);

					//Insert Others and Self values
					OO.mergeCells(xSpreadsheet, posCol, negCol-1, row, row);
					OO.mergeCells(xSpreadsheet, negCol, othersCol-1, row, row);
					OO.insertNumeric(xSpreadsheet, othersValue, row, othersCol);
					OO.insertNumeric(xSpreadsheet, selfValue, row, selfCol);
					OO.setFontSize(xSpreadsheet, posCol, selfCol, row, row, 12);

					//Centralize and draw border for the data
					OO.setCellAllignment(xSpreadsheet, posCol, selfCol, row, row, 1, 2);

					//Color competency
					OO.setBGColor(xSpreadsheet, startColumn, endColumn - 1, row, row, BGCOLOR);

					//Increment Competency count
					compCount++;

					//Add border lines
					OO.setTableBorder(xSpreadsheet, startColumn, posCol - 1, row, row, false, false, true, true, true, true);
					OO.setTableBorder(xSpreadsheet, posCol, negCol - 1, row, row, false, false, true, true, true, true);
					OO.setTableBorder(xSpreadsheet, negCol, othersCol - 1, row, row, false, false, true, true, true, true);
					OO.setTableBorder(xSpreadsheet, othersCol, endColumn - 1, row, row, true, false, true, true, true, true);

					row++;


					/*************************************
					 * Print KB list for each competency *
					 *************************************/
					int KBID = 0;
					String KBName = "";

					Vector KBList = KBList(compID);
					if(KBList.size() != 0){
						for (int j = 0; j < KBList.size(); j++) {
							voKeyBehaviour voKB = (voKeyBehaviour) KBList.elementAt(j);
							KBID = voKB.getKeyBehaviourID();
							KBName = voKB.getKeyBehaviour();

							Double[] KBResult = new Double[]{0.0, 0.0};
							Vector RT = RatingTask();

							for (int k = 0; k < RT.size(); k++) {
								votblSurveyRating vo = (votblSurveyRating) RT.elementAt(k);

								RTID = vo.getRatingTaskID();
								RTCode = vo.getRatingCode();

								if (RTCode.equals("CP")) {
									Vector result = MeanResult(RTID, compID, KBID);

									for (int l = 0; l < result.size(); l++) {
										String[] arr = (String[]) result.elementAt(l);

										//arr[0]: CompetencyID
										//arr[1]: Type
										//arr[2]: AvgMean as Result
										int type = Integer.parseInt(arr[1]);
										if(type == 1){
											KBResult[0] = Double.parseDouble(arr[2]); 
										} else if (type == 4) {
											KBResult[1] = Double.parseDouble(arr[2]); 
										}
									} // End of for loop, results size

									//Skip when ratings of both others and self are the same.
									if( (KBResult[0] > KBResult[1]) || (KBResult[0] < KBResult[1]) ){

										/*************************************************************************
										 * Check if line exceeds new page. If yes, insert page break 			 * 
										 * This section is similar to ABOVE checking before printing competency. *
										 *************************************************************************/
										//To cater to if the Key Behavior name is starting on a new page
										//And reset the variables: startBorder, lastPageRowCount and currentPageHeight 
										//Check height and insert pagebreak where necessary
										pageHeightLimit = 22272	;//Page limit is 22272
										currentPageHeight = 1076;

										//calculate the height of the table that is being added.
										for(int i=startBorder; i <= row; i++){
											currentPageHeight += OO.getRowHeight(xSpreadsheet, i, startColumn);
										}

										currentPageHeight += 1076;

										if(currentPageHeight > pageHeightLimit){//adding the table will exceed a single page, insert page break
											//Draw the border
											OO.setTableBorder(xSpreadsheet, startColumn, endColumn - 1, row - 1, row - 1, false, false, false, false, false, true);
											//Insert page break
											insertPageBreak(xSpreadsheet, startColumn, endColumn, row );
											//to move the table two lines down
											row += 2;
											//reset values
											startBorder = row;
											lastPageRowCount = row;
											currentPageHeight = 0;

											//Insert 2 new labels CP and CPR before Gap and set font size to 12
											OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Positive"), row, posCol);
											OO.mergeCells(xSpreadsheet, posCol, negCol-1, row, row);
											OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Negative"), row, negCol);
											OO.mergeCells(xSpreadsheet, negCol, othersCol-1, row, row);
											OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Others"), row, othersCol);
											OO.mergeCells(xSpreadsheet, othersCol, selfCol-1, row, row);
											OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Self"), row, selfCol);
											OO.mergeCells(xSpreadsheet, selfCol, endColumn-1, row, row);

											OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
											OO.setFontSize(xSpreadsheet, posCol, selfCol, row, row, 12);
											OO.setCellAllignment(xSpreadsheet, posCol, selfCol, row, row, 1, 2);
											OO.setTableBorder(xSpreadsheet, startColumn, posCol - 1, row, row, false, false, false, false, false, true);
											OO.setTableBorder(xSpreadsheet, posCol, negCol - 1, row, row, false, false, true, true, true, true);
											OO.setTableBorder(xSpreadsheet, negCol, othersCol - 1, row, row, false, false, true, true, true, true);
											OO.setTableBorder(xSpreadsheet, othersCol, endColumn - 1, row, row, true, false, true, true, true, true);
											row++;
										} //End of checking for page limit

										//Insert Key behavior Name
										//Added translation to the key behaviour name, Chun Yeong 1 Aug 2011
										OO.insertString(xSpreadsheet, "KB: " + getTranslatedKeyBehavior(UnicodeHelper.getUnicodeStringAmp(KBName)), row, firstColumn);
										OO.mergeCells(xSpreadsheet, 0, posCol-1, row, row);

										//Insert positive or negative
										if(KBResult[0] > KBResult[1]){ // If others(All) > self
											OO.insertString(xSpreadsheet, "X", row, posCol);
										} else if(KBResult[0] < KBResult[1]){ // If others(All) < self
											OO.insertString(xSpreadsheet, "X", row, negCol);
										}

										OO.mergeCells(xSpreadsheet, posCol, negCol-1, row, row);
										OO.mergeCells(xSpreadsheet, negCol, othersCol-1, row, row);

										//Insert Others and Self values
										OO.insertNumeric(xSpreadsheet, KBResult[0], row, othersCol);
										OO.insertNumeric(xSpreadsheet, KBResult[1], row, selfCol);
										OO.setFontSize(xSpreadsheet, posCol, selfCol, row, row, 12);

										//Centralize the data
										OO.setCellAllignment(xSpreadsheet, posCol, selfCol, row, row, 1, 2);

										//Draw borders
										OO.setTableBorder(xSpreadsheet, startColumn, posCol - 1, row, row, false, false, true, true, false, false);
										OO.setTableBorder(xSpreadsheet, posCol, negCol - 1, row, row, false, false, true, true, false, false);
										OO.setTableBorder(xSpreadsheet, negCol, othersCol - 1, row, row, false, false, true, true, false, false);
										OO.setTableBorder(xSpreadsheet, othersCol, endColumn - 1, row, row, true, false, true, true, false, false);

										row++;
									}

								} // End of if RTCode is 'CP'

							}//End of for loop, RT size

						}//End of for loop, KB List

						//Add border lines
						OO.setTableBorder(xSpreadsheet, startColumn, endColumn - 1, row, row, false, false, true, true, false, true);
						OO.setTableBorder(xSpreadsheet, posCol, negCol - 1, row, row, false, false, true, true, false, false);
						OO.setTableBorder(xSpreadsheet, negCol, othersCol - 1, row, row, false, false, true, true, false, false);
						OO.setTableBorder(xSpreadsheet, othersCol, endColumn - 1, row, row, true, false, true, true, false, false);
						row++;
					} //End if, when there are KB to display

				} // End of for loop, each competency to display
			} // end of for loop , cluster list
		} //End if, table of data to display
	} // End of InsertPositiveBlindSpotAnalysis

	/**
	 * This method inserts the blind spot analysis into individual report.
	 * Warning: Currently only applicable for Current Proficiencies (CP). Missing indonesian translation.
	 * @param positive - Either TRUE for positive blind spots or FALSE for negative blind spots
	 * @throws Exception
	 * 
	 * @author Chun Yeong
	 * @since v1.3.12.96 //27 May 2011
	 */
	public void InsertBlindSpotAnalysis(Boolean positive) throws Exception{
		int surveyLevel = Integer.parseInt(surveyInfo[0]);

		int compCount = 1;
		int compID = 0;
		String compName = "";
		int maxScale = MaxScale();

		column = 0;

		int firstColumn = 0;
		//Added to define columns where positive, negative, others and self are inserted in spreadsheet
		int posCol = 6;
		int negCol = 8;
		int othersCol = 10;
		int selfCol = 11;
		double othersValue = 0.0;
		double selfValue = 0.0;
		Vector vComp = CompetencyByName();
		Vector vCompDisplay = new Vector();
		String[] Result = new String[4];

		int RTID = 0;
		String RTCode = "";

		for (int i = 0; i < vComp.size(); i++) {  
			Result = new String[4];

			voCompetency voComp = (voCompetency) vComp.elementAt(i);
			compID = voComp.getCompetencyID();
			compName = voComp.getCompetencyName();

			Vector RT = RatingTask();
			for (int j = 0; j < RT.size(); j++) {
				votblSurveyRating vo = (votblSurveyRating) RT.elementAt(j);
				RTID = vo.getRatingTaskID();
				RTCode = vo.getRatingCode();

				//RTID = 1, RTCode = CP
				//RTID = 2, RTCode = CPR

				Vector result = null;
				if(surveyLevel == 0) { //Competency level
					result = MeanResult(RTID, compID, 0);
				} else { //KB level
					result = KBMean(RTID, compID);
				}


				if (RTCode.equals("CP")) { 
					for (int k = 0; k < result.size(); k++) {                        
						String[] arr = (String[]) result.elementAt(k);

						//arr[0]: CompetencyID
						//arr[1]: Type
						//arr[2]: CAST(AVG(AvgMean) AS numeric(38, 2)) AS Result
						int type = Integer.parseInt(arr[1]);
						if(type == 1 || type == 4){
							Result[0] = compID + "";
							Result[1] = compName;
							if (type == 1) {
								Result[2] = CompTrimmedMeanforAll(RTID, compID) + "";
							} else {
								Result[3] = Double.parseDouble(arr[2]) + "";
							}
						}
					}
					try{othersValue = Double.parseDouble(Result[2]);}catch(Exception ex){othersValue = 0.0;}
					try{selfValue = Double.parseDouble(Result[3]);}catch(Exception ex){selfValue = 0.0;}

					if(positive){
						//3 conditions: 1) others must be more than (maxScale/2)
						//				2) both others and self results are not the same
						//				3) others must be more than self
						if(othersValue >= (4.0) && (othersValue > selfValue) && (othersValue != selfValue)) {
							vCompDisplay.add(Result);
						}
					} else if (!positive) {
						//3 conditions: 1) self must be more than (maxScale/2)
						//				2) both others and self results are not the same
						//				3) self must be more than others
						if(selfValue >= (4.0) && (selfValue > othersValue) && (othersValue != selfValue)) {
							vCompDisplay.add(Result);
						}
					}
				} //else if RTCode is CPR/FPR
			}
		}//End of for loop, Competency list

		/***********************
		 * Construct the table *
		 ***********************/
		if(vCompDisplay.size() != 0) {

			int startBorder = 0;
			//First time entering here, if last page row count is = 0, or >= row
			//Set the startBorder to the row

			if(lastPageRowCount == 0) {
				startBorder = row;
				lastPageRowCount = row;
			} else {
				startBorder = lastPageRowCount;
			}

			//Set up headers, Chun Yeong 1 Aug 2011
			//\u2264 writes 'Smaller than or Equals to' or <=
			//\u003E writes 'Greater than' or >
			String title = "";
			String titleDesc = "";
			if(positive){
				title = trans.tslt(templateLanguage, "Positive Blind Spots");
				titleDesc = trans.tslt(templateLanguage, "Competencies/KB where others rated you better than what you rated yourself " 
						+ "and where the rating from others is above the effective point " 
						+ "of the scale used (i.e.")+ " " + 4.0 + " \u2264 " + trans.tslt(templateLanguage, "Other's Rating")
						+ " \u003E " + trans.tslt(templateLanguage, "Self Rating") + ")";
			}
			else {
				title = trans.tslt(templateLanguage, "Negative Blind Spots");
				titleDesc = trans.tslt(templateLanguage, "Competencies/KB where you rated yourself better than others rated you " 
						+ "and where your self rating is above the effective point " 
						+ "of the scale used (i.e. ")+ " " + 4.0 + " \u2264 " + trans.tslt(templateLanguage, "Self Rating") 
						+ " \u003E " + trans.tslt(templateLanguage, "Other's Rating") + ")";
			}

			//To cater to if the negative table is starting on a new page
			//And reset the variables: startBorder, lastPageRowCount and currentPageHeight 
			//Check height and insert pagebreak where necessary
			int pageHeightLimit = 22272	;//Page limit is 22272
			int currentPageHeight = 1076;
			//calculate the height of the table that is being added.
			for(int i=startBorder; i <= row; i++){
				currentPageHeight += OO.getRowHeight(xSpreadsheet, i, startColumn);
			}

			currentPageHeight += 1614;

			if(currentPageHeight > pageHeightLimit || ((pageHeightLimit - currentPageHeight) <= 3000)){//adding the table will exceed a single page, insert page break
				//Draw the border
				OO.setTableBorder(xSpreadsheet, startColumn, endColumn - 1, row - 1, row - 1, false, false, false, false, false, true);
				//Insert page break
				insertPageBreak(xSpreadsheet, startColumn, endColumn, row );	
				//to move the table two lines down
				row += 2;
				//reset values
				startBorder = row;
				lastPageRowCount = row;
				currentPageHeight = 0;

			} //End of checking for page limit



			//Insert the title
			OO.insertString(xSpreadsheet, UnicodeHelper.getUnicodeStringAmp(title), row, firstColumn);
			OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
			OO.mergeCells(xSpreadsheet, startColumn, posCol - 1, row, row);
			row+=2;

			//Insert the description
			OO.insertString(xSpreadsheet, UnicodeHelper.getUnicodeStringAmp(titleDesc), row, firstColumn);
			OO.mergeCells(xSpreadsheet, startColumn, endColumn - 1, row, row);
			row+=2;

			//Insert 2 new labels CP and CPR before Gap and set font size to 12
			OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Positive"), row, posCol);
			OO.mergeCells(xSpreadsheet, posCol, negCol-1, row, row);
			OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Negative"), row, negCol);
			OO.mergeCells(xSpreadsheet, negCol, othersCol-1, row, row);
			OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Others"), row, othersCol);
			OO.mergeCells(xSpreadsheet, othersCol, selfCol-1, row, row);
			OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Self"), row, selfCol);
			OO.mergeCells(xSpreadsheet, selfCol, endColumn-1, row, row);

			OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
			OO.setFontSize(xSpreadsheet, posCol, selfCol, row, row, 12);
			OO.setCellAllignment(xSpreadsheet, posCol, selfCol, row, row, 1, 2);
			//OO.setBGColor(xSpreadsheet, startColumn, endColumn - 1, row, row, BGCOLOR);
			OO.setTableBorder(xSpreadsheet, startColumn, posCol - 1, row, row, false, false, false, false, false, true);
			OO.setTableBorder(xSpreadsheet, posCol, negCol - 1, row, row, false, false, true, true, true, true);
			OO.setTableBorder(xSpreadsheet, negCol, othersCol - 1, row, row, false, false, true, true, true, true);
			OO.setTableBorder(xSpreadsheet, othersCol, endColumn - 1, row, row, true, false, true, true, true, true);

			row++;


			for(Object obj: vCompDisplay) {

				/****************************************************************************
				 * Check if line exceeds new page. If yes, insert page break 			    * 
				 * This section is similar to BOTTOM checking before printing key behavior. *
				 ****************************************************************************/
				//To cater to if the competency name is starting on a new page
				//And reset the variables: startBorder, lastPageRowCount and currentPageHeight 
				//Check height and insert pagebreak where necessary
				pageHeightLimit = 22272	;//Page limit is 22272
				currentPageHeight = 1076;

				//calculate the height of the table that is being added.
				for(int i=startBorder; i <= row; i++){
					currentPageHeight += OO.getRowHeight(xSpreadsheet, i, startColumn);
				}

				currentPageHeight += 1076;

				if(currentPageHeight > pageHeightLimit || ((pageHeightLimit - currentPageHeight) <= 3000)){//adding the table will exceed a single page, insert page break
					//Draw the border
					OO.setTableBorder(xSpreadsheet, startColumn, endColumn - 1, row - 1, row - 1, false, false, false, false, false, true);
					//Insert page break
					insertPageBreak(xSpreadsheet, startColumn, endColumn, row );	
					//to move the table two lines down
					row += 2;
					//reset values
					startBorder = row;
					lastPageRowCount = row;
					currentPageHeight = 0;

					//Insert 2 new labels CP and CPR before Gap and set font size to 12
					OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Positive"), row, posCol);
					OO.mergeCells(xSpreadsheet, posCol, negCol-1, row, row);
					OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Negative"), row, negCol);
					OO.mergeCells(xSpreadsheet, negCol, othersCol-1, row, row);
					OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Others"), row, othersCol);
					OO.mergeCells(xSpreadsheet, othersCol, selfCol-1, row, row);
					OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Self"), row, selfCol);
					OO.mergeCells(xSpreadsheet, selfCol, endColumn-1, row, row);

					OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
					OO.setFontSize(xSpreadsheet, posCol, selfCol, row, row, 12);
					OO.setCellAllignment(xSpreadsheet, posCol, selfCol, row, row, 1, 2);
					OO.setTableBorder(xSpreadsheet, startColumn, posCol - 1, row, row, false, false, false, false, false, true);
					OO.setTableBorder(xSpreadsheet, posCol, negCol - 1, row, row, false, false, true, true, true, true);
					OO.setTableBorder(xSpreadsheet, negCol, othersCol - 1, row, row, false, false, true, true, true, true);
					OO.setTableBorder(xSpreadsheet, othersCol, endColumn - 1, row, row, true, false, true, true, true, true);
					row++;
				} //End of checking for page limit


				//Print competency
				//temp[0] = competency id; temp[1] = competency name; temp[2] = other's rating; temp[3] = self rating; 
				String[] temp = (String[]) obj;
				compID = Integer.parseInt(temp[0]);
				compName = temp[1];
				try{othersValue = Double.parseDouble(temp[2]);}catch(Exception ex){othersValue = 0.0;}
				try{selfValue = Double.parseDouble(temp[3]);}catch(Exception ex){selfValue = 0.0;}
				if(positive){ //Positive, insert into positive column
					OO.insertString(xSpreadsheet, "X", row, posCol);
				} else if (!positive){ //Negative, insert into negative column
					OO.insertString(xSpreadsheet, "X", row, negCol);
				}

				//Insert Competency Name
				//Added translation to competency name, Chun Yeong 1 Aug 2011
				OO.insertString(xSpreadsheet, compCount + ". " + UnicodeHelper.getUnicodeStringAmp(getTranslatedCompetency(temp[1]).elementAt(0).toString()), row, firstColumn);
				OO.mergeCells(xSpreadsheet, startColumn, posCol - 1, row, row);

				//Insert Others and Self values
				OO.mergeCells(xSpreadsheet, posCol, negCol-1, row, row);
				OO.mergeCells(xSpreadsheet, negCol, othersCol-1, row, row);
				OO.insertNumeric(xSpreadsheet, othersValue, row, othersCol);
				OO.insertNumeric(xSpreadsheet, selfValue, row, selfCol);
				OO.setFontSize(xSpreadsheet, posCol, selfCol, row, row, 12);

				//Centralize and draw border for the data
				OO.setCellAllignment(xSpreadsheet, posCol, selfCol, row, row, 1, 2);

				//Color competency
				OO.setBGColor(xSpreadsheet, startColumn, endColumn - 1, row, row, BGCOLOR);

				//Increment Competency count
				compCount++;

				//Add border lines
				OO.setTableBorder(xSpreadsheet, startColumn, posCol - 1, row, row, false, false, true, true, true, true);
				OO.setTableBorder(xSpreadsheet, posCol, negCol - 1, row, row, false, false, true, true, true, true);
				OO.setTableBorder(xSpreadsheet, negCol, othersCol - 1, row, row, false, false, true, true, true, true);
				OO.setTableBorder(xSpreadsheet, othersCol, endColumn - 1, row, row, true, false, true, true, true, true);

				row++;


				/*************************************
				 * Print KB list for each competency *
				 *************************************/
				int KBID = 0;
				String KBName = "";

				Vector KBList = KBList(compID);
				if(KBList.size() != 0){
					for (int j = 0; j < KBList.size(); j++) {
						voKeyBehaviour voKB = (voKeyBehaviour) KBList.elementAt(j);
						KBID = voKB.getKeyBehaviourID();
						KBName = voKB.getKeyBehaviour();

						Double[] KBResult = new Double[]{0.0, 0.0};
						Vector RT = RatingTask();

						for (int k = 0; k < RT.size(); k++) {
							votblSurveyRating vo = (votblSurveyRating) RT.elementAt(k);

							RTID = vo.getRatingTaskID();
							RTCode = vo.getRatingCode();

							if (RTCode.equals("CP")) {
								Vector result = MeanResult(RTID, compID, KBID);

								for (int l = 0; l < result.size(); l++) {
									String[] arr = (String[]) result.elementAt(l);

									//arr[0]: CompetencyID
									//arr[1]: Type
									//arr[2]: AvgMean as Result
									int type = Integer.parseInt(arr[1]);
									if(type == 1){
										KBResult[0] = Double.parseDouble(arr[2]); 
									} else if (type == 4) {
										KBResult[1] = Double.parseDouble(arr[2]); 
									}
								} // End of for loop, results size

								//Skip when ratings of both others and self are the same.
								if( (KBResult[0] > KBResult[1]) || (KBResult[0] < KBResult[1]) ){

									/*************************************************************************
									 * Check if line exceeds new page. If yes, insert page break 			 * 
									 * This section is similar to ABOVE checking before printing competency. *
									 *************************************************************************/
									//To cater to if the Key Behavior name is starting on a new page
									//And reset the variables: startBorder, lastPageRowCount and currentPageHeight 
									//Check height and insert pagebreak where necessary
									pageHeightLimit = 22272	;//Page limit is 22272
									currentPageHeight = 1076;

									//calculate the height of the table that is being added.
									for(int i=startBorder; i <= row; i++){
										currentPageHeight += OO.getRowHeight(xSpreadsheet, i, startColumn);
									}

									currentPageHeight += 1076;

									if(currentPageHeight > pageHeightLimit){//adding the table will exceed a single page, insert page break
										//Draw the border
										OO.setTableBorder(xSpreadsheet, startColumn, endColumn - 1, row - 1, row - 1, false, false, false, false, false, true);
										//Insert page break
										insertPageBreak(xSpreadsheet, startColumn, endColumn, row );
										//to move the table two lines down
										row += 2;
										//reset values
										startBorder = row;
										lastPageRowCount = row;
										currentPageHeight = 0;

										//Insert 2 new labels CP and CPR before Gap and set font size to 12
										OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Positive"), row, posCol);
										OO.mergeCells(xSpreadsheet, posCol, negCol-1, row, row);
										OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Negative"), row, negCol);
										OO.mergeCells(xSpreadsheet, negCol, othersCol-1, row, row);
										OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Others"), row, othersCol);
										OO.mergeCells(xSpreadsheet, othersCol, selfCol-1, row, row);
										OO.insertString(xSpreadsheet, trans.tslt(templateLanguage, "Self"), row, selfCol);
										OO.mergeCells(xSpreadsheet, selfCol, endColumn-1, row, row);

										OO.setFontBold(xSpreadsheet, startColumn, endColumn, row, row);
										OO.setFontSize(xSpreadsheet, posCol, selfCol, row, row, 12);
										OO.setCellAllignment(xSpreadsheet, posCol, selfCol, row, row, 1, 2);
										OO.setTableBorder(xSpreadsheet, startColumn, posCol - 1, row, row, false, false, false, false, false, true);
										OO.setTableBorder(xSpreadsheet, posCol, negCol - 1, row, row, false, false, true, true, true, true);
										OO.setTableBorder(xSpreadsheet, negCol, othersCol - 1, row, row, false, false, true, true, true, true);
										OO.setTableBorder(xSpreadsheet, othersCol, endColumn - 1, row, row, true, false, true, true, true, true);
										row++;
									} //End of checking for page limit

									//Insert Key behavior Name
									//Added translation to the key behaviour name, Chun Yeong 1 Aug 2011
									OO.insertString(xSpreadsheet, "KB: " + getTranslatedKeyBehavior(UnicodeHelper.getUnicodeStringAmp(KBName)), row, firstColumn);
									OO.mergeCells(xSpreadsheet, 0, posCol-1, row, row);
									int rowNum = UnicodeHelper.getUnicodeStringAmp(KBName).length() / 38 + 1;
                                    OO.setRowHeight(xSpreadsheet, row, firstColumn, 500*rowNum);
									//Insert positive or negative
									if(KBResult[0] > KBResult[1]){ // If others(All) > self
										OO.insertString(xSpreadsheet, "X", row, posCol);
									} else if(KBResult[0] < KBResult[1]){ // If others(All) < self
										OO.insertString(xSpreadsheet, "X", row, negCol);
									}

									OO.mergeCells(xSpreadsheet, posCol, negCol-1, row, row);
									OO.mergeCells(xSpreadsheet, negCol, othersCol-1, row, row);

									//Insert Others and Self values
									OO.insertNumeric(xSpreadsheet, KBResult[0], row, othersCol);
									OO.insertNumeric(xSpreadsheet, KBResult[1], row, selfCol);
									OO.setFontSize(xSpreadsheet, posCol, selfCol, row, row, 12);

									//Centralize the data
									OO.setCellAllignment(xSpreadsheet, posCol, selfCol, row, row, 1, 2);

									//Draw borders
									OO.setTableBorder(xSpreadsheet, startColumn, posCol - 1, row, row, false, false, true, true, false, false);
									OO.setTableBorder(xSpreadsheet, posCol, negCol - 1, row, row, false, false, true, true, false, false);
									OO.setTableBorder(xSpreadsheet, negCol, othersCol - 1, row, row, false, false, true, true, false, false);
									OO.setTableBorder(xSpreadsheet, othersCol, endColumn - 1, row, row, true, false, true, true, false, false);

									row++;
								}

							} // End of if RTCode is 'CP'

						}//End of for loop, RT size

					}//End of for loop, KB List

					//Add border lines
					OO.setTableBorder(xSpreadsheet, startColumn, endColumn - 1, row, row, false, false, true, true, false, true);
					OO.setTableBorder(xSpreadsheet, posCol, negCol - 1, row, row, false, false, true, true, false, false);
					OO.setTableBorder(xSpreadsheet, negCol, othersCol - 1, row, row, false, false, true, true, false, false);
					OO.setTableBorder(xSpreadsheet, othersCol, endColumn - 1, row, row, true, false, true, true, false, false);
					row++;
				} //End if, when there are KB to display

			} // End of for loop, each competency to display

		} //End if, table of data to display

	} // End of InsertPositiveBlindSpotAnalysis

	public static void main (String [] args) throws IOException, Exception
	{	

		ImptIndividualReport IR = new ImptIndividualReport();

		int surveyID = 438;
		int targetID = 2328;//6636
		//System.out.println("TEST");
		long past = System.currentTimeMillis();
		// Commented by Tracy 01 Sep 08 IR.Report(489, 7711, 2, "Individual Report (Test).xls", 2);	
		long now = System.currentTimeMillis();

		IR.Report(498,498,6611,6404,"IndividualReport220908153454.xls",2,"", "", "", 0, "");
		//System.out.println("Time taken: " + (past-now)/1000);
		/*
		int surveyID = 459;
		int targetID = 2410;
		IR.ReportToyota(surveyID, targetID, 124, "IndividualReportToyota.xls");
		 */
		/*
		int surveyID = 458;
		int targetID = 6497;
		IR.ReportAllianz(surveyID, targetID, 6450, "Individual Report Allianz.xls");
		 */

		//int surveyID = 466;
		//int targetID = 6612;
		//int surveyID = 462;
		//int targetID = 1076;
		//IR.reportDevelopmentMap(surveyID, targetID,"Development Map.xls", true);

		//int surveyID = 459;
		//int targetID = 2105;
		//int pkUser = 1;
		//int targetID [] = {7155,7156,7160,7162,7165,7170,7171,7172,7174,7175,7177,7180,7181,7182,7187,7191,7192,7199,7200,7205,7211,7214,7220,7221,7223,7226,7229,7230,7231,7233};
		//String file_name [] = {"Natcha Teinwutichai.xls", "Nawanan Chalard.xls", "Nopparat Pornrattanapitak.xls", "Nuchanart Pitchayanan.xls", "Ornsuda Pornpattrapong.xls", "Panya Kitcharoenkankul.xls", "Paparnin Lertpaitoonpan.xls", "Paranee Leelasettakul.xls", "Patanee Chokdeepanich.xls", "Patcharin Jingkaojai.xls", "Paweenuch Sutthirat.xls", "Pijika Wongwiwat.xls", "Pinnapa Nakham.xls", "Piyada Soontornchaiya.xls", "Prasarn Gritsanawarun.xls", "Rungmit Chunhengpan.xls","Rungrawee Selananda.xls", "Saowanee Jarusruangchai.xls", "Saroch Pornputtkul.xls", "Siriporn Saeteaw.xls", "Somsaluay Suwanprasop.xls", "Sukanya Niyomthamakit.xls", "Surakit Chunharotrit.xls", "Surat Pornputtichai.xls", "Sutisophan Chuaywongyart.xls", "Thanarat Somphol.xls", "Usaporn Preechawud.xls", "Vithita Prasopakarakit.xls", "Vuthiphan Vuthiphan.xls", "Wannaporn Sukhonpun.xls"};

		/*int targetID [] = {2394};
		String file_name [] = {"WEERAYUT RASSADONVIJIT.xls"};
		 */
		//for(int i=0; i<targetID.length; i++)
		//IR.Report(surveyID, targetID[i], 6559, file_name[i], 2);



		//System.exit(1);
	}

	public int getFormat() {
		return format;
	}

	public void setFormat(int format) {
		this.format = format;
	}

	public int getPageCount() {
		return pageCount;
	}

	public void setPageCount(int pageCount) {
		this.pageCount = pageCount;
	} 
	
	public void insertPageBreak(XSpreadsheet xs, int startColumn , int endColumn, int row){
	try{ OO.insertPageBreak(xs, startColumn, endColumn, row);}
	catch(Exception e){
		
	}
	setPageCount(pageCount + 1);
	}
}
