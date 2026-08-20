package com.company.workerportal.dao;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public final class JpaUtil {

    private static final EntityManagerFactory EMF =
            Persistence.createEntityManagerFactory("workerPortalPU");

    private JpaUtil() {
    }

    public static EntityManagerFactory getEmf() {
        return EMF;
    }
}
