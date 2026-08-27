package com.company.workerportal.service;

import com.company.workerportal.dao.UserDAO;
import com.company.workerportal.dao.WorkerDAO;
import com.company.workerportal.model.User;
import com.company.workerportal.model.Worker;
import com.company.workerportal.security.PasswordUtil;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

import java.time.LocalDate;
import java.util.List;

@Stateless
@Remote(WorkerServiceRemote.class)
public class WorkerService implements WorkerServiceRemote {

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_VIEWER = "VIEWER";

    private final WorkerDAO workerDAO = new WorkerDAO();
    private final UserDAO userDAO = new UserDAO();

    // Operations a VIEWER (read-only) role is allowed to invoke.
    private static final java.util.Set<String> READ_ONLY_OPERATIONS =
            java.util.Set.of("getAllWorkers", "searchWorkers", "getWorkerById",
                    "getDistinctRoles", "validate");

    public WorkerDTO[] getAllWorkers(AuthRequest caller) {
        checkAllowed(caller, "getAllWorkers");
        List<Worker> workers = workerDAO.findAll();
        return workers.stream().map(this::toDTO).toArray(WorkerDTO[]::new);
    }

    public WorkerDTO[] searchWorkers(AuthRequest caller, String searchTerm, String role, String sortField, boolean descending,
                                      LocalDate dateFrom, LocalDate dateTo) {
        checkAllowed(caller, "searchWorkers");
        List<Worker> workers = workerDAO.search(searchTerm, role, sortField, descending, dateFrom, dateTo);
        return workers.stream().map(this::toDTO).toArray(WorkerDTO[]::new);
    }

    public String[] getDistinctRoles(AuthRequest caller) {
        checkAllowed(caller, "getDistinctRoles");
        List<String> roles = workerDAO.findDistinctRoles();
        return roles.toArray(new String[0]);
    }

    public WorkerDTO getWorkerById(AuthRequest caller, Long id) {
        checkAllowed(caller, "getWorkerById");
        Worker worker = workerDAO.findById(id);
        return worker != null ? toDTO(worker) : null;
    }

    public Long addWorker(AuthRequest caller, WorkerDTO dto) {
        checkAllowed(caller, "addWorker");
        String error = validate(caller, dto);
        if (error != null) {
            return null;
        }
        Worker worker = fromDTO(dto);
        worker.setId(null);
        workerDAO.save(worker);
        return worker.getId();
    }

    public boolean updateWorker(AuthRequest caller, Long id, WorkerDTO dto) {
        checkAllowed(caller, "updateWorker");
        if (id == null || workerDAO.findById(id) == null) {
            return false;
        }
        String error = validate(caller, dto);
        if (error != null) {
            return false;
        }
        Worker worker = fromDTO(dto);
        worker.setId(id);
        workerDAO.update(worker);
        return true;
    }

    public boolean deleteWorker(AuthRequest caller, Long id) {
        checkAllowed(caller, "deleteWorker");
        if (id == null || workerDAO.findById(id) == null) {
            return false;
        }
        workerDAO.delete(id);
        return true;
    }

    public String validate(AuthRequest caller, WorkerDTO dto) {
        checkAllowed(caller, "validate");
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

    /**
     * Validates the WS-Security caller credentials against the database and
     * enforces role-based authorization for the given operation.
     *
     * <p>A {@code null} caller represents an already-authenticated internal
     * caller (Web UI / REST front-controllers) and skips the SOAP authorization
     * rules. A non-null caller is the SOAP gateway (soap-translator) forwarding
     * WS-Security credentials, which are always verified and role-checked.</p>
     *
     * @throws SecurityException with a clear message when authentication or
     *                           authorization fails.
     */
    private void checkAllowed(AuthRequest caller, String operation) {
        if (caller == null) {
            return;
        }
        if (isBlank(caller.getUsername()) || isBlank(caller.getPassword())) {
            throw new SecurityException("Missing or blank WS-Security credentials");
        }
        User user = userDAO.findByUsername(caller.getUsername());
        if (user == null || !PasswordUtil.verify(caller.getPassword(), user.getPasswordHash())) {
            throw new SecurityException("Invalid credentials");
        }
        String role = user.getRole();
        if (!ROLE_ADMIN.equals(role) && !READ_ONLY_OPERATIONS.contains(operation)) {
            throw new SecurityException("Access denied: operation '" + operation
                    + "' not permitted for role '" + role + "'");
        }
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
