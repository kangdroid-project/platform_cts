package android.net.cts.legacy.api22.permission;

import android.net.TrafficStats;
import android.test.AndroidTestCase;

import androidx.test.filters.MediumTest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class QtaguidPermissionTest extends AndroidTestCase {

    private static final String QTAGUID_STATS_FILE = "/proc/net/xt_qtaguid/stats";

    @MediumTest
    public void testDevQtaguidSane() throws Exception {
        File f = new File("/dev/xt_qtaguid");
        assertTrue(f.canRead());
        assertFalse(f.canWrite());
        assertFalse(f.canExecute());
    }

    public void testAccessPrivateTrafficStats() throws IOException {

        final int ownAppUid = getContext().getApplicationInfo().uid;
        try {
            BufferedReader qtaguidReader = new BufferedReader(new FileReader(QTAGUID_STATS_FILE));
            String line;
            // Skip the header line;
            qtaguidReader.readLine();
            while ((line = qtaguidReader.readLine()) != null) {
                String tokens[] = line.split(" ");
                // Go through all the entries we find the qtaguid stats and fail if we find a stats
                // with different uid.
                if (tokens.length > 3 && !tokens[3].equals(String.valueOf(ownAppUid))) {
                    fail("Other apps detailed traffic stats leaked, self uid: "
                         + String.valueOf(ownAppUid) + " find uid: " + tokens[3]);
                }
            }
            qtaguidReader.close();
        } catch (FileNotFoundException e) {
            fail("Was not able to access qtaguid/stats: " + e);
        }
    }

    private void accessOwnTrafficStats(long expectedTxBytes) throws IOException {

        final int ownAppUid = getContext().getApplicationInfo().uid;

        long totalTxBytes = 0;
        try {
            BufferedReader qtaguidReader = new BufferedReader(new FileReader(QTAGUID_STATS_FILE));
            String line;
            while ((line = qtaguidReader.readLine()) != null) {
                String tokens[] = line.split(" ");
                if (tokens.length > 3 && tokens[3].equals(String.valueOf(ownAppUid))) {
                    // Check the total stats of this uid is larger then 1MB
                    if (tokens[2].equals("0x0")) {
                        totalTxBytes += Integer.parseInt(tokens[7]);
                    }
                }
            }
            qtaguidReader.close();
        } catch (FileNotFoundException e) {
            fail("Was not able to access qtaguid/stats: " + e);
        }
        assertTrue(totalTxBytes + " expected to be greater than or equal to"
            + expectedTxBytes + "bytes", totalTxBytes >= expectedTxBytes);
    }

    public void testAccessOwnQtaguidTrafficStats() throws IOException {

        // Transfer 1MB of data across an explicitly localhost socket.
        final int byteCount = 1024;
        final int packetCount = 1024;

        final ServerSocket server = new ServerSocket(0);
        new Thread("CreatePrivateDataTest.createTrafficStatsWithTags") {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket("localhost", server.getLocalPort());
                    // Make sure that each write()+flush() turns into a packet:
                    // disable Nagle.
                    socket.setTcpNoDelay(true);
                    OutputStream out = socket.getOutputStream();
                    byte[] buf = new byte[byteCount];
                    for (int i = 0; i < packetCount; i++) {
                        TrafficStats.setThreadStatsTag(i % 10);
                        TrafficStats.tagSocket(socket);
                        out.write(buf);
                        out.flush();
                    }
                    out.close();
                    socket.close();
                } catch (IOException e) {
                  assertTrue("io exception" + e, false);
                }
            }
        }.start();

        try {
            Socket socket = server.accept();
            InputStream in = socket.getInputStream();
            byte[] buf = new byte[byteCount];
            int read = 0;
            while (read < byteCount * packetCount) {
                int n = in.read(buf);
                assertTrue("Unexpected EOF", n > 0);
                read += n;
            }
        } finally {
            server.close();
        }

        accessOwnTrafficStats(byteCount * packetCount);
    }
}
