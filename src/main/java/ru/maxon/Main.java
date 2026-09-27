package ru.maxon;

import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.maxon.dao.UserDao;
import ru.maxon.entity.User;
import ru.maxon.exception.DatabaseOperationException;
import ru.maxon.util.HibernateUtil;

import java.util.List;
import java.util.Scanner;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        SessionFactory sessionFactory;
        try {
            sessionFactory = HibernateUtil.getSessionFactory();
        } catch (Throwable e) {
            System.err.println("Не удалось запустить Hibernate. Проверьте hibernate.cfg.xml и доступность PostgreSQL.");
            return;
        }

        UserDao userDao = new UserDao(sessionFactory);
        try (Scanner scanner = new Scanner(System.in)) {
            runMenu(scanner, userDao);
        } finally {
            HibernateUtil.shutdown();
        }
    }

    private static void runMenu(Scanner scanner, UserDao dao) {
        while (true) {
            System.out.println("\n=== User Service ===");
            System.out.println("1. Создать пользователя");
            System.out.println("2. Найти пользователя по ID");
            System.out.println("3. Показать всех пользователей");
            System.out.println("4. Обновить пользователя");
            System.out.println("5. Удалить пользователя");
            System.out.println("0. Выход");
            System.out.print("Выберите действие: ");

            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> create(scanner, dao);
                    case "2" -> find(scanner, dao);
                    case "3" -> list(dao);
                    case "4" -> update(scanner, dao);
                    case "5" -> delete(scanner, dao);
                    case "0" -> { return; }
                    default -> System.out.println("Неизвестный пункт меню.");
                }
            } catch (DatabaseOperationException e) {
                System.out.println("Ошибка БД: " + e.getMessage());
                log.error("Операция завершилась ошибкой", e);
            } catch (IllegalArgumentException e) {
                System.out.println("Некорректные данные: " + e.getMessage());
            }
        }
    }

    private static void create(Scanner scanner, UserDao dao) {
        String name = readRequired(scanner, "Имя: ");
        String email = readRequired(scanner, "Email: ");
        int age = readAge(scanner);
        User user = dao.create(new User(name, email, age));
        System.out.println("Создано: " + user);
    }

    private static void find(Scanner scanner, UserDao dao) {
        long id = readLong(scanner, "ID: ");
        User user = dao.findById(id);
        System.out.println(user == null ? "Пользователь не найден." : user);
    }

    private static void list(UserDao dao) {
        List<User> users = dao.findAll();
        if (users.isEmpty()) {
            System.out.println("Пользователей пока нет.");
            return;
        }
        users.forEach(System.out::println);
    }

    private static void update(Scanner scanner, UserDao dao) {
        long id = readLong(scanner, "ID пользователя: ");
        String name = readRequired(scanner, "Новое имя: ");
        String email = readRequired(scanner, "Новый email: ");
        int age = readAge(scanner);
        System.out.println(dao.update(id, name, email, age)
                ? "Пользователь обновлён."
                : "Пользователь не найден.");
    }

    private static void delete(Scanner scanner, UserDao dao) {
        long id = readLong(scanner, "ID пользователя: ");
        System.out.println(dao.delete(id)
                ? "Пользователь удалён."
                : "Пользователь не найден.");
    }

    private static String readRequired(Scanner scanner, String prompt) {
        System.out.print(prompt);
        String value = scanner.nextLine().trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("поле не должно быть пустым");
        }
        return value;
    }

    private static int readAge(Scanner scanner) {
        int age = (int) readLong(scanner, "Возраст: ");
        if (age < 0 || age > 150) {
            throw new IllegalArgumentException("возраст должен быть от 0 до 150");
        }
        return age;
    }

    private static long readLong(Scanner scanner, String prompt) {
        System.out.print(prompt);
        try {
            return Long.parseLong(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ожидалось целое число");
        }
    }
}
