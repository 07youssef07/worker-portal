package com.company.workerportal.web;

import com.company.workerportal.service.WorkerDTO;
import com.company.workerportal.service.WorkerService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@WebServlet("/workers")
public class WorkersServlet extends HttpServlet {

    private final WorkerService workerService = new WorkerService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String q = req.getParameter("q");
        String role = req.getParameter("role");
        String sort = req.getParameter("sort");
        String dir = req.getParameter("dir");
        boolean descending = "desc".equalsIgnoreCase(dir);

        WorkerDTO[] workers = workerService.searchWorkers(q, role, sort, descending, null, null);
        String[] roles = workerService.getDistinctRoles();

        List<WorkerDTO> workerList = Arrays.asList(workers);
        List<String> roleList = Arrays.asList(roles);

        req.setAttribute("workers", workerList);
        req.setAttribute("roles", roleList);
        req.setAttribute("q", q == null ? "" : q);
        req.setAttribute("role", role == null ? "" : role);
        req.setAttribute("sort", sort == null || sort.isBlank() ? "lastName" : sort);
        req.setAttribute("dir", descending ? "desc" : "asc");

        req.getRequestDispatcher("/workers.jsp").forward(req, resp);
    }
}
