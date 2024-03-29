
<%
	// Author: Dai Yong in June 2013
%>
<%@ page import="java.sql.*"%>
<%@ page import="java.io.*"%>
<%@ page import="java.util.*"%>
<%@ page import="java.util.Date"%>
<%@ page import="java.text.SimpleDateFormat"%>
<%@ page import="CP_Classes.vo.*"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<!-- CSS -->

<link type="text/css" rel="stylesheet" href="../lib/css/bootstrap.css">
<link type="text/css" rel="stylesheet"
	href="../lib/css/bootstrap-responsive.css">
<link type="text/css" rel="stylesheet"
	href="../lib/css/bootstrap.min.css">
<link type="text/css" rel="stylesheet"
	href="../lib/css/bootstrap-responsive.min.css">


<!-- jQuery -->
<script type="text/javascript" src="../lib/js/bootstrap.min.js"></script>
<script type="text/javascript" src="../lib/js/bootstrap.js"></script>
<script type="text/javascript" src="../lib/js/jquery-1.9.1.js"></script>
<script type="text/javascript" src="../lib/js/bootstrap.min.js"></script>
<script type="text/javascript" src="../lib/js/bootstrap-dropdown.js"></script>


<title>User Assignment</title>
<%@ page pageEncoding="UTF-8"%>
<meta http-equiv="Content-Type" content="text/html">
<style type="text/css">
<!--
body {
	
}
-->
</style>
<jsp:useBean id="logchk" class="CP_Classes.Login" scope="session" />
<jsp:useBean id="Database" class="CP_Classes.Database" scope="session" />
<jsp:useBean id="User" class="CP_Classes.User" scope="session" />
<jsp:useBean id="CoachOrganization" class="Coach.CoachOrganization"
	scope="session" />
<jsp:useBean id="SessionSetup" class="Coach.SessionSetup"
	scope="session" />
<jsp:useBean id="CoachDateGroup" class="Coach.CoachDateGroup"
	scope="session" />
<jsp:useBean id="CoachSlotGroup" class="Coach.CoachSlotGroup"
	scope="session" />
<jsp:useBean id="Venue" class="Coach.CoachVenue" scope="session" />
<jsp:useBean id="Export" class="CP_Classes.Export" scope="session" />
<jsp:useBean id="setting" class="CP_Classes.Setting" scope="session" />
<script type="text/javascript">
	var x = parseInt(window.screen.width) / 2 - 240; // the number 250 is the exact half of the width of the pop-up and so should be changed according to the size of the pop-up
	var y = parseInt(window.screen.height) / 2 - 115; // the number 125 is the exact half of the height of the pop-up and so should be changed according to the size of the pop-up

	function proceed(form) {

	}
	function check(field) {
		var isValid = 0;
		var clickedValue = 0;
		//check whether any checkbox selected
		if (field == null) {
			isValid = 2;

		} else {

			if (isNaN(field.length) == false) {
				for (i = 0; i < field.length; i++)
					if (field[i].checked) {
						clickedValue = field[i].value;
						isValid = 1;
					}
			} else {
				if (field.checked) {
					clickedValue = field.value;
					isValid = 1;
				}

			}
		}

		if (isValid == 1)
			return clickedValue;
		else if (isValid == 0)
			alert("No record selected");
		else if (isValid == 2)
			alert("No record available");

		isValid = 0;

	}
	function closeSlotFunction(form, field) {
		var value = check(field);
		if (value) {
			form.action = "UserAssignment.jsp?close=" + value;
			form.method = "post";
			form.submit();
		}

	}
	function unSignUp(form, field) {
		var value = check(field);
		if (value) {
			form.action = "UserAssignment.jsp?unSignUp=" + value;
			form.method = "post";
			form.submit();
		}

	}

	function openSlotFunction(form, field) {
		var value = check(field);
		if (value) {
			form.action = "UserAssignment.jsp?open=" + value;
			form.method = "post";
			form.submit();
		}

	}

	function addSlotFunction(form, field) {
		if (form.selSession.value == "0") {
			alert("Please select Coaching Session");
		} else {
			var myWindow=window.open('AddSlotToUserAssignment.jsp','windowRef','scrollbars=no, width=480, height=500');
			myWindow.moveTo(x,y);
		    myWindow.location.href = "AddSlotToUserAssignment.jsp?SessionID=" + field.value;
		}

	}
	function deleteSlotFunction(form, field) {
		var value = check(field);
		if (value) {
			if (confirm("Are you sure to delete the user assignment? Deleting is irreversable.")) {
				form.action = "UserAssignment.jsp?deleteSlot=" + value;
				form.method = "post";
				form.submit();
			}
		}
	}
	function assignUser(form, field) {

		var value = check(field);
		if (value) {
			var myWindow = window.open('AddUser.jsp?UserAssignment=' + value,
					'windowRef', 'scrollbars=no, width=480, height=250');
			var query = "AddUser.jsp?UserAssignment=" + value;
			myWindow.moveTo(x, y);
			myWindow.location.href = query;

		}
	}
	function changeVenue(form, field) {

		var value = check(field);
		if (value) {
			var myWindow = window.open(
					'EditUserAssignmentVenue.jsp?UserAssignment=' + value,
					'windowRef', 'scrollbars=no, width=480, height=250');
			var query = "EditUserAssignmentVenue.jsp?UserAssignment=" + value;
			myWindow.moveTo(x, y);
			myWindow.location.href = query;
		}
	}
	function changeCoach(form, field) {

		var value = check(field);
		if (value) {
			var myWindow = window.open(
					'EditUserAssignmentCoach.jsp?UserAssignment=' + value,
					'windowRef', 'scrollbars=no, width=480, height=250');
			var query = "EditUserAssignmentCoach.jsp?UserAssignment=" + value;
			myWindow.moveTo(x, y);
			myWindow.location.href = query;
		}
	}

	function setSessionName(form) {

		if (form.selSession.value == "0") {
			alert("Please select Coaching Session");
		} else {
			form.action = "UserAssignment.jsp?setSession=1";
			form.method = "post";
			form.submit();
		}

	}
	function setSessionDate(form) {

		form.action = "UserAssignment.jsp?setdate=1";
		form.method = "post";
		form.submit();

	}
	function bookingStatusReport(form) {
		form.action = "UserAssignment.jsp?report=1";
		form.method = "post";
		form.submit();
	}
