/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jdbc.core.support;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.jdbc.support.lob.LobHandler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Test cases for the SQL LOB value:
 * <p>
 * BLOB:
 * 1. Types.BLOB: setBlobAsBytes (byte[])
 * 2. String: setBlobAsBytes (byte[])
 * 3. else: IllegalArgumentException
 * <p>
 * CLOB:
 * 4. String or NULL: setClobAsString (String)
 * 5. InputStream: setClobAsAsciiStream (InputStream)
 * 6. Reader: setClobAsCharacterStream (Reader)
 * 7. else: IllegalArgumentException
 *
 * @author Alef Arendsen
 */
@RunWith(MockitoJUnitRunner.class)
public class SqlLobValueTests {

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private LobHandler handler;

    @Mock
    private LobCreator creator;

    @Captor
    private ArgumentCaptor<InputStream> inputStreamCaptor;

    @Before
    public void setUp() {
        given(handler.getLobCreator()).willReturn(creator);
    }

    @Test
    public void test1() throws SQLException {
        byte[] testBytes = "Bla".getBytes();
        SqlLobValue lob = new SqlLobValue(testBytes, handler);
        lob.setTypeValue(preparedStatement, 1, Types.BLOB, "test");
        verify(creator).setBlobAsBytes(preparedStatement, 1, testBytes);
    }

    @Test
    public void test2() throws SQLException {
        String testString = "Bla";
        SqlLobValue lob = new SqlLobValue(testString, handler);
        lob.setTypeValue(preparedStatement, 1, Types.BLOB, "test");
        verify(creator).setBlobAsBytes(preparedStatement, 1, testString.getBytes());
    }

    @Test
    public void test3() throws SQLException {
        SqlLobValue lob = new SqlLobValue(new InputStreamReader(new ByteArrayInputStream("Bla".getBytes())), 12);
        assertThatIllegalArgumentException().isThrownBy(() ->
                lob.setTypeValue(preparedStatement, 1, Types.BLOB, "test"));
    }

    @Test
    public void test4() throws SQLException {
        String testContent = "Bla";
        SqlLobValue lob = new SqlLobValue(testContent, handler);
        lob.setTypeValue(preparedStatement, 1, Types.CLOB, "test");
        verify(creator).setClobAsString(preparedStatement, 1, testContent);
    }

    @Test
    public void test5() throws Exception {
        byte[] testContent = "Bla".getBytes();
        SqlLobValue lob = new SqlLobValue(new ByteArrayInputStream(testContent), 3, handler);
        lob.setTypeValue(preparedStatement, 1, Types.CLOB, "test");
        verify(creator).setClobAsAsciiStream(eq(preparedStatement), eq(1), inputStreamCaptor.capture(), eq(3));
        byte[] bytes = new byte[3];
        inputStreamCaptor.getValue().read(bytes);
        assertThat(bytes).isEqualTo(testContent);
    }

    @Test
    public void test6() throws SQLException {
        byte[] testContent = "Bla".getBytes();
        ByteArrayInputStream bais = new ByteArrayInputStream(testContent);
        InputStreamReader reader = new InputStreamReader(bais);
        SqlLobValue lob = new SqlLobValue(reader, 3, handler);
        lob.setTypeValue(preparedStatement, 1, Types.CLOB, "test");
        verify(creator).setClobAsCharacterStream(eq(preparedStatement), eq(1), eq(reader), eq(3));
    }

    @Test
    public void test7() throws SQLException {
        SqlLobValue lob = new SqlLobValue("bla".getBytes());
        assertThatIllegalArgumentException().isThrownBy(() ->
                lob.setTypeValue(preparedStatement, 1, Types.CLOB, "test"));
    }

    @Test
    public void testOtherConstructors() throws SQLException {
        // a bit BS, but we need to test them, as long as they don't throw exceptions

        SqlLobValue lob = new SqlLobValue("bla");
        lob.setTypeValue(preparedStatement, 1, Types.CLOB, "test");

        SqlLobValue lob2 = new SqlLobValue("bla".getBytes());
        assertThatIllegalArgumentException().isThrownBy(() ->
                lob2.setTypeValue(preparedStatement, 1, Types.CLOB, "test"));

        lob = new SqlLobValue(new ByteArrayInputStream("bla".getBytes()), 3);
        lob.setTypeValue(preparedStatement, 1, Types.CLOB, "test");

        lob = new SqlLobValue(new InputStreamReader(new ByteArrayInputStream(
                "bla".getBytes())), 3);
        lob.setTypeValue(preparedStatement, 1, Types.CLOB, "test");

        // same for BLOB
        lob = new SqlLobValue("bla");
        lob.setTypeValue(preparedStatement, 1, Types.BLOB, "test");

        lob = new SqlLobValue("bla".getBytes());
        lob.setTypeValue(preparedStatement, 1, Types.BLOB, "test");

        lob = new SqlLobValue(new ByteArrayInputStream("bla".getBytes()), 3);
        lob.setTypeValue(preparedStatement, 1, Types.BLOB, "test");

        SqlLobValue lob3 = new SqlLobValue(new InputStreamReader(new ByteArrayInputStream(
                "bla".getBytes())), 3);
        assertThatIllegalArgumentException().isThrownBy(() ->
                lob3.setTypeValue(preparedStatement, 1, Types.BLOB, "test"));
    }

    @Test
    public void testCorrectCleanup() throws SQLException {
        SqlLobValue lob = new SqlLobValue("Bla", handler);
        lob.setTypeValue(preparedStatement, 1, Types.CLOB, "test");
        lob.cleanup();
        verify(creator).setClobAsString(preparedStatement, 1, "Bla");
        verify(creator).close();
    }

    @Test
    public void testOtherSqlType() throws SQLException {
        SqlLobValue lob = new SqlLobValue("Bla", handler);
        assertThatIllegalArgumentException().isThrownBy(() ->
                lob.setTypeValue(preparedStatement, 1, Types.SMALLINT, "test"));
    }

}
