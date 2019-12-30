/**
 * Healenium-web Copyright (C) 2019 EPAM
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.healenium.handlers.proxy;

import com.epam.healenium.SelfHealingEngine;
import com.epam.healenium.utils.ProxyFactory;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriver.TargetLocator;
import org.openqa.selenium.WebElement;

@Log4j2
public class SelfHealingProxyInvocationHandler extends BaseHandler {

    public SelfHealingProxyInvocationHandler(SelfHealingEngine engine) {
        super(engine);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        ClassLoader loader = driver.getClass().getClassLoader();
        switch (method.getName()) {
            case "findElement":
                WebElement element = findElement((By) args[0]);
                return Optional.ofNullable(element).map(it -> wrapElement(it, loader)).orElse(null);
            case "getCurrentEngine":
                return engine;
            case "getDelegate":
                return driver;
            case "switchTo":
                clearStash();
                TargetLocator switched = (TargetLocator) method.invoke(driver, args);
                return wrapTarget(switched, loader);
            default:
                return method.invoke(driver, args);
        }
    }

    private WebElement wrapElement(WebElement element, ClassLoader loader) {
        WebElementProxyHandler elementProxyHandler = new WebElementProxyHandler(element, engine);
        return ProxyFactory.createWebElementProxy(loader, elementProxyHandler);
    }

    private TargetLocator wrapTarget(TargetLocator locator, ClassLoader loader) {
        TargetLocatorProxyInvocationHandler handler = new TargetLocatorProxyInvocationHandler(locator, engine);
        return ProxyFactory.createTargetLocatorProxy(loader, handler);
    }

}
