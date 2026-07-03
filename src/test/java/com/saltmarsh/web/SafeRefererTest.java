package com.saltmarsh.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SafeRefererTest {

    @Test
    void allowsSimpleRelativePath() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Referer", "http://localhost:8080/reservations/1");
        assertEquals("/reservations/1", GlobalControllerAdvice.safeReferer(request));
    }

    @Test
    void rejectsProtocolRelativePath() {
        // URI path for http://evil.test//evil.test/phish can be //evil...
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Referer", "http://localhost:8080//evil.test/phish");
        assertEquals("/", GlobalControllerAdvice.safeReferer(request));
    }

    @Test
    void rejectsMissingReferer() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertEquals("/", GlobalControllerAdvice.safeReferer(request));
    }

    @Test
    void rejectsBackslashPath() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Referer", "http://localhost:8080/foo\\bar");
        assertEquals("/", GlobalControllerAdvice.safeReferer(request));
    }
}
