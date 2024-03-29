<%@ page import="java.sql.*,
                 java.io.*,
                 java.text.DateFormat,
                 java.util.*,
                 java.util.Date,
                 java.text.*,
                 java.lang.String,
				 CP_Classes.vo.*"%>
				 
<jsp:useBean id="logchk" class="CP_Classes.Login" scope="session"/>                   
<jsp:useBean id="Rpt9" class="CP_Classes.DevelopmentGuide" scope="session"/>
<jsp:useBean id="setting" class="CP_Classes.Setting" scope="session"/>
<jsp:useBean id="CE_Survey" class="CP_Classes.Create_Edit_Survey" scope="session"/>
<jsp:useBean id="user" class="CP_Classes.User" scope="session"/>
<jsp:useBean id="trans" class="CP_Classes.Translate" scope="session"/>
<jsp:useBean id="userJ" class="CP_Classes.User_Jenty" scope="session"/>     
<% 	// added to check whether organisation is a consulting company
// Mark Oei 09 Mar 2010 %>
<jsp:useBean id="Org" class="CP_Classes.Organization" scope="session"/>


<html>
<head>
<meta http-equiv="Content-Type" content="text/html">
<%@ page pageEncoding="UTF-8"%>
<%// by lydia Date 05/09/2008 Fix jsp file to support Thai language %>
</head>
<SCRIPT LANGUAGE="JavaScript">
var target
function check(field)
{
	var check= false;
	
	for (i = 0; i < field.length; i++) 
	{
		if(field[i].checked)
		{
			target = field[i].value;
			check = true;
		}
		else if(field[i].selected != "" && check == false)
		{
			if(field[i].value != "")
			{
				check = true;
			}
		}
	}

	return check;
		
}

function chkSelect(form,field)
{	
	x = document.DevelopmentGuide
	var flag = false;
	
	if(check(field))
	{	
		if(target == 1)
		{
			if(check(x.selSurvey))
			{
				flag = true;
			}else{
				alert("<%=trans.tslt("Please select survey")%>");
			}
		}
		else if(target == 2)
		{
			if(check(x.selSurvey0) && check(x.selGroup) && check(x.selTarget)){
				flag = true;
			}else{		
				var msg = "Please select following information: \n";
				if(x.selSurvey0.selectedIndex==""){
					msg += " - Survey \n";
				}
				if(x.selGroup.selectedIndex==""){
					msg += " - Group \n";
				}
				if(x.selTarget.selectedIndex==""){
					msg += " - Target \n";
				}
				if(x.selDept.selectedIndex==""){
					msg += " - Department \n";
				}
				if(x.selDiv.selectedIndex==""){
					msg += " - Division \n";
				}
				alert(msg);
			}
		}
		else if(target == 3)
		{
				flag = true;
		}

		if(flag)
		{
			form.action="Report_DevelopmentGuide.jsp?preview=1";
			form.method="post";
			form.submit();
		}
		
	}else{
			alert("<%=trans.tslt("Please select the source type")%>");
		}
}
function proceed(form,field)
{
	form.action="Report_DevelopmentGuide.jsp?proceed="+field.value;
	form.method="post";
	form.submit();
}

function surv(form,field)
{
	if(field.value=='')return;
	
	form.action="Report_DevelopmentGuide.jsp?surv="+field.value;
	form.method="post";
	form.submit();
}

function surv1(form,field)
{
	if(field.value=='')return;
	
	form.action="Report_DevelopmentGuide.jsp?surv1="+field.value;
	form.method="post";
	form.submit();
}

function group(form,field1, field2, field3)
{
	form.action="Report_DevelopmentGuide.jsp?group="+field1.value + "&div=" + field2.value + "&dept=" + field3.value ;
	form.method="post";
	form.submit();
}

function populateDept(form, field)
{
	form.action="Report_DevelopmentGuide.jsp?div=" + field.value;
	form.method="post";
	form.submit();
}

function populateGrp(form, field1, field2)
{
	form.action="Report_DevelopmentGuide.jsp?div="+ field1.value + "&dept=" + field2.value;
	form.method="post";
	form.submit();
}

//Gwen Oh - 27/09/2011: Add selectLang function to filter templates with respect to the selected language
//Yiping - 05/01/2012: Add Chinese Language
function selectLang(form, field)
{	if(field.value == 2 || field.value == 5){
		alert("Templates in this language are currently not available.\nThe default English template will be used.");
	}
	form.action = "Report_DevelopmentGuide.jsp?selectLang=" + field.value;
	form.method = "post";
	form.submit();	
}

//Gwen Oh - 04/10/2011: Add selectTemplate function
function selectTemplate(form, field)
{	
	form.action = "Report_DevelopmentGuide.jsp?selectTemplate=" + field.value;
	form.method = "post";
	form.submit();	
}
</script>

<body>
<%

