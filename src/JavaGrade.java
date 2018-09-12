/**
 * 
 */

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TreeMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.cli.*;

/**
 * @author Shad Sterling <me@shadsterling.com>
 *
 */
public class JavaGrade {
	public static DateTimeFormatter jgtime = DateTimeFormatter.ofPattern( "yyyy-MMM-dd-HH-mm-ss" ); // Format for output; ":" disallowed on some systems (turns to "/" on MacOS)

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Options options = new Options();
		options.addOption( null, "in-plain", true, "Directory containing plain submissions in each folder" );
		options.addOption( null, "in-bs", true, "Directory containing Brightspace submissions" );
		options.addOption( null, "out", true, "Output directory for gradable submissions (default: \"JavaGrade\" sibling to first inpout dir)" );
		options.addOption( null, "group", true, "Subdirectory in output for this run (default: time of run)" );
		options.addOption( null, "dup", true, "Directory containing additional files to add to each submission" );
		options.addOption( null, "execute", false, "Run each submission after compiling" );
		options.addOption( null, "main", true, "Java file with main method to run on each submission" );
		CommandLineParser parser = new PosixParser();
		CommandLine cmd = null;
		if( 0 != args.length ) try {
			cmd = parser.parse( options, args);
		} catch ( ParseException e1 ) {
			System.err.println( "Unable to parse command-line options: "+e1.getMessage() );
			e1.printStackTrace();
		}
		if( null == cmd ) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "javagrade", options );
		} else {
			JavaGrade jg = new JavaGrade( cmd );
			jg.doall();
		}
	}
	
	File[] indirs_plain, indirs_bs, dupdirs;
	File outdir, subdir, main;
	String group;
	boolean execute;
	TreeMap<String,User> users;
	
	public JavaGrade( CommandLine cmd ) {
		if( cmd.hasOption( "out" ) ) {
			outdir = new File( cmd.getOptionValue( "out" ) );
		}
		if( cmd.hasOption( "group" ) ) {
			group = cmd.getOptionValue( "group" );
		} else {
			group = jgtime.format( LocalDateTime.now() );
		}
		if( cmd.hasOption( "in-plain" ) ) {
			String opts[] = cmd.getOptionValues( "in-plain" );
			indirs_plain = new File[opts.length];
			for( int i = 0; i < opts.length; i++ ) {
				indirs_plain[i] = new File( opts[i] );
			}
			if( null == outdir ) {
				outdir = new File( indirs_plain[0].getParentFile(), "JavaGrade" );
			}
		} else {
			indirs_plain = new File[0];
		}
		if( cmd.hasOption( "in-bs" ) ) {
			String opts[] = cmd.getOptionValues( "in-bs" );
			indirs_bs = new File[opts.length];
			for( int i = 0; i < opts.length; i++ ) {
				indirs_bs[i] = new File( opts[i] );
			}
			if( null == outdir ) {
				outdir = new File( indirs_bs[0].getParentFile(), "JavaGrade" );
			}
		} else {
			indirs_bs = new File[0];
		}
		if( null == outdir ) {
			outdir = new File( "./JavaGrade" );
		}
		subdir = new File( outdir, group );
		if( cmd.hasOption( "dup" ) ) {
			String opts[] = cmd.getOptionValues( "dup" );
			dupdirs = new File[opts.length];
			for( int i = 0; i < opts.length; i++ ) {
				System.out.println( "DUPDIR "+opts[i] );
				dupdirs[i] = new File( opts[i] );
			}
		}
		execute = cmd.hasOption( "execute" );
		if( cmd.hasOption( "main" ) ) {
			main = new File( cmd.getOptionValue("main") );
		}
		users = new TreeMap<>();
	}
	
	public void doall() {
		if( null != indirs_plain && 0 != indirs_plain.length ) {
			System.out.println( "UNIMPLEMENTED: plain inputs; skipping" );
		}
		if( null != indirs_bs ) {
			DateTimeFormatter bstime = DateTimeFormatter.ofPattern( "MMM d, yyyy hmm a" ); // This is the format used in BrightSpace filenames
			for( File indir : indirs_bs ) {
				System.out.println( "Reading Brightspace submissions in "+indir );
				for( File file : indir.listFiles() ) {  // TODO: FIXME: listFiles can return null when indir does not exist
					String[] parts = file.getName().split( " - ", 4 );
					if( 4 > parts.length ) {
						if( file.getName().equals("index.html") ) { // TODO: FIXME: actually use the BrightSpace index?
							System.out.println( "Skipping file with invalid name: "+file.getName() );
						}
					} else {
						String uid = parts[0];
						String fullname = parts[1];
						String standardname = User.standardName( parts[1] );
						String time_str = parts[2];
						LocalDateTime time = LocalDateTime.parse( time_str, bstime );
						String filename = parts[3];
						User u = users.get( standardname );
						if( null == u ) {
							u = new User( fullname, uid );
							users.put( standardname, u );
						}
						u.add( time, filename, file.getPath() );
					}
				}
				// TODO: FIXME: print summary of loaded users/submissions/files
			}
		}
		// TODO: FIXME: print summary of loaded users/submissions/files
		System.out.println( "Working in "+subdir );
		subdir.mkdirs();
		for( User u : users.values() ) {
			System.out.println( "=============== "+u );
			try {
				u.grade( subdir, dupdirs, execute );
			} catch (IOException e) {
				System.out.println( "     =========== Failed!  Exception arranging files: "+e.toString() );
				e.printStackTrace();
			}
		}
		System.out.println( "========== Done." );
	}
	
	public String toString() {
		StringBuilder r = new StringBuilder();
		r.append( "<"+super.toString() );
		r.append( " outdir=\""+outdir+"\"" );
		r.append( " group=\""+group+"\"" );
		r.append( " indirs_bs=[" );
		if( indirs_bs.length > 0 ) {
			r.append( "\"" );
			r.append( indirs_bs[0] );
			r.append( "\"" );
			for( int i=1; i<indirs_bs.length; i++ ) {
				r.append( ",\"" );
				r.append( indirs_bs[i] );
				r.append( "\"" );
			}
		}
		r.append( "]" );
		r.append( " indirs_plain=[" );
		if( indirs_plain.length > 0 ) {
			r.append( "\"" );
			r.append( indirs_plain[0] );
			r.append( "\"" );
			for( int i=1; i<indirs_plain.length; i++ ) {
				r.append( ",\"" );
				r.append( indirs_plain[i] );
				r.append( "\"" );
			}
		}
		r.append( "]" );
		r.append( " main=\""+main+"\"" );
		r.append( ">" );
		return r.toString();
	}

	static class User {
		String firstname;
		String middlename;
		String lastname;
		String uid;
		TreeMap<LocalDateTime,Submission> submissions;

		static String[] splitName( String fullname ) {
			String[] parts = fullname.split("\\s+");
			String[] r = { parts[0], null, parts[parts.length-1] };
			if( 2 < parts.length ) { r[1] = String.join( " ", Arrays.copyOfRange( parts, 1, parts.length-1 ) ); }
			/*
			System.out.println( "fullname   = "+fullname );
			System.out.println( "parts      = "+parts.length+":[ \""+String.join("\", \"", parts)+"\" ]" );
			if( 2 < parts.length ) { 
				System.out.println( "midparts   = "+(parts.length-2)+":[ \""+String.join("\", \"", Arrays.copyOfRange( parts, 1, parts.length-1 ))+"\" ]" );
			} else {
				System.out.println( "midparts   = 0:[]" );
			}
			System.out.println( "firstname  = "+r[0] );
			System.out.println( "middlename = "+r[1] );
			System.out.println( "lastname   = "+r[2] );
			//*/
			return r;
		}
		static String joinName( String[] nameparts ) {
			//System.out.println( "nameparts  = "+nameparts.length+":[ \""+String.join("\", \"", nameparts)+"\" ]" );
			String s = nameparts[2]+", "+nameparts[0];
			if( null != nameparts[1] ) { s += " "+nameparts[1]; }
			//System.out.println( "joined     = "+s );
			return s;
		}
		static String standardName( String fullname ) {
			return joinName( splitName( fullname ) );
		}


		public User( String name, String uid ) {
			String[] nameparts = splitName( name );
			this.firstname = nameparts[0];
			this.middlename = nameparts[1];
			this.lastname = nameparts[2];
			this.uid = uid;
			this.submissions = new TreeMap<>();
		}
		String getName() {
			String[] nameparts = { this.firstname, this.middlename, this.lastname };
			return joinName( nameparts );
		}
		int countSubmissions() { return this.submissions.size(); }
		int countFiles() { int sum=0; for( Submission s : this.submissions.values() ) { sum += s.countFiles(); }; return sum; }

		void add( LocalDateTime time, String filename, String source ) {
			Submission s = this.submissions.get( time );
			if( null == s ) {
				s = new Submission( time );
				this.submissions.put( time, s );
			}
			s.add( filename, source );
		}

		void grade( File outdir_parent, File[] dupdirs, boolean execute ) throws IOException {
			File outdir = new File( outdir_parent, this.getName() + " (" + this.uid + ")" );
			outdir.mkdir();
			for( Submission s : this.submissions.values() ) {
				s.collect( outdir, dupdirs );
			}
			Submission s = this.submissions.get(this.submissions.lastKey());
			s.build();
			if( execute ) {
				s.run();
			}
		}

		public String toString() {
			int count = this.countSubmissions();
			String s = this.getName()+" ("+this.uid+")  - "+count+" submission";
			if( 1 == count ) {
				s += " ("+jgtime.format( this.submissions.firstKey() )+")";
			} else {
				s += "s";
			}
			if( 0 != count ) {
				int filecount = this.countFiles();
				s += ", "+filecount+" file";
				if( 1 == filecount ) {
					s += ": " + this.submissions.get(this.submissions.firstKey()).getFilenames()[0];
				} else {
					s += "s";
				}
			}
			return s;
		}
	}
	static class Submission {
		LocalDateTime time;
		TreeMap<String,String> files;
		File outdir;

		public Submission( LocalDateTime time ) {
			this.time = time;
			this.files = new TreeMap<>();
		}
		int countFiles() { return this.files.size(); }
		String[] getFilenames() {
			String[] r = new String[this.files.size()];
			int i = 0;
			for( String filename : this.files.navigableKeySet() ) {
				r[i] = filename;
				i++;
			}
			return r;
		}

		void add( String file, String source ) {
			this.files.put( file, source );
		}

		void collect( File outdir_parent, File[] dupdirs ) throws IOException {
			this.outdir = new File( outdir_parent, jgtime.format( this.time ) );
			outdir.mkdir();
			for( Map.Entry<String,String> entry : this.files.entrySet() ) { // copy files from BrightSpace submission, demangeling name
				String filename = entry.getKey();
				String sourcename = entry.getValue();
				File target = new File( outdir, filename );
				File source = new File( sourcename );
				//TODO: FIXME: handle extracting archives
				Files.copy( source.toPath(),  target.toPath(), StandardCopyOption.REPLACE_EXISTING );  // Replace existing to match compiler behavior
			}
			if( null != dupdirs ) {
				for( File dupdir : dupdirs ) {
					for( Path source : Files.newDirectoryStream( dupdir.toPath() ) ) {
						//TODO: FIXME: handle subdirectories currectly
						String subpath = dupdir.toURI().relativize(source.toUri()).getPath(); // relative path adapted from http://stackoverflow.com/a/205655/776723
						Path target = new File( outdir, subpath ).toPath();
						Files.copy( source,  target, StandardCopyOption.REPLACE_EXISTING );  // Replace existing to prevent submission overrides
					}
				}
			//}
			}
		}
		int build() throws IOException {
			System.out.println( "     =========== Compiling "+jgtime.format( this.time ) );
			List<String> components = new ArrayList<>( 1+this.files.size() );
			components.add( "javac" ); // TODO: FIXME: assuming javac is in path...
			for( String filename : this.files.keySet() ) {
				if( filename.matches( ".*\\.java$" ) ) {
					components.add( filename );
				}
			}
			ProcessBuilder command = new ProcessBuilder(components);
			command.inheritIO();
			command.directory( this.outdir );
			Process process = command.start();
			while(true) {
				try {
					process.waitFor();
					break;
				} catch (InterruptedException e) {
					continue; // if interrupted before process exits, keep waiting
				}
			}
			int status = process.exitValue();
			if( 0 != status ) {
				System.out.println( "     =========== Compiler status: "+status );
			}
			//TODO: FIXME: rearrange folders according to packages
			return status;
		}
		int run() throws IOException {
			File[] allfiles = this.outdir.listFiles();
			List<String> classes = new ArrayList<>();
			int status = -1;
			for( File file : allfiles ) {
				if( file.toString().matches( ".*\\.class$" ) ) {
					String filename = file.getName();
					String classname = filename.substring(0,filename.length()-6); // remove the last 6 chars, which are ".class"
					classes.add( classname );
				}
			}
			if( 0 == classes.size() ) {
				System.out.println( "     =========== Compiled classes: "+classes.size() );
			} else {
				URLClassLoader loader = new URLClassLoader( new URL[]{outdir.toURI().toURL()} ); // Because loading class files has to be complicated
				List<String> mainclasses = new ArrayList<>();
				for( String classname : classes ) {
					try {
						Class<?> c = loader.loadClass( classname );
						c.getMethod( "main", String[].class );
						mainclasses.add( classname ); // if no exceptions are thrown, this class has a main method
					} catch( ClassNotFoundException e ) {
						//TODO: FIXME: include these errors in output
					} catch( NoSuchMethodException e ) {
						// Not having a main method is normal
					} catch( SecurityException e ) {
						System.out.println( "          ====== ! Security Exception checking class \""+classname+"\": "+e.toString() );
					} catch( NoClassDefFoundError e ) {
						System.out.println( "          ====== ! Not Found Exception checking class \""+classname+"\" (this may be due to a spurious package declaration): "+e.toString() );
					}
				}
				loader.close();
				if( 0 == mainclasses.size() ) {
					System.out.println( "     =========== Executable classes: "+mainclasses.size() );
					status = -2;
				} else {
					if( 1 != mainclasses.size() ) {
						System.out.println( "     =========== Too many executable classes: "+String.join(", ", mainclasses) );
						status = -3;
					} else {
						String classname = mainclasses.get(0);
						System.out.println( "     =========== Executing "+classname+" (this may be interactive and/or open a GUI)" );
						ProcessBuilder command = new ProcessBuilder("java",classname);
						command.inheritIO();
						command.directory( this.outdir );
						Process process = command.start();
						while(true) {
							try {
								process.waitFor();
								break;
							} catch (InterruptedException e) {
								continue; // if interrupted before process exits, keep waiting
							}
						}
						status = process.exitValue();
						System.out.println( "     =========== "+classname+" exit status: "+status );
					}
				}
			}
			return status;
		}
	}

}
