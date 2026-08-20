package com.company.workerportal.dao;

import com.company.workerportal.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

public class UserDAO {

    public User findByUsername(String username) {
        EntityManager em = JpaUtil.getEmf().createEntityManager();
        try {
            return em.createNamedQuery("User.findByUsername", User.class)
                    .setParameter("username", username)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            em.close();
        }
    }
}
