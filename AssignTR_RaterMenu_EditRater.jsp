<%@ page import="java.sql.*,
                 java.io.*,
                 java.util.*,
                 java.text.*,
                 CP_Classes.vo.votblSurveyRelationSpecific,
                 CP_Classes.vo.votblAssignment,
                 java.lang.String,CP_Classes.SurveyResult"%>  
                 
<jsp:useBean id="assignTR" class="CP_Classes.AssignTarget_Rater" scope="session"/>
<jsp:useBean id="CE_Survey" class="CP_Classes.Create_Edit_Survey" scope="session"/>
<jsp:useBean id="user" class="CP_Classes.User" scope="session"/>
<jsp:useBean id="logchk" class="CP_Classes.Login" scope="session"/>   
<jsp:useBean id="trans" class="CP_Classes.Translate" scope="session"/>
<jsp:useBean id="Department" class="CP_Classes.Department" scope="session"/>
<jsp:useBean id="Division" class="CP_Classes.Division" scope="session"/>                  
<jsp:useBean id="Group" class="CP_Classes.Group" scope="session"/>
<jsp:useBean id="Orgs" class="CP_Classes.Organization" scope="session"/>
<jsp:useBean id="RR" class="CP_Classes.RaterRelation" scope="session"/>
<jsp:useBean id="SurveyRelationSpecific" class ="CP_Classes.SurveyRelationSpecific" scope="session"/>
<%@ page import="CP_Classes.vo.voGroup"%>
<%@ page import="CP_Classes.vo.voDepartment"%>
<%@ page import="CP_Classes.vo.voDivision"%>
<%@ page import="CP_Classes.vo.votblRelationHigh"%>
<%@ page import="CP_Classes.vo.votblRelationSpecific"%>
<html>
<head>
<%@ page pageEncoding="UTF-8"%>
<meta http-equiv="Content-Type" content="text/html">
<%// by lydia Date 05/09/2008 Fix jsp file to support Thai language %>
</head>
<SCRIPT LANGUAGE=JAVASCRIPT>

function check(field)
{
	var isValid = 0;
	var clickedValue = 0;
	//check whether any checkbox selected
	if( field == null ) {
		isValid = 2;
	
	} else {

		if(isNaN(field.length) == false) {
			for (i = 0; i < field.length; i++)
				if(field[i].checked) {
					clickedValue = field[i].value;
					isValid = 1;
				}
		}else {		
			if(field.checked) {
				clickedValue = field.value;
				isValid = 1;
			}
				
		}
	}
	
	if(isValid == 1)
		return clickedValue;
	else if(isValid == 0)
		alert("No record selected");
	else if(isValid == 2)
		alert("No record available");
	
	isValid = 0;
		
}

function closeME(form)
{ 
	form.action = "AssignTR_RaterMenu_EditRater.jsp?close=1";
	form.method="post";
	form.submit();
}
function refresh(form, field)
{
	form.action = "AssignTR_RaterMenu_EditRater.jsp?refresh="+field.value;
	form.method="post";
	form.submit();	
}

function edit(form, field)
{
		alert(field.value);
	  if (confirm("Edit Rater?"))
	    {
			form.action="AssignTR_RaterMenu_EditRater.jsp?edit=" +field.value;
			form.method="post";
			form.submit();		
		}
	}



function raterChange(form,field)
{
	
	form.action="AssignTR_RaterMenu_EditRater.jsp?change="+field.value;
	form.method="post";
	form.submit();	
}

function checkedAll(form, field, checkAll)
{	
	if(checkAll.checked == true) 
		for(var i=0; i<field.length; i++)
			field[i].checked = true;
	else 
		for(var i=0; i<field.length; i++)
			field[i].checked = false;	
}



</SCRIPT>
<body>
<%

String username=(String)session.getAttribute("username");
if (!logchk.isUsable(username)) 
  {%> <font size="2">
   
    <script>
	parent.location.href = "index.jsp";
	</script>
<%  } 

assignTR.setGroupID(0);


if(request.getParameter("change") != null)
{
	int var2 = new Integer(request.getParameter("change")).intValue();
	assignTR.set_selectedRaterID(var2);
}

if(request.getParameter("edit") != null)
{
	
	boolean isEdited;
	int count = 0;
	
    String code = request.getParameter("edit");
    System.out.println("java" +  code);
    isEdited = assignTR.editAssignment(assignTR.get_selectedAssID(), code, logchk.getPKUser());
    
    if(isEdited){
    	
    
			
	assignTR.set_selectedTargetID(0);
	assignTR.set_selectedAssID(0);
%>
	<script>
	    alert("Rater Relation successfully edited.")
		location.href = "AssignTarget_Rater.jsp";
	</script>
<%

    }
    else{
    	assignTR.set_selectedTargetID(0);
    	assignTR.set_selectedAssID(0);
    %>
    	<script>
    	    alert("Rater Relation not edited.")
    		location.href = "AssignTarget_Rater.jsp";
    	</script>
    <%
    }
}

