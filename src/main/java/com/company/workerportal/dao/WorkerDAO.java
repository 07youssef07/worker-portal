package com.company.workerportal.dao;

import com.company.workerportal.model.Worker;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Set;

public class WorkerDAO {

    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "firstName", "lastName", "dateOfBirth", "role");

    public List<Worker> findAll() {
        EntityManager em = JpaUtil.getEmf().createEntityManager();
        try {
            return em.createNamedQuery("Worker.findAll", Worker.class).getResultList();
        } finally {
            em.close();
        }
    }

    public List<Worker> search(String searchTerm, String role, String sortField, boolean descending) {
        EntityManager em = JpaUtil.getEmf().createEntityManager();
        try {
            StringBuilder jpql = new StringBuilder("SELECT w FROM Worker w WHERE 1=1");

            boolean hasSearch = searchTerm != null && !searchTerm.isBlank();
            boolean hasRole = role != null && !role.isBlank();

            if (hasSearch) {
                jpql.append(" AND (LOWER(w.firstName) LIKE :term OR LOWER(w.lastName) LIKE :term)");
            }
            if (hasRole) {
                jpql.append(" AND w.role = :role");
            }

            String field = (sortField != null && SORTABLE_FIELDS.contains(sortField)) ? sortField : "lastName";
            jpql.append(" ORDER BY w.").append(field).append(descending ? " DESC" : " ASC");

            TypedQuery<Worker> query = em.createQuery(jpql.toString(), Worker.class);
            if (hasSearch) {
                query.setParameter("term", "%" + searchTerm.trim().toLowerCase() + "%");
            }
            if (hasRole) {
                query.setParameter("role", role.trim());
            }

            return query.getResultList();
        } finally {
            em.close();
        }
    }

    public List<String> findDistinctRoles() {
        EntityManager em = JpaUtil.getEmf().createEntityManager();
        try {
            return em.createQuery(
                    "SELECT DISTINCT w.role FROM Worker w ORDER BY w.role", String.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public Worker findById(Long id) {
        EntityManager em = JpaUtil.getEmf().createEntityManager();
        try {
            return em.find(Worker.class, id);
        } finally {
            em.close();
        }
    }

    public void save(Worker worker) {
        EntityManager em = JpaUtil.getEmf().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(worker);
            tx.commit();
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public void update(Worker worker) {
        EntityManager em = JpaUtil.getEmf().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.merge(worker);
            tx.commit();
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public void delete(Long id) {
        EntityManager em = JpaUtil.getEmf().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Worker worker = em.find(Worker.class, id);
            if (worker != null) {
                em.remove(worker);
            }
            tx.commit();
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }
}
