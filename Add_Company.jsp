<%// Author: Dai Yong in June 2013%>
<%@ page import="java.sql.*,
                 java.io.*,
                 java.lang.String"%>  
<jsp:useBean id="logchk" class="CP_Classes.Login" scope="session"/>  
<jsp:useBean id="CE_Survey" class="CP_Classes.Create_Edit_Survey" scope="session"/>
<jsp:useBean id="db" class="CP_Classes.Database" scope="session"/>
<jsp:useBean id="CC" class="CP_Classes.ConsultingCompany" scope="session"/>
<jsp:useBean id="trans" class="CP_Classes.Translate" scope="session"/>
<html>
<head>
<%@ page pageEncoding="UTF-8"%>
<meta http-equiv="Content-Type" content="text/html">
<%// by lydia Date 05/09/2008 Fix jsp file to support Thai language %>
<title>Add Consulting Company</title>
</head>
<SCRIPT LANGUAGE="JavaScript">
function validate()
{
    x = document.Add_Company
    if (x.txtCCName.value == "")
    {
	alert("<%=trans.tslt("Enter Company Name")%>");
	return false 
	}
	//\\Added by Ha 06/06/08
	if (x.txtCCDesc.value == "")
    {
	alert("<%=trans.tslt("Enter Consulting Company Description")%>");
	return false 
	}
	
	//\\Added by Ha 02/06/08
	if (confirm("Add Company?"))
	return true;
	else return false;
	return true;
	
}
</SCRIPT>
<body bgcolor="#FFFFCC">
<%
//response.setHeader("Pragma", "no-cache");
//response.setHeader("Cache-Control", "no-cache");
//response.setDateHeader("expires", 0);

String username=(String)session.getAttribute("username");

  if (!logchk.isUsable(username)) 
  {%> <font size="2">
   
    	    	<script>
	parent.location.href = "index.jsp";
</script>
<%  } 
  else 
  { 	
  
if(request.getParameter("btnAdd") != null)
{
	String CCName = request.getParameter("txtCCName");
	String CCDesc = request.getParameter("txtCCDesc");
	String CCCode = request.getParameter("txtCCCode"); // added organisation code
	boolean isExist = false;
	boolean canAdd = false;
	try
	{
	//Added by Ha 02/06/08, re-edit by Ha 09/06/08
	//Before adding, check whether that company exists in database
		int action  = 1;//action = 1 means add
		isExist = CC.checkRecordExist(CCName, CCDesc, logchk.getPKUser(), logchk.getCompany(),action);
    	if (isExist == false)
    		// added organisation code to cater for auto-generation of organisation
    		// when Super Administrator creates a consulting company
    		// Mark Oei 09 Mar 2010
    		canAdd = CC.addRecord(CCName, CCDesc, CCCode, logchk.getPKUser());
		if (canAdd)
		{%>
			<script>
			alert("<%=trans.tslt("Added successfully")%>");
			</script>
		<%}
		else
		//Added by Ha 06/06/08
		{%>
			<script>
			alert("<%=trans.tslt("Record exists")%>");
			</script>
		<%}		
		%>
		<script>
			window.close();
			//opener.location.href('OrganizationList.jsp');
			opener.location.href='OrganizationList.jsp';
		</script>
		<%
	}
	catch(SQLException SQLE)
	{
		%>
		<script>
		alert("<%=trans.tslt("Existing Relation")%>");
		</script>
<%	}

}

%>
  <form name="Add_Company" action="Add_Company.jsp" method="post" onsubmit="return validate()">
<table border="0" width="100%" cellspacing="0" cellpadding="0" bgcolor="#FFFFCC">
	<tr>
		<td><b><font face="Arial" color="#000080" size="2">
		<%= trans.tslt("Add New Consulting Company") %>
		</font></b></td>
	</tr>
	<tr>
		<td>&nbsp;</td>
	</tr>
	<tr>
		<td>&nbsp;</td>
	</tr>
</table>
<table border="0" width="100%" cellspacing="0" cellpadding="0">
	<tr>
		<td width="181"><font face="Arial" size="2">
		<%= trans.tslt("Company Name") %>:
		</font></td>
		<td><input type="text" name="txtCCName" size="50"></td>
	</tr>
	<tr>
		<td width="181">&nbsp;</td>
		<td>&nbsp;</td>
	</tr>
	<tr>
		<td width="181"><font face="Arial" size="2">
		<%= trans.tslt("Company Description") %>:</font></td>
		<td><input type="text" name="txtCCDesc" size="50"></td>
	</tr>
	<tr>
		<td width="181">&nbsp;</td>
		<td>&nbsp;</td>
		</tr>
		<!-- Add Organisation Code -->
		<tr>
			<td width="181"><font face="Arial" size="2">
			Organisation Code:</font></td>
			<td><input type="text" name="txtCCCode" size="50"></td>
		</tr>
	<tr>
		<td width="181">&nbsp;</td>
		<td>&nbsp;</td>
	</tr>
	</table>
<table border="0" width="100%" cellspacing="0" cellpadding="0">
	<tr>
		<td width="50%" align="right">&nbsp;</td>
		<td align="right" colspan="2">&nbsp;</td>
	</tr>
	<tr>
		<td width="50%" align="right">
		<p align="center">
		<font size="2">
   		
   		<input type="submit" value="<%= trans.tslt("Register New Company") %>" name="btnAdd" style="float: right"></td>
		<td align="right" width="23%">
		
   
		</td>
		<td align="right" width="27%">
		<font size="2">
   
		<input type="button" value="<%= trans.tslt("Close") %>" name="btnClose" style="float: right" onclick="window.close()"></td>
	</tr>
</table>
</form>
<%	}	%>
</body>
</html>