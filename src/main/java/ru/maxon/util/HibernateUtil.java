package ru.maxon.util;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.maxon.entity.User;

public final class HibernateUtil {
    private static final Logger log = LoggerFactory.getLogger(HibernateUtil.class);
    private static SessionFactory sessionFactory;

    private HibernateUtil() {
    }

    public static synchronized SessionFactory getSessionFactory() {
        if (sessionFactory == null || sessionFactory.isClosed()) {
            Configuration configuration = new Configuration()
                    .configure("hibernate.cfg.xml")
                    .addAnnotatedClass(User.class);

            setIfPresent(configuration, "hibernate.connection.url", "test.db.url");
            setIfPresent(configuration, "hibernate.connection.username", "test.db.username");
            setIfPresent(configuration, "hibernate.connection.password", "test.db.password");
            setIfPresent(configuration, "hibernate.hbm2ddl.auto", "test.db.schema");

            try {
                sessionFactory = configuration.buildSessionFactory();
            } catch (Exception e) {
                log.error("Не удалось создать Hibernate SessionFactory", e);
                throw new ExceptionInInitializerError(e);
            }
        }
        return sessionFactory;
    }

    private static void setIfPresent(Configuration configuration, String hibernateKey, String systemKey) {
        String value = System.getProperty(systemKey);
        if (value != null && !value.isBlank()) {
            configuration.setProperty(hibernateKey, value);
        }
    }

    public static synchronized void shutdown() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
        }
        sessionFactory = null;
    }
}
