package edu.isi.ikcap.wings.util;

import java.util.HashMap;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

public class WGetOpt {
	public static String guiProgram = "WingsGUI";

	public static void displayUsage(String program) {
		if (!program.equals(guiProgram))
			System.out.println("usage: " + program + " [options] [seed|template options]");
		else
			System.out.println("usage: " + program + " [options]");
		System.out.println();
		System.out.println("options:");
		System.out.println(" -h, --help Show this help message (default)");
		System.out.println(" -c, --conf=<file> Specify the Wings properties file path");
		System.out.println(" -l, --logdir=<directory> Specify the directory to store logs");
		System.out.println(" -L, --libname=<name> Specify the Concrete Components Library to use");
		System.out.println(" -o, --outputdir=<directory> Specify the directory to store daxes");
		System.out.println(" -d, --domaindir=<directory> Load a domain from the specified directory");
		System.out.println(" -i, --initdomaindir=<directory> Initialize a new domain at the specified directory");
		if (!program.equals(guiProgram)) {
			System.out.println(" -r, --requestid=<id> Specify the Request ID to use");
			System.out.println(" -D, --getData=<file> Select appropriate data and store in <file>");
			System.out.println(" -P, --getParameters=<file> Get appropriate parameters and store in <file>");
			System.out.println(" -E, --elaborate=<file> Elaborate given template and store in <file>");
			System.out.println(" -V, --validate=<file> Validate given template and store rdf in <file>");
			System.out.println(" -T, --trim=<n> Trim the search space to return 'n' or less daxes");
			System.out.println();
			System.out.println("seed|template options:");
			System.out.println(" -s, --seed=<name> Specify the Seed name");
			System.out.println(" -t, --template=<name> Specify the Template name");
			System.out.println();
		}
	}

	public static HashMap<String, String> getOptions(String program, String[] args) {
		// Parse Arguments for SeedFile and TemplateID(optional)
		if (args.length == 0 && !program.equals(guiProgram)) {
			displayUsage(program);
			return null;
		}

		String sopts = "hc:l:o:d:i:r:L:D:P:E:V:T:s:t:";
		if (program.equals(guiProgram))
			sopts = "hc:l:o:d:i:";

		LongOpt[] lopts = { new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h'), 
				new LongOpt("conf", LongOpt.REQUIRED_ARGUMENT, null, 'c'),
				new LongOpt("logdir", LongOpt.REQUIRED_ARGUMENT, null, 'l'), 
				new LongOpt("outputdir", LongOpt.REQUIRED_ARGUMENT, null, 'o'),
				new LongOpt("domaindir", LongOpt.REQUIRED_ARGUMENT, null, 'd'),
				new LongOpt("initdomaindir", LongOpt.REQUIRED_ARGUMENT, null, 'i'),
				new LongOpt("requestid", LongOpt.REQUIRED_ARGUMENT, null, 'r'),
				new LongOpt("libname", LongOpt.REQUIRED_ARGUMENT, null, 'L'),
				new LongOpt("getData", LongOpt.REQUIRED_ARGUMENT, null, 'D'), 
				new LongOpt("getParameters", LongOpt.REQUIRED_ARGUMENT, null, 'P'),
				new LongOpt("elaborate", LongOpt.REQUIRED_ARGUMENT, null, 'E'),
				new LongOpt("validate", LongOpt.REQUIRED_ARGUMENT, null, 'V'),
				new LongOpt("trim", LongOpt.REQUIRED_ARGUMENT, null, 'T'), 
				new LongOpt("seed", LongOpt.REQUIRED_ARGUMENT, null, 's'),
				new LongOpt("template", LongOpt.REQUIRED_ARGUMENT, null, 't')
				};
		if (program.equals(guiProgram)) {
			lopts = new LongOpt[] { new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h'), 
					new LongOpt("conf", LongOpt.REQUIRED_ARGUMENT, null, 'c'),
					new LongOpt("logdir", LongOpt.REQUIRED_ARGUMENT, null, 'l'), 
					new LongOpt("outputdir", LongOpt.REQUIRED_ARGUMENT, null, 'o'),
					new LongOpt("domaindir", LongOpt.REQUIRED_ARGUMENT, null, 'd'),
					new LongOpt("libname", LongOpt.REQUIRED_ARGUMENT, null, 'L'),
					new LongOpt("initdomaindir", LongOpt.REQUIRED_ARGUMENT, null, 'i')
			};
		}

		int code;
		HashMap <String, String> options = new HashMap<String, String>();

		Getopt g = new Getopt(program, args, sopts, lopts);
		while ((code = g.getopt()) != -1) {
			switch (code) {
			case '?':
				displayUsage(program);
				return null;
			case 'h':
				displayUsage(program);
				return null;
			case 'T':
				String trimnum = g.getOptarg();
				boolean ok = true;
				try {
					if (Integer.parseInt(trimnum) == 0)
						ok = false;
				} catch (NumberFormatException e) {
					ok = false;
				}
				if (!ok) {
					System.err.println("-T or --trim takes a non-zero number argument");
					displayUsage(program);
					return null;
				}
				options.put("trim", trimnum);
				break;
			case 'r':
				options.put("requestid", g.getOptarg());
				break;
			case 'L':
				options.put("libname", g.getOptarg());
				break;
			case 'c':
				options.put("conf",g.getOptarg());
				break;
			case 's':
				options.put("seed", g.getOptarg());
				break;
			case 't':
				options.put("template", g.getOptarg());
				break;
			case 'l':
				options.put("logdir", g.getOptarg());
				break;
			case 'o':
				options.put("outputdir", g.getOptarg());
				break;
			case 'd':
				options.put("domaindir", g.getOptarg());
				break;
			case 'i':
				options.put("initdomaindir", g.getOptarg());
				break;
			case 'D':
				options.put("getData", g.getOptarg());
				break;
			case 'P':
				options.put("getParameters", g.getOptarg());
				break;
			case 'V':
				options.put("validate", g.getOptarg());
				break;
			case 'E':
				options.put("elaborate", g.getOptarg());
				break;
			default:
				displayUsage(program);
				return null;
			}
		}
		return options;
	}
}
