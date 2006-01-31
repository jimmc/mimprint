/* MailMessage.java
 *
 * Jim McBeath, November 26, 2004
 */

package net.jimmc.util;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.ProtocolException;
import java.net.UnknownHostException;

/** A simple mailer.
 * Create MailMessage object, set from/to/subject/body with the accessor
 * methods, then call {@link #send} to send it via SMTP to the mailer.
 * The defailt mailhost is "mailhost", which you can change by calling
 * the {@link #setMailHost} method.
 */
public class MailMessage {
    Socket socket;
    DataInputStream in;
    PrintStream out;

    private String from;
    public void setFrom(String from) { this.from = from; }
    public String getFrom() { return from; }

    private String to;
    public void setTo(String to) { this.to = to; }
    public String getTo() { return to; }

    private String subject;
    public void setSubject(String subject) { this.subject = subject; }
    public String getSubject() { return subject; }

    private String body;
    public void setBody(String body) { this.body = body; }
    public String getBody() { return body; }

    //The machine from which we send mail (via SMTP)
    //If not set before we send the mail, this gets set to the local hostname
    private String fromHost = "localhost";
    public void setFromHost(String fromHost) { this.fromHost = fromHost; }
    public String getFromHost() { return fromHost; }

    //The machine to which we send mail (via SMTP)
    private String mailHost = "mailhost";
    public void setMailHost(String mailHost) { this.mailHost = mailHost; }
    public String getMailHost() { return mailHost; }

    /** Create an empty message. */
    public MailMessage() {
System.out.println("Creating new mail message");
    }

    public void send()
	    throws UnknownHostException, IOException, ProtocolException {
	//Get our host name
System.out.println("Sending mail message");
	if (fromHost==null)
	    fromHost = InetAddress.getLocalHost().toString();

	//Open the connection
System.out.println("Connecting to "+mailHost+":25");
	socket = new Socket(mailHost, 25);
	in = new DataInputStream(socket.getInputStream());
	out = new PrintStream(socket.getOutputStream());

	String inLine;

        //Read the initial message
	inLine = requireLineStartingWith("220");
	while (inLine.length()>3 && inLine.charAt(3)=='-') {
	    inLine = requireLineStartingWith("220");
	}

	//Send our HELO message and other protocol messages
	sendAndRequire("HELO "+fromHost,"250");
	sendAndRequire("MAIL FROM: "+from,"250");
	sendAndRequire("RCPT TO: "+to,"250");

	//Announce our intent to send the data
	sendAndRequire("DATA","354");

	//Send the header
	out.println("From: "+from);
	out.println("To: "+to);
	out.println("Subject: "+subject);
	out.println("X-Mailer: JRaceman Java SMTP");
	out.println();		//blank line after header
	out.println(body);
	out.println(".");	//end of body
	out.flush();

	inLine = requireLineStartingWith("250");

	sendLine("QUIT");

	out.close();
	in.close();
	socket.close();
    }

    /** Send a line to our output and throw an exceptio if we don't get
     * a reply with the expected prefix.
     */
    private String sendAndRequire(String line, String replyPrefix)
	    throws IOException, ProtocolException {
	sendLine(line);
	return requireLineStartingWith(replyPrefix);
    }

    /** Send a line to our output and flush it.
     */
    private void sendLine(String line) {
System.out.println("Sending mail line: "+line);
	out.println(line);
	out.flush();
    }

    /** Read a line from mailhost, throw exception if
     * it doesn't start with the prefix.
     * @param prefix What the line must start with.
     * @return The line read from the input.
     */
    private String requireLineStartingWith(String prefix)
	    throws IOException, ProtocolException {
	String inLine = in.readLine();
System.out.println("Read mail line: "+inLine);
	if (!inLine.startsWith(prefix)) {
System.out.println("  was expecting "+prefix);
	    throw new ProtocolException(inLine);
	}
	return inLine;
    }

    public String toString() {
	StringBuffer sb = new StringBuffer();
	sb.append("From: ").append(from).append("\n");
	sb.append("To: ").append(to).append("\n");
	sb.append("Subject: ").append(subject).append("\n");
	sb.append("\n");
	sb.append(body);
System.out.println("Mail.toString() returns "+sb.toString());
	return sb.toString();
    }
}
