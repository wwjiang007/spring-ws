/*
 * Copyright 2006 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ws.transport.http;

import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ws.FaultAwareWebServiceMessage;
import org.springframework.ws.NoEndpointFoundException;
import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.transport.WebServiceMessageReceiver;

public class WebServiceMessageReceiverHandlerAdapterTest extends TestCase {

    private static final String REQUEST = " <SOAP-ENV:Envelope\n" +
            "  xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "  SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" + "   <SOAP-ENV:Body>\n" +
            "       <m:GetLastTradePrice xmlns:m=\"Some-URI\">\n" + "           <symbol>DIS</symbol>\n" +
            "       </m:GetLastTradePrice>\n" + "   </SOAP-ENV:Body>\n" + "</SOAP-ENV:Envelope>";

    private WebServiceMessageReceiverHandlerAdapter adapter;

    private MockHttpServletRequest httpRequest;

    private MockHttpServletResponse httpResponse;

    private MockControl factoryControl;

    private WebServiceMessageFactory factoryMock;

    private MockControl messageControl;

    private FaultAwareWebServiceMessage responseMock;

    private FaultAwareWebServiceMessage requestMock;

    protected void setUp() throws Exception {
        adapter = new WebServiceMessageReceiverHandlerAdapter();
        httpRequest = new MockHttpServletRequest();
        httpResponse = new MockHttpServletResponse();
        factoryControl = MockControl.createControl(WebServiceMessageFactory.class);
        factoryMock = (WebServiceMessageFactory) factoryControl.getMock();
        adapter.setMessageFactory(factoryMock);
        messageControl = MockControl.createControl(FaultAwareWebServiceMessage.class);
        requestMock = (FaultAwareWebServiceMessage) messageControl.getMock();
        responseMock = (FaultAwareWebServiceMessage) messageControl.getMock();
    }

    public void testHandleNonPost() throws Exception {
        httpRequest.setMethod(HttpTransportConstants.METHOD_GET);
        replayMockControls();
        WebServiceMessageReceiver endpoint = new WebServiceMessageReceiver() {

            public void receive(MessageContext messageContext) throws Exception {
            }
        };
        adapter.handle(httpRequest, httpResponse, endpoint);
        assertEquals("METHOD_NOT_ALLOWED expected", HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                httpResponse.getStatus());
        verifyMockControls();
    }

    public void testHandlePostNoResponse() throws Exception {
        httpRequest.setMethod(HttpTransportConstants.METHOD_POST);
        httpRequest.setContent(REQUEST.getBytes("UTF-8"));
        httpRequest.setContentType("text/xml; charset=\"utf-8\"");
        httpRequest.setCharacterEncoding("UTF-8");
        factoryMock.createWebServiceMessage(null);
        factoryControl.setMatcher(MockControl.ALWAYS_MATCHER);
        factoryControl.setReturnValue(responseMock);

        replayMockControls();
        WebServiceMessageReceiver endpoint = new WebServiceMessageReceiver() {

            public void receive(MessageContext messageContext) throws Exception {
            }
        };

        adapter.handle(httpRequest, httpResponse, endpoint);

        assertEquals("Invalid status code on response", HttpServletResponse.SC_ACCEPTED, httpResponse.getStatus());
        assertEquals("Response written", 0, httpResponse.getContentAsString().length());
        verifyMockControls();
    }

    public void testHandlePostResponse() throws Exception {
        httpRequest.setMethod(HttpTransportConstants.METHOD_POST);
        httpRequest.setContent(REQUEST.getBytes("UTF-8"));
        httpRequest.setContentType("text/xml; charset=\"utf-8\"");
        httpRequest.setCharacterEncoding("UTF-8");
        factoryMock.createWebServiceMessage(null);
        factoryControl.setMatcher(MockControl.ALWAYS_MATCHER);
        factoryControl.setReturnValue(requestMock);
        factoryControl.expectAndReturn(factoryMock.createWebServiceMessage(), responseMock);
        messageControl.expectAndReturn(responseMock.hasFault(), false);
        responseMock.writeTo(null);
        messageControl.setMatcher(MockControl.ALWAYS_MATCHER);

        replayMockControls();
        WebServiceMessageReceiver endpoint = new WebServiceMessageReceiver() {

            public void receive(MessageContext messageContext) throws Exception {
                messageContext.getResponse();
            }
        };

        adapter.handle(httpRequest, httpResponse, endpoint);

        assertEquals("Invalid status code on response", HttpServletResponse.SC_OK, httpResponse.getStatus());
        verifyMockControls();
    }

    public void testHandlePostFault() throws Exception {
        httpRequest.setMethod(HttpTransportConstants.METHOD_POST);
        httpRequest.setContent(REQUEST.getBytes("UTF-8"));
        httpRequest.setContentType("text/xml; charset=\"utf-8\"");
        httpRequest.setCharacterEncoding("UTF-8");
        factoryMock.createWebServiceMessage(null);
        factoryControl.setMatcher(MockControl.ALWAYS_MATCHER);
        factoryControl.setReturnValue(requestMock);
        factoryControl.expectAndReturn(factoryMock.createWebServiceMessage(), responseMock);
        messageControl.expectAndReturn(responseMock.hasFault(), true);
        responseMock.writeTo(null);
        messageControl.setMatcher(MockControl.ALWAYS_MATCHER);

        replayMockControls();
        WebServiceMessageReceiver endpoint = new WebServiceMessageReceiver() {

            public void receive(MessageContext messageContext) throws Exception {
                messageContext.getResponse();
            }
        };

        adapter.handle(httpRequest, httpResponse, endpoint);

        assertEquals("Invalid status code on response", HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                httpResponse.getStatus());
        verifyMockControls();
    }

    public void testHandleNotFound() throws Exception {
        httpRequest.setMethod(HttpTransportConstants.METHOD_POST);
        httpRequest.setContent(REQUEST.getBytes("UTF-8"));
        httpRequest.setContentType("text/xml; charset=\"utf-8\"");
        httpRequest.setCharacterEncoding("UTF-8");
        factoryMock.createWebServiceMessage(null);
        factoryControl.setMatcher(MockControl.ALWAYS_MATCHER);
        factoryControl.setReturnValue(requestMock);

        replayMockControls();

        WebServiceMessageReceiver endpoint = new WebServiceMessageReceiver() {

            public void receive(MessageContext messageContext) throws Exception {
                throw new NoEndpointFoundException(messageContext.getRequest());
            }
        };

        adapter.handle(httpRequest, httpResponse, endpoint);
        assertEquals("No 404 returned", HttpServletResponse.SC_NOT_FOUND, httpResponse.getStatus());

        verifyMockControls();

    }

    private void replayMockControls() {
        factoryControl.replay();
        messageControl.replay();
    }

    private void verifyMockControls() {
        factoryControl.verify();
        messageControl.verify();
    }

}