String username=(String)session.getAttribute("username");

  if (!logchk.isUsable(username)) 
  {%> <font size="2">
   
	<script>
	parent.location.href = "index.jsp";
	</script>
<%  } 
  else 
  { 

int langID = Rpt9.getLang();

if(request.getParameter("proceed") != null)
{ 
	int var2 = new Integer(request.getParameter("selOrg")).intValue();
	logchk.setOrg(var2);
}
//Gwen Oh - 04/10/2011: Check for change in selected language or template
else if(request.getParameter("selectLang") != null) {
	
	langID = Integer.parseInt(request.getParameter("selectLang"));
	Rpt9.setLang(langID);

	//Reset the selected template
	Rpt9.setSelectedTemplate("");
}
else if (request.getParameter("selectTemplate") != null) {

	Rpt9.setSelectedTemplate(request.getParameter("selectTemplate"));
}

if(request.getParameter("surv") != null)
{ 
	int var1 = new Integer(request.getParameter("selSurvey")).intValue();
	Rpt9.setSurvey_ID(var1);
	
	String var3 = request.getParameter("chkType");
	if(var3 != null)
	{
		int Type = new Integer(var3).intValue();
		Rpt9.setType(Type);
	}
}

if(request.getParameter("surv1") != null)
{ 
	int var1 = new Integer(request.getParameter("selSurvey0")).intValue();
	CE_Survey.setSurvey_ID(var1);
	
	String var3 = request.getParameter("chkType");
	if(var3 != null)
	{
		int Type = new Integer(var3).intValue();
		Rpt9.setType(Type);
	}
	//Added by Ha 27/05/08 to change value to default when changing upper layer value
	CE_Survey.set_DivID(0);
	CE_Survey.set_DeptID(0);
	CE_Survey.set_GroupID(0);
}



if(request.getParameter("div") != null)
{
	int divID = new Integer(request.getParameter("div")).intValue();
	CE_Survey.set_DivID(divID);
	
	String var3 = request.getParameter("chkType");
	if(var3 != null)
	{
		int Type = new Integer(var3).intValue();
		Rpt9.setType(Type);
	}
	//Added by Ha 27/05/08 to change value to default when changing upper layer value
	CE_Survey.set_DeptID(0);
	CE_Survey.set_GroupID(0);
}

if(request.getParameter("dept") != null)
{
	int deptID = new Integer(request.getParameter("dept")).intValue();
	CE_Survey.set_DeptID(deptID);
	
	String var3 = request.getParameter("chkType");
	if(var3 != null)
	{
		int Type = new Integer(var3).intValue();
		Rpt9.setType(Type);
	}
	CE_Survey.set_GroupID(0);
}
if(request.getParameter("group") != null)
{ 
	int var1 = new Integer(request.getParameter("selGroup")).intValue();
	CE_Survey.set_GroupID(var1);
	
	String var3 = request.getParameter("chkType");
	if(var3 != null)
	{
		int Type = new Integer(var3).intValue();
		Rpt9.setType(Type);
	}
    
}

if(request.getParameter("preview") != null)
{
	if(langID != 0){
		if(request.getParameter("engResInclude") != null){
			Rpt9.setEngResInclude(true);
		}else{
			Rpt9.setEngResInclude(false);
		}
	}
	Date timeStamp = new java.util.Date();
	SimpleDateFormat dFormat = new SimpleDateFormat("ddMMyyHHmmss");
	String temp  =  dFormat.format(timeStamp);
	
	String Survey_Name = "";
	int Type = new Integer(request.getParameter("chkType")).intValue();
	Rpt9.setType(Type);
	
	//System.out.println("OrganisationID = " + logchk.getOrg());
	if(Type == 1)
	{
		String file_name = "Development Guide " + temp + ".xls";
	
		Rpt9.AllSurvey(Rpt9.getSurvey_ID(),logchk.getPKUser(), file_name);
		
		votblSurvey vo_SurveyDetail = CE_Survey.getSurveyDetail(Rpt9.getSurvey_ID());
		
		Survey_Name = vo_SurveyDetail.getSurveyName();
	
		
		//read the file name.
			
		String output = setting.getReport_Path() + "\\"+file_name;
		File f = new File (output);
	
		//set the content type(can be excel/word/powerpoint etc..)
		response.reset();
		response.setContentType ("application/xls");
		//set the header and also the Name by which user will be prompted to save
		response.addHeader ("Content-Disposition", "attachment;filename="+file_name+"");
			
		//get the file name
		String name = f.getName().substring(f.getName().lastIndexOf("/") + 1,f.getName().length());
		//OPen an input stream to the file and post the file contents thru the 
		//servlet output stream to the client m/c
		InputStream in = new FileInputStream(f);
		ServletOutputStream outs = response.getOutputStream();
				
		int bit = 256;
		int i = 0;

		try 
		{
			while ((bit) >= 0) {
				bit = in.read();
				outs.write(bit);
			}
    	}
    	catch (IOException ioe) 
    	{
    		ioe.printStackTrace(System.out);
    	}

		outs.flush();
		outs.close();
		in.close();	
     }
     else if(Type == 2)
     {
     	String [] TDetail = new String [14];
     	int TargetID = new Integer(request.getParameter("selTarget")).intValue();
     	String TargetName = "";
     	String SurvName = "";     
		boolean NotNull = false;
		int nameSequence = 0;
		     
     	Vector rs_Target = CE_Survey.getTargets(TargetID);		
		if(rs_Target.size()>0){
			votblOrganization vo=(votblOrganization)rs_Target.elementAt(0);
			nameSequence = vo.getNameSequence();
			TDetail = user.getUserDetail(TargetID,nameSequence);
			TargetName = TDetail[0]+", "+TDetail[1];
		}
     	String file_name = "Development Guide of "+TargetName+" as of "+ temp + ".xls";
		
		
     	NotNull = Rpt9.SelTarget(CE_Survey.getSurvey_ID(),TargetID,logchk.getPKUser(),file_name);
     	
     	if(NotNull)
     	{
			/***********************
			*Edit By James 15 - Nov 2007
			************************/
	     	votblSurvey voSD = CE_Survey.getSurveyDetail(CE_Survey.getSurvey_ID());
			
			SurvName = voSD.getSurveyName();
			
			String output = setting.getReport_Path() + "\\"+file_name;
			File f = new File (output);
				
			response.reset();
			//set the content type(can be excel/word/powerpoint etc..)
			response.setContentType ("application/xls");
			//set the header and also the Name by which user will be prompted to save
			response.addHeader ("Content-Disposition", "attachment;filename="+file_name+"");
				
			//get the file name
			String name = f.getName().substring(f.getName().lastIndexOf("/") + 1,f.getName().length());
			//OPen an input stream to the file and post the file contents thru the 
			//servlet output stream to the client m/c
					
			InputStream in = new FileInputStream(f);
			ServletOutputStream outs = response.getOutputStream();
			
			int bit = 256;
			int i = 0;
				
    		try {
    			while ((bit) >= 0) 
    			{
    				bit = in.read();
    				outs.write(bit);
    			}
            } 
            catch (IOException ioe) 
            {
            	ioe.printStackTrace(System.out);
            }
    		outs.flush();
    		outs.close();
    		in.close();	
				            		
			}
		else
		{	%>
			<script>
				alert("No Development Guide Report for this user");
			</script>
	<%	}
			
		}
	else if(Type == 3)
	{%>
	<script>	
		var x = parseInt(window.screen.width) / 2 - 500;  // the number 250 is the exact half of the width of the pop-up and so should be changed according to the size of the pop-up
		var y = parseInt(window.screen.height) / 2 - 300;  // the number 125 is the exact half of the height of the pop-up and so should be changed according to the size of the pop-up
	
			var myWindow=window.open('Report_DevelopmentGuide_CompetencyList.jsp','windowRef','scrollbars=no, width=950, height=600');
			myWindow.moveTo(x,y);
	    	myWindow.location.href = 'Report_DevelopmentGuide_CompetencyList.jsp';
	    	
	    	</script>
	<%	}
	}
%>
<form name="DevelopmentGuide" action="Report_DevelopmentGuide.jsp" method="post">
<table border="0" width="505" cellspacing="0" cellpadding="0" style="border-width:0px; " bordercolor="#3399FF">
	<tr>
		<td width="505" colspan="6"><b>
		<font face="Arial" size="2" color="#000080">
		<%=trans.tslt("Development Guide")%></font></b></td>
	</tr>
	<tr>
		<td >&nbsp;</td>
	</tr>
	<tr>
		<td width="430" colspan="5"> 
    	<b><font face="Arial" size="2"><%=trans.tslt("Organisation")%>:</font></b><font face="Arial" size="2">&nbsp;&nbsp;&nbsp;		
<%
// Added to check whether organisation is also a consulting company
// if yes, will display a dropdown list of organisation managed by this company
// else, it will display the current organisation only
// Mark Oei 09 Mar 2010
	String [] UserDetail = new String[14];
	UserDetail = CE_Survey.getUserDetail(logchk.getPKUser());
	boolean isConsulting = true;
	isConsulting = Org.isConsulting(UserDetail[10]); // check whether organisation is a consulting company 
	if (isConsulting){ %>
		<select size="1" name="selOrg" onchange="proceed(this.form,this.form.selOrg)">
		<option value="0" selected><%=trans.tslt("All")%></option>
	<%
		Vector vOrg = logchk.getOrgList(logchk.getCompany());

		for(int i=0; i<vOrg.size(); i++)
		{
			votblOrganization vo = (votblOrganization)vOrg.elementAt(i);
			int PKOrg = vo.getPKOrganization();
			String OrgName = vo.getOrganizationName();

			if(logchk.getOrg() == PKOrg)
			{ %>
				<option value=<%=PKOrg%> selected><%=OrgName%></option>
			<% } else { %>
				<option value=<%=PKOrg%>><%=OrgName%></option>
			<%	}	
		} 
	} else { %>
		<select size="1" name="selOrg" onchange="proceed(this.form,this.form.selOrg)">
		<option value=<%=logchk.getSelfOrg()%>><%=UserDetail[10]%></option>
	<% } // End of isConsulting %>
</select></font></td>
		<td width="1" > </td>
	</tr>
	<tr>
		<td width="27" align="center" height="25" bordercolor="#000080">&nbsp;</td>
		<td width="242" align="center" height="25" bordercolor="#000080">&nbsp;</td>
		<td width="207" height="25" colspan="3" bordercolor="#000080">&nbsp;</td>
		<td width="1" height="25"></td>
	</tr>
	<!--Gwen Oh - 27/09/2011: Add language filter-->
	<tr>
		<td> 
    	<b><font face="Arial" size="2"><%=trans.tslt("Language")%>:</font></b><font face="Arial" size="2">&nbsp;&nbsp;&nbsp;
		</font></td>
		<td>
		<select size="1" name="selLang" onchange="selectLang(this.form,this.form.selLang)">
		<%
		String[] languages = {"English", "Indonesian", "Thai", "Chinese(simplified)", "Chinese(tranditional)", "Korean"};
		String s = "";
		for(int i=0; i<languages.length; i++) {
			if(i==langID) { 
				s = "selected";
			}
		%>
			<option value = <%=i%> <%=s%>><%=languages[i]%></option>
		<%
			s = "";
		}
		%>
		</select>
		</td>
		</tr>
		<% 
		if(langID != 0){
		%>
		<tr>
		<td colspan="4"><b><font size="2" face="Arial"><%=trans.tslt("&nbsp;&nbsp;--Include English Resource") %>:</font></b>
		<input type="checkbox" name = "engResInclude" value ="T"></td>
		</tr>
		<%
		}
		%>
	<tr>
		<td width="27" align="center" height="25" bordercolor="#000080">&nbsp;</td>
		<td width="242" align="center" height="25" bordercolor="#000080">&nbsp;</td>
		<td width="207" height="25" colspan="3" bordercolor="#000080">&nbsp;</td>
		<td width="1" height="25"></td>
	</tr>
	<tr>
		<td width="430" colspan="5"> 
    	<b><font face="Arial" size="2"><%=trans.tslt("Template")%>:</font></b><font face="Arial" size="2">&nbsp;&nbsp;&nbsp;
		
		<select size="1" name="selTemplate" onchange="selectTemplate(this.form,this.form.selTemplate)">
		
		<%
			//Gwen Oh - 03/10/11: Add Template dropdown list
			
			File dir = new File(setting.getOOReportTemplatePath());
			String defTemplate = "DevelopmentGuide Template.xls";
			String[] templates = dir.list();
					
			final CharSequence char0 = "eng", char1 = "indo", char2 = "thai", char3 = "kor", char4 = "chinese", char5 = "simplified", char6 ="traditional";
			
			//English
			if(langID==0) {			
				templates = dir.list(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return (name.startsWith("DevelopmentGuide"));
				}});
			} 
			//Indonesian
			else if(langID==1) {
				templates = dir.list(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return ((name.startsWith("DevelopmentGuide") && name.toLowerCase().contains(char1))
							|| name.equals("DevelopmentGuide Template.xls"));
				}});		
				defTemplate = "DevelopmentGuide Template_Indo.xls";
			
			}
			//Thai
			else if(langID==2) {
				templates = dir.list(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return ((name.startsWith("DevelopmentGuide") && name.toLowerCase().contains(char2))
							|| name.equals("DevelopmentGuide Template.xls"));
				}});		
				//defTemplate = "DevelopmentGuide Template_Thai.xls";
			} 
			//Chinese tranditional
			else if(langID==4) {
				templates = dir.list(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return ((name.startsWith("DevelopmentGuide") && name.toLowerCase().contains(char4) && name.toLowerCase().contains(char6))
							|| name.equals("DevelopmentGuide Template.xls"));
				}});		
				defTemplate = "DevelopmentGuide Template_chinese(traditional).xls";
			}

			//TODO: added by Yiping. Chinese Simplified
			else if(langID==3) {
				templates = dir.list(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return ((name.startsWith("DevelopmentGuide") && name.toLowerCase().contains(char4) && name.toLowerCase().contains(char5))
							|| name.equals("DevelopmentGuide Template.xls"));
				}});		
				defTemplate = "DevelopmentGuide Template_chinese(simplified).xls";
			}
			//Korean
			else if(langID==5) {
				templates = dir.list(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.equals("DevelopmentGuide Template.xls");}});
			}
			
			String sel = "";
			
			String selectedTemplate = Rpt9.getSelectedTemplate();
			//If no template has been selected, set the selected template as the default template
			if (selectedTemplate.equals("")) {
				selectedTemplate = defTemplate;
				Rpt9.setSelectedTemplate(defTemplate);
			}
			
			for(int i=0; i<templates.length; i++) {
				if(templates[i].equalsIgnoreCase(selectedTemplate) ) { sel = "selected";}
				%><option value = "<%=templates[i]%>" <%=sel%>><%=templates[i]%></option><%
				sel = "";
			}
			
		%>
		
		</select>
		</font>
		</td>

		<td width="1"></td>
	</tr>
	<tr>
		<td width="27" align="center" height="25" bordercolor="#000080">&nbsp;</td>
		<td width="242" align="center" height="25" bordercolor="#000080">&nbsp;</td>
		<td width="207" height="25" colspan="3" bordercolor="#000080">&nbsp;</td>
		<td width="1" height="25"></td>
	</tr>
	<tr>
		<td width="26" align="center" height="25" style="border-left-style: solid; border-left-width: 1px; border-right-style:none; border-right-width:medium; border-top-style:solid; border-top-width:1px; border-bottom-style:none; border-bottom-width:medium" bordercolor="#3399FF" bgcolor="#FFFFCC" bordercolorlight="#3399FF" bordercolordark="#3399FF"> <font size="2">
   		<font face="Arial">
   		<%	if(Rpt9.getType() == 1)
   		{	%>
    	<input type="radio" value="1" checked name="chkType">
    	<%}	else{%>
    	<input type="radio" value="1" name="chkType">
    	<%	}	%>
    	
    	</font>
    	
    	</td>
		<td colspan='4' align="center" style="border-right:solid 1px #3399FF;border-top:solid 1px #3399FF;" bgcolor="#FFFFCC" height="49">
			<p align="left"><b><font face="Arial" size="2" color="#000080">
			<%=trans.tslt("Create From Existing Survey")%></font></b>
		</td>
	</tr>
	<tr>
		<td width="26" align="center" height="22" style="border-left-style: solid; border-left-width: 1px; border-bottom-style: none; border-bottom-width: medium; border-right-style:none; border-right-width:medium; border-top-style:none; border-top-width:medium" bordercolor="#3399FF" bgcolor="#FFFFCC" bordercolorlight="#3399FF" bordercolordark="#3399FF">&nbsp;
		</td>
		<td width="242" align="center" height="22" style="border-style:none; border-width:medium; " bordercolor="#3399FF" bgcolor="#FFFFCC" bordercolorlight="#3399FF" bordercolordark="#3399FF">
		<p align="left"><font face="Arial" size="2"><%=trans.tslt("Survey")%>:</font></td>
		<td width="206" height="22" colspan="3" style="border-right-style: solid; border-right-width: 1px; border-bottom-style: none; border-bottom-width: medium; border-left-style:none; border-left-width:medium; border-top-style:none; border-top-width:medium" bordercolor="#3399FF" bgcolor="#FFFFCC" bordercolorlight="#3399FF" bordercolordark="#3399FF"> <font size="2">
   
    	<font face="Arial">
   
    	<select size="1" name="selSurvey" onchange="surv(this.form,this.form.selSurvey)">
		<%//if(Rpt9.getSurvey_ID() == 0){	
		%>
    	<option value="" selected>&nbsp;</option>
		<%//}
	
		/********************************
		*Edit By James 15 - Nov  2007
		*********************************/
		
	Vector rs_SurveyDetail = CE_Survey.getSurveys(logchk.getCompany(),logchk.getOrg());		
	//while(rs_SurveyDetail.next())
	for(int i=0;i<rs_SurveyDetail.size();i++)
	{
		votblSurvey voSD=(votblSurvey)rs_SurveyDetail.elementAt(i);
		int Surv_ID = voSD.getSurveyID();
		String Surv_Name = voSD.getSurveyName();
		
	if(Rpt9.getSurvey_ID() == Surv_ID)
	{
%>
		<option value=<%=Surv_ID%> selected><%=Surv_Name%></option>
<%	}
	else	
	{%>
		<option value=<%=Surv_ID%> ><%=Surv_Name%></option>

<%	}	
}%>

