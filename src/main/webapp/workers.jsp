<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.company.workerportal.service.WorkerDTO" %>
<%@ page import="java.util.List" %>
<%!
    private String sortUrl(String contextPath, String field, String currentSort,
                            String currentDir, String q, String role) {
        String nextDir = field.equals(currentSort) && "asc".equals(currentDir) ? "desc" : "asc";
        StringBuilder sb = new StringBuilder(contextPath).append("/workers?sort=").append(field)
                .append("&dir=").append(nextDir);
        if (q != null && !q.isEmpty()) {
            sb.append("&q=").append(java.net.URLEncoder.encode(q, java.nio.charset.StandardCharsets.UTF_8));
        }
        if (role != null && !role.isEmpty()) {
            sb.append("&role=").append(java.net.URLEncoder.encode(role, java.nio.charset.StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private String sortIndicator(String field, String currentSort, String currentDir) {
        if (!field.equals(currentSort)) {
            return "";
        }
        return "asc".equals(currentDir) ? " &#9650;" : " &#9660;";
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Workers - Worker Portal</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<%
    String contextPath = request.getContextPath();
    String q = (String) request.getAttribute("q");
    String role = (String) request.getAttribute("role");
    String sort = (String) request.getAttribute("sort");
    String dir = (String) request.getAttribute("dir");
    @SuppressWarnings("unchecked")
    List<String> roles = (List<String>) request.getAttribute("roles");
%>
<div class="page">
    <header class="topbar">
        <h1>Company Workers</h1>
        <div class="topbar-right">
            <a class="btn" href="<%= contextPath %>/workers/form">+ Add worker</a>
            <span>Signed in as <%= session.getAttribute("username") %></span>
            <a class="logout" href="<%= contextPath %>/logout">Log out</a>
        </div>
    </header>

    <form class="filters" method="get" action="<%= contextPath %>/workers">
        <input type="text" name="q" placeholder="Search by name..." value="<%= q %>">

        <select name="role">
            <option value="">All roles</option>
            <%
                if (roles != null) {
                    for (String r : roles) {
                        boolean selected = r.equals(role);
            %>
            <option value="<%= r %>" <%= selected ? "selected" : "" %>><%= r %></option>
            <%
                    }
                }
            %>
        </select>

        <input type="hidden" name="sort" value="<%= sort %>">
        <input type="hidden" name="dir" value="<%= dir %>">

        <button type="submit">Filter</button>
        <% if ((q != null && !q.isEmpty()) || (role != null && !role.isEmpty())) { %>
        <a class="link" href="<%= contextPath %>/workers">Clear</a>
        <% } %>
    </form>

    <table class="workers-table">
        <thead>
        <tr>
            <th><a class="sort-link" href="<%= sortUrl(contextPath, "firstName", sort, dir, q, role) %>">First name<%= sortIndicator("firstName", sort, dir) %></a></th>
            <th><a class="sort-link" href="<%= sortUrl(contextPath, "lastName", sort, dir, q, role) %>">Last name<%= sortIndicator("lastName", sort, dir) %></a></th>
            <th><a class="sort-link" href="<%= sortUrl(contextPath, "dateOfBirth", sort, dir, q, role) %>">Date of birth<%= sortIndicator("dateOfBirth", sort, dir) %></a></th>
            <th><a class="sort-link" href="<%= sortUrl(contextPath, "role", sort, dir, q, role) %>">Role<%= sortIndicator("role", sort, dir) %></a></th>
            <th>Actions</th>
        </tr>
        </thead>
        <tbody>
        <%
            @SuppressWarnings("unchecked")
            List<WorkerDTO> workers = (List<WorkerDTO>) request.getAttribute("workers");
            if (workers == null || workers.isEmpty()) {
        %>
        <tr>
            <td colspan="5" class="empty">No workers found.</td>
        </tr>
        <%
            } else {
                for (WorkerDTO w : workers) {
        %>
        <tr>
            <td><%= w.getFirstName() %></td>
            <td><%= w.getLastName() %></td>
            <td><%= w.getDateOfBirth() %></td>
            <td><%= w.getRole() %></td>
            <td class="actions">
                <a class="link" href="<%= contextPath %>/workers/form?id=<%= w.getId() %>">Edit</a>
                <form method="post" action="<%= contextPath %>/workers/delete"
                      onsubmit="return confirm('Delete this worker?');" class="inline-form">
                    <input type="hidden" name="id" value="<%= w.getId() %>">
                    <button type="submit" class="link danger">Delete</button>
                </form>
            </td>
        </tr>
        <%
                }
            }
        %>
        </tbody>
    </table>
</div>
</body>
</html>
