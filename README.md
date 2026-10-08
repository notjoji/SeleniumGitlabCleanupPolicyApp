# Selenium GitLab Cleanup Policy App

Проект для автоматизации процесса проставления тегов (cleanup policy keep tags) во всех проектах группы GitLab.

## Примечания

- Используется AD-авторизация без 2FA, требуется передавать переменные окружения через args/создать локальный .env
- При ошибке во время выполнения теста дальнейшие изменения не срабатывают

## Стек технологий

- Java 17
- Maven
- Spring Boot 4.1.1
- Selenium 4.50.0

## Использование

- Клонирование репозитория
```bash
git clone https://github.com/notjoji/SeleniumGitlabCleanupPolicyApp <path-to-folder>
cd <path-to-folder>
```

- Добавление .env в корень проекта с переменными GITLAB_USERNAME и GITLAB_PASSWORD
- Запуск автоматизации
```bash
mvn clean test
```