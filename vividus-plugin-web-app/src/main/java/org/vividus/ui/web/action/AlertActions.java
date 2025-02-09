/*
 * Copyright 2019-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vividus.ui.web.action;

import static org.openqa.selenium.support.ui.ExpectedConditions.alertIsPresent;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vividus.selenium.IWebDriverProvider;
import org.vividus.selenium.manager.IWebDriverManager;
import org.vividus.ui.action.IWaitActions;

public class AlertActions implements IAlertActions
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AlertActions.class);

    @Inject private IWebDriverProvider webDriverProvider;
    @Inject private IWaitActions waitActions;
    @Inject private IWebDriverManager webDriverManager;
    @Inject private IWindowsActions windowsActions;
    private Duration waitForAlertTimeout;

    @Override
    public boolean isAlertPresent(WebDriver webDriver)
    {
        return switchToAlert(webDriver).isPresent();
    }

    @Override
    public Optional<Alert> switchToAlert()
    {
        return switchToAlert(webDriverProvider.get());
    }

    private Optional<Alert> switchToAlert(WebDriver webDriver)
    {
        if (webDriverManager.isMobile())
        {
            LOGGER.warn("Skipped alert interaction in mobile context");
            return Optional.empty();
        }
        try
        {
            return Optional.ofNullable(webDriver.switchTo().alert());
        }
        catch (NoAlertPresentException e)
        {
            return Optional.empty();
        }
        catch (NoSuchWindowException e)
        {
            windowsActions.switchToPreviousTab();
            return Optional.empty();
        }
    }

    @Override
    public boolean waitForAlert(WebDriver webDriver)
    {
        return waitActions.wait(webDriver, waitForAlertTimeout, alertIsPresent(), false)
                .isWaitPassed();
    }

    @Override
    public void processAlert(Action action)
    {
        switchToAlert().ifPresent(alert -> processAlert(action, alert));
    }

    @Override
    public void processAlert(Action action, Alert alert)
    {
        action.process(alert, webDriverManager);
    }

    public void setWaitForAlertTimeout(Duration waitForAlertTimeout)
    {
        this.waitForAlertTimeout = waitForAlertTimeout;
    }

    public enum Action
    {
        ACCEPT
        {
            private static final String ANDROID_OK_BUTTON_LOCATOR = "//android.widget.Button[@text='OK']";

            @Override
            void process(Alert alert, IWebDriverManager webDriverManager)
            {
                try
                {
                    alert.accept();
                }
                catch (WebDriverException e)
                {
                    LOGGER.atWarn().addArgument(e::getMessage).log(
                            "Could not accept alert smoothly, trying to perform it in native context: {}");
                    if (webDriverManager.isAndroid())
                    {
                        webDriverManager.performActionInNativeContext(webDriver ->
                        {
                            List<WebElement> buttons = webDriver.findElements(By.xpath(ANDROID_OK_BUTTON_LOCATOR));
                            if (1 == buttons.size())
                            {
                                buttons.get(0).click();
                            }
                        });
                    }
                }
            }
        },
        DISMISS
        {
            @Override
            void process(Alert alert, IWebDriverManager webDriverManager)
            {
                alert.dismiss();
            }
        };

        abstract void process(Alert alert, IWebDriverManager webDriverManager);
    }
}