</select></font></td>
	</tr>
	<tr>
		<td width="457" align="center" colspan="5" style="border-left-style: solid; border-left-width: 1px; border-right-style: solid; border-right-width: 1px; border-top-style: none; border-top-width: medium; border-bottom-style: solid; border-bottom-width: 1px" bordercolor="#3399FF" bgcolor="#FFFFCC" bordercolorlight="#3399FF" bordercolordark="#3399FF">&nbsp;
		</td>
	</tr>
	<tr>
		<td width="458" align="center" colspan="5">&nbsp;
		</td>
	</tr>
	<tr>
		<td width="26" align="center" style="border-left-style: solid; border-left-width: 1px; border-top-style: solid; border-top-width: 1px; border-right-style:none; border-right-width:medium; border-bottom-style:none; border-bottom-width:medium" bordercolor="#3399FF" bgcolor="#FFFFCC" height="49">
		<font size="2">
   
    	<font face="Arial">
   
    	<%	if(Rpt9.getType() == 2)	
    	{%>
    	<input type="radio" value="2" checked name="chkType">
    	<%}	else	{%>
    	<input type="radio" value="2" name="chkType">
    	<%	}	%>
</font>
</td>
		<td colspan='4' align="center" style="border-right:solid 1px #3399FF;border-top:solid 1px #3399FF;" bgcolor="#FFFFCC" height="49">
			<p align="left"><b><font face="Arial" size="2" color="#000080">
			<%=trans.tslt("Create from analyzed target")%></font></b>
		</td>
	</tr>
	<tr>
		<td width="26" align="center" style="border-left-style: solid; border-left-width: 1px; border-right-style:none; border-right-width:medium; border-top-style:none; border-top-width:medium; border-bottom-style:none; border-bottom-width:medium" bordercolor="#3399FF" bgcolor="#FFFFCC">&nbsp;
		</td>
		<td width="242" align="center" bordercolor="#3399FF" style="border-style: none; border-width: medium" bgcolor="#FFFFCC">
		<p align="left"><font face="Arial" size="2"><%=trans.tslt("Survey")%>:</font></td>
		<td width="206" align="center" colspan="3" style="border-right-style: solid; border-right-width: 1px; border-left-style:none; border-left-width:medium; border-top-style:none; border-top-width:medium; border-bottom-style:none; border-bottom-width:medium" bordercolor="#3399FF" bgcolor="#FFFFCC">
		<p align="left"> <font face="Arial" size="2">
   
    	<select size="1" name="selSurvey0" onchange="surv1(this.form,this.form.selSurvey0)">
		<%//if(CE_Survey.getSurvey_ID() == 0){	
		%>
    	<option value="" selected>&nbsp;</option>
		<%//}
	
		/********************************
		*Edit By James 15 - Nov  2007
		*********************************/
		
	Vector rs_SurveyDet = CE_Survey.getSurveys(logchk.getCompany(),logchk.getOrg());		
	//while(rs_SurveyDetail.next())
	for(int j=0;j<rs_SurveyDet.size();j++)
	{
		votblSurvey voSD=(votblSurvey)rs_SurveyDet.elementAt(j);
		int Surv_ID = voSD.getSurveyID();
		String Surv_Name = voSD.getSurveyName();
		
	if(CE_Survey.getSurvey_ID() == Surv_ID)
	{
%>
		<option value=<%=Surv_ID%> selected><%=Surv_Name%></option>
<%	}
	else	
	{%>
		<option value=<%=Surv_ID%> ><%=Surv_Name%></option>

<%	}	
}%>

