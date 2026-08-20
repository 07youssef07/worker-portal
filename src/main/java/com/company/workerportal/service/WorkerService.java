package com.company.workerportal.service;

import com.company.workerportal.dao.WorkerDAO;
import com.company.workerportal.model.Worker;

import java.util.List;

public class WorkerService {

    private final WorkerDAO workerDAO = new WorkerDAO();

    public Worker[] getAllWorkers() {
        List<Worker> workers = workerDAO.findAll();
        return workers.toArray(new Worker[0]);
    }

    public Worker[] searchWorkers(String searchTerm, String role, String sortField, boolean descending) {
        List<Worker> workers = workerDAO.search(searchTerm, role, sortField, descending);
        return workers.toArray(new Worker[0]);
    }

    public String[] getDistinctRoles() {
        List<String> roles = workerDAO.findDistinctRoles();
        return roles.toArray(new String[0]);
    }

    public Worker getWorkerById(Long id) {
        return workerDAO.findById(id);
    }

    
    public Long addWorker(Worker worker) {
        String error = validate(worker);
        if (error != null) {
            return null;
        }
        worker.setId(null);
        workerDAO.save(worker);
        return worker.getId();
    }

    
    public boolean updateWorker(Long id, Worker worker) {
        if (id == null || workerDAO.findById(id) == null) {
            return false;
        }
        String error = validate(worker);
        if (error != null) {
            return false;
        }
        worker.setId(id);
        workerDAO.update(worker);
        return true;
    }

    public boolean deleteWorker(Long id) {
        if (id == null || workerDAO.findById(id) == null) {
            return false;
        }
        workerDAO.delete(id);
        return true;
    }

    public String validate(Worker worker) {
        if (worker == null) {
            return "Worker is required.";
        }
        if (isBlank(worker.getFirstName())) {
            return "firstName is required.";
        }
        if (isBlank(worker.getLastName())) {
            return "lastName is required.";
        }
        if (worker.getDateOfBirth() == null) {
            return "dateOfBirth is required.";
        }
        if (worker.getDateOfBirth().isAfter(java.time.LocalDate.now())) {
            return "dateOfBirth cannot be in the future.";
        }
        if (isBlank(worker.getRole())) {
            return "role is required.";
        }
        return null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