if(request.getParameter("assID") != null)
{	 
	int assignmentid = Integer.parseInt(request.getParameter("assID"));
	assignTR.set_selectedAssID(assignmentid);								
} 

if(request.getParameter("targetID") != null)
{	 
	int targetID = Integer.parseInt(request.getParameter("targetID"));
	assignTR.set_selectedTargetID(targetID);							
} 

if(request.getParameter("close") != null)
{	
	assignTR.set_selectedTargetID(0);
	assignTR.set_selectedAssID(0);
	%>
	<script>
		location.href ='AssignTarget_Rater.jsp';
	</script>
<%
}

/************************************************** ADDING TOGGLE FOR SORTING PURPOSE *************************************************/

	int toggle = assignTR.getToggle();	//0=asc, 1=desc
	int type = 1; //1=name, 2=origin		
			
	if(request.getParameter("name") != null)
	{	 
		if(toggle == 0)
			toggle = 1;
		else
			toggle = 0;
		
		assignTR.setToggle(toggle);
		
		type = Integer.parseInt(request.getParameter("name"));			 
		assignTR.setSortType(type);									
	} 

/*********************************************************END ADDING TOGGLE FOR SORTING PURPOSE *************************************/

%>
<form name="AssignTR_RaterMenu_EditRater" action="AssignTR_RaterMenu_EditRater.jsp" method="post">
<table border="0" width="610">
	<tr>
		<td><b><font face="Arial" size="2"><font color="#000080"><%= trans.tslt("Edit Rater") %></font>
		</font></b></td>
	</tr>
	<tr>
		<td>&nbsp;</td>
	</tr>
</table>
<table border="1" width="610" bgcolor="#FFFFFF" bordercolor="#FFFFFF" style="border-left-width: 0px; border-right-width: 0px" cellspacing="1">
	<tr>
		<td width="174" style="border-right-style:solid; border-right-width:1px; border-bottom-style:solid; border-bottom-width:1px; border-top-style:solid; border-top-width:1px" height="22" align="left" bgcolor="#FFFFCC" bordercolor="#3399FF" colspan="2">
		<font size="2">
   
    	<font face="Arial" style="font-weight:700" size="2">&nbsp;
    	<%= trans.tslt("Selected Survey") %>:</font></td>
		<td style="border-left-style:solid; border-left-width:1px; border-bottom-style:solid; border-bottom-width:1px; border-top-style:solid; border-top-width:1px" height="22" bgcolor="#FFFFCC" bordercolor="#3399FF" colspan="2" width="448">
<font face="Arial" size="2">
<%	
	
	String SurveyName= CE_Survey.getSurveyName(CE_Survey.getSurvey_ID());
	
%>
	&nbsp<%=SurveyName%>



</font>

</td>
	</tr>
	<tr>
<%
	String [] TargetDetail = new String[13];
    int assignmentID = assignTR.get_selectedAssID();
    votblAssignment assignment = assignTR.getAssignment(assignmentID);
	TargetDetail = user.getUserDetail(assignment.getTargetLoginID(),assignTR.get_NameSequence());
	
	
	
%>	
		<td width="174" height="25" style="border-right-style:solid; border-right-width:1px; border-top-style:solid; border-top-width:1px; border-bottom-style:solid; border-bottom-width:1px" align="left" bgcolor="#FFFFCC" bordercolor="#3399FF" colspan="2">
		<font size="2">
   
    	<font face="Arial" style="font-weight:700" size="2">
		&nbsp;<%= trans.tslt("Target Family Name") %>:</font></td>
		<td height="25" style="border-left-style:solid; border-left-width:1px; border-top-style:solid; border-top-width:1px; border-bottom-style:solid; border-bottom-width:1px" bgcolor="#FFFFCC" bordercolor="#3399FF" colspan="2" width="448">
		<font face="Arial" size="2">&nbsp;<%=TargetDetail[0]%></font></td>
	</tr>
	<tr>
		<td width="174" style="border-right-style:solid; border-right-width:1px; border-top-style:solid; border-top-width:1px; border-bottom-style:solid; border-bottom-width:1px" align="left" bgcolor="#FFFFCC" bordercolor="#3399FF" colspan="2">
		&nbsp;<font face="Arial" style="font-weight:700" size="2">
		<%= trans.tslt("Target Other Name") %>:</font></td>
		<td style="border-left-style:solid; border-left-width:1px; border-top-style:solid; border-top-width:1px; border-bottom-style:solid; border-bottom-width:1px" bgcolor="#FFFFCC" bordercolor="#3399FF" colspan="2" width="448">
		<font face="Arial" size="2">&nbsp;<%=TargetDetail[1]%></font></td>
	</tr>
</table>
<p></p>