</script>
</head>

<body>

	<!-- select Session -->
	<%@ include file="nav.jsp"%>

	<%
		String username = (String) session.getAttribute("username");
		Vector userAssignments = new Vector();
		if (!logchk.isUsable(username)) {
	%>
	<font size="2"> <script>
		parent.location.href = "../index.jsp";
	</script> <%
 	} else {
 		Vector sessionlist = SessionSetup.getAllSession();
 		userAssignments = SessionSetup.getUserAssignment();
 		if (request.getParameter("setSession") != null) {
 			//set up the org
 			int sessionPK = Integer.parseInt(request
 					.getParameter("selSession"));
 			SessionSetup.setSelectedSession(sessionPK);
 			System.out.println("Selected Session:" + sessionPK);
 			// get the data using session PK
 			userAssignments = SessionSetup.getUserAssignment();
 		}
 		if (request.getParameter("setdate") != null) {
 			//set up the org
 			int datePK = Integer.parseInt(request
 					.getParameter("selDate"));
 			SessionSetup.setSelectedDate(datePK);
 			if (SessionSetup.getSelectedDate() == 0) {
 				userAssignments = SessionSetup.getUserAssignment();
 			} else {
 				userAssignments = SessionSetup
 						.getUserAssignment(SessionSetup
 								.getSelectedDate());
 			}
 		}
 		if (request.getParameter("open") != null) {
 			int selAssignmentPK = Integer.parseInt(request
 					.getParameter("selAssignment"));
 			SessionSetup.openSlot(selAssignmentPK);
 			if (SessionSetup.getSelectedDate() == 0) {
 				userAssignments = SessionSetup.getUserAssignment();
 			} else {
 				userAssignments = SessionSetup
 						.getUserAssignment(SessionSetup
 								.getSelectedDate());
 			}
 		}
 		if (request.getParameter("close") != null) {
 			int selAssignmentPK = Integer.parseInt(request
 					.getParameter("selAssignment"));
 			SessionSetup.closeSlot(selAssignmentPK);
 			if (SessionSetup.getSelectedDate() == 0) {
 				userAssignments = SessionSetup.getUserAssignment();
 			} else {
 				userAssignments = SessionSetup
 						.getUserAssignment(SessionSetup
 								.getSelectedDate());
 			}
 		}
 		if (request.getParameter("deleteSlot") != null) {
 			int selAssignmentPK = Integer.parseInt(request
 					.getParameter("selAssignment"));
 			SessionSetup.deleteUserAssignment(selAssignmentPK);
 			if (SessionSetup.getSelectedDate() == 0) {
 				userAssignments = SessionSetup.getUserAssignment();
 			} else {
 				userAssignments = SessionSetup
 						.getUserAssignment(SessionSetup
 								.getSelectedDate());
 			}
 		}

 		if (request.getParameter("unSignUp") != null) {
 			int selAssignmentPK = Integer.parseInt(request
 					.getParameter("selAssignment"));
 			SessionSetup.setSelectUser(-1);
 			SessionSetup.setSelectedUserAssignment(selAssignmentPK);
 			SessionSetup.updateUser();
 			if (SessionSetup.getSelectedDate() == 0) {
 				userAssignments = SessionSetup.getUserAssignment();
 			} else {
 				userAssignments = SessionSetup
 						.getUserAssignment(SessionSetup
 								.getSelectedDate());
 			}
 %> <script type="text/javascript">
		alert("Delete booking successfully");
	</script> <%
 	}
 		if (request.getParameter("report") != null) {

 			if (SessionSetup.getSelectedSession() == 0) {
 %> <script type="text/javascript">
		alert("Please select coaching session");
	</script> <%
 	} else {

 				String file_name = "";
 				int iExportType = 12;

 				Export.setOrgID(logchk.getOrg()); //Set OrgID first
 				Export.setSelectedSession(SessionSetup
 						.getSelectedSession());
 				Export.export(iExportType, logchk.getPKUser());
 				file_name = Export.getCoachingStatusFileName();

 				//read the file name.		
 				String output = setting.getReport_Path() + "\\"
 						+ file_name;
 				File f = new File(output);

 				response.reset();
 				//set the content type(can be excel/word/powerpoint etc..)
 				response.setContentType("application/xls");
 				//set the header and also the Name by which user will be prompted to save
 				response.addHeader("Content-Disposition",
 						"attachment;filename=\"" + file_name + "\"");

 				//get the file name
 				String name = f.getName().substring(
 						f.getName().lastIndexOf("/") + 1,
 						f.getName().length());
 				//OPen an input stream to the file and post the file contents thru the 
 				//servlet output stream to the client m/c
 				InputStream in = new FileInputStream(f);
 				ServletOutputStream outs = response.getOutputStream();

 				int bit = 256;
 				int i = 0;

 				try {
 					while ((bit) >= 0) {
 						bit = in.read();
 						outs.write(bit);
 					}
 					//System.out.println("" +bit);
 				} catch (IOException ioe) {
 					ioe.printStackTrace(System.out);
 				}
 				outs.flush();
 				outs.close();
 				in.close();
 			}

 		}
 		/************************************************** ADDING TOGGLE FOR SORTING PURPOSE *************************************************/

 		int toggle = SessionSetup.getToggle(); //0=asc, 1=desc
 		int type = 1; //1=date, 2=starting time 3= coach 4= status

 		if (request.getParameter("name") != null) {
 			if (toggle == 0)
 				toggle = 1;
 			else
 				toggle = 0;

 			SessionSetup.setToggle(toggle);

 			type = Integer.parseInt(request.getParameter("name"));
 			SessionSetup.setSortType(type);
 		}

 		/************************************************** ADDING TOGGLE FOR SORTING PURPOSE *************************************************/
 %>

		<form method="post">
			<p>
			<p>
				<b><font color="#000080" size="3" face="Arial">Candidate
						Assignment</font></b>
			</p>
			<br> <br>
			<table>
				<tr>
					<td><b><font color="#000080" size="2" face="Arial">Session
								Name: </font></b></td>
					<td width="500" colspan="1"><select size="1" name="selSession"
						onChange="setSessionName(this.form)">
							<option value=0>Select a Session Name</option>
							<%
								for (int i = 0; i < sessionlist.size(); i++) {

										voCoachSession sessionDistic = (voCoachSession) sessionlist
												.elementAt(i);
										int sessionPK = sessionDistic.getPK();
										String sessionName = sessionDistic.getName();
										String sessionCode = sessionDistic.getDescription();

										if (SessionSetup.getSelectedSession() == sessionPK) {
							%>
							<option value=<%=sessionPK%> selected><%=sessionName%>
								<%
									} else {
								%>
							
							<option value=<%=sessionPK%>><%=sessionName%>
								<%
									}
										}
								%>
							
					</select></td>
				</tr>
				<tr>
					<td height="20"></td>
				</tr>
				<tr>
					<td><b><font color="#000080" size="2" face="Arial">Session
								Date:</font></b></td>
					<td width="500" colspan="1"><select size="1" name="selDate"
						onChange="setSessionDate(this.form)">
							<option value=0>ALL</option>
							<%
								for (int i = 0; i < SessionSetup.getCoachDates().size(); i++) {

										voCoachDate date = (voCoachDate) SessionSetup
												.getCoachDates().elementAt(i);
										int datePK = date.getPK();
										String Date = date.getDate().substring(0, 10);
										Date dateString = new SimpleDateFormat("yyyy-MM-dd")
												.parse(Date);
										SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");
										String finalDate = sdf.format(dateString);
										if (SessionSetup.getSelectedDate() == datePK) {
							%>
							<option value=<%=datePK%> selected><%=finalDate%>
								<%
									} else {
								%>
							
							<option value=<%=datePK%>><%=finalDate%>
								<%
									}
										}
								%>
							
					</select></td>
				</tr>
			</table>
			<br>
			<p>Tips: Can sort the table by clicking the column name</p>
			<table>


				<th width="30" bgcolor="navy" bordercolor="#3399FF" align="center"><b>
						<font style='color: white'>&nbsp;</font>
				</b></th>
				<th width="30" bgcolor="navy" bordercolor="#3399FF" align="center"><b>
						<font style='color: white'>No</font>
				</b></th>
				<th width="80" bgcolor="navy" bordercolor="#3399FF" align="center"><a
					href="UserAssignment.jsp?name=1"><b> <font
							style='color: white'>Date</font>
					</b></th>
				<th width="70" bgcolor="navy" bordercolor="#3399FF" align="center"><a
					href="UserAssignment.jsp?name=2"><b> <font
							style='color: white'>Starting Time</font>
					</b></th>
				<th width="70" bgcolor="navy" bordercolor="#3399FF" align="center"><a
					href="UserAssignment.jsp?name=5"><b> <font
							style='color: white'>Ending Time</font>
					</b></th>
				<th width="150" bgcolor="navy" bordercolor="#3399FF" align="center"><a
					href="UserAssignment.jsp?name=3"><b> <font
							style='color: white'>Coach</font>
					</b></th>
				<th width="150" bgcolor="navy" bordercolor="#3399FF" align="center"><a
					href="UserAssignment.jsp?name=3"><b> <font
							style='color: white'>Venue</font>
					</b></th>

				<th width="120" bgcolor="navy" bordercolor="#3399FF" align="center"><b>
						<font style='color: white'>Organization Name</font>
				</b></th>

				<th width="40" bgcolor="navy" bordercolor="#3399FF" align="center"><a
					href="UserAssignment.jsp?name=4"><b> <font
							style='color: white'>Status</font>
					</b></th>
				<th width="200" bgcolor="navy" bordercolor="#3399FF" align="center"><b>
						<font style='color: white'>User</font>
				</b></th>

				<%
					int DisplayNo = 1;

						for (int i = 0; i < userAssignments.size(); i++) {
							voCoachUserAssignment userAssignment = new voCoachUserAssignment();
							userAssignment = (voCoachUserAssignment) userAssignments
									.elementAt(i);

							int AssignmentPK = userAssignment.getAssignmentPK();
							int StartingTime = userAssignment.getStartingTime();
							int EndingTime = userAssignment.getEndingTime();
							int venueFK = userAssignment.getVenueFK();
							String CoachName = userAssignment.getCoachName();
							String Date = userAssignment.getDate().substring(0, 10);
							Date date = new SimpleDateFormat("yyyy-MM-dd").parse(Date);
							SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");
							String finalDate = sdf.format(date);
							String OrganizationName = userAssignment
									.getOrganizationName();
							String sessionVenue = userAssignment.getSessionVenue();
							int Status = userAssignment.getStatus();
							int UserFK = userAssignment.getUserFK();
							String userNameInAssignment = SessionSetup
									.getUserNamebyID(UserFK);
							//System.out.println("User PK:"+UserFK);
							String fullName = User.getUserInfo(UserFK).getFamilyName()
									+ " " + User.getUserInfo(UserFK).getGivenName();
							String startingTime4Digits;
							String endingTime4Digits;
							if (StartingTime < 1000) {
								startingTime4Digits = "0" + StartingTime;
							} else {
								startingTime4Digits = "" + StartingTime;
							}
							if (EndingTime < 1000) {
								endingTime4Digits = "0" + EndingTime;
							} else {
								endingTime4Digits = "" + EndingTime;
							}
							voCoachVenue venue = Venue.getSelectedCoachVenue(venueFK);
							String address1 = venue.getVenue1();
				%>
				<tr onMouseOver="this.bgColor = '#99ccff'"
					onMouseOut="this.bgColor = '#FFFFCC'">
					<td style="border-width: 1px"><font size="2"> <input
							type="radio" name="selAssignment" value=<%=AssignmentPK%>></font></td>
					<td align="center"><%=DisplayNo%></td>
					<td align="center"><%=finalDate%></td>
					<td align="center"><%=startingTime4Digits%></td>
					<td align="center"><%=endingTime4Digits%></td>
					<td align="center"><%=CoachName%></td>
					<!-- address1 column -->
					<%
						if (address1 == null || "".equalsIgnoreCase(address1)) {
					%><td align="center">&nbsp;</td>
					<%
						} else {
					%>
					<td align="center"><%=address1%></td>
					<%
						}
					%>
					<!-- address1 column -->
					<td align="center"><%=OrganizationName%></td>
					<!-- Status column -->
					<%
						if (Status == 1)
					%>
					<td align="center">Open</td>
					<%
						else {
					%>
					<td align="center">Closed</td>
					<%
						}
					%>
					<!-- Status column -->
					<td align="center"><%=fullName%></td>


				</tr>
				<%
					DisplayNo++;

						}
				%>
			</table>
			<br> <br>




			<div class="btn-toolbar" style="margin: 0;">
				<div class="btn-group">
					<button class="btn dropdown-toggle" data-toggle="dropdown">
						Slot Management <span class="caret"></span>
					</button>
					<ul class="dropdown-menu">
						<li><input class="btn" type="button" name="close"
							value="Close Slot"
							onclick="closeSlotFunction(this.form,this.form.selAssignment)"></li>
						<li><input class="btn" type="button" name="add"
							value="Add Slot"
							onclick="addSlotFunction(this.form,this.form.selSession)"></li>
						<li><input class="btn" type="button" name="close"
							value="Close Slot"
							onclick="closeSlotFunction(this.form,this.form.selAssignment)"></li>
						<li><input class="btn" type="button" name="open"
							value="Delete Slot"
							onclick="deleteSlotFunction(this.form,this.form.selAssignment)"></li>

					</ul>
				</div>
				<!-- /btn-group -->
				<div class="btn-group">
					<button class="btn btn-primary dropdown-toggle"
						data-toggle="dropdown">
						Coach and Venue Mangement <span class="caret"></span>
					</button>
					<ul class="dropdown-menu">
						<li><input class="btn btn-primary" type="button" name="coach"
							value="Change Coach"
							onclick="changeCoach(this.form,this.form.selAssignment)"></li>
						<li><input class="btn btn-primary" type="button" name="venue"
							value="Change Venue"
							onclick="changeVenue(this.form,this.form.selAssignment)"></li>
					</ul>
				</div>
				<!-- /btn-group -->

				<div class="btn-group">
					<button class="btn btn-success dropdown-toggle"
						data-toggle="dropdown">
						Booking Managment <span class="caret"></span>
					</button>
					<ul class="dropdown-menu">
						<li><input class="btn btn-success" type="button"
							name="assign" value="Book Candidate"
							onclick="assignUser(this.form,this.form.selAssignment)"></li>
						<li><input class="btn btn-success" type="button"
							name="unsign" value="Delete Booking"
							onclick="unSignUp(this.form,this.form.selAssignment)"></li>
					</ul>
				</div>
				<!-- /btn-group -->



			</div>
			<!-- /btn-toolbar -->
		</form> <%
 	}
 %>
</body>
</html>