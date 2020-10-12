package org.codehaus.plexus.archiver;

import org.codehaus.plexus.logging.AbstractLogger;
import org.codehaus.plexus.logging.Logger;

import java.util.ArrayList;
import java.util.List;

public class CapturingLog extends AbstractLogger
{
    public CapturingLog( int threshold, String name )
    {
        super( threshold, name );
    }

    static class Message {
        public final String message;
        public final Throwable throwable;

        public Message( String message, Throwable throwable )
        {
            this.message = message;
            this.throwable = throwable;
        }

        @Override
        public String toString()
        {
            return "Message{" + "message='" + message + '\'' + ", throwable=" + throwable + '}';
        }
    }

    private final List<Message> debugs = new ArrayList<>();
    @Override
    public void debug( String s, Throwable throwable )
    {
        debugs.add( new Message( s, throwable ) );
    }

    public List<Message> getDebugs()
    {
        return debugs;
    }


    private final List<Message> infos = new ArrayList<>();
    @Override
    public void info( String s, Throwable throwable )
    {
        infos.add( new Message( s, throwable ) );
    }

    public List<Message> getInfos()
    {
        return infos;
    }

    private final List<Message> warns = new ArrayList<>();
    @Override
    public void warn( String s, Throwable throwable )
    {
        warns.add( new Message( s, throwable ) );
    }

    public List<Message> getWarns()
    {
        return warns;
    }

    private final List<Message> errors = new ArrayList<>();
    @Override
    public void error( String s, Throwable throwable )
    {
        errors.add( new Message( s, throwable ) );
    }

    public List<Message> getErors()
    {
        return errors;
    }

    private final List<Message> fatals = new ArrayList<>();
    @Override
    public void fatalError( String s, Throwable throwable )
    {
        fatals.add( new Message( s, throwable ) );
    }

    public List<Message> getFatals()
    {
        return fatals;
    }

    @Override
    public Logger getChildLogger( String s )
    {
        return null;
    }
}
