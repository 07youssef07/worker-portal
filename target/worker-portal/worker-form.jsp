<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.company.workerportal.model.Worker" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Worker - Worker Portal</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<%
    Worker worker = (Worker) request.getAttribute("worker");
    boolean isEdit = worker != null && worker.getId() != null;
%>
<div class="page">
    <header class="topbar">
        <h1><%= isEdit ? "Edit Worker" : "Add Worker" %></h1>
    </header>

    <form class="worker-form" method="post" action="${pageContext.request.contextPath}/workers/form">
        <% if (request.getAttribute("error") != null) { %>
        <p class="error"><%= request.getAttribute("error") %></p>
        <% } %>

        <% if (isEdit) { %>
        <input type="hidden" name="id" value="<%= worker.getId() %>">
        <% } %>

        <label for="firstName">First name</label>
        <input type="text" id="firstName" name="firstName" required
               value="<%= worker != null && worker.getFirstName() != null ? worker.getFirstName() : "" %>">

        <label for="lastName">Last name</label>
        <input type="text" id="lastName" name="lastName" required
               value="<%= worker != null && worker.getLastName() != null ? worker.getLastName() : "" %>">

        <label for="dateOfBirth">Date of birth</label>
        <input type="date" id="dateOfBirth" name="dateOfBirth" required
               value="<%= worker != null && worker.getDateOfBirth() != null ? worker.getDateOfBirth() : "" %>">

        <label for="role">Role</label>
        <input type="text" id="role" name="role" required
               value="<%= worker != null && worker.getRole() != null ? worker.getRole() : "" %>">

        <div class="form-actions">
            <button type="submit">Save</button>
            <a class="link" href="${pageContext.request.contextPath}/workers">Cancel</a>
        </div>
    </form>
</div>
</body>
</html>
