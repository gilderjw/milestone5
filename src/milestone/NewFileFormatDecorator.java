package milestone;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;

import ArgumentProcessors.CommandLineProcessor;
import ArgumentProcessors.SettingsFileCommandArgumentProcessor;
import application.CodeProcessor;

public class NewFileFormatDecorator extends SettingsFileCommandArgumentProcessor{

	public NewFileFormatDecorator(CommandLineProcessor p) {
		super(p);
	}

	@Override
	public CodeProcessor process(String[] args) {

		for(int i = 0; i < args.length; i++){
			if(this.verifyPrefix(args[i])){
				File file = new File(args[i].substring(args[i].indexOf('=')+1));

				// replace the file we are passing through
				args[i] = "cfg=temp.cfg";

				try {
					BufferedReader bs = new BufferedReader(new FileReader(file));
					PrintWriter bw = new PrintWriter("temp.cfg", "UTF-8");

					String line = bs.readLine();

					while (line != null) {
						bw.print(line.replaceAll(":", "="));
						System.out.println(line.replaceAll(":", "="));
						line = bs.readLine();
						if(line != null){
							bw.println();
						}
					}

					bs.close();
					bw.close();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}

		return this.p.process(args);
	}
}
