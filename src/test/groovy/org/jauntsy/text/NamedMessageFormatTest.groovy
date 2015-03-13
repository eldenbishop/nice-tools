package org.jauntsy.text

import java.text.MessageFormat

/**
 * Created by ebishop on 3/12/15.
 */
class NamedMessageFormatTest extends GroovyTestCase {

    void testSimple() {
        assertEquals('Hello Joe', format('Hello {name}', [name:'Joe']))
    }

    void testChoice() {
        def legacyFmt = 'There {0,choice,0#are no files|1#is one file|1<are {0,number,integer} files}.'
        assertEquals('There are no files.', MessageFormat.format(legacyFmt, 0))
        assertEquals('There is one file.', MessageFormat.format(legacyFmt, 1))
        assertEquals('There are 2 files.', MessageFormat.format(legacyFmt, 2))

        def namedFormat = "There {numFiles,choice,0#are no files|1#is one file|1<are {numFiles,number,integer} files}."
        assertEquals('There are no files.', format(namedFormat, [numFiles:0]))
        assertEquals('There is one file.', format(namedFormat, [numFiles:1]))
        assertEquals('There are 2 files.', format(namedFormat, [numFiles:2]))
    }

    void testNested() {
        assertEquals('Hello Joe', format('Hello {user.name}', [user:[name:'Joe',age:27]]))
    }

    void testFormatting() {
        assertEquals('items: a, b, c', format('items: {items}', [items:['a','b','c']]))
        assertEquals('items: a, b, [c, d, e]', format('items: {items}', [items:['a','b',['c','d','e']]]))
        assertEquals('joe: [age: 27, name: [first: Joe, last: Smith]]', format('joe: {joe}', [joe:[name:[first:"Joe",last:"Smith"],age:27]]))
    }

    void testEscape() {
        assertEquals("Hello", NamedMessageFormat.escapeSimpleMessage("Hello"))
        assertEquals("Hello '{'name:''Joe'''}'", NamedMessageFormat.escapeSimpleMessage("Hello {name:'Joe'}"))
        assertEquals("Hello '{{{'world'}}}'", NamedMessageFormat.escapeSimpleMessage("Hello {{{world}}}"))
        assertEquals("Hello '{{{'''world'''}}}'", NamedMessageFormat.escapeSimpleMessage("Hello {{{'world'}}}"))
        assertEquals("Hello '{{''{''{{'''world'''}}''}''}}'", NamedMessageFormat.escapeSimpleMessage("Hello {{'{'{{'world'}}'}'}}"))
        def complicated = "If \"id\" and \"from\" are not specified in the envelope, the root pipeline element must be a load operation of the form {\"load\":{\"id\":<processor-name>,\"from\":<stream-name>[,\"by\":<optional-grouping-columns>]}}"
        def expected = "If \"id\" and \"from\" are not specified in the envelope, the root pipeline element must be a load operation of the form '{'\"load\":'{'\"id\":<processor-name>,\"from\":<stream-name>[,\"by\":<optional-grouping-columns>]'}}'"
        def actual = NamedMessageFormat.escapeSimpleMessage(complicated)
        assertEquals(expected, actual)
        assertEquals(
                complicated,
                NamedMessageFormat.format(actual, [:])
        )
    }

    void testConvert() {
        assertEquals("Hello", NamedMessageFormat.convert("Hello",[]))
        assertEquals("Hello {0}", NamedMessageFormat.convert("Hello {name}",["name"]))
        assertEquals("Hello ''{0}''", NamedMessageFormat.convert("Hello ''{name}''",["name"]))
        assertEquals("Hello '{'{0}'}'", NamedMessageFormat.convert("Hello '{'{name}'}'",["name"]))
        assertEquals("Hello '''{'{0}'}'''", NamedMessageFormat.convert("Hello '''{'{name}'}'''",["name"]))
        assertEquals("Hello '{''{'{0}'}''}'", NamedMessageFormat.convert("Hello '{''{'{name}'}''}'",["name"]))
    }

    void testConvertRetainsSingleQuotes() {
        // *** this test for an error encountered when formatting a complex message which included json snippets and the offending {}' symbols
        def raw = "If \"id\" and \"from\" are not specified in the envelope, the root pipeline element must be a load operation of the form '{'\"load\":'{'\"id\":<processor-name>,\"from\":<stream-name>[,\"by\":<optional-grouping-columns>]'}''}'"
        assertEquals(raw, NamedMessageFormat.convert(raw, []))
    }

    void testThatOneBug() {
        assertEquals("{'{foo}'}", MessageFormat.format("'{''{'foo'}''}'", [] as Object[]))
        def message = 'If "id" and "from" are not specified in the envelope, the root pipeline element must be a load operation of the form {"load":{"id":<processor-name>,"from":<stream-name>[,"by":<optional-grouping-columns>]}}'
        assertEquals(message, NamedMessageFormat.format(NamedMessageFormat.escapeSimpleMessage(message), [:]))
    }

    private String format(String pattern, Map pams) {
        return NamedMessageFormat.format(pattern, pams);
    }

}
