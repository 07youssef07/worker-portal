package com.company.workerportal.web;

import com.company.workerportal.service.WorkerService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/workers/delete")
public class WorkerDeleteServlet extends HttpServlet {

    private final WorkerService workerService = new WorkerService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String idParam = req.getParameter("id");
        if (idParam != null && !idParam.isBlank()) {
            workerService.deleteWorker(Long.valueOf(idParam));
        }
        resp.sendRedirect(req.getContextPath() + "/workers");
    }
}
