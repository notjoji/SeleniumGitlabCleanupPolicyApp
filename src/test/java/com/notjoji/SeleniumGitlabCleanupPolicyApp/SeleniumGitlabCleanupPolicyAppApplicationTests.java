package com.notjoji.SeleniumGitlabCleanupPolicyApp;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.CollectionUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
class SeleniumGitlabCleanupPolicyAppApplicationTests
{

  private static final Logger log = LoggerFactory.getLogger(SeleniumGitlabCleanupPolicyAppApplicationTests.class);
  private static final String[] BASE_TAGS_PATHS = {"-", "settings", "packages_and_registries", "cleanup_image_tags"};

  @Value("${gitlab.baseUrl}")
  String gitlabBaseUrl;

  @Value("${gitlab.group}")
  String gitlabGroup;

  @Value("${gitlab.username}")
  String gitlabUsername;

  @Value("${gitlab.password}")
  String gitlabPassword;

  @Value("${releases.keepVersionTags}")
  String releasesKeepVersionTags;

  @Value("${releases.keepOtherTags}")
  String releasesKeepOtherTags;

  @Value("${modulesPath}")
  String modulesPath;

  @Autowired
  private ResourceLoader resourceLoader;

  WebDriver driver;

  @BeforeEach
  public void setup()
  {
    driver = new ChromeDriver();
    driver.manage().timeouts().implicitlyWait(Duration.ofMillis(500));
  }

  @AfterEach
  public final void teardown()
  {
    if (driver != null) {
      driver.quit();
    }
  }

  @DisplayName("Setting cleanup policy to all projects in GitLab group")
  @Test
  void settingCleanupPolicyTest()
  {
    List<String> modules = new ArrayList<>();
    Resource resource = resourceLoader.getResource(modulesPath);
    try (InputStream is = resource.getInputStream();
         BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
      reader.lines().forEach(modules::add);
    } catch (IOException e) {
      log.error("Произошла ошибка при чтении файла: {}", e.getMessage(), e);
      return;
    }

    if (CollectionUtils.isEmpty(modules)) {
      log.error("Нет модулей для запуска теста");
      return;
    }

    String keepTags = "(?:" + releasesKeepVersionTags + "|" + releasesKeepOtherTags + ")";

    log.info("Авторизация в GitLab...");

    String authorizationPage = UriComponentsBuilder.fromUriString(gitlabBaseUrl)
        .pathSegment("users", "sign_in")
        .toUriString();
    driver.get(authorizationPage);

    WebElement adTab = driver.findElement(By.cssSelector("#gl_tab_nav__tab_1"));
    adTab.click();

    WebElement ldapUsername = driver.findElement(By.cssSelector("#ldapmain_username"));
    ldapUsername.sendKeys(gitlabUsername);

    WebElement ldapPassword = driver.findElement(By.cssSelector("#ldapmain_password"));
    ldapPassword.sendKeys(gitlabPassword);

    WebElement signInBtn = driver.findElement(By.cssSelector("#ldapmain > form > button"));
    signInBtn.click();

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(1));

    for (String module : modules) {
      log.info("Старт обработки модуля {}", module);

      UriComponentsBuilder moduleSettingsPageBuilder = UriComponentsBuilder.fromUriString(gitlabBaseUrl)
          .pathSegment(gitlabGroup, module);
      for (String basePath : BASE_TAGS_PATHS) {
        moduleSettingsPageBuilder.pathSegment(basePath);
      }
      String moduleSettingsPageUrl = moduleSettingsPageBuilder.toUriString();

      log.info(moduleSettingsPageUrl);
      driver.get(moduleSettingsPageUrl);

      WebElement keepTagsTb = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("#keep-regex")));
      keepTagsTb.clear();
      keepTagsTb.sendKeys(keepTags);

      WebElement saveChangesBt = driver.findElement(By.xpath("//*[@id=\"content-body\"]/section/div[3]/form/div[3]/button"));
      saveChangesBt.click();

      // дожидаемся, когда страница перезагрузится и произойдет редирект
      wait.until(ExpectedConditions.not(ExpectedConditions.urlContains("cleanup_image_tags")));

      log.info("Завершение обработки модуля {}", module);
    }
  }
}
