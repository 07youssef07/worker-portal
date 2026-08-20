<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Login - Worker Portal</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="login-wrapper">
    <form class="login-card" method="post" action="${pageContext.request.contextPath}/login">
        <h1>Worker Portal</h1>

        <% if (request.getAttribute("error") != null) { %>
        <p class="error"><%= request.getAttribute("error") %></p>
        <% } %>

        <label for="username">Username</label>
        <input type="text" id="username" name="username" required autofocus>

        <label for="password">Password</label>
        <input type="password" id="password" name="password" required>

        <button type="submit">Sign in</button>
    </form>
</div>
</body>
</html>
