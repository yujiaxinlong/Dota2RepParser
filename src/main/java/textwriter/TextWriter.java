package textwriter;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;

public class TextWriter {
	private String path;
	private Boolean append;
	private PrintWriter print_line;
	
	public TextWriter( String file_path ,Boolean append) {

		path = file_path;
		this.append = append;

		}
	
	public void writeToFile( String textLine ) throws IOException {
		FileWriter writer = new FileWriter( path,append );
		print_line = new PrintWriter( writer );
		print_line.printf( "%s" + "%n" , textLine);
	}
	
	public void close(){
		print_line.close();
	}
}
