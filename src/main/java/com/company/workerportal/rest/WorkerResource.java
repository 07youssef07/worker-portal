package com.company.workerportal.rest;

import com.company.workerportal.model.Worker;
import com.company.workerportal.service.WorkerService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * JSON REST API for workers, secured with HTTP Basic Auth (see
 * ApiAuthFilter). Delegates all business logic to WorkerService rather
 * than talking to WorkerDAO directly.
 *
 * Endpoints:
 *   GET    /api/workers                -> list all (optionally ?q=&role=&sort=&dir=)
 *   GET    /api/workers/{id}           -> get one
 *   POST   /api/workers                -> create (JSON body, no "id" field needed)
 *   PUT    /api/workers/{id}           -> update
 *   DELETE /api/workers/{id}           -> delete
 */
@Path("/workers")
public class WorkerResource {

    private final WorkerService workerService = new WorkerService();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Worker[] getAll(@QueryParam("q") String q,
                            @QueryParam("role") String role,
                            @QueryParam("sort") String sort,
                            @QueryParam("dir") String dir) {
        boolean descending = "desc".equalsIgnoreCase(dir);
        if (q == null && role == null && sort == null) {
            return workerService.getAllWorkers();
        }
        return workerService.searchWorkers(q, role, sort, descending);
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOne(@PathParam("id") Long id) {
        Worker worker = workerService.getWorkerById(id);
        if (worker == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorMessage("Worker " + id + " not found"))
                    .build();
        }
        return Response.ok(worker).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(Worker worker) {
        String error = workerService.validate(worker);
        if (error != null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorMessage(error))
                    .build();
        }
        Long newId = workerService.addWorker(worker);
        worker.setId(newId);
        return Response.status(Response.Status.CREATED).entity(worker).build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("id") Long id, Worker worker) {
        String error = workerService.validate(worker);
        if (error != null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorMessage(error))
                    .build();
        }
        boolean updated = workerService.updateWorker(id, worker);
        if (!updated) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorMessage("Worker " + id + " not found"))
                    .build();
        }
        worker.setId(id);
        return Response.ok(worker).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        boolean deleted = workerService.deleteWorker(id);
        if (!deleted) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorMessage("Worker " + id + " not found"))
                    .build();
        }
        return Response.noContent().build();
    }

    /** Simple JSON error body: {"message": "..."} */
    public static class ErrorMessage {
        public String message;

        public ErrorMessage() {
        }

        public ErrorMessage(String message) {
            this.message = message;
        }
    }
}