</select></font></td>
	</tr>
	<tr>
		<td width="26" align="center" style="border-left-style: solid; border-left-width: 1px; border-right-style:none; border-right-width:medium; border-top-style:none; border-top-width:medium; border-bottom-style:none; border-bottom-width:medium" bordercolor="#3399FF" bgcolor="#FFFFCC">&nbsp;
		</td>
		<td width="242" align="center" bordercolor="#3399FF" style="border-style: none; border-width: medium" bgcolor="#FFFFCC">&nbsp;
		</td>
		<td width="201" align="center" colspan="3" bordercolor="#3399FF" style="border-left-style:none; border-left-width:medium; border-right-style:solid; border-right-width:1px; border-top-style:none; border-top-width:medium; border-bottom-style:none; border-bottom-width:medium" bgcolor="#FFFFCC">&nbsp;
		</td>
	</tr>
	<tr>
		<td width="26" align="center" style="border-left-style: solid; border-left-width: 1px; border-right-style:none; border-right-width:medium; border-top-style:none; border-top-width:medium; border-bottom-style:none; border-bottom-width:medium" bordercolor="#3399FF" bgcolor="#FFFFCC">&nbsp;
		</td>
		<td width="242" align="center" bordercolor="#3399FF" style="border-style: none; border-width: medium" bgcolor="#FFFFCC">
		<p align="left"><font face="Arial" size="2"><%=trans.tslt("Division")%>:</font></td>
		<td width="206" align="center" colspan="3" style="border-right-style: solid; border-right-width: 1px; border-left-style:none; border-left-width:medium; border-top-style:none; border-top-width:medium; border-bottom-style:none; border-bottom-width:medium" bordercolor="#3399FF" bgcolor="#FFFFCC">
		<p align="left"> <font face="Arial" size="2">
   
    	<select size="1" name="selDiv" onchange = "populateDept(this.form, this.form.selDiv)">

    	<option value="0" selected>&nbsp;</option>

