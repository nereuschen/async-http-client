/*
 * Copyright 2010 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.ning.http.client.async;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.testng.annotations.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

import static org.testng.Assert.*;

public abstract class RetryRequestTest extends AbstractBasicTest {
    public static class SlowAndBigHandler extends AbstractHandler {

        public void handle(String pathInContext, Request request,
                           HttpServletRequest httpRequest, HttpServletResponse httpResponse)
                throws IOException, ServletException {

            int load = 100;
            httpResponse.setStatus(200);
            httpResponse.setContentLength(load);
            httpResponse.setContentType("application/octet-stream");

            httpResponse.flushBuffer();


            OutputStream os = httpResponse.getOutputStream();
            for (int i = 0; i < load; i++) {
                os.write(i % 255);

                try {
                    Thread.sleep(300);
                } catch (InterruptedException ex) {
                    // nuku
                }


                if (i > load / 10) {
                    httpResponse.sendError(500);
                }
            }

            httpResponse.getOutputStream().flush();
            httpResponse.getOutputStream().close();
        }
    }

    protected String getTargetUrl() {
        return String.format("http://127.0.0.1:%d/", port1);
    }

    @Override
    public AbstractHandler configureHandler() throws Exception {
        return new SlowAndBigHandler();
    }


    @Test(groups = {"standalone", "default_provider"})
    public void testMaxRetry() throws Throwable {
        AsyncHttpClient ahc = getAsyncHttpClient(new AsyncHttpClientConfig.Builder().setMaxRequestRetry(0).build());
        try {
            ahc.executeRequest(ahc.prepareGet(getTargetUrl()).build()).get();
            fail();
        } catch (Exception t) {
            assertNotNull(t.getCause());
            assertEquals(t.getCause().getClass(), IOException.class);
            assertEquals(t.getCause().getMessage(), "Remotely Closed");
        }

        ahc.close();
    }
}
