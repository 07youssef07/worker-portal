package com.company.workerportal.rest;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Activates JAX-RS and mounts all REST resources under /api.
 * WorkerResource below is reachable at: /worker-portal/api/workers
 */
@ApplicationPath("/api")
public class RestApplication extends Application {
}
