import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;
import java.io.FileWriter;
import java.io.BufferedWriter;

import weka.core.*;
import weka.core.converters.ArffSaver;
import weka.datagenerators.ClassificationGenerator;

public class TransactionsGenPlugin extends ClassificationGenerator {

    /**
     * for serialization
     */
    static final long serialVersionUID = 6069033710635728720L;

    /**
     * Number of attributes the dataset should have
     */
    protected int m_NumAttributes;

    /**
     * the number of transactions to generate
     */
    protected int m_NumTransactions;

    /**
     * the transaction fees
     */
    protected double[] m_transactionFees;

    /**
     * the transaction sizes
     */
    protected double[] m_transactionSizes;

    private static Attribute senderAddress;
    private static Attribute recipientAddress;
    private static Attribute amount;
    private static Attribute transactionFee;


    /**
     * Initializes the generator with default values
     */
    public TransactionsGenPlugin() {
        super();

        setNumAttributes(defaultNumAttributes());
        setNumTransactions(defaultNumTransactions());
    }

    /**
     * Returns a string describing this data generator.
     *
     * @return a description of the data generator suitable for displaying in the
     * explorer/experimenter gui
     */
    public String globalInfo() {
        return "BitcoinTransactionGenerator generates random Bitcoin transactions with transaction fees and sizes.";
    }


    /**
     * Generates a comment string that documentates the data generator. By default
     * this string is added at the beginning of the produced output as ARFF file
     * type, next after the options.
     *
     * @return string contains info about the generated rules
     */
    @Override
    public String generateStart() {
        StringBuilder result = new StringBuilder();

        result.append("%\n");
        result.append("% BitcoinTransactionGenerator - Random Bitcoin Transaction Generator\n");
        result.append("% Generates random Bitcoin transactions with transaction fees and sizes.\n");
        result.append("% Options:\n");
        result.append("%   -n <num_transactions>: Number of transactions to generate\n");
        result.append("%   -o <output_file.arff>: Output ARFF file name\n");
        result.append("%\n");

        return result.toString();
    }

    /**
     * Generates a comment string that documentats the data generator. By default
     * this string is added at the end of theproduces output as ARFF file type.
     *
     * @return string contains info about the generated rules
     * @throws Exception if the generating of the documentaion fails
     */
    @Override
    public String generateFinished() {
        // Additional information to include at the end of the ARFF file
        return "% End of Bitcoin transaction data.";
    }

    /**
     * Return if single mode is set for the given data generator mode depends on
     * option setting and or generator type.
     *
     * @return single mode flag
     * @throws Exception if mode is not set yet
     */
    @Override
    public boolean getSingleModeFlag() throws Exception {
        return true;
    }

    /**
     * Returns the revision string.
     *
     * @return the revision
     */
    @Override
    public String getRevision() {
        return RevisionUtils.extract("$Revision$");
    }

    /**
     * Returns an enumeration describing the available options.
     *
     * @return an enumeration of all the available options
     */
    @Override
    public Enumeration<Option> listOptions() {
        Vector<Option> result = enumToVector(super.listOptions());

        result.add(new Option("\tThe number of transactions to generate (default " + defaultNumTransactions() + ").",
                "n", 1, "-n <num>"));

        return result.elements();
    }

    /**
     * Parses a list of options for this object.
     *
     * @param options the list of options as an array of strings
     * @throws Exception if an option is not supported
     */
    @Override
    public void setOptions(String[] options) throws Exception {
        String tmpStr;

        super.setOptions(options);

        tmpStr = Utils.getOption('n', options);
        if (tmpStr.length() != 0) {
            setNumTransactions(Integer.parseInt(tmpStr));
        } else {
            setNumTransactions(defaultNumTransactions());
        }
    }

    /**
     * Gets the current settings of the datagenerator.
     *
     * @return an array of strings suitable for passing to setOptions
     */
    @Override
    public String[] getOptions() {
        Vector<String> result;
        String[] options;
        int i;

        result = new Vector<String>();
        options = super.getOptions();
        for (i = 0; i < options.length; i++) {
            result.add(options[i]);
        }

        result.add("-n");
        result.add("" + getNumTransactions());

        return result.toArray(new String[result.size()]);
    }

    /**
     * returns the default number of attributes
     *
     * @return the default number of attributes
     */
    protected int defaultNumAttributes() {
        return 4; // We'll have two attributes: transaction fee and transaction size
    }

    /**
     * Sets the number of attributes the dataset should have.
     *
     * @param numAttributes the new number of attributes
     */
    public void setNumAttributes(int numAttributes) {
        m_NumAttributes = numAttributes;
    }

    /**
     * Gets the number of attributes that should be produced.
     *
     * @return the number of attributes that should be produced
     */
    public int getNumAttributes() {
        return m_NumAttributes;
    }

    /**
     * Returns the tip text for this property
     *
     * @return tip text for this property suitable for displaying in the
     * explorer/experimenter gui
     */
    public String numAttributesTipText() {
        return "The number of attributes the generated data will contain.";
    }

    /**
     * returns the default number of transactions
     *
     * @return the default number of transactions
     */
    protected int defaultNumTransactions() {
        return 100; // Default number of transactions
    }

