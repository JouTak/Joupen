package org.joutak.loginpluginforjoutak.utils;

import jakarta.persistence.EntityManager;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Утилиты для работы с транзакциями в тестах плагина.
 */
public class DBRepositoryUtils {

    /**
     * Выполняет операцию внутри транзакции и возвращает результат.
     *
     * @param em EntityManager
     * @param action Функция, принимающая EntityManager и возвращающая результат
     * @param <T> Тип результата
     * @return Результат выполнения функции
     */
    public static <T> T runInTransaction(EntityManager em, Function<EntityManager, T> action) {
        try {
            em.getTransaction().begin();
            T result = action.apply(em);
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Ошибка при выполнении транзакции", e);
        } finally {
            em.close();
        }
    }

    /**
     * Выполняет операцию внутри транзакции без возвращаемого результата.
     *
     * @param em EntityManager
     * @param action Потребитель, принимающий EntityManager
     */
    public static void runInTransactionVoid(EntityManager em, Consumer<EntityManager> action) {
        try {
            em.getTransaction().begin();
            action.accept(em);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Ошибка при выполнении транзакции", e);
        } finally {
            em.close();
        }
    }

    // Приватный конструктор чтобы запретить создание экземпляров
    private DBRepositoryUtils() {
    }
}
