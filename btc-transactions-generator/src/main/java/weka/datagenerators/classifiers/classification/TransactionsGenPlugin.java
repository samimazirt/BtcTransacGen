package weka.datagenerators.classifiers.classification;

import java.io.File;
import java.util.*;

import com.google.gson.*;
import weka.core.*;
import weka.core.converters.ArffSaver;
import weka.datagenerators.ClassificationGenerator;
import wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;
import wf.bitcoin.javabitcoindrpcclient.GenericRpcException;
import wf.bitcoin.javabitcoindrpcclient.util.Chain;
import wf.bitcoin.javabitcoindrpcclient.util.Util;

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
        return 25;
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
     *
     * Parsing options
     */
    public void optionsParser(String[] options) throws Exception {

        String tmpStr = Utils.getOption('n', options);
        if (tmpStr.length() != 0) {
            setNumTransactions(Integer.parseInt(tmpStr));
        } else {
            setNumTransactions(defaultNumTransactions());
        }
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
        ArrayList<String> trustedValues = new ArrayList<String>(2);
        trustedValues.add("true");
        trustedValues.add("false");

        Attribute amount = new Attribute("amount");
        Attribute fee = new Attribute("fee");
        Attribute confirmations = new Attribute("confirmations");
        //Attribute trusted = new Attribute("trusted");
        Attribute txid = new Attribute("txid", (ArrayList<String>) null);
        Attribute wtxid = new Attribute("wtxid", (ArrayList<String>) null);
        Attribute time = new Attribute("time");
        Attribute timereceived = new Attribute("timereceived");
        Attribute bip125Replaceable = new Attribute("bip125_replaceable", (ArrayList<String>) null);
        Attribute details = new Attribute("details");
        Attribute hex = new Attribute("hex", (ArrayList<String>) null);

// Attributes for details array elements
        Attribute address1 = new Attribute("address1", (ArrayList<String>) null);
        Attribute category1 = new Attribute("category1", (ArrayList<String>) null);
        Attribute amount1 = new Attribute("amount1");
        Attribute label1 = new Attribute("label1", (ArrayList<String>) null);
        Attribute vout1 = new Attribute("vout1");
        Attribute fee1 = new Attribute("fee1");
        Attribute abandoned1 = new Attribute("abandoned1", trustedValues);

        Attribute address2 = new Attribute("address2", (ArrayList<String>) null);
        Attribute parentDescs2 = new Attribute("parent_descs2");
        Attribute category2 = new Attribute("category2", (ArrayList<String>) null);
        Attribute amount2 = new Attribute("amount2");
        Attribute label2 = new Attribute("label2", (ArrayList<String>) null);
        Attribute vout2 = new Attribute("vout2");

        Attribute hexValue = new Attribute("hex_value");


        // Add all attributes to the atts list
        atts.add(amount);
        atts.add(fee);
        atts.add(confirmations);
        atts.add(new Attribute("trusted", trustedValues));;
        atts.add(txid);
        atts.add(wtxid);
        atts.add(time);
        atts.add(timereceived);
        atts.add(bip125Replaceable);
        atts.add(details);
        atts.add(hex);

// Add attributes for details array elements
        atts.add(address1);
        atts.add(category1);
        atts.add(amount1);
        atts.add(label1);
        atts.add(vout1);
        atts.add(fee1);
        atts.add(abandoned1);

        atts.add(address2);
        atts.add(parentDescs2);
        atts.add(category2);
        atts.add(amount2);
        atts.add(label2);
        atts.add(vout2);

        atts.add(hexValue);



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

    public Instances generateFromApplication() throws Exception {
        Gson gson = new Gson();

        Instances dataset = defineDataFormat();
        dataset.setClassIndex(dataset.numAttributes() - 1); // Set class index

        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] [%4$-7s] %5$s %n");

        BitcoindRpcClient client = new BitcoinJSONRPCClient();
        Util.ensureRunningOnChain(Chain.REGTEST, client);



        JsonRPCClient jsonRpcClient = new JsonRPCClient();
        String randomWalletName = Application.generateRandomString(10);


        try {
            Map<String, Object> result = jsonRpcClient.createWallet(randomWalletName);
            String walletName = (String) result.get("name");
            String warning = (String) result.get("warning");
            Application.LOGGER.info("Wallet created: " + walletName);
            if (warning != null) {
                Application.LOGGER.warning("Warning: " + warning);
            }
        } catch (GenericRpcException e) {
            Application.LOGGER.severe("Error creating wallet: " + e.getMessage());
            //return; // Exit the function if an error occurs


            try {
                Map<String, Object> result = jsonRpcClient.loadWallet(randomWalletName);
                String walletName = (String) result.get("name");
                String warning = (String) result.get("warning");
                Application.LOGGER.info("Wallet loaded: " + walletName);
                if (warning != null) {
                    Application.LOGGER.warning("Warning: " + warning);
                }
            } catch (GenericRpcException eLoad) {
                Application.LOGGER.severe("Error loading wallet: " + eLoad.getMessage());
                //return; // Exit the function if an error occurs
            }



        }

        String addr1 = client.getNewAddress();
        List<String> generatedBlocksHashes = client.generateToAddress(510, addr1);

        System.out.println("YEEEESSSS" + jsonRpcClient.getBalance());





        for (int i = 0; i < getNumTransactions(); i++) {
            Instance instance = new DenseInstance(dataset.numAttributes()); // Generate instance using superclass method

            // Associate the instance with the dataset
            instance.setDataset(dataset);

            String transaction = Application.signRawTransactionWithKeyTest_P2SH_P2WPKH(client, addr1);
            String preprocessedTransaction = transaction.replaceAll("label=,", "label=empty,");
            JsonObject transactionObject = new JsonObject();
            try {
                transactionObject = gson.fromJson(preprocessedTransaction, JsonObject.class);
                // Process the transactionObject
            } catch (JsonSyntaxException e) {
                // Handle the JSON parsing error
                e.printStackTrace(); // Print the stack trace for debugging
                // Handle the error gracefully, e.g., log it or display a friendly error message
            }

            // Set values from transactionObject to respective attributes
            instance.setValue(dataset.attribute("fee"), transactionObject.get("fee").getAsDouble());
            instance.setValue(dataset.attribute("confirmations"), transactionObject.get("confirmations").getAsInt());
            instance.setValue(dataset.attribute("trusted"), String.valueOf(transactionObject.get("trusted").getAsBoolean()));
            instance.setValue(dataset.attribute("txid"), transactionObject.get("txid").getAsString());
            instance.setValue(dataset.attribute("wtxid"), transactionObject.get("wtxid").getAsString());
            instance.setValue(dataset.attribute("time"), transactionObject.get("time").getAsLong());
            instance.setValue(dataset.attribute("timereceived"), transactionObject.get("timereceived").getAsLong());
            instance.setValue(dataset.attribute("bip125_replaceable"), transactionObject.get("bip125-replaceable").getAsString());
            instance.setValue(dataset.attribute("hex"), transactionObject.get("hex").getAsString());

            JsonArray detailsArray = transactionObject.getAsJsonArray("details");

            for (int j = 0; j < detailsArray.size(); j++) {
                JsonObject detailObject = detailsArray.get(j).getAsJsonObject();

                // Now you can use `i` as the index
                // Set values for details array attributes
                if (j == 0) {
                    instance.setValue(dataset.attribute("fee" + (j + 1)), detailObject.get("fee").getAsDouble());
                    instance.setValue(dataset.attribute("abandoned" + (j + 1)), String.valueOf(detailObject.get("abandoned").getAsBoolean()));
                    instance.setValue(dataset.attribute("address" + (j + 1)), client.getNewAddress());
                }
                instance.setValue(dataset.attribute("address" + (j + 1)), detailObject.get("address").getAsString());
                instance.setValue(dataset.attribute("category" + (j + 1)), detailObject.get("category").getAsString());
                instance.setValue(dataset.attribute("amount" + (j + 1)), detailObject.get("amount").getAsDouble());
                instance.setValue(dataset.attribute("label" + (j + 1)), detailObject.get("label").getAsString());
                instance.setValue(dataset.attribute("vout" + (j + 1)), detailObject.get("vout").getAsInt());
                instance.setValue(dataset.attribute("amount"), detailObject.get("amount").getAsDouble());

            }

            // Generate random transaction fee, size, sender address, and receiver address

            dataset.add(instance);
        }

        try {
            Map<String, Object> result = jsonRpcClient.unloadWallet(randomWalletName);
            String warning = (String) result.get("warning");
            Application.LOGGER.info("Wallet unloaded: " + randomWalletName);
            if (warning != null) {
                Application.LOGGER.warning("Warning: " + warning);
            }
        } catch (GenericRpcException e) {
            Application.LOGGER.severe("Error unloading wallet: " + e.getMessage());
            //return; // Exit the function if an error occurs
        }

        return dataset;
    }

    /**
     * weka.datagenerators.classifiers.classification.Main method for executing this class.
     *
     * @param args should contain arguments for the data producer:
     *             -o <output_file.arff>: specify the output ARFF file name
     *             -n <num_transactions>: specify the number of transactions to generate
     */
    public static void main(String[] args) {
        TransactionsGenPlugin generator = new TransactionsGenPlugin();

        try {
            generator.optionsParser(args);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }


        // Generate dataset
        try {
            Instances dataset = generator.generateFromApplication();
            // Output dataset to ARFF file
            String outputFile = Utils.getOption('o', args);
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
