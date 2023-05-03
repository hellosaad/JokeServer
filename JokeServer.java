/*1. Name of the student: Saad Mohammed Khaled

2. Date of Submission : 30-04-2023

3. Java version used : 20 (build 20+36-2344)

4. Precise command-line compilation examples / instructions:

> javac JokeServer.java

5. Precise examples / instructions to run this program:

In separate shell windows:

> java JokeServer
> java JokeClient
> java JokeClientAdmin

6. Full list of files needed for running the program:

 a. JokeServer.java

*/

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Scanner;

// JokeClient class
class JokeClient {
    // Main method of JokeClient class
    public static void main(String argv[]) throws IOException {
        // Create a new JokeClient object and run it
        JokeClient cc = new JokeClient(argv);
        cc.run(argv);
    }

    // JokeClient constructor
    public JokeClient(String argv[]) {
        // Empty constructor, can be used for any initialization if required
    }

    // Run method of JokeClient class, responsible for establishing connection with
    // the server
    // and handling user input
    public void run(String[] argv) throws IOException {
        // Declare variables for socket and server name
        Socket socket;
        String serverName;

        // Determine server name based on provided arguments, default to localhost
        if (argv.length < 1) {
            serverName = "localhost";
        } else {
            serverName = argv[0];
        }

        // Create a Scanner object for reading user input from the console
        Scanner input = new Scanner(System.in);
        System.out.println("Enter your name: ");
        System.out.flush();
        String userName = input.nextLine();
        System.out.println("Hello " + userName);

        // Print connection success message
        System.out.println("\nThe connection to the JokeServer at port 4546 has been successful.");

        // Generate a unique client ID using UUID
        String clientId = UUID.randomUUID().toString();

        // Main loop for handling user input, will continue until user types 'quit'
        while (true) {
            // Prompt the user for input
            System.out.print("To obtain a joke or proverb, press Enter, and to leave, type quit:");

            // Read user input from the console
            String userInput = input.nextLine();

            // Verify whether the user wishes to end the program.
            if (userInput.equalsIgnoreCase("quit")) {
                break;
            }

            // Using the server name and port, create a new socket connection to the server.
            socket = new Socket(serverName, 4546);

            // Set up an ObjectOutputStream to send the userName to the server
            ObjectOutputStream outgoingObject = new ObjectOutputStream(socket.getOutputStream());
            outgoingObject.writeObject(userName);

            // Create input and output streams for communication with the server
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            // Send a request message to the server to ask for a joke or proverb
            out.println("request");

            // Read the server's response and show it on the console.
            String responseFromServer = in.readLine();
            System.out.println(responseFromServer);

            // terminate the server's socket connection.
            socket.close();
        }
    }
}

// JokeServer class definition, responsible for serving jokes and proverbs to
// connected clients
public class JokeServer {
    // Declare class variables to store the current mode, jokes, and proverbs
    static int mode = 0; // 0 for jokes, 1 for proverbs
    static String[] jokes = {
            "JA: What concert costs 45 cents to get in? 50 cent featuring nickelback.",
            "JB: Yesterday I farted on my wallet, now i have gas money",
            "JC: What do you call an Aligator in a vest? An Investigator",
            "JD: You know what the mushroom said? that he is a FunGi"
    };
    static String[] proverbs = {
            "PA: Its always in the eyes,chico.",
            "PB: Make Hay while the sun shines.",
            "PC: A Fake Friend is worse than a thousand enemies.",
            "PD: A night full of stars,but i would still look at you."
    };

    // Main method of JokeServer class
    public static void main(String args[]) throws IOException {
        // Declare variables for server configuration
        int serverPort = 4546;
        Socket sock;

        // Print server startup message
        System.out.println("Launching Saad's Joke Server 1.0 and Listening at Port " + serverPort + ".\n");


        // Start the AdminLooper thread to listen for mode change requests from
        // JokeClientAdmin
        AdminLooper AL = new AdminLooper();
        Thread t = new Thread(AL);
        t.start();

        // Create a new ServerSocket to listen for client connections
        ServerSocket servSock = new ServerSocket(serverPort, 6);
        System.out.println("ServerSocket is ready to accept connections.");
        printMode();

        // Main loop for accepting client connections and creating worker threads to
        // handle them
        while (true) {
            // Accept incoming client connection
            sock = servSock.accept();

            // Create a new JokeWorker object and start it in a new thread
            JokeWorker worker = new JokeWorker(sock);
            Thread workerThread = new Thread(worker);
            workerThread.start();
        }
    }

