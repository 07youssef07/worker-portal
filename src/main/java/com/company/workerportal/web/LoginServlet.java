package com.company.workerportal.web;

import com.company.workerportal.dao.UserDAO;
import com.company.workerportal.model.User;
import com.company.workerportal.security.PasswordUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.getRequestDispatcher("/login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        User user = (username != null && !username.isBlank())
                ? userDAO.findByUsername(username.trim())
                : null;

        boolean ok = user != null && password != null
                && PasswordUtil.verify(password, user.getPasswordHash());

        if (ok) {
            HttpSession session = req.getSession(true);
            session.setAttribute("username", user.getUsername());
            resp.sendRedirect(req.getContextPath() + "/workers");
        } else {
            req.setAttribute("error", "Invalid username or password.");
            req.getRequestDispatcher("/login.jsp").forward(req, resp);
        }
    }
}
