package com.company.workerportal.web;

import com.company.workerportal.service.WorkerDTO;
import com.company.workerportal.service.WorkerService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/workers/form")
public class WorkerFormServlet extends HttpServlet {

    private final WorkerService workerService = new WorkerService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String idParam = req.getParameter("id");
        if (idParam != null && !idParam.isBlank()) {
            WorkerDTO worker = workerService.getWorkerById(null, Long.valueOf(idParam));
            if (worker == null) {
                resp.sendRedirect(req.getContextPath() + "/workers");
                return;
            }
            req.setAttribute("worker", worker);
        }
        req.getRequestDispatcher("/worker-form.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String idParam = req.getParameter("id");
        String firstName = trimOrNull(req.getParameter("firstName"));
        String lastName = trimOrNull(req.getParameter("lastName"));
        String dobParam = trimOrNull(req.getParameter("dateOfBirth"));
        String role = trimOrNull(req.getParameter("role"));

        WorkerDTO worker = new WorkerDTO();
        if (idParam != null && !idParam.isBlank()) {
            worker.setId(Long.valueOf(idParam));
        }
        worker.setFirstName(firstName);
        worker.setLastName(lastName);
        worker.setDateOfBirth(dobParam);
        worker.setRole(role);

        String error = workerService.validate(null, worker);

        if (error != null) {
            req.setAttribute("worker", worker);
            req.setAttribute("error", error);
            req.getRequestDispatcher("/worker-form.jsp").forward(req, resp);
            return;
        }

        if (worker.getId() == null) {
            workerService.addWorker(null, worker);
        } else {
            workerService.updateWorker(null, worker.getId(), worker);
        }

        resp.sendRedirect(req.getContextPath() + "/workers");
    }

    private static String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
