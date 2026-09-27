package ru.maxon.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.maxon.entity.User;
import ru.maxon.exception.DatabaseOperationException;

import java.util.List;
import java.util.function.Function;

public class UserDao {
    private static final Logger log = LoggerFactory.getLogger(UserDao.class);
    private final SessionFactory sessionFactory;

    public UserDao(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public User create(User user) {
        return inTransaction("create user", session -> {
            session.persist(user);
            session.flush();
            return user;
        });
    }

    public User findById(long id) {
        try (Session session = sessionFactory.openSession()) {
            return session.get(User.class, id);
        } catch (RuntimeException e) {
            log.error("Не удалось прочитать пользователя id={}", id, e);
            throw new DatabaseOperationException("Ошибка чтения пользователя", e);
        }
    }

    public List<User> findAll() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("from User u order by u.id", User.class)
                    .getResultList();
        } catch (RuntimeException e) {
            log.error("Не удалось получить список пользователей", e);
            throw new DatabaseOperationException("Ошибка чтения пользователей", e);
        }
    }

    public boolean update(long id, String name, String email, int age) {
        return inTransaction("update user", session -> {
            User user = session.get(User.class, id);
            if (user == null) {
                return false;
            }
            user.setName(name);
            user.setEmail(email);
            user.setAge(age);
            return true;
        });
    }

    public boolean delete(long id) {
        return inTransaction("delete user", session -> {
            User user = session.get(User.class, id);
            if (user == null) {
                return false;
            }
            session.remove(user);
            return true;
        });
    }

    private <T> T inTransaction(String operation, Function<Session, T> action) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            T result = action.apply(session);
            transaction.commit();
            return result;
        } catch (ConstraintViolationException e) {
            rollback(transaction);
            log.warn("Нарушено ограничение БД при операции: {}", operation, e);
            throw new DatabaseOperationException(
                    "Нарушено ограничение базы данных (например, email уже используется)", e);
        } catch (RuntimeException e) {
            rollback(transaction);
            log.error("Ошибка Hibernate/PostgreSQL при операции: {}", operation, e);
            throw new DatabaseOperationException("Не удалось выполнить операцию: " + operation, e);
        }
    }

    private void rollback(Transaction transaction) {
        if (transaction != null && transaction.isActive()) {
            try {
                transaction.rollback();
            } catch (RuntimeException rollbackError) {
                log.error("Не удалось откатить транзакцию", rollbackError);
            }
        }
    }
}

