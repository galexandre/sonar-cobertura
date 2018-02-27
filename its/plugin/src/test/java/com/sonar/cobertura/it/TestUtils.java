/*
 * Cobertura :: Integration Tests
 * Copyright (C) 2018 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package com.sonar.cobertura.it;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.container.Server;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;

import javax.annotation.Nullable;

public class TestUtils {

    /**
     * @deprecated replaced by {@link Tester#wsClient()}
     */
    public static WsClient newWsClient(Orchestrator orchestrator) {
        return newUserWsClient(orchestrator, null, null);
    }

    /**
     * @deprecated replaced by {@link Tester#wsClient()}
     */
    public static WsClient newUserWsClient(Orchestrator orchestrator, @Nullable String login, @Nullable String password) {
        Server server = orchestrator.getServer();
        return WsClientFactories.getDefault().newClient(HttpConnector.newBuilder()
                .url(server.getUrl())
                .credentials(login, password)
                .build());
    }

}
