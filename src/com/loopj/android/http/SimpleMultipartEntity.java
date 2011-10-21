/*
    Android Asynchronous Http Client
    Copyright (c) 2011 James Smith <james@loopj.com>
    http://loopj.com

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

/*
    This code is taken from Rafael Sanches' blog.
    http://blog.rafaelsanches.com/2011/01/29/upload-using-multipart-post-using-httpclient-in-android/
*/

package com.loopj.android.http;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.message.BasicHeader;

import android.util.Log;

class SimpleMultipartEntity implements HttpEntity {
    private final static char[] MULTIPART_CHARS = "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    private static final String TAG = SimpleMultipartEntity.class.getSimpleName();

    private String boundary = null;

    private List<Object> partStreams = new ArrayList<Object>();
    private boolean isSetLast = false;
    private boolean isSetFirst = false;

    public SimpleMultipartEntity() {
        partStreams.add(new ByteArrayOutputStream());
        final StringBuffer buf = new StringBuffer();
        final Random rand = new Random();
        for (int i = 0; i < 30; i++) {
            buf.append(MULTIPART_CHARS[rand.nextInt(MULTIPART_CHARS.length)]);
        }
        this.boundary = buf.toString();

    }
    
    private ByteArrayOutputStream getOut() {
        Object obj = partStreams.get(partStreams.size()-1);
        if(obj instanceof ByteArrayOutputStream) {
            return (ByteArrayOutputStream) obj;
        } else {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            partStreams.add(out);
            return out;
        }
    }

    public void writeFirstBoundaryIfNeeds(){
        if(!isSetFirst){
            try {
                getOut().write(("--" + boundary + "\r\n").getBytes());
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }

        isSetFirst = true;
    }

    public void writeLastBoundaryIfNeeds() {
        if(isSetLast){
            return;
        }

        try {
            getOut().write(("\r\n--" + boundary + "--\r\n").getBytes());
        } catch (final IOException e) {
            e.printStackTrace();
        }

        isSetLast = true;
    }

    public void addPart(final String key, final String value) {
        writeFirstBoundaryIfNeeds();
        try {
            ByteArrayOutputStream out = getOut();
            out.write(("Content-Disposition: form-data; name=\"" +key+"\"\r\n\r\n").getBytes());
            out.write(value.getBytes());
            out.write(("\r\n--" + boundary + "\r\n").getBytes());
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public void addPart(final String key, final String fileName, final InputStream fin){
        addPart(key, fileName, fin, "application/octet-stream");
    }

    public void addPart(final String key, final String fileName, final InputStream fin, String type){
        writeFirstBoundaryIfNeeds();
        try {
            ByteArrayOutputStream out = getOut();
            type = "Content-Type: "+type+"\r\n";
            out.write(("Content-Disposition: form-data; name=\""+ key+"\"; filename=\"" + fileName + "\"\r\n").getBytes());
            out.write(type.getBytes());
            out.write("Content-Transfer-Encoding: binary\r\n\r\n".getBytes());
            
            partStreams.add(fin);
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public void addPart(final String key, final File value) {
        try {
            addPart(key, value.getName(), new FileInputStream(value));
        } catch (final FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public long getContentLength() {
        writeLastBoundaryIfNeeds();
        for (Object obj : partStreams) {
            if(obj instanceof InputStream) return -1;
        }
        return getOut().toByteArray().length;
    }

    public Header getContentType() {
        return new BasicHeader("Content-Type", "multipart/form-data; boundary=" + boundary);
    }

    public boolean isChunked() {
        return false;
    }

    public boolean isRepeatable() {
        return false;
    }

    public boolean isStreaming() {
        return false;
    }

    public void writeTo(final OutputStream outstream) throws IOException {
        Log.i(TAG, "writeTo: Starting");
        final byte[] tmp = new byte[4096];
        
        for (Object obj : partStreams) {
            if(obj instanceof ByteArrayOutputStream) {
                ByteArrayOutputStream out = (ByteArrayOutputStream) obj;
                outstream.write(out.toByteArray());
            } else if(obj instanceof FileInputStream) {
                FileInputStream in = (FileInputStream) obj;
                try {
                    int l = 0;
                    while ((l = in.read(tmp)) != -1) {
                        outstream.write(tmp, 0, l);
                    }
                    in.close();
                } catch (IOException e) {
                    Log.e(TAG, "writeTo: Caught IOException: " + e.getMessage());
                }
            } else {
                Log.i(TAG, "writeTo: Failing because we got a: " + obj.getClass().getName() + " toString: " + obj.toString());
                throw new RuntimeException("SimpleMultipartEntity.writeTo does not know how to handle a part of type: " + obj.getClass().getName());
            }
        }
        outstream.flush();
        Log.i(TAG, "writeTo: Finished");
    }

    public Header getContentEncoding() {
        return null;
    }

    public void consumeContent() throws IOException,
    UnsupportedOperationException {
        Log.i(TAG, "consumeContent not implemented...");
        if (isStreaming()) {
            throw new UnsupportedOperationException(
            "Streaming entity does not implement #consumeContent()");
        }
    }

    public InputStream getContent() throws IOException,
    UnsupportedOperationException {
        Log.i(TAG, "getContent not implemented...");
        throw new UnsupportedOperationException("getContent not implemented!");
//        return new ByteArrayInputStream(out.toByteArray());
    }
}