    /**
     * Sets the number of transactions to generate.
     *
     * @param numTransactions the new number of transactions
     */
    public void setNumTransactions(int numTransactions) {
        m_NumTransactions = numTransactions;
    }

    /**
     * Gets the number of transactions to generate.
     *
     * @return the number of transactions to generate
     */
    public int getNumTransactions() {
        return m_NumTransactions;
    }

    /**
     * Returns the tip text for this property
     *
     * @return tip text for this property suitable for displaying in the
     * explorer/experimenter gui
     */
    public String numTransactionsTipText() {
        return "The number of transactions to generate.";
    }

    /**
     * Initializes the format for the dataset produced.
     *
     * @return the format for the dataset
     * @throws Exception if the generating of the format failed
     */
    @Override
    public Instances defineDataFormat() throws Exception {
        ArrayList<Attribute> atts = new ArrayList<Attribute>();

        // Add attributes for sender address and receiver address as String attributes
        Attribute senderAddress = new Attribute("sender_address", (ArrayList<String>) null);
        Attribute receiverAddress = new Attribute("receiver_address", (ArrayList<String>) null);

        // Add attributes for transaction fee and transaction size as numeric attributes
        Attribute transactionFee = new Attribute("transaction_fee");
        Attribute transactionSize = new Attribute("transaction_size");

        // Add all attributes to the atts list
        atts.add(senderAddress);
        atts.add(receiverAddress);
        atts.add(transactionFee);
        atts.add(transactionSize);


        // Create Instances object with the defined attributes
        Instances datasetFormat = new Instances(getRelationNameToUse(), atts, 0);

        // Set class index to the last attribute
        datasetFormat.setClassIndex(datasetFormat.numAttributes() - 1);

        return datasetFormat;
    }

    /**
     * Generates one example of the dataset.
     *
     * @return the generated example
     * @throws Exception if the format of the dataset is not yet defined
     */
    @Override
    public Instance generateExample() throws Exception {
        Instance instance = new DenseInstance(4); // Generate instance using superclass method

        // Associate the instance with the dataset
        Instances dataset = defineDataFormat();
        instance.setDataset(dataset);

        // Generate random transaction fee, size, sender address, and receiver address
        instance.setValue(0, "bc1q" + Long.toHexString(new Random().nextLong()).toUpperCase()); // Random sender address
        instance.setValue(1, "bc1q" + Long.toHexString(new Random().nextLong()).toUpperCase()); // Random receiver address
        instance.setValue(2, Math.random() * 10); // Random transaction fee between 0 and 10
        instance.setValue(3, Math.random() * 1000); // Random transaction size between 0 and 1000

        return instance;
    }

    /**
     * Generates all examples of the dataset.
     *
     * @return the generated dataset
     * @throws Exception if the format of the dataset is not yet defined
     */
    @Override
    public Instances generateExamples() throws Exception {
        Instances dataset = defineDataFormat();
        dataset.setClassIndex(dataset.numAttributes() - 1); // Set class index

        System.out.println(dataset);
        for (int i = 0; i < getNumTransactions(); i++) {
            Instance instance = new DenseInstance(4); // Generate instance using superclass method

            // Associate the instance with the dataset
            instance.setDataset(dataset);

            // Generate random transaction fee, size, sender address, and receiver address
            instance.setValue(0, "bc1q" + Long.toHexString(new Random().nextLong()).toUpperCase()); // Random sender address
            instance.setValue(1, "bc1q" + Long.toHexString(new Random().nextLong()).toUpperCase()); // Random receiver address
            instance.setValue(2, Math.random() * 10); // Random transaction fee between 0 and 10
            instance.setValue(3, Math.random() * 1000); // Random transaction size between 0 and 1000

            dataset.add(instance);
        }

        return dataset;
    }

    /**
     * Main method for executing this class.
     *
     * @param args should contain arguments for the data producer:
     *             -o <output_file.arff>: specify the output ARFF file name
     *             -n <num_transactions>: specify the number of transactions to generate
     */
    public static void main(String[] args) {
        TransactionsGenPlugin generator = new TransactionsGenPlugin();
        // Set options
        /*try {
            generator.setOptions(args);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }*/

        // Generate dataset
        try {
            Instances dataset = generator.generateExamples();

            // Output dataset to ARFF file
            String outputFile = Utils.getOption('o', args);
            System.out.println("ok");
            System.out.println(outputFile);
            System.out.println("ko");

            if (outputFile == null || outputFile.isEmpty()) {
                System.out.println("Output file not specified. Use -o <output_file.arff> to specify output file. Saving to output.arff");
                outputFile = "./output.arff";
            }


            // Save dataset to ARFF file
            ArffSaver arffSaver = new ArffSaver();
            arffSaver.setInstances(dataset);
            arffSaver.setFile(new File(outputFile));
            arffSaver.writeBatch();

            ArffSaver saver = new ArffSaver();
            saver.setInstances(dataset);
            saver.setFile(new File("transactions.arff"));
            saver.writeBatch();

            System.out.println("Dataset saved to " + outputFile);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

}