<%		

	/*****************************************
	*  Edit By James 15 - Nov 2007
	******************************************/
	Vector rs_Div = CE_Survey.getAllDivisions(CE_Survey.getSurvey_ID());
	//ResultSet rs_Div = db.getRecord(command1);
	
	//while(rs_Div.next())
	for(int k=0;k<rs_Div.size();k++)
	{
	voDivision vo_Div = (voDivision)rs_Div.elementAt(k);
		int DivisionID = vo_Div.getPKDivision();
		String DivisionName = vo_Div.getDivisionName();
				
	if(CE_Survey.get_DivID() == DivisionID)
	{
%>		<option value=<%=DivisionID%> selected><%=DivisionName%></option>
<%	}
	else	
	{	
	%>
		<option value=<%=DivisionID%>> <%=DivisionName%></option>
<%
	}
}%>
</select></font></td>
	</tr>
	<tr>
		<td width="26" align="center" style="border-left-style: solid; border-left-width: 1px; border-right-style:none; border-right-width:medium; border-top-style:none; border-top-width:medium; border-bottom-style:none; border-bottom-width:medium" bordercolor="#3399FF" bgcolor="#FFFFCC">&nbsp;
		</td>
		<td width="242" align="center" bordercolor="#3399FF" style="border-style: none; border-width: medium" bgcolor="#FFFFCC">&nbsp;
		</td>
		<td width="201" align="center" colspan="3" bordercolor="#3399FF" style="border-left-style:none; border-left-width:medium; border-right-style:solid; border-right-width:1px; border-top-style:none; border-top-width:medium; border-bottom-style:none; border-bottom-width:medium" bgcolor="#FFFFCC">&nbsp;
		</td>
	</tr>
	<tr>
		<td width="26" align="center" style="border-left-style: solid; border-left-width: 1px; border-right-style:none; border-right-width:medium; border-top-style:none; border-top-width:medium; border-bottom-style:none; border-bottom-width:medium" bordercolor="#3399FF" bgcolor="#FFFFCC">&nbsp;
		</td>
		<td width="242" align="center" bordercolor="#3399FF" style="border-style: none; border-width: medium" bgcolor="#FFFFCC">
		<p align="left"><font face="Arial" size="2"><%=trans.tslt("Department")%>:</font></td>
		<td width="206" align="center" colspan="3" style="border-right-style: solid; border-right-width: 1px; border-left-style:none; border-left-width:medium; border-top-style:none; border-top-width:medium; border-bottom-style:none; border-bottom-width:medium" bordercolor="#3399FF" bgcolor="#FFFFCC">
		<p align="left"> <font face="Arial" size="2">
   
    	<select size="1" name="selDept" onchange = "populateGrp(this.form,this.form.selDiv,this.form.selDept)">

    	<option value="0" selected>&nbsp;</option>