    // Method to get the current mode (joke or proverb) of the server
    public static int getMode() {
        return mode;
    }

    // a technique for printing the server's mode at the moment to the console
    static void printMode() {
        if (mode == 0) {
            System.out.println("The server is now operating in JOKE mode.");
        } else if (mode == 1) {
            System.out.println("The server is now operating in PROVERB mode.");
        }
    }
}

// JokeWorker class definition, responsible for processing client requests in a
// separate thread
class JokeWorker implements Runnable {
    // Declare instance variables for the worker
    private Socket sock;
    private String clientId;
    private static Map<String, Set<String>> jokesServed = new HashMap<>();
    private static Map<String, Set<String>> proverbsServed = new HashMap<>();
    private static HashMap<String, Integer> jokeCount = new HashMap<>();
    private static HashMap<String, Integer> proverbCount = new HashMap<>();
    static String userName;

    // JokeWorker constructor
    JokeWorker(Socket s) {
        sock = s;
    }

    // Replace the Runnable interface's run function with your own.
    @Override
    public void run() {
        try {
            // Set up the client connection's input and output streams
            BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            PrintWriter out = new PrintWriter(sock.getOutputStream(), true);

            // Deserialize the incoming object containing the user's name
            try {
                ObjectInputStream incomingObject = new ObjectInputStream(sock.getInputStream());
                userName = (String) incomingObject.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            // Print the received userName
            System.out.println(userName + " has requested for a " + (JokeServer.getMode() == 0 ? "joke" : "proverb"));

            // Initialize the jokes and proverbs served maps if the userName is not already
            // present
            if (!jokesServed.containsKey(userName)) {
                jokesServed.put(userName, new HashSet<>());
            }

            if (!proverbsServed.containsKey(userName)) {
                proverbsServed.put(userName, new HashSet<>());
            }

            // Main loop to process user input and serve jokes or proverbs
            while (true) {
                // Read user input from the client connection
                String userInput = in.readLine();

                // Check if the user input is a request for a joke or proverb
                if (!"request".equalsIgnoreCase(userInput)) {
                    continue;
                }

                // Check the server mode and serve either a joke or a proverb
                if (JokeServer.getMode() == 0) {
                    // Initialize the joke count for the user if not already present
                    if (!jokeCount.containsKey(userName)) {
                        jokeCount.put(userName, 0);
                    }

                    // Get the next joke to serve to the user
                    String joke = getNextJoke();

                    // Send the joke to the user, replacing the prefix with the user's name
                    if (joke != null) {
                        jokeCount.put(userName, jokeCount.get(userName) + 1);
                        out.println(joke.replaceFirst(":", " " + userName + ":"));
                    } else {
                        out.println(getNextJoke().replaceFirst(":", " " + userName + ":")
                                + " *** JOKE CYCLE IS COMPLETED *** ");
                    }
                } else if (JokeServer.getMode() == 1) {
                    // Initialize the proverb count for the user if not already present
                    if (!proverbCount.containsKey(userName)) {
                        proverbCount.put(userName, 0);
                    }

                    // Get the next proverb to serve to the user
                    String proverb = getNextProverb();

                    // Send the proverb to the user, replacing the prefix with the user's name
                    if (proverb != null) {
                        proverbCount.put(userName, proverbCount.get(userName) + 1);
                        out.println(proverb.replaceFirst(":", " " + userName + ":"));
                    } else {
                        out.println(getNextProverb().replaceFirst(":", " " + userName + ":")
                                + " *** PROVERB CYCLE IS COMPLETED *** ");
                    }
                }
            }
        } catch (IOException e) {
            // Handle exceptions related to input/output
            System.out.println("Connection closed: " + e.getMessage());
        } finally {
            // Close the socket connection in a finally block to ensure it is always closed
            try {
                sock.close();
            } catch (Exception e) {
                System.out.println("Error closing socket: " + e.getMessage());
            }
        }
    }

    // Method to get the next joke for the user
    private String getNextJoke() {
        // Check if the user has been served all jokes already
        if (jokeCount.containsKey(userName) && jokeCount.get(userName) >= 3) {
            jokeCount.put(userName, 0);
            jokesServed.put(userName, new HashSet<>());
            return null;
        }

        // Randomly select a joke from the server's joke list
        String joke = JokeServer.jokes[new Random().nextInt(JokeServer.jokes.length)];

        // Ensure the selected joke has not already been served to the user
        while (true) {
            if (!jokesServed.get(userName).contains(joke)) {
                break;
            } else {
                joke = JokeServer.jokes[new Random().nextInt(JokeServer.jokes.length)];
            }
        }

        // Add the selected joke to the served jokes map for the user
        if (joke != null) {
            jokesServed.get(userName).add(joke);
        } else {
            jokesServed.get(userName).clear();

            // If the joke has been served before, find a new joke
            if (jokesServed.get(userName).contains(joke)) {
                joke = JokeServer.jokes[new Random().nextInt(JokeServer.jokes.length)];
            }

            jokesServed.get(userName).add(joke);
            return "Joke cycle completed." + joke;
        }
        return joke;
    }

    // Method to get the next proverb for the user
    private String getNextProverb() {
        // Check if the user has been served all proverbs already
        if (proverbCount.get(userName) >= 3) {
            proverbCount.put(userName, 0);
            proverbsServed.put(userName, new HashSet<>());
            return null;
        }

        // Randomly select a proverb from the server's proverb list
        String proverb = JokeServer.proverbs[new Random().nextInt(JokeServer.proverbs.length)];

        // Ensure the selected proverb has not already been served to the user
        while (true) {
            if (!proverbsServed.get(userName).contains(proverb)) {
                break;
            } else {
                proverb = JokeServer.proverbs[new Random().nextInt(JokeServer.proverbs.length)];
            }
        }

        // Add the selected proverb to the served proverbs map for the user
        if (proverb != null) {
            proverbsServed.get(userName).add(proverb);
        } else {
            proverbsServed.get(userName).clear();

            // If the proverb has been served before, find a new proverb
            if (proverbsServed.get(userName).contains(proverb)) {
                proverb = JokeServer.proverbs[new Random().nextInt(JokeServer.proverbs.length)];
            }

            proverbsServed.get(userName).add(proverb);
            return "Proverb cycle completed. " + proverb;
        }
        return proverb;
    }
}

class AdminLooper implements Runnable {
    // Variable to control the state of the admin thread
    public static boolean adminControlSwitch = true;

