package backEnd;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class Wallet {
    /**
     * The RandomAccessFile of the wallet file
     */  
    private RandomAccessFile file;

    /**
     * Used for locks.
     */
    private FileChannel channel;

    /**
     * Creates a Wallet object
     *
     * A Wallet object interfaces with the wallet RandomAccessFile
     */
    public Wallet () throws Exception {

        this.file = new RandomAccessFile(new File("backEnd/wallet.txt"), "rw");
        this.channel = file.getChannel();   
    }

    /**
     * Gets the wallet balance. 
     *
     * @return                   The content of the wallet file as an integer
     */
    public int getBalance() throws IOException {
        FileLock lock = channel.lock(0, Long.MAX_VALUE, true); // shared LOCK — locks the wallet file to prevent concurrent access but read is allowed
        try {
            return getBalanceUnsafe();
        } finally {
            lock.release();
        }
    }

    private int getBalanceUnsafe() throws IOException {
        this.file.seek(0);
        return Integer.parseInt(this.file.readLine());
    }

    /**
     * Sets a new balance in the wallet
     *
     * @param  newBalance          new balance to write in the wallet
     */
    private void setBalance(int newBalance) throws Exception {
        this.file.setLength(0);
        String str = Integer.valueOf(newBalance).toString()+'\n'; 
        this.file.writeBytes(str); 
    }

    /**
     * Closes the RandomAccessFile in this.file
     */
    public void close() throws Exception {
        this.file.close();
    }

    /**
     * The method returns true if the withdraw was possible (i.e. the user had enough balance), 
     * and returns false otherwise.
     */
    public boolean safeWithdraw(int valueToWithdraw) throws Exception {
        FileLock lock = channel.lock(); // exclusive LOCK — locks the wallet file to prevent concurrent access
        try {
            int balance = this.getBalanceUnsafe(); // CHECK — reads wallet.txt once and caches it
            Thread.sleep(4000);
            if (balance < valueToWithdraw) {
                return false;
            }

            this.setBalance(balance - valueToWithdraw); // USE — uses the cached value, not a fresh read
            return true;
        
        } finally {
            lock.release(); // UNLOCK — releases the lock on the wallet file
        }
    }
}
