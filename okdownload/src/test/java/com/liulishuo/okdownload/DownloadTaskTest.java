/*
 * Copyright (c) 2017 LingoChamp Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.liulishuo.okdownload;

import android.net.Uri;

import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointStoreOnCache;
import com.liulishuo.okdownload.core.connection.DownloadConnection;
import com.liulishuo.okdownload.core.download.DownloadStrategy;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE)
public class DownloadTaskTest {

    @BeforeClass
    public static void setupClass() throws IOException {
        TestUtils.mockOkDownload();
    }

    @Test
    public void addHeader() throws Exception {
        final String url = "mock url";
        final Uri mockFileUri = mock(Uri.class);
        when(mockFileUri.getPath()).thenReturn("mock path");
        DownloadTask.Builder builder = new DownloadTask.Builder(url, mockFileUri);


        final String mockKey1 = "mock key1";
        final String mockKey2 = "mock key2";
        final String mockValue1 = "mock value1";
        final String mockValue2 = "mock value2";

        builder.addHeader(mockKey1, mockValue1);
        builder.addHeader(mockKey1, mockValue2);

        builder.addHeader(mockKey2, mockValue2);

        final Map<String, List<String>> headerMap = builder.build().getHeaderMapFields();
        assertThat(headerMap).isNotNull();

        assertThat(headerMap).containsKey(mockKey1).containsKey(mockKey2);

        final List<String> key1Values = headerMap.get(mockKey1);
        assertThat(key1Values).containsOnly(mockValue1, mockValue2);

        final List<String> key2Values = headerMap.get(mockKey2);
        assertThat(key2Values).containsOnly(mockValue2);
    }

    private final String parentPath = "./p-path/";
    private final String filename = "filename";

    @Before
    public void setup() throws IOException {
        new File(parentPath).mkdir();
        new File(parentPath, filename).createNewFile();
    }

    @After
    public void tearDown() {
        new File(parentPath, filename).delete();
        new File(parentPath).delete();
    }


    @Test
    public void enqueue() {
        final DownloadTask[] tasks = new DownloadTask[2];
        tasks[0] = new DownloadTask.Builder("url1", "path", "filename1").build();
        tasks[1] = new DownloadTask.Builder("url2", "path", "filename1").build();

        final DownloadListener listener = mock(DownloadListener.class);
        DownloadTask.enqueue(tasks, listener);

        assertThat(tasks[0].getListener()).isEqualTo(listener);
        assertThat(tasks[1].getListener()).isEqualTo(listener);

        verify(OkDownload.with().downloadDispatcher()).enqueue(eq(tasks));
    }

    @Test
    public void equal() throws IOException {
        // for id
        when(OkDownload.with().breakpointStore()).thenReturn(spy(new BreakpointStoreOnCache()));

        final Uri uri = mock(Uri.class);
        when(uri.getPath()).thenReturn(parentPath);

        // origin is:
        // 1. uri is directory
        // 2. filename is provided
        DownloadTask task = new DownloadTask
                .Builder("url", uri)
                .setFilename(filename)
                .build();

        // compare to:
        // 1. uri is not directory
        // 2. filename is provided by uri.
        final Uri anotherUri = mock(Uri.class);
        when(anotherUri.getPath()).thenReturn(parentPath + filename);
        DownloadTask anotherTask = new DownloadTask
                .Builder("url", anotherUri)
                .build();
        assertThat(task.equals(anotherTask)).isTrue();

        // compare to:
        // 1. uri is directory
        // 2. filename is not provided
        anotherTask = new DownloadTask
                .Builder("url", uri)
                .build();
        // expect: not same
        assertThat(task.equals(anotherTask)).isFalse();


        // compare to:
        // 1. uri is directory
        // 2. filename is provided and different
        anotherTask = new DownloadTask
                .Builder("url", uri)
                .setFilename("another-filename")
                .build();
        // expect: not same
        assertThat(task.equals(anotherTask)).isFalse();

        // origin is:
        // 1. uri is directory
        // 2. filename is not provided
        DownloadTask noFilenameTask = new DownloadTask
                .Builder("url", uri)
                .build();

        // filename is enabled by response
        final BreakpointInfo info = mock(BreakpointInfo.class);
        when(info.getFilenameHolder()).thenReturn(mock(DownloadStrategy.FilenameHolder.class));
        new DownloadStrategy().validFilenameFromResponse("response-filename",
                noFilenameTask, info, mock(DownloadConnection.Connected.class));

        // compare to:
        // 1. uri is directory
        // 2. filename is provided
        anotherTask = new DownloadTask
                .Builder("url", uri)
                .setFilename("another-filename")
                .build();
        assertThat(noFilenameTask.equals(anotherTask)).isFalse();

        // compare to:
        // 1. uri is directory
        // 2. filename is not provided
        anotherTask = new DownloadTask
                .Builder("url", uri)
                .build();

        assertThat(noFilenameTask.equals(anotherTask)).isTrue();

        // compare to:
        // 1. uri is directory
        // 2. filename is provided and the same to the response-filename
        anotherTask = new DownloadTask
                .Builder("url", uri)
                .setFilename("response-filename")
                .build();
        assertThat(noFilenameTask.equals(anotherTask)).isTrue();
    }
}