<%		

		int iDivID = 0;
		if(request.getParameter("div") != null)
		{
			if (request.getParameter("div").equals("")){
				iDivID = 0;
				}
			else
				iDivID = Integer.parseInt(request.getParameter("div"));
				
			CE_Survey.set_DivID(iDivID);
			
			if(request.getParameter("dept") == null) {
				CE_Survey.set_DeptID(0);
				CE_Survey.set_GroupID(0);
			}
		}
	/********************************
	*Edit By James 14 - Nov 2007
	************************************/
	Vector rs_Dept = CE_Survey.getAllDepartments(CE_Survey.getSurvey_ID(),CE_Survey.get_DivID());
	//ResultSet rs_Dept = db.getRecord(command2);
	
	for(int l=0;l<rs_Dept.size();l++)
	//while(rs_Dept.next())
	{
	voDepartment vo_Dept = (voDepartment)rs_Dept.elementAt(l);
		int DeptID = vo_Dept.getPKDepartment();
		String DeptName = vo_Dept.getDepartmentName();
				
	if(CE_Survey.get_DeptID() == DeptID)
	{
%>
		<option value=<%=DeptID%> selected><%=DeptName%></option>
<%	}
	else	
	{%>

	<option value=<%=DeptID%> > <%=DeptName%></option>
<%
	}
}%>
</select></font></td>
	</tr>
	<tr>
		<td width="26" align="center" style="border-left-style: solid; border-left-width: 1px; border-right-style:none; border-right-width:medium; border-top-style:none; border-top-width:medium; border-bottom-style:none; border-bottom-width:medium" bordercolor="#3399FF" bgcolor="#FFFFCC">&nbsp;
		</td>
		<td width="242" align="center" bordercolor="#3399FF" style="border-style: none; border-width: medium" bgcolor="#FFFFCC">&nbsp;
		</td>
		<td width="201" align="center" colspan="3" bordercolor="#3399FF" style="border-left-style:none; border-left-width:medium; border-right-style:solid; border-right-width:1px; border-top-style:none; border-top-width:medium; border-bottom-style:none; border-bottom-width:medium" bgcolor="#FFFFCC">&nbsp;
		</td>
	</tr>
	<tr>
		<td width="26" align="center" style="border-left-style: solid; border-left-width: 1px; border-right-style:none; border-right-width:medium; border-top-style:none; border-top-width:medium; border-bottom-style:none; border-bottom-width:medium" bordercolor="#3399FF" bgcolor="#FFFFCC">&nbsp;
		</td>
		<td width="242" align="center" bordercolor="#3399FF" style="border-style: none; border-width: medium" bgcolor="#FFFFCC">
		<p align="left"><font face="Arial" size="2"><%=trans.tslt("Group")%>:</font></td>
		<td width="206" align="center" colspan="3" style="border-right-style: solid; border-right-width: 1px; border-left-style:none; border-left-width:medium; border-top-style:none; border-top-width:medium; border-bottom-style:none; border-bottom-width:medium" bordercolor="#3399FF" bgcolor="#FFFFCC">
		<p align="left"> <font face="Arial" size="2">
   
    	<select size="1" name="selGroup" onchange = "group(this.form,this.form.selGroup, this.form.selDiv, this.form.selDept)">

    	<option value="0" selected>&nbsp;</option>