    // The AdminLooper class's run function will be performed when the admin thread
    // launches.
    @Override
    public void run() {
        // Print to console to indicate that the admin thread has started
        System.out.println("within the admin looper thread");

        // Variables to store the length of the queue and the port number for the admin
        // connection
        int port = 5051;
        Socket sock;

        // Try to create a ServerSocket for admin connections
        try {
            ServerSocket servsock = new ServerSocket(port, 6);

            // Keep accepting connections from the JokeClientAdmin as long as the
            // adminControlSwitch is true
            while (adminControlSwitch) {
                sock = servsock.accept();


                // Create a PrintWriter to send messages to the connected JokeClientAdmin
                PrintWriter out = new PrintWriter(sock.getOutputStream(), true);

                // Create a BufferedReader to read input from the connected JokeClientAdmin
                BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));

                // Read the userInput from the JokeClientAdmin
                String userInput = in.readLine();

                // Change the mode of the server based on the userInput
                if (userInput.equalsIgnoreCase("joke") || userInput.equalsIgnoreCase("proverb")) {
                    JokeServer.mode = userInput.equalsIgnoreCase("joke") ? 0 : 1;
                    out.println("Server mode changed to "
                            + (userInput.equalsIgnoreCase("joke") ? "Joke Mode" : "Proverb Mode"));

                    // Print the current mode of the server to the console
                    JokeServer.printMode();
                } else {
                    // If the userInput is invalid, print an error message to the console
                    System.out.println("Invalid command. Please try again.");
                }

                // Close the socket connection with the JokeClientAdmin
                sock.close();
            }

