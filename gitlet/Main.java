package gitlet;

import static gitlet.Utils.join;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author AL-Shoukaku
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        } else if (!args[0].equals("init") && !join(".gitlet").exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                Gitlet.init(args);
                break;
            case "add":
                Gitlet.add(args);
                break;
            case "commit":
                Gitlet.commit(args);
                break;
            case "rm":
                Gitlet.rm(args);
                break;
            case "log":
                Gitlet.log(args);
                break;
            case "global-log":
                Gitlet.globalLog(args);
                break;
            default:
                System.out.println("No command with that name exists.");
                System.exit(0);
                break;
        }
    }
}
