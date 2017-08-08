package Application2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;

import java.nio.file.Files;

import java.util.Properties;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.Charset;
import java.nio.file.StandardOpenOption;

import java.util.Vector;
import java.util.regex.Pattern;

public class EresConverter
{
  private static final String[] MONTHS =
  {
    "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12"
  };
  //private static final String DIR_BASE = "/export/home/lws/eres/";
  //private static final String DIR_DEST = "/export/home/lws/eres/modified/";
  private static final String FILE_BASE = "httpd.access.";
  private static final String FILTERED_STUB = "_split_filtered.log";
  private static final String MODIFIED_STUB = "_split_modified.log";
  private static final String SITE = "eres.library.ucla.edu";
  private static final String PATTERN =
    "lis32.*Sentinel|\\\"Sentinel|www2.library.ucla.edu 406|unitproj.library.ucla.edu 406|unitproj1.library.ucla.edu 406|\\\"ApacheBench/";

  private static Path inputDir;
  private static Path outputDir;
  private static Properties props;
  private static String year;
  private static String propsFilename;

  public EresConverter()
  {
    super();
  }

  public static void main( String[] args )
  {
    switch ( args.length )
    {
      case 1:
        propsFilename = args[ 0 ];
        year = args[ 1 ];
        break;
      default:
        System.err.println( "Usage: EresConverter propsFile year" );
        System.exit( 1 );
    }

    loadProperties();
    setPaths();

    for ( String theMonth : MONTHS )
    {
      int theDay;

      for ( theDay = 1; theDay < 32; theDay++ )
      {
        Path filtered;
        Path modified;
        Path original;
        String fileName;

        fileName =
          FILE_BASE.concat( year ).concat( theMonth ).concat( ( theDay < 10 ? "0".concat( String.valueOf( theDay ) ) :
                                                                String.valueOf( theDay ) ) );
        original = inputDir.resolve( fileName );
        filtered = inputDir.resolve( fileName.concat( FILTERED_STUB ) );
        modified = outputDir.resolve( fileName.concat( MODIFIED_STUB ) );

        if ( Files.isReadable( original ) )
        {
          run_exclude_filter( original, filtered );
          add_site_apache( filtered, modified );
        }
        else
        {
          System.out.println( original.toAbsolutePath() + " does not exist" );
        }
      }
    }
  }

  private static void loadProperties()
  {
    props = new Properties();
    try
    {
      props.load( new FileInputStream( propsFilename ) );
    }
    catch ( IOException ioe )
    {
      System.err.println( "Unable to open properties file: " + propsFilename );
      ioe.printStackTrace();
      System.exit( 2 );
    }
  }

  private static void setPaths()
  {
    inputDir = Paths.get( props.getProperty( "path.source" ) );
    outputDir = Paths.get( props.getProperty( "path.destination" ) );
  }

  @SuppressWarnings( "oracle.jdeveloper.java.nested-assignment" )
  private static void run_exclude_filter( Path original_log, Path filtered_log )
  {
    BufferedReader reader;
    BufferedWriter writer;
    String line;

    try
    {
      reader = Files.newBufferedReader( original_log );
      writer =
        Files.newBufferedWriter( filtered_log, Charset.forName( "US-ASCII" ), StandardOpenOption.CREATE,
                                 StandardOpenOption.WRITE );
      line = null;

      while ( ( line = reader.readLine() ) != null )
      {
        if ( !Pattern.matches( PATTERN, line ) )
          writer.write( line );
        writer.newLine();
      }

      reader.close();
      writer.flush();
      writer.close();
    }
    catch ( IOException ioe )
    {
      ioe.printStackTrace();
    }
  }

  @SuppressWarnings( "oracle.jdeveloper.java.nested-assignment" )
  private static void add_site_apache( Path filtered_log, Path modified_log )
  {
    BufferedReader reader;
    BufferedWriter writer;
    String line;

    try
    {
      reader = Files.newBufferedReader( filtered_log );
      writer =
        Files.newBufferedWriter( modified_log, Charset.forName( "US-ASCII" ), StandardOpenOption.CREATE,
                                 StandardOpenOption.WRITE );
      line = null;

      while ( ( line = reader.readLine() ) != null )
      {
        String[] original_log_entries;
        Vector<String> modified_log_entries;

        original_log_entries = line.trim().split( " " );
        modified_log_entries = new Vector<String>( original_log_entries.length + 2 );

        modified_log_entries.add( original_log_entries[ 0 ] );
        modified_log_entries.add( SITE );

        for ( int index = 1; index < original_log_entries.length; index++ )
          modified_log_entries.add( original_log_entries[ index ] );

        writer.write( join( modified_log_entries ) );
        writer.newLine();
      }

      reader.close();
      writer.flush();
      writer.close();
    }
    catch ( IOException ioe )
    {
      ioe.printStackTrace();
    }
  }

  private static String join( Vector<String> input )
  {
    StringBuffer buffer;

    buffer = new StringBuffer();
    input.stream().forEach( i -> buffer.append( i ).append( " " ) );

    return buffer.toString().trim();
  }
}
