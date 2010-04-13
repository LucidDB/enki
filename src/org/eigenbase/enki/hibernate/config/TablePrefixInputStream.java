/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2009 The Eigenbase Project
// Copyright (C) 2009 SQLstream, Inc.
// Copyright (C) 2009 Dynamo BI Corporation
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation; either version 2.1 of the License, or (at
// your option) any later version.
// 
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
// 
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
*/
package org.eigenbase.enki.hibernate.config;

import java.io.*;

import org.eigenbase.enki.hibernate.codegen.*;

/**
 * @author Stephan Zuercher
 */
public class TablePrefixInputStream extends InputStream
{
    public static final int BASE_BUFFER_SIZE = 8192;
    
    private static final byte[] REF;
    static {
        try {
            REF = HibernateMappingHandler.TABLE_REF.getBytes("UTF-8");
        } catch(UnsupportedEncodingException e) {
            throw new InternalError("UTF-8 is unsupported");
        }
    }
    
    private final InputStream in;
    private final byte[] tablePrefix;
    
    private final boolean mayGrow;
    
    private byte[] buffer;
    private int pos;
    private int end;
    private int bufferSize;
    
    public TablePrefixInputStream(InputStream in, String tablePrefixInit)
    {
        this.in = in;
        try {
            this.tablePrefix = tablePrefixInit.getBytes("UTF-8");
        } catch(UnsupportedEncodingException e) {
            throw new InternalError("UTF-8 is unsupported");
        }
        
        this.bufferSize = BASE_BUFFER_SIZE;
        this.buffer = new byte[BASE_BUFFER_SIZE];
        this.pos = 0;
        this.end = 0;
        
        this.mayGrow = tablePrefix.length > REF.length;
    }

    @Override
    public int read()
        throws IOException
    {
        if (!checkBuffer()) {
            return -1;
        }
        
        return buffer[pos++];
    }

    @Override
    public int available()
        throws IOException
    {
        int available = end - pos;
        if (available < 0) {
            return 0;
        }
        return available;
    }

    @Override
    public void close() throws IOException
    {
        buffer = null;
        pos = 0;
        end = 0;

        in.close();
    }

    @Override
    public int read(byte[] b, int off, int len)
        throws IOException
    {
        if (!checkBuffer()) {
            return -1;
        }

        if (end - pos >= len) {
            System.arraycopy(buffer, pos, b, off, len);
            pos += len;
            return len;
        }
        
        int read = 0;
        do {
            int remaining = end - pos;
            System.arraycopy(buffer, pos, b, off, remaining);
            read += remaining;
            pos += remaining;
            len -= remaining;
            off += remaining;
            
            if (!checkBuffer()) {
                // ran out of data
                return read;
            }
        } while(end - pos < len);
        
        System.arraycopy(buffer, pos, b, off, len);
        pos += len;
        read += len;
        
        return read;
    }

    @Override
    public int read(byte[] b)
        throws IOException
    {
        return read(b, 0, b.length);
    }
    
    private boolean checkBuffer() throws IOException
    {
        if (pos < end) {
            return true;
        }

        pos = 0;
        
        int num;
        if (end > 0 && end < bufferSize) {
            // Partial REF match at end of buffer, copy it to the start of
            // the buffer.
            int to = 0;
            int from = end;
            while(from < bufferSize) {
                buffer[to++] = buffer[from++];
            }
            
            // Fill up the buffer, if possible
            int read = in.read(buffer, to, buffer.length - to);
            if (read > 0) {
                num = to + read;
            } else {
                num = to;
            }
        } else {
            num = in.read(buffer, 0, buffer.length);
            if (num < 0) {
                end = 0;
                return false;
            }
        }

        // set bufferSize and end to num bytes in buffer; fixRefs may update
        // the end to reflect a partial match that will be handled later
        bufferSize = num;
        end = num;
        
        fixRefs();
        
        return true;
    }
    
    private void fixRefs() throws IOException
    {
        if (mayGrow) {
            ByteArrayOutputStream newBuffer = new ByteArrayOutputStream(end);
            int from = 0;
            int trueEnd = end;
            boolean endedPartialRef = false;
            int nextFrom;
            LOOP_GROW:
            while((nextFrom = findStart(from)) >= 0) {
                if (nextFrom > from) {
                    newBuffer.write(buffer, from, nextFrom - from);
                    from = nextFrom;
                }
                
                MatchType matchType = matchesRef(from);
                switch(matchType) {
                case FULL:
                    newBuffer.write(tablePrefix);
                    from += REF.length;
                    break;
                    
                case PARTIAL:
                    endedPartialRef = true;
                    break LOOP_GROW;
                    
                case NONE:
                    newBuffer.write(buffer, from, 1);
                    from++;
                    break;
                }
            }
            
            if (endedPartialRef) {
                end = newBuffer.size();
            }
            
            // copy remainder, including any partial refs
            if (from < trueEnd) {
                newBuffer.write(buffer, from, trueEnd - from);
            }
            
            if (!endedPartialRef) {
                end = newBuffer.size();
                
            }
            bufferSize = newBuffer.size();
            
            if (bufferSize > buffer.length) {
                buffer = newBuffer.toByteArray();
            } else {
                System.arraycopy(
                    newBuffer.toByteArray(), 0, buffer, 0, newBuffer.size());
            }
        } else {
            int to = 0;
            int from = 0;
            int trueEnd = end;
            boolean endedPartialRef = false;
            int nextFrom;
            LOOP_SHRINK:
            while((nextFrom = findStart(from)) >= 0) {
                if (nextFrom > from) {
                    if (from != to) {
                        while(from < nextFrom) {
                            buffer[to++] = buffer[from++];
                        }
                    } else {
                        to += (nextFrom - from);
                        from = nextFrom;
                    }
                }
                
                MatchType matchType = matchesRef(from);
                switch(matchType) {
                case FULL:
                    for(int i = 0, n = tablePrefix.length; i < n; i++) {
                        buffer[to++] = tablePrefix[i];
                    }
                    from += REF.length;
                    break;
                    
                case PARTIAL:
                    // Set end to start of partial match
                    endedPartialRef = true;
                    break LOOP_SHRINK;
                    
                case NONE:
                    // Copy one character
                    if (from != to) {
                        buffer[to] = buffer[from];
                    }
                    to++;
                    from++;
                    break;
                }
            }
            
            if (endedPartialRef) {
                end = to;
            }
            
            // copy remainder, including any partial refs
            if (to != from) {
                while(from < trueEnd) {
                    buffer[to++] = buffer[from++];
                }
            } else {
                to = trueEnd;
            }
            
            if (!endedPartialRef) {
                end = to;
            }
            bufferSize = to;
        }
    }
    
    private int findStart(int begin)
    {
        byte s = REF[0];
        for(int i = begin, n = end; i < n; i++) {
            if (buffer[i] == s) {
                return i;
            }
        }
        return -1;
    }
    
    private MatchType matchesRef(int begin)
    {
        int finish = Math.min(begin + REF.length, end);
        int count = 0;
        for(int i = begin, j = 0; i < finish; i++, j++) {
            if (buffer[i] != REF[j]) {
                return MatchType.NONE;
            }
            count++;
        }
        
        if (count == REF.length) {
            return MatchType.FULL;
        }
        
        return MatchType.PARTIAL;
    }
    
    private static enum MatchType
    {
        FULL,
        PARTIAL,
        NONE;
    }
}

// End TablePrefixInputStream.java
