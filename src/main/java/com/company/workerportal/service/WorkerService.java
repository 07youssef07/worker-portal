package com.company.workerportal.service;

import com.company.workerportal.dao.WorkerDAO;
import com.company.workerportal.model.Worker;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
@Remote(WorkerServiceRemote.class)
public class WorkerService implements WorkerServiceRemote {

    private final WorkerDAO workerDAO = new WorkerDAO();

    public WorkerDTO[] getAllWorkers() {
        List<Worker> workers = workerDAO.findAll();
        return workers.stream().map(this::toDTO).toArray(WorkerDTO[]::new);
    }

    public WorkerDTO[] searchWorkers(String searchTerm, String role, String sortField, boolean descending,
                                      LocalDate dateFrom, LocalDate dateTo) {
        List<Worker> workers = workerDAO.search(searchTerm, role, sortField, descending, dateFrom, dateTo);
        return workers.stream().map(this::toDTO).toArray(WorkerDTO[]::new);
    }

    public String[] getDistinctRoles() {
        List<String> roles = workerDAO.findDistinctRoles();
        return roles.toArray(new String[0]);
    }

    public WorkerDTO getWorkerById(Long id) {
        Worker worker = workerDAO.findById(id);
        return worker != null ? toDTO(worker) : null;
    }

    public Long addWorker(WorkerDTO dto) {
        String error = validate(dto);
        if (error != null) {
            return null;
        }
        Worker worker = fromDTO(dto);
        worker.setId(null);
        workerDAO.save(worker);
        return worker.getId();
    }

    public boolean updateWorker(Long id, WorkerDTO dto) {
        if (id == null || workerDAO.findById(id) == null) {
            return false;
        }
        String error = validate(dto);
        if (error != null) {
            return false;
        }
        Worker worker = fromDTO(dto);
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

    public String validate(WorkerDTO dto) {
        if (dto == null) {
            return "Worker is required.";
        }
        if (isBlank(dto.getFirstName())) {
            return "firstName is required.";
        }
        if (isBlank(dto.getLastName())) {
            return "lastName is required.";
        }
        if (isBlank(dto.getDateOfBirth())) {
            return "dateOfBirth is required.";
        }
        try {
            LocalDate dob = LocalDate.parse(dto.getDateOfBirth());
            if (dob.isAfter(LocalDate.now())) {
                return "dateOfBirth cannot be in the future.";
            }
        } catch (Exception e) {
            return "dateOfBirth must be in yyyy-MM-dd format.";
        }
        if (isBlank(dto.getRole())) {
            return "role is required.";
        }
        return null;
    }

    private WorkerDTO toDTO(Worker w) {
        WorkerDTO dto = new WorkerDTO();
        dto.setId(w.getId());
        dto.setFirstName(w.getFirstName());
        dto.setLastName(w.getLastName());
        dto.setDateOfBirth(w.getDateOfBirth() != null ? w.getDateOfBirth().toString() : null);
        dto.setRole(w.getRole());
        return dto;
    }

    private Worker fromDTO(WorkerDTO dto) {
        Worker worker = new Worker();
        worker.setFirstName(dto.getFirstName());
        worker.setLastName(dto.getLastName());
        worker.setDateOfBirth(dto.getDateOfBirth() != null ? LocalDate.parse(dto.getDateOfBirth()) : null);
        worker.setRole(dto.getRole());
        return worker;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