<table border="1" width="610" bgcolor="#FFFFCC" bordercolor="#3399FF">
	<tr>
		<td colspan="5" bgcolor="#000080">
		<p align="center">
		<b><font face="Arial" size="2" color="#FFFFFF"><%= trans.tslt("Rater") %></font></b></td>
	</tr>
	<tr>
		<td width="28" align="center" bgcolor="#000080">
          &nbsp		
		</td>
		<td width="146" align="center" bgcolor="#000080">
		<b>
		<a href="AssignTR_TargetMenu_AddRater.jsp?name=1">
		<font style='font-family:Arial;color:white' size="2">
		<u><%= trans.tslt("Family Name") %></u></font></a></b></td>
		<td width="171" align="center" bgcolor="#000080">
		<b>
		<a href="AssignTR_TargetMenu_AddRater.jsp?name=2"><font style='font-family:Arial;color:white' size="2">
		<u><%= trans.tslt("Other Name") %></u></font></a></b></td>
		<td width="165" align="center" bgcolor="#000080">
		<b>
		<a href="AssignTR_TargetMenu_AddRater.jsp?name=3"><font style='font-family:Arial;color:white' size="2">
		<u><%= trans.tslt("Login Name") %></u></font></a></b></td>
		<td width="165" align="center" bgcolor="#000080">
		<b><font face="Arial" size="2" color="#FFFFFF"><%= trans.tslt("Relation") %></font></b></td>
	</tr>
<%
	

	String RaterDetail[] = new String[13];
	
    RaterDetail = user.getUserDetail(assignment.getRaterLoginID(), assignTR.get_NameSequence());
    
    int relationSpec = assignment.getRTSpecific();
    int relationHigh = assignment.getRTRelation();
	%>	
	<tr onMouseOver = "this.bgColor = '#99ccff'"
    	onMouseOut = "this.bgColor = '#FFFFcc'">
		<td width="28" align="center">
		&nbsp
		</td>
		<td width="146" align="center">
		<font face="Arial" size="2"><%=RaterDetail[0]%></font></td>
		<td width="171" align="center">
		<font face="Arial" size="2"><%=RaterDetail[1]%></font></td>
		<td width="165" align="center">
		<font face="Arial" size="2"><%=RaterDetail[2]%></font></td>
		<td width="165" align="center">
		<font face="Arial" size="2">

		<select size="1" name=selRelation>
<%
		Vector vRelHigh = RR.getAllRelationHigh(assignment.getRaterLoginID(), assignTR.get_selectedTargetID());
	
		for(int k=0; k<vRelHigh.size();k++)
		{
			votblRelationHigh vo = (votblRelationHigh)vRelHigh.elementAt(k);
			int RelID = vo.getRelationID();
			
			
				String RelHigh = vo.getRelationHigh(); 
				
				/*
				*Change(s): Set the current relation as selected
				*Reason(s): To show the current rater relation
				*Updated by: Liu Taichen
				*Updated on: 6 June 2012
				*/
				if((relationSpec ==0) && (relationHigh == RelID)){
				%>		
						<option value=<%="High"+RelID%> selected><%=RelHigh%></option>
				<%	
				}
				else
				{
					%>		
					<option value=<%="High"+RelID%>><%=RelHigh%></option>
			<%	
					
				}
				/*
				*Change(s): Use class SurveyRelationSpecific to manage relation specifics
				*Reason(s): To associate relation specific to survey instead of organization
				*Updated by: Liu Taichen
				*Updated on: 5 June 2012
				*/
				//Vector vRelSpecific = RR.getAllRelationSpecific(logchk.getOrg(),PKUser, assignTR.get_selectedTargetID());
				Vector vRelSpecific = SurveyRelationSpecific.getRelationSpecific(RelID, CE_Survey.getSurvey_ID());
				for(int i=0; i<vRelSpecific.size();i++)
				{
					votblSurveyRelationSpecific so = (votblSurveyRelationSpecific)vRelSpecific.elementAt(i);
					int SpecID = so.getSpecificID();
					String RelSpec = so.getRelationSpecific();
					
					if(relationSpec == SpecID){
			%>
							
					<option value=<%="Spec"+SpecID%> selected><%=RelSpec%></option>
			<%		
					}
					else
					{
						%>
						
						<option value=<%="Spec"+SpecID%>><%=RelSpec%></option>
				<%		
						
					}
		//System.out.println("RIANTONEW 'High" + RelID + ", " + RelHigh + "'");
		
		}
		
	

		}
	%>
		
		</select>
		</font>
	</td>
</tr>
<%	
//}
%>
</table>
<p></p>
	</tr>
	<tr>
		<td width="538">
		<input type="button" value="<%= trans.tslt("Edit") %>" name="btnEdit" style="float: right" onclick="edit(this.form, this.form.selRelation )"></td>
		<td>
		<input type="button" value="<%= trans.tslt("Cancel") %>" name="btnCancel" style="float: right" onclick="closeME(this.form)"></td>
	</tr>
	
</table>
</form>

</body>
</html>