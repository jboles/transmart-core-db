package org.transmartproject.db

import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class TransmartCoreGrailsPluginTests {

    @Test
    void testStringAsLikeLiteral() {
        assertThat ''.respondsTo('asLikeLiteral'),
                hasSize(greaterThanOrEqualTo(1))

        def data = [
                ''            : '',
                'foo'         : 'foo',
                '\\'          : '\\\\',
                '%'           : '\\%',
                '_'           : '\\_',
                '\\%'         : '\\\\\\%',
                'f%\\_oo\\\\' : 'f\\%\\\\\\_oo\\\\\\\\',
        ]

        data.each { String input, String expected ->
            assertThat input.asLikeLiteral(), is(equalTo(expected))
        }
    }
}
