import weka.core.Instances;
import weka.core.Instance;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.DenseInstance;
import weka.core.converters.ArffSaver;
import java.io.File;
import java.util.Random;

public class BlockchainTransactionGenerator {
    private static FastVector attributes;
    private static Attribute senderAddress;
    private static Attribute recipientAddress;
    private static Attribute amount;
    private static Attribute transactionFee;

    static {
        // Change senderAddress and recipientAddress to string attributes
        senderAddress = new Attribute("SenderAddress", (FastVector) null);
        recipientAddress = new Attribute("RecipientAddress", (FastVector) null);
        amount = new Attribute("Amount");
        transactionFee = new Attribute("TransactionFee");

        attributes = new FastVector();
        attributes.addElement(senderAddress);
        attributes.addElement(recipientAddress);
        attributes.addElement(amount);
        attributes.addElement(transactionFee);
    }

    public static void main(int numberOfTransactions) throws Exception {
        Instances transactions = generateTransactions(numberOfTransactions);

        ArffSaver saver = new ArffSaver();
        saver.setInstances(transactions);
        saver.setFile(new File("transactions.arff"));
        saver.writeBatch();
    }

    private static Instances generateTransactions(int numTransactions) {
        Instances data = new Instances("BlockchainTransactions", attributes, numTransactions);

        for (int i = 0; i < numTransactions; i++) {
            Instance transaction = new DenseInstance(attributes.size());

            String sender = generateRandomAddress();
            String recipient = generateRandomAddress();

            double transactionAmount = generateRandomAmount();
            double fee = generateRandomFee();

            transaction.setValue(senderAddress, sender);
            transaction.setValue(recipientAddress, recipient);
            transaction.setValue(amount, transactionAmount);
            transaction.setValue(transactionFee, fee);

            data.add(transaction);
        }

        return data;
    }

    private static String generateRandomAddress() {
        return "bc1q" + Long.toHexString(new Random().nextLong()).toUpperCase();
    }

    private static double generateRandomAmount() {
        return new Random().nextDouble() * 1000;
    }

    private static double generateRandomFee() {
        return new Random().nextDouble() * 10;
    }
}