            // Close the ServerSocket when the adminControlSwitch is set to false
            servsock.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class JokeClientAdmin {
    // The main method to start the JokeClientAdmin
    public static void main(String argv[]) throws IOException {
        JokeClientAdmin jc = new JokeClientAdmin(argv);
        jc.run(argv);
    }

    // Constructor for JokeClientAdmin class
    public JokeClientAdmin(String argv[]) {
        // This constructor is empty but can be utilized for further customizations
    }

    // The run method to start the JokeClientAdmin logic
    public void run(String[] argv) throws IOException {
        String serverName;
        // Determine the server name based on command-line arguments
        if (argv.length < 1) {
            serverName = "localhost";
        } else {
            serverName = argv[0];
        }

        // Create a scanner to read user input
        Scanner scanner = new Scanner(System.in);
        boolean jokeMode = false;

        // Keep looping to allow the user to toggle the server mode
        while (true) {
            System.out.print("Press Enter to toggle the server mode: ");
            System.out.flush();
            scanner.nextLine();

            String userInput = jokeMode ? "joke" : "proverb";

            // Create a socket to connect to the server's admin port
            Socket socket = new Socket(serverName, 5051);
            System.out.println("\nJokeClientAdmin has successfully connected to the JokeServer at port 5051");

            // Create a BufferedReader to read the server's response
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Create a PrintWriter to send commands to the server
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            // Send the userInput (joke or proverb) to the server
            out.println(userInput);

            // Read the response from the server
            String responseFromServer = in.readLine();
            System.out.println(responseFromServer);

            // Close the socket connection with the server
            socket.close();

            // Toggle the jokeMode variable to switch between joke and proverb modes
            jokeMode = !jokeMode;
        }
    }
}

/*
 * Comments-Discussion postings
 * 1. Hello,
 * I am confused as to if the cycle completion message should appear on the
 * Jokeclient
 * console automatically after 4 jokes/proverbs have been requested by the user
 * or,
 * is it fine if it appears after the user presses enter (to make a request)
 * after requesting the 4 jokes/proverbs?
 * 2.Hello,
 * Somehow, I missed this discussion posting and yeah I tried to set each
 * individual client by taking the input at the
 * JokeClientAdmin console where the user had to specify the mode and the
 * username but was unable to maintain the state of these conversations.
 * I went through the checklist and the webpage thoroughly and found out that
 * the mode is set for the whole server but thought
 * that if I could implement the above-said feature I could put it up in the
 * bragging right section of the JokeServer checklist.
 */

/* JokeLog.txt */
/* JOKECLIENT */
/*
(base) saadmohammedkhaled@Saads-MacBook-Air src % javac *.java
(base) saadmohammedkhaled@Saads-MacBook-Air src % java JokeClient
Enter your name:
Saad
Hello Saad

The connection to the JokeServer at port 4546 has been successful.
To obtain a joke or proverb, press Enter, and to leave, type quit:
JA Saad: What concert costs 45 cents to get in? 50 cent featuring nickelback.
To obtain a joke or proverb, press Enter, and to leave, type quit:
JB Saad: Yesterday I farted on my wallet, now i have gas money
To obtain a joke or proverb, press Enter, and to leave, type quit:
JC Saad: What do you call an Aligator in a vest? An Investigator
To obtain a joke or proverb, press Enter, and to leave, type quit:
JC Saad: What do you call an Aligator in a vest? An Investigator *** JOKE CYCLE IS COMPLETED ***
To obtain a joke or proverb, press Enter, and to leave, type quit:
PD Saad: A night full of stars,but i would still look at you.
To obtain a joke or proverb, press Enter, and to leave, type quit:
PC Saad: A Fake Friend is worse than a thousand enemies.
To obtain a joke or proverb, press Enter, and to leave, type quit:
PB Saad: Make Hay while the sun shines.
To obtain a joke or proverb, press Enter, and to leave, type quit:
JA Saad: What concert costs 45 cents to get in? 50 cent featuring nickelback.
To obtain a joke or proverb, press Enter, and to leave, type quit:quit
(base) saadmohammedkhaled@Saads-MacBook-Air src %
 */

/* JOKESERVER */
/*
 (base) saadmohammedkhaled@Saads-MacBook-Air src % java JokeServer
Launching Saad's Joke Server 1.0 and Listening at Port 4546.

within the admin looper thread
ServerSocket is ready to accept connections.
The server is now operating in JOKE mode.
Saad has requested for a joke
Saad has requested for a joke
Saad has requested for a joke
Saad has requested for a joke
The server is now operating in PROVERB mode.
Saad has requested for a proverb
Saad has requested for a proverb
Saad has requested for a proverb
The server is now operating in JOKE mode.
Saad has requested for a joke
 */

/* JOKECLIENTADMIN */
/*
 (base) saadmohammedkhaled@Saads-MacBook-Air src % java JokeClientAdmin
Press Enter to toggle the server mode:

JokeClientAdmin has successfully connected to the JokeServer at port 5051
Server mode changed to Proverb Mode
Press Enter to toggle the server mode:

JokeClientAdmin has successfully connected to the JokeServer at port 5051
Server mode changed to Joke Mode
Press Enter to toggle the server mode:
 */
