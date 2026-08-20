package com.company.workerportal.soap;

import com.company.workerportal.model.Worker;
import com.company.workerportal.service.WorkerService;
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;


@WebService(serviceName = "WorkerService", portName = "WorkerServicePort",
        targetNamespace = "http://soap.workerportal.company.com/")
public class WorkerSoapService {

    private final WorkerService workerService = new WorkerService();

    @WebMethod(operationName = "getAllWorkers")
    @WebResult(name = "workers")
    public WorkerSoapDTO[] getAllWorkers() {
        Worker[] workers = workerService.getAllWorkers();
        WorkerSoapDTO[] dtos = new WorkerSoapDTO[workers.length];
        for (int i = 0; i < workers.length; i++) {
            dtos[i] = toDto(workers[i]);
        }
        return dtos;
    }

    @WebMethod(operationName = "getWorkerById")
    @WebResult(name = "worker")
    public WorkerSoapDTO getWorkerById(@WebParam(name = "id") long id) {
        Worker worker = workerService.getWorkerById(id);
        return worker == null ? null : toDto(worker);
    }

    @WebMethod(operationName = "addWorker")
    @WebResult(name = "id")
    public long addWorker(@WebParam(name = "worker") WorkerSoapDTO dto) {
        Worker worker = toEntity(dto, null);
        if (worker == null) {
            return -1;
        }
        Long newId = workerService.addWorker(worker);
        return newId == null ? -1 : newId;
    }

    @WebMethod(operationName = "updateWorker")
    @WebResult(name = "success")
    public boolean updateWorker(@WebParam(name = "worker") WorkerSoapDTO dto) {
        if (dto == null || dto.getId() == null) {
            return false;
        }
        Worker worker = toEntity(dto, dto.getId());
        if (worker == null) {
            return false;
        }
        return workerService.updateWorker(dto.getId(), worker);
    }

    @WebMethod(operationName = "deleteWorker")
    @WebResult(name = "success")
    public boolean deleteWorker(@WebParam(name = "id") long id) {
        return workerService.deleteWorker(id);
    }

    private static WorkerSoapDTO toDto(Worker w) {
        return new WorkerSoapDTO(
                w.getId(),
                w.getFirstName(),
                w.getLastName(),
                w.getDateOfBirth() == null ? null : w.getDateOfBirth().toString(),
                w.getRole());
    }

    private static Worker toEntity(WorkerSoapDTO dto, Long id) {
        if (dto == null || isBlank(dto.getFirstName()) || isBlank(dto.getLastName())
                || isBlank(dto.getDateOfBirth()) || isBlank(dto.getRole())) {
            return null;
        }
        LocalDate dob;
        try {
            dob = LocalDate.parse(dto.getDateOfBirth());
        } catch (DateTimeParseException e) {
            return null;
        }
        Worker worker = new Worker();
        worker.setId(id);
        worker.setFirstName(dto.getFirstName());
        worker.setLastName(dto.getLastName());
        worker.setDateOfBirth(dob);
        worker.setRole(dto.getRole());
        return worker;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