<%		

		int iDeptID = 0;
		if(request.getParameter("dept") != null)
		{
			if (request.getParameter("dept").equals("")){
				iDeptID = 0;
				}
			else
				iDeptID = Integer.parseInt(request.getParameter("dept"));
				
			CE_Survey.set_DeptID(iDeptID);
		} 
		
	/***********************************
	*Edit By James 15- Nov 2007
	************************************/
	//ResultSet rs_Group = db.getRecord(query2);
	//while(rs_Group.next())
	Vector rs_Group = CE_Survey.getAllGroups(CE_Survey.getSurvey_ID(),CE_Survey.get_DeptID());
	for(int m=0;m<rs_Group.size();m++)
	{
		voGroup vo_Group = (voGroup)rs_Group.elementAt(m);
		int GroupID = vo_Group.getPKGroup();
		String GroupName = vo_Group.getGroupName();
				
	if(CE_Survey.get_GroupID() == GroupID)
	{
%>
		<option value=<%=GroupID%> selected><%=GroupName%></option>
<%	}
	else	
	{%>

	<option value=<%=GroupID%> > <%=GroupName%></option>
<%
	}
}%>
</select></font></td>
	</tr>
	<tr>
		<td width="26" align="center" style="border-left-style: solid; border-left-width: 1px; border-right-style:none; border-right-width:medium; border-top-style:none; border-top-width:medium; border-bottom-style:none; border-bottom-width:medium" bordercolor="#3399FF" height="20" bgcolor="#FFFFCC">&nbsp;
		</td>
		<td width="242" align="center" bordercolor="#3399FF" style="border-style: none; border-width: medium" height="20" bgcolor="#FFFFCC">&nbsp;
		</td>
		<td width="201" align="center" colspan="3" bordercolor="#3399FF" style="border-left-style:none; border-left-width:medium; border-right-style:solid; border-right-width:1px; border-top-style:none; border-top-width:medium; border-bottom-style:none; border-bottom-width:medium" height="20" bgcolor="#FFFFCC">&nbsp;
		</td>
	</tr>
	<tr>
		<td width="26" align="center" style="border-left-style: solid; border-left-width: 1px; border-bottom-style: none; border-bottom-width: medium; border-right-style:none; border-right-width:medium; border-top-style:none; border-top-width:medium" bordercolor="#3399FF" bgcolor="#FFFFCC">&nbsp;
		</td>
		<td width="242" align="center" style="border-style:none; border-width:medium; " bordercolor="#3399FF" bgcolor="#FFFFCC">
		<p align="left"><font face="Arial" size="2"><%=trans.tslt("Target")%>:</font></td>
		<td width="206" align="center" colspan="3" style="border-right-style: solid; border-right-width: 1px; border-bottom-style: none; border-bottom-width: medium; border-left-style:none; border-left-width:medium; border-top-style:none; border-top-width:medium" bordercolor="#3399FF" bgcolor="#FFFFCC">
		<p align="left"> <font face="Arial" size="2">
   
    	<select size="1" name="selTarget">
