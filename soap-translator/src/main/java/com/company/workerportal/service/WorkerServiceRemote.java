package com.company.workerportal.service;

import java.time.LocalDate;

public interface WorkerServiceRemote {

    WorkerDTO[] getAllWorkers();

    WorkerDTO[] searchWorkers(String searchTerm, String role, String sortField,
                               boolean descending, LocalDate dateFrom, LocalDate dateTo);

    String[] getDistinctRoles();

    WorkerDTO getWorkerById(Long id);

    Long addWorker(WorkerDTO worker);

    boolean updateWorker(Long id, WorkerDTO worker);

    boolean deleteWorker(Long id);

    String validate(WorkerDTO worker);
}
