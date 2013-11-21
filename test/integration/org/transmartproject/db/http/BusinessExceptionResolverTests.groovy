package org.transmartproject.db.http

import org.junit.Test
import org.springframework.context.ApplicationContext
import org.springframework.web.servlet.HandlerExceptionResolver

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class BusinessExceptionResolverTests {

    def grailsApplication

    @Test
    void testExceptionResolverWasReplaced() {
        ApplicationContext ctx = grailsApplication.mainContext
        def beans = ctx.getBeansOfType(HandlerExceptionResolver)

        assertThat beans, hasEntry(equalTo('businessExceptionResolver'),
                any(BusinessExceptionResolver))
    }
}
