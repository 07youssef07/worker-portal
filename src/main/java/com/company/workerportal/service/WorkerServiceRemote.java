package com.company.workerportal.service;

import com.company.workerportal.model.Worker;
import java.time.LocalDate;

public interface WorkerServiceRemote {

    Worker[] getAllWorkers();

    Worker[] searchWorkers(String searchTerm, String role, String sortField,
                           boolean descending, LocalDate dateFrom, LocalDate dateTo);

    String[] getDistinctRoles();

    Worker getWorkerById(Long id);

    Long addWorker(Worker worker);

    boolean updateWorker(Long id, Worker worker);

    boolean deleteWorker(Long id);

    String validate(Worker worker);
}