<%
	String [] TDetail = new String [14];
	
	int seq = userJ.NameSequence(logchk.getOrg());
	/***********************************
	*Edit By James 15- Nov 2007
	************************************/
	Vector rs_Target = CE_Survey.getAllUsers(seq,CE_Survey.getSurvey_ID(),CE_Survey.get_GroupID());
	for(int n=0;n<rs_Target.size();n++)
	{
		voUser vo_Target =(voUser)rs_Target.elementAt(n);
		int TargetID = vo_Target.getPKUser();
		//int nameSequence = rs_Target.getInt("NameSequence");
		
		//TDetail = user.getUserDetail(TargetID,nameSequence);
%>
	<option value=<%=TargetID%> selected> <%=vo_Target.getFullName()%></option>
<%
	}%>
</select></font></td>
	</tr>
	<tr>
		<td width="457" align="center" colspan="5" style="border-left-style: solid; border-left-width: 1px; border-right-style:solid; border-right-width:1px; border-top-style:none; border-top-width:medium; border-bottom-style:solid; border-bottom-width:1px" bordercolor="#3399FF" bgcolor="#FFFFCC">&nbsp;
		</td>
	</tr>
	<tr>
		<td  align="center"  style="border-top-style: none; border-top-width: medium">&nbsp;
		</td>
	</tr>
	<tr>
		<td width="27" align="center"> <font size="2">
   
    	<font face="Arial">
   
    	<%	if(Rpt9.getType() == 3)
    	{	%>
    	<input type="radio" value="3" checked name="chkType">
    	<%	}else	{%>
    	<input type="radio" value="3" name="chkType">
    	<%}	%></font></td>
		<td width="398" align="center" colspan="3"> 
		<p align="left"><font face="Arial" size="2">&nbsp;<b><font color="#000080">
		<%=trans.tslt("Create from scratch")%></font></b></font></td>
	</tr>
	<tr>
		<td width="27" align="center">&nbsp; </td>
		<td width="242" align="center">&nbsp; </td>
		<td width="146" align="center" rowspan="2" colspan="2"> <font size="2">
   
	
   
		<font face="Arial">
   <% if(!logchk.getCompanyName().equalsIgnoreCase("demo") || logchk.getUserType() == 1) {
%>
<input type="button" value="<%=trans.tslt("Preview")%>" name="btnPreview" style="float: right"  onclick="chkSelect(this.form,this.form.chkType)">
<%   
   } else { 
   %>
<input type="button" value="<%=trans.tslt("Preview")%>" name="btnPreview" style="float: right"  onclick="chkSelect(this.form,this.form.chkType)" disabled>   
   <%
   } %>
		
		
		</font></td>
	</tr>
	<tr>
		<td width="27" align="center">&nbsp; </td>
		<td width="242" align="center"> <font size="2">
   
		<p align="right">
		</td>
	</tr>
</table>
</form>
<%	}	%>

<p></p>
<%@ include file="Footer.jsp"%>
</body>
</html>
