package org.joutak.loginpluginforjoutak.database;

import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.PersistenceUnitTransactionType;

import javax.sql.DataSource;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class CustomPersistenceUnitInfo implements PersistenceUnitInfo {

    private final String name;              // Имя persistence unit
    private final List<String> managedClassNames; // Список классов сущностей
    private final Properties properties;    // Свойства конфигурации

    // Конструктор для инициализации полей
    public CustomPersistenceUnitInfo(String name, List<String> managedClassNames, Properties properties) {
        this.name = name;
        this.managedClassNames = managedClassNames;
        this.properties = properties != null ? properties : new Properties();
    }

    @Override
    public String getPersistenceUnitName() {
        return name; // Возвращает имя persistence unit, например, "myPU"
    }

    @Override
    public String getPersistenceProviderClassName() {
        return "org.hibernate.jpa.HibernatePersistenceProvider"; // Указываем Hibernate как провайдер
    }

    @Override
    public PersistenceUnitTransactionType getTransactionType() {
        return PersistenceUnitTransactionType.RESOURCE_LOCAL; // Используем локальные транзакции
    }

    @Override
    public DataSource getJtaDataSource() {
        return null; // Не используем JTA, возвращаем null
    }

    @Override
    public DataSource getNonJtaDataSource() {
        return null; // Не используем не-JTA DataSource, настройки переданы через properties
    }

    @Override
    public List<String> getMappingFileNames() {
        return Collections.emptyList(); // Нет дополнительных маппинг-файлов
    }

    @Override
    public List<URL> getJarFileUrls() {
        return Collections.emptyList(); // Нет JAR-файлов с сущностями
    }

    @Override
    public URL getPersistenceUnitRootUrl() {
        return null; // Не используем persistence.xml, возвращаем null
    }

    @Override
    public List<String> getManagedClassNames() {
        return managedClassNames; // Список имён классов сущностей
    }

    @Override
    public boolean excludeUnlistedClasses() {
        return true; // Исключаем не перечисленные классы
    }
//todo мб когда-то понадобится
    @Override
    public SharedCacheMode getSharedCacheMode() {
        return SharedCacheMode.NONE; // Отключаем кэширование
    }

    @Override
    public ValidationMode getValidationMode() {
        return ValidationMode.AUTO; // Автоматическая валидация
    }

    @Override
    public Properties getProperties() {
        return properties; // Свойства конфигурации (URL, пользователь, пароль и т.д.)
    }

    @Override
    public String getPersistenceXMLSchemaVersion() {
        return "2.2"; // Версия схемы persistence.xml для Jakarta EE
    }

    @Override
    public ClassLoader getClassLoader() {
        return Thread.currentThread().getContextClassLoader(); // Текущий класс-лоадер
    }

    @Override
    public void addTransformer(ClassTransformer transformer) {
        // Оставляем пустым, так как трансформаторы классов не требуются
    }

    @Override
    public ClassLoader getNewTempClassLoader() {
        return null; // Не нужен временный класс-лоадер
    }